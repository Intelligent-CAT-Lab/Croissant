package com.mutation.asyncWait;


import com.mutation.SootMutationOperator;
import soot.*;
import soot.jimple.*;
import soot.util.Chain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// TODO: Add documentation

// TODO: latch.await() -> latch.await(0, TimeUnit.MILLISECONDS);

public class VoidLatchAwaitMO extends SootMutationOperator {

    private List<Unit> suitableUnits = new ArrayList<>();

    /**
     * Constructor for VoidLatchAwait Mutation Operator
     *
     * @param inputDir  Where the mutation operator will look for the compiled file
     * @param className Name of the class which will be mutated
     */
    public VoidLatchAwaitMO(String inputDir, String className, String startMethod) {
        super(inputDir, className, startMethod);
    }

    public VoidLatchAwaitMO() {
        super();
    }

    /**
     * Locates assertion statements that are suitable for mutation
     * @return list of suitable assertion statements
     */
    @Override
    public List<Unit> locateUnits() {
        Chain<Unit> units = getUnits();
        Iterator<Unit> itr = units.snapshotIterator();
        List<Unit> suitableAssertStmts = new ArrayList<>();

        while(itr.hasNext()) {
            Unit currentUnit = itr.next();
            Stmt currentStmt = (Stmt) currentUnit;
            if (currentStmt.containsInvokeExpr() && currentStmt.getInvokeExpr() instanceof VirtualInvokeExpr) {
                VirtualInvokeExpr currentVirtualInvoke = (VirtualInvokeExpr) currentStmt.getInvokeExpr();
                if (currentVirtualInvoke.getMethod().toString().contains("<java.util.concurrent.CountDownLatch: void await()>")) {
                    suitableAssertStmts.add(currentUnit);
                }
            }
        }
        return suitableAssertStmts;
    }

    @Override
    public boolean isApplicable() {
        return locateUnits().size() > 0;
    }

    /**
     * TODO
     */
    public void mutateMethod() throws Exception {
        this.suitableUnits = locateUnits();
        for (Unit suitableUnit : this.suitableUnits) {
            // $r3 = <java.util.concurrent.TimeUnit: java.util.concurrent.TimeUnit MILLISECONDS>;
            /* Declare the local */
            Local millisecondsL = Jimple.v().newLocal("millisecondTimeUnit", RefType.v("java.util.concurrent.TimeUnit"));
            getJimpleBody().getLocals().addFirst(millisecondsL);

            /* Assign the local */
            // TODO: Util class for getMethod with error handling, force resolve, and load.
            Scene.v().forceResolve("java.util.concurrent", SootClass.SIGNATURES);
            Scene.v().forceResolve("java.util.concurrent.TimeUnit", SootClass.SIGNATURES);
            SootMethod millisecondsMethod = Scene.v().getMethod("<java.util.concurrent.TimeUnit: java.util.concurrent.TimeUnit valueOf(java.lang.String)>");
            Expr millisecondsExpr = Jimple.v().newStaticInvokeExpr(millisecondsMethod.makeRef(), StringConstant.v("MILLISECONDS"));
            AssignStmt millisecondsStmt = Jimple.v().newAssignStmt(millisecondsL, millisecondsExpr);
            getUnits().insertBefore(millisecondsStmt, suitableUnit);

            // $z1 = virtualinvoke r1.<java.util.concurrent.CountDownLatch: boolean await(long,java.util.concurrent.TimeUnit)>(100L, $r3);
            Value base = ((VirtualInvokeExpr) ((Stmt) suitableUnit).getInvokeExpr()).getBase();
            SootMethod mutatedLatch = Scene.v().getMethod("<java.util.concurrent.CountDownLatch: boolean await(long,java.util.concurrent.TimeUnit)>");
            VirtualInvokeExpr mutatedExpr = Jimple.v().newVirtualInvokeExpr((Local) base, mutatedLatch.makeRef(), LongConstant.v(0), millisecondsL);
            Stmt mutatedStmt = Jimple.v().newInvokeStmt(mutatedExpr);
            getUnits().insertBefore(mutatedStmt, suitableUnit);
            getUnits().remove(suitableUnit);
        }
        this.addNonDeterminism(this.getCurrentMethod());
    }

    @Override
    public int getMutantNumber() {
        return this.suitableUnits.size();
    }
}
