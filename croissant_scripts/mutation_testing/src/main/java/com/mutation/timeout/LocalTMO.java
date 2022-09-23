package com.mutation.timeout;

import soot.*;
import soot.jimple.*;
import soot.tagkit.Tag;

import java.util.ArrayList;
import java.util.List;

public class LocalTMO extends TimeOutMutationOperator {


    private Tag timeoutTag;

    /*
    1. Locate test with timeout value
    2. Copy the test
    3. Call the original and the copy in the same test method (copy(); original();)
    4. Execute the test with a subprocess and measure how long it takes
    5. Add timeout value to the 1/4 of the execution time
     */

    /*
    timeOutTest1() {
        foo();
    }

    @Test(timeout = 10)
    timeOutTest2() {
        foo();
    }


    timeOutTest1 -> cached
    timeOutTest2 -> 9 ms to execute

    timeOutTest2 -> 11 ms to execute
    timeOutTest1 -> 9 ms to execute



     */

    @Override
    public int getMutantNumber() {
        return 1;
    }

    @Override
    public void mutateMethod() throws Exception {
        this.cloneTest();
        this.createMainTest();
        exportClass(this.inputDir, "mutant");
        long timeoutValue = this.getAppropriateTimeoutValue();
        addTimeOutAnnotation(this.cloneTest, timeoutValue);
    }

    private void addTimeOutAnnotation(SootMethod test, long timeoutValue) {
        Tag timeoutAnnotation = this.createTestAnnotationWithTimeout(timeoutValue);
        test.addTag(timeoutAnnotation);
    }


    public Local createThisLocal(SootMethod test) {
        //TODO: add to SootMutationOperator
        Local thisLocal = Jimple.v().newLocal("thisLocalTest", RefType.v(this.getCurrentClass().getName()));
        test.getActiveBody().getLocals().add(thisLocal);
        return thisLocal;
    }

    public void castThisLocal(SootMethod test, Local thisLocal, Local identityLocal) {
        //TODO: add to SootMutationOperator
        CastExpr castExpr = Jimple.v().newCastExpr(identityLocal, thisLocal.getType());
        AssignStmt assignStmt = Jimple.v().newAssignStmt(thisLocal, castExpr);
        InsertStmtEnd(test, assignStmt);
    }




    public void createMainTest() {
        SootMethod mainTest = new SootMethod(
            "testMain" + this.getCurrentMethod().getName(),
            new ArrayList<Type>(),
            VoidType.v(),
            Modifier.PUBLIC,
            new ArrayList<>()
        );
        Body mainTestBody = Jimple.v().newBody(mainTest);
        Local thisLocal = Jimple.v().newLocal("thisLocal", RefType.v(mainTest.getName()));
        mainTestBody.getLocals().add(thisLocal);
        IdentityStmt thisStmt = Jimple.v().newIdentityStmt(thisLocal, Jimple.v().newThisRef(RefType.v(mainTest.getName())));
        mainTestBody.getUnits().addFirst(thisStmt);


        mainTest.setActiveBody(mainTestBody);
        mainTest.getActiveBody().getUnits().addLast(Jimple.v().newReturnVoidStmt());
        this.getCurrentClass().addMethod(mainTest);

        Local identityLocal = getIdentityLocal(mainTest);

        /*castThisLocal(mainTest, thisLocal, identityLocal);*/

        VirtualInvokeExpr copyTestExpr = Jimple.v().newVirtualInvokeExpr(thisLocal, this.cloneTest.makeRef());
        InvokeStmt copyTestStmt = Jimple.v().newInvokeStmt(copyTestExpr);
        this.InsertStmtEnd(mainTest, copyTestStmt);

        VirtualInvokeExpr originalTestExpr = Jimple.v().newVirtualInvokeExpr(thisLocal, this.getCurrentClass().getMethod(this.getCurrentMethod().getSubSignature()).makeRef());
        InvokeStmt originalTestStmt = Jimple.v().newInvokeStmt(originalTestExpr);
        this.InsertStmtEnd(mainTest, originalTestStmt);

        mainTest.addTag(this.createTestAnnotation());

        this.mainTest = mainTest;
    }


    @Override
    public boolean isApplicable() {
        List<Tag> tags = this.getCurrentClass().getMethod(this.getCurrentMethod().getSubSignature()).getTags();
        for (Tag tag : tags) {
            if (tag.toString().contains("timeout value:")) {
                return true;
            }
        }
        return false;
    }

}
