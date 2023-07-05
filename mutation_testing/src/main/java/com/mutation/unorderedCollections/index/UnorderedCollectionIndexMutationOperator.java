package com.mutation.unorderedCollections.index;

import com.mutation.SootMutationOperator;
import soot.*;
import soot.jimple.*;
import soot.util.Chain;
import com.mutation.SootMutationOperator;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JInterfaceInvokeExpr;
import soot.util.Chain;
import soot.util.HashMultiMap;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.IOException;
import java.util.*;

public class UnorderedCollectionIndexMutationOperator extends SootMutationOperator {
    private String collectionType;
    private String elementType;
    private Local collection;
    private Local elementToInsert;
    private Value valueToInsert;
    private String insertSig;
    private Value invokeOp;
    private final List<String> apiSignatures;
    private final List<String> knownCollections;

    public UnorderedCollectionIndexMutationOperator() {
        this.apiSignatures = new ArrayList<>(
            Arrays.asList(
                "<java.lang.Class: java.lang.reflect.Method[] getMethods()>"));
        this.insertSig = "<java.lang.Class: java.lang.reflect.Method[] getName()>";

        this.knownCollections = new ArrayList<>(
            Arrays.asList(
                "Array"));

        this.collectionType = null;
        this.collection = null;
        this.invokeOp = null;
    }

