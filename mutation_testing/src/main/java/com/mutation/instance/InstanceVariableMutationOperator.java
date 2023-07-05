package com.mutation.instance;

import com.mutation.SootTestInjector;
import soot.*;
import soot.jimple.*;
import soot.util.Chain;

import java.util.*;

public abstract class InstanceVariableMutationOperator extends SootTestInjector {

    protected final List<SootField> variables;

    public InstanceVariableMutationOperator() {
        super();
        this.variables = new ArrayList<>();
    }

    @Override
    public void setCurrentClass(String inputDir, String className) {
        super.setCurrentClass(inputDir, className);
        locateVariables();
    }

    @Override
    public void setCurrentClass(SootClass currentClass) {
        /*super.setCurrentClass(currentClass);*/
        locateVariables();
    }



    @Override
    public boolean isApplicable() {
        return this.variables.size() > 0;
    }
// {
//        return this.variables.size() > 0;
//    }

    private Local getRValueForField(SootMethod test, SootField field) {
        Local rValue = Jimple.v().newLocal("$"+field.getName(), field.getType());
        test.getActiveBody().getLocals().addFirst(rValue);
        return rValue;
    }

    protected Local getIsEmpty(SootMethod test, Local rValue, String collectionName, String isEmptyMethodSignature) {

        SootClass sootClass = Scene.v().getSootClass(collectionName);
        SootMethod isEmpty = sootClass.getMethod(isEmptyMethodSignature);
        InterfaceInvokeExpr virtualInvokeExpr = Jimple.v().newInterfaceInvokeExpr(rValue, isEmpty.makeRef());

        Local boolLocal = Jimple.v().newLocal("boolLocal", BooleanType.v());

        test.getActiveBody().getLocals().addFirst(boolLocal);

        AssignStmt assignStmt = Jimple.v().newAssignStmt(boolLocal, virtualInvokeExpr);
        InsertStmtEnd(test, assignStmt);

        return boolLocal;
    }

    private void initializeRValue(SootMethod test, SootField field, Local rValue, String collectionInitMethodSignature, String collectionName) {
        NewExpr newExpr = Jimple.v().newNewExpr(RefType.v(collectionName));
        Stmt assignStmt = Jimple.v().newAssignStmt(rValue, newExpr);
        InsertStmtEnd(test, assignStmt);

        SootMethod initMethod = Scene.v().getMethod(collectionInitMethodSignature);
        SpecialInvokeExpr specialInvokeExpr = Jimple.v().newSpecialInvokeExpr(rValue, initMethod.makeRef());
        Stmt stmt = Jimple.v().newInvokeStmt(specialInvokeExpr);
        InsertStmtEnd(test, stmt);
    }

    private void initializeFieldWithRValue(SootMethod test, SootField field, Local rValue) {
        Local identityLocal = this.getIdentityLocal(test);
        InstanceFieldRef instanceFieldRef = Jimple.v().newInstanceFieldRef(identityLocal, field.makeRef());
        AssignStmt initFieldStmt = Jimple.v().newAssignStmt(instanceFieldRef, rValue);
        InsertStmtEnd(test, initFieldStmt);
    }

    protected void initializeCollection(SootMethod test, SootField field, String collectionInitMethodSignature, String collectionName) {
        Local rValue = getRValueForField(test, field);
        initializeRValue(test, field, rValue, collectionInitMethodSignature, collectionName);
        initializeFieldWithRValue(test, field, rValue);
    }

    protected Local getDoesContain(SootMethod test, Local rValue, String collectionName, String doesContainMethodSignature) {
        SootClass hashMapClass = Scene.v().loadClass(collectionName,SootClass.SIGNATURES);
        SootMethod sootMethod = hashMapClass.getMethod(doesContainMethodSignature);
        InterfaceInvokeExpr virtualInvokeExpr = Jimple.v().newInterfaceInvokeExpr(rValue, sootMethod.makeRef(), StringConstant.v("x"));

        Local boolLocal = Jimple.v().newLocal("boolLocal", BooleanType.v());
        test.getActiveBody().getLocals().addFirst(boolLocal);
        AssignStmt assignStmt = Jimple.v().newAssignStmt(boolLocal, virtualInvokeExpr);

        InsertStmtEnd(test, assignStmt);

        return boolLocal;
    }

    public void insertToCollection(SootMethod test, Local rValue, String collectionName, String addMethodSignature) {
        SootClass hashMapClass = Scene.v().loadClass(collectionName,SootClass.SIGNATURES);
        SootMethod sootMethod = hashMapClass.getMethod(addMethodSignature);

        //TODO: generalize
        List<Value> args = new ArrayList<>();
        if (sootMethod.getParameterCount() == 2) {
            args.add(StringConstant.v("x"));
            args.add(StringConstant.v("y"));
        }
        else if (sootMethod.getParameterCount() == 1) {
            args.add(StringConstant.v("x"));
        }

        InterfaceInvokeExpr virtualInvokeExpr = Jimple.v().newInterfaceInvokeExpr(rValue, sootMethod.makeRef(), args);
        InvokeStmt invokeStmt = Jimple.v().newInvokeStmt(virtualInvokeExpr);
        InsertStmtEnd(test, invokeStmt);
    }

