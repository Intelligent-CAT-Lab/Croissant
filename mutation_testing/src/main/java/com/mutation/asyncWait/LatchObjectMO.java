package com.mutation.asyncWait;


import com.mutation.SootMutationOperator;
import soot.Unit;
import soot.jimple.LongConstant;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;
import soot.util.Chain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// TODO: Add documentation

public class LatchObjectMO extends SootMutationOperator {
    /**
     * Constructor for LatchObject Mutation Operator
     *
     * @param inputDir Where the mutation operator will look for the compiled file
     * @param className Name of the class which will be mutated
     */

    private List<Unit> suitableUnits = new ArrayList<>();

    public LatchObjectMO(String inputDir, String className, String startMethod) {
        super(inputDir, className, startMethod);
    }

    public LatchObjectMO() {
        super();
    }

    /**
     * Locates assertion statements that are suitable for mutation
     * @return list of suitable assertion statements
     */
    @Override
    // TODO: Return latch.await(Long, TimeUnit) directly regardless of stmt type
    public List<Unit> locateUnits() {
        Chain<Unit> units = getUnits();
        Iterator<Unit> itr = units.snapshotIterator();
        List<Unit> suitableAssertStmts = new ArrayList<>();

        while(itr.hasNext()) {
            Unit currentUnit = itr.next();
            Stmt currentStmt = (Stmt) currentUnit;
            if (currentStmt.containsInvokeExpr() && currentStmt.getInvokeExpr() instanceof VirtualInvokeExpr) {
                VirtualInvokeExpr currentVirtualInvoke = (VirtualInvokeExpr) currentStmt.getInvokeExpr();
                if (currentVirtualInvoke.getMethod().toString().contains("<java.util.concurrent.CountDownLatch: boolean await(long,java.util.concurrent.TimeUnit)>")) {
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
            Unit mutatedUnit = (Unit) suitableUnit.clone();
            changeArgument(mutatedUnit, 0, LongConstant.v(0));
            getUnits().insertBefore(mutatedUnit, suitableUnit);
            getUnits().remove(suitableUnit);
        }
        this.addNonDeterminism(this.getCurrentMethod());
    }

    @Override
    public int getMutantNumber() {
        return this.suitableUnits.size();
    }
}
