package com.mutation.cachedDataIsNotTearedDown;

import com.mutation.SootMutationOperator;
import soot.*;
import soot.jimple.*;
import soot.options.Options;
import soot.util.Chain;
import soot.util.JasminOutputStream;

import java.io.*;
import java.util.Iterator;
import java.util.List;

public abstract class CacheDataMutationOperator extends SootMutationOperator {

    protected Stmt addStmt;
    protected Local cacheLocal;
    protected String methodSignature;

    protected CacheDataMutationOperator(String methodSignature) {
        this.methodSignature = methodSignature;
    }

    public abstract void createstateSetterTests();

    @Override
    public void mutateMethod() throws Exception {

        SootMethod polluterTest = this.createPolluterTest();
        try {
            this.getCurrentClass().addMethod(polluterTest);
        } catch (Exception e) {

        }

        SootMethod brittleTest = this.createBrittleTest();
        try {
            this.getCurrentClass().addMethod(brittleTest);
        } catch (Exception e) {

        }
        createstateSetterTests();

       /* SootMethod mainTest = this.createMainTest(polluterTest, brittleTest);
        this.getCurrentClass().addMethod(mainTest);
        this.addNonDeterminism(mainTest);*/

    }

    protected SootMethod createMainTest(SootMethod polluterTest, SootMethod brittleTest) {

        SootMethod mainTest = this.createTestMethod("testMainMutant" + polluterTest.getName() + brittleTest.getName());
        Local identityLocal = getIdentityLocal(mainTest);
        Local thisLocal = createThisLocal(mainTest);
        this.getCurrentMethod().getActiveBody().getLocals().add(thisLocal);
        castThisLocal(mainTest, thisLocal, identityLocal);

        VirtualInvokeExpr polluterInvocation = Jimple.v().newVirtualInvokeExpr(thisLocal, polluterTest.makeRef());
        InvokeStmt polluterStmt = Jimple.v().newInvokeStmt(polluterInvocation);
        InsertStmtEnd(mainTest, polluterStmt);

        VirtualInvokeExpr victimTestInvocation = Jimple.v().newVirtualInvokeExpr(thisLocal, brittleTest.makeRef());
        InvokeStmt victimTestStmt = Jimple.v().newInvokeStmt(victimTestInvocation);
        InsertStmtEnd(mainTest, victimTestStmt);

        return mainTest;
    }

    @Override
    public <T extends Unit> List<T> locateUnits() {
        return null;
    }

    private SootMethod createOrderDependentTest(String testName) {
        SootMethod test = this.createTestMethod(testName+this.getClass().getSimpleName());
        //this.addIgnoreAnnotation(test);
        test.setActiveBody((Body) this.getCurrentMethod().getActiveBody().clone());
        return test;
    }


    protected SootMethod createPolluterTest() {
        SootMethod test = createOrderDependentTest("b_polluter" + this.getCurrentMethod().getName());
        addPollutingStatement(test);
        return test;
    }

    private Stmt getEquivalentStmt(SootMethod test, Stmt stmt) {
        Chain<Unit> units = test.getActiveBody().getUnits();
        Iterator<Unit> iterator = units.snapshotIterator();

        while (iterator.hasNext()) {
            Stmt currentStmt = (Stmt) iterator.next();
            if (currentStmt.toString().equals(stmt.toString())) {
                return currentStmt;
            }
        }
        return null;
    }

    protected void addPollutingStatement(SootMethod test) {
        this.InsertStmtEnd(test, (Stmt) this.getEquivalentStmt(test, this.addStmt).clone());
    }

    protected abstract void addBrittleStatement(SootMethod test);

    protected SootMethod createBrittleTest() {
        SootMethod test = this.createTestMethod("a_brittle" + this.getCurrentMethod().getName());
        //this.addIgnoreAnnotation(test);
        addBrittleStatement(test);
        return test;
    }

    @Override
    public boolean isApplicable() {
        Chain<Unit> units = this.getCurrentMethod().getActiveBody().getUnits();
        Iterator<Unit> unitIterator = units.snapshotIterator();

        while (unitIterator.hasNext()) {
            Stmt stmt = (Stmt) unitIterator.next();

            if (!stmt.containsInvokeExpr()) {
                continue;
            }

            InvokeExpr invokeExpr = stmt.getInvokeExpr();

            if (!(invokeExpr instanceof InstanceInvokeExpr)) {
                continue;
            }

            InstanceInvokeExpr instanceInvokeExpr = (InstanceInvokeExpr) invokeExpr;

            if (instanceInvokeExpr.getMethod().getSignature().contains(this.methodSignature)) {
                this.cacheLocal = (Local) instanceInvokeExpr.getBase();
                this.addStmt = stmt;
                return true;
            }
        }
        return false;
    }

    @Override
    public void exportClass(String outputDir, String methodName) throws IOException {
        this.setCurrentClass(this.getCurrentClass());
        super.exportClass(outputDir, methodName);
    }



    protected Local getEquivalentLocal(SootMethod test, Local originalLocal) {
        Chain<Local> locals = test.getActiveBody().getLocals();
        Iterator<Local> localIterator = locals.snapshotIterator();

        while (localIterator.hasNext()) {
            Local local = localIterator.next();
            if (local.getName().equals(originalLocal.getName())) {
                return local;
            }
        }
        return null;
    }

    @Override
    public int getMutantNumber() {
        return 1;
    }

}
