package com.mutation.unorderedCollections.iteration;

import soot.*;
import soot.jimple.AssignStmt;
import soot.jimple.Jimple;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class JsonIUCMO extends IterationUnorderedCollectionMutationOperator {


    @Override
    protected SootMethod getSizeFunction() {
        return null;
    }

    @Override
    protected List<Stmt> getExecStmts() {
        List<Stmt> execStmts = new ArrayList<>();
        return execStmts;
    }

    @Override
    protected SootMethod getIteratorMethod() {
        Scene.v().forceResolve("org.json.JSONObject", SootClass.BODIES);
        SootClass javaUtilCollection = Scene.v().getSootClass("org.json.JSONObject");
        for (SootMethod sm : javaUtilCollection.getMethods()) {
            if (sm.toString().contains("<org.json.JSONObject: java.util.Iterator keys()>")) {
                return javaUtilCollection.getMethod("java.util.Iterator keys()");
            }
        }

        SootMethod phantomMethod = new SootMethod("keys",
            new ArrayList<>(), RefType.v("java.util.Iterator"), Modifier.PUBLIC);
        phantomMethod.setPhantom(true);
        javaUtilCollection.addMethod(phantomMethod);

        return phantomMethod;
    }

    @Override
    protected String getCollectionName() {
        return "org.json.JSONObject";
    }

    @Override
    public void mutateMethod() throws Exception {
        SootMethod pseudoHashMethod = this.createPseudoHashFunction();
        this.getCurrentClass().addMethod(pseudoHashMethod);
        this.tests = new HashMap<>();

        this.originalMethod = this.getCurrentMethod();

        for (int i = 0; i < this.setLocals.size(); i++) {
            this.createWhileLoopTest(i);
        }
    }

    @Override
    protected List<Stmt> createIterator(Local collectionLocal, SootMethod test) {

        List<Stmt> iteratorStmts = new ArrayList<>();



        SootClass iteratorClass = Scene.v().getSootClass("java.util.Iterator");
        Local iteratorLocal = Jimple.v().newLocal("iteratorLocal", iteratorClass.getType());
        test.getActiveBody().getLocals().add(iteratorLocal);

        SootMethod iteratorMethod = getIteratorMethod();
        VirtualInvokeExpr invokeExpr = Jimple.v().newVirtualInvokeExpr(collectionLocal, iteratorMethod.makeRef());
        AssignStmt assignStmt = Jimple.v().newAssignStmt(iteratorLocal, invokeExpr);


        iteratorStmts.add(assignStmt);

        return iteratorStmts;
    }

    @Override
    public String getAddMethod() {
        return null;
    }

    @Override
    public int getMutantNumber() {
        return 1;
    }
}
