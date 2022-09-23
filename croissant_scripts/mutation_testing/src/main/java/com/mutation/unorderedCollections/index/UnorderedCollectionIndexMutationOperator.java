package com.mutation.unorderedCollections.index;

import com.mutation.SootMutationOperator;
import soot.*;
import soot.jimple.*;
import soot.util.Chain;

import java.io.IOException;
import java.util.*;

public abstract class UnorderedCollectionIndexMutationOperator extends SootMutationOperator {

    /*
    1. Locate locals
        2. Add a value to the collection
        3. Assert
        4. Remove the value
     */


    protected Map<Local, SootMethod> localSootMethodMap;
    protected SootMethod originalTest;

    public UnorderedCollectionIndexMutationOperator() {
        this.localSootMethodMap = new HashMap<>();
    }

    public SootMethod getCopyMethod() {
        Scene.v().forceResolve(this.getCollectionName(), SootClass.HIERARCHY);
        SootClass collectionClass = Scene.v().getSootClass(this.getCollectionName());
        return collectionClass.getMethod(this.getCopyMethodSignature());
    }

    @Override
    public void exportClass(String outputDir, String methodName) throws IOException {
        for (Map.Entry<Local, SootMethod> entry : this.localSootMethodMap.entrySet()) {
            String testName = methodName + entry.getKey().getName();
            SootMethod test = entry.getValue();
            test.setName(testName);
            this.getCurrentClass().addMethod(test);
        }
        exportClass(outputDir);
    }

    @Override
    public int getMutantNumber() {
        return 1;
    }

    public abstract String getCopyMethodSignature();

    public abstract String getCollectionName();

    public abstract String getCollectionNameShort();

    private Map<Local, SootMethod> locateUnorderedCollectionLocals() {
        Map<Local, SootMethod> unorderedLocals = new HashMap<>();
        for (Local local : this.getCurrentMethod().getActiveBody().getLocals()) {
            if (local.getType().toString().contains(getCollectionNameShort()))  {
                unorderedLocals.put(local, null);
            }
        }
        return unorderedLocals;
    }

    protected Local getEquivalentLocal(Local originalLocal, SootMethod mutantTest) {
        for (Local local : mutantTest.getActiveBody().getLocals()) {
            if (local.getName().equals(originalLocal.getName())) {
                return local;
            }
        }
        return null;
    }

    @Override
    public void mutateMethod() throws Exception {
        Iterator<Local> iterator = this.localSootMethodMap.keySet().iterator();
        while (iterator.hasNext()) {
            Local local = iterator.next();
            SootMethod mutant = mutateMethod(local);
            mutant.addTag(this.createTestAnnotation());
            this.localSootMethodMap.put(local, mutant);
        }
    }

    protected SootMethod mutateMethod(Local local) {


        SootMethod mutantTest = new SootMethod(this.originalTest.getName() + local.getName() + "UCIMO", this.originalTest.getParameterTypes(), this.originalTest.getReturnType(), this.originalTest.getModifiers(), this.originalTest.getExceptions());
        Body mutantBody = (Body) this.originalTest.getActiveBody().clone();
        mutantTest.setActiveBody(mutantBody);

        Local equivLocal = getEquivalentLocal(local, mutantTest);


        try {
            mutantTest.getActiveBody().getLocals().add(equivLocal);
        } catch (Exception e) {
            System.out.println("already contained local");
        }

        List<Value> args = this.getArgs(mutantTest);

        this.addValueToCollection(mutantTest, equivLocal, args);

        List<Stmt> assertStmts = this.getAssertStmts(mutantTest, args, equivLocal);
        for (Stmt assertStmt : assertStmts) {
            this.InsertStmtEnd(mutantTest, assertStmt);
        }

        List<Stmt> removeStmts = this.getRemoveValueStmts(equivLocal, args);

        for (Stmt removeStmt : removeStmts) {
            this.InsertStmtEnd(mutantTest, removeStmt);
        }

        this.addNonDeterminism(mutantTest);

        return mutantTest;
    }

    protected void addValueToCollection(SootMethod mutantTest, Local collectionLocal, List<Value> args) {
        SootMethod addMethod = this.getAddMethod(mutantTest);

        InterfaceInvokeExpr addExpr = Jimple.v().newInterfaceInvokeExpr(collectionLocal, addMethod.makeRef(), args);
        InvokeStmt addStmt = Jimple.v().newInvokeStmt(addExpr);

        this.InsertStmtEnd(mutantTest.getActiveBody(), addStmt);
    }

