package com.mutation.unorderedCollections.iteration;

import com.mutation.SootMutationOperator;
import soot.*;
import soot.jimple.*;
import soot.util.Chain;

import java.io.IOException;
import java.util.*;

public abstract class IterationUnorderedCollectionMutationOperator extends SootMutationOperator {
    Map<String, SootMethod> tests;
    List<Local> setLocals;
    List<Stmt> assertionUnits;
    SootMethod originalMethod;

    public IterationUnorderedCollectionMutationOperator() {
        this.tests = new HashMap<>();
        this.setLocals = new ArrayList<>();
        this.assertionUnits = new ArrayList<>();
    }
    /*
    1. Locate unordered collection
    2. Insert mockHash function if not inserted
    3. Iterate over the collection and call function:
    4. Insert tests:
        1. while + iterator test
        2. Convert to array then iterate with for loop


    ------------- construction of loops ----------------

    List<Stmt> createLoop(
        Local (Local)
        boolean expr (Expr)
        execStmts (List<Stmt>)
    )

    List<Stmt> createForLoop(
        Local (Local of collection)
        execStmts
    )

    List<Stmt> createWhileLoop(
        Local (Local of iterator)
        execStmt
    )

    Local createIterator(
        Local (Local of collection)
    ) //returns the local of iterator

    Local getHasNext(
        Local (Local of iterator)
    )

    Local getNext(
        Local (Local of iterator)
        Type (Type which it will be casted)
    )


     */

    private List<Stmt> createLoop(AssignStmt evalStmt, IfStmt booleanStmt, List<Stmt> execStmts, Stmt continueStmt) {
        List<Stmt> loop = new ArrayList<>();

        loop.add(evalStmt);
        loop.add(booleanStmt);

        for (Stmt stmt : execStmts) {
            loop.add(stmt);
        }

        if (continueStmt != null) {
            loop.add(continueStmt);
        }

        Stmt gotoStmt = Jimple.v().newGotoStmt(evalStmt);
        loop.add(gotoStmt);

        return loop;
    }

    protected abstract SootMethod getSizeFunction();

    protected abstract List<Stmt> getExecStmts();

    protected final List<Stmt> getAssertUnit(Local collectionLocal, Local resultLocal, Local resultLocalRValue, SootMethod test) {
        this.assertionUnits.clear();
        SootMethod initMethod = Scene.v().getMethod("<java.lang.Integer: java.lang.Integer valueOf(int)>");
        Expr initExpr = Jimple.v().newStaticInvokeExpr(initMethod.makeRef(), resultLocalRValue);
        AssignStmt initializeResultLocalStmt = Jimple.v().newAssignStmt(resultLocal, initExpr);
        this.assertionUnits.add(initializeResultLocalStmt);

        StaticInvokeExpr initializeExpected = Jimple.v().newStaticInvokeExpr(initMethod.makeRef(), IntConstant.v(0));
        Local expected = Jimple.v().newLocal("expected", RefType.v("java.lang.Integer"));
        test.getActiveBody().getLocals().add(expected);
        AssignStmt initializeExpectedStmt = Jimple.v().newAssignStmt(expected, initializeExpected);
        this.assertionUnits.add(initializeExpectedStmt);

        SootMethod assertEquals = this.getAssert();
        Value[] args = new Value[]{
            resultLocal,
            expected
        };
        StaticInvokeExpr assertInvocation = Jimple.v().newStaticInvokeExpr(assertEquals.makeRef(), args);
        InvokeStmt assertStmt = Jimple.v().newInvokeStmt(assertInvocation);
        this.assertionUnits.add(assertStmt);

        return assertionUnits;
    }

