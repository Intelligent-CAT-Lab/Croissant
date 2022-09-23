package com.mutation.filepermission;

import com.mutation.SootMutationOperator;
import soot.*;
import soot.jimple.*;
import soot.options.Options;
import soot.util.Chain;
import soot.util.JasminOutputStream;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class FileMutationOperator extends SootMutationOperator {

    public SootMethod victimTest;
    public String fileName;
    public Unit fileCreationUnit; //TODO: remove
    public Local fileRValue;


    protected abstract SootMethod setFilePermission(String fileName, Integer mode, String testName);

    protected SootMethod createstateSetterTest(int stateSetterNumber) {
        return this.makestateSetterTest(setFilePermission(this.fileName, 1, "c_stateSetter" + stateSetterNumber + victimTest.getName()));
    }

    protected void createstateSetterTests() {
        for (int i = 0; i < 5; i++) {
            SootMethod stateSetterTest = createstateSetterTest(i);
            this.getCurrentClass().addMethod(stateSetterTest);
        }
    }

    protected Unit findConstructorUnit(SootMethod test) {
        Chain<Unit> units = test.getActiveBody().getUnits();
        Iterator<Unit> iterator = units.snapshotIterator();

        while (iterator.hasNext()) {
            Unit unit = iterator.next();
            Stmt stmt = (Stmt) unit;

            if (!(stmt instanceof AssignStmt)) {
                continue;
            }

            AssignStmt assignStmt = (AssignStmt) stmt;

            if (assignStmt.getLeftOp().toString().equals(this.fileRValue.toString())) {
                return stmt;
            }
        }
        return null;
    }

    public SootMethod createVictimTest() {
        SootMethod victimTest = this.createTestMethod("b_victim" + this.victimTest.getName());
        Body clonedBody = (Body) this.victimTest.getActiveBody().clone();
        victimTest.setActiveBody(clonedBody);
        return victimTest;
    }

    @Override
    public void mutateMethod() throws Exception {

        createstateSetterTests();

        SootMethod polluterTest = createPolluterTest(this.fileName);
        this.getCurrentClass().addMethod(polluterTest);

        /*SootMethod dependenceTest = createDependenceTest(this.fileName);
        this.getCurrentClass().addMethod(dependenceTest);*/

        SootMethod victimTest = createVictimTest();
        this.getCurrentClass().addMethod(victimTest);




/*        SootMethod mainTest = createMainTest(polluterTest, dependenceTest);
        this.getCurrentClass().addMethod(mainTest);*/

        /*this.addNonDeterminism(mainTest);*/
    }


    @Override
    public <T extends Unit> List<T> locateUnits() {
        return null;
    }

    protected abstract SootMethod createDependenceTest(String fileName);

    protected abstract SootMethod createPolluterTest(String fileName);

    protected String findValue(Local var) {
        Chain<Unit> units = this.getUnits();
        Iterator<Unit> iterator = units.snapshotIterator();

        while (iterator.hasNext()) {
            Stmt stmt = (Stmt) iterator.next();

            if (!(stmt instanceof AssignStmt)) {
                continue;
            }

            AssignStmt assignStmt = (AssignStmt) stmt;

            if (!assignStmt.getLeftOp().equivTo(var)) {
                continue;
            }

            if (!(assignStmt.getRightOp() instanceof StringConstant)) {
                continue;
            }

            return ((StringConstant) assignStmt.getRightOp()).value;
        }
        return null;
    }

    protected SootMethod createMainTest(SootMethod polluterTest, SootMethod dependencyTest) {

        Unit tryCatchStartUnit = null;
        Unit tryCatchEndUnit;

        SootMethod mainTest = this.createTestMethod("mainTestMutant" + polluterTest.getName() + dependencyTest.getName() + this.getClass().getSimpleName());
        Local identityLocal = getIdentityLocal(mainTest);
        Local thisLocal = createThisLocal(mainTest);
        castThisLocal(mainTest, thisLocal, identityLocal);

        /*VirtualInvokeExpr polluterInvocation = Jimple.v().newVirtualInvokeExpr(thisLocal, polluterTest.makeRef());
        InvokeStmt polluterStmt = Jimple.v().newInvokeStmt(polluterInvocation);
        InsertStmtEnd(mainTest, polluterStmt);
        tryCatchStartUnit = polluterStmt;*/

        /*VirtualInvokeExpr victimTestInvocation;
        SootMethodRef victimTestRef = null;*/

        /*try {
            victimTestRef = this.victimTest.makeRef();
        } catch (NullPointerException e) {
            this.victimTest = this.getCurrentClass().getMethod(this.victimTest.getSubSignature());
            victimTestRef = this.victimTest.makeRef();
        } finally {
            victimTestInvocation = Jimple.v().newVirtualInvokeExpr(thisLocal, victimTestRef);
        }*/

        /*InvokeStmt victimTestStmt = Jimple.v().newInvokeStmt(victimTestInvocation);
        InsertStmtEnd(mainTest, victimTestStmt);*/

        Stmt lastStmt = odInvoke(this.getCurrentClass(), mainTest, null, polluterTest, victimTest);

        Chain<Unit> units = mainTest.getActiveBody().getUnits();
        Iterator<Unit> iterator = units.snapshotIterator();

        while (iterator.hasNext()) {
            Stmt stmt = (Stmt) iterator.next();
            if (stmt instanceof IdentityStmt) {
                continue;
            } else {
                tryCatchStartUnit = stmt;
                break;
            }
        }

        tryCatchEndUnit = lastStmt;

        VirtualInvokeExpr dependencyTestInvocation = Jimple.v().newVirtualInvokeExpr(thisLocal, dependencyTest.makeRef());
        InvokeStmt dependencyTestStmt = Jimple.v().newInvokeStmt(dependencyTestInvocation);
        //InsertStmtEnd(mainTest, dependencyTestStmt);

        ArrayList<Unit> catchBlock = new ArrayList<>();
        catchBlock.add(dependencyTestStmt);

        this.wrapTestWithTryCatch(mainTest, tryCatchStartUnit, tryCatchEndUnit, catchBlock);

        return mainTest;
    }


    @Override
    public void exportClass(String outputDir, String methodName) throws IOException {
        exportSubclasses(outputDir);
        Options.v().set_output_dir(outputDir);
        String fileName = SourceLocator.v().getFileNameFor(this.getCurrentClass(), Options.output_format_class);
        OutputStream streamOut = new JasminOutputStream(
            new FileOutputStream(fileName));
        PrintWriter writerOut = new PrintWriter(
            new OutputStreamWriter(streamOut));
        JasminClass jasminClass = new soot.jimple.JasminClass(this.getCurrentClass());
        jasminClass.print(writerOut);
        writerOut.flush();
        streamOut.close();
    }

    public void wrapTestWithTryCatch(SootMethod test, Unit startingUnit, Unit endingUnit, List<Unit> catchBlock) {


        Local exceptionClass = Jimple.v().newLocal("exception", RefType.v("java.lang.Exception"));
        test.getActiveBody().getLocals().add(exceptionClass);

        SootMethod assertTrue = this.getAssertTrue();

        StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(assertTrue.makeRef(), IntConstant.v(0));
        Stmt staticInvokeStmt = Jimple.v().newInvokeStmt(staticInvokeExpr);
        catchBlock.add(staticInvokeStmt);

        this.tryCatch(test, startingUnit, endingUnit, catchBlock, exceptionClass);
    }

    @Override
    public int getMutantNumber() {
        return 1;
    }
}
