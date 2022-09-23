package com.mutation.unorderedCollections.index;

import soot.*;
import soot.jimple.*;

import java.util.ArrayList;
import java.util.List;

public class MapUCIMO extends UnorderedCollectionIndexMutationOperator {

    //TODO: change name
    @Override
    public String getCopyMethodSignature() {
        return "java.lang.Object put(java.lang.Object,java.lang.Object)";
    }

    @Override
    public String getCollectionName() {
        return "java.util.Map";
    }

    @Override
    public String getCollectionNameShort() {
        return "Map";
    }

    @Override
    protected List<Value> getArgs(SootMethod test) {
        //TODO: dynamically get type
        List<Value> args = new ArrayList<>();

        SootMethod initMethod = Scene.v().getMethod("<java.lang.Integer: java.lang.Integer valueOf(int)>");

        Local key = Jimple.v().newLocal("key", RefType.v("java.lang.Integer"));
        test.getActiveBody().getLocals().add(key);
        StaticInvokeExpr initializeKeyExpr = Jimple.v().newStaticInvokeExpr(initMethod.makeRef(), IntConstant.v(1));
        AssignStmt initializeKeyStmt = Jimple.v().newAssignStmt(key, initializeKeyExpr);
        this.InsertStmtEnd(test, initializeKeyStmt);

        Local value = Jimple.v().newLocal("value", RefType.v("java.lang.Integer"));
        test.getActiveBody().getLocals().add(value);
        StaticInvokeExpr initializeValueExpr = Jimple.v().newStaticInvokeExpr(initMethod.makeRef(), IntConstant.v(1));
        AssignStmt initializeValueStmt = Jimple.v().newAssignStmt(value, initializeValueExpr);
        this.InsertStmtEnd(test, initializeValueStmt);

        args.add(key);
        args.add(value);

        return args;
    }

    @Override
    protected SootMethod getAddMethod(SootMethod test) {
        SootClass collectionClass = Scene.v().forceResolve(this.getCollectionName(), SootClass.SIGNATURES);
        return collectionClass.getMethod("java.lang.Object put(java.lang.Object,java.lang.Object)");
    }


    @Override
    protected SootMethod getRemoveMethod() {
        SootClass collectionClass = Scene.v().forceResolve(this.getCollectionName(), SootClass.SIGNATURES);
        return collectionClass.getMethod("boolean remove(java.lang.Object,java.lang.Object)");
    }

    @Override
    protected List<Stmt> getAssertStmts(SootMethod test, List<Value> args, Local collectionLocal) {

        List<Stmt> assertStmts = new ArrayList<>();
        Local expected = Jimple.v().newLocal("expected", RefType.v("java.lang.Integer"));
        test.getActiveBody().getLocals().add(expected);

        SootMethod initMethod = Scene.v().getMethod("<java.lang.Integer: java.lang.Integer valueOf(int)>");
        StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(initMethod.makeRef(), IntConstant.v(6));
        AssignStmt initExpected = Jimple.v().newAssignStmt(expected, staticInvokeExpr);
        assertStmts.add(initExpected);

        List<Stmt> mapToListStmts = this.mapToList(test, collectionLocal);

        for (Stmt stmt : mapToListStmts) {
            assertStmts.add(stmt);
        }

        return assertStmts;
    }


}