    protected List<Stmt> createWhileLoop(Local collectionLocal, Local resultLocal, Local resultLocalRValue, SootMethod test, Local newThisLocal) {

        try {
            test.getActiveBody().getLocals().add(collectionLocal);
        } catch (Exception e) {


        }

        List<Stmt> iteratorStmts = this.createIterator(collectionLocal, test);
        AssignStmt iteratorInitialization = (AssignStmt) iteratorStmts.get(iteratorStmts.size() - 1);
        Local iteratorLocal = (Local) iteratorInitialization.getLeftOp();

        Chain<Local> locals = test.getActiveBody().getLocals();
        if (!locals.contains(iteratorLocal)) {
            locals.add(iteratorLocal);
        }

        List<Stmt> assertionUnits = getAssertUnit(collectionLocal, resultLocal, resultLocalRValue, test);
        Stmt pivot = assertionUnits.get(0);

        AssignStmt evalStmt = this.getHasNext(iteratorLocal, test);
        Value evalStmtResult = evalStmt.getLeftOp();

        EqExpr ifHasNext = Jimple.v().newEqExpr(evalStmtResult, IntConstant.v(0));
        IfStmt booleanStmt = Jimple.v().newIfStmt(ifHasNext, pivot);

        List<Stmt> execStmts = new ArrayList<>();
        //TODO: dynamically get type
        Type type = this.getType(collectionLocal);
        if (type == null) {
            type = RefType.v("java.lang.Integer");
        }
        List<Stmt> getNextStmts = this.getNext(iteratorLocal, type, test);
        Local nextLocal = (Local) ((AssignStmt) getNextStmts.get(getNextStmts.size() - 1)).getLeftOp();
        execStmts.addAll(getNextStmts);
        SootMethod pseudoHashFunction = this.getCurrentClass().getMethodByName("pseudoHashFunction");

        Local thisLocal = newThisLocal;
        VirtualInvokeExpr hashFunctionInvocation = Jimple.v().newVirtualInvokeExpr(thisLocal, pseudoHashFunction.makeRef(), nextLocal);
        Local hashFunctionResult = Jimple.v().newLocal("hashFunctionResult", IntType.v());
        test.getActiveBody().getLocals().add(hashFunctionResult);
        AssignStmt hashFunctionResultInit = Jimple.v().newAssignStmt(hashFunctionResult, hashFunctionInvocation);
        execStmts.add(hashFunctionResultInit);

        AddExpr addResultWithHashResult = Jimple.v().newAddExpr(resultLocalRValue, hashFunctionResult);
        AssignStmt updateResultLocal = Jimple.v().newAssignStmt(resultLocalRValue, addResultWithHashResult);
        execStmts.add(updateResultLocal);

        List<Stmt> loop = createLoop(evalStmt, booleanStmt, execStmts, null);

        for (int i = 0; i < iteratorStmts.size(); i++) {
            loop.add(i, iteratorStmts.get(i));
        }

        return loop;
    }


    protected List<Stmt> createForLoop(Local collectionLocal, Local resultLocal, Local resultLocalRValue, SootMethod test) {

        Chain<Local> testLocals = test.getActiveBody().getLocals();
        List<Stmt> assertionUnits = getAssertUnit(collectionLocal, resultLocal, resultLocalRValue, test);
        Unit pivot = assertionUnits.get(0);

        Local sizeFunctionResult = Jimple.v().newLocal("sizeFunctionResult", IntType.v());
        if (!testLocals.contains(sizeFunctionResult)) {
            test.getActiveBody().getLocals().add(sizeFunctionResult);
        }

        InterfaceInvokeExpr invokeExpr = Jimple.v().newInterfaceInvokeExpr(collectionLocal, getSizeFunction().makeRef());
        AssignStmt evalStmt = Jimple.v().newAssignStmt(sizeFunctionResult, invokeExpr);

        Local currentIndex = Jimple.v().newLocal("currentIndex", IntType.v());
        if (!testLocals.contains(currentIndex)) {
            test.getActiveBody().getLocals().add(currentIndex);
        }
        Stmt initalizeCurrentIndex = Jimple.v().newAssignStmt(currentIndex, IntConstant.v(0));
        //this.InsertStmtBeginning(initalizeCurrentIndex);

        GeExpr geExpr = Jimple.v().newGeExpr(currentIndex, sizeFunctionResult);
        IfStmt booleanStmt = Jimple.v().newIfStmt(geExpr, pivot);

        AddExpr updateCurrentIndex = Jimple.v().newAddExpr(currentIndex, IntConstant.v(1));
        Stmt continueStmt = Jimple.v().newAssignStmt(currentIndex, updateCurrentIndex);

        List<Stmt> loop = createLoop(evalStmt, booleanStmt, getExecStmts(), continueStmt);
        loop.add(0, initalizeCurrentIndex);

        return loop;

    }