    protected void clearCollection(SootMethod test, Local rValue, String collection, String clearMethodSignature) {
        SootClass hashMapClass = Scene.v().loadClass(collection,SootClass.SIGNATURES);
        SootMethod sootMethod = hashMapClass.getMethod(clearMethodSignature);

        InterfaceInvokeExpr virtualInvokeExpr = Jimple.v().newInterfaceInvokeExpr(rValue, sootMethod.makeRef());
        InvokeStmt invokeStmt = Jimple.v().newInvokeStmt(virtualInvokeExpr);

        InsertStmtEnd(test, invokeStmt);
    }

    public void createMainTest(SootMethod[] innerTests, SootField field) {
        SootMethod mainTest = createTestMethod("testMainMutant" + field.getName());
        initializeField(mainTest, field);
        odInvoke(this.getCurrentClass(), mainTest, null, innerTests[0], innerTests[2]);
        injectTest(mainTest);
    }


    public SootMethod createMainTest(SootMethod polluter, SootMethod victim, SootField field) {
        SootMethod mainTest = createTestMethod("testMainMutant" + field.getName());
        initializeField(mainTest, field);
        odInvoke(this.getCurrentClass(), mainTest, null, polluter, victim);
        return mainTest;
    }


    /*public void addTestToMainTest(SootMethod mainTest, SootMethod innerTest) {
        Local identityLocal = getIdentityLocal(mainTest);
        Local thisLocal = createThisLocal(mainTest);

        if (identityLocal != null) {
            castThisLocal(mainTest, thisLocal, identityLocal);
        }

        VirtualInvokeExpr virtualInvokeExpr = Jimple.v().newVirtualInvokeExpr(thisLocal, innerTest.makeRef());
        InvokeStmt invokeStmt = Jimple.v().newInvokeStmt(virtualInvokeExpr);
        InsertStmtEnd(mainTest, invokeStmt);
    }*/

    public void createAndInjectTests(SootField field) {
        /*SootMethod[] tests = new SootMethod[] {
            createWipeTest(field),
            createReadTest(field),
        };*/

        /*createMainTest(tests, field);*/
        /*for (SootMethod test: tests) {
            addIgnoreAnnotation(test);
            injectTest(test);
        }*/

        /*SootMethod wipeTest = createWipeTest(field);*/
        //SootMethod readTest = createReadTest(field);
        /*addIgnoreAnnotation(wipeTest);*/
        /*injectTest(wipeTest);*/


    }
    private String createTest(String testName) {
        return "@Test\n" +
            "public void " + testName + "()" + "{\n";
    }


    @Override
    public void mutateMethod() throws Exception {

//        LinkedList<SootField> clonedVariables = cloneVariables();
//
//        for (SootField field: clonedVariables) {
//            try {
//                this.getCurrentClass().addField(field);
//            } catch (Exception e) {
//                field.setDeclaringClass(this.getCurrentClass());
//            }
//        }
            /*createAndInjectTests(field);*/



//            SootMethod wipeTest = createWipeTest(field);
//            //addIgnoreAnnotation(wipeTest);
//            try {
//                this.getCurrentClass().addMethod(wipeTest);
//            } catch (Exception e) {
//                System.out.println("test already present");
//            }
//
//            SootMethod readTest = createReadTest(field);
//            //addIgnoreAnnotation(readTest);
//            try {
//                this.getCurrentClass().addMethod(readTest);
//            } catch (Exception e) {
//                System.out.println("test already present");
//            }

            //SootMethod mainTest = createTestMethod("testMainMutant" + field.getName());
            /*initializeField(mainTest, field);
            odInvoke(this.getCurrentClass(), mainTest, null, wipeTest, readTest);
            try {
                this.getCurrentClass().addMethod(mainTest);
            } catch (Exception e) {
                e.printStackTrace();
            }*/


        //System.out.println();
    /*    while (iter.hasNext()) {
            SootField clonedVariable = iter.next();
            createAndInjectTests(clonedVariable);
        }*/
        /*for (SootField clonedVariable: clonedVariables) {
            //createAndInjectTests(clonedVariable);
        }*/
    }

    public LinkedList<SootField> cloneVariables() {
        LinkedList<SootField> clonedVariables = new LinkedList<>();
        /*int i = 0;
        while (i < this.variables.size()) {
            clonedVariables.add(this.variables.get(i));
            i++;
        }*/
        for (SootField field: this.variables) {
            clonedVariables.add(cloneVariable(field));
        }
        return clonedVariables;
    }

