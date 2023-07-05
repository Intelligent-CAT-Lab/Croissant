package com.mutation.instance;

import soot.*;
import soot.jimple.*;

public class PrimitiveIVMO extends InstanceVariableMutationOperator{
    String type;
    @Override
    protected String getTestName() {
        return null;
    }

    @Override
    protected boolean isFieldAppropriate(SootField field) {
        switch(field.getType().toString()) {
            case "boolean":
            case "int":
            case "short":
            case "byte":
            case "long":
            case "double":
            case "char":
            case "float":
                return true;
            default:
                return false;
        }
    }

    @Override
    public void initializeField(SootMethod test, SootField rValue) {
        Local identityLocal = prepAndGetThisLocal(test);
        InstanceFieldRef instanceFieldRef = Jimple.v().newInstanceFieldRef(identityLocal, rValue.makeRef());
        AssignStmt assignStmt = null;
        switch (rValue.getType().toString()) {
            case "short":
            case "int":
            case "char":
            case "byte":
            case "boolean":
                assignStmt = Jimple.v().newAssignStmt(instanceFieldRef, IntConstant.v(1));
                break;
            case "long":
                assignStmt = Jimple.v().newAssignStmt(instanceFieldRef, LongConstant.v(1));
                break;
            case "double":
                assignStmt = Jimple.v().newAssignStmt(instanceFieldRef, DoubleConstant.v(1));
                break;
            case "float":
                assignStmt = Jimple.v().newAssignStmt(instanceFieldRef, FloatConstant.v(1));
                break;
        }


        InsertStmtEnd(test, assignStmt);
    }
    @Override
    public SootMethod createWipeTest(SootField field) {
        long startTime = System.currentTimeMillis();

        SootMethod test = createTestMethod("CroissantMutant_OD_IVD_testWipeMutant" + field.getName() + "_NoTemplate");
        Local identityLocal = getIdentityLocal(test);
        InstanceFieldRef instanceFieldRef = Jimple.v().newInstanceFieldRef(identityLocal, field.makeRef());

        AssignStmt assignStmt = null;
        String typeString = field.getType().toString();
        switch (field.getType().toString()) {
            case "short":
            case "int":
            case "char":
            case "byte":
            case "boolean":
                assignStmt = Jimple.v().newAssignStmt(instanceFieldRef, IntConstant.v(0));
                break;
            case "long":
                assignStmt = Jimple.v().newAssignStmt(instanceFieldRef, LongConstant.v(0));
                break;
            case "double":
                assignStmt = Jimple.v().newAssignStmt(instanceFieldRef, DoubleConstant.v(0));
                break;
            case "float":
                assignStmt = Jimple.v().newAssignStmt(instanceFieldRef, FloatConstant.v(0));
                break;
        }


        this.InsertStmtEnd(test, assignStmt);

        Local fieldRvalue = Jimple.v().newLocal("fieldRValue", instanceFieldRef.getType());
        test.getActiveBody().getLocals().addFirst(fieldRvalue);
        AssignStmt assignStmtRValue = Jimple.v().newAssignStmt(fieldRvalue, instanceFieldRef);
        this.InsertStmtEnd(test, assignStmtRValue);

        insertWipeFieldAssert(test, fieldRvalue);
        long endTime= System.currentTimeMillis();
        com.framework.OperatorSelector.MUTANT_TIME.put(test.getName(), endTime - startTime);


        return test;
    }

    @Override
    public SootMethod createReadTest(SootField field) {

        SootMethod test = this.createTestMethod("testReadMutant" + field.getName());
        Local identityLocal = getIdentityLocal(test);
        InstanceFieldRef instanceFieldRef = Jimple.v().newInstanceFieldRef(identityLocal, field.makeRef());
        Local fieldRvalue = Jimple.v().newLocal("fieldRValue", instanceFieldRef.getType());
        test.getActiveBody().getLocals().addFirst(fieldRvalue);
        AssignStmt assignStmtRValue = Jimple.v().newAssignStmt(fieldRvalue, instanceFieldRef);
        this.InsertStmtEnd(test, assignStmtRValue);
        insertReadFieldAssert(test, fieldRvalue);

        return test;
    }

