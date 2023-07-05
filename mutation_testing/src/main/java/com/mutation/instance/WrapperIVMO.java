package com.mutation.instance;

import soot.*;
import soot.jimple.*;

public class WrapperIVMO extends InstanceVariableMutationOperator {

    @Override
    protected String getTestName() {
        return null;
    }

    @Override
    protected boolean isFieldAppropriate(SootField field) {
        switch (field.getType().toString()) {
            case "java.lang.Boolean":
            case "java.lang.Integer":
            case "java.lang.Short":
            case "java.lang.Long":
            case "java.lang.Double":
            case "java.lang.Char":
            case "java.lang.Float":
                //TODO: implement for byte
                return true;
            default:
                return false;
        }
    }

    //TODO: overload this to accomodate Local
    protected void setValue(SootMethod test, SootField rValue, int val) {
        Local identityLocal = prepAndGetThisLocal(test);
        InstanceFieldRef instanceFieldRef = Jimple.v().newInstanceFieldRef(identityLocal, rValue.makeRef());
        AssignStmt assignStmt = null;
        SootMethod initMethod = null;
        Expr initExpr = null;
        Local initExprResult;
        switch (rValue.getType().toString()) {
            case "java.lang.Short":
                initMethod = Scene.v().getMethod("<java.lang.Short: java.lang.Short valueOf(short)>");
                initExpr = Jimple.v().newStaticInvokeExpr(initMethod.makeRef(), IntConstant.v(val));
                initExprResult = Jimple.v().newLocal("initExprResult", RefType.v("java.lang.Short"));
                break;
            case "java.lang.Integer":
                initMethod = Scene.v().getMethod("<java.lang.Integer: java.lang.Integer valueOf(int)>");
                initExpr = Jimple.v().newStaticInvokeExpr(initMethod.makeRef(), IntConstant.v(val));
                initExprResult = Jimple.v().newLocal("initExprResult", RefType.v("java.lang.Integer"));
                break;
            case "java.lang.Char":
                initMethod = Scene.v().getMethod("<java.lang.Character: java.lang.Character valueOf(char)>");
                initExpr = Jimple.v().newStaticInvokeExpr(initMethod.makeRef(), IntConstant.v(val));
                initExprResult = Jimple.v().newLocal("initExprResult", RefType.v("java.lang.Char"));
                break;
            case "java.lang.Boolean":
                initMethod = Scene.v().getMethod("<java.lang.Boolean: java.lang.Boolean valueOf(boolean)>");
                initExpr = Jimple.v().newStaticInvokeExpr(initMethod.makeRef(), IntConstant.v(val));
                initExprResult = Jimple.v().newLocal("initExprResult", RefType.v("java.lang.Boolean"));
                break;
            case "java.lang.Long":
                initMethod = Scene.v().getMethod("<java.lang.Long: java.lang.Long valueOf(long)>");
                initExpr = Jimple.v().newStaticInvokeExpr(initMethod.makeRef(), LongConstant.v(val));
                initExprResult = Jimple.v().newLocal("initExprResult", RefType.v("java.lang.Long"));
                break;
            case "java.lang.Double":
                initMethod = Scene.v().getMethod("<java.lang.Double: java.lang.Double valueOf(double)>");
                initExpr = Jimple.v().newStaticInvokeExpr(initMethod.makeRef(), DoubleConstant.v(val));
                initExprResult = Jimple.v().newLocal("initExprResult", RefType.v("java.lang.Double"));
                break;
            case "java.lang.Float":
                initMethod = Scene.v().getMethod("<java.lang.Float: java.lang.Float valueOf(float)>");
                initExpr = Jimple.v().newStaticInvokeExpr(initMethod.makeRef(), FloatConstant.v(val));
                initExprResult = Jimple.v().newLocal("initExprResult", RefType.v("java.lang.Float"));
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + rValue.getType().toString());
        }

        test.getActiveBody().getLocals().add(initExprResult);
        AssignStmt initExprResultInitialize = Jimple.v().newAssignStmt(initExprResult, initExpr);
        this.InsertStmtEnd(test, initExprResultInitialize);

