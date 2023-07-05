package com.mutation.instance;


import polyglot.ast.Assign;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JNewExpr;
import soot.tagkit.Tag;
import soot.util.Chain;

import java.util.*;

public class CustomClassIVMO extends InstanceVariableMutationOperator{

    /*
    1. Locate an instance variable that is not a primitive, wrapper or collection
    2. Locate where the variable gets constructed
    3. Locate a test that invokes any method on that variable
    4. Add test: Use the copy the body of the test / method as add test + assert not null + with copied variable
    5. Read test: Use the copy of the test + handle null pointer exception + with copied variable
    6: Wipe test: set variable to null + assert null
     */

    public Map<Type, SootMethod> fieldToMethodWithConstructor;
    public Map<Type, SootMethod> fieldToMethodWithVirtualInvoke;

    public CustomClassIVMO() {
        super();
        this.fieldToMethodWithVirtualInvoke = new HashMap<>();
        this.fieldToMethodWithConstructor = new HashMap<>();
    }


    @Override
    public boolean isApplicable() {
        return (this.variables.size() > 0) && (this.fieldToMethodWithVirtualInvoke.size() > 0);
    }

    private boolean locateMethodWithVirtualInvokeHelper(SootField field, SootMethod method) {


        if (method.getParameterCount() > 0 || !method.getReturnType().toString().equals(VoidType.v().toString()) || method.isStatic()) {
            return false;
        }

        Chain<Unit> units = method.getActiveBody().getUnits();
        Iterator<Unit> iterator = units.snapshotIterator();

        while (iterator.hasNext()) {
            Stmt stmt = (Stmt) iterator.next();
            if (!stmt.containsInvokeExpr()) {
                continue;
            }
            if (!(stmt.getInvokeExpr() instanceof VirtualInvokeExpr)) {
                continue;
            }

            VirtualInvokeExpr virtualInvokeExpr = (VirtualInvokeExpr) stmt.getInvokeExpr();

            if (!(virtualInvokeExpr.getBase() instanceof Local)) {
                continue;
            }
            Local local = (Local) virtualInvokeExpr.getBase();

            if (local.getType().equals(field.getType())) {
                this.fieldToMethodWithVirtualInvoke.put(field.getType(), method);
                return true;
            }
        }
        return false;
    }

    private boolean locateMethodWithVirtualInvoke(SootField field) {
        List<SootMethod> methods = this.getCurrentClass().getMethods();
        for (SootMethod method: methods) {
            boolean result  = locateMethodWithVirtualInvokeHelper(field, method);
            if (result) {
                return true;
            }
        }
        return false;
    }

    @Override
    public SootMethod createWipeTest(SootField field) {
        long startTime = System.currentTimeMillis();
        SootMethod test = this.createTestMethod("CroissantMutant_OD_IVD_VictimMutant");

        Local strLocal = Jimple.v().newLocal("strLocal",RefType.v("java.lang.String"));
        Body body = test.retrieveActiveBody();
        Local thisLocal = this.getThisLocal(test.getActiveBody());
        body.getLocals().add(strLocal);
        AssignStmt stmt = Jimple.v().newAssignStmt (strLocal, StringConstant.v("original"));
        this.InsertStmtEnd(test.getActiveBody(),stmt);


        InstanceFieldRef instanceFieldRef = Jimple.v().newInstanceFieldRef(thisLocal, field.makeRef());
        AssignStmt assignFieldToNull = Jimple.v().newAssignStmt(instanceFieldRef, NullConstant.v());
        this.InsertStmtEnd(test.getActiveBody(), assignFieldToNull);
        long endTime= System.currentTimeMillis();
        com.framework.OperatorSelector.MUTANT_TIME.put(test.getName(), endTime - startTime);

        return test;
    }

