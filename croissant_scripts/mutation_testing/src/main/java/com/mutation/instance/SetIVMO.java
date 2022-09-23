package com.mutation.instance;

import soot.*;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.StaticInvokeExpr;

public class SetIVMO extends InstanceVariableMutationOperator {
    @Override
    protected String getTestName() {
        return null;
    }

    @Override
    protected boolean isFieldAppropriate(SootField field) {
        return field.getType().toString().contains("Set")  && field.getType().toString().contains("java.util");
    }


    @Override
    public void initializeField(SootMethod test, SootField field) {
        Scene.v().forceResolve("java.util.HashSet", SootClass.SIGNATURES);
        this.initializeCollection(test, field,  "<java.util.HashSet: void <init>()>", "java.util.HashSet");
    }


    @Override
    public void wipeField(SootMethod test, Value rValue) {
        clearCollection(test, (Local) rValue, "java.util.Set", "void clear()");
        Local boolLocal = getIsEmpty(test, (Local) rValue, "java.util.Set", "boolean isEmpty()");
        insertWipeFieldAssert(test, boolLocal);
    }

    @Override
    public void insertWipeFieldAssert(SootMethod test, Local boolLocal) {
        insertAssertTrue(test, boolLocal);
    }

    @Override
    public void insertToField(SootMethod test, Local rValue) {
        insertToCollection(test, rValue, "java.util.Set", "boolean add(java.lang.Object)");
        Local boolLocal = getDoesContain(test, rValue, "java.util.Set","boolean contains(java.lang.Object)");
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
        Local boolLocal = getDoesContain(test, rValue, "java.util.Set","boolean contains(java.lang.Object)");
        insertReadFieldAssert(test, boolLocal);
    }

    @Override
    public void insertReadFieldAssert(SootMethod test, Local boolLocal) {
        insertAssertTrue(test, boolLocal);
    }
}