    protected abstract SootMethod getIteratorMethod();

    protected List<Stmt> createIterator(Local collectionLocal, SootMethod test) {

        List<Stmt> iteratorStmts = new ArrayList<>();

        Chain<Local> locals = test.getActiveBody().getLocals();
        if (!locals.contains(collectionLocal)) {
            locals.add(collectionLocal);
        }

        SootClass iteratorClass = Scene.v().getSootClass("java.util.Iterator");
        Local iteratorLocal = Jimple.v().newLocal("iteratorLocal", iteratorClass.getType());
        test.getActiveBody().getLocals().add(iteratorLocal);

        SootMethod iteratorMethod = getIteratorMethod();
        InterfaceInvokeExpr invokeExpr = Jimple.v().newInterfaceInvokeExpr(collectionLocal, iteratorMethod.makeRef());
        AssignStmt assignStmt = Jimple.v().newAssignStmt(iteratorLocal, invokeExpr);

        iteratorStmts.add(assignStmt);

        return iteratorStmts;
    }

    @Override
    public <T extends Unit> List<T> locateUnits() {
        return null;
    }


    protected AssignStmt getHasNext(Local iteratorLocal, SootMethod test) {
        Scene.v().forceResolve("java.util.Iterator", SootClass.SIGNATURES);
        Local booleanLocal = Jimple.v().newLocal("booleanLocal", BooleanType.v());
        test.getActiveBody().getLocals().add(booleanLocal);

        SootClass iteratorClass = Scene.v().getSootClass("java.util.Iterator");
        SootMethod hasNextMethod = iteratorClass.getMethodByName("hasNext");
        InterfaceInvokeExpr hasNextExpr = Jimple.v().newInterfaceInvokeExpr(iteratorLocal, hasNextMethod.makeRef());
        AssignStmt hasNextStmt = Jimple.v().newAssignStmt(booleanLocal, hasNextExpr);

        return hasNextStmt;
    }

    protected List<Stmt> getNext(Local iteratorLocal, Type type, SootMethod test) {

        //Type should be RefType.v(java.lang.Integer) etc NOT IntType.v()

        if (type.equals(NullType.v())) {
            type = RefType.v("java.lang.Integer");
        }

        List<Stmt> list = new ArrayList<>();


        Scene.v().forceResolve("java.lang.Object", SootClass.BODIES);
        Local objectLocal = Jimple.v().newLocal("objectLocal", RefType.v("java.lang.Object"));
        test.getActiveBody().getLocals().add(objectLocal);

        SootClass iteratorClass = Scene.v().getSootClass("java.util.Iterator");
        SootMethod nextMethod = iteratorClass.getMethodByName("next");
        InterfaceInvokeExpr hasNextExpr = Jimple.v().newInterfaceInvokeExpr(iteratorLocal, nextMethod.makeRef());
        AssignStmt assignStmt = Jimple.v().newAssignStmt(objectLocal, hasNextExpr);
        list.add(assignStmt);

        Local castedLocal = Jimple.v().newLocal("castedLocal", type);
        test.getActiveBody().getLocals().addLast(castedLocal);
        CastExpr castExpr = Jimple.v().newCastExpr(objectLocal, type);
        AssignStmt castAssign = Jimple.v().newAssignStmt(castedLocal, castExpr);
        list.add(castAssign);

        return list;
    }

    private void addLoop(List<Stmt> loop, Local local, SootMethod test) {
        this.getJimpleBody().getUnits().addAll(loop);
        for (Stmt stmt : loop) {
            this.InsertStmtEnd(test, stmt);
        }

        for (Stmt assertionUnit : this.assertionUnits) {
            this.InsertStmtEnd(test, assertionUnit);
        }
    }


