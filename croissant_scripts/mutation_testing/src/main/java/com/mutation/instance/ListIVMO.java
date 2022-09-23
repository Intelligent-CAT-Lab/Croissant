package com.mutation.instance;

import soot.*;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.StaticInvokeExpr;

public class ListIVMO extends InstanceVariableMutationOperator{
    @Override
    protected String getTestName() {
        return null;
    }

    @Override
    protected boolean isFieldAppropriate(SootField field) {
        return field.getType().toString().contains("List") && field.getType().toString().contains("java.util");
    }


    @Override
    public void initializeField(SootMethod test, SootField field) {
        Scene.v().forceResolve("java.util.ArrayList", SootClass.SIGNATURES);
        this.initializeCollection(test, field,  "<java.util.ArrayList: void <init>()>", "java.util.ArrayList");
    }


    @Override
    public void wipeField(SootMethod test, Value rValue) {
        clearCollection(test, (Local) rValue, "java.util.List", "void clear()");
        Local boolLocal = getIsEmpty(test, (Local) rValue, "java.util.List", "boolean isEmpty()");
        insertWipeFieldAssert(test, boolLocal);
    }

    @Override
    public void insertWipeFieldAssert(SootMethod test, Local boolLocal) {
        insertAssertTrue(test, boolLocal);
    }

    @Override
    public void insertToField(SootMethod test, Local rValue) {
        insertToCollection(test, rValue, "java.util.List", "boolean add(java.lang.Object)");
        Local boolLocal = getDoesContain(test, rValue, "java.util.List","boolean contains(java.lang.Object)");
        insertInsertToFieldAssert(test, rValue, boolLocal);
    }

    @Override
    public void insertInsertToFieldAssert(SootMethod test, Local rValue, Local boolLocal) {
        insertAssertTrue(test, boolLocal);
    }

    public void insertAssertTrue(SootMethod test, Local boolLocal) {
        SootMethod assertTrueMethod = this.getAssertTrue();
        StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(assertTrueMethod.makeRef(), boolLocal);
        InvokeStmt invokeStmt = Jimple.v().newInvokeStmt(staticInvokeExpr);
        InsertStmtEnd(test, invokeStmt);
    }

    @Override
    public void readField(SootMethod test, Local rValue) {
        Local boolLocal = getDoesContain(test, rValue, "java.util.List","boolean contains(java.lang.Object)");
        insertReadFieldAssert(test, boolLocal);
    }

    @Override
    public void insertReadFieldAssert(SootMethod test, Local boolLocal) {
        insertAssertTrue(test, boolLocal);
    }
}
