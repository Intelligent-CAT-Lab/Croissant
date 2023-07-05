package com.mutation.tooRestrictiveRange;


import com.mutation.SootMutationOperator;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JArrayRef;
import soot.util.Chain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// TODO: Add documentation

public class TRRInjectAssertLocal extends SootMutationOperator {
    SootClass phantomClass;
    SootMethod phantomAssert;
    List<Unit> suitableUnits = new ArrayList<>();


    /**
     * Constructor for VoidLatchAwait Mutation Operator
     *
     * @param inputDir  Where the mutation operator will look for the compiled file
     * @param className Name of the class which will be mutated
     */
    public TRRInjectAssertLocal(String inputDir, String className, String startMethod) {
        super(inputDir, className, startMethod);
    }

    @Override
    public int getMutantNumber() {
        return suitableUnits.size();
    }

    @Override
    public void runOnceBefore() {
        phantomAssert = this.getAssertTrue();
    }

    @Override
    public void runOnceAfter() {

    }

    public TRRInjectAssertLocal() {
        super();
    }

    /**
     * Locates assertion statements that are suitable for mutation
     * @return list of suitable assertion statements
     */
    // TODO byte, short, int, long, float, double
    @Override
    public List<Unit> locateUnits() {
        Chain<Unit> units = getUnits();
        Iterator<Unit> itr = units.snapshotIterator();
        List<Unit> suitableAssertStmts = new ArrayList<>();

        while(itr.hasNext()) {
            Unit currentUnit = itr.next();
            Stmt currentStmt = (Stmt) currentUnit;
            if (currentStmt instanceof AssignStmt) {
                Type StmtType = ((AssignStmt) currentStmt).getLeftOp().getType();
                if (((AssignStmt) currentStmt).getLeftOp() instanceof JArrayRef || ((AssignStmt) currentStmt).getLeftOp() instanceof InstanceFieldRef) {
                    continue;
                }
                if (isTypeAppropriate(StmtType)) {
                    suitableAssertStmts.add(currentUnit);
                    break; // TODO since we'll need to mutate one statement only.
                }
            }
        }
        return suitableAssertStmts;
    }

    protected boolean isTypeAppropriate(Type StmtType) {
        switch(StmtType.toString()) {
            case "boolean":
            case "int":
            case "short":
            case "byte":
            case "long":
            case "double":
            case "char":
            case "float":
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean isApplicable() {
        return locateUnits().size() > 0;
    }

    Value[] compareStmts(AssignStmt currentStmt) {
        Constant constantZero;
        switch (currentStmt.getLeftOp().getType().toString()) {
            case "short":
            case "int":
            case "char":
            case "byte":
            case "boolean":
                constantZero = IntConstant.v(0);
                break;
            case "long":
                constantZero = LongConstant.v(0);
                break;
            case "double":
                constantZero = DoubleConstant.v(0);
                break;
            case "float":
                constantZero = FloatConstant.v(0);
                break;
            default:
                return null;
        }

        return new Value[]{Jimple.v().newGeExpr(currentStmt.getLeftOp(), constantZero),
        Jimple.v().newLeExpr(currentStmt.getLeftOp(), constantZero)};
    }

    /**
     * TODO
     */
    public void mutateMethod() throws Exception {
        this.suitableUnits = locateUnits();
        for (Unit suitableUnit : suitableUnits) {
            AssignStmt currentStmt = (AssignStmt)suitableUnit;

            Local inRange = Jimple.v().newLocal("isInRange", BooleanType.v());
            getJimpleBody().getLocals().addFirst(inRange);

            AssignStmt falseStmt = Jimple.v().newAssignStmt(inRange, IntConstant.v(0));
            AssignStmt trueStmt = Jimple.v().newAssignStmt(inRange, IntConstant.v(1));

            List<Value> args = new ArrayList<>();
            args.add(inRange);

            StaticInvokeExpr assertTrueExpr = Jimple.v().newStaticInvokeExpr(phantomAssert.makeRef(), args);

            Stmt assertTrueStmt = Jimple.v().newInvokeStmt(assertTrueExpr);

            Value[] comparisons = compareStmts(currentStmt);

            if (comparisons == null) throw new Error("Unsupported statement type: "+currentStmt.getLeftOp().getType());

            IfStmt If1 = Jimple.v().newIfStmt(
                comparisons[0],
                falseStmt
            );
            IfStmt If2 = Jimple.v().newIfStmt(
                comparisons[1],
                falseStmt
            );

            getUnits().insertAfter(If1, suitableUnit);
            getUnits().insertBefore(trueStmt, If1);
            getUnits().insertAfter(If2, If1);

            Stmt skipFalse = Jimple.v().newGotoStmt(assertTrueStmt); // TODO goto assertTrue()
            getUnits().insertAfter(skipFalse, If2);

            getUnits().insertAfter(falseStmt, skipFalse);

            getUnits().insertAfter(assertTrueStmt, falseStmt);
        }
        this.addNonDeterminism(this.getCurrentMethod());
    }
}