    private boolean locateMethodWithConstructorHelper(SootField field, SootMethod method) {

        List<Tag> tags = method.getTags();

        for (Tag tag: tags) {
            if (tag.toString().contains("BeforeEach")) {
                this.fieldToMethodWithConstructor.put(field.getType(), method);
                return true;
            }
        }

        if (method.getParameterCount() > 0 || !method.getReturnType().toString().equals(VoidType.v().toString()) || method.isStatic()) {
            return false;
        }

        Chain<Unit> units = method.getActiveBody().getUnits();
        Iterator<Unit> iterator = units.snapshotIterator();


        while (iterator.hasNext()) {

            Stmt stmt = (Stmt) iterator.next();

            if (!(stmt instanceof JAssignStmt)) {
                continue;
            }

            Value rightOp = ((AssignStmt) stmt).getRightOp();

            if (!(rightOp instanceof JNewExpr)) {
                continue;
            }

            if (rightOp.getType().toString().equals(field.getType().toString())) {
                this.fieldToMethodWithConstructor.put(field.getType(), method);
                return true;
            }
        }
        return false;
    }

    private boolean locateMethodWithConstructor(SootField field) {
        List<SootMethod> methods = this.getCurrentClass().getMethods();
        for (SootMethod method: methods) {
            boolean result  = locateMethodWithConstructorHelper(field, method);
            if (result) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void createAndInjectTests(SootField field) {
        SootMethod[] tests = new SootMethod[] {
            createAddTest(field),
            createWipeTest(field),
            createReadTest(field)
        };

        for (SootMethod test: tests) {
            injectTest(test);
            //addIgnoreAnnotation(test);
        }

        //createMainTest(tests, field);
    }


    @Override
    protected String getTestName() {
        return null;
    }

    @Override
    protected boolean isFieldAppropriate(SootField field) {
        String fieldString = field.getType().toString();
        Set<String> primitiveTypes = new HashSet<>(Arrays.asList("boolean", "int", "short", "byte", "long", "double", "char", "float"));
        if (fieldString.contains("java.util") || primitiveTypes.contains(fieldString)) {
            return false;
        }
        return (locateMethodWithConstructor(field) && locateMethodWithVirtualInvoke(field));
    }




    public void addAssertNotNull(Body body, Local rValue) {
        SootMethod assertNotNullMethod = this.getAssertNull();
        StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(assertNotNullMethod.makeRef(), rValue);
        InvokeStmt invokeStmt = Jimple.v().newInvokeStmt(staticInvokeExpr);
        this.InsertStmtEnd(body, invokeStmt);
    }

    public Local createAddTestBody(SootMethod test, SootField field) {
       SootMethod methodWithVirtualInvocation = this.fieldToMethodWithVirtualInvoke.get(field.getType());
       Body methodBodyClone = (Body) methodWithVirtualInvocation.retrieveActiveBody().clone();
       Local rValue = transformMethodBodyToMutantField(test, methodBodyClone, field);
        test.setActiveBody(methodBodyClone);
        return rValue;
    }


    @Override
    public void initializeField(SootMethod test, SootField field) {
        SootMethod methodWithConstructor = this.fieldToMethodWithConstructor.get(field.getType());
        Body methodBodyClone = (Body) methodWithConstructor.retrieveActiveBody().clone();
        Local rValue = transformMethodBodyToMutantField(test, methodBodyClone, field);
        //addAssertNotNull(methodBodyClone, rValue);
        test.setActiveBody(methodBodyClone);
    }

    private Local getInitRValue(Body body, SootField field) {
        Chain<Unit> units = body.getUnits();
        Iterator<Unit> iterator = units.snapshotIterator();

        while(iterator.hasNext()) {
            Stmt stmt = (Stmt) iterator.next();

            if (!(stmt instanceof AssignStmt)) {
                continue;
            }

            AssignStmt assignStmt = (AssignStmt) stmt;

            if (assignStmt.getLeftOp().getType().equals(field.getType())) {
                return (Local) assignStmt.getLeftOp();
            }
        }
        return null;
    }

    private Local addMutantLocalToBody(Body body, SootField field) {
        Local local = Jimple.v().newLocal(field.getName(), field.getType());
        body.getLocals().add(local);
        return local;
    }



    private Local transformMethodBodyToMutantField(SootMethod test, Body body, SootField field ) {
        Chain<Unit> units = body.getUnits();
        Iterator<Unit> iterator = units.snapshotIterator();
        Local targetLocal = null;

        while(iterator.hasNext()) {
            Stmt stmt = (Stmt) iterator.next();

            if (!(stmt instanceof AssignStmt)) {
                continue;
            }

            AssignStmt assignStmt = (AssignStmt) stmt;

            if (!(assignStmt.getLeftOp() instanceof InstanceFieldRef)) {
                continue;
            }


            InstanceFieldRef instanceFieldRef = (InstanceFieldRef) assignStmt.getLeftOp();

            if (instanceFieldRef.getField().getType().equals(field.getType())) {
                InstanceFieldRef newInstanceFieldRef = Jimple.v().newInstanceFieldRef(instanceFieldRef.getBase(), field.makeRef());
                assignStmt.setLeftOp(
                    newInstanceFieldRef
                );
            }
        }

        return targetLocal;
    }

    @Override
    public void wipeField(SootMethod test, Value rValue) {

    }

    @Override
    public void insertWipeFieldAssert(SootMethod test, Local boolLocal) {

    }

    @Override
    public void insertToField(SootMethod test, Local rValue) {

    }

    @Override
    public void insertInsertToFieldAssert(SootMethod test, Local rValue, Local boolLocal) {

    }

    @Override
    public void readField(SootMethod test, Local rValue) {

    }

    @Override
    public void insertReadFieldAssert(SootMethod test, Local boolLocal) {

    }

    @Override
    public void createMainTest(SootMethod[] innerTests, SootField field) {
        long startTime = System.currentTimeMillis();
        SootMethod mainTest = createTestMethod("CroissantMutant_OD_IVD_testMainMutant" + field.getName()+"_NoTemplate");
        initializeField(mainTest, field);
        /*for (SootMethod innerTest: innerTests) {
            addTestToMainTest(mainTest, innerTest);
        }
        this.addNonDeterminism(mainTest);*/
        odInvoke(this.getCurrentClass(), mainTest, null, innerTests[0], innerTests[1]);
        mainTest.setDeclaringClass(this.getCurrentClass());
        long endTime= System.currentTimeMillis();
        com.framework.OperatorSelector.MUTANT_TIME.put(mainTest.getName(), endTime - startTime);
    }

    @Override
    public SootMethod createAddTest(SootField field) {
        long startTime = System.currentTimeMillis();
        SootMethod testAdd = this.createTestMethod("CroissantMutant_OD_IVD_testAAddMutant" + field.getName()+"_NoTemplate");
        testAdd.setDeclaringClass(this.getCurrentClass());
        initializeField(testAdd, field);
        long endTime= System.currentTimeMillis();
        com.framework.OperatorSelector.MUTANT_TIME.put(testAdd.getName(), endTime - startTime);

        return testAdd;
    }

    public void createReadTestBody(SootMethod test, SootField field) {
        SootMethod methodWithVirtualInvocation = this.fieldToMethodWithVirtualInvoke.get(field.getType());
        Body methodBodyClone = (Body) methodWithVirtualInvocation.retrieveActiveBody().clone();
        transformMethodBodyToMutantField(test, methodBodyClone, field);

        Local thisLocal = this.getThisLocal(methodBodyClone);
        InstanceFieldRef instanceFieldRef = Jimple.v().newInstanceFieldRef(thisLocal, field.makeRef());

        Local rValueForField = Jimple.v().newLocal("rValueForField", field.getType());
        methodBodyClone.getLocals().add(rValueForField);
        AssignStmt assignStmt = Jimple.v().newAssignStmt(rValueForField, instanceFieldRef);


        SootMethod assertNotNullMethod = this.getAssertNotNull();
        StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(assertNotNullMethod.makeRef(), rValueForField);
        InvokeStmt invokeStmt = Jimple.v().newInvokeStmt(staticInvokeExpr);

        this.InsertStmtBeginning(methodBodyClone, invokeStmt);
        this.InsertStmtBeginning(methodBodyClone, assignStmt);

        test.setActiveBody(methodBodyClone);
    }

    @Override
    public SootMethod createReadTest(SootField field) {
        long startTime = System.currentTimeMillis();
        SootMethod testRead = this.createTestMethod("CroissantMutant_OD_IVD_testBReadMutant_" + field.getName()+"_NoTemplate");
        createReadTestBody(testRead, field);
        long endTime= System.currentTimeMillis();
        com.framework.OperatorSelector.MUTANT_TIME.put(testRead.getName(), endTime - startTime);

        return testRead;
    }
}
