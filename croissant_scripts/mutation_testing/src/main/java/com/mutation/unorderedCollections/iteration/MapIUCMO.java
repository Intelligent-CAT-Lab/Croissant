package com.mutation.unorderedCollections.iteration;

import soot.*;
import soot.jimple.AssignStmt;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.Stmt;
import soot.util.Chain;

import java.util.ArrayList;
import java.util.List;

public class MapIUCMO extends IterationUnorderedCollectionMutationOperator {

    @Override
    protected SootMethod getSizeFunction() {
        SootClass setClass = Scene.v().getSootClass("java.util.Map");
        return setClass.getMethodByName("size");
    }

    @Override
    protected List<Stmt> getExecStmts() {
        List<Stmt> execStmts = new ArrayList<>();
        return execStmts;
    }


    @Override
    protected SootMethod getIteratorMethod() {
        Scene.v().forceResolve("java.util.Collection", SootClass.SIGNATURES);
        SootClass javaUtilCollection = Scene.v().getSootClass("java.util.Collection");
        SootMethod iteratorInvokeMethod = javaUtilCollection.getMethod("java.util.Iterator iterator()");
        return iteratorInvokeMethod;
    }

    @Override
    protected String getCollectionName() {
        return "Map";
    }

    @Override
    protected List<Stmt> createIterator(Local collectionLocal, SootMethod test) {

        Chain<Local> locals = test.getActiveBody().getLocals();
        if (!locals.contains(collectionLocal)) {
            locals.add(collectionLocal);
        }

        List<Stmt> iteratorStmts = new ArrayList<>();

        Local collectionRValue = Jimple.v().newLocal("collectionRValue", RefType.v("java.util.Collection"));
        test.getActiveBody().getLocals().add(collectionRValue);

        String className = "java.util.Map";
        Scene.v().forceResolve(className, SootClass.SIGNATURES);
        SootClass collectionClass = Scene.v().getSootClass(className);
        SootMethod getValues = collectionClass.getMethod("java.util.Collection values()");
        InterfaceInvokeExpr getValueExpr = Jimple.v().newInterfaceInvokeExpr(collectionLocal, getValues.makeRef());
        AssignStmt initializeCollectionRValue = Jimple.v().newAssignStmt(collectionRValue, getValueExpr);
        iteratorStmts.add(initializeCollectionRValue);


        Local iteratorLocal = Jimple.v().newLocal("iteratorLocal", RefType.v("java.util.Iterator"));
        this.getCurrentMethod().getActiveBody().getLocals().add(iteratorLocal);
        SootMethod iteratorInvokeMethod = this.getIteratorMethod();
        InterfaceInvokeExpr iteratorInitExpr = Jimple.v().newInterfaceInvokeExpr(collectionRValue, iteratorInvokeMethod.makeRef());
        AssignStmt iteratorInit = Jimple.v().newAssignStmt(iteratorLocal, iteratorInitExpr);
        iteratorStmts.add(iteratorInit);

        return iteratorStmts;
    }

    @Override
    public String getAddMethod() {
        return "<java.util.Map: java.lang.Object put(java.lang.Object,java.lang.Object)>";
    }


}
