package com.mutation.tooRestrictiveRange;

import com.mutation.SootMutationOperator;
import soot.*;
import soot.jimple.*;
import soot.util.Chain;

import java.util.LinkedList;
import java.util.List;

// TODO: Add documentation

public class WIPTRRInjectAssertInstance extends SootMutationOperator { // FIXME change to SootTestInjector when fixed
    //public class TRRInjectAssertInstance extends SootTestInjector {
    SootClass phantomClass;
    SootMethod phantomAssert;
    List<SootField> suitableFields;


    /**
     * Constructor for VoidLatchAwait Mutation Operator
     *
     * @param inputDir  Where the mutation operator will look for the compiled file
     * @param className Name of the class which will be mutated
     */
    public WIPTRRInjectAssertInstance(String inputDir, String className, String startMethod) {
        //super(inputDir, className, startMethod);
    }

    @Override
    public int getMutantNumber() {
        return this.suitableFields.size();
    }

    @Override
    public void runOnceBefore() {
        // TODO inject virtualInvoke Assert.assertTrue.
        phantomClass = Scene.v().getSootClass("org.junit.Assert");
        phantomAssert = this.getAssertTrue();
    }

    /**
     * Locates assertion statements that are suitable for mutation
     *
     * @return list of suitable assertion statements
     */
    // TODO byte, short, int, long, float, double
    public void runOnceAfter() {

    }

    public WIPTRRInjectAssertInstance() {
        super();
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
        this.suitableFields = locateUnits();
        return this.suitableFields.size() > 0;
    }

    @Override
    public List<SootField> locateUnits() {
        List<SootField> relevantFields = new LinkedList<>();

        Chain<SootField> fields = getCurrentClass().getFields();
        for (SootField sootField: fields) {
            if (isTypeAppropriate(sootField.getType()) && !sootField.isStatic()) {
                relevantFields.add(sootField); // TODO: Inject a method for each instance var
            }
        }
        return relevantFields;
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

    private Local getThisLocal() {
        for (Local local : this.getCurrentMethod().getActiveBody().getLocals()) {
            if (local.getType().equals(this.getCurrentClass().getType())) {
                return local;
            }
        }
        return null;
    }

    /**
     * TODO
     */
    public void mutateMethod() throws Exception {
        this.suitableFields = locateUnits();
        for (SootField suitableField : this.suitableFields) {
            Local thisLocal = getThisLocal();

            Local instanceCopyL = Jimple.v().newLocal("instanceCopy", suitableField.getType());
            getJimpleBody().getLocals().add(instanceCopyL);

            InstanceFieldRef instanceFieldRef = Jimple.v().newInstanceFieldRef(thisLocal, suitableField.makeRef());
            AssignStmt instanceCopy = Jimple.v().newAssignStmt(instanceCopyL, instanceFieldRef);
            InsertStmtEnd(getCurrentMethod(), instanceCopy);

            /*AssignStmt currentStmt = (AssignStmt)suitableUnit;

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

            getUnits().insertAfter(assertTrueStmt, falseStmt);*/
        }
        this.addNonDeterminism(this.getCurrentMethod());
    }

    protected String getTestName() {
        return "testTRRInjectInstanceMutant";
    }
}
