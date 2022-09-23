package com.mutation.initializationOfTimeValuesTwice;

import com.mutation.SootTestInjector;
import soot.*;
import soot.jimple.Jimple;
import soot.jimple.LongConstant;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;

import java.util.ArrayList;
import java.util.List;

public class TimeValueInitializationMutationOperator extends SootTestInjector {


    /*
    String rep of source code and then compile it and then soot on it
    TOOD: Move it testInjector class
     */

    /**
     * Where the desired mutation is going to take place.
     * Classes which will extend from this abstract class need to implement this method.
     */
    @Override
    public void mutateMethod() {

        createTest();
        Local timeStamp1, timeStamp2;

        timeStamp1 = Jimple.v().newLocal("timeStamp1", LongType.v());
        timeStamp2 = Jimple.v().newLocal("timeStamp2", LongType.v());

        this.injectTest.getActiveBody().getLocals().addFirst(timeStamp1);
        this.injectTest.getActiveBody().getLocals().addFirst(timeStamp2);


        Scene.v().forceResolve("java.lang", SootClass.SIGNATURES);
        Scene.v().forceResolve("java.lang.System", SootClass.SIGNATURES);
        SootMethod timeStampInitMethod = Scene.v().getMethod("<java.lang.System: long currentTimeMillis()>");


        StaticInvokeExpr timeStampInitExpr = Jimple.v().newStaticInvokeExpr(timeStampInitMethod.makeRef());


        Stmt timeStampInitStmt1 = Jimple.v().newAssignStmt(timeStamp1, timeStampInitExpr);
        Stmt timeStampInitStmt2 = Jimple.v().newAssignStmt(timeStamp2, timeStampInitExpr);

        setCurrentMethod(this.injectTest);

        //*** add wait
        //<java.lang.Thread: void sleep(long)>

        Scene.v().forceResolve("java.lang.Thread", SootClass.SIGNATURES);
        SootMethod waitMethod = Scene.v().getMethod("<java.lang.Thread: void sleep(long)>");
        StaticInvokeExpr waitMethodInvocation = Jimple.v().newStaticInvokeExpr(waitMethod.makeRef(), LongConstant.v(1));
        Stmt waitMethodStmt = Jimple.v().newInvokeStmt(waitMethodInvocation);

        //***

        InsertStmtEnd(timeStampInitStmt1);
        InsertStmtEnd(waitMethodStmt);
        InsertStmtEnd(timeStampInitStmt2);

        Local timeStamp1R, timeStamp2R;

        timeStamp1R = Jimple.v().newLocal("timeStamp1R", RefType.v("java.lang.Long"));
        timeStamp2R = Jimple.v().newLocal("timeStamp2R", RefType.v("java.lang.Long"));

        this.injectTest.getActiveBody().getLocals().addFirst(timeStamp1R);
        this.injectTest.getActiveBody().getLocals().addFirst(timeStamp2R);

        SootMethod initMethod = Scene.v().getMethod("<java.lang.Long: java.lang.Long valueOf(long)>");

        StaticInvokeExpr initExpr1 = Jimple.v().newStaticInvokeExpr(initMethod.makeRef(), timeStamp1);
        StaticInvokeExpr initExpr2 = Jimple.v().newStaticInvokeExpr(initMethod.makeRef(), timeStamp2);

        Stmt initStmt1 = Jimple.v().newAssignStmt(timeStamp1R, initExpr1);
        Stmt initStmt2 = Jimple.v().newAssignStmt(timeStamp2R, initExpr2);

        InsertStmtEnd(initStmt1);

        InsertStmtEnd(initStmt2);


        try {
            Scene.v().forceResolve("org.junit.Assert", SootClass.BODIES);
        }
        catch (Exception e) {
            System.out.println("Assert does not exist");
        }
        SootMethod phantomAssert = getAssert();


        List<Value> args = new ArrayList<>();
        args.add(timeStamp1R);
        args.add(timeStamp2R);

        StaticInvokeExpr phantomExpr = Jimple.v().newStaticInvokeExpr(phantomAssert.makeRef(), args);

        Stmt phantomStmt = Jimple.v().newInvokeStmt(phantomExpr);

        InsertStmtEnd(phantomStmt);


        injectTest();

    }


    @Override
    protected String getTestName() {
        return "testTimeValueInitializationTwiceMutant";
    }

    @Override
    public List<SootField> locateUnits() {
        return null;
    }

    @Override
    public boolean isApplicable() {
        return true;
    }


    @Override
    public int getMutantNumber() {
        return 1;
    }

}
