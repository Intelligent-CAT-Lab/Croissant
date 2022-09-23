package com.mutation.framework;

import com.mutation.SootMutationOperator;
import com.mutation.SootTestInjector;
import soot.*;
import soot.jimple.*;
import soot.tagkit.AnnotationTag;
import soot.tagkit.Tag;
import soot.tagkit.VisibilityAnnotationTag;
import soot.util.Chain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class MockitoMutationOperator extends SootTestInjector {

    private SootField fieldWithMock;
    private SootMethod testWithVerifyTimes;

    public void initializeMock() {
        SootMethod clinit = this.getCurrentClass().getMethodByName("<clinit>");

        // $r0 = staticinvoke <org.mockito.Mockito: java.lang.Object mock(java.lang.Class)>(class "Ljava/util/ArrayList;")

        Local objectLocal = Jimple.v().newLocal("objectLocal", RefType.v("java.lang.Object"));
        clinit.getActiveBody().getLocals().add(objectLocal);

        List<Type> args = new ArrayList<>();
        args.add(RefType.v("java.lang.Class"));
        SootMethod mockMethod = this.createPhantomMethod(
            "org.mockito.Mockito",
            "<org.mockito.Mockito: java.lang.Object mock(java.lang.Class)>",
            "mock",
            args,
            RefType.v("java.lang.Object"),
            true
            );
        ClassConstant classConstant = ClassConstant.v("L"+this.fieldWithMock.getType().toString().replace(".","/")+";");
        StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(mockMethod.makeRef(), classConstant);
        AssignStmt assignStmt = Jimple.v().newAssignStmt(objectLocal, staticInvokeExpr);
        this.InsertStmtEnd(clinit, assignStmt);

        //$r1 = (java.util.List) $r0
        Local castedLocal = Jimple.v().newLocal("$r1", objectLocal.getType());
        clinit.getActiveBody().getLocals().add(castedLocal);
        CastExpr castExpr = Jimple.v().newCastExpr(objectLocal, this.fieldWithMock.getType());
        AssignStmt assignStmt1 = Jimple.v().newAssignStmt(castedLocal, castExpr);
        this.InsertStmtEnd(clinit, assignStmt1);

        SootField sootField = null;

        for (SootField field: this.getCurrentClass().getFields()) {
            if (field.getName().equals(this.fieldWithMock.getName())) {
                sootField = field;
            }
        }

        if (sootField == null) {

            try {
                this.getCurrentClass().getFields().add(this.fieldWithMock);
            } catch (Exception e) {
                this.fieldWithMock.setDeclaringClass(this.getCurrentClass());
            }
            addMockTag(fieldWithMock);
            sootField = fieldWithMock;
        }

        //<com.example.UselessTest: java.util.List mock> = $r1
        SootFieldRef sootFieldRef = null;
        try {
            sootFieldRef = sootField.makeRef();
        } catch (Exception e) {
            sootField.setDeclaringClass(this.getCurrentClass());
            sootFieldRef = sootField.makeRef();
        }
        FieldRef fieldRef = Jimple.v().newStaticFieldRef(sootFieldRef);
        AssignStmt assignStmt2 = Jimple.v().newAssignStmt(fieldRef, castedLocal);
        this.InsertStmtEnd(clinit, assignStmt2);


    }

    public void addMockTag(SootField field) {
        /*
        Visibility Annotation: level: RUNTIME (runtime-visible)
        Annotations:
        Annotation type: Lorg/mockito/Mock; without elements
         */

        AnnotationTag annotationTag = new AnnotationTag("Lorg/mockito/Mock;");
        VisibilityAnnotationTag tag = new VisibilityAnnotationTag(0);
        tag.addAnnotation(annotationTag);
        field.addTag(tag);
    }


    public SootMethod createstateSetterTest(int stateSetterTestNumber) {
        SootMethod stateSetterTest = this.createTestMethod("c_stateSetter" + "__MockitoMutationOperator__" + stateSetterTestNumber);

        List<Type> args = new ArrayList<>();
        args.add(ArrayType.v(
            RefType.v("java.lang.Object"), 1
        ));

        SootMethod resetMethod = this.createPhantomMethod(
            "org.mockito.Mockito",
            "org.mockito.Mockito: void reset(java.lang.Object[])",
            "reset",
                    args,
                    VoidType.v(),
            true
        );

        SootField sootField = null;

        for (SootField field: this.getCurrentClass().getFields()) {
            if (field.getName().equals(this.fieldWithMock.getName())) {
                sootField = field;
            }
        }

        if (sootField == null) {

            try {
                this.getCurrentClass().getFields().add(this.fieldWithMock);
            } catch (Exception e) {
                this.fieldWithMock.setDeclaringClass(this.getCurrentClass());
            }
            addMockTag(fieldWithMock);
            sootField = fieldWithMock;
        }

        initializeMock();

        Local array = Jimple.v().newLocal("array", ArrayType.v(sootField.getType(), 1));
        stateSetterTest.getActiveBody().getLocals().add(array);
        NewArrayExpr newArrayExpr = Jimple.v().newNewArrayExpr(sootField.getType(), IntConstant.v(1));
        AssignStmt arrayInit = Jimple.v().newAssignStmt(array, newArrayExpr);
        this.InsertStmtEnd(stateSetterTest, arrayInit);

        Local mockLocal = Jimple.v().newLocal("mockLocal", sootField.getType());
        stateSetterTest.getActiveBody().getLocals().add(mockLocal);

        sootField.setDeclaringClass(this.getCurrentClass());
        StaticFieldRef staticFieldRef = Jimple.v().newStaticFieldRef(sootField.makeRef());
        AssignStmt mockLocalAssign = Jimple.v().newAssignStmt(mockLocal, staticFieldRef);
        this.InsertStmtEnd(stateSetterTest, mockLocalAssign);

        ArrayRef arrayRef = Jimple.v().newArrayRef(array, IntConstant.v(0));
        AssignStmt arrayRefAssign = Jimple.v().newAssignStmt(arrayRef, mockLocal);
        this.InsertStmtEnd(stateSetterTest, arrayRefAssign);

        StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(resetMethod.makeRef(), array);
        InvokeStmt invokeStmt = Jimple.v().newInvokeStmt(staticInvokeExpr);
        this.InsertStmtEnd(stateSetterTest, invokeStmt);

        transformFieldRefs(stateSetterTest);
        return makestateSetterTest(stateSetterTest);
    }

    public void createstateSetterTests() {
        for (int i = 0; i < 5; i++) {
            SootMethod stateSetterTest = this.createstateSetterTest(i);
            if (!this.getCurrentClass().declaresMethod(stateSetterTest.getSubSignature())) {
                this.getCurrentClass().addMethod(stateSetterTest);
            }
        }
    }


    @Override
    public void mutateMethod() throws Exception {
        SootMethod polluterTest = this.createPolluterTest();
        try {
            this.getCurrentClass().addMethod(polluterTest);
        } catch (Exception e) {

        }
        transformFieldRefs(polluterTest);

        SootMethod currentMethod = this.getCurrentMethod();
        SootMethod victimTest = this.createTestMethod("b_victim_mockito"+ currentMethod.getName());
        victimTest.setActiveBody((Body) currentMethod.getActiveBody().clone());
        transformFieldRefs(victimTest);

        try {
            this.getCurrentClass().addMethod(victimTest);
        } catch (Exception e) {

        }

        createstateSetterTests();

        //SootMethod mainTest = this.createMainTest(polluterTest);
/*        try {
            //this.getCurrentClass().addMethod(mainTest);
        } catch (Exception e) {
            *//*e.printStackTrace();*//*
        }*/

        //this.addNonDeterminism(mainTest);
    }

    private SootMethod removeVerificationFromPolluter(SootMethod polluter) {
        Chain<Unit> units = polluter.getActiveBody().getUnits();
        Iterator<Unit> iterator = units.snapshotIterator();
        boolean startRemoving = false;

        while (iterator.hasNext()) {
            Unit unit = iterator.next();
            Stmt stmt = (Stmt) unit;

            if (startRemoving && !(stmt instanceof ReturnVoidStmt)) {
                units.remove(unit);
            }

            if (!(stmt instanceof AssignStmt)) {
                continue;
            }

            AssignStmt assignStmt = (AssignStmt) stmt;

            if (!(assignStmt.getRightOp() instanceof InvokeExpr)) {
                continue;
            }

            InvokeExpr invokeExpr = (InvokeExpr) assignStmt.getRightOp();
            String methodName = invokeExpr.getMethod().getName();

            if (methodName.equals("verify")) {
                startRemoving = true;
                units.remove(unit);
            }

        }
        return polluter;
    }

    protected SootMethod createPolluterTest() {
        SootMethod test = this.createTestMethod("a_polluterTest" + testWithVerifyTimes.getName());
        test.setActiveBody((Body) this.getCurrentMethod().getActiveBody().clone());
        return removeVerificationFromPolluter(test);
    }

    protected SootMethod createMainTest(SootMethod polluterTest) {

        SootMethod mainTest = this.createTestMethod("testMainMutant");
        Local identityLocal = getIdentityLocal(mainTest);
        Local thisLocal = createThisLocal(mainTest);
        castThisLocal(mainTest, thisLocal, identityLocal);

        /*VirtualInvokeExpr polluterInvocation = Jimple.v().newVirtualInvokeExpr(thisLocal, polluterTest.makeRef());
        InvokeStmt polluterStmt = Jimple.v().newInvokeStmt(polluterInvocation);
        InsertStmtEnd(mainTest, polluterStmt);


        VirtualInvokeExpr victimTestInvocation = Jimple.v().newVirtualInvokeExpr(thisLocal, this.getCurrentMethod().makeRef());
        InvokeStmt victimTestStmt = Jimple.v().newInvokeStmt(victimTestInvocation);
        InsertStmtEnd(mainTest, victimTestStmt);*/
        odInvoke(this.getCurrentClass(), mainTest, null, polluterTest, this.getCurrentMethod());

        return mainTest;
    }

    @Override
    public <T extends Unit> List<T> locateUnits() {
        return null;
    }

    public boolean doesContainMockAnnotation(SootField sootField) {
        List<Tag> tags = sootField.getTags();
        for (Tag tag : tags) {
            if (tag.toString().contains("Lorg/mockito/Mock")) {
                return true;
            }
        }
        return false;
    }

    public SootMethod findTestWithVerifyTimes() {
        for (SootMethod test : this.getCurrentClass().getMethods()) {
            if (isTestWithVerifyTimes(test) && !test.getName().toLowerCase().contains("mutant")) {
                return test;
            }
        }
        return null;
    }

    public boolean isTestWithVerifyTimes(SootMethod test) {
        Chain<Unit> units = test.getActiveBody().getUnits();
        Iterator<Unit> unitIterator = units.snapshotIterator();

        while (unitIterator.hasNext()) {
            Stmt stmt = (Stmt) unitIterator.next();

            if (!(stmt instanceof AssignStmt)) {
                continue;
            }

            AssignStmt assignStmt = (AssignStmt) stmt;

            Value rightOp = assignStmt.getRightOp();

            if (!(rightOp instanceof StaticInvokeExpr)) {
                continue;
            }

            StaticInvokeExpr staticInvokeExpr = (StaticInvokeExpr) rightOp;

            if (staticInvokeExpr.getMethod().getSignature().contains("org.mockito.verification.VerificationMode times(int)")) {
                return true;
            }
        }
        return false;
    }


    SootField originalField;
    public SootField cloneField(SootField fieldWithMock) {
        SootField fieldWithMockClone = new SootField(fieldWithMock.getName() + "Mutant", fieldWithMock.getType(), fieldWithMock.getModifiers());
        /*this.getCurrentClass().getFields().add(fieldWithMockClone);*/
        originalField = fieldWithMock;
        return fieldWithMockClone;
    }

    public void transformFieldRefs(SootMethod sootMethod) {
        Chain<Unit> units = sootMethod.getActiveBody().getUnits();
        Iterator<Unit> itr = units.snapshotIterator();

        while(itr.hasNext()) {
            Stmt stmt = (Stmt) itr.next();

            if (!(stmt instanceof AssignStmt)) {
                continue;
            }
            AssignStmt assignStmt = (AssignStmt) stmt;

            if (assignStmt.getRightOp().toString().equals(this.originalField.makeRef().toString())) {
                SootField sootField = null;

                for (SootField field: this.getCurrentClass().getFields()) {
                    if (field.getName().equals(this.fieldWithMock.getName())) {
                        sootField = field;
                    }
                }

                if (sootField == null) {
                    this.fieldWithMock.setDeclaringClass(this.getCurrentClass());
                    sootField = fieldWithMock;
                }

                FieldRef newSootFieldRef = Jimple.v().newStaticFieldRef(sootField.makeRef());
                assignStmt.setRightOp(newSootFieldRef);

            }

        }

    }

    @Override
    public boolean isApplicable() {

        this.fieldWithMock = null;
        this.testWithVerifyTimes = null;

        Chain<SootField> fields = this.getCurrentClass().getFields();
        Iterator<SootField> fieldIterator = fields.snapshotIterator();

        while (fieldIterator.hasNext()) {
            SootField field = fieldIterator.next();
            if (this.doesContainMockAnnotation(field)) {
                this.fieldWithMock = cloneField(field);
            }
        }

        this.testWithVerifyTimes = this.findTestWithVerifyTimes();

        if (this.testWithVerifyTimes == null) {
            return false;
        }
        this.setCurrentMethod(testWithVerifyTimes);

        return ((this.fieldWithMock != null) && (this.testWithVerifyTimes != null));
    }


    @Override
    public int getMutantNumber() {
        return 1;
    }

    @Override
    protected String getTestName() {
        return this.getCurrentMethod().getName();
    }
}
