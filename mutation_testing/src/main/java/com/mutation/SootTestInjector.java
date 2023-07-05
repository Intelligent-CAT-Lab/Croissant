package com.mutation;


import soot.*;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.tagkit.Tag;
import soot.util.Chain;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

//TODO: remove instance variable injectTest so that STI handles multiple tests
public abstract class SootTestInjector extends SootMutationOperator {

    protected SootMethod injectTest;

    protected abstract String getTestName();

    public void createTest() {

        //create the test signature
        this.injectTest = Scene.v().makeSootMethod(getTestName(), new LinkedList<>(), VoidType.v(), Modifier.PUBLIC);
        //create body for the new test
        JimpleBody body = Jimple.v().newBody(this.injectTest);

        //add "this" local to the test because it is not static
        this.injectTest.setActiveBody(body);
        Local thisLocal = this.getOrCreateThisLocal(this.injectTest);

        //add return statement to the test
        body.getUnits().add(Jimple.v().newReturnVoidStmt());

        //add test annotation
        Tag testAnnotation = createTestAnnotation();
        this.injectTest.addTag(testAnnotation);
    }



    public void injectTest() {
        this.addNonDeterminism(this.injectTest);
        SootMethod sm = this.getCurrentClass().getMethodByNameUnsafe(this.injectTest.getName());
        if (sm == null) {
            getCurrentClass().addMethod(this.injectTest);
        }
    }


    public void injectTest(SootMethod sootMethod) {
        sootMethod.setDeclaringClass(this.getCurrentClass());
        /*if (!mainClass.declaresMethod(sootMethod.getSubSignature())) {
            mainClass.addMethod(sootMethod);
        }*/
    }

    @Override
    public void exportClass(String outputDir, String methodName) throws IOException {
        exportClass(outputDir);
    }

    /**
     * Get units of a method which can later be used for iteration over units
     * @return Chain object of all units in the target method
     */
    @Override
    public Chain<Unit> getUnits() {
        if (this.injectTest != null) {
            Body targetMethodBody = this.injectTest.getActiveBody();
            return targetMethodBody.getUnits();
        } else {
            return this.getCurrentMethod().getActiveBody().getUnits();
        }

    }
}
