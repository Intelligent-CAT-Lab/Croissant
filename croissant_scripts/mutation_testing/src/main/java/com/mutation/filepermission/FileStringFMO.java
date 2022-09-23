package com.mutation.filepermission;


import soot.*;
import soot.jimple.*;
import soot.util.Chain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class FileStringFMO extends FileMutationOperator {

    String methodSignature;

    protected FileStringFMO(String methodSignature) {
        this.methodSignature = methodSignature;
    }


    @Override
    public boolean isApplicable() {

        this.fileName = null;

        Chain<Unit> units = this.getJimpleBody().getUnits();
        Iterator<Unit> iterator = units.snapshotIterator();

        while (iterator.hasNext()) {
            Stmt stmt = (Stmt) iterator.next();

            if (!stmt.containsInvokeExpr()) {
                continue;
            }

            InvokeExpr invokeExpr = stmt.getInvokeExpr();

            if (!(invokeExpr instanceof SpecialInvokeExpr)) {
                continue;
            }

            SpecialInvokeExpr specialInvokeExpr = (SpecialInvokeExpr) invokeExpr;

            if (specialInvokeExpr.getMethod().getSignature().contains(this.methodSignature)) {

                if (specialInvokeExpr.getArg(0) instanceof StringConstant) {
                    this.fileName = ((StringConstant) specialInvokeExpr.getArg(0)).value;
                } else if (specialInvokeExpr.getArg(0) instanceof Local) {
                    Local arg = (Local) specialInvokeExpr.getArg(0);
                    this.fileName = this.findValue(arg);
                }
                this.victimTest = this.getCurrentMethod();
            }
        }
        return (this.fileName != null);
    }

    @Override
    protected SootMethod createDependenceTest(String fileName) {
        SootMethod dependence = setFilePermission(fileName, 1);
        /*this.addIgnoreAnnotation(dependence);*/
        return dependence;
    }

    @Override
    protected SootMethod createPolluterTest(String fileName) {
        SootMethod polluter = setFilePermission(fileName, 0);
        /*this.addIgnoreAnnotation(polluter);*/
        return polluter;
    }

    protected SootMethod setFilePermission(String fileName, Integer mode) {
        return setFilePermission(fileName, mode, this.victimTest.getName());
    }

    protected SootMethod setFilePermission(String fileName, Integer mode, String testName) {

        SootMethod test;
        if (mode == 0) {
            test = this.createTestMethod("a_PolluterMutant" + testName);
        } else {
            test = this.createTestMethod("c_stateSetter" + testName);
        }
        test.setActiveBody((Body) this.getCurrentMethod().getActiveBody().clone());

        Local fileLocal = Jimple.v().newLocal("fileLocal", RefType.v("java.io.File"));
        test.getActiveBody().getLocals().add(fileLocal);

        Local fileLocalRValue = Jimple.v().newLocal("$fileLocal", RefType.v("java.io.File"));
        test.getActiveBody().getLocals().add(fileLocalRValue);

        List<Stmt> statements = new ArrayList<>();

        NewExpr newFileExpr = Jimple.v().newNewExpr(RefType.v("java.io.File"));
        AssignStmt newFileAssign = Jimple.v().newAssignStmt(fileLocalRValue, newFileExpr);
        this.InsertStmtBeginning(test, newFileAssign);

        SootClass fileClass = Scene.v().forceResolve("java.io.File", SootClass.SIGNATURES);
        SootMethod fileInitMethod = fileClass.getMethod("void <init>(java.lang.String)");
        SpecialInvokeExpr fileInitExpr = Jimple.v().newSpecialInvokeExpr(fileLocalRValue, fileInitMethod.makeRef(), StringConstant.v(fileName));
        InvokeStmt invokeStmt = Jimple.v().newInvokeStmt(fileInitExpr);
        test.getActiveBody().getUnits().insertAfter(invokeStmt, newFileAssign);

        AssignStmt initFileLocal = Jimple.v().newAssignStmt(fileLocal, fileLocalRValue);
        test.getActiveBody().getUnits().insertAfter(initFileLocal, invokeStmt);

        SootMethod setWriteable = fileClass.getMethod("boolean setWritable(boolean)");
        VirtualInvokeExpr setWritableExpr = Jimple.v().newVirtualInvokeExpr(fileLocal, setWriteable.makeRef(), IntConstant.v(mode));
        InvokeStmt setWritableStmt = Jimple.v().newInvokeStmt(setWritableExpr);
        test.getActiveBody().getUnits().insertAfter(setWritableStmt, initFileLocal);

        SootMethod setReadable = fileClass.getMethod("boolean setReadable(boolean)");
        VirtualInvokeExpr setReadableExpr = Jimple.v().newVirtualInvokeExpr(fileLocal, setReadable.makeRef(), IntConstant.v(mode));
        InvokeStmt setReadableStmt = Jimple.v().newInvokeStmt(setReadableExpr);
/*        this.InsertStmtBeginning(test, setReadableStmt);*/

        if (mode == 1) {
            SootClass fileWriterClass = Scene.v().forceResolve("java.io.FileWriter", SootClass.SIGNATURES);

            Local fileWriterL = Jimple.v().newLocal("fileWriter", fileWriterClass.getType());
            test.getActiveBody().getLocals().add(fileWriterL);
            Local fileWriterR = Jimple.v().newLocal("$fileWriter", fileWriterClass.getType());
            test.getActiveBody().getLocals().add(fileWriterR);

            //$r2 = new java.io.FileWriter;
            NewExpr newFileWriterExpr = Jimple.v().newNewExpr(fileWriterClass.getType());
            AssignStmt newFileWriterAssign = Jimple.v().newAssignStmt(fileWriterR, newFileWriterExpr);
            this.InsertStmtEnd(test, newFileWriterAssign);

            //specialinvoke $r2.<java.io.FileWriter: void <init>(java.io.File)>(r1);
            SootMethod initMethod = fileWriterClass.getMethod("void <init>(java.io.File)");
            SpecialInvokeExpr fileWriterInitExpr = Jimple.v().newSpecialInvokeExpr(fileWriterR, initMethod.makeRef(), fileLocal);
            InvokeStmt fileWriterInitStmt = Jimple.v().newInvokeStmt(fileWriterInitExpr);
            this.InsertStmtEnd(test, fileWriterInitStmt);

            //r3 = $r2;
            AssignStmt fileWriterAssign = Jimple.v().newAssignStmt(fileWriterL, fileWriterR);
            this.InsertStmtEnd(test, fileWriterAssign);

            //virtualinvoke r3.<java.io.FileWriter: void write(java.lang.String)>("Hello user3821496\n");
            SootMethod writerMethod = fileWriterClass.getMethodByNameUnsafe("write");

            if (writerMethod == null) {
                List<Type> phantomArgs = new ArrayList<>();
                phantomArgs.add(RefType.v("java.lang.String"));
                writerMethod = createPhantomMethod(fileWriterClass.getName(), "void write(java.lang.String)", "write", phantomArgs, VoidType.v(), false);
            }

            VirtualInvokeExpr writeInvokeExpr = Jimple.v().newVirtualInvokeExpr(fileWriterL, writerMethod.makeRef(), StringConstant.v("hello world"));
            InvokeStmt writeInvokeStmt = Jimple.v().newInvokeStmt(writeInvokeExpr);
            this.InsertStmtEnd(test, writeInvokeStmt);



        }

        return test;
    }
}