    SootMethod createPseudoHashFunction() {

        List<Stmt> execStmts = new ArrayList<>();

        Local arg = Jimple.v().newLocal("arg", RefType.v("java.lang.Object"));
        IdentityStmt paramIdentityStmt = Jimple.v().newIdentityStmt(arg, Jimple.v().newParameterRef(RefType.v("java.lang.Object"), 0));
        execStmts.add(paramIdentityStmt);

        Local returnValueRValue = Jimple.v().newLocal("returnValueRValueRValue", IntType.v());
        AssignStmt initReturnValue = Jimple.v().newAssignStmt(returnValueRValue, IntConstant.v(1));
        execStmts.add(initReturnValue);

        /*Local returnValue = Jimple.v().newLocal("returnValue", IntType.v());*/
/*
        SootMethod initMethod = Scene.v().getMethod("<java.lang.Integer: java.lang.Integer valueOf(int)>");
        StaticInvokeExpr initExpr = Jimple.v().newStaticInvokeExpr(initMethod.makeRef(), returnValueRValue);
        AssignStmt initializeReturn = Jimple.v().newAssignStmt(returnValue, initExpr);
        execStmts.add(initializeReturn);
*/

        List<Type> params = new ArrayList<>();
        params.add(RefType.v("java.lang.Object"));


        //TODO: refactor returnValueRValue name
        SootMethod pseudoHash = this.createMethod("pseudoHashFunction", execStmts, returnValueRValue, params);
        pseudoHash.getActiveBody().getLocals().add(arg);
        /*pseudoHash.getActiveBody().getLocals().add(returnValueRValue);*/

        pseudoHash.setDeclaringClass(this.getCurrentClass());

        return pseudoHash;
    }

    protected abstract String getCollectionName();

    @Override
    protected void addNonDeterminism(SootMethod test) {


        SootMethod getThresholdMethod = this.createGetThresholdMethod();

        try {
            this.getCurrentClass().addMethod(getThresholdMethod);
        } catch (Exception e) {
            getThresholdMethod = this.getCurrentClass().getMethod(getThresholdMethod.getSubSignature());
        }


        Local probabilityResult = Jimple.v().newLocal("probabilityResult", DoubleType.v());
        test.getActiveBody().getLocals().add(probabilityResult);
        SootMethod randomMethod = this.getRandomMethod();
        StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(randomMethod.makeRef());
        AssignStmt assignStmt = Jimple.v().newAssignStmt(probabilityResult, staticInvokeExpr);

        Local thisLocal = this.getOrCreateThisLocal(test);
        Local thresholdLocal = Jimple.v().newLocal("thresholdLocal", DoubleType.v());
        test.getActiveBody().getLocals().add(thresholdLocal);
        VirtualInvokeExpr getThresholdInvoke = Jimple.v().newVirtualInvokeExpr(thisLocal, getThresholdMethod.makeRef());
        AssignStmt initializeThresholdLocal = Jimple.v().newAssignStmt(thresholdLocal, getThresholdInvoke);

        Chain<Unit> units = test.getActiveBody().getUnits();
        Stmt firstStmt = null;

        Iterator iterator = units.snapshotIterator();

        while(iterator.hasNext()) {
            Stmt stmt = (Stmt) iterator.next();

            if (stmt instanceof IdentityStmt) {
                continue;
            } else {
                firstStmt = stmt;
                break;
            }
        }

        Stmt returnStmt = Jimple.v().newReturnVoidStmt();

        this.InsertStmtBeginning(test, returnStmt);
        this.InsertStmtBeginning(test, assignStmt);
        this.InsertStmtBeginning(test, initializeThresholdLocal);


        //IfStmt ifStmt = Jimple.v().newIfStmt(Jimple.v().newLeExpr(probabilityResult, thresholdLocal), firstStmt);
        //units.insertBefore(ifStmt,returnStmt);

    }

