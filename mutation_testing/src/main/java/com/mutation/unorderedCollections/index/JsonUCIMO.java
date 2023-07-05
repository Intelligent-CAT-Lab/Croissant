//package com.mutation.unorderedCollections.index;
//
//import soot.*;
//import soot.jimple.*;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//
//public class JsonUCIMO extends UnorderedCollectionIndexMutationOperator {
//
//    static SootClass phantomClass = initializePhantomClass();
//
//    private static void addPutMethod(SootClass phantomClass) {
//        ArrayList<Type> parameterTypes = new ArrayList(Arrays.asList(
//            RefType.v("java.lang.String"),
//            RefType.v("java.lang.Object")
//        ));
//        Type returnType = RefType.v("org.json.JSONObject");
//        Integer modifier = Modifier.PUBLIC;
//        List<SootClass> thrownExceptions = new ArrayList<>();
//
//        SootMethod phantomMethod = new SootMethod(
//            "put",
//            parameterTypes,
//            returnType,
//            modifier,
//            thrownExceptions
//        );
//
//        phantomClass.addMethod(phantomMethod);
//    }
//
//    private static void addRemoveMethod(SootClass phantomClass) {
//        ArrayList<Type> parameterTypes = new ArrayList(Arrays.asList(
//            RefType.v("java.lang.String")
//        ));
//        Type returnType = RefType.v("java.lang.Object");
//        Integer modifier = Modifier.PUBLIC;
//        List<SootClass> thrownExceptions = new ArrayList<>();
//
//        SootMethod phantomMethod = new SootMethod(
//            "remove",
//            parameterTypes,
//            returnType,
//            modifier,
//            thrownExceptions
//        );
//
//        phantomClass.addMethod(phantomMethod);
//    }
//
//    private static void addKeysMethod(SootClass phantomClass) {
//        ArrayList<Type> parameterTypes = new ArrayList(Arrays.asList());
//        Type returnType = RefType.v("java.util.Iterator");
//        Integer modifier = Modifier.PUBLIC;
//        List<SootClass> thrownExceptions = new ArrayList<>();
//
//        SootMethod phantomMethod = new SootMethod(
//            "keys",
//            parameterTypes,
//            returnType,
//            modifier,
//            thrownExceptions
//        );
//
//        phantomClass.addMethod(phantomMethod);
//    }
//
//    private static SootClass initializePhantomClass() {
//        SootClass phantomClass = Scene.v().makeSootClass("org.json.JSONObject");
//        Scene.v().addClass(phantomClass);
//        addKeysMethod(phantomClass);
//        addRemoveMethod(phantomClass);
//        addPutMethod(phantomClass);
//        return phantomClass;
//    }
//
//    public void getSootClass() {
//        SootClass phantomClass = Scene.v().getSootClass("org.json.JSONObject");
//
//
//        //TODO: add other methods
//    }
//
//    @Override
//    public String getCopyMethodSignature() {
//        SootClass phantomClass = Scene.v().getSootClass("org.json.JSONObject");
//        return "org.json.JSONObject put(java.lang.String, java.lang.Object)";
//    }
//
//    @Override
//    public String getCollectionName() {
//        return "org.json.JSONObject";
//    }
//
//    @Override
//    public String getCollectionNameShort() {
//        return "JSONObject";
//    }
//
//    @Override
//    protected List<Value> getArgs(SootMethod test) {
//        //TODO: dynamically get type
//        List<Value> args = new ArrayList<>();
//
//        SootMethod initMethod = Scene.v().getMethod("<java.lang.Integer: java.lang.Integer valueOf(int)>");
//
//        Local key = Jimple.v().newLocal("key", RefType.v("java.lang.String"));
//        test.getActiveBody().getLocals().add(key);
//        AssignStmt initializeKeyStmt = Jimple.v().newAssignStmt(key, StringConstant.v("a"));
//        this.InsertStmtEnd(test, initializeKeyStmt);
//
//        Local value = Jimple.v().newLocal("value", RefType.v("java.lang.Integer"));
//        test.getActiveBody().getLocals().add(value);
//        StaticInvokeExpr initializeValueExpr = Jimple.v().newStaticInvokeExpr(initMethod.makeRef(), IntConstant.v(1));
//        AssignStmt initializeValueStmt = Jimple.v().newAssignStmt(value, initializeValueExpr);
//        this.InsertStmtEnd(test, initializeValueStmt);
//
//        args.add(key);
//        args.add(value);
//
//        return args;
//    }
//
//    @Override
//    protected SootMethod getAddMethod(SootMethod test) {
//        return phantomClass.getMethodByName("put");
//    }
//
//    private SootMethod getKeysMethod() {
//        return phantomClass.getMethodByName("keys");
//    }
//
//
//    @Override
//    protected List<Stmt> getAssertStmts(SootMethod test, List<Value> args, Local collectionLocal) {
//
//        List<Stmt> assertStmts = new ArrayList<>();
//
//        Local iteratorLocal = Jimple.v().newLocal("iteratorLocal", RefType.v("java.util.Iterator"));
//        test.getActiveBody().getLocals().add(iteratorLocal);
//
//        SootMethod keysMethod = this.getKeysMethod();
//        VirtualInvokeExpr getElement = Jimple.v().newVirtualInvokeExpr(collectionLocal, keysMethod.makeRef());
//        AssignStmt initIterator = Jimple.v().newAssignStmt(iteratorLocal, getElement);
//        assertStmts.add(initIterator);
//
//        List<Stmt> nextStmts = this.getNext(iteratorLocal, RefType.v("java.lang.Object"), test);
//
//        for (Stmt nextStmt : nextStmts) {
//            assertStmts.add(nextStmt);
//        }
//
//        Local expectedLocal = Jimple.v().newLocal("expected", RefType.v("java.lang.Integer"));
//        test.getActiveBody().getLocals().add(expectedLocal);
//        SootMethod initMethod = Scene.v().getMethod("<java.lang.Integer: java.lang.Integer valueOf(int)>");
//        StaticInvokeExpr initExpr = Jimple.v().newStaticInvokeExpr(initMethod.makeRef(), IntConstant.v(6));
//        AssignStmt initStmt = Jimple.v().newAssignStmt(expectedLocal, initExpr);
//        assertStmts.add(initStmt);
//
//        SootMethod assertEqual = this.getAssert();
//        Value[] assertArgs = new Value[]{
//            ((AssignStmt) nextStmts.get(nextStmts.size() - 1)).getLeftOp(),
//            expectedLocal
//        };
//        StaticInvokeExpr assertExpr = Jimple.v().newStaticInvokeExpr(assertEqual.makeRef(), assertArgs);
//        InvokeStmt assertStmt = Jimple.v().newInvokeStmt(assertExpr);
//        assertStmts.add(assertStmt);
//
//        return assertStmts;
//    }
//
//    @Override
//    protected SootMethod getRemoveMethod() {
//        return phantomClass.getMethodByName("remove");
//    }
//
//    @Override
//    protected void addValueToCollection(SootMethod mutantTest, Local collectionLocal, List<Value> args) {
//        SootMethod addMethod = this.getAddMethod(mutantTest);
//
//        VirtualInvokeExpr addExpr = Jimple.v().newVirtualInvokeExpr(collectionLocal, addMethod.makeRef(), args);
//        InvokeStmt addStmt = Jimple.v().newInvokeStmt(addExpr);
//
//        this.InsertStmtEnd(mutantTest.getActiveBody(), addStmt);
//    }
//
//    @Override
//    protected List<Stmt> getRemoveValueStmts(Local collectionLocal, List<Value> args) {
//
//        List<Stmt> removeStmts = new ArrayList<>();
//
//        SootMethod removeMethod = this.getRemoveMethod();
//        VirtualInvokeExpr removeExpr = Jimple.v().newVirtualInvokeExpr(collectionLocal, removeMethod.makeRef(), args.get(0));
//        InvokeStmt removeStmt = Jimple.v().newInvokeStmt(removeExpr);
//
//        removeStmts.add(removeStmt);
//
//        return removeStmts;
//    }
//}
