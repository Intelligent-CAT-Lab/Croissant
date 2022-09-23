package com.mutation.unorderedCollections.assertionInsert;


import com.mutation.SootMutationOperator;
import com.mutation.unorderedCollections.util.StringEditor;
import soot.*;
import soot.jimple.*;
import soot.util.Chain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public abstract class UnorderedAssertionMO extends SootMutationOperator {

    private final List<String> dataStructure;
    protected Local targetLocal;
    protected String addMethod;
    private String dataStructureClass;
    protected StringEditor stringEditor;

    public UnorderedAssertionMO(List<String> dataStructure, String addMethod) {
        super();
        this.addMethod = addMethod;
        this.dataStructure = dataStructure;
    }


    @Override
    public int getMutantNumber() {
        return 1;
    }

    @Override
    public List<SootField> locateUnits() {
        return null;
    }

    @Override
    public boolean isApplicable() {
        return (this.targetLocal != null);
    }

    @Override
    public void setCurrentMethod(String newMethod) {

        targetLocal = null;
        dataStructureClass = null;

        super.setCurrentMethod(newMethod);
        locateUnorderedCollection();
    }

    protected void locateUnorderedCollection() {
        Chain<Local> locals = getJimpleBody().getLocals();
        Iterator<Local> localIterator = locals.snapshotIterator();

        while(localIterator.hasNext()) {
            Local currentLocal = localIterator.next();
                for (String version: this.dataStructure) {

                    if (version == null) {
                        continue;
                    }

                    if (currentLocal.getType().equals(RefType.v(version))) {
                        // if runtime is here, that means this local is an instance of an unordered collection
                        //List<String> contents = findUnorderedCollectionContents(currentLocal);


                        this.targetLocal = currentLocal;
                        this.dataStructureClass = version;
                        return;
                    }
                }
        }
    }




    public abstract List<String> createContents();

    public void mutateMethod() {


        // Create the string representation
        List<String> contents = createContents();
        String stringRepresentation = this.stringEditor.createStringRepresentation(contents);

        // Create the rvalue to hold the result of toString() operation
        Local rValue = Jimple.v().newLocal("rvalue", RefType.v("java.lang.String"));
        getJimpleBody().getLocals().addFirst(rValue);

        // Get the toString() method
        SootClass unorderedClass = Scene.v().getSootClass(this.dataStructureClass);
        SootMethod toStringMethod;

        try {
            toStringMethod = unorderedClass.getMethodByName("toString");
        } catch (Exception e) {
            toStringMethod = new SootMethod("toString",
                null, RefType.v("java.lang.String"), Modifier.PUBLIC );
            Integer x = toStringMethod.getParameterCount();
            unorderedClass.addMethod(toStringMethod);
        }

        for (SootMethod sm: unorderedClass.getMethods()) {
            if (sm.toString().contains("toString")) {
                toStringMethod = sm;
            }
        }



        // Create toString statement

        InstanceInvokeExpr toStringExpr = null;
        if (this.dataStructureClass == "java.util.Map" || this.dataStructureClass == "java.util.Set") {
            // this means it is an interface
            toStringExpr = Jimple.v().newInterfaceInvokeExpr(this.targetLocal, toStringMethod.makeRef());
        }
        else {
            toStringExpr = Jimple.v().newVirtualInvokeExpr(this.targetLocal, toStringMethod.makeRef());
        }
        Stmt toStringStmt = Jimple.v().newAssignStmt(rValue, toStringExpr);
        InsertStmtEnd(toStringStmt);

        // Create assert expression
        SootMethod assertion = getAssert();

        List<Value> args = new ArrayList<>();
        args.add(rValue);
        args.add(StringConstant.v(stringRepresentation));

        StaticInvokeExpr assertExpr = Jimple.v().newStaticInvokeExpr(assertion.makeRef(), args);
        Stmt assertStmt = Jimple.v().newInvokeStmt(assertExpr);


        args.set(1, StringConstant.v(stringRepresentation));

        StaticInvokeExpr shuffledAssertExpr = Jimple.v().newStaticInvokeExpr(assertion.makeRef(), args);

        // Insert assert statement
        InsertStmtEnd(assertStmt);
        this.addNonDeterminism(this.getCurrentMethod());
    }

    protected List<String> findUnorderedCollectionContents(Local unorderedCollection) {
        return findUnorderedCollectionContents(unorderedCollection, "");
    }

    protected List<String> findUnorderedCollectionContents(Local unorderedCollection, String separator) {
        List<String> contents = new ArrayList<>();
        for (Unit unit: getUnits()) {
            Stmt associatedStmt = (Stmt) unit;
            if (associatedStmt.containsInvokeExpr() && associatedStmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
                InstanceInvokeExpr instanceInvokeExpr = (InstanceInvokeExpr) associatedStmt.getInvokeExpr();
                String instanceInvokeExprString = instanceInvokeExpr.toString();
                Boolean check1 = instanceInvokeExprString.contains(this.dataStructureClass);
                Boolean check2 = instanceInvokeExprString.contains(this.addMethod);
                if (check1 && check2) {
                    contents = populateWithRandom(instanceInvokeExpr, separator);
                }
            }
        }
        if (contents.size() == 0) {
            contents = populateWithRandom(separator);
        }
        return contents;
    }


    protected List<String> populateWithRandom(String separator) {
        List<String> contents = new ArrayList<>();
        String alphaNumeric = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvxyz";
        int capacity = ThreadLocalRandom.current().nextInt(1, 10);
        int length = ThreadLocalRandom.current().nextInt(1, 10);
        for (int i = 0; i < capacity; i++) {
            StringBuilder stringBuilder = new StringBuilder(length);
            for (int j = 0; j < length; j++) {
                stringBuilder.append(alphaNumeric.charAt((int) (alphaNumeric.length() * Math.random())));
            }

            stringBuilder.append(separator);

            for (int j = 0; j < length; j++) {
                stringBuilder.append(alphaNumeric.charAt((int) (alphaNumeric.length() * Math.random())));
            }
            contents.add(stringBuilder.toString());
        }
        return contents;
    }

    protected List<String> populateWithRandom(InstanceInvokeExpr instanceInvokeExpr, String separator) {
        List<String> contents = new ArrayList<>();
        int capacity = ThreadLocalRandom.current().nextInt(1, 10);
        if (
            instanceInvokeExpr.getArg(0).getType().toString().contains("Integer") ||
                instanceInvokeExpr.getArg(0).getType().toString().contains("Double") ||
                instanceInvokeExpr.getArg(0).getType().toString().contains("Long") ||
                instanceInvokeExpr.getArg(0).getType().toString().contains("Short")
        ) {
            for (int i = 0; i < capacity; i++) {
                int randomField = (int) ((Math.random() * 1000));
                int randomValue = (int) ((Math.random() * 1000));
                contents.add(randomField + separator + randomValue);
            }

        } else {
            String alphaNumeric = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvxyz";
            int length = ThreadLocalRandom.current().nextInt(1, 10);
            for (int i = 0; i < capacity; i++) {
                StringBuilder stringBuilder = new StringBuilder(capacity);
                for (int j = 0; j < length; j++) {
                    stringBuilder.append(alphaNumeric.charAt((int) (alphaNumeric.length() * Math.random())));
                }

                stringBuilder.append(separator);

                for (int j = 0; j < length; j++) {
                    stringBuilder.append(alphaNumeric.charAt((int) (alphaNumeric.length() * Math.random())));
                }
                contents.add(stringBuilder.toString());
            }
        }
        return contents;
    }

    protected List<String> populateWithRandom(InstanceInvokeExpr instanceInvokeExpr) {
        return populateWithRandom(instanceInvokeExpr, "");
    }
}
