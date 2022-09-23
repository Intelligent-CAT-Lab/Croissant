package com.mutation.reflection;


import com.mutation.SootMutationOperator;
import soot.*;
import soot.jimple.*;
import soot.util.Chain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class UnorderedPopulationMutationOperator extends SootMutationOperator {
    private String collectionType;
    private String elementType;
    private Local collection;
    private final List<String> apiSignatures;
    private final List<String> knownCollections;

    public UnorderedPopulationMutationOperator() {
        this.apiSignatures = new ArrayList<>(
            Arrays.asList(
                "<java.lang.Class: java.lang.reflect.Method[] getMethods()>"
            )
        );

        this.knownCollections = new ArrayList<>(
            Arrays.asList(
                "Array"
            )
        );

        this.collectionType = null;
        this.collection = null;
    }


    private boolean initialize(AssignStmt assignStmt) {

        Value rightValue = assignStmt.getRightOp();
        if (!(rightValue instanceof InvokeExpr)) {
            return false;
        }
        InvokeExpr invokeExpr = (InvokeExpr) rightValue;

        Value leftValue = assignStmt.getLeftOp();
        if (!(leftValue instanceof Local)) {
            return false;
        }
        Local local = (Local) leftValue;

        for (String apiSignature: this.apiSignatures) {
            if (invokeExpr.getMethod().getSignature().equals(apiSignature)) {
                for (String collection: this.knownCollections) {
                    if (isKnown(collection, local)) {
                        this.collection = local;
                        this.collectionType = getCollectionType(invokeExpr.getMethod().getSignature());
                        this.elementType = getElementType(invokeExpr.getMethod().getSignature());
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isKnown(String collection, Local local) {
        if (collection.equals("Array")) {
            Type type = local.getType();
            return (type instanceof  ArrayType);
        }
        return false;
    }


    private String getElementTypeArray(String signature) {
        Pattern pattern = Pattern.compile("<java.lang.Class: (.*)\\[\\].*>");
        Matcher matcher = pattern.matcher(signature);
        matcher.find();
        return matcher.group(1);
    }

    private String getElementType(String signature) {
        if (this.collectionType.equals("Array")) {
            return getElementTypeArray(signature);
        }
        return null;
    }

    private boolean isArray(String collectionType) {
        Pattern pattern = Pattern.compile("java\\.lang\\..*\\..*\\[\\]");
        Matcher matcher = pattern.matcher(collectionType);
        matcher.find();
        return matcher.matches();
    }

    private String getCollectionType(String methodSignature) {
        Pattern pattern = Pattern.compile("<.*: (.*) .*>");
        Matcher matcher = pattern.matcher(methodSignature);
        matcher.find();
        String collectionType = matcher.group(1);

        if (isArray(collectionType)) {
            return "Array";
        }
        return null;
    }

    private void locateApiCall() {
        Chain<Unit> units = getUnits();
        Iterator<Unit> iterator = units.snapshotIterator();

        while(iterator.hasNext()) {
            Stmt stmt = (Stmt) iterator.next();

            if (!(stmt instanceof AssignStmt)) {
                continue;
            }

            AssignStmt assignStmt = (AssignStmt) stmt;
            if (initialize(assignStmt)) {
                return;
            }
        }
     }



    private void insertAssert(Local local) {
        SootMethod assertion = this.getAssert();
        List<Value> args = new ArrayList<>();
        args.add(local);
        args.add(StringConstant.v("x"));
        StaticInvokeExpr assertExpr = Jimple.v().newStaticInvokeExpr(assertion.makeRef(), args);
        Stmt assertStmt = Jimple.v().newInvokeStmt(assertExpr);
        this.InsertStmtEnd(assertStmt);
    }

    private Local getElementArray() {
        Local element = Jimple.v().newLocal("newLocal", RefType.v(this.elementType));
        ArrayRef jArrayRef = Jimple.v().newArrayRef(this.collection, IntConstant.v(0));
        AssignStmt assignStmt = Jimple.v().newAssignStmt(element, jArrayRef);
        this.getJimpleBody().getLocals().addFirst(element);
        this.InsertStmtEnd(assignStmt);
        return element;
    }

    private Local getElement() {
        if (this.collectionType.equals("Array")) {
            return getElementArray();
        }
        return null;
    }

    @Override
    public void setCurrentMethod(String newMethod) {
        super.setCurrentMethod(newMethod);
        this.collectionType = null;
        this.elementType = null;
        this. collection = null;
        locateApiCall();
    }

    @Override
    public List<SootField> locateUnits() {
        return null;
    }

    @Override
    public boolean isApplicable() {
        return (this.collectionType != null) && (this.collection != null);
    }

    @Override
    public void mutateMethod() throws Exception {
        Local local = getElement();
        insertAssert(local);
        this.addNonDeterminism(this.getCurrentMethod());
    }

    @Override
    public void setCurrentClass(SootClass currentClass) {
        this.collectionType = null;
        this.elementType = null;
        this.collection = null;
        super.setCurrentClass(currentClass);
    }


    @Override
    public int getMutantNumber() {
        return 1;
    }
}