    protected abstract List<Value> getArgs(SootMethod test);

    protected abstract SootMethod getAddMethod(SootMethod test);

    @Override
    public <T extends Unit> List<T> locateUnits() {
        return null;
    }

    @Override
    public boolean isApplicable() {
        this.originalTest = this.getCurrentMethod();
        this.localSootMethodMap = this.locateUnorderedCollectionLocals();
        return this.localSootMethodMap.values().size() > 0;
    }

    protected abstract List<Stmt> getAssertStmts(SootMethod test, List<Value> args, Local collectionLocal);

    protected abstract SootMethod getRemoveMethod();

    protected List<Stmt> getRemoveValueStmts(Local collectionLocal, List<Value> args) {

        List<Stmt> removeStmts = new ArrayList<>();

        SootMethod removeMethod = this.getRemoveMethod();
        InterfaceInvokeExpr removeExpr = Jimple.v().newInterfaceInvokeExpr(collectionLocal, removeMethod.makeRef(), args);
        InvokeStmt removeStmt = Jimple.v().newInvokeStmt(removeExpr);

        removeStmts.add(removeStmt);

        return removeStmts;
    }

    protected List<Stmt> convertSetToList(SootMethod test, Local setLocal) {
        List<Stmt> convertSetToListStmts = new ArrayList<>();

        Local listLocal = Jimple.v().newLocal("setLocal", RefType.v("java.util.ArrayList"));
        test.getActiveBody().getLocals().add(listLocal);

        NewExpr listNewExpr = Jimple.v().newNewExpr(RefType.v("java.util.ArrayList"));
        AssignStmt listNewStmt = Jimple.v().newAssignStmt(listLocal, listNewExpr);
        convertSetToListStmts.add(listNewStmt);

        SootClass arrayListSootClass = Scene.v().forceResolve("java.util.ArrayList", SootClass.SIGNATURES);
        SootMethod arrayListConstructor = arrayListSootClass.getMethod("void <init>(java.util.Collection)");
        SpecialInvokeExpr constructionExpr = Jimple.v().newSpecialInvokeExpr(listLocal, arrayListConstructor.makeRef(), setLocal);
        InvokeStmt invokeStmt = Jimple.v().newInvokeStmt(constructionExpr);

        convertSetToListStmts.add(invokeStmt);

        return convertSetToListStmts;

    }

    protected List<Stmt> getLastValueOfList(SootMethod test, Local listLocal) {

        List<Stmt> getLastValueStmts = new ArrayList<>();

        SootClass arrayListSootClass = Scene.v().forceResolve("java.util.ArrayList", SootClass.SIGNATURES);
        SootMethod getMethod = arrayListSootClass.getMethod("java.lang.Object get(int)");
        SootMethod sizeMethod = arrayListSootClass.getMethod("int size()");

        Local sizeLocal = Jimple.v().newLocal("sizeLocal", IntType.v());
        test.getActiveBody().getLocals().add(sizeLocal);
        InvokeExpr sizeMethodInvoke = Jimple.v().newVirtualInvokeExpr(listLocal, sizeMethod.makeRef());
        AssignStmt sizeLocalInit = Jimple.v().newAssignStmt(sizeLocal, sizeMethodInvoke);
        getLastValueStmts.add(sizeLocalInit);

        SubExpr updateSizeLocalExpr = Jimple.v().newSubExpr(sizeLocal, IntConstant.v(1));
        AssignStmt updateSizeLocalStmt = Jimple.v().newAssignStmt(sizeLocal, updateSizeLocalExpr);
        getLastValueStmts.add(updateSizeLocalStmt);

        VirtualInvokeExpr getExpr = Jimple.v().newVirtualInvokeExpr(listLocal, getMethod.makeRef(), sizeLocal);
        Local getStmtResult = Jimple.v().newLocal("getStmtResult", RefType.v("java.lang.Object"));
        test.getActiveBody().getLocals().add(getStmtResult);
        AssignStmt initializeGetStmtResult = Jimple.v().newAssignStmt(getStmtResult, getExpr);
        getLastValueStmts.add(initializeGetStmtResult);

        return getLastValueStmts;
    }

