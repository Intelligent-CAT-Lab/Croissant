package com.mutation.asyncWait;


import com.mutation.SootMutationOperator;
import soot.Unit;
import soot.jimple.LongConstant;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.util.Chain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// TODO: Add documentation

public class ThreadSleepMO extends SootMutationOperator {
    /**
     * Constructor for ThreadSleep Mutation Operator
     *
     * @param inputDir Where the mutation operator will look for the compiled file
     * @param className Name of the class which will be mutated
     */

    private List<Unit> suitableUnits = new ArrayList<>();

    public ThreadSleepMO(String inputDir, String className, String startMethod) {
        super(inputDir, className, startMethod);
    }

    public ThreadSleepMO() {
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
            if (currentStmt.containsInvokeExpr() && currentStmt.getInvokeExpr() instanceof StaticInvokeExpr) {
                StaticInvokeExpr currentStaticInvoke = (StaticInvokeExpr) currentStmt.getInvokeExpr();
                if (currentStaticInvoke.getMethod().toString().contains("<java.lang.Thread: void sleep(")) {
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