        assignStmt = Jimple.v().newAssignStmt(instanceFieldRef, initExprResult);
        InsertStmtEnd(test, assignStmt);
    }

    protected void setValue(SootMethod test, Local rValue, int val) {
        AssignStmt assignStmt = null;
        SootMethod initMethod = null;
        Expr initExpr = null;
        switch (rValue.getType().toString()) {
            case "java.lang.Short":
                initMethod = Scene.v().getMethod("<java.lang.Short: java.lang.Short valueOf(short)>");
                initExpr = Jimple.v().newStaticInvokeExpr(initMethod.makeRef(), IntConstant.v(val));
                break;
            case "java.lang.Integer":
                initMethod = Scene.v().getMethod("<java.lang.Integer: java.lang.Integer valueOf(int)>");
                initExpr = Jimple.v().newStaticInvokeExpr(initMethod.makeRef(), IntConstant.v(val));
                break;
            case "java.lang.Char":
                initMethod = Scene.v().getMethod("<java.lang.Character: java.lang.Character valueOf(char)>");
                initExpr = Jimple.v().newStaticInvokeExpr(initMethod.makeRef(), IntConstant.v(val));
                break;
            case "java.lang.Boolean":
                initMethod = Scene.v().getMethod("<java.lang.Boolean: java.lang.Boolean valueOf(boolean)>");
                initExpr = Jimple.v().newStaticInvokeExpr(initMethod.makeRef(), IntConstant.v(val));
                break;
            case "java.lang.Long":
                initMethod = Scene.v().getMethod("<java.lang.Long: java.lang.Long valueOf(long)>");
                initExpr = Jimple.v().newStaticInvokeExpr(initMethod.makeRef(), LongConstant.v(val));
                break;
            case "java.lang.Double":
                initMethod = Scene.v().getMethod("<java.lang.Double: java.lang.Double valueOf(double)>");
                initExpr = Jimple.v().newStaticInvokeExpr(initMethod.makeRef(), DoubleConstant.v(val));
                break;
            case "java.lang.Float":
                initMethod = Scene.v().getMethod("<java.lang.Float: java.lang.Float valueOf(float)>");
                initExpr = Jimple.v().newStaticInvokeExpr(initMethod.makeRef(), FloatConstant.v(val));
                break;
        }

        assignStmt = Jimple.v().newAssignStmt(rValue, initExpr);
        InsertStmtEnd(test, assignStmt);
    }

    @Override
    public void initializeField(SootMethod test, SootField rValue) {
        this.setValue(test, rValue, 1);
    }

    @Override
    public void wipeField(SootMethod test, Value rValue) {

    }

    @Override
    public void insertWipeFieldAssert(SootMethod test, Local boolLocal) {

    }

    @Override
    public void insertToField(SootMethod test, Local rValue) {

    }

    @Override
    public void insertInsertToFieldAssert(SootMethod test, Local rValue, Local boolLocal) {

    }

    public void wipeField(SootMethod test, SootField rValue) {
        this.setValue(test, rValue, 0);
    }

    private void insertAssertEqualWrapper(SootMethod test, FieldRef boolLocal, Integer value) {
        SootMethod assertEqual = getAssert();

        Local expected = Jimple.v().newLocal("expected", RefType.v("java.lang.Integer"));
        SootMethod initMethod = Scene.v().getMethod("<java.lang.Integer: java.lang.Integer valueOf(int)>");
        Expr initExpr = Jimple.v().newStaticInvokeExpr(initMethod.makeRef(), IntConstant.v(1));
        AssignStmt assignStmt = Jimple.v().newAssignStmt(expected, initExpr);
        test.getActiveBody().getLocals().add(expected);
        this.InsertStmtEnd(test, assignStmt);

        Local actual = Jimple.v().newLocal("actual", RefType.v("java.lang.Integer"));
        test.getActiveBody().getLocals().add(actual);
        AssignStmt initActual = Jimple.v().newAssignStmt(actual, boolLocal);
        this.InsertStmtEnd(test, initActual);

        Value[] args;
        if (value == null) {
            args = new Value[]{
                expected,
                actual
            };
        } else {
            args = new Value[]{
                expected,
                actual
            };
        }

        StaticInvokeExpr assertExpr = Jimple.v().newStaticInvokeExpr(assertEqual.makeRef(), args);
        InvokeStmt invokeStmt = Jimple.v().newInvokeStmt(assertExpr);
        InsertStmtEnd(test, invokeStmt);
    }

    public void insertWipeFieldAssert(SootMethod test, SootField boolLocal) {
        Local thisLocal = this.getThisLocal(test.getActiveBody());
        FieldRef instanceFieldRef = Jimple.v().newInstanceFieldRef(thisLocal, boolLocal.makeRef());
        this.insertAssertEqualWrapper(test, instanceFieldRef, 0);
    }

    @Override
    public SootMethod createWipeTest(SootField field) {
        SootMethod test = createTestMethod("testWipeMutant" + field.getName());
        wipeField(test, field);

        return test;
    }


    public void insertToField(SootMethod test, SootField rValue) {
        SootMethod initMethod = Scene.v().getMethod("<java.lang.Integer: java.lang.Integer valueOf(int)>");
        Expr initExpr = Jimple.v().newStaticInvokeExpr(initMethod.makeRef(), IntConstant.v(1));
        Local thisLocal = this.getThisLocal(test.getActiveBody());
        FieldRef fieldRef = Jimple.v().newInstanceFieldRef(thisLocal, rValue.makeRef());
        Local valueHolder = Jimple.v().newLocal("valueHolder", RefType.v("java.lang.Integer"));
        test.getActiveBody().getLocals().add(valueHolder);
        AssignStmt initValueHolder = Jimple.v().newAssignStmt(valueHolder, initExpr);
        this.InsertStmtEnd(test, initValueHolder);
        AssignStmt assignStmt = Jimple.v().newAssignStmt(fieldRef, valueHolder);
        InsertStmtEnd(test, assignStmt);
        this.insertAssertEqualWrapper(test, fieldRef, null);
    }


    @Override
    public SootMethod createAddTest(SootField field) {
        SootMethod test = createTestMethod("testAddMutant" + field.getName());
        insertToField(test, field);
        Local expected = Jimple.v().newLocal("expected", RefType.v("java.lang.Integer"));
        SootMethod initMethod = Scene.v().getMethod("<java.lang.Integer: java.lang.Integer valueOf(int)>");
        Expr initExpr = Jimple.v().newStaticInvokeExpr(initMethod.makeRef(), IntConstant.v(1));
        AssignStmt assignStmt = Jimple.v().newAssignStmt(expected, initExpr);
        test.getActiveBody().getLocals().add(expected);
        this.InsertStmtEnd(test, assignStmt);

        Local actual = Jimple.v().newLocal("actual", RefType.v("java.lang.Integer"));
        test.getActiveBody().getLocals().add(actual);
        Local thisLocal = this.getThisLocal(test.getActiveBody());
        FieldRef fieldRef = Jimple.v().newInstanceFieldRef(thisLocal, field.makeRef());
        AssignStmt initActual = Jimple.v().newAssignStmt(actual, fieldRef);
        this.InsertStmtEnd(test, initActual);

        SootMethod assertMethod = this.getAssert();
        Value[] args = new Value[]{
            actual,
            expected
        };

        StaticInvokeExpr assertExpr = Jimple.v().newStaticInvokeExpr(assertMethod.makeRef(), args);
        InvokeStmt assertStmt = Jimple.v().newInvokeStmt(assertExpr);
        this.InsertStmtEnd(test, assertStmt);

        return test;
    }

    public void insertInsertToFieldAssert(SootMethod test, Local rValue, SootField boolLocal) {
        Local thisLocal = this.getThisLocal(test.getActiveBody());
        FieldRef fieldRef = Jimple.v().newInstanceFieldRef(thisLocal, boolLocal.makeRef());
        this.insertAssertEqualWrapper(test, fieldRef, 2);
    }

    @Override
    public void readField(SootMethod test, Local rValue) {

    }

    @Override
    public void insertReadFieldAssert(SootMethod test, Local boolLocal) {

    }

    public void insertReadFieldAssert(SootMethod test, SootField boolLocal) {
        Local thisLocal = this.getThisLocal(test.getActiveBody());
        FieldRef instanceFieldRef = Jimple.v().newInstanceFieldRef(thisLocal, boolLocal.makeRef());
        this.insertAssertEqualWrapper(test, instanceFieldRef, 1);
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

        Local rValue = Jimple.v().newLocal("rValue", IntType.v());
        test.getActiveBody().getLocals().add(rValue);
        SootMethod intValueMethod = Scene.v().getMethod("<java.lang.Integer: int intValue()>");
        VirtualInvokeExpr intValueExpr = Jimple.v().newVirtualInvokeExpr(local, intValueMethod.makeRef());
        AssignStmt initRValue = Jimple.v().newAssignStmt(rValue, intValueExpr);
        this.InsertStmtEnd(test, initRValue);
        CastExpr castExpr = Jimple.v().newCastExpr(rValue, LongType.v());

        AssignStmt assignStmt = Jimple.v().newAssignStmt(castResultLocal, castExpr);
        this.InsertStmtEnd(test, assignStmt);
        return castResultLocal;
    }

    @Override
    public SootMethod createReadTest(SootField field) {
        SootMethod test = createTestMethod("testReadMutant" + field.getName());

        Local thisLocal = this.getThisLocal(test.getActiveBody());
        FieldRef fieldRef = Jimple.v().newInstanceFieldRef(thisLocal, field.makeRef());
        Local actual = Jimple.v().newLocal("actual", RefType.v("java.lang.Integer"));
        test.getActiveBody().getLocals().add(actual);
        AssignStmt initActual = Jimple.v().newAssignStmt(actual, fieldRef);
        this.InsertStmtEnd(test, initActual);

        Local expected = Jimple.v().newLocal("expected", RefType.v("java.lang.Integer"));
        test.getActiveBody().getLocals().add(expected);
        SootMethod initMethod = Scene.v().getMethod("<java.lang.Integer: java.lang.Integer valueOf(int)>");
        StaticInvokeExpr intValueExpr = Jimple.v().newStaticInvokeExpr(initMethod.makeRef(), IntConstant.v(1));
        AssignStmt initRValue = Jimple.v().newAssignStmt(expected, intValueExpr);
        this.InsertStmtEnd(test, initRValue);

        SootMethod assertMethod = this.getAssert();
        Value[] args = new Value[]{
            expected,
            actual
        };
        StaticInvokeExpr assertEqualExpr = Jimple.v().newStaticInvokeExpr(assertMethod.makeRef(), args);
        InvokeStmt assertEqualStmt = Jimple.v().newInvokeStmt(assertEqualExpr);
        this.InsertStmtEnd(test, assertEqualStmt);

        return test;
    }

}