    @Override
    public SootMethod createAddTest(SootField field) {

        //TODO: merge with createWipeTest
        SootMethod test = createTestMethod("testAddMutant" + field.getName());
        Local identityLocal = getIdentityLocal(test);
        InstanceFieldRef instanceFieldRef = Jimple.v().newInstanceFieldRef(identityLocal, field.makeRef());


        AssignStmt assignStmt = null;
        switch (field.getType().toString()) {
            case "short":
            case "int":
            case "char":
            case "byte":
            case "boolean":
                assignStmt = Jimple.v().newAssignStmt(instanceFieldRef, IntConstant.v(0));
                break;
            case "long":
                assignStmt = Jimple.v().newAssignStmt(instanceFieldRef, LongConstant.v(1));
                break;
            case "double":
                assignStmt = Jimple.v().newAssignStmt(instanceFieldRef, DoubleConstant.v(0));
                break;
            case "float":
                assignStmt = Jimple.v().newAssignStmt(instanceFieldRef, FloatConstant.v(0));
                break;
        }
        this.InsertStmtEnd(test, assignStmt);

        Local fieldRvalue = Jimple.v().newLocal("fieldRValue", instanceFieldRef.getType());
        test.getActiveBody().getLocals().addFirst(fieldRvalue);
        AssignStmt assignStmtRValue = Jimple.v().newAssignStmt(fieldRvalue, instanceFieldRef);
        this.InsertStmtEnd(test, assignStmtRValue);

        //TODO: refactor this to use insertAssertAdd
        insertAssertEqualPrimitive(test, fieldRvalue, 2);

        return test;
    }

    @Override
    public void wipeField(SootMethod test, Value rValue) {
        //TODO: refactor, argument should be Value not local so that it also works with field refs and such
        AssignStmt assignStmt = Jimple.v().newAssignStmt(rValue, IntConstant.v(0));
        InsertStmtEnd(test, assignStmt);
        insertWipeFieldAssert(test, (Local) rValue);
    }

    @Override
    public void insertWipeFieldAssert(SootMethod test, Local boolLocal) {
        insertAssertEqualPrimitive(test, boolLocal, 0);
    }

    @Override
    public void insertToField(SootMethod test, Local rValue) {
       AssignStmt assignStmt = Jimple.v().newAssignStmt(rValue, IntConstant.v(2));
       InsertStmtEnd(test, assignStmt);
       insertInsertToFieldAssert(test, rValue, null);
    }

    @Override
    public void insertInsertToFieldAssert(SootMethod test, Local rValue, Local boolLocal) {
        insertAssertEqualPrimitive(test, boolLocal, 2);
    }

    @Override
    public void readField(SootMethod test, Local rValue) {
    }

    @Override
    public void insertReadFieldAssert(SootMethod test, Local boolLocal) {
        insertAssertEqualPrimitive(test, boolLocal, 1);
    }

    private void insertAssertEqualPrimitive(SootMethod test, Local boolLocal, Integer value) {
        SootMethod assertEqual = getAssert(LongType.v());
        Local longLocal = castVariableToLong(test, boolLocal);
        Value[] args = new Value[] {
            longLocal,
            LongConstant.v(value)
        };
        StaticInvokeExpr assertExpr = Jimple.v().newStaticInvokeExpr(assertEqual.makeRef(), args);
        InvokeStmt invokeStmt = Jimple.v().newInvokeStmt(assertExpr);
        InsertStmtEnd(test, invokeStmt);
    }

    private Local castVariableToLong(SootMethod test, Local local) {
        if (local.getType().toString().equals("long")) {
            Local castResultLocal = Jimple.v().newLocal("castResultLocal", LongType.v());
            test.getActiveBody().getLocals().addFirst(castResultLocal);
            AssignStmt assignStmt = Jimple.v().newAssignStmt(castResultLocal, local);
            this.InsertStmtEnd(test, assignStmt);
            return castResultLocal;
        }
        Local castResultLocal = Jimple.v().newLocal("castResultLocal", LongType.v());
        test.getActiveBody().getLocals().addFirst(castResultLocal);
        CastExpr castExpr = Jimple.v().newCastExpr(local, LongType.v());
        AssignStmt assignStmt = Jimple.v().newAssignStmt(castResultLocal, castExpr);
        this.InsertStmtEnd(test, assignStmt);
        return castResultLocal;
    }

}
