package com.mutation.filepermission;

import soot.*;
import soot.jimple.*;
import soot.util.Chain;

import java.util.Iterator;

public class FileObjectFMO extends FileMutationOperator {
    @Override
    public boolean isApplicable() {

        this.fileName = null;
        this.victimTest = null;

        Chain<Unit> units = this.getJimpleBody().getUnits();
        Iterator<Unit> iterator = units.snapshotIterator();
        Stmt fileWriterStmt = null;

        while (iterator.hasNext()) {
            Stmt stmt = (Stmt) iterator.next();

            if (!stmt.containsInvokeExpr()) {
                continue;
            }

            InvokeExpr invokeExpr = stmt.getInvokeExpr();
            //System.out.println(invokeExpr.getMethod().getSignature());

            if (!(invokeExpr instanceof SpecialInvokeExpr)) {
                continue;
            }

            SpecialInvokeExpr specialInvokeExpr = (SpecialInvokeExpr) invokeExpr;
            //System.out.println(specialInvokeExpr.getMethod().getSignature());

            if (specialInvokeExpr.getMethod().getSignature().contains("java.io.File: void <init>(java.lang.String)") && specialInvokeExpr.getArg(0) instanceof StringConstant) {
                this.fileName = ((StringConstant) specialInvokeExpr.getArg(0)).value;
                this.victimTest = this.getCurrentMethod();
                this.fileRValue = (Local) specialInvokeExpr.getBase();
            } else if (specialInvokeExpr.getMethod().getSignature().contains("java.io.FileWriter: void <init>(java.io.File)")) {
                fileWriterStmt = stmt;
            }
        }

        return ((this.fileName != null) && (fileWriterStmt != null));
    }


    protected SootMethod setFilePermission(String fileName) {
        return setFilePermission(1, fileName);
    }



    private SootMethod setFilePermission(int mode, String fileName) {
        return setFilePermission(fileName, mode, victimTest.getName());
    }

    @Override
    protected SootMethod setFilePermission(String fileName, Integer mode, String testName) {

        SootMethod polluterTest;
        if (mode == 1) {
            polluterTest = this.createTestMethod("CleanerMutant" + testName);
        } else {
            polluterTest = this.createTestMethod("PolluterMutant" + testName);
        }

        polluterTest.setActiveBody((Body) this.getJimpleBody().clone());
        Local fileLocal = this.findFileLocal(polluterTest);

        SootClass fileClass = Scene.v().forceResolve("java.io.File", SootClass.SIGNATURES);
        SootMethod setWritableMethod = fileClass.getMethod("boolean setWritable(boolean)");

        VirtualInvokeExpr setWritableFalseExpr = Jimple.v().newVirtualInvokeExpr(fileLocal, setWritableMethod.makeRef(), IntConstant.v(mode));
        InvokeStmt setWritableFalseStmt = Jimple.v().newInvokeStmt(setWritableFalseExpr);


        /*specialinvoke $r0.<java.io.File: void <init>(java.lang.String)>("test.txt")*/

        Chain<Unit> units = polluterTest.getActiveBody().getUnits();
        Unit pivot1 = null;
        Unit pivot0 = null;
        Iterator<Unit> unitIterator = units.snapshotIterator();
        if (mode == 1){
        while(unitIterator.hasNext()) {
            Unit unit = unitIterator.next();
            Stmt stmt = (Stmt) unit;

            if (!(stmt instanceof InvokeStmt)) {
                continue;
            }

            InvokeStmt invokeStmt = (InvokeStmt) stmt;
            InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();

            if (!(invokeExpr.getMethod().getSignature().contains("init"))) {
                continue;
            }

            if (!(invokeExpr instanceof SpecialInvokeExpr)) {
                continue;
            }

            SpecialInvokeExpr specialInvokeExpr = (SpecialInvokeExpr) invokeExpr;

            if (!(specialInvokeExpr.getBase().equals(setWritableFalseExpr.getBase()))) {
                continue;
            }

            pivot1 = unit;
        }
        }
        if (mode == 0){
            while(unitIterator.hasNext()){
                Unit unit = unitIterator.next();
                Stmt stmt = (Stmt) unit;

                if (!(stmt instanceof InvokeStmt)) {
                    continue;
                }

                InvokeStmt invokeStmt = (InvokeStmt) stmt;
                InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();

                if (!(invokeExpr.getMethod().getSignature().contains("<java.io.BufferedWriter: void close()>"))) {
                    continue;
                }
                pivot0 = unit;

            }

        }




        /*this.InsertStmtBeginning(polluterTest, setWritableFalseStmt);*/
        if (mode == 1) {
            polluterTest.getActiveBody().getUnits().insertAfter(setWritableFalseStmt, pivot1);
        }
        if (mode == 0){
            polluterTest.getActiveBody().getUnits().insertAfter(setWritableFalseStmt, pivot0);

        }
        /*this.addIgnoreAnnotation(polluterTest);*/

        if (mode == 1) {
            boolean remove = false;
            unitIterator = units.snapshotIterator();

            while (unitIterator.hasNext()) {
                Unit unit = unitIterator.next();
                Stmt stmt = (Stmt) unit;

                if (!remove && stmt.equals(setWritableFalseStmt)) {
                    remove = true;
                }

                else if (!(stmt instanceof ReturnVoidStmt) && remove) {
                    units.remove(unit);
                }
            }
        }
        if (mode == 0){
            unitIterator = units.snapshotIterator();
            while (unitIterator.hasNext()){
                Unit unit = unitIterator.next();
                Stmt stmt = (Stmt) unit;
                if (!stmt.containsInvokeExpr()) {
                    continue;
                }

                InvokeExpr invokeExpr = stmt.getInvokeExpr();
                if (invokeExpr.getMethod().getSignature().contains("<java.io.Writer: void write(java.lang.String)>"))
                {
                    //System.out.println(invokeExpr.getMethod().getSignature());
                    units.remove(unit);
                }
                if (invokeExpr.getMethod().getSignature().contains("<java.io.BufferedWriter: void <init>(java.io.Writer)>"))
                {
                    //System.out.println(invokeExpr.getMethod().getSignature());
                    units.remove(unit);
                }
                if (invokeExpr.getMethod().getSignature().contains("<java.io.BufferedWriter: void close()>"))
                {
                    //System.out.println(invokeExpr.getMethod().getSignature());
                    units.remove(unit);
                }
            }

        }


        return polluterTest;
    }


    protected SootMethod createDependenceTest(String fileName) {
        return setFilePermission(1, fileName);
    }

    @Override
    protected SootMethod createPolluterTest(String fileName) {
        return setFilePermission(0, fileName);
    }


    public Local findFileLocal(SootMethod test) {
        Chain<Unit> units = test.getActiveBody().getUnits();
        Iterator<Unit> unitIterator = units.snapshotIterator();

        while (unitIterator.hasNext()) {
            Stmt stmt = (Stmt) unitIterator.next();

            if (!stmt.containsInvokeExpr()) {
                continue;
            }

            if (!(stmt.getInvokeExpr() instanceof SpecialInvokeExpr)) {
                continue;
            }

            SpecialInvokeExpr specialInvokeExpr = (SpecialInvokeExpr) stmt.getInvokeExpr();

            if (specialInvokeExpr.getMethod().getSignature().contains("java.io.FileWriter: void <init>(java.io.File)")) {
                return (Local) specialInvokeExpr.getArg(0);
            }
        }
        return null;
    }
}
