package com.mutation;

import soot.*;
import soot.jimple.*;
import soot.jimple.internal.JCastExpr;
import soot.jimple.internal.JIdentityStmt;
import soot.jimple.internal.JNewExpr;
import soot.options.Options;
import soot.tagkit.*;
import soot.util.Chain;
import soot.util.JasminOutputStream;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * Abstract mutation operator that all mutation operators will inherit from
 */
public abstract class SootMutationOperator implements MutationOperator {
    private SootClass mainClass;
    private Body currentMethod;
    protected HashMap<Local, List<Stmt>> localToStmtMap;
    private String className;
    private SootMethod currentSootMethod; //TODO: rename accordingly
    protected String inputDir;
    private String currentMethodSubsignature;
    private final List<SootClass> subclasses = new ArrayList<>();
    private double threshold;
    private boolean jupiter;

    public void setJupiter(boolean jupiter) {
        this.jupiter = jupiter;
    }


    // FIXME remove when implemented in all MOs.
    public void runOnceBefore() {

    }
    public void runOnceAfter() {

    }



    public SootMethod createIncrementstateSetterCounterMethod(SootField counterField) {
        SootMethod incrementstateSetterCounterMethod = this.createMethod("incrementCleanerCounter", new ArrayList<>(), null, new ArrayList<>());
        if (this.getCurrentClass().declaresMethod(incrementstateSetterCounterMethod.getSubSignature())) {
            return this.getCurrentClass().getMethod(incrementstateSetterCounterMethod.getSubSignature());
        }

        Local counterFieldLocal = Jimple.v().newLocal("counterFieldLocal", DoubleType.v());
        incrementstateSetterCounterMethod.getActiveBody().getLocals().add(counterFieldLocal);

        counterField.setDeclaringClass(this.getCurrentClass());
        FieldRef counterFieldRef = Jimple.v().newStaticFieldRef(counterField.makeRef());
        AssignStmt counterFieldLocalInit = Jimple.v().newAssignStmt(counterFieldLocal, counterFieldRef);
        this.InsertStmtEnd(incrementstateSetterCounterMethod, counterFieldLocalInit);

        Local updatedValue = Jimple.v().newLocal("updatedValue", counterField.getType());
        incrementstateSetterCounterMethod.getActiveBody().getLocals().add(updatedValue);
        AddExpr addExpr = Jimple.v().newAddExpr(counterFieldLocal, DoubleConstant.v(1.0));
        AssignStmt updatedValueAssign = Jimple.v().newAssignStmt(updatedValue, addExpr);
        this.InsertStmtEnd(incrementstateSetterCounterMethod, updatedValueAssign);

        this.getCurrentClass().addMethod(incrementstateSetterCounterMethod);

        AssignStmt incrementField = Jimple.v().newAssignStmt(counterFieldRef, updatedValue);
        this.InsertStmtEnd(incrementstateSetterCounterMethod, incrementField);

        return incrementstateSetterCounterMethod;
    }

    public SootField injectstateSetterCounterField() {
        SootField stateSetterCounter = new SootField("stateSetterCounter", DoubleType.v(), java.lang.reflect.Modifier.STATIC);
        String stateSetterCounterName = stateSetterCounter.getName();

        Boolean containsstateSetterCounter = false;
        Chain<SootField> fields = this.getCurrentClass().getFields();
        Iterator<SootField> fieldIterator = fields.iterator();

        while(fieldIterator.hasNext()) {
            SootField sootField = fieldIterator.next();
            String sootFieldName = sootField.getName();

            if (sootFieldName.equals(stateSetterCounterName)) {
                containsstateSetterCounter = true;
            }
        }

        if (!containsstateSetterCounter) {
            //fields.add(stateSetterCounter);
            this.getCurrentClass().addField(stateSetterCounter);
        }


        return stateSetterCounter;
    }

    public SootMethod makestateSetterTest(SootMethod stateSetterTest, double index) {
        SootField stateSetterCounter = injectstateSetterCounterField();
        SootMethod incrementstateSetterCounterMethod = this.createIncrementstateSetterCounterMethod(stateSetterCounter);
        SootMethod getstateSetterCountMethod = createGetstateSetterCountMethod();

        try {
            this.getCurrentClass().addMethod(incrementstateSetterCounterMethod);
        } catch (Exception e) {
            incrementstateSetterCounterMethod = this.getCurrentClass().getMethod(incrementstateSetterCounterMethod.getSubSignature());
        }


        try {
            this.getCurrentClass().addMethod(getstateSetterCountMethod);
        } catch (Exception e) {
            getstateSetterCountMethod = this.getCurrentClass().getMethod(getstateSetterCountMethod.getSubSignature());
        }


        List<Stmt> stmts = new ArrayList<>();

        Local thisLocal = this.getThisLocal(stateSetterTest.getActiveBody(), this.getCurrentClass());
        VirtualInvokeExpr incrementstateSetterCounterExpr = Jimple.v().newVirtualInvokeExpr(thisLocal, incrementstateSetterCounterMethod.makeRef());
        InvokeStmt incrementstateSetterStmt = Jimple.v().newInvokeStmt(incrementstateSetterCounterExpr);
        /*this.InsertStmtBeginning(stateSetterTest, incrementstateSetterStmt);*/
        stmts.add(incrementstateSetterStmt);

        Local configLocal = Jimple.v().newLocal("configLocal", DoubleType.v());
        stateSetterTest.getActiveBody().getLocals().add(configLocal);
        VirtualInvokeExpr getCounterConfigExpr = null;

        getCounterConfigExpr = Jimple.v().newVirtualInvokeExpr(thisLocal, getstateSetterCountMethod.makeRef());
        AssignStmt configLocalInit = Jimple.v().newAssignStmt(configLocal, getCounterConfigExpr);
        stmts.add(configLocalInit);


        Local counterLocal = Jimple.v().newLocal("counterLocal", stateSetterCounter.getType());
        stateSetterTest.getActiveBody().getLocals().add(counterLocal);
//        StaticFieldRef counterFieldRef = null;
//        try {
//            counterFieldRef = Jimple.v().newStaticFieldRef(stateSetterCounter.makeRef());
//        } catch (Exception e) {
//            stateSetterCounter.setDeclaringClass(this.getCurrentClass());
//            counterFieldRef = Jimple.v().newStaticFieldRef(stateSetterCounter.makeRef());
//        }
        AssignStmt counterLocalInit = Jimple.v().newAssignStmt(counterLocal, DoubleConstant.v(index));
        stmts.add(counterLocalInit);


        Collections.reverse(stmts);

        for (Stmt stmt: stmts) {
            this.InsertStmtBeginning(stateSetterTest, stmt);
        }


        Chain<Unit> units = stateSetterTest.getActiveBody().getUnits();
        Iterator<Unit> unitIterator = units.iterator();

        Stmt returnStmt = null;
        while(unitIterator.hasNext()) {
            Stmt stmt = (Stmt) unitIterator.next();

            if (!(stmt instanceof ReturnVoidStmt)) {
                continue;
            }
            returnStmt = stmt;
        }


        GeExpr geExpr = Jimple.v().newGeExpr(counterLocal, configLocal);
        IfStmt ifStmt = Jimple.v().newIfStmt(geExpr, returnStmt);
        units.insertAfter(ifStmt, counterLocalInit);

        return stateSetterTest;
    }


