//package com.mutation.unorderedCollections.index;
//
//import soot.*;
//import soot.jimple.*;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class SetUCIMO extends UnorderedCollectionIndexMutationOperator {
//    @Override
//    public String getCopyMethodSignature() {
//        return "boolean add(java.lang.Object)";
//    }
//
//    @Override
//    public String getCollectionName() {
//        return "java.util.Set";
//    }
//
//    @Override
//    public String getCollectionNameShort() {
//        return "Set";
//    }
//
//    @Override
//    protected List<Value> getArgs(SootMethod test) {
//        List<Value> args = new ArrayList<>();
//
//        SootMethod initMethod = Scene.v().getMethod("<java.lang.Integer: java.lang.Integer valueOf(int)>");
//
//        Local key = Jimple.v().newLocal("key", RefType.v("java.lang.Integer"));
//        test.getActiveBody().getLocals().add(key);
//        StaticInvokeExpr initializeKeyExpr = Jimple.v().newStaticInvokeExpr(initMethod.makeRef(), IntConstant.v(1));
//        AssignStmt initializeKeyStmt = Jimple.v().newAssignStmt(key, initializeKeyExpr);
//        this.InsertStmtEnd(test, initializeKeyStmt);
//
//        args.add(key);
//        return args;
//    }
//
//    @Override
//    protected SootMethod getAddMethod(SootMethod test) {
//        SootClass collectionClass = Scene.v().forceResolve(this.getCollectionName(), SootClass.SIGNATURES);
//        return collectionClass.getMethod("boolean add(java.lang.Object)");
//    }
//
//    @Override
//    protected List<Stmt> getAssertStmts(SootMethod test, List<Value> args, Local collectionLocal) {
//        List<Stmt> assertStmts = new ArrayList<>();
//        Local expected = Jimple.v().newLocal("expected", RefType.v("java.lang.Integer"));
//        test.getActiveBody().getLocals().add(expected);
//
//        SootMethod initMethod = Scene.v().getMethod("<java.lang.Integer: java.lang.Integer valueOf(int)>");
//        StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(initMethod.makeRef(), IntConstant.v(6));
//        AssignStmt initExpected = Jimple.v().newAssignStmt(expected, staticInvokeExpr);
//        assertStmts.add(initExpected);
//
//        List<Stmt> convertSetToListStmts = this.convertSetToList(test, collectionLocal);
//        Local listLocal = (Local) ((AssignStmt) convertSetToListStmts.get(0)).getLeftOp();
//        for (Stmt stmt : convertSetToListStmts) {
//            assertStmts.add(stmt);
//        }
//        List<Stmt> lastValueStmts = this.getLastValueOfList(test, listLocal);
//        for (Stmt stmt : lastValueStmts) {
//            assertStmts.add(stmt);
//        }
//
//        Local getStmtResult = (Local) ((AssignStmt) lastValueStmts.get(lastValueStmts.size() - 1)).getLeftOp();
//        List<Stmt> assertionStmts = getAssertStmts(test, getStmtResult);
//
//        for (Stmt stmt : assertionStmts) {
//            assertStmts.add(stmt);
//        }
//        return assertStmts;
//    }
//
//    @Override
//    protected SootMethod getRemoveMethod() {
//        SootClass collectionClass = Scene.v().forceResolve(this.getCollectionName(), SootClass.SIGNATURES);
//        return collectionClass.getMethod("boolean remove(java.lang.Object)");
//    }
//}
