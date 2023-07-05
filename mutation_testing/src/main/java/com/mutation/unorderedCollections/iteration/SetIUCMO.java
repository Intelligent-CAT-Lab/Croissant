package com.mutation.unorderedCollections.iteration;

import soot.Scene;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.jimple.Stmt;

import java.util.ArrayList;
import java.util.List;

public class SetIUCMO extends IterationUnorderedCollectionMutationOperator {


    public SetIUCMO() {
        super();
        this.setLocals = new ArrayList<>();
    }

    @Override
    protected final SootMethod getSizeFunction() {
        SootClass setClass = Scene.v().getSootClass("java.util.Set");
        return setClass.getMethodByName("size");
    }

    @Override
    protected final List<Stmt> getExecStmts() {
        List<Stmt> execStmts = new ArrayList<>();
        return execStmts;
    }


    @Override
    protected SootMethod getIteratorMethod() {
        Scene.v().forceResolve("java.util.Iterator", SootClass.SIGNATURES);
        SootClass iteratorClass = Scene.v().getSootClass("java.util.Set");
        SootMethod iteratorMethod = null;
        try {
            iteratorMethod = iteratorClass.getMethod("java.util.Iterator iterator()");
        } catch (Exception e) {
            iteratorClass = Scene.v().forceResolve("java.util.Set", SootClass.SIGNATURES);
            iteratorMethod = iteratorClass.getMethod("java.util.Iterator iterator()");
        }
        return iteratorMethod;
    }

    @Override
    protected String getCollectionName() {
        return "Set";
    }

    @Override
    public String getAddMethod() {
        return "<java.util.Set: boolean add(java.lang.Object)>";
    }


    @Override
    public List<SootField> locateUnits() {
        return null;
    }


}
