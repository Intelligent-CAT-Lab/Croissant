package com.mutation.cachedDataIsNotTearedDown;

import soot.*;
import soot.jimple.*;
import soot.util.Chain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

public class CaffeineCDMO extends CacheDataMutationOperator {
    String key;

    public CaffeineCDMO() {
        super("void put(java.lang.Object,java.lang.Object)");
    }


    protected void findKey() {
        SootMethod test = this.getCurrentMethod();

        Chain<Unit> units = test.getActiveBody().getUnits();
        Iterator<Unit> iterator = units.snapshotIterator();

        while(iterator.hasNext()) {
            Stmt stmt = (Stmt) iterator.next();

            if (!(stmt instanceof InvokeStmt)) {
                continue;
            }

            InvokeStmt invokeStmt = (InvokeStmt) stmt;
            InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();

            if (!(invokeExpr instanceof InterfaceInvokeExpr)) {
                continue;
            }

            InterfaceInvokeExpr interfaceInvokeExpr = (InterfaceInvokeExpr) invokeExpr;

            String methodSignature = interfaceInvokeExpr.getMethod().getSignature();

            if (!methodSignature.contains("void put(java.lang.Object,java.lang.Object)")) {
                continue;
            }

            Local base =  (Local) interfaceInvokeExpr.getBase();

            if (!base.getType().toString().equals("com.github.benmanes.caffeine.cache.Cache")) {
                continue;
            }

            Value key = interfaceInvokeExpr.getArg(0);

            if (key.getType().toString().equals("java.lang.String")) {
                StringConstant stringConstant = (StringConstant) key;
                this.key = stringConstant.value;
            }
        }
    }

    @Override
    protected void addBrittleStatement(SootMethod test) {

        findKey();

        Local key = Jimple.v().newLocal(this.key, RefType.v("java.lang.String"));
        test.getActiveBody().getLocals().add(key);

        AssignStmt initKey = Jimple.v().newAssignStmt(key, StringConstant.v(this.key));
        this.InsertStmtEnd(test, initKey);

        SootMethod getIfPresent = this.createPhantomMethod(
            "com.github.benmanes.caffeine.cache.Cache",
            "java.lang.Object getIfPresent(java.lang.Object)",
            "getIfPresent",
            Collections.singletonList(RefType.v("java.lang.Object")),
            RefType.v("java.lang.Object"),
            false
        );

        //$r3 = r2.<UselessTest: com.github.benmanes.caffeine.cache.Cache cache>;
        Local cacheLocal = Jimple.v().newLocal("cacheLocal", RefType.v("com.github.benmanes.caffeine.cache.Cache"));
        test.getActiveBody().getLocals().add(cacheLocal);

        Local identityLocal = this.getIdentityLocal(test);
        SootField field = this.getCacheFieldRef().getField();
        FieldRef fieldRef = Jimple.v().newStaticFieldRef(field.makeRef());

        AssignStmt initCacheLocal = Jimple.v().newAssignStmt(cacheLocal, fieldRef);
        this.InsertStmtEnd(test, initCacheLocal);

        Local cachedObject = Jimple.v().newLocal("cachedLocal", RefType.v("java.lang.Object"));
        test.getActiveBody().getLocals().add(cachedObject);

        InterfaceInvokeExpr getIfPresentInvoke = Jimple.v().newInterfaceInvokeExpr(cacheLocal, getIfPresent.makeRef(), key);
        AssignStmt initCachedObject = Jimple.v().newAssignStmt(cachedObject, getIfPresentInvoke);
        this.InsertStmtEnd(test, initCachedObject);

        SootMethod assertNullSootMethod = this.getAssertNull(); //fixed
        StaticInvokeExpr assertNullInvoke = Jimple.v().newStaticInvokeExpr(assertNullSootMethod.makeRef(), cachedObject);
        InvokeStmt assertNullStmt = Jimple.v().newInvokeStmt(assertNullInvoke);

        this.InsertStmtEnd(test, assertNullStmt);
    }


    private StaticFieldRef getCacheFieldRef() {
        return getCacheFieldRef(this.getCurrentMethod());
    }


    private StaticFieldRef getCacheFieldRef(SootMethod method) {
        Chain<Unit> units = method.getActiveBody().getUnits();
        Iterator<Unit> unitIterator = units.snapshotIterator();

        while (unitIterator.hasNext()) {
            Stmt stmt = (Stmt) unitIterator.next();

            if (!(stmt instanceof AssignStmt)) {
                continue;
            }

            AssignStmt assignStmt = (AssignStmt) stmt;

            if (!(assignStmt.getLeftOp() instanceof Local)) {
                continue;
            }

            if (!(assignStmt.getRightOp() instanceof StaticFieldRef)) {
                continue;
            }

            return (StaticFieldRef) ((AssignStmt) stmt).getRightOp();
        }
        return null;
    }

    @Override
    public void createstateSetterTests() {
        for (int i=0; i < 50; i++) {
            SootMethod stateSetterTest = this.createstateSetterTest(i);
            this.getCurrentClass().addMethod(stateSetterTest);
        }
    }

    public SootMethod createstateSetterTest(int stateSetterTestIndex) {
        SootMethod stateSetterTest = this.createTestMethod("cleanerMutant" + stateSetterTestIndex + "__CacheDataMutationOperator" );
        /*stateSetterTest.setActiveBody(
            (Body) this.getCurrentMethod()
                .getActiveBody()
                .clone()
        );*/

        StaticFieldRef cacheFieldRef = this.getCacheFieldRef();
        SootMethod invalidateAllMethod = this.createPhantomMethod(
            "com.github.benmanes.caffeine.cache.Cache",
            "com.github.benmanes.caffeine.cache.Cache: void invalidateAll()",
            "invalidateAll",
            new ArrayList<>(),
            VoidType.v(),
            false,
            true
        );


        Local cacheLocal = Jimple.v().newLocal("cacheLocal", RefType.v("com.github.benmanes.caffeine.cache.Cache"));
        stateSetterTest.getActiveBody().getLocals().add(cacheLocal);

        AssignStmt assignStmt = Jimple.v().newAssignStmt(cacheLocal, cacheFieldRef);
        this.InsertStmtEnd(stateSetterTest, assignStmt);


        InterfaceInvokeExpr interfaceInvokeExpr = Jimple.v().newInterfaceInvokeExpr(cacheLocal, invalidateAllMethod.makeRef());
        InvokeStmt invokeStmt = Jimple.v().newInvokeStmt(interfaceInvokeExpr);
        this.InsertStmtEnd(stateSetterTest, invokeStmt);
        /*InterfaceInvokeExpr interfaceInvokeExpr = Jimple.v().newInterfaceInvokeExpr((Local) cacheFieldRef, invalidateAllMethod.makeRef());*/


        return this.makestateSetterTest(stateSetterTest,stateSetterTestIndex);
    }

}