    protected List<Stmt> getAssertStmts(SootMethod test, Local getStmtResult) {

        List<Stmt> assertStmts = new ArrayList<>();

        Local expectedLocal = Jimple.v().newLocal("expectedLocal", RefType.v("java.lang.Integer"));
        test.getActiveBody().getLocals().add(expectedLocal);
        SootMethod initMethod = Scene.v().getMethod("<java.lang.Integer: java.lang.Integer valueOf(int)>");
        StaticInvokeExpr initExpr = Jimple.v().newStaticInvokeExpr(initMethod.makeRef(), IntConstant.v(6));
        AssignStmt initStmt = Jimple.v().newAssignStmt(expectedLocal, initExpr);
        assertStmts.add(initStmt);

        Value[] args = new Value[]{
            getStmtResult,
            expectedLocal
        };

        SootMethod assertMethod = this.getAssert();
        StaticInvokeExpr assertExpr = Jimple.v().newStaticInvokeExpr(assertMethod.makeRef(), args);
        Stmt assertStmt = Jimple.v().newInvokeStmt(assertExpr);

        assertStmts.add(assertStmt);

        return assertStmts;
    }

    public List<Stmt> mapToList(SootMethod test, Local collectionLocal) {

        List<Stmt> assertStmts = new ArrayList<>();

        SootClass collectionClass = Scene.v().getSootClass("java.util.Map");
        SootMethod keySetMethod = collectionClass.getMethod("java.util.Set keySet()");
        InterfaceInvokeExpr keySetMethodExpr = Jimple.v().newInterfaceInvokeExpr(collectionLocal, keySetMethod.makeRef());

        Local setLocal = Jimple.v().newLocal("setLocal", RefType.v("java.util.Set"));
        test.getActiveBody().getLocals().add(setLocal);

        AssignStmt initSetLocal = Jimple.v().newAssignStmt(setLocal, keySetMethodExpr);
        assertStmts.add(initSetLocal);

        List<Stmt> convertSetToListStmts = this.convertSetToList(test, setLocal);
        Local listLocal = (Local) ((AssignStmt) convertSetToListStmts.get(0)).getLeftOp();
        for (Stmt stmt : convertSetToListStmts) {
            assertStmts.add(stmt);
        }

        List<Stmt> lastValueStmts = this.getLastValueOfList(test, listLocal);
        for (Stmt stmt : lastValueStmts) {
            assertStmts.add(stmt);
        }

        Local getStmtResult = (Local) ((AssignStmt) lastValueStmts.get(lastValueStmts.size() - 1)).getLeftOp();
        List<Stmt> assertionStmts = getAssertStmts(test, getStmtResult);

        for (Stmt stmt : assertionStmts) {
            assertStmts.add(stmt);
        }
        return assertStmts;
    }

    protected List<Stmt> getNext(Local iteratorLocal, Type type, SootMethod test) {

        //Type should be RefType.v(java.lang.Integer) etc NOT IntType.v())

        List<Stmt> list = new ArrayList<>();


        Scene.v().forceResolve("java.lang.Object", SootClass.SIGNATURES);
        Scene.v().forceResolve("java.util.Iterator", SootClass.SIGNATURES);
        Local objectLocal = Jimple.v().newLocal("objectLocal", RefType.v("java.lang.Object"));
        test.getActiveBody().getLocals().add(objectLocal);

        SootClass iteratorClass = Scene.v().getSootClass("java.util.Iterator");
        SootMethod nextMethod = iteratorClass.getMethodByName("next");
        InterfaceInvokeExpr hasNextExpr = Jimple.v().newInterfaceInvokeExpr(iteratorLocal, nextMethod.makeRef());
        AssignStmt assignStmt = Jimple.v().newAssignStmt(objectLocal, hasNextExpr);
        list.add(assignStmt);

        Local castedLocal = Jimple.v().newLocal("castedLocal", type);
        test.getActiveBody().getLocals().add(castedLocal);
        CastExpr castExpr = Jimple.v().newCastExpr(objectLocal, type);
        AssignStmt castAssign = Jimple.v().newAssignStmt(castedLocal, castExpr);
        list.add(castAssign);

        return list;
    }

}
