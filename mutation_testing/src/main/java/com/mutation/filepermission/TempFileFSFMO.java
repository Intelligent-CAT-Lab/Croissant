package com.mutation.filepermission;

import soot.*;
import soot.jimple.*;
import soot.util.Chain;

import java.util.Iterator;

public class TempFileFSFMO extends FileStringFMO {

    String extension;

    public TempFileFSFMO() {
        super("java.io.File createTempFile(java.lang.String,java.lang.String)");
    }

    @Override
    protected SootMethod createPolluterTest(String fileName) {
        SootMethod polluter = setFilePermission(fileName, 0);
        return polluter;
    }

    @Override
    public void mutateMethod() throws Exception {
        SootMethod polluterTest = createPolluterTest(this.fileName);
        this.getCurrentClass().addMethod(polluterTest);
        polluterTest.setActiveBody(setFilePermission("mutant" + this.getCurrentMethodName(), 0).getActiveBody());
        this.addNonDeterminism(polluterTest);
    }

    @Override
    public boolean isApplicable() {

        this.fileName = null;

        Chain<Unit> units = this.getJimpleBody().getUnits();
        Iterator<Unit> iterator = units.snapshotIterator();

        this.fileName = null;
        this.victimTest = null;

        while (iterator.hasNext()) {
            Stmt stmt = (Stmt) iterator.next();

            if (!(stmt instanceof AssignStmt)) {
                continue;
            }

            AssignStmt assignStmt = (AssignStmt) stmt;
            Value rightOp = assignStmt.getRightOp();

            if (!(rightOp instanceof StaticInvokeExpr)) {
                continue;
            }

            StaticInvokeExpr staticInvokeExpr = (StaticInvokeExpr) rightOp;

            if (staticInvokeExpr.getMethod().getSignature().contains(this.methodSignature)) {

                if (staticInvokeExpr.getArg(0) instanceof StringConstant) {
                    this.fileName = ((StringConstant) staticInvokeExpr.getArg(0)).value;
                } else if (staticInvokeExpr.getArg(0) instanceof Local) {
                    Local arg = (Local) staticInvokeExpr.getArg(0);
                    this.fileName = this.findValue(arg);
                }

                if (staticInvokeExpr.getArg(1) instanceof NullConstant) {
                    this.extension = "";
                } else if (staticInvokeExpr.getArg(1) instanceof Local) {
                    Local arg = (Local) staticInvokeExpr.getArg(1);
                    this.fileName = this.findValue(arg);
                } else {
                    this.extension = ((StringConstant) staticInvokeExpr.getArg(1)).value;
                }
                this.victimTest = this.getCurrentMethod();
                this.victimTest.setName("victimMutant_"+this.victimTest.getName());
            }

        }
        return (this.fileName != null);
    }


    public Local locateTempFileLocal(SootMethod test) {
        Chain<Unit> units = test.getActiveBody().getUnits();
        Iterator<Unit> unitIterator = units.snapshotIterator();

        while (unitIterator.hasNext()) {
            Stmt stmt = (Stmt) unitIterator.next();
            if (!(stmt instanceof AssignStmt)) {
                continue;
            }

            Value rightOp = ((AssignStmt) stmt).getRightOp();

            if (!(rightOp instanceof StaticInvokeExpr)) {
                continue;
            }

            StaticInvokeExpr staticInvokeExpr = (StaticInvokeExpr) rightOp;

            if (staticInvokeExpr.getMethod().getSignature().contains(this.methodSignature)) {
                return (Local) ((AssignStmt) stmt).getLeftOp();
            }
        }
        return null;
    }

    @Override
    protected SootMethod setFilePermission(String fileName, Integer mode) {

        SootMethod test;
        if (mode == 0) {
            test = this.createTestMethod("a_PolluterMutant_" + this.victimTest.getName() + "__");
        } else {
            test = this.createTestMethod("c_DependenceMutant_" + this.victimTest.getName() + "__");
        }
        test.setActiveBody((Body) this.getCurrentMethod().getActiveBody().clone());

        Local fileLocal = this.locateTempFileLocal(test);
        SootClass fileClass = Scene.v().forceResolve("java.io.File", SootClass.SIGNATURES);

        SootMethod setWriteable = fileClass.getMethod("boolean setWritable(boolean)");
        VirtualInvokeExpr setWritableExpr = Jimple.v().newVirtualInvokeExpr(fileLocal, setWriteable.makeRef(), IntConstant.v(mode));
        InvokeStmt setWritableStmt = Jimple.v().newInvokeStmt(setWritableExpr);
        this.InsertStmtBeginning(test, setWritableStmt);


        SootMethod setReadable = fileClass.getMethod("boolean setReadable(boolean)");
        VirtualInvokeExpr setReadableExpr = Jimple.v().newVirtualInvokeExpr(fileLocal, setReadable.makeRef(), IntConstant.v(mode));
        InvokeStmt setReadableStmt = Jimple.v().newInvokeStmt(setReadableExpr);
        this.InsertStmtBeginning(test, setReadableStmt);

        return test;
    }
}