    /**
     * Constructor for abstract mutation operator
     *
     * @param inputDir  Where the mutation operator will look for the compiled file
     * @param className Name of the class which will be mutated
     */
    protected SootMutationOperator(String inputDir, String className, String startMethod) {

        this.inputDir = inputDir;
        this.mainClass = setUp(inputDir, className);


        SootMethod targetMethod = this.mainClass.getMethod(startMethod);

        this.currentMethod = (Body) targetMethod.getActiveBody().clone();
        this.localToStmtMap = walk();

        this.threshold = 1;

        Scene.v().forceResolve("org.junit.jupiter.api", SootClass.BODIES);
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public void tryCatch(SootMethod test, Unit startingUnit, Unit endingUnit, List<Unit> catchBlock, Local caughtL) {
        assert catchBlock.size() > 0;
        // TODO assert for Exception class existence

        GotoStmt gotoStmt = Jimple.v().newGotoStmt(test.getActiveBody().getUnits().getSuccOf(endingUnit));

        // caughtL := @caughtexception;
        CaughtExceptionRef caughtRef = Jimple.v().newCaughtExceptionRef();
        IdentityStmt caughtStmt = Jimple.v().newIdentityStmt(caughtL, caughtRef);

        /*getUnits().insertAfter(gotoStmt, endingUnit);
        getUnits().insertAfter(caughtStmt, gotoStmt);
        getUnits().insertAfter(catchBlock, caughtStmt);*/

        test.getActiveBody().getUnits().insertAfter(gotoStmt, endingUnit);
        test.getActiveBody().getUnits().insertAfter(caughtStmt, gotoStmt);
        test.getActiveBody().getUnits().insertAfter(catchBlock, caughtStmt);

        SootClass exceptionClass = Scene.v().getSootClass(caughtL.getType().toString());
        Trap serverStartTrap = Jimple.v().newTrap(exceptionClass, startingUnit, gotoStmt, caughtStmt);

        test.getActiveBody().getTraps().add(serverStartTrap);
    }

    public void tryCatch(Unit startingUnit, Unit endingUnit, List<Unit> catchBlock, Local caughtL) {
        tryCatch(this.getCurrentMethod(), startingUnit, endingUnit, catchBlock, caughtL);
    }


    protected void addNonDeterminism(SootMethod method) {
        this.addNonDeterminism(method, this.threshold);
    }

    private void addNonDeterminism(SootMethod method, double threshold) {

        SootMethod getThresholdMethod = this.createGetThresholdMethod();

        try {
            this.getCurrentClass().addMethod(getThresholdMethod);
        } catch (Exception e) {
            getThresholdMethod = this.getCurrentClass().getMethod(getThresholdMethod.getSubSignature());
        }


        Local local = Jimple.v().newLocal("probability", DoubleType.v());
        method.getActiveBody().getLocals().add(local);
        SootMethod randomMethod = this.getRandomMethod();
        StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(randomMethod.makeRef());
        AssignStmt assignStmt = Jimple.v().newAssignStmt(local, staticInvokeExpr);
        this.InsertStmtBeginning(method, assignStmt);


        /*****/

        Local thisLocal = this.getOrCreateThisLocal(method);

        Local thresholdLocal = Jimple.v().newLocal("thresholdLocal", DoubleType.v());
        method.getActiveBody().getLocals().add(thresholdLocal);
        VirtualInvokeExpr getThresholdInvoke = Jimple.v().newVirtualInvokeExpr(thisLocal, getThresholdMethod.makeRef());
        AssignStmt initializeThresholdLocal = Jimple.v().newAssignStmt(thresholdLocal, getThresholdInvoke);
        this.InsertStmtBeginning(method, initializeThresholdLocal);
        /*******/

        SootMethod assertTrue = this.getAssertTrue();
        StaticInvokeExpr assertTrueExpr = Jimple.v().newStaticInvokeExpr(assertTrue.makeRef(), IntConstant.v(1));
        InvokeStmt assertTrueStmt = Jimple.v().newInvokeStmt(assertTrueExpr);
        this.InsertStmtEnd(method, assertTrueStmt);


        GeExpr geExpr = Jimple.v().newGeExpr(local, thresholdLocal);
        IfStmt ifStmt = Jimple.v().newIfStmt(geExpr, assertTrueStmt);
        method.getActiveBody().getUnits().insertAfter(ifStmt, assignStmt);
    }

    private void addNonDeterminism(SootMethod method, double threshold, Stmt pivot) {

        SootMethod getThresholdMethod = this.createGetThresholdMethod();


        try {
            this.getCurrentClass().addMethod(getThresholdMethod);
        } catch (Exception e) {
        }

        /*****/
        Local thisLocal = this.getOrCreateThisLocal(method);

        Local thresholdLocal = Jimple.v().newLocal("thresholdLocal", DoubleType.v());
        method.getActiveBody().getLocals().add(thresholdLocal);
        VirtualInvokeExpr getThresholdInvoke = Jimple.v().newVirtualInvokeExpr(thisLocal, getThresholdMethod.makeRef());
        AssignStmt initializeThresholdLocal = Jimple.v().newAssignStmt(thresholdLocal, getThresholdInvoke);
        this.InsertStmtBeginning(method, initializeThresholdLocal);
        /*******/

        Local local = Jimple.v().newLocal("probability", DoubleType.v());
        method.getActiveBody().getLocals().add(local);
        SootMethod randomMethod = this.getRandomMethod();
        StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(randomMethod.makeRef());
        AssignStmt assignStmt = Jimple.v().newAssignStmt(local, staticInvokeExpr);
        method.getActiveBody().getUnits().insertBefore(assignStmt, pivot);


        SootMethod assertTrue = this.getAssertTrue();
        StaticInvokeExpr assertTrueExpr = Jimple.v().newStaticInvokeExpr(assertTrue.makeRef(), IntConstant.v(1));
        InvokeStmt assertTrueStmt = Jimple.v().newInvokeStmt(assertTrueExpr);
        this.InsertStmtEnd(method, assertTrueStmt);


        GeExpr geExpr = Jimple.v().newGeExpr(local, thresholdLocal);
        IfStmt ifStmt = Jimple.v().newIfStmt(geExpr, assertTrueStmt);
        method.getActiveBody().getUnits().insertAfter(ifStmt, assignStmt);
    }

    protected void addNonDeterminism(SootMethod method, Stmt pivot) {
        addNonDeterminism(method, this.threshold, pivot);
    }


    protected SootMethod getRandomMethod() {
        SootMethod randomMethod = null;
        SootClass mathClass = Scene.v().forceResolve("java.lang.Math", SootClass.SIGNATURES);
        try {
            randomMethod = mathClass.getMethod("double random()");
        } catch (Exception e) {
            randomMethod = new SootMethod("random", new ArrayList<>(), DoubleType.v());
            randomMethod.setPhantom(true);
            mathClass.addMethod(randomMethod);
            return randomMethod;
        }
        return randomMethod;
    }

    protected Local getOrCreateThisLocal(SootMethod method) {

        Chain<Local> locals = method.getActiveBody().getLocals();

        Iterator<Local> localIterator = locals.snapshotIterator();

        while (localIterator.hasNext()) {
            Local local = localIterator.next();
            if (local.getType().equals(this.getCurrentClass().getType())) {
                return local;
            }
        }


        Local thisLocal = Jimple.v().newLocal("thisLocal", this.getCurrentClass().getType());
        method.getActiveBody().getLocals().add(thisLocal);
        ThisRef thisRef = Jimple.v().newThisRef(this.getCurrentClass().getType());
        IdentityStmt identityStmt = Jimple.v().newIdentityStmt(thisLocal, thisRef);
        method.getActiveBody().getUnits().addFirst(identityStmt);

        return thisLocal;
    }


    protected SootMutationOperator() {
    }

    @Override
    public String getCurrentMethodName() {
        return this.currentMethodSubsignature;
    }

    public void setCurrentMethod(String newMethod) {
        SootMethod targetMethod = null;
        targetMethod = this.mainClass.getMethod(newMethod);
        this.currentMethod = (Body) targetMethod.getActiveBody().clone();
        this.currentMethodSubsignature = targetMethod.getName();
        this.currentSootMethod = Scene.v().makeSootMethod(targetMethod.getName(), targetMethod.getParameterTypes(), targetMethod.getReturnType(), targetMethod.getModifiers(), targetMethod.getExceptions());
        this.currentSootMethod.setActiveBody(this.currentMethod);
        this.localToStmtMap = walk();
    }

    public void setCurrentMethod(SootMethod newMethod) {

        try {
            this.currentSootMethod = this.getCurrentClass().getMethodByName(newMethod.getName());
        } catch (Exception e) {
            newMethod.setDeclared(false);
            this.getCurrentClass().addMethod(newMethod);
            newMethod.setDeclaringClass(this.getCurrentClass());
            this.currentSootMethod = this.getCurrentClass().getMethodByName(newMethod.getName());
        }
        this.currentMethod = (Body)this.currentSootMethod.retrieveActiveBody().clone();
        /*this.currentMethod = (Body) newMethod.getActiveBody().clone();*/
        /*this.currentSootMethod = newMethod;*/
    }

    public void setCurrentClass(String inputDir, String className) {
        this.mainClass = setUp(inputDir, className);
        this.inputDir = inputDir;
    }

    public void setCurrentClass(SootClass currentClass) {
        this.mainClass = currentClass;
    }

    public SootClass getCurrentClass() {
        return this.mainClass;
    }

    private void initializeAllClasses() {
        Chain<SootClass> SootClasses  = Scene.v().getClasses();
        for (SootClass sootClass :SootClasses) {
            if (sootClass.getName().contains(this.mainClass.getName()) && !sootClass.getName().equals(this.mainClass.getName())) {
                Scene.v().forceResolve(sootClass.getName(), SootClass.HIERARCHY);
            }
        }
    }

    /**
     * When loading a class, soot only retrieves the skeleton (method signatures etc.) of the class.
     * Tying to execute a class mutated by soot throws a linker error because methods are not implemented.
     * This method retrieves active bodies of all methods the class that is being loaded
     * @param mainClass the mainClass loaded by the setUp method
     */
    protected void retrieveActiveAllMethods(SootClass mainClass) {
        for (SootMethod sm : mainClass.getMethods()) {
            if (!sm.isAbstract()) {
                sm.setActiveBody(sm.retrieveActiveBody());
            }
        }
    }

    protected SootMethod getCurrentMethod() {
        return this.currentSootMethod;
    }

    public SootMethod  createMethod(String methodName, List<Stmt> execStmts, Value returnValue, List<Type> params) {

        SootMethod method;
        //create the test signature
        if (returnValue == null)
        {
            method = Scene.v().makeSootMethod(methodName, params, VoidType.v(), Modifier.PUBLIC);
        }
        else {
            method = Scene.v().makeSootMethod(methodName, params, returnValue.getType(), Modifier.PUBLIC);
        }
        //create body for the new test
        JimpleBody body = Jimple.v().newBody(method);

        //add "this" local to the test because it is not static
        method.setActiveBody(body);
        Local thisLocal = Jimple.v().newLocal("thisLocal", RefType.v(methodName));
        body.getLocals().add(thisLocal);
        IdentityStmt thisStmt = Jimple.v().newIdentityStmt(thisLocal, Jimple.v().newThisRef(RefType.v(methodName)));
        body.getUnits().add(thisStmt);

        //add return statement to the test
        if (returnValue == null) {
            body.getUnits().add(Jimple.v().newReturnVoidStmt());
        }
        else {
            if (returnValue instanceof Local) {
                body.getLocals().add((Local)returnValue);
            }
            body.getUnits().add(Jimple.v().newReturnStmt(returnValue));
        }

        for (Stmt execStmt : execStmts) {
            this.InsertStmtEnd(method, execStmt);
        }

        return method;
    }

    public SootMethod createSootMethod(SootMethod original, String methodName) {
        SootMethod newSootMethod = Scene.v().makeSootMethod(methodName, original.getParameterTypes(), original.getReturnType(), original.getModifiers(), original.getExceptions());
        newSootMethod.setActiveBody((Body) this.currentMethod.clone());
        return newSootMethod;
    }

    public void createAndAddTest(String methodName) {
        SootMethod newSootMethod = createSootMethod(this.getCurrentMethod(), methodName);
        newSootMethod.addTag(createTestAnnotation());

        try {
            this.getCurrentClass().addMethod(newSootMethod);
        } catch (Exception e) {

        }
    }

    public void exportClass(String outputDir, SootClass sootClass) throws IOException {


        Scene.v().forceResolve("com.fasterxml.jackson.core.util.Named", SootClass.SIGNATURES);
        Options.v().set_output_dir(outputDir);
        String fileName = SourceLocator.v().getFileNameFor(sootClass, Options.output_format_class);

        OutputStream streamOut = new JasminOutputStream(
            new FileOutputStream(fileName));
        PrintWriter writerOut = new PrintWriter(
            new OutputStreamWriter(streamOut));
        JasminClass jasminClass = new soot.jimple.JasminClass(sootClass);
        jasminClass.print(writerOut);
        writerOut.flush();
        streamOut.close();
    }

    protected String getClassName() {
        return SourceLocator.v().getFileNameFor(mainClass, Options.output_format_class);
    }

    public void exportClass(String outputDir) throws IOException {
        exportSubclasses(outputDir);
        Options.v().set_output_dir(outputDir);
        String fileName = SourceLocator.v().getFileNameFor(mainClass, Options.output_format_class);
        OutputStream streamOut = new JasminOutputStream(
            new FileOutputStream(fileName));
        PrintWriter writerOut = new PrintWriter(
            new OutputStreamWriter(streamOut));
        JasminClass jasminClass = new soot.jimple.JasminClass(mainClass);
        jasminClass.print(writerOut);
        writerOut.flush();
        streamOut.close();
    }

    public SootMethod createTestMethod(String testName) {

        testName = testName + this.getClass().getSimpleName();

        //create the test signature
        SootMethod newTest = Scene.v().makeSootMethod(testName, new LinkedList<>(), VoidType.v(), Modifier.PUBLIC);
        //create body for the new test
        JimpleBody body = Jimple.v().newBody(newTest);

        //add "this" local to the test because it is not static
        newTest.setActiveBody(body);
        Local thisLocal = Jimple.v().newLocal("thisLocal", RefType.v(this.getCurrentClass().getName()));
        body.getLocals().add(thisLocal);
        IdentityStmt thisStmt = Jimple.v().newIdentityStmt(thisLocal, Jimple.v().newThisRef(RefType.v(this.getCurrentClass().getName())));
        body.getUnits().add(thisStmt);

        //add return statement to the test
        body.getUnits().add(Jimple.v().newReturnVoidStmt());

        //add test annotation
        Tag testAnnotation = createTestAnnotation();
        newTest.addTag(testAnnotation);

        return newTest;
    }

    public void addGroundTruthAnnotation() {

        SootClass sootClass = this.getCurrentClass();


        if (this.jupiter) {
            /*
            Annotation: type: Lorg/junit/jupiter/api/TestMethodOrder; num elems: 1 elems:
Annotation Element: kind: c name: value decription: Lorg/junit/jupiter/api/MethodOrderer$MethodName;

             */

            for (Tag tag: sootClass.getTags()) {
                String tagName = tag.toString();
                if (tagName.contains("TestMethodOrder")) {
                    return;
                }
            }
            AnnotationTag annotationTag = new AnnotationTag("Lorg/junit/jupiter/api/TestMethodOrder;");
            /*
            Annotation Element: kind: c name: value decription: Lorg/junit/jupiter/api/MethodOrderer$MethodName;
             */
            AnnotationElem annotationClassElem = new AnnotationClassElem(
                "Lorg/junit/jupiter/api/MethodOrderer$MethodName;",
                'c',
                "value"
            );
            annotationTag.addElem(annotationClassElem);
            VisibilityAnnotationTag tag = new VisibilityAnnotationTag(0);
            tag.addAnnotation(annotationTag);
            sootClass.addTag(tag);
        } else {
            for (Tag tag: sootClass.getTags()) {
                String tagName = tag.toString();
                if (tagName.contains("FixMethodOrder")) {
                    return;
                }
            }
            AnnotationTag annotationTag = new AnnotationTag("Lorg/junit/FixMethodOrder;");
            AnnotationElem annotationElem = new AnnotationEnumElem(
                "Lorg/junit/runners/MethodSorters;",
                "NAME_ASCENDING",
                'e',
                "value"
            );

            annotationTag.addElem(annotationElem);
            VisibilityAnnotationTag tag = new VisibilityAnnotationTag(0);
            tag.addAnnotation(annotationTag);

            sootClass.addTag(tag);
        }

    }

    protected Tag createTestAnnotation() {

        if (jupiter) {
            AnnotationTag annotationTag = new AnnotationTag("Lorg/junit/jupiter/api/Test;");
            VisibilityAnnotationTag tag = new VisibilityAnnotationTag(0);
            tag.addAnnotation(annotationTag);
            return tag;
        } else {
            AnnotationTag annotationTag = new AnnotationTag("Lorg/junit/Test;", null);
            VisibilityAnnotationTag visibilityAnnotationTag = new VisibilityAnnotationTag(0);
            visibilityAnnotationTag.addAnnotation(annotationTag);
            return visibilityAnnotationTag;
        }

    }

    protected Tag createTestAnnotationWithTimeout(long timeoutValue) {

        String type = "Lorg/junit/Test;";
        AnnotationLongElem[] timeOutElem = new AnnotationLongElem[]{new AnnotationLongElem(timeoutValue, 'J', "timeout")};
        AnnotationTag annotationTag = new AnnotationTag(type, Arrays.asList(timeOutElem));
        VisibilityAnnotationTag visibilityAnnotationTag = new VisibilityAnnotationTag(0);
        visibilityAnnotationTag.addAnnotation(annotationTag);
        return visibilityAnnotationTag;

    }

    /**
     * Exports mutated version of the class as a class file
     *
     * @param outputDir the directory in which the exported class will be located in
     * @throws IOException throws an exception when the directory is not found
     */
    @Override
    public void exportClass(String outputDir, String methodName) throws IOException {
        createAndAddTest(methodName);
        for (SootMethod sm : this.getCurrentClass().getMethods()) {
            if (!sm.getSignature().contains("Mutant")) {
                sm.setActiveBody(sm.retrieveActiveBody());
            }
        }
        exportClass(outputDir);
    }

    public void renameSubclasses(String fileName) {
        Chain<SootClass> classes = Scene.v().getClasses();
        Iterator<SootClass> itr = classes.snapshotIterator();
        while (itr.hasNext()) {
            SootClass sootClass = itr.next();

            try {
                if (sootClass.hasOuterClass() && sootClass.getOuterClass().equals(this.mainClass)) {

                    Scene.v().forceResolve(sootClass.getName(), SootClass.BODIES);
                    retrieveActiveAllMethods(sootClass);
                    String subClassName = sootClass.getName();
                    String[] subClassNameParsed = sootClass.getName().split("\\$");
                    subClassName = fileName + "$" + subClassNameParsed[1];
                    sootClass.rename(subClassName);
                }
            } catch (Exception e) {
                continue;
            }
        }
    }

    private List<SootClass> getSubclasses() {
        List<SootClass> subclassses = new ArrayList<>();
        for (SootClass sootClass: Scene.v().getClasses()){
            if (!sootClass.toString().contains(this.mainClass.getName())) {
                continue;
            }

            if (sootClass.toString().equals(this.mainClass.getName())) {
                continue;
            }

            String[] subclassNameParsed = sootClass.toString().split("\\$");
            String subclassName = subclassNameParsed[subclassNameParsed.length-1];

            if (!subclassName.matches("[0-9]*")) {
                subclassses.add(sootClass);
            }
        }
        return subclassses;
    }

    public String getPreMutationName(String mutatedName) {
        String nameWithPackage =  mutatedName.replaceAll("Mutant[0-9]*Test", "");
        String[] parsedNameWithPackage = nameWithPackage.split("\\.");
        String preMutationName = parsedNameWithPackage[parsedNameWithPackage.length-1];
        return preMutationName;
    }

    public void transformSubclassVirtualInvokeMethod(SootMethod sootMethod) {
        Chain<Unit> units = sootMethod.getActiveBody().getUnits();
        Iterator<Unit> itr = units.snapshotIterator();

        while(itr.hasNext()) {
            Stmt stmt = (Stmt) itr.next();
            if (!(stmt.containsInvokeExpr())) {
                continue;
            }

            InvokeExpr invokeExpr = stmt.getInvokeExpr();

            if (!(invokeExpr instanceof VirtualInvokeExpr)) {
                continue;
            }

            VirtualInvokeExpr virtualInvokeExpr = (VirtualInvokeExpr) invokeExpr;

            for (SootClass subClass: getSubclasses()) {
                retrieveActiveAllMethods(subClass);

                if (virtualInvokeExpr.getMethod().getSignature().contains(subClass.getName())) {
                    String subSignature = virtualInvokeExpr.getMethod().getSubSignature();
                    SootMethod newMethod = subClass.getMethod(subSignature);
                    virtualInvokeExpr.setMethodRef(newMethod.makeRef());
                }
            }
        }
    }

    private void transformSubclassVirtualInvoke() {
        retrieveActiveAllMethods(this.mainClass);
        for (SootMethod sootMethod : this.mainClass.getMethods()) {
            transformSubclassVirtualInvokeMethod(sootMethod);
        }
    }

    private void transformSubclassCastMethod (SootMethod sootMethod) {
        Chain<Unit> units = sootMethod.getActiveBody().getUnits();
        Iterator<Unit> itr = units.snapshotIterator();

        while(itr.hasNext()) {
            Stmt stmt = (Stmt) itr.next();
            if (!(stmt instanceof AssignStmt)) {
                continue;
            }

            AssignStmt assignStmt = (AssignStmt) stmt;

            if (!(assignStmt.getRightOp() instanceof JCastExpr)) {
                continue;
            }

            JCastExpr jCastExpr = (JCastExpr) assignStmt.getRightOp();


            for (SootClass subClass: getSubclasses()) {
                String nonMutantName = subClass.getName().replaceAll("MutantTest[0-9]*", "");

                if (jCastExpr.getCastType().toString().equals(subClass.getName())) {
                    jCastExpr.setCastType(subClass.getType());
                }
            }
        }
    }
    private void transformSubclassCast() {
        retrieveActiveAllMethods(this.mainClass);
        for (SootMethod sootMethod : this.mainClass.getMethods()) {
            transformSubclassCastMethod(sootMethod);
        }
    }

    private void transformSubclassAssignStmt(Stmt stmt) {
        Value value = ((AssignStmt) stmt).getRightOp();
        if (value instanceof InstanceInvokeExpr) {
            transformSubclassInstanceInvokeExpr((InstanceInvokeExpr) value);
        }
        else if (value instanceof StaticInvokeExpr) {
            transformSubclassStaticInvokeExpr((StaticInvokeExpr) value);
        }
    }

    private void transformSubclassInstanceInvokeExpr(InstanceInvokeExpr instanceInvokeExpr) {
        if (doArgumentsContainSubclass(instanceInvokeExpr)) {
            String preMutationName = getPreMutationName(this.mainClass.getName());
            List<Value> args = instanceInvokeExpr.getArgs();
            for (int i = 0; i < args.size(); i++) {
                if (!args.get(i).toString().contains(preMutationName)) {
                    continue;
                }
                if (!(args.get(i) instanceof ClassConstant)) {
                    continue;
                }

                String classConstantValue = ((ClassConstant) args.get(i)).value;
                String[] newNameParsed = this.className.split("\\.");
                String newName = newNameParsed[newNameParsed.length-1];
                classConstantValue = classConstantValue.replaceAll(preMutationName+"[Mutant]*[0-9]*[Test]*\\$", newName+"\\$");
                instanceInvokeExpr.setArg(i, ClassConstant.v(classConstantValue));
            }
        }
    }

    private void transformSubclassStaticInvokeExpr(StaticInvokeExpr staticInvokeExpr) {
        if (doArgumentsContainSubclass(staticInvokeExpr)) {
            String mutantSubsignature = staticInvokeExpr.getMethod().getSubSignature().replaceFirst(getPreMutationName(this.mainClass.getName()),this.mainClass.getName());
            SootMethod sootMethod = null;
            try {
                sootMethod = this.mainClass.getMethod(mutantSubsignature);
                staticInvokeExpr.setMethodRef(sootMethod.makeRef());
            } catch (Exception e) {
                System.out.println(mutantSubsignature+" is not included in the test suite itself");
            }

        }
    }

    private boolean isBaseSubclass(InstanceInvokeExpr instanceInvokeExpr) {
        Value value = instanceInvokeExpr.getBase();

        if (!(value instanceof Local)) {
            return false;
        }

        Local local = (Local) value;
        for (SootClass subClass: getSubclasses()) {
            String preMutationName = getPreMutationName(subClass.getName());
            if (local.getType().toString().equals(preMutationName)) {
                return true;
            }
        }
        return false;
    }

    private boolean doArgumentsContainSubclass(InvokeExpr invokeExpr) {
        List<Value> args = invokeExpr.getArgs();
        List<SootClass> subclasses = getSubclasses();
        for (Value arg: args) {
            for (SootClass subClass: getSubclasses()) {
                String[] subClassNameParsed = subClass.getName().split("\\$");
                String subClassName = subClassNameParsed[subClassNameParsed.length-1];
                String[] argNameParsed = arg.toString().split("\\$");
                String argName = argNameParsed[argNameParsed.length-1];
                if (argName.contains(subClassName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void transformSubclassMethod(SootMethod sootMethod) {
        Chain<Unit> units = sootMethod.getActiveBody().getUnits();
        Iterator<Unit> itr = units.snapshotIterator();

        while(itr.hasNext()) {
            Stmt stmt = (Stmt) itr.next();

            if (stmt instanceof AssignStmt) {
                transformSubclassAssignStmt(stmt);
            }
            else if (stmt.containsInvokeExpr()) {
                if (stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
                    transformSubclassInstanceInvokeExpr((InstanceInvokeExpr) stmt.getInvokeExpr());
                }
                else if (stmt.getInvokeExpr() instanceof StaticInvokeExpr) {
                    transformSubclassStaticInvokeExpr((StaticInvokeExpr) stmt.getInvokeExpr());
                }
            }
        }
    }

    private void transformSubclassMethods() {
        retrieveActiveAllMethods(this.mainClass);
        for (SootMethod sootMethod : this.mainClass.getMethods()) {
            if (!sootMethod.isAbstract()) {
                transformSubclassMethod(sootMethod);
            }
        }
    }



    /**
     * Setup sets necessary settings, loads necessary classes, loads the class which will be mutated.
     * Then it invokes retrieveActiveAllMethods to avoid linker errors
     * It also adds basic class java.lang.System for insertion methods such as System.out.println
     * @param inputDir The directory in which the class file exits (the one that will be mutated)
     * @param className Name of the class that will be mutated
     * @return SootClass object that has been prepared to mutate
     */
    protected SootClass setUp(String inputDir, String className) {
        //setting options
        G.reset();

        Options.v().setPhaseOption("jb.tr", "ignore-wrong-staticness:true");
        Options.v().set_prepend_classpath(true);
        //
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_soot_classpath(inputDir);
        // if you do not use set_output_dir setting,
        // soot creates a sootOutput folder and puts exported class files there
        SootClass sc = Scene.v().loadClassAndSupport(className);
        sc.setApplicationClass();
        Scene.v().loadNecessaryClasses();
        Scene.v().addBasicClass("java.lang.System",SootClass.SIGNATURES);


        //loading soot class from scene
        SootClass mainClass = Scene.v().getSootClass(className);

        retrieveActiveAllMethods(mainClass);
        //initializeAllClasses();


        return mainClass;
    }


    /**
     * Locates the first occurrence of method invocation according to search parameters and returns a Unit object.
     * Returned object can be later passed as an argument for other methods to be mutated or be used for location for insertions.
     * @param invocMethod Name of the method whose invocation is sought within the body of mainMethod
     * @param criteria keyword that the string representation of invocMethod needs to contain. Can be used to distinguish between mutliple invocations of the same method
     * @return Returns the unit of the invocation
     */
    protected Unit locateMethodFirst(String invocMethod, String criteria) {
        Body mainMethodBody = this.currentMethod;

        Iterator<Unit> iter = mainMethodBody.getUnits().snapshotIterator();
        while(iter.hasNext()) {
            Unit currentUnit = iter.next();
            Stmt currentStmt = (Stmt) currentUnit;
            if (!currentStmt.containsInvokeExpr()) {
                continue;
            }

            InvokeExpr expr = currentStmt.getInvokeExpr();
            if (expr instanceof StaticInvokeExpr) {
                if (expr.toString().contains(invocMethod) && expr.toString().contains(criteria)) {
                    return currentUnit;
                }
            }

        }
        return null;
    }

    /**
     * Locates the first occurrence of method invocation and returns a Unit object.
     * returned object can be later passed as an argument for other methods to be mutated or be used for location for insertions.
     * @param invocMethod Name of the method whose invocation is sought within the body of mainMethod
     * @return Returns the unit of the invocation
     */
    protected Unit locateMethodFirst(String invocMethod) {
        return locateMethodFirst(invocMethod, "");
    }

    /**
     * Mutates the argument passed to a certain method invocation.
     * @param methodInvocation the method that is going to be mutated. The unit object can be located using locateMethod methods
     * @param index The index that is going to be mutated
     * @param argument The argument which will replace the argument passed to that index.
     *                 Due to polymorphism, this argument can be a constant or a local object.
     *                 If this argument is a constant such as StringConstant the mutation will look like foo("a"); => foo("b");
     *                 If this argument is a local, then the mutation will look like int x=1; foo(x); => int x = 1; int y = 2; foo(y);
     *                 Keep in mind that primitive value argument mutations should be done with new locals or mutating initializations of locals
     *                 and string argument mutations can be carried out by using constants
     * @throws Exception Throws an exception methodInvocation argument is invalid (does not contain a method invocation)
     */
    protected void changeArgument(Unit methodInvocation, Integer index, Value argument) throws Exception {
        Stmt methodInvocStmt = (Stmt) methodInvocation;
        if (!methodInvocStmt.containsInvokeExpr()) {
            throw new Exception("unit does not contain an invocation expression");
        }
        InvokeExpr methodExpr = methodInvocStmt.getInvokeExpr();
        methodExpr.setArg(index, argument);
    }

    /**
     * Returns a list of arguments passed into a method
     * @param methodInvocation arguments of methodInvocation will be returned
     * @return List<Value> which contains all arguments passed down to the method invocation.
     *         Values might either be constants or locals
     * @throws Exception Throws exception when methodInvocation argument is invalid (does not contain an invocation)
     */
    protected List<Value> getArguments(Unit methodInvocation) throws Exception {
        Stmt methodInvocStmt = (Stmt) methodInvocation;
        if (!methodInvocStmt.containsInvokeExpr()) {
            throw new Exception("unit does not contain an invocation expression");
        }
        InvokeExpr methodExpr = methodInvocStmt.getInvokeExpr();
        return methodExpr.getArgs();
    }

    /**
     * Locates the last occurrence of method invocation according to search parameters and returns a Unit object.
     * Returned object can be later passed as an argument for other methods to be mutated or be used for location for insertions.
     * @param regexExp keyword that the string representation of invocMethod needs to contain. Can be used to distinguish between mutliple invocations of the same method
     * @return Returns the unit of the invocation
     */
    protected Unit locateMethodLast(Pattern regexExp) {
        Body mainMethodBody = this.currentMethod;
        Unit lastUnit = null;

        Iterator<Unit> iter = mainMethodBody.getUnits().snapshotIterator();

        while(iter.hasNext()) {
            Unit currentUnit = iter.next();
            Stmt currentStmt = (Stmt) currentUnit;

            if (!currentStmt.containsInvokeExpr()) {
                continue;
            }

            InvokeExpr expr = currentStmt.getInvokeExpr();
            if (expr instanceof StaticInvokeExpr) {
                if (regexExp.matcher(expr.toString()).matches()) {
                    lastUnit = currentUnit;
                }
            }

        }
        return lastUnit;
    }

    /**
     * Locates the first occurrence of method invocation and returns a Unit object.
     * eturned object can be later passed as an argument for other methods to be mutated or be used for location for insertions.
     * @param invocMethod Name of the method whose invocation is sought within the body of mainMethod
     * @return Returns the unit of the invocation
     */
    protected Unit locateMethodLast(String invocMethod) {
        return locateMethodLast(Pattern.compile(invocMethod));
    }

    /**
     * Locates the all occurrences of method invocation according to search parameters and returns a Unit object.
     * Returned object can be later passed as an argument for other methods to be mutated or be used for location for insertions.
     * @param regexExp keyword that the string representation of invocMethod needs to contain. Can be used to distinguish between mutliple invocations of the same method
     * @return Returns the list of unit of invocations
     */
    protected List<Unit> locateMethodAll(Pattern regexExp) {
        Body mainMethodBody = this.currentMethod;
        List<Unit> allInvocations = new ArrayList<>();

        Iterator<Unit> iter = mainMethodBody.getUnits().snapshotIterator();
        while(iter.hasNext()) {
            Unit currentUnit = iter.next();
            Stmt currentStmt = (Stmt) currentUnit;

            if (!currentStmt.containsInvokeExpr()) {
                continue;
            }

            InvokeExpr expr = currentStmt.getInvokeExpr();
            if (expr instanceof StaticInvokeExpr) {
                if (regexExp.matcher(expr.toString()).matches()) {
                    allInvocations.add(currentUnit);
                }
            }

        }
        return allInvocations;
    }

    /**
     * Locates the all occurrence of method invocation and returns a Unit object.
     * eturned object can be later passed as an argument for other methods to be mutated or be used for location for insertions.
     * @param mainMethod mainMethod's body will be analyzed to find where invocMethod is invoked
     * @param invocMethod Name of the method whose invocation is sought within the body of mainMethod
     * @return Returns all units of invocations
     */
    protected List<Unit> locateMethodAll(String mainMethod, String invocMethod) {
        return locateMethodAll(Pattern.compile(invocMethod));
    }

    /**
     * Inserts a statement to the beginning of the body of the target method
     * @param stmt Statement that is going to be inserted
     */
    public void InsertStmtBeginning(Stmt stmt) {
        Chain<Unit> units = getUnits();
        Iterator<Unit> itr = units.snapshotIterator();
        Unit pivot = null;

        while (itr.hasNext()) {
            Stmt currentStmt = (Stmt) itr.next();

            if (currentStmt instanceof JIdentityStmt) {
                pivot = currentStmt;
            } else {
                break;
            }

        }

        if (pivot == null) {
            itr = units.snapshotIterator();
            pivot = itr.next();
        }
        units.insertAfter(stmt, pivot);
    }

    public void InsertStmtBeginning(SootMethod test, Stmt stmt) {
        //TODO: merge with other InsertStmtBeginning
        Chain<Unit> units = test.getActiveBody().getUnits();
        Iterator<Unit> itr = units.snapshotIterator();

        while(itr.hasNext()) {
            Stmt currentStmt = (Stmt)itr.next();

            if (currentStmt instanceof IdentityStmt) {
                continue;
            }

            units.insertBefore(stmt, currentStmt);
            break;
        }
    }

    public void InsertStmtBeginning(Body body, Stmt stmt) {
        //TODO: merge with other InsertStmtBeginning
        Chain<Unit> units = body.getUnits();
        Iterator<Unit> itr = units.snapshotIterator();
        Unit pivot = null;

        while(itr.hasNext()) {
            Stmt currentStmt = (Stmt)itr.next();

            if (currentStmt instanceof IdentityStmt)  {
                pivot = currentStmt;
            }
        }
        units.insertAfter(stmt, pivot);
    }

    /**
     * Inserts a statement to the end of the body of the target method
     * @param stmt Statement that is going to be inserted
     */
    public void InsertStmtEnd(Stmt stmt) {
        Chain<Unit> units = getUnits();
        Iterator<Unit> itr = units.snapshotIterator();
        Stmt currentStmt = null;
        while(itr.hasNext()) {
            currentStmt = (Stmt)itr.next();

            if (!currentStmt.containsInvokeExpr()) {
                continue;
            }

            InvokeExpr expr = currentStmt.getInvokeExpr();
        }

        units.insertBefore(stmt, currentStmt);
    }

    public void InsertStmtEnd(SootMethod sootMethod, Stmt stmt) {
        InsertStmtEnd(sootMethod.getActiveBody(), stmt);
    }

    public void InsertStmtEnd(Body body, Stmt stmt) {
        Chain<Unit> units = body.getUnits();
        Iterator<Unit> itr = units.snapshotIterator();
        Stmt currentStmt = null;
        while(itr.hasNext()) {
            currentStmt = (Stmt)itr.next();

            if (!currentStmt.containsInvokeExpr()) {
                continue;
            }

            InvokeExpr expr = currentStmt.getInvokeExpr();
        }

        units.insertBefore(stmt, currentStmt);

    }

    /**
     * Creates a new variable and initializes it
     * @param variable a simpleEntry object, the key needs to be the type of the variable and value needs to be the value of the variable
     * @return returns the local of the newly inserted variable which can be then used for further mutations such as changing argument passed to a method invocation
     */
    public Local newPrimitiveLocal(AbstractMap.SimpleEntry<String, String> variable) {
        Body targetMethodBody = this.currentMethod;
        Local arg;
        String type, value;
        SootMethod initMethod;
        StaticInvokeExpr initExpr;
        Stmt initStmt;


        type = variable.getKey();
        value = variable.getValue();

        switch(type) {
            case "Integer":
                arg = Jimple.v().newLocal("newlyInserted", RefType.v("java.lang.Integer"));
                initMethod = Scene.v().getMethod("<java.lang.Integer: java.lang.Integer valueOf(int)>");
                initExpr = Jimple.v().newStaticInvokeExpr(initMethod.makeRef(), IntConstant.v(Integer.parseInt(value)));
                break;

            case "Short":
                arg = Jimple.v().newLocal("newlyInserted", RefType.v("java.lang.Short"));
                initMethod = Scene.v().getMethod("<java.lang.Short: java.lang.Short valueOf(short)>");
                initExpr = Jimple.v().newStaticInvokeExpr(initMethod.makeRef(), IntConstant.v(Integer.parseInt(value)));
                break;

            case "Long":
                arg = Jimple.v().newLocal("newlyInserted", RefType.v("java.lang.Long"));
                initMethod = Scene.v().getMethod("<java.lang.Long: java.lang.Long valueOf(long)>");
                initExpr = Jimple.v().newStaticInvokeExpr(initMethod.makeRef(), LongConstant.v(Long.parseLong(value)));
                break;

            case "Float":
                arg = Jimple.v().newLocal("newlyInserted", RefType.v("java.lang.Float"));
                initMethod = Scene.v().getMethod("<java.lang.Float: java.lang.Float valueOf(float)>");
                initExpr = Jimple.v().newStaticInvokeExpr(initMethod.makeRef(), FloatConstant.v(Float.parseFloat(value)));
                break;

            case "Double":
                arg = Jimple.v().newLocal("newlyInserted", RefType.v("java.lang.Double"));
                initMethod = Scene.v().getMethod("<java.lang.Double: java.lang.Double valueOf(double)>");
                initExpr = Jimple.v().newStaticInvokeExpr(initMethod.makeRef(), DoubleConstant.v(Double.parseDouble(value)));
                break;

            case "Boolean":
                arg = Jimple.v().newLocal("newlyInserted", RefType.v("java.lang.Boolean"));
                initMethod = Scene.v().getMethod("<java.lang.Boolean: java.lang.Boolean valueOf(boolean)>");
                initExpr = Jimple.v().newStaticInvokeExpr(initMethod.makeRef(), IntConstant.v(Integer.parseInt(value)));
                break;

            case "Character":
                arg = Jimple.v().newLocal("newlyInserted", RefType.v("java.lang.Character"));
                initMethod = Scene.v().getMethod("<java.lang.Character: java.lang.Character valueOf(char)>");
                initExpr = Jimple.v().newStaticInvokeExpr(initMethod.makeRef(), IntConstant.v(value.charAt(0)));
                break;

            default:
                throw new IllegalStateException("Unexpected value: " + type);
        }


        initStmt = Jimple.v().newAssignStmt(arg, initExpr);
        InsertStmtBeginning(initStmt);
        targetMethodBody.getLocals().addFirst(arg);
        return arg;

    }

    /**
     * Used to get the body of the target method
     * @return returns the jimple body of the target method
     */
    public Body getJimpleBody() {
        return this.currentMethod;
    }

    /**
     * Creates a method invocation statement which can be later used for subsequent mutations
     * such as inserting a new method invocation
     * @param methodToInvoke name of the method which is going to be invokes
     * @param arguments arguments that will be passed on the the invocation
     * @return Stmt (statement) of the method invocation which can be later used for subsequent mutations
     */
    protected Stmt createMethodStmt(String methodToInvoke, List<Value> arguments) {
        SootMethod method = this.mainClass.getMethod(methodToInvoke);
        InvokeExpr methodExpr = Jimple.v().newStaticInvokeExpr(method.makeRef(), arguments);

        return Jimple.v().newInvokeStmt(methodExpr);
    }

    /**
     * Get units of a method which can later be used for iteration over units
     * @return Chain object of all units in the target method
     */
    public Chain<Unit> getUnits() {
        Body targetMethodBody = this.currentMethod;
        return targetMethodBody.getUnits();
    }

    /**
     * Inserts method statement just before the return statement
     * @param targetMethodName name of the target method
     * @param methodToInvoke the method which will be invoked
     * @param arguments arguments that will be passed to the method
     * @return Stmt (statement) of the inserted method
     */
    protected Stmt insertMethodLast(String targetMethodName, String methodToInvoke, List<Value> arguments) {
        Stmt methodStmt = createMethodStmt(methodToInvoke, arguments);
        Body targetMethodBody = this.currentMethod;
        InsertStmtEnd(methodStmt);
        return methodStmt;
    }

    /**
     * Inserts method statement to the beginning of the body
     * @param targetMethodName name of the target method
     * @param methodToInvoke the method which will be invoked
     * @param arguments arguments that will be passed to the method
     * @return Stmt (statement) of the inserted method
     */
    protected Stmt insertMethodBeginning(String targetMethodName, String methodToInvoke, List<Value> arguments) {
        Stmt methodStmt = createMethodStmt(methodToInvoke, arguments);
        Body targetMethodBody = this.currentMethod;
        InsertStmtBeginning(methodStmt);
        return methodStmt;
    }

    /**
     * Inserts method statement after the pivot
     * @param methodToInvoke the method which will be invoked
     * @param arguments arguments that will be passed to the method
     * @param pivot Statement which will be used as a location for insertion.
     *              Insertion will be made just after this statement
     * @return Stmt (statement) of the inserted method
     */
    protected Stmt insertMethodAfter(String methodToInvoke, List<Value> arguments, Stmt pivot) {
        Stmt methodStmt = createMethodStmt(methodToInvoke, arguments);
        Chain<Unit> targetMethodUnits = getUnits();
        targetMethodUnits.insertAfter(methodStmt, pivot);
        return methodStmt;
    }

    /**
     * Inserts method statement after the pivot
     * @param methodToInvoke the method which will be invoked
     * @param arguments arguments that will be passed to the method
     * @param pivot Statement which will be used as a location for insertion.
     *              Insertion will be made just before this statement
     * @return Stmt (statement) of the inserted method
     */
    protected Stmt insertMethodBefore(String methodToInvoke, List<Value> arguments, Stmt pivot) {
        Stmt methodStmt = createMethodStmt(methodToInvoke, arguments);
        Chain<Unit> targetMethodUnits = getUnits();
        targetMethodUnits.insertBefore(methodStmt, pivot);
        return methodStmt;
    }

    /**
     * Inserts method statement after the pivot
     * @param targetMethodStmt the method which will be invoked
     * @param pivot Statement which will be used as a location for insertion.
     *              Insertion will be made just before this statement
     */
    public void insertMethodAfter(Stmt targetMethodStmt, Stmt pivot) {
        Chain<Unit> chain = getUnits();
        chain.insertAfter(targetMethodStmt,pivot);
    }

    /**
     * Changes a datastructure from oldDataStructure to newDataStructure such as changing LinkedHashSet to HashSet
     * @param concreteVariable lvalue instance associated with the declaration and initialization of old data structure
     * @param ephemeralVariable rvalue instance associated with the declaration and initialization of old data structure
     * @param oldDataStructure the old datastructure name (such as java.util.HashSet)
     * @param newDataStructure the new datastructure name (such as java.util.HashSet)
     */
    public void changeDataStructure(Local concreteVariable, Local ephemeralVariable, String oldDataStructure, String newDataStructure) {
        Body targetMethodBody = this.currentMethod;

        //Change the declaration
        concreteVariable.setType(RefType.v(newDataStructure));
        ephemeralVariable.setType(RefType.v(newDataStructure));


        //Change initialization

        Chain<Unit> units = targetMethodBody.getUnits();
        Iterator<Unit> itr = units.snapshotIterator();

        while(itr.hasNext()) {
            Stmt stmt = (Stmt) itr.next();

            //Change SpecialInvokeExpr (specialinvoke $r0.<java.util.LinkedHashSet: void <init>()>();)
            if (stmt.containsInvokeExpr() && (stmt.getInvokeExpr()) instanceof SpecialInvokeExpr) {
                SpecialInvokeExpr expr = (SpecialInvokeExpr) stmt.getInvokeExpr();
                SootMethod newInit = Scene.v().getMethod("<"+newDataStructure+": void <init>()>");
                expr.setMethodRef(newInit.makeRef());
            }

            //Change new expression ($r0 = new java.util.LinkedHashSet;)
            if (stmt instanceof AssignStmt) {
                if (((AssignStmt) stmt).getRightOp().toString().contains("new "+oldDataStructure)) {
                    if (((AssignStmt) stmt).getLeftOp().equals(ephemeralVariable)) {
                        AssignStmt newStmt = (AssignStmt) stmt;
                        SootClass newDataStructureClass =  Scene.v().getSootClass(newDataStructure);
                        JNewExpr newExpr = new JNewExpr(newDataStructureClass.getType());
                        newStmt.setRightOp(newExpr);
                    }
                }
            }

            //Change function calls (virtualinvoke r1.<java.util.LinkedHashSet: boolean add(java.lang.Object)>("Alperen");)
            if (stmt.containsInvokeExpr() && stmt.getInvokeExpr() instanceof VirtualInvokeExpr) {
                VirtualInvokeExpr currentVirtualInvoke = (VirtualInvokeExpr) stmt.getInvokeExpr();
                if (currentVirtualInvoke.getBase() == concreteVariable) {
                    currentVirtualInvoke.setMethodRef(currentVirtualInvoke.getMethod().makeRef());

                }
            }
        }
    }

    /**
     * Creates a hashmap that has locals as key and values as methods that are associated with that local
     * (local is an argument of a Virtual / Static invocation expression or it is the base of a virtual invocation or it is left/right operand of an assign statement)
     * @return hashmap that has locals as key and values as methods that are associated with that local
     */
    public HashMap<Local, List<Stmt>> walk() {
        HashMap<Local, List<Stmt>> LocalToMethodMap = new HashMap<>();
        for (Local local: getJimpleBody().getLocals()) {
            LocalToMethodMap.put(local, new ArrayList<>());
        }

        Chain<Unit> units = getUnits();
        Iterator<Unit> itr = units.snapshotIterator();

        while(itr.hasNext()) {
            Stmt currentStmt = (Stmt) itr.next();
            if (!currentStmt.containsInvokeExpr()) {
                continue;
            }

            if (currentStmt.getInvokeExpr() instanceof VirtualInvokeExpr) {
                VirtualInvokeExpr currentVirtualInvoke = (VirtualInvokeExpr) currentStmt.getInvokeExpr();
                if (currentVirtualInvoke.getBase() instanceof Local) {
                    Local base = (Local) currentVirtualInvoke.getBase();
                    if (LocalToMethodMap.containsKey(base)) {
                        LocalToMethodMap.get(base).add(currentStmt);
                    }
                    else {
                        LocalToMethodMap.put(base, new ArrayList<>());
                        LocalToMethodMap.get(base).add(currentStmt);
                    }

                }
                for (Value val: currentVirtualInvoke.getArgs()) {
                    if (val instanceof Local) {
                        if (LocalToMethodMap.containsKey(val)) {
                            LocalToMethodMap.get(val).add(currentStmt);
                        }
                        else {
                            LocalToMethodMap.put((Local) val, new ArrayList<>());
                            LocalToMethodMap.get(val).add(currentStmt);
                        }
                    }
                }
            }

            if (currentStmt.getInvokeExpr() instanceof StaticInvokeExpr) {
                StaticInvokeExpr currentStaticInvoke = (StaticInvokeExpr) currentStmt.getInvokeExpr();
                for (Value val: currentStaticInvoke.getArgs()) {
                    if (val instanceof Local) {
                        LocalToMethodMap.get(val).add(currentStmt);
                    }
                }
            }

            if(currentStmt instanceof AssignStmt) {
                Local key =(Local)((AssignStmt)currentStmt).getLeftOp();
                if (LocalToMethodMap.containsKey(key)) {
                    LocalToMethodMap.get(key).add(currentStmt);
                }
                else {
                    LocalToMethodMap.put(key, new ArrayList<>());
                    LocalToMethodMap.get(key).add(currentStmt);
                }

            }

        }
        return LocalToMethodMap;
    }

    /**
     * Removes stmt from jimple body
     * @param stmtToRemove statement that is intended to be removed
     */
    public void removeStmt(Stmt stmtToRemove) {
        Chain<Unit> units = getUnits();
        units.remove(stmtToRemove);
    }


    public abstract <T extends Unit> List<T> locateUnits();

    protected SootMethod getAssertTrue() {

        String classSignature = null;
        if (jupiter) {

            classSignature = "org.junit.jupiter.api.Assertions";
        }
        else {
            classSignature = "org.junit.Assert";
        }


        Scene.v().forceResolve(classSignature, SootClass.BODIES);
        for (SootMethod sm: Scene.v().getSootClass(classSignature).getMethods()) {
            if (sm.getSubSignature().contains("void assertTrue(boolean)")){
                return Scene.v().getSootClass(classSignature).getMethod("void assertTrue(boolean)");
            }
        }
        SootClass phantomClass = Scene.v().getSootClass(classSignature);
        SootMethod phantomAssert = new SootMethod("assertTrue",
            Collections.singletonList(RefType.v(classSignature)), VoidType.v(), Modifier.PUBLIC | Modifier.STATIC);
        phantomAssert.setPhantom(true);
        List<Type> params = new ArrayList<>();
        params.add(BooleanType.v());
        phantomAssert.setParameterTypes(params);

        try {
            phantomClass.addMethod(phantomAssert);
        } catch (Exception e) {
            phantomAssert = phantomClass.getMethod(phantomAssert.getSubSignature());
        }

        return phantomAssert;
    }


    protected SootMethod getAssertFalse() {
        String classSignature = null;
        if (this.jupiter) {

            classSignature = "org.junit.jupiter.api.Assertions";
        }
        else {
            classSignature = "org.junit.Assert";
        }
        Scene.v().forceResolve(classSignature, SootClass.BODIES);
        for (SootMethod sm : Scene.v().getSootClass(classSignature).getMethods()) {
            if (sm.getSubSignature().contains("void assertFalse(boolean)")) {
                return sm;
            }
        }
        SootClass phantomClass = Scene.v().getSootClass(classSignature);
        SootMethod phantomAssert = new SootMethod("assertFalse",
            Collections.singletonList(RefType.v(classSignature)), VoidType.v(), Modifier.PUBLIC | Modifier.STATIC);
        phantomAssert.setPhantom(true);
        List<Type> params = new ArrayList<>();
        params.add(BooleanType.v());
        phantomAssert.setParameterTypes(params);

        try {
            phantomClass.addMethod(phantomAssert);
        } catch (Exception e) {
            phantomAssert = phantomClass.getMethod(phantomAssert.getSubSignature());
        }

        return phantomAssert;
    }

    protected SootMethod getAssert() {
        String classSignature = null;
        if (this.jupiter) {
            classSignature = "org.junit.jupiter.api.Assertions";
        }
        else {
            classSignature = "org.junit.Assert";
        }
        Scene.v().forceResolve(classSignature, SootClass.BODIES);
        for (SootMethod sm : Scene.v().getSootClass(classSignature).getMethods()) {
            if (sm.toString().contains("<"+classSignature+": void assertEquals(java.lang.Object,java.lang.Object)>")) {
                return Scene.v().getSootClass(classSignature).getMethod("void assertEquals(java.lang.Object,java.lang.Object)");
            }
        }
        SootClass phantomClass = Scene.v().getSootClass(classSignature);
        SootMethod phantomAssert = new SootMethod("assertEquals",
            Collections.singletonList(RefType.v(classSignature)), VoidType.v(), Modifier.PUBLIC | Modifier.STATIC);
        phantomAssert.setPhantom(true);
        List<Type> params = new ArrayList<>();
        params.add(RefType.v("java.lang.Object"));
        params.add(RefType.v("java.lang.Object"));
        phantomAssert.setParameterTypes(params);


        try {
            phantomClass.addMethod(phantomAssert);
        } catch (Exception e) {
            phantomAssert = phantomClass.getMethod(phantomAssert.getSubSignature());
        }


        return phantomAssert;
    }

    protected SootMethod createPhantomMethod(String className, String methodSignature, String methodName, List<Type> params, Type returnType, Boolean isStatic) {
        return createPhantomMethod(className, methodSignature, methodName, params, returnType, isStatic, false);
    }
    protected SootMethod createPhantomMethod(String className, String methodSignature, String methodName, List<Type> params, Type returnType, Boolean isStatic, Boolean noModifiers) {
        SootClass sootClass = Scene.v().forceResolve(className, SootClass.BODIES);
        for (SootMethod sm : sootClass.getMethods()) {
            if (sm.getSubSignature().contains(methodSignature)) {
                return sm;
            }
        }



        SootClass phantomClass = Scene.v().getSootClass(className);
        SootMethod phantomMethod;

        if (noModifiers) {
            phantomMethod = new SootMethod(methodName, params, returnType, 0);
        } else {

            if (isStatic) {
                phantomMethod = new SootMethod(methodName, params, returnType, Modifier.PUBLIC | Modifier.STATIC);
            } else {
                phantomMethod = new SootMethod(methodName, params, returnType, Modifier.PUBLIC);
            }
        }



        phantomMethod.setPhantom(true);
        try {
            phantomClass.addMethod(phantomMethod);
        } catch (Exception e) {
            SootMethod alreadyIncludedMethod = sootClass.getMethod(phantomMethod.getSubSignature());
            return alreadyIncludedMethod;
        }
        return phantomMethod;
    }

    protected SootMethod getAssertNotNull() {
        String classSignature = null;
        if (this.jupiter) {

            classSignature = "org.junit.jupiter.api.Assertions";
        }
        else {
            classSignature = "org.junit.Assert";
        }
        Scene.v().forceResolve(classSignature, SootClass.BODIES);




        if (className != null) {
            SootClass assertionClass = Scene.v().getSootClass(className);
            for (SootMethod sm: Scene.v().getSootClass(className).getMethods()) {
                if (sm.toString().contains("<"+jupiter+": void assertNotNull(java.lang.Object)>")){
                    return sm;
                }
            }
        }
        SootClass phantomClass = Scene.v().getSootClass(classSignature);
        SootMethod phantomAssert = new SootMethod("assertNotNull",
            Collections.singletonList(RefType.v(classSignature)), VoidType.v(), Modifier.PUBLIC | Modifier.STATIC);
        phantomAssert.setPhantom(true);
        List<Type> params = new ArrayList<>();
        params.add(RefType.v("java.lang.Object"));
        phantomAssert.setParameterTypes(params);

        try {
            phantomClass.addMethod(phantomAssert);
        } catch (Exception e) {
            phantomAssert = phantomClass.getMethod(phantomAssert.getSubSignature());
        }

        return phantomAssert;
    }

    //TODO: Merge all assert methods
    protected SootMethod getAssertNull() {
        String classSignature = null;
        if (this.jupiter) {

            classSignature = "org.junit.jupiter.api.Assertions";
        }
        else {
            classSignature = "org.junit.Assert";
        }
        Scene.v().forceResolve(classSignature, SootClass.BODIES);
        for (SootMethod sm: Scene.v().getSootClass(classSignature).getMethods()) {
            if (sm.toString().contains("<"+classSignature+": void assertNull(java.lang.Object)>")){
                return sm;
            }
        }
        SootClass phantomClass = Scene.v().getSootClass(classSignature);
        SootMethod phantomAssert = new SootMethod("assertNull",
            Collections.singletonList(RefType.v(classSignature)), VoidType.v(), Modifier.PUBLIC | Modifier.STATIC);
        phantomAssert.setPhantom(true);
        List<Type> params = new ArrayList<>();
        params.add(RefType.v("java.lang.Object"));
        phantomAssert.setParameterTypes(params);

        try {
            phantomClass.addMethod(phantomAssert);
        } catch (Exception e) {
            phantomAssert = phantomClass.getMethod(phantomAssert.getSubSignature());
        }

        return phantomAssert;
    }


    protected SootMethod getAssert(Type type) {
        String classSignature = null;
        if (this.jupiter) {

            classSignature = "org.junit.jupiter.api.Assertions";
        }
        else {
            classSignature = "org.junit.Assert";
        }
        Scene.v().forceResolve(classSignature, SootClass.BODIES);
        for (SootMethod sm: Scene.v().getSootClass(classSignature).getMethods()) {
            String methodSignature = "<"+classSignature+": void assertEquals(" + type.toString() + "," + type +")>";
            if (sm.toString().contains(methodSignature)){
                return sm;
            }
        }
        SootClass phantomClass = Scene.v().getSootClass(classSignature);
        SootMethod phantomAssert = new SootMethod("assertEquals",
            Collections.singletonList(RefType.v(classSignature)), VoidType.v(), Modifier.PUBLIC | Modifier.STATIC);
        phantomAssert.setPhantom(true);
        List<Type> params = new ArrayList<>();
        params.add(type);
        params.add(type);
        phantomAssert.setParameterTypes(params);

        try {
            phantomClass.addMethod(phantomAssert);
        } catch (Exception e) {
            phantomAssert = phantomClass.getMethod(phantomAssert.getSubSignature());
        }
        return phantomAssert;
    }

    public abstract boolean isApplicable();

    public void exportSubclasses(String outputDir) throws IOException {
        List<SootClass> subclasses = getSubclasses();
        for (SootClass subclass: subclasses) {
            Scene.v().forceResolve(subclass.getName(), SootClass.BODIES);
            retrieveActiveAllMethods(subclass);
            try {
                exportClass(outputDir, subclass);
            } catch (IOException e) {
                System.out.println("failed to export subclass: " + subclass);
            }
        }
    }

    protected SootClass getClass(String className) {
        return setUp(this.inputDir, className);
    }

    protected void addSubclass(SootClass subclass) throws IOException {
        subclass.setOuterClass(this.mainClass);
        exportClass(this.inputDir, subclass);
    }

    public Local getIdentityLocal(SootMethod test) {
        Body body = test.getActiveBody();
        Chain<Unit> units = body.getUnits();
        Iterator<Unit> iterator = units.snapshotIterator();

        while (iterator.hasNext()) {
            Stmt stmt = (Stmt) iterator.next();
            if (!(stmt instanceof JIdentityStmt)) {
                continue;
            }
            JIdentityStmt jIdentityStmt = (JIdentityStmt) stmt;
            return (Local) jIdentityStmt.getLeftOp();
        }
        return null;
    }

    public Local createThisLocal(SootMethod test) {
        Local thisLocal = Jimple.v().newLocal("thisLocalTest", RefType.v(this.getCurrentClass().getName()));
        test.getActiveBody().getLocals().add(thisLocal);
        return thisLocal;
    }

    public void addIgnoreAnnotation(SootMethod test) {
        Tag ignoreTag = createIgnoreAnnotation();
        test.addTag(ignoreTag);
    }

    protected Tag createIgnoreAnnotation() {
        AnnotationTag annotationTag;
        if (jupiter) {
            annotationTag = new AnnotationTag("Lorg/junit/jupiter/api/Ignore;");
        } else {
            annotationTag = new AnnotationTag("Lorg/junit/Ignore;", null);
        }
        VisibilityAnnotationTag visibilityAnnotationTag = new VisibilityAnnotationTag(0);
        visibilityAnnotationTag.addAnnotation(annotationTag);
        return visibilityAnnotationTag;

    }

    public void castThisLocal(SootMethod test, Local thisLocal, Local identityLocal) {
        CastExpr castExpr = Jimple.v().newCastExpr(identityLocal, thisLocal.getType());
        AssignStmt assignStmt = Jimple.v().newAssignStmt(thisLocal, castExpr);
        InsertStmtEnd(test, assignStmt);
    }

    public SootMethod createGetThresholdMethod() {
        return createConfigGetterMethod("getThreshold", "mutation.threshold");
    }

    public SootMethod createGetstateSetterCountMethod() {
        return createConfigGetterMethod("getstateSetterCount", "mutation.count");
    }

    public SootMethod createConfigGetterMethod(String methodName, String config) {
        List<Stmt> execStmts = new ArrayList<>();

        SootClass propertiesClass = Scene.v().forceResolve("java.util.Properties", SootClass.BODIES);
        SootMethod propertiesInitMethod = propertiesClass.getMethod("void <init>()");


        try {
            propertiesClass.addMethod(propertiesInitMethod);
        } catch (Exception e) {

        }


        SootClass fileInputStreamClass = Scene.v().forceResolve("java.io.FileInputStream", SootClass.SIGNATURES);

        SootMethod fileInputStreamInitMethod = null;
        List<Type> params = new ArrayList<>();
        params.add(RefType.v("java.lang.String"));
        fileInputStreamInitMethod = fileInputStreamClass.getMethod("void <init>(java.lang.String)");
        /*fileInputStreamInitMethod.setPhantom(true);*/
        /*try {

            fileInputStreamClass.addMethod(fileInputStreamInitMethod);
        } catch (Exception e) {
            e.printStackTrace();
        }*/


        Local stringLocalFileName = Jimple.v().newLocal("fileName", RefType.v("java.lang.String"));
        Local propertyString = Jimple.v().newLocal("propertyString", RefType.v("java.lang.String"));
        Local properties = Jimple.v().newLocal("properties", propertiesClass.getType());
        Local $properties = Jimple.v().newLocal("$properties", propertiesClass.getType());
        Local fileInputStream = Jimple.v().newLocal("fileInputStream", fileInputStreamClass.getType());
        Local $fileInputStream = Jimple.v().newLocal("$fileInputStream", fileInputStreamClass.getType());
        Local threshold = Jimple.v().newLocal("threshold", RefType.v("java.lang.Double"));
        Local primThreshold = Jimple.v().newLocal("primThreshold", DoubleType.v());

        //r0 = "mutation.config";
        AssignStmt initStringLocalFileName = Jimple.v().newAssignStmt(stringLocalFileName, StringConstant.v(Paths.get("target", "test-classes", "mutation.config").toString()));
        execStmts.add(initStringLocalFileName);

        //$r1 = new java.util.Properties;
        NewExpr newProperties = Jimple.v().newNewExpr(propertiesClass.getType());
        AssignStmt init$Properties = Jimple.v().newAssignStmt($properties, newProperties);
        execStmts.add(init$Properties);

        //specialinvoke $r1.<java.util.Properties: void <init>()>();

        SpecialInvokeExpr propertiesSpecialInvokeExpr = Jimple.v().newSpecialInvokeExpr($properties, propertiesInitMethod.makeRef());
        InvokeStmt propertiesSpecialInvokeStmt = Jimple.v().newInvokeStmt(propertiesSpecialInvokeExpr); //FIXME
        execStmts.add(propertiesSpecialInvokeStmt);

        //r2 = $r1;
        AssignStmt initProperties = Jimple.v().newAssignStmt(properties, $properties);
        execStmts.add(initProperties);

        //$r3 = new java.io.FileInputStream;
        NewExpr newFileInputStream = Jimple.v().newNewExpr(fileInputStreamClass.getType());
        AssignStmt init$FileInputStream = Jimple.v().newAssignStmt($fileInputStream, newFileInputStream);
        execStmts.add(init$FileInputStream);

        //specialinvoke $r3.<java.io.FileInputStream: void <init>(java.lang.String)>(r0);
        SpecialInvokeExpr fileInputStreamInitExpr = Jimple.v().newSpecialInvokeExpr($fileInputStream, fileInputStreamInitMethod.makeRef(), stringLocalFileName);
        InvokeStmt fileInputStreamStmt = Jimple.v().newInvokeStmt(fileInputStreamInitExpr);
        execStmts.add(fileInputStreamStmt);

        //r4 = $r3;
        AssignStmt fileInputStreamAssignStmt = Jimple.v().newAssignStmt(fileInputStream, $fileInputStream);
        execStmts.add(fileInputStreamAssignStmt);

        //virtualinvoke r2.<java.util.Properties: void load(java.io.InputStream)>(r4);
        SootMethod loadMethod = propertiesClass.getMethod("void load(java.io.InputStream)");
        VirtualInvokeExpr loadInputStream = Jimple.v().newVirtualInvokeExpr(properties, loadMethod.makeRef(), fileInputStream);
        InvokeStmt loadInputStmt = Jimple.v().newInvokeStmt(loadInputStream);
        execStmts.add(loadInputStmt);

        //$r5 = virtualinvoke r2.<java.util.Properties: java.lang.String getProperty(java.lang.String)>("mutation.threshold");
        SootMethod getPropertyMethod = propertiesClass.getMethod("java.lang.String getProperty(java.lang.String)");
        VirtualInvokeExpr virtualInvokeExpr = Jimple.v().newVirtualInvokeExpr(properties, getPropertyMethod.makeRef(), StringConstant.v(config));
        AssignStmt initPropertyString = Jimple.v().newAssignStmt(propertyString, virtualInvokeExpr);
        execStmts.add(initPropertyString);

        //r6 = staticinvoke <java.lang.Double: java.lang.Double valueOf(java.lang.String)>($r5);
        SootClass doubleClass = Scene.v().getSootClass("java.lang.Double");
        SootMethod doubleValueOfMethod = doubleClass.getMethod("java.lang.Double valueOf(java.lang.String)");
        StaticInvokeExpr doubleValueOfExpr = Jimple.v().newStaticInvokeExpr(doubleValueOfMethod.makeRef(), propertyString);
        AssignStmt doubleAssignStmt = Jimple.v().newAssignStmt(threshold, doubleValueOfExpr);
        execStmts.add(doubleAssignStmt);

        //$d0 = virtualinvoke r6.<java.lang.Double: double doubleValue()>();
        SootMethod doubleValueMethod = doubleClass.getMethod("double doubleValue()");
        VirtualInvokeExpr doubleValueInvokeExpr = Jimple.v().newVirtualInvokeExpr(threshold, doubleValueMethod.makeRef());
        AssignStmt doubleValueAssignStmt = Jimple.v().newAssignStmt(primThreshold, doubleValueInvokeExpr);
        execStmts.add(doubleValueAssignStmt);

        /*execStmts = new ArrayList<>();*/

        /*AssignStmt debug = Jimple.v().newAssignStmt(primThreshold, DoubleConstant.v(1.0D));
        execStmts.add(debug);*/

        SootMethod method = this.createMethod(methodName, execStmts, primThreshold, new ArrayList<>());

        method.getActiveBody().getLocals().add(stringLocalFileName);
        method.getActiveBody().getLocals().add(propertyString);
        method.getActiveBody().getLocals().add(properties);
        method.getActiveBody().getLocals().add($properties);
        method.getActiveBody().getLocals().add(fileInputStream);
        method.getActiveBody().getLocals().add($fileInputStream);
        method.getActiveBody().getLocals().add(threshold);

        //method.getActiveBody().getLocals().add(primThreshold);

        /*SootClass exceptionClass = Scene.v().loadClassAndSupport("java.io.Exception");
        exceptionClass.setName("IOException");
        method.addException(exceptionClass);*/

        return method;
    }

    public SootMethod createRandom(SootClass sootClass) {

        SootMethod method = sootClass.getMethodByNameUnsafe("random");

        if (method != null) {
            return method;
        }

        List<Type> params = new ArrayList<>();
        params.add(
            DoubleType.v()
        );

        Local $z0 = Jimple.v().newLocal("$z0", BooleanType.v());
        SootMethod randomMethod = createMethodWithoutIdentity(
            sootClass,
            "random",
            new ArrayList<>(),
            $z0,
            params
        );

        Local $r0 = Jimple.v().newLocal("$r0", RefType.v("java.util.concurrent.ThreadLocalRandom"));
        Local d0 = Jimple.v().newLocal("d0", DoubleType.v());
        Local d1 = Jimple.v().newLocal("d1", DoubleType.v());
        Local $b0 = Jimple.v().newLocal("$b0", ByteType.v());
        Local r1 = Jimple.v().newLocal("r1", RefType.v(sootClass));

        randomMethod.getActiveBody().getLocals().add($r0);
        randomMethod.getActiveBody().getLocals().add(d0);
        randomMethod.getActiveBody().getLocals().add(d1);
        randomMethod.getActiveBody().getLocals().add($b0);
        randomMethod.getActiveBody().getLocals().add(r1);
        randomMethod.getActiveBody().getLocals().add($z0);

        List<Stmt> stmts = new ArrayList<>();

        ThisRef thisRef = Jimple.v().newThisRef(RefType.v(sootClass));
        stmts.add(
            Jimple.v().newIdentityStmt(r1, thisRef)
        ); //0
        stmts.add(
            Jimple.v().newIdentityStmt(d1, Jimple.v().newParameterRef(DoubleType.v(), 0))
        ); //1
        SootMethod current = Scene.v().forceResolve("java.util.concurrent.ThreadLocalRandom", SootClass.SIGNATURES).getMethodByName("current");
        stmts.add(
            Jimple.v().newAssignStmt($r0, Jimple.v().newStaticInvokeExpr(current.makeRef()))
        ); //2
        SootMethod nextDouble = Scene.v().forceResolve("java.util.concurrent.ThreadLocalRandom", SootClass.SIGNATURES).getMethod("double nextDouble(double,double)");
        List<Value> args2 = new ArrayList<>();
        args2.add(DoubleConstant.v(0D));
        args2.add(DoubleConstant.v(1D));
        stmts.add(
            Jimple.v().newAssignStmt(d0, Jimple.v().newVirtualInvokeExpr($r0, nextDouble.makeRef(),args2))
        ); //3

        stmts.add(
            Jimple.v().newAssignStmt($b0, Jimple.v().newCmplExpr(d0, d1))
        ); //4

        Stmt $z00 = Jimple.v().newAssignStmt($z0, IntConstant.v(0));
        stmts.add(
            Jimple.v().newIfStmt(Jimple.v().newLeExpr($b0, IntConstant.v(0)), $z00)
        ); //5
        stmts.add(
            Jimple.v().newAssignStmt($z0, IntConstant.v(1))
        ); //6
        ReturnStmt returnStmt = Jimple.v().newReturnStmt($z0);
        stmts.add(
            Jimple.v().newGotoStmt(returnStmt)
        );
        stmts.add($z00);
        stmts.add(returnStmt);

        Chain<Unit> chain = randomMethod.getActiveBody().getUnits();
        chain.addAll(stmts);

        try {
            sootClass.addMethod(randomMethod);
        } catch (Exception e) {
            return randomMethod;
        }
        return randomMethod;
    }

    public SootMethod createShuffleMethod(SootClass sootClass) {
        List<Type> params = new ArrayList<>();
        params.add(
            RefType.v("java.util.LinkedList")
        );

        params.add(
            RefType.v("java.lang.String")
        );
        params.add(
            RefType.v("java.lang.String")
        );

        params.add(
            BooleanType.v()
        );

        Local linkedListLocal = Jimple.v().newLocal("linkedListLocal", RefType.v("java.util.LinkedList"));

        /*
        stmts.add(
                Jimple.v().newIdentityStmt(r8, Jimple.v().newParameterRef(RefType.v("java.util.LinkedList"), 0))
        ); //1

        stmts.add(
                Jimple.v().newIdentityStmt(r2, Jimple.v().newParameterRef(RefType.v("java.lang.String"), 1))
        ); //2

        stmts.add(
                Jimple.v().newIdentityStmt(r5, Jimple.v().newParameterRef(RefType.v("java.lang.String"), 2))
        ); //3

        stmts.add(
                Jimple.v().newIdentityStmt(z2, Jimple.v().newParameterRef(BooleanType.v(), 3))
         */

        SootMethod shuffleMethod = createMethodWithoutIdentity(
            sootClass,
            "shuffleMethods",
            new ArrayList<>(),
            linkedListLocal,
            params
        );

        Local r0 = Jimple.v().newLocal("r0",RefType.v(sootClass));
        Local r1 = Jimple.v().newLocal("r1",RefType.v("java.lang.Class"));
        Local r2 = Jimple.v().newLocal("r2", RefType.v("java.lang.String"));
        Local $r3 = Jimple.v().newLocal("$r3", ArrayType.v(RefType.v("java.lang.Class"),1));
        Local r4 = Jimple.v().newLocal("r4", RefType.v("java.lang.reflect.Method"));
        Local r5 = Jimple.v().newLocal("r5", RefType.v("java.lang.String"));
        Local $r6 = Jimple.v().newLocal("$r6", ArrayType.v(RefType.v("java.lang.Class"), 1));
        Local r7 = Jimple.v().newLocal("r7", RefType.v("java.lang.reflect.Method"));
        Local r8 = Jimple.v().newLocal("r8", RefType.v("java.util.LinkedList"));
        Local $r9 = Jimple.v().newLocal("$r9", RefType.v("java.util.concurrent.ThreadLocalRandom"));
        Local $i0 = Jimple.v().newLocal("$i0", IntType.v());
        Local $i1 = Jimple.v().newLocal("$i1", IntType.v());
        Local $i2 = Jimple.v().newLocal("$i2", IntType.v());
        Local r10 = Jimple.v().newLocal("r10", RefType.v("java.lang.Integer"));
        Local $r11 = Jimple.v().newLocal("$r11", RefType.v("java.util.concurrent.ThreadLocalRandom"));
        Local $i3 = Jimple.v().newLocal("$i3", IntType.v());
        Local $i4 = Jimple.v().newLocal("$i4", IntType.v());
        Local $i5 = Jimple.v().newLocal("$i5", IntType.v());
        Local $i6 = Jimple.v().newLocal("$i6", IntType.v());
        Local r12 = Jimple.v().newLocal("r12", RefType.v("java.lang.Integer"));
        Local z2 = Jimple.v().newLocal("z2", BooleanType.v());
        Local $i7 = Jimple.v().newLocal("$i7", IntType.v());
        Local $i8 = Jimple.v().newLocal("$i8", IntType.v());
        Local $i9 = Jimple.v().newLocal("$i9", IntType.v());
        Local $i10 = Jimple.v().newLocal("$i10", IntType.v());

        shuffleMethod.getActiveBody().getLocals().add(r0);
        shuffleMethod.getActiveBody().getLocals().add(r1);
        shuffleMethod.getActiveBody().getLocals().add(r2);
        shuffleMethod.getActiveBody().getLocals().add($r3);
        shuffleMethod.getActiveBody().getLocals().add(r4);
        shuffleMethod.getActiveBody().getLocals().add(r5);
        shuffleMethod.getActiveBody().getLocals().add($r6);
        shuffleMethod.getActiveBody().getLocals().add(r7);
        shuffleMethod.getActiveBody().getLocals().add(r8);
        shuffleMethod.getActiveBody().getLocals().add($r9);
        shuffleMethod.getActiveBody().getLocals().add($i0);
        shuffleMethod.getActiveBody().getLocals().add($i1);
        shuffleMethod.getActiveBody().getLocals().add($i2);
        shuffleMethod.getActiveBody().getLocals().add(r10);
        shuffleMethod.getActiveBody().getLocals().add($r11);
        shuffleMethod.getActiveBody().getLocals().add($i3);
        shuffleMethod.getActiveBody().getLocals().add($i4);
        shuffleMethod.getActiveBody().getLocals().add($i5);
        shuffleMethod.getActiveBody().getLocals().add($i6);
        shuffleMethod.getActiveBody().getLocals().add(r12);
        shuffleMethod.getActiveBody().getLocals().add(z2);
        shuffleMethod.getActiveBody().getLocals().add($i7);
        shuffleMethod.getActiveBody().getLocals().add($i8);
        shuffleMethod.getActiveBody().getLocals().add($i9);
        shuffleMethod.getActiveBody().getLocals().add($i10);

        ArrayList<Stmt> stmts = new ArrayList<>();

        ThisRef thisRef = Jimple.v().newThisRef(RefType.v(sootClass));
        stmts.add(
            Jimple.v().newIdentityStmt(r0, thisRef)
        ); //0

        stmts.add(
            Jimple.v().newIdentityStmt(r8, Jimple.v().newParameterRef(RefType.v("java.util.LinkedList"), 0))
        ); //1

        stmts.add(
            Jimple.v().newIdentityStmt(r2, Jimple.v().newParameterRef(RefType.v("java.lang.String"), 1))
        ); //2

        stmts.add(
            Jimple.v().newIdentityStmt(r5, Jimple.v().newParameterRef(RefType.v("java.lang.String"), 2))
        ); //3

        stmts.add(
            Jimple.v().newIdentityStmt(z2, Jimple.v().newParameterRef(BooleanType.v(), 3))
        ); //4


        SootMethod getClass = Scene.v().forceResolve("java.lang.Object", SootClass.SIGNATURES).getMethod("java.lang.Class getClass()");
        stmts.add(
            Jimple.v().newAssignStmt(r1,Jimple.v().newVirtualInvokeExpr(r0, getClass.makeRef()))
        ); //5

        stmts.add(
            Jimple.v().newAssignStmt($r3, Jimple.v().newNewArrayExpr(RefType.v("java.lang.Class"),IntConstant.v(0)))
        ); //6

        SootMethod getMethod = Scene.v().forceResolve("java.lang.Class", SootClass.SIGNATURES).getMethodByName("getMethod");
        ArrayList<Value> args = new ArrayList<>();
        args.add(r2);
        args.add($r3);
        stmts.add(
            Jimple.v().newAssignStmt(r4, Jimple.v().newVirtualInvokeExpr(r1, getMethod.makeRef(), args))
        ); //7

        stmts.add(
            Jimple.v().newAssignStmt($r6, Jimple.v().newNewArrayExpr(RefType.v("java.lang.Class"),IntConstant.v(0)))
        ); //8

        ArrayList<Value> args2 = new ArrayList<>();
        args2.add(r5);
        args2.add($r6);
        stmts.add(
            Jimple.v().newAssignStmt(
                r7,
                Jimple.v().newVirtualInvokeExpr(r1, getMethod.makeRef(), args2)
            )
        ); //9

        SootMethod remove = Scene.v().forceResolve("java.util.LinkedList", SootClass.SIGNATURES).getMethod("boolean remove(java.lang.Object)");
        stmts.add(
            Jimple.v().newInvokeStmt(
                Jimple.v().newVirtualInvokeExpr(
                    r8,
                    remove.makeRef(),
                    r4)
            )
        ); //10

        stmts.add(
            Jimple.v().newInvokeStmt(
                Jimple.v().newVirtualInvokeExpr(
                    r8,
                    remove.makeRef(),
                    r7
                )
            )
        ); //11

        SootMethod shuffle = Scene.v().forceResolve("java.util.Collections", SootClass.SIGNATURES).getMethod("void shuffle(java.util.List)");
        stmts.add(
            Jimple.v().newInvokeStmt(
                Jimple.v().newStaticInvokeExpr(
                    shuffle.makeRef(),
                    r8
                )
            )
        ); //12

        SootMethod current = Scene.v().forceResolve("java.util.concurrent.ThreadLocalRandom", SootClass.SIGNATURES).getMethodByName("current");
        stmts.add(
            Jimple.v().newAssignStmt(
                $r9,
                Jimple.v().newStaticInvokeExpr(
                    current.makeRef()
                )
            )
        ); //13

        SootMethod size = Scene.v().forceResolve("java.util.LinkedList", SootClass.SIGNATURES).getMethodByName("size");
        stmts.add(
            Jimple.v().newAssignStmt(
                $i0,
                Jimple.v().newVirtualInvokeExpr(r8, size.makeRef())
            )
        ); //14

        stmts.add(
            Jimple.v().newAssignStmt(
                $i1,
                Jimple.v().newSubExpr($i0, IntConstant.v(1))
            )
        ); //15

        SootMethod nextInt = Scene.v().forceResolve("java.util.concurrent.ThreadLocalRandom", SootClass.SIGNATURES).getMethod("int nextInt(int,int)");
        List<Value> args3 = new ArrayList<>();
        args3.add(IntConstant.v(0));
        args3.add($i1);
        stmts.add(
            Jimple.v().newAssignStmt(
                $i2,
                Jimple.v().newVirtualInvokeExpr($r9, nextInt.makeRef(), args3)
            )
        ); //16

        SootMethod valueOf = Scene.v().forceResolve("java.lang.Integer", SootClass.BODIES).getMethod("java.lang.Integer valueOf(int)");
        stmts.add(
            Jimple.v().newAssignStmt(
                r10,
                Jimple.v().newStaticInvokeExpr(
                    valueOf.makeRef(),
                    $i2
                )
            )
        ); //17

        stmts.add(
            Jimple.v().newAssignStmt(
                $r11, Jimple.v().newStaticInvokeExpr(current.makeRef())
            )
        ); // 18


        SootMethod intValue = Scene.v().forceResolve("java.lang.Integer", SootClass.BODIES).getMethodByName("intValue");
        stmts.add(
            Jimple.v().newAssignStmt(
                $i3,
                Jimple.v().newVirtualInvokeExpr(
                    r10,
                    intValue.makeRef()
                )
            )
        ); // 19


        stmts.add(
            Jimple.v().newAssignStmt(
                $i4,
                Jimple.v().newAddExpr($i3, IntConstant.v(1))
            )
        ); // 20

        stmts.add(
            Jimple.v().newAssignStmt(
                $i5,
                Jimple.v().newVirtualInvokeExpr(
                    r8,
                    size.makeRef()
                )
            )
        ); // 21

        List<Value> args4 = new ArrayList<>();
        args4.add($i4);
        args4.add($i5);
        stmts.add(
            Jimple.v().newAssignStmt(
                $i6,
                Jimple.v().newVirtualInvokeExpr(
                    $r11,
                    nextInt.makeRef(),
                    args4

                )
            )
        ); // 22

        stmts.add(
            Jimple.v().newAssignStmt(
                r12,
                Jimple.v().newStaticInvokeExpr(valueOf.makeRef(), $i6)
            )
        ); // 23

        Stmt r12IntegerValueStmt = Jimple.v().newAssignStmt(
            $i7,
            Jimple.v().newVirtualInvokeExpr(r12, intValue.makeRef())
        );
        stmts.add(
            Jimple.v().newIfStmt(
                Jimple.v().newEqExpr(z2, IntConstant.v(0)),
                r12IntegerValueStmt
            )
        ); //24


        stmts.add(
            Jimple.v().newAssignStmt(
                $i9,
                Jimple.v().newVirtualInvokeExpr(r10, intValue.makeRef())
            )
        ); //25

        SootMethod add = Scene.v().forceResolve("java.util.LinkedList", SootClass.BODIES).getMethod("void add(int,java.lang.Object)");
        List<Value> args5 = new ArrayList<>();
        args5.add($i9);
        args5.add(r7);
        stmts.add(
            Jimple.v().newInvokeStmt(
                Jimple.v().newVirtualInvokeExpr(r8, add.makeRef(), args5)
            )
        ); //26

        stmts.add(
            Jimple.v().newAssignStmt(
                $i10,
                Jimple.v().newVirtualInvokeExpr(r12, intValue.makeRef())
            )
        ); //27

        List<Value> args6 = new ArrayList<>();
        args6.add($i10);
        args6.add(r4);
        stmts.add(
            Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(r8, add.makeRef(), args6))
        ); //28

        ReturnStmt returnStmt = Jimple.v().newReturnStmt(r8);
        stmts.add(
            Jimple.v().newGotoStmt(returnStmt)
        ); // 29

        stmts.add(r12IntegerValueStmt); // 30

        List<Value> args7 = new ArrayList<>();
        args7.add($i7);
        args7.add(r7);
        stmts.add(
            Jimple.v().newInvokeStmt(
                Jimple.v().newVirtualInvokeExpr(
                    r8,
                    add.makeRef(),
                    args7
                )
            )
        ); //31

        stmts.add(
            Jimple.v().newAssignStmt(
                $i8,
                Jimple.v().newVirtualInvokeExpr(
                    r10,
                    intValue.makeRef()
                )
            )
        ); //32

        List<Value> args8 = new ArrayList<>();
        args8.add($i8);
        args8.add(r4);
        stmts.add(
            Jimple.v().newInvokeStmt(
                Jimple.v().newVirtualInvokeExpr(r8,
                    add.makeRef(),
                    args8)
            )
        ); //33

        stmts.add(returnStmt); //34

        Chain<Unit> units = shuffleMethod.getActiveBody().getUnits();

        units.addAll(stmts);

        /*for (int i = 0; i < 5; i++) {
            units.add(stmts.get(i));
        } units.add(stmts.get(stmts.size()-1));*/
        try {
            sootClass.addMethod(shuffleMethod);
        } catch (Exception e) {
            return sootClass.getMethod(shuffleMethod.getSubSignature());
        }
        return shuffleMethod;

    }

    public SootMethod createMethodWithoutIdentity(SootClass declaringClass, String methodName, List<Stmt> execStmts, Value returnValue, List<Type> params) {

        SootMethod method;
        //create the test signature
        if (returnValue == null)
        {
            method = Scene.v().makeSootMethod(methodName, params, VoidType.v(), Modifier.PUBLIC);
        }
        else {
            method = Scene.v().makeSootMethod(methodName, params, returnValue.getType(), Modifier.PUBLIC);
        }
        //create body for the new test
        JimpleBody body = Jimple.v().newBody(method);
        method.setActiveBody(body);

        /*int i = 0;*/
        /*for (Type param: params) {*/

        /*}*/

        //add return statement to the test

        for (Stmt execStmt : execStmts) {
            InsertStmtEnd(execStmt, method);
        }

        return method;
    }

    public SootMethod addReflectionInvoke(SootClass sootClass) {
        List<Type> params = new ArrayList<>();
        params.add(
            RefType.v("java.util.LinkedList")
        );

        Local linkedListLocal = Jimple.v().newLocal("linkedListLocal", RefType.v("java.util.LinkedList"));

        SootMethod reflectInvoke = createMethodWithoutIdentity(
            sootClass,
            "reflectInvoke",
            new ArrayList<>(),
            null,
            params
        );

        List<Stmt> stmts = new ArrayList<>();

        Local r0 = Jimple.v().newLocal("r0", RefType.v("java.util.List"));
        Local i0 = Jimple.v().newLocal("i0", IntType.v());
        Local $r1 = Jimple.v().newLocal("$r1", RefType.v("java.lang.Object"));
        Local r2 = Jimple.v().newLocal("r2", RefType.v("com.example.printJimple"));
        Local $r3 = Jimple.v().newLocal("$r3", ArrayType.v(RefType.v("java.lang.Object"),1));
        Local $r4 = Jimple.v().newLocal("$r4", RefType.v("java.lang.reflect.Method"));
        Local i1 = Jimple.v().newLocal("r0", IntType.v());

        reflectInvoke.getActiveBody().getLocals().add(r0);
        reflectInvoke.getActiveBody().getLocals().add(i0);
        reflectInvoke.getActiveBody().getLocals().add($r1);
        reflectInvoke.getActiveBody().getLocals().add(r2);
        reflectInvoke.getActiveBody().getLocals().add($r3);
        reflectInvoke.getActiveBody().getLocals().add($r4);
        reflectInvoke.getActiveBody().getLocals().add(i1);

        ThisRef thisRef = Jimple.v().newThisRef(RefType.v(sootClass));
        stmts.add(
            Jimple.v().newIdentityStmt(r2, thisRef)
        ); //0

        stmts.add(
            Jimple.v().newIdentityStmt(r0, Jimple.v().newParameterRef(RefType.v("java.util.List"),0))
        ); //1

        SootMethod sizeMethod = Scene.v().forceResolve("java.util.List", SootClass.SIGNATURES).getMethod("int size()");
        stmts.add(
            Jimple.v().newAssignStmt(i0, Jimple.v().newInterfaceInvokeExpr(r0, sizeMethod.makeRef()))
        ); //2

        stmts.add(
            Jimple.v().newAssignStmt(i1, IntConstant.v(0))
        ); //3

        ReturnVoidStmt returnStmt = Jimple.v().newReturnVoidStmt();
        IfStmt ifStmt = Jimple.v().newIfStmt(Jimple.v().newGeExpr(i1, i0), returnStmt);
        stmts.add(ifStmt); //4

        SootMethod getMethod = Scene.v().forceResolve("java.util.List", SootClass.SIGNATURES).getMethod("java.lang.Object get(int)");
        stmts.add(
            Jimple.v().newAssignStmt(
                $r1,
                Jimple.v().newInterfaceInvokeExpr(
                    r0,
                    getMethod.makeRef(),
                    i1
                )
            )
        ); //5

        stmts.add(
            Jimple.v().newAssignStmt(
                $r4,
                Jimple.v().newCastExpr(
                    $r1,
                    RefType.v("java.lang.reflect.Method")
                )
            )
        ); //6

        stmts.add(
            Jimple.v().newAssignStmt(
                $r3,
                Jimple.v().newNewArrayExpr(RefType.v("java.lang.Object"), IntConstant.v(0))
            )
        ); //7

        SootMethod invokeMethod = Scene.v().forceResolve("java.lang.reflect.Method", SootClass.SIGNATURES).getMethod("java.lang.Object invoke(java.lang.Object,java.lang.Object[])");
        List<Value> args = new ArrayList<>();
        args.add(r2);
        args.add($r3);
        stmts.add(
            Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr($r4, invokeMethod.makeRef(),args))
        ); //8

        stmts.add(
            Jimple.v().newAssignStmt(i1, Jimple.v().newAddExpr(i1, IntConstant.v(1)))
        ); //9

        stmts.add(
            Jimple.v().newGotoStmt(ifStmt)
        ); // 10

        stmts.add(returnStmt); //11

        for (Stmt stmt: stmts) {
            reflectInvoke.getActiveBody().getUnits().add(stmt);
        }

        try {
            sootClass.addMethod(reflectInvoke);
        } catch (Exception e) {
            return sootClass.getMethod(reflectInvoke.getSubSignature());
        }
        return reflectInvoke;
    }

    public SootMethod createMethodListMethod(SootClass sootClass) {
        List<Type> params = new ArrayList<>();
        params.add(
            ArrayType.v(RefType.v("java.lang.reflect.Method"),1)
        );

        Local linkedListLocal = Jimple.v().newLocal("linkedListLocal", RefType.v("java.util.LinkedList"));

        SootMethod createMethodList = createMethodWithoutIdentity(
            sootClass,
            "createMethodList",
            new ArrayList<>(),
            linkedListLocal,
            params
        );

        Local $r0 = Jimple.v().newLocal("$r0", RefType.v("java.util.LinkedList"));
        Local r1 = Jimple.v().newLocal("r1", RefType.v("java.util.LinkedList"));
        Local r2 = Jimple.v().newLocal("r2", ArrayType.v(RefType.v("java.lang.reflect.Method"), 1));
        Local i0 = Jimple.v().newLocal("i0", IntType.v());
        Local r3 = Jimple.v().newLocal("r3", RefType.v("java.lang.reflect.Method"));
        Local $r4 = Jimple.v().newLocal("$r4", RefType.v("java.lang.String"));
        Local $z0 = Jimple.v().newLocal("$z0", BooleanType.v());
        Local $i1 = Jimple.v().newLocal("$i1", IntType.v());
        Local $r5 = Jimple.v().newLocal("$r5", RefType.v("java.lang.String"));
        Local $z1 = Jimple.v().newLocal("$z1", BooleanType.v());
        Local last = Jimple.v().newLocal("last", BooleanType.v());
        Local $r6 = Jimple.v().newLocal("$r6", RefType.v("java.lang.reflect.Method"));
        Local r7 = Jimple.v().newLocal("r7", RefType.v(sootClass));
        Local i2 = Jimple.v().newLocal("i2", IntType.v());

        createMethodList.getActiveBody().getLocals().add($r0);
        createMethodList.getActiveBody().getLocals().add(r1);
        createMethodList.getActiveBody().getLocals().add(r2);
        createMethodList.getActiveBody().getLocals().add(i0);
        createMethodList.getActiveBody().getLocals().add(r3);
        createMethodList.getActiveBody().getLocals().add($r4);
        createMethodList.getActiveBody().getLocals().add($z0);
        createMethodList.getActiveBody().getLocals().add($i1);
        createMethodList.getActiveBody().getLocals().add($r5);
        createMethodList.getActiveBody().getLocals().add($z1);
        createMethodList.getActiveBody().getLocals().add($r6);
        createMethodList.getActiveBody().getLocals().add(r7);
        createMethodList.getActiveBody().getLocals().add(i2);
        createMethodList.getActiveBody().getLocals().add(last);

        List<Stmt> stmts = new ArrayList<>();
        ThisRef thisRef = Jimple.v().newThisRef(RefType.v(sootClass));
        stmts.add(
            Jimple.v().newIdentityStmt(r7, thisRef)
        ); //0

        IdentityRef identityRef = Jimple.v().newParameterRef(ArrayType.v(RefType.v("java.lang.reflect.Method"),1),0);
        stmts.add(
            Jimple.v().newIdentityStmt(r2, identityRef)
        ); //1

        stmts.add(
            Jimple.v().newAssignStmt(
                $r0,
                Jimple.v().newNewExpr(RefType.v("java.util.LinkedList"))
            )
        ); //2

        SootMethod linkedListInitMethod = Scene.v().forceResolve("java.util.LinkedList", SootClass.SIGNATURES).getMethod("void <init>()");
        stmts.add(
            Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr($r0, linkedListInitMethod.makeRef()))
        ); //3

        stmts.add(
            Jimple.v().newAssignStmt(
                r1, $r0
            )
        ); //4

        stmts.add(
            Jimple.v().newAssignStmt(
                i0, Jimple.v().newLengthExpr(r2)
            )
        ); //5

        stmts.add(
            Jimple.v().newAssignStmt(
                i2, IntConstant.v(0)
            )
        ); //6

        ReturnStmt returnStmt = Jimple.v().newReturnStmt(r1);
        IfStmt ifStmt = Jimple.v().newIfStmt(Jimple.v().newGeExpr(i2, i0), returnStmt);
        stmts.add(ifStmt); //7

        stmts.add(
            Jimple.v().newAssignStmt(
                r3,
                Jimple.v().newArrayRef(
                    r2, i2
                )
            )
        ); //8

        SootMethod getNameMethod = Scene.v().forceResolve("java.lang.reflect.Method", SootClass.SIGNATURES).getMethod("java.lang.String getName()");
        stmts.add(
            Jimple.v().newAssignStmt(
                $r4,
                Jimple.v().newVirtualInvokeExpr(r3, getNameMethod.makeRef())
            )
        ); //9

        SootMethod containsMethod = Scene.v().forceResolve("java.lang.String", SootClass.SIGNATURES).getMethod("boolean contains(java.lang.CharSequence)");
        stmts.add(
            Jimple.v().newAssignStmt($z0, Jimple.v().newVirtualInvokeExpr($r4, containsMethod.makeRef(),StringConstant.v("test")))
        ); //10


        AssignStmt incrementi2 = Jimple.v().newAssignStmt(i2, Jimple.v().newAddExpr(i2, IntConstant.v(1)));
        stmts.add(
            Jimple.v().newIfStmt(Jimple.v().newEqExpr($z0, IntConstant.v(0)), incrementi2)
        ); //11

        SootMethod getParamCountMethod = Scene.v().forceResolve("java.lang.reflect.Method", SootClass.SIGNATURES).getMethod("int getParameterCount()");
        stmts.add(
            Jimple.v().newAssignStmt($i1, Jimple.v().newVirtualInvokeExpr(r3, getParamCountMethod.makeRef()))
        ); //12

        stmts.add(
            Jimple.v().newIfStmt(Jimple.v().newNeExpr($i1, IntConstant.v(0)), incrementi2)
        ); //13

        stmts.add(
            Jimple.v().newAssignStmt($r5, Jimple.v().newVirtualInvokeExpr(r3,getNameMethod.makeRef()))
        ); //14

        stmts.add(
            Jimple.v().newAssignStmt(
                $z1, Jimple.v().newVirtualInvokeExpr($r5, containsMethod.makeRef(), StringConstant.v("Mutant"))
            )
        ); //15

        stmts.add(
            Jimple.v().newIfStmt(
                Jimple.v().newNeExpr($z1,IntConstant.v(0)),
                incrementi2
            )
        ); //16

        stmts.add(
            Jimple.v().newAssignStmt(
                last, Jimple.v().newVirtualInvokeExpr($r5, containsMethod.makeRef(), StringConstant.v("$"))
            )
        ); //15

        stmts.add(
            Jimple.v().newIfStmt(
                Jimple.v().newNeExpr(last,IntConstant.v(0)),
                incrementi2
            )
        ); //16

        stmts.add(
            Jimple.v().newAssignStmt(
                $r6,
                Jimple.v().newArrayRef(r2, i2)
            )
        ); //17

        SootMethod addMethod = Scene.v().forceResolve("java.util.LinkedList", SootClass.SIGNATURES).getMethod("boolean add(java.lang.Object)");
        stmts.add(
            Jimple.v().newInvokeStmt(
                Jimple.v().newVirtualInvokeExpr(
                    r1, addMethod.makeRef(), $r6
                )
            )
        ); //18

        stmts.add(incrementi2); // 19
        stmts.add(
            Jimple.v().newGotoStmt(ifStmt)
        ); // 20

        stmts.add(returnStmt); //21

        for (Stmt stmt: stmts) {
            createMethodList.getActiveBody().getUnits().add(stmt);
        }


        try {
            sootClass.addMethod(createMethodList);
        } catch (Exception e) {
            return createMethodList;
        }

        return createMethodList;
    }

    public void InsertStmtEnd(Stmt stmt, SootMethod sm) {
        Chain<Unit> units = sm.getActiveBody().getUnits();
        Iterator<Unit> itr = units.snapshotIterator();
        Stmt currentStmt = null;
        while(itr.hasNext()) {
            currentStmt = (Stmt)itr.next();

            if (!currentStmt.containsInvokeExpr()) {
                continue;
            }

            InvokeExpr expr = currentStmt.getInvokeExpr();
        }

        units.insertBefore(stmt, currentStmt);
    }
    protected Local getThisLocal(Body body, SootClass sc) {
        Chain<Local> locals =  body.getLocals();
        Iterator<Local> iterator = locals.snapshotIterator();
        while (iterator.hasNext()) {
            Local local = iterator.next();
            if (local.getType().toString().equals(sc.toString())) {
                return local;
            }
        }

        return null;

    }
    public Stmt odInvoke(SootClass sootClass, SootMethod sootMethod, Unit point, SootMethod polluter, SootMethod victim) {
        SootMethod random = createRandom(sootClass);

        createMethodListMethod(sootClass);
        Body body = sootMethod.getActiveBody();

        Local reflectionLocal = Jimple.v().newLocal("reflectionLocal", RefType.v("java.lang.Class"));
        body.getLocals().add(reflectionLocal);

        Local thisLocal = getThisLocal(body, sootClass);

        SootClass objectClass = Scene.v().getSootClass("java.lang.Object");
        SootMethod getClassMethod = objectClass.getMethodByName("getClass");

        VirtualInvokeExpr getClassVirtualInvoke = Jimple.v().newVirtualInvokeExpr(thisLocal, getClassMethod.makeRef());
        AssignStmt getClassAssignStatement = Jimple.v().newAssignStmt(reflectionLocal, getClassVirtualInvoke);

        InsertStmtEnd(getClassAssignStatement, sootMethod);

        Local methodsArray = Jimple.v().newLocal("methodsArray", ArrayType.v(RefType.v("java.lang.reflect.Method"),1));
        body.getLocals().add(methodsArray);

        SootClass reflectionClass = Scene.v().getSootClass("java.lang.Class");
        SootMethod getMethodsMethod = reflectionClass.getMethodByName("getMethods");
        VirtualInvokeExpr getMethodVirtualInvokeExpr = Jimple.v().newVirtualInvokeExpr(reflectionLocal, getMethodsMethod.makeRef());
        AssignStmt getMethodStatement = Jimple.v().newAssignStmt(methodsArray, getMethodVirtualInvokeExpr);

        InsertStmtEnd(getMethodStatement, sootMethod);



        // $r3 = new java.util.LinkedList;
        Scene.v().forceResolve("java.util.LinkedList", SootClass.SIGNATURES);
        Local $linkedListLocal = Jimple.v().newLocal("$linkedListLocal", RefType.v("java.util.LinkedList"));
        body.getLocals().add($linkedListLocal);
        NewExpr newLinkedListExpr = Jimple.v().newNewExpr(RefType.v("java.util.LinkedList"));
        AssignStmt newLikedListRValueInitStmt = Jimple.v().newAssignStmt($linkedListLocal, newLinkedListExpr);
        InsertStmtEnd(newLikedListRValueInitStmt, sootMethod);

        // $r4 = staticinvoke <java.util.Arrays: java.util.List asList(java.lang.Object[])>(r2);
        Local listLocal = Jimple.v().newLocal("listLocal", RefType.v("java.util.List"));
        body.getLocals().add(listLocal);
        //SootClass javaArraysClass = Scene.v().forceResolve("java.util.Arrays", SootClass.SIGNATURES);
        SootMethod asListMethod = sootClass.getMethodByName("createMethodList");
        VirtualInvokeExpr asListStaticInvokeExpr = Jimple.v().newVirtualInvokeExpr(thisLocal, asListMethod.makeRef(), methodsArray);
        AssignStmt asListAssignStmt = Jimple.v().newAssignStmt(listLocal, asListStaticInvokeExpr);
        InsertStmtEnd(asListAssignStmt, sootMethod);

        // specialinvoke $r3.<java.util.LinkedList: void <init>(java.util.Collection)>($r4);
        SootClass javaLinkedListClass = Scene.v().getSootClass("java.util.LinkedList");
        SootMethod linkedListInitMethod = javaLinkedListClass.getMethod("void <init>(java.util.Collection)");
        SpecialInvokeExpr linkedListInitSpecialInvokeExpr = Jimple.v().newSpecialInvokeExpr($linkedListLocal, linkedListInitMethod.makeRef(), listLocal);
        InvokeStmt linkedListInitSpecialInvokeStmt = Jimple.v().newInvokeStmt(linkedListInitSpecialInvokeExpr);
        InsertStmtEnd(linkedListInitSpecialInvokeStmt, sootMethod);


        // r5 = $r3;
        Local linkedListLocal = Jimple.v().newLocal("linkedListLocal", RefType.v("java.util.LinkedList"));
        body.getLocals().add(linkedListLocal);
        AssignStmt assignLinkedListStmt = Jimple.v().newAssignStmt(linkedListLocal, $linkedListLocal);
        InsertStmtEnd(assignLinkedListStmt, sootMethod);


        //$r6 = newarray (java.lang.Class)[0];
        NewArrayExpr newArrayExpr = Jimple.v().newNewArrayExpr(RefType.v("java.lang.Class"),IntConstant.v(0));
        Local classArrayLocal = Jimple.v().newLocal("classArrayLocal", ArrayType.v(RefType.v("java.lang.Class"),1));
        body.getLocals().add(classArrayLocal);
        AssignStmt initClassArrayLocal = Jimple.v().newAssignStmt(classArrayLocal, newArrayExpr);
        InsertStmtEnd(initClassArrayLocal, sootMethod);

        // $r7 = virtualinvoke r1.<java.lang.Class: java.lang.reflect.Method getMethod(java.lang.String,java.lang.Class[])>("reflect", $r6);
        Local methodLocal = Jimple.v().newLocal("methodLocal", RefType.v("java.lang.reflect.Method"));
        body.getLocals().add(methodLocal);
        SootMethod getMethodMethod = reflectionClass.getMethodByName("getMethod");
        List<Value> arguments = new ArrayList<>();
        arguments.add(classArrayLocal);
        arguments.add(StringConstant.v(sootMethod.getName()));
        VirtualInvokeExpr getMethodCurrent = Jimple.v().newVirtualInvokeExpr(reflectionLocal, getMethodMethod.makeRef(), arguments);
        AssignStmt getMethodAssign = Jimple.v().newAssignStmt(methodLocal, getMethodCurrent);
        /*InsertStmtEnd(getMethodAssign, sootMethod);*/

        // interfaceinvoke r5.<java.util.List: boolean remove(java.lang.Object)>($r7);
        SootMethod removeMethod = Scene.v().getSootClass("java.util.List").getMethod("boolean remove(java.lang.Object)");
        InterfaceInvokeExpr listRemoveExpr = Jimple.v().newInterfaceInvokeExpr(linkedListLocal, removeMethod.makeRef(), methodLocal);
        InvokeStmt listRemoveStmt = Jimple.v().newInvokeStmt(listRemoveExpr);
        /*InsertStmtEnd(listRemoveStmt, sootMethod); */ //TODO: this should be present in MOs?


        Local booleanLocal = Jimple.v().newLocal("booleanLocal", BooleanType.v());
        sootMethod.getActiveBody().getLocals().add(booleanLocal);

        Local doubleLocal = Jimple.v().newLocal("doubleLocal", DoubleType.v());
        sootMethod.getActiveBody().getLocals().add(doubleLocal);

        SootMethod getThresholdMethod = sootClass.getMethodByNameUnsafe("getThreshold");

        if (getThresholdMethod == null) {
            sootClass.addMethod(createGetThresholdMethod());
            getThresholdMethod = sootClass.getMethodByName("getThreshold");
        }

        VirtualInvokeExpr getThresholdExpr = Jimple.v().newVirtualInvokeExpr(thisLocal, getThresholdMethod.makeRef());
        AssignStmt doubleAssign = Jimple.v().newAssignStmt(doubleLocal, getThresholdExpr);
        InsertStmtEnd(doubleAssign, sootMethod);

        AssignStmt booleanAssign = Jimple.v().newAssignStmt(booleanLocal, Jimple.v().newVirtualInvokeExpr(thisLocal, random.makeRef(), doubleLocal));
        InsertStmtEnd(booleanAssign, sootMethod);


        SootMethod shuffle = createShuffleMethod(sootClass);
        List<Value> args2 = new ArrayList<>();
        args2.add(linkedListLocal);
        args2.add(StringConstant.v(polluter.getName()));
        args2.add(StringConstant.v(victim.getName()));
        args2.add(booleanLocal);
        VirtualInvokeExpr shuffleInvoke = Jimple.v().newVirtualInvokeExpr(thisLocal, shuffle.makeRef(), args2);
        InvokeStmt shuffleStmt = Jimple.v().newInvokeStmt(shuffleInvoke);
        InsertStmtEnd(shuffleStmt, sootMethod);


        //****
        //SootMethod reflectionInvokeMethod = addReflectionInvoke(sootClass);
        //VirtualInvokeExpr reflectionInvokeExpr = Jimple.v().newVirtualInvokeExpr(thisLocal, reflectionInvokeMethod.makeRef(), linkedListLocal);
        //InvokeStmt reflectionInvokeStmt = Jimple.v().newInvokeStmt(reflectionInvokeExpr);
        //InsertStmtEnd(reflectionInvokeStmt, sootMethod);

        SootMethod reflectInvoke = addReflectionInvoke(sootClass);
        VirtualInvokeExpr reflectInvokeInvoke = Jimple.v().newVirtualInvokeExpr(thisLocal, reflectInvoke.makeRef(), linkedListLocal);
        InvokeStmt reflectInvokeStmt = Jimple.v().newInvokeStmt(reflectInvokeInvoke);
        InsertStmtEnd(reflectInvokeStmt, sootMethod);

        return reflectInvokeStmt;



        /*


       @Test
       test1() {}

       @Test
       test2() {}

       @Test
       test3() {}

       @Ignore
       testPolluter() {}

       @Ignore
       testBrittle() {}

       @Test
       testMain() {}


         */


    }

}