    private boolean initialize(AssignStmt assignStmt) {

        Value rightValue = assignStmt.getRightOp();
        if (!(rightValue instanceof InvokeExpr)) {
            return false;
        }
        InvokeExpr invokeExpr = (InvokeExpr) rightValue;
        this.invokeOp = rightValue;
        // System.out.println(invokeExpr.getMethod().getSignature());

        Value leftValue = assignStmt.getLeftOp();
        if (!(leftValue instanceof Local)) {
            return false;
        }
        Local local = (Local) leftValue;

        for (String apiSignature : this.apiSignatures) {
            if (invokeExpr.getMethod().getSignature().equals(apiSignature)) {
                for (String collection : this.knownCollections) {
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
            return (type instanceof ArrayType);
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

//    private boolean locateApiCall() {
//        Chain<Unit> units = getUnits();
//        Iterator<Unit> iterator = units.snapshotIterator();
//
//        while (iterator.hasNext()) {
//            Stmt stmt = (Stmt) iterator.next();
//            if ((stmt instanceof InvokeStmt)) {
//                System.out.println(stmt.getInvokeExpr().getMethod().getSignature());
//                if (stmt.getInvokeExpr().getMethod().getSignature()
//                    .equals("<java.util.AbstractCollection: java.lang.String toString()>")) {
//
//                    // InvokeExpr in = stmt.getInvokeExpr();
//                    // Local ll = Jimple.v().newLocal("methodNameLocalll",
//                    // RefType.v("java.lang.String"));
//                    // AssignStmt asg = Jimple.v().newAssignStmt(ll, in);
//                    // this.getJimpleBody().getLocals().addFirst(ll);
//                    for (Local temp : this.getJimpleBody().getLocals()) {
//                        // System.out.println("============="+temp.getType().toString());
//                        // System.out.println("============="+temp.getType().toString());
//                        if (temp.getType().toString().equals("java.util.HashSet")) {
//                            this.collection = temp;
//                            this.collectionType = "Array";
//                            // this.elementType =
//                            // getElementType(stmt.getInvokeExpr().getMethod().getSignature());
//                            System.out.println(this.collectionType);
//                            return true;
//                        }
//                    }
//                }
//
//            }
//
//            // if (!(stmt instanceof AssignStmt)) {
//            // continue;
//            // }
//            //
//            // AssignStmt assignStmt = (AssignStmt) stmt;
//            // if (initialize(assignStmt)) {
//            // return;
//            // }
//        }
//        return false;
//    }

     private void locateApiCall() {
     Chain<Unit> units = getUnits();
     Iterator<Unit> iterator = units.snapshotIterator();

     while(iterator.hasNext()) {
     Stmt stmt = (Stmt) iterator.next();
     // if ((stmt instanceof InvokeStmt))
     //System.out.println(stmt.getInvokeExpr().getMethod().getSignature());

     if (!(stmt instanceof AssignStmt)) {
     continue;
     }

     AssignStmt assignStmt = (AssignStmt) stmt;
     if (initialize(assignStmt)) {
     return;
     }
     }
     }

    private Stmt insertAssert(Local local, Local local2) {
        SootMethod assertion = this.getAssert();
        List<Value> args = new ArrayList<>();
        // SootMethodRef smGetName = Scene.v().getMethod("<java.lang.Class:
        // java.lang.reflect.Method[] getMethods()>").makeRef();
        // VirtualInvokeExpr invoke = soot.jimple.Jimple.v().newVirtualInvokeExpr(local,
        // smGetName);
        // Stmt nameStmt = Jimple.v().newInvokeStmt(invoke);
        // this.InsertStmtEnd(nameStmt);
        // Local element = Jimple.v().newLocal("methodNameLocal",
        // RefType.v(this.elementType));
        // AssignStmt assignStmt = Jimple.v().newAssignStmt(element, invoke);
        // this.getJimpleBody().getLocals().addFirst(element);
        // this.InsertStmtEnd(assignStmt);
        // args.add(element);
        // args.add(local);
        // args.add(StringConstant.v("public java.lang.Object
        // java.util.HashMap.remove(java.lang.Object)"));
        args.add(local);
        args.add(local2);

        StaticInvokeExpr assertExpr = Jimple.v().newStaticInvokeExpr(assertion.makeRef(), args);
        Stmt assertStmt = Jimple.v().newInvokeStmt(assertExpr);
        this.InsertStmtEnd(assertStmt);
        return assertStmt;
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
        this.collection = null;
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
        // insertAssert(local);

        this.addFlake(this.getCurrentMethod(), local);
        // this.controlflake(this.getCurrentMethod(),local);
        this.addNonDeterminism(this.getCurrentMethod());
    }

    public void addFlake(SootMethod method, Local local) {
        Local getMethodsAgain = Jimple.v().newLocal("getAgainMethods", this.collection.getType());
        AssignStmt assignAgain = Jimple.v().newAssignStmt(getMethodsAgain, this.invokeOp);
        this.getJimpleBody().getLocals().addFirst(getMethodsAgain);
        this.InsertStmtEnd(assignAgain);

        Local OriginalList = createLists(this.collection, 10, "OriginalList");
        Local NewList = createLists(getMethodsAgain, 10, "NewList");

        Local OriginalElement = Jimple.v().newLocal("OriginalElement", RefType.v(this.elementType));
        ArrayRef jArrayRefOriginal = Jimple.v().newArrayRef(OriginalList, IntConstant.v(0));
        AssignStmt OriginalAssignStmt = Jimple.v().newAssignStmt(OriginalElement, jArrayRefOriginal);
        this.getJimpleBody().getLocals().addFirst(OriginalElement);
        this.InsertStmtEnd(OriginalAssignStmt);

        Local NewElement = Jimple.v().newLocal("NewElement", RefType.v(this.elementType));
        ArrayRef jArrayRefNew = Jimple.v().newArrayRef(NewList, IntConstant.v(0));
        AssignStmt NewAssignStmt = Jimple.v().newAssignStmt(NewElement, jArrayRefNew);
        this.getJimpleBody().getLocals().addFirst(NewElement);
        this.InsertStmtEnd(NewAssignStmt);

        Stmt asstStmt = insertAssert(OriginalElement, NewElement);
    }
    // Local OriginalHashSet = createHashSet(OriginalList, 10, "OriginalHashSet");
    // Local NewHashSet = createHashSet(OriginalList, 10, "NewHashSet");
    // Local FirstElementFromOriginalHashSet =
    // getFirstElementFromHashSet(OriginalHashSet, 10,
    // "FirstElementFromOriginalHashSet");
    // Local FirstElementFromNewHashSet = getFirstElementFromHashSet(NewHashSet, 10,
    // "FirstElementFromNewHashSet");
    // Stmt asstStmt = insertAssert(FirstElementFromOriginalHashSet,
    // FirstElementFromNewHashSet);

    // private SootClass createClass(Integer size, String generalName, Local
    // methodList){
    // SootClass newClass = new SootClass(generalName+size.toString(),
    // Modifier.PUBLIC);
    // newClass.setSuperclass(Scene.v().getSootClass("java.lang.Class"));
    // Scene.v().addClass(newClass);
    // for (Integer i = 1; i <= size; i++){
    // Value index = IntConstant.v(i);
    // Local temp =
    // Jimple.v().newLocal("temp"+i.toString(),RefType.v(this.elementType));
    // this.getJimpleBody().getLocals().addFirst(temp);
    // Value rightSide = Jimple.v().newArrayRef(methodList, IntConstant.v(i));
    // AssignStmt asignStmt1 = Jimple.v().newAssignStmt(temp, rightSide);
    // this.InsertStmtEnd(asignStmt1);
    //
    // SootMethod method = new SootMethod(temp.getName(),
    // Arrays.asList(new Type[] {ArrayType.v(RefType.v("java.lang.String"), 1)}),
    // VoidType.v(), Modifier.PUBLIC | Modifier.STATIC);
    // newClass.addMethod(method);
    // }
    // return newClass;
    // }
    // private Local getFirstMethod(SootClass Class){
    // SootMethodRef smGetName = Scene.v().getMethod("<java.lang.Class:
    // java.lang.reflect.Method[] getDeclaredMethods()>").makeRef();
    // Local newArrayLocal =
    // Jimple.v().newLocal("Cla",RefType.v(Class.getType().getSootClass()));
    // this.getJimpleBody().getLocals().addFirst(newArrayLocal);
    // VirtualInvokeExpr invoke =
    // soot.jimple.Jimple.v().newVirtualInvokeExpr(newArrayLocal,smGetName);
    // Local element = Jimple.v().newLocal("methodNameLocal",
    // RefType.v(this.elementType));
    // AssignStmt assignStmt = Jimple.v().newAssignStmt(element, invoke);
    // this.getJimpleBody().getLocals().addFirst(element);
    // this.InsertStmtEnd(assignStmt);
    // return element;
    //
    // }
    private Local createHashSet(Local fromLocal, Integer size, String generalName) {
        NewExpr hashsetExpr = Jimple.v().newNewExpr(RefType.v("java.util.HashSet"));
        Local hashset = Jimple.v().newLocal(generalName + size.toString(), RefType.v("<java.util.HashSet"));
        AssignStmt assignStmt = Jimple.v().newAssignStmt(hashset, hashsetExpr);
        this.getJimpleBody().getLocals().addFirst(hashset);
        this.InsertStmtEnd(assignStmt);

        for (int i = 0; i < size; i++) {
            Value index = IntConstant.v(i);
            Local temp = Jimple.v().newLocal("temp", RefType.v(this.elementType));
            this.getJimpleBody().getLocals().addFirst(temp);
            Value rightSide = Jimple.v().newArrayRef(fromLocal, IntConstant.v(i));
            AssignStmt asignStmt1 = Jimple.v().newAssignStmt(temp, rightSide);
            this.InsertStmtEnd(asignStmt1);

            SootMethodRef ref = Scene.v().getMethod("<java.util.HashSet: boolean add(java.lang.Object)>").makeRef();
            VirtualInvokeExpr invoke = soot.jimple.Jimple.v().newVirtualInvokeExpr(hashset, ref, temp);
            Local element = Jimple.v().newLocal("methodNameLocal", RefType.v(this.elementType));
            AssignStmt assignStmt2 = Jimple.v().newAssignStmt(element, invoke);
            this.getJimpleBody().getLocals().addFirst(element);
            this.InsertStmtEnd(assignStmt2);
        }
        return hashset;
    }

    private Local getFirstElementFromHashSet(Local HashSet, Integer size, String generalName) {

        // <java.util.Iterator: java.lang.Object next()>
        // <java.util.AbstractCollection: java.lang.String toString()>
        // <java.util.HashSet: java.util.Iterator iterator()>
        SootMethodRef ref = Scene.v().getMethod("<java.util.HashSet: java.util.Iterator iterator()>").makeRef();
        VirtualInvokeExpr invoke = soot.jimple.Jimple.v().newVirtualInvokeExpr(HashSet, ref);
        Local element = Jimple.v().newLocal(generalName + size.toString(), RefType.v(this.elementType));
        AssignStmt assignStmt = Jimple.v().newAssignStmt(element, invoke);
        this.getJimpleBody().getLocals().addFirst(element);
        this.InsertStmtEnd(assignStmt);

        SootMethodRef iterRef = Scene.v().getMethod("<java.util.Iterator: java.lang.Object next()>").makeRef();
        InterfaceInvokeExpr iterInvoke = soot.jimple.Jimple.v().newInterfaceInvokeExpr(element, iterRef);
        Local iterElement = Jimple.v().newLocal(generalName + "iter" + size.toString(), RefType.v(this.elementType));
        AssignStmt assignIterStmt = Jimple.v().newAssignStmt(iterElement, iterInvoke);
        this.getJimpleBody().getLocals().addFirst(iterElement);
        this.InsertStmtEnd(assignIterStmt);

        return iterElement;
    }

    private Local createLists(Local fromLocal, Integer size, String generalName) {

        NewArrayExpr arrayExpr = Jimple.v().newNewArrayExpr(RefType.v(this.elementType), IntConstant.v(size));
        Local newArrayLocal = Jimple.v().newLocal(generalName + size.toString(),
            ArrayType.v(RefType.v(this.elementType), size));
        AssignStmt assignStmt = Jimple.v().newAssignStmt(newArrayLocal, arrayExpr);
        this.getJimpleBody().getLocals().addFirst(newArrayLocal);
        this.InsertStmtEnd(assignStmt);

        for (int i = 0; i < size; i++) {
            Value index = IntConstant.v(i);
            Local temp = Jimple.v().newLocal("temp", RefType.v(this.elementType));
            this.getJimpleBody().getLocals().addFirst(temp);
            Value rightSide = Jimple.v().newArrayRef(fromLocal, IntConstant.v(i));
            AssignStmt asignStmt1 = Jimple.v().newAssignStmt(temp, rightSide);
            this.InsertStmtEnd(asignStmt1);
            ArrayRef leftSide = Jimple.v().newArrayRef(newArrayLocal, index);
            AssignStmt asignStmt2 = Jimple.v().newAssignStmt(leftSide, temp);
            this.InsertStmtEnd(asignStmt2);
        }
        return newArrayLocal;
    }

    private Local getFirstElement(Local List, Integer size, String generalName) {
        Local element = Jimple.v().newLocal(generalName + size.toString(), RefType.v(this.elementType));
        ArrayRef jArrayRef = Jimple.v().newArrayRef(List, IntConstant.v(0));
        AssignStmt assignStmt = Jimple.v().newAssignStmt(element, jArrayRef);
        this.getJimpleBody().getLocals().addFirst(element);
        this.InsertStmtEnd(assignStmt);
        return element;
    }

    public void controlflake(SootMethod method, Local local) {
        SootMethod getThresholdMethod = this.createGetThresholdMethod();
        try {
            this.getCurrentClass().addMethod(getThresholdMethod);
        } catch (Exception e) {
            getThresholdMethod = this.getCurrentClass().getMethod(getThresholdMethod.getSubSignature());
        }
        Local thisLocal = this.getOrCreateThisLocal(method);
        Local thresholdLocal = Jimple.v().newLocal("threshold", DoubleType.v());
        method.getActiveBody().getLocals().add(thresholdLocal);
        VirtualInvokeExpr getThresholdInvoke = Jimple.v().newVirtualInvokeExpr(thisLocal, getThresholdMethod.makeRef());
        AssignStmt initializeThresholdLocal = Jimple.v().newAssignStmt(thresholdLocal, getThresholdInvoke);
        this.InsertStmtBeginning(method, initializeThresholdLocal);

        // get methods again, to compare getMethodsAgain with this.collection
        Local getMethodsAgain = Jimple.v().newLocal("getAgainMethods", this.collection.getType());
        AssignStmt asAgain = Jimple.v().newAssignStmt(getMethodsAgain, this.invokeOp);
        this.getJimpleBody().getLocals().addFirst(getMethodsAgain);
        this.InsertStmtEnd(asAgain);

        for (Integer i = 1; i <= 10; i++) {
            Local OriginalList = createLists(this.collection, i, "OriginalList");
            Local OriginalHashSet = createHashSet(OriginalList, i, "OriginalHashSet");
            Local NewHashSet = createHashSet(OriginalList, i, "NewHashSet");
            Local FirstElementFromOriginalHashSet = getFirstElementFromHashSet(OriginalHashSet, i,
                "FirstElementFromOriginalHashSet");
            Local FirstElementFromNewHashSet = getFirstElementFromHashSet(NewHashSet, i, "FirstElementFromNewHashSet");
            Stmt asstStmt = insertAssert(FirstElementFromOriginalHashSet, FirstElementFromNewHashSet);

            // GeExpr geExpr = Jimple.v().newGeExpr(DoubleConstant.v(0),thresholdLocal);
            // NopStmt nop = Jimple.v().newNopStmt();
            // this.InsertStmtEnd(nop);
            // IfStmt ifStmt = Jimple.v().newIfStmt(geExpr, nop);
            // method.getActiveBody().getUnits().insertBefore(ifStmt, asstStmt10);

            // Local NewList = createLists(getMethodsAgain,i,"NewList");
            // Local firstElementFromOriginalList =
            // getFirstElement(OriginalList,i,"firstElementFromOriginalList");
            // Local firstElementFromNewList=
            // getFirstElement(NewList,i,"firstElementFromOriginalList");
        }

        // Local OriginalList10 = createLists(this.collection,10,"OriginalList");
        // Local NewList10 = createLists(getMethodsAgain,10,"NewList");

        // Local firstElementFromOriginalList10 =
        // getFirstElement(OriginalList10,10,"firstElementFromOriginalList");
        // Local firstElementFromNewList10=
        // getFirstElement(NewList10,10,"firstElementFromOriginalList");

        // Stmt asstStmt10 =
        // insertAssert(firstElementFromOriginalList10,firstElementFromNewList10);

        // GeExpr geExpr = Jimple.v().newGeExpr(DoubleConstant.v(0),thresholdLocal);
        // NopStmt nop = Jimple.v().newNopStmt();
        // this.InsertStmtEnd(nop);
        // IfStmt ifStmt = Jimple.v().newIfStmt(geExpr, nop);
        // method.getActiveBody().getUnits().insertBefore(ifStmt, asstStmt10);

        // NewArrayExpr arrayExpr =
        // Jimple.v().newNewArrayExpr(RefType.v(this.elementType), IntConstant.v(10));
        // Local arrayLocal10 =
        // Jimple.v().newLocal("c10",ArrayType.v(RefType.v(this.elementType),10 ));
        // AssignStmt as10 = Jimple.v().newAssignStmt(arrayLocal10,arrayExpr);
        // this.getJimpleBody().getLocals().addFirst(arrayLocal10);
        // this.InsertStmtEnd(as10);
        //
        // NewArrayExpr arrayExpr2 =
        // Jimple.v().newNewArrayExpr(RefType.v(this.elementType), IntConstant.v(10));
        // Local arrayLocal102 =
        // Jimple.v().newLocal("c10",ArrayType.v(RefType.v(this.elementType),10 ));
        // AssignStmt as102 = Jimple.v().newAssignStmt(arrayLocal102,arrayExpr2);
        // this.getJimpleBody().getLocals().addFirst(arrayLocal102);
        // this.InsertStmtEnd(as102);

        // for(int i = 0; i < 10; i++){
        // Value index = IntConstant.v(i);
        // Local temp2 = Jimple.v().newLocal("temp",RefType.v(this.elementType));
        // this.getJimpleBody().getLocals().addFirst(temp2);
        // Value rightSide = Jimple.v().newArrayRef(getAgain, IntConstant.v(i));
        // AssignStmt as12 = Jimple.v().newAssignStmt(temp2, rightSide);
        // this.InsertStmtEnd(as12);
        // ArrayRef leftSide = Jimple.v().newArrayRef(arrayLocal102, index);
        // AssignStmt as22 = Jimple.v().newAssignStmt(leftSide, temp2);
        // this.InsertStmtEnd(as22);
        // }

        // for(int i = 0; i < 10; i++){
        // Value index = IntConstant.v(i);
        // Local temp = Jimple.v().newLocal("temp",RefType.v(this.elementType));
        // this.getJimpleBody().getLocals().addFirst(temp);
        // Value rightSide = Jimple.v().newArrayRef(this.collection, IntConstant.v(i));
        // AssignStmt as1 = Jimple.v().newAssignStmt(temp, rightSide);
        // this.InsertStmtEnd(as1);
        // ArrayRef leftSide = Jimple.v().newArrayRef(arrayLocal10, index);
        // AssignStmt as2 = Jimple.v().newAssignStmt(leftSide, temp);
        // this.InsertStmtEnd(as2);
        // }

        // Local element = Jimple.v().newLocal("ele1", RefType.v(this.elementType));
        // ArrayRef jArrayRef = Jimple.v().newArrayRef(arrayLocal10, IntConstant.v(0));
        // AssignStmt assignStmt = Jimple.v().newAssignStmt(element, jArrayRef);
        // this.getJimpleBody().getLocals().addFirst(element);
        // this.InsertStmtEnd(assignStmt);
        //
        // Local element2 = Jimple.v().newLocal("ele2", RefType.v(this.elementType));
        // ArrayRef jArrayRef2 = Jimple.v().newArrayRef(getAgain, IntConstant.v(0));
        // AssignStmt assignStmt2 = Jimple.v().newAssignStmt(element2, jArrayRef2);
        // this.getJimpleBody().getLocals().addFirst(element2);
        // this.InsertStmtEnd(assignStmt2);
        //
        //
        // assertion
        // SootMethod assertion = this.getAssert();
        // List<Value> args = new ArrayList<>();
        // args.add(element);
        // args.add(element2);
        //
        // StaticInvokeExpr assertExpr =
        // Jimple.v().newStaticInvokeExpr(assertion.makeRef(), args);
        // Stmt assertStmt = Jimple.v().newInvokeStmt(assertExpr);
        // this.InsertStmtEnd(assertStmt);

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