    String createWhileLoopTest(int i) {

        SootMethod currentMethod = this.getCurrentClass().getMethod(this.originalMethod.getSubSignature());
        String methodName = "CroissantMutant_NOD_IUC_"+currentMethod.getName() + "whileLoop" + this.getClass().getSimpleName();

        SootMethod whileLoopTest = new SootMethod(methodName, currentMethod.getParameterTypes(), currentMethod.getReturnType(), currentMethod.getModifiers(), currentMethod.getExceptions());
        whileLoopTest.setActiveBody((Body) currentMethod.retrieveActiveBody().clone());

        whileLoopTest.addTag(this.createTestAnnotation());

        Chain<Unit> units = whileLoopTest.getActiveBody().getUnits();
        Iterator<Unit> itr = units.snapshotIterator();


        Local oldThisLocal = null;
        Local newThisLocal = null;

        while(itr.hasNext()) {
            Stmt stmt = (Stmt) itr.next();

            if (!(stmt instanceof IdentityStmt)) {
                continue;
            }

            IdentityStmt identityStmt = (IdentityStmt) stmt;

            if (!(identityStmt.getRightOp() instanceof ThisRef)) {
                continue;
            }

            ThisRef thisRef = (ThisRef) identityStmt.getRightOp();

            ThisRef newThisRef = Jimple.v().newThisRef(RefType.v(currentMethod.getName() + "whileLoop"));

            identityStmt.setRightOp(newThisRef);
            oldThisLocal = (Local) identityStmt.getLeftOp();
            newThisLocal = Jimple.v().newLocal(oldThisLocal.getName() + "new", RefType.v(whileLoopTest.getName()));
            whileLoopTest.getActiveBody().getLocals().add(newThisLocal);
            identityStmt.setLeftOp(newThisLocal);

            //this.addNonDeterminism(whileLoopTest);

            break;
        }


        itr = units.snapshotIterator();

        while(itr.hasNext()) {
            Stmt stmt = (Stmt) itr.next();

            if (stmt instanceof AssignStmt) {
                AssignStmt assignStmt = (AssignStmt) stmt;

                if (assignStmt.getRightOp() instanceof VirtualInvokeExpr) {
                    VirtualInvokeExpr virtualInvokeExpr = (VirtualInvokeExpr) assignStmt.getRightOp();

                    if (virtualInvokeExpr.getBase().equivTo(oldThisLocal)) {
                        virtualInvokeExpr.setBase(newThisLocal);
                    }


                }


            } else if (stmt instanceof InvokeStmt) {

                InvokeStmt invokeStmt = (InvokeStmt) stmt;

                if (invokeStmt.getInvokeExpr() instanceof VirtualInvokeExpr) {

                    VirtualInvokeExpr virtualInvokeExpr = (VirtualInvokeExpr) invokeStmt.getInvokeExpr();

                    if (virtualInvokeExpr.getBase().equivTo(oldThisLocal)) {
                        virtualInvokeExpr.setBase(newThisLocal);
                    }
                }

            }
        }


        Local resultLocalRValue = Jimple.v().newLocal("resultLocalRValue", IntType.v());
        whileLoopTest.getActiveBody().getLocals().add(resultLocalRValue);
        Local resultLocal = Jimple.v().newLocal("resultLocal", RefType.v("java.lang.Integer"));
        whileLoopTest.getActiveBody().getLocals().add(resultLocal);
        AssignStmt assignStmt = Jimple.v().newAssignStmt(resultLocalRValue, IntConstant.v(1));
        this.InsertStmtEnd(whileLoopTest, assignStmt);

        List<Local> CollectionLocals = getCollectionLocals(whileLoopTest, this.getCollectionName());
        /*setCurrentMethod(whileLoopTest);*/
        List<Stmt> whileLoop = createWhileLoop(CollectionLocals.get(i), resultLocal, resultLocalRValue, whileLoopTest, newThisLocal);
        addLoop(whileLoop, CollectionLocals.get(i), whileLoopTest);
        for (Local local: CollectionLocals) {
            try {
                whileLoopTest.getActiveBody().getLocals().add(local);
            } catch (Exception e) {
                /*e.printStackTrace();*/
            }
        }

        /*this.addNonDeterminism(whileLoopTest);*/

        this.tests.put("whileLoop", whileLoopTest);
        return methodName;
    }