    protected abstract boolean isFieldAppropriate(SootField field);

    public void locateVariables() {

        if (getCurrentClass() != null) {
            Chain<SootField> fields = getCurrentClass().getFields();
            for (SootField sootField: fields) {
                if (isFieldAppropriate(sootField) && !sootField.isStatic()) {
                    this.variables.add(sootField);
                    return;
                }
            }
        }

    }

    public SootField cloneVariable(SootField field) {

        String fieldName = "Mutant" + field.getName() + "__";
        SootField clonedField = new SootField(fieldName, field.getType(), field.getModifiers());
        return clonedField;
    }

    public abstract void initializeField(SootMethod test, SootField rValue);

    protected Local getThisLocal(Body body) {
        Chain<Local> locals =  body.getLocals();
        Iterator<Local> iterator = locals.snapshotIterator();
        while (iterator.hasNext()) {
            Local local = iterator.next();
            if (local.getType().toString().equals(this.getCurrentClass().toString())) {
                return local;
            }
        }
        return null;
    }

    public Local prepAndGetThisLocal(SootMethod test) {
        Local identityLocal = getIdentityLocal(test);
        Local thisLocal = createThisLocal(test);
        castThisLocal(test, thisLocal, identityLocal);
        return thisLocal;
    }

    public Local createRValueForField(SootMethod test, SootField field) {
        Local rValue = Jimple.v().newLocal("$"+field.getName(), field.getType());
        test.getActiveBody().getLocals().addFirst(rValue);
        return rValue;
    }

    public void initRValue(SootMethod test, Local rValue, Local thisLocal, SootField field) {
        InstanceFieldRef instanceFieldRef = Jimple.v().newInstanceFieldRef(thisLocal, field.makeRef());
        AssignStmt assignStmt = Jimple.v().newAssignStmt(rValue, instanceFieldRef);
        InsertStmtEnd(test, assignStmt);
    }

    public Local callField(SootMethod test, SootField field) {
        Local thisLocal = prepAndGetThisLocal(test);
        Local rValue = createRValueForField(test, field);
        initRValue(test, rValue, thisLocal, field);
        return rValue;
    }

    public AbstractMap.SimpleEntry<SootMethod, Local> createAndPrepTest(SootField field, String testName) {
        long startTime = System.currentTimeMillis();
        SootMethod test = createTestMethod("CroissantMutant_OD_IVD_Victim");
        Local rValue = callField(test, field);
        long endTime= System.currentTimeMillis();
        com.framework.OperatorSelector.MUTANT_TIME.put(test.getName(), endTime - startTime);

        return (new AbstractMap.SimpleEntry<SootMethod, Local>(test, rValue));
    }

    public void createAndInsertAssertTrue(SootMethod mainTest, Value booleanValue) {
        SootMethod assertTrue = getAssertTrue();
        StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(assertTrue.makeRef(), booleanValue);
        InvokeStmt invokeStmt = Jimple.v().newInvokeStmt(staticInvokeExpr);
        mainTest.getActiveBody().getUnits().addFirst(invokeStmt);
    }

    //TODO: refactor all abstract methods, argument should be Value not local so that it also works with field refs and such
    public abstract void wipeField(SootMethod test, Value rValue);

    public abstract void insertWipeFieldAssert(SootMethod test, Local boolLocal);

    public abstract void insertToField(SootMethod test, Local rValue);

    public abstract void insertInsertToFieldAssert(SootMethod test, Local rValue, Local boolLocal);

    public abstract void readField(SootMethod test, Local rValue);

    public abstract void insertReadFieldAssert(SootMethod test, Local boolLocal);

    public SootMethod createWipeTest(SootField field) {
        AbstractMap.SimpleEntry<SootMethod, Local> testPair = createAndPrepTest(field, "testAWipeMutant");

        SootMethod test = testPair.getKey();
        Local rValue = testPair.getValue();

        wipeField(test, rValue);

        return test;
    }

    public SootMethod createReadTest(SootField field) {
        AbstractMap.SimpleEntry<SootMethod, Local> testPair = createAndPrepTest(field, "testBReadMutant");

        SootMethod test = testPair.getKey();
        Local rValue = testPair.getValue();

        readField(test, rValue);

        return test;
    }

    public SootMethod createAddTest(SootField field) {
        AbstractMap.SimpleEntry<SootMethod, Local> testPair = createAndPrepTest(field, "testAAddMutant");

        SootMethod test = testPair.getKey();
        Local rValue = testPair.getValue();

        insertToField(test, rValue);

        return test;
    }

    @Override
    public List<SootField> locateUnits() {
        return null;
    }


    @Override
    public int getMutantNumber() {
        return this.variables.size();
    }
}
