package com.mutation.unorderedCollections.orderedStringConversion;

import com.mutation.unorderedCollections.util.StringEditor;
import soot.Unit;
import soot.jimple.Stmt;

import java.util.List;

/**
 * Unordered Collection Mutation Operator for hashsets
 */
public class JsonStringMO extends OrderedStringConversionMutationOperator {
    public JsonStringMO() {
        super("org.json.JSONObject", "org.json.JSONObject");
        this.stringEditor = new StringEditor("{", "}", ":");
    }

    /**
     * Overridden because JSON is not converted into another datatype
     */
    @Override
    public void mutateMethod() {
        List<Unit> SuitableUnits = locateUnits();
        for (Unit unit : SuitableUnits) {
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

            Stmt mutatedAssertion = mutateAssertion(unit, mutatedValue);
            insertMethodAfter(mutatedAssertion, (Stmt) unit);
            break;
        }
        this.addNonDeterminism(this.getCurrentMethod());
    }

    @Override
    public String toString() {
        return "JSON Unordered Collection Mutation Operator";
    }
}
