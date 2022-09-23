package com.mutation.unorderedCollections.orderedStringConversion;

import com.mutation.SootMutationOperator;
import com.mutation.unorderedCollections.util.StringEditor;
import soot.Body;
import soot.Local;
import soot.Unit;
import soot.Value;
import soot.jimple.*;
import soot.util.Chain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Abstract class for mutation operators concerned with unordered collection flakiness
 */
public abstract class OrderedStringConversionMutationOperator extends SootMutationOperator {
    String oldDataStructure;
    String newDataStructure;
    List<Unit> suitableAssertStmts;
    StringEditor stringEditor;

    OrderedStringConversionMutationOperator(String oldDataStructure, String newDataStructure) {
        super();
        this.oldDataStructure = oldDataStructure;
        this.newDataStructure = newDataStructure;
    }

    @Override
    public int getMutantNumber() {
        return 1;
    }


    @Override
    public void setCurrentMethod(String newMethod) {
        super.setCurrentMethod(newMethod);
        locateUnits();
    }

    /**
     * Locates assertion statements that are suitable for mutation
     * @return list of suitable assertion statements
     */
    public List<Unit> locateUnits() {
        Chain<Unit> units = getUnits();
        HashMap<Local, List<Stmt>> localToInvocation = this.localToStmtMap;
        Iterator<Unit> itr = units.snapshotIterator();
        this.suitableAssertStmts = new ArrayList<>();

        while(itr.hasNext()) {
            Unit currentUnit = itr.next();
            Stmt currentStmt = (Stmt) currentUnit;
            if (currentStmt.containsInvokeExpr() && currentStmt.getInvokeExpr() instanceof StaticInvokeExpr) {
                StaticInvokeExpr currentStaticInvoke = (StaticInvokeExpr) currentStmt.getInvokeExpr();
                if (currentStaticInvoke.getMethod().toString().contains("<org.junit.Assert: void assertEquals(java.lang.Object,java.lang.Object)>")) {
                    //Located an assert statement

                    if (!(currentStaticInvoke.getArg(0) instanceof StringConstant || currentStaticInvoke.getArg(0) instanceof Local)) {
                        continue;
                    }
                    if (!(currentStaticInvoke.getArg(1) instanceof Local || currentStaticInvoke.getArg(1) instanceof StringConstant)) {
                        continue;
                    }

                    List<Stmt> statements = null;

                    if (currentStaticInvoke.getArg(1) instanceof Local) {
                        statements = localToInvocation.get((Local) currentStaticInvoke.getArg(1));
                    } else if (currentStaticInvoke.getArg(0) instanceof Local) {
                        statements = localToInvocation.get((Local) currentStaticInvoke.getArg(0));
                    } else {
                        continue;
                    }


                    for (Stmt statement: statements) {
                        if(statement instanceof AssignStmt && ((AssignStmt)statement).getRightOp().toString().contains("<"+oldDataStructure+": java.lang.String toString")) {
                            suitableAssertStmts.add(currentUnit);
                        }
                    }
                }
            }
        }
        return suitableAssertStmts;
    }

    /**
     * Gets hardcoded string from the assertion statement
     * The returned string value is then shuffled and used to create a mutant assertion
     * @param unit Assertion unit
     * @return hardcoded string from the inputted assertion
     * @throws Exception if unit does not contain an invocation statement, it throws an exception
     */
    public String getHardcodedString(Unit unit) throws Exception {
        List<Value> arguments;
        arguments = getArguments(unit);
        if (arguments.get(0) instanceof StringConstant) {
            return arguments.get(0).toString();
        }
        return arguments.get(1).toString();

    }

    /**
     * Creates a new permutation of the hardcoded string argument in the assertion statement
     * @param expectedValue hardcoded string argument in the assertion statement
     * @return shuffled string that is distinct from the inputted string
     */
    public String newPermutation(String expectedValue) {
        List<String> contents = this.stringEditor.getContents(expectedValue);
        this.stringEditor.shuffle(contents);
        return this.stringEditor.createStringRepresentation(contents);
    }

    /**
     * Gets suitable assertions, extracts hardcoded string values from them,
     * create a new permutation of the hardcoded string, use it to construct another assertion,
     * and then insert the assertion
     */
    public void mutateMethod() {
        List<Unit> SuitableUnits = this.suitableAssertStmts;
        for (Unit unit: SuitableUnits){
            String expectedValue;
            try {
                expectedValue = getHardcodedString(unit);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            String mutatedValue;
            try {
                mutatedValue = newPermutation(expectedValue);
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
            //TODO: here
            Local stringRep = (Local) ((Stmt) unit).getInvokeExpr().getArg(1);
            //TODO: find assign statement
            Local concrete = findConvertedInstance(stringRep);
            Local ephemeral = findEphemeral(concrete);
            changeDataStructure(concrete, ephemeral, oldDataStructure, newDataStructure);
            Stmt mutatedAssertion = mutateAssertion(unit, mutatedValue);
            insertMethodAfter(mutatedAssertion,(Stmt)unit);
            break;
        }
        this.addNonDeterminism(this.getCurrentMethod());
    }

    /**
     * Finds the collection instance on which toString() has been invoked
     * @param stringRepresentation Result local of the toString()
     * @return Returns the collection instance on which toString() has been invoked
     */
    public Local findConvertedInstance(Local stringRepresentation) {
        Body body = getJimpleBody();
        Chain<Unit> units = body.getUnits();

        Iterator<Unit> itr = units.snapshotIterator();

        while(itr.hasNext()) {
            Stmt current = (Stmt) itr.next();
            if (current instanceof AssignStmt && ((AssignStmt) current).getLeftOp() == stringRepresentation) {
                VirtualInvokeExpr virtualInvocation = (VirtualInvokeExpr) ((AssignStmt )current).getRightOp();
                return (Local) virtualInvocation.getBase();
            }
        }
        return null;

    }



    /**
     * Collection declaration and initialization requires two locals such as r0 and $r1
     * Variables marked with $ are rvalues
     * This method returns the rvalue associated with the lvalue
     * Finds the variable associated with the collection instance
     * @param concrete the lvalue local object
     * @return rvalue local object
     */
    public Local findEphemeral(Local concrete) {
        Body targetMethodBody = getJimpleBody();
        Chain<Unit> units = targetMethodBody.getUnits();
        Iterator<Unit> itr = units.snapshotIterator();

        while(itr.hasNext()) {
            Stmt stmt = (Stmt) itr.next();
            if (stmt instanceof AssignStmt
                && ((AssignStmt) stmt).getLeftOp() instanceof Local
                && ((AssignStmt) stmt).getRightOp() instanceof Local) {
                if (concrete.equals(((AssignStmt) stmt).getLeftOp())) {
                    return (Local) ((AssignStmt) stmt).getRightOp();
                }

            }
        }
        return null;
    }

    /**
     * Mutates the assertion by copying the unit, changing the argument of the unit and then returning the statement
     * @param assertionUnit assertion unit that is going to be mtuated
     * @param newPermutation new permutation of the hardcoded string
     * @return returns the mutated version of the assertion as a statement
     */
    public Stmt mutateAssertion(Unit assertionUnit, String newPermutation) {

        // copy the unit
        Unit mutatedUnit = (Unit) assertionUnit.clone();

        // change arg of unit
        try {
            changeArgument(mutatedUnit, 0, StringConstant.v(newPermutation));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return (Stmt) mutatedUnit;
    }

    public boolean isApplicable() {
        return (this.suitableAssertStmts.size() > 0);
    }
}