    void createForLoopTest(int i) {
        /*//TODO: remove code duplication with createWhileLoopTest
        SootMethod currentMethod = this.originalMethod;

        SootMethod forLoopTest = new SootMethod(currentMethod.getName() + "forLoop"+ this.getClass().getSimpleName(), currentMethod.getParameterTypes(), currentMethod.getReturnType(), currentMethod.getModifiers(), currentMethod.getExceptions());
        forLoopTest.setActiveBody((Body) currentMethod.getActiveBody().clone());

        forLoopTest.addTag(this.createTestAnnotation());

        Local resultLocalRValue = Jimple.v().newLocal("resultLocalRValue", IntType.v());
        forLoopTest.getActiveBody().getLocals().add(resultLocalRValue);
        Local resultLocal = Jimple.v().newLocal("resultLocal", RefType.v("java.lang.Integer"));
        forLoopTest.getActiveBody().getLocals().add(resultLocal);
        AssignStmt assignStmt = Jimple.v().newAssignStmt(resultLocalRValue, IntConstant.v(1));
        this.InsertStmtEnd(forLoopTest, assignStmt);

        List<Local> locals = getCollectionLocals(forLoopTest, this.getCollectionName());
        setCurrentMethod(forLoopTest);
        List<Stmt> forLoop = createForLoop(locals.get(i), resultLocal, resultLocalRValue, forLoopTest);
        addLoop(forLoop, locals.get(i), forLoopTest);


        this.addNonDeterminism(forLoopTest);
        this.tests.put("forLoop", forLoopTest);*/
    }

    @Override
    public void mutateMethod() throws Exception {
        long startTime = System.currentTimeMillis();
        String testName = "";
        SootMethod pseudoHashMethod = this.createPseudoHashFunction();
        try {
            this.getCurrentClass().addMethod(pseudoHashMethod);
        } catch (Exception e) {
            //TODO: make exception more specific
            System.out.println("pseudohash has already been added");
        }
        this.tests = new HashMap<>();

        this.originalMethod = this.getCurrentMethod();

        for (int i = 0; i < this.setLocals.size(); i++) {

            String name = this.createWhileLoopTest(i);
            testName = name;
            /*this.createForLoopTest(i);*/
        }

        long endTime= System.currentTimeMillis();
        com.framework.OperatorSelector.MUTANT_TIME.put(testName,endTime - startTime);
    }

    @Override
    public void exportClass(String outputDir, String methodName) throws IOException {
        for (Map.Entry<String, SootMethod> entry : this.tests.entrySet()) {
            String testName = methodName + entry.getKey();
            SootMethod test = entry.getValue();
            try {
                this.getCurrentClass().addMethod(test);
            } catch (Exception e) {
                exportClass(outputDir);
            }
            exportClass(outputDir);

        }
    }

    @Override
    public int getMutantNumber() {
        return 2;
    }

    public List<Local> getCollectionLocals(SootMethod test, String collectionName) {
        Chain<Local> locals = test.getActiveBody().getLocals();
        Iterator<Local> localIterator = locals.snapshotIterator();
        List<Local> collectionLocals = new ArrayList<>();
        while (localIterator.hasNext()) {
            Local local = localIterator.next();
            if (local.getType().toString().contains(collectionName)) {
                collectionLocals.add(local);
            }
        }
        return collectionLocals;
    }

    @Override
    public boolean isApplicable() {
        this.setLocals.clear();
        this.assertionUnits = new ArrayList<>();
        this.setLocals = this.getCollectionLocals(this.getCurrentMethod(), this.getCollectionName());
        int trapChainSize = this.getCurrentMethod().getActiveBody().getTraps().size();
        return (this.setLocals.size() > 0) && trapChainSize == 0;
    }

    public abstract String getAddMethod();

    public Type getType(Local collectionLocal) {
        Chain<Unit> units = this.originalMethod.getActiveBody().getUnits();
        Iterator<Unit> iterator = units.snapshotIterator();

        while (iterator.hasNext()) {
            Stmt stmt = (Stmt) iterator.next();
            if (!stmt.containsInvokeExpr()) {
                continue;
            }

            InvokeExpr invokeExpr = stmt.getInvokeExpr();
            if (!(invokeExpr instanceof InstanceInvokeExpr)) {
                continue;
            }

            InstanceInvokeExpr instanceInvokeExpr = (InstanceInvokeExpr) invokeExpr;

            if (instanceInvokeExpr.getBase().toString().equals(collectionLocal.toString()) && instanceInvokeExpr.getMethod().getSignature().equals(this.getAddMethod())) {
                {
                    return instanceInvokeExpr.getArg(0).getType();
                }
            }
        }
        return null;
    }

}
