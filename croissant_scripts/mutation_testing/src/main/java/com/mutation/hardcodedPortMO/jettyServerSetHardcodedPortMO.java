package com.mutation.hardcodedPortMO;

import com.mutation.SootMutationOperator;
import soot.*;
import soot.jimple.*;
import soot.util.Chain;

import java.util.*;

public class jettyServerSetHardcodedPortMO extends SootMutationOperator {
    SootClass phantomServerClass, phantomAssertClass;
    SootMethod phantomServerInit, phantomAssertFalse;

    @Override
    public void runOnceBefore() {
        // new ServerSocket(int);
        phantomServerClass = Scene.v().getSootClass("java.net.ServerSocket");
        phantomServerInit = new SootMethod("<init>",
            Collections.singletonList(RefType.v("java.net.ServerSocket")), VoidType.v(), Modifier.PUBLIC);
        phantomServerInit.setPhantom(true);
        List<Type> paramsServer = new ArrayList<>();
        paramsServer.add(IntType.v());
        phantomServerInit.setParameterTypes(paramsServer);
        phantomServerClass.addMethod(phantomServerInit);

        // Assert.assertFalse(Boolean);
        phantomAssertClass = Scene.v().getSootClass("org.junit.Assert");
        phantomAssertFalse = new SootMethod("assertFalse",
            Collections.singletonList(RefType.v("org.junit.Assert")), VoidType.v(), Modifier.PUBLIC | Modifier.STATIC);
        phantomAssertFalse.setPhantom(true);
        List<Type> paramsAssert = new ArrayList<>();
        paramsAssert.add(BooleanType.v());
        phantomAssertFalse.setParameterTypes(paramsAssert);
        phantomAssertClass.addMethod(phantomAssertFalse);

        Scene.v().forceResolve("java.io.IOException", SootClass.SIGNATURES);
        Scene.v().forceResolve("java.lang.String", SootClass.SIGNATURES);
    }

    @Override
    public void mutateMethod() throws Exception {
        Local pollutingServer = Jimple.v().newLocal("pollutingServer", RefType.v("java.net.ServerSocket"));
        getJimpleBody().getLocals().add(pollutingServer);

        Stmt newStmt = Jimple.v().newAssignStmt(pollutingServer, Jimple.v().newNewExpr(RefType.v("java.net.ServerSocket")));
        InsertStmtBeginning(newStmt);

        List<Unit> serverInitUnits = locateInit();
        ListIterator<Unit> InitIt = serverInitUnits.listIterator();

        IntConstant portNumber;

        if (InitIt.hasNext()) {
            Unit serverInitUnit = InitIt.next();
            portNumber = (IntConstant)((InvokeStmt) serverInitUnit).getInvokeExpr().getArg(0);
        } else {
            throw new RuntimeException("InitIt must include at least one server.start()!");
        }

        //SootMethod socketInit = Scene.v().getMethod("<java.net.ServerSocket: void <init>(int)>");
        SootMethod socketInit = phantomServerInit;
        SpecialInvokeExpr specialInvokeExpr = Jimple.v().newSpecialInvokeExpr(pollutingServer, socketInit.makeRef(), portNumber);
        Stmt socketServerStmt = Jimple.v().newInvokeStmt(specialInvokeExpr);
        getUnits().insertAfter(socketServerStmt, newStmt);

        // TODO utility function
        Local testVar = Jimple.v().newLocal("testVar", RefType.v("java.lang.String"));
        getJimpleBody().getLocals().add(testVar);

        Local caughtL = Jimple.v().newLocal("caughtL", RefType.v("java.io.IOException"));
        getJimpleBody().getLocals().add(caughtL);

        List<Unit> serverStartUnits = locateStart();
        ListIterator<Unit> StartIt = serverStartUnits.listIterator();

        if (StartIt.hasNext()) {
            Unit serverStartUnit = StartIt.next();

            GotoStmt gotoStmt = Jimple.v().newGotoStmt(getUnits().getSuccOf(serverStartUnit));

            // caughtL := @caughtexception;
           /* CaughtExceptionRef caughtRef = Jimple.v().newCaughtExceptionRef();
            IdentityStmt caughtStmt = Jimple.v().newIdentityStmt(caughtL, caughtRef);
            getUnits().insertAfter(caughtStmt, serverStartUnit);*/

            // Assert.assertFalse(exception.getMessage().contains("Failed to bind to"));
            SootMethod exceptionGetMessageMethod = Scene.v().getSootClass("java.io.IOException").getSuperclass().getSuperclass().getMethod("java.lang.String getMessage()");
            VirtualInvokeExpr exceptionGetMessageExpr = Jimple.v().newVirtualInvokeExpr(caughtL, exceptionGetMessageMethod.makeRef());
            AssignStmt testStmt = Jimple.v().newAssignStmt(testVar, StringConstant.v("a"));
            /*getUnits().insertAfter(testStmt, caughtStmt);*/
            this.InsertStmtEnd(testStmt);

            // TODO rename testX to something meaningful
            Local testResult = Jimple.v().newLocal("testResult", BooleanType.v());
            this.getCurrentMethod().getActiveBody().getLocals().add(testResult);
            SootMethod stringContains = Scene.v().getMethod("<java.lang.String: boolean contains(java.lang.CharSequence)>");
            VirtualInvokeExpr stringContainsInvoke = Jimple.v().newVirtualInvokeExpr(testVar, stringContains.makeRef(), StringConstant.v("Failed to bind to")); //FIXME: bunda problem var
            AssignStmt testStmt2 = Jimple.v().newAssignStmt(testResult, stringContainsInvoke);
            /*getUnits().insertAfter(testStmt2, testStmt);*/
            this.InsertStmtEnd(testStmt2);

            StaticInvokeExpr assertFalseExpr = Jimple.v().newStaticInvokeExpr(phantomAssertFalse.makeRef(), testResult);
            Stmt assertFalseStmt = Jimple.v().newInvokeStmt(assertFalseExpr);
            /*getUnits().insertAfter(assertFalseStmt, testStmt2);*/
            /*this.InsertStmtEnd(assertFalseStmt);*/

            Stmt throwStmt = Jimple.v().newThrowStmt(caughtL);
            /*getUnits().insertAfter(throwStmt, assertFalseStmt);*/
            /*this.InsertStmtEnd(throwStmt);*/
            // throw exception;

            /*getUnits().insertAfter(gotoStmt, serverStartUnit);*/

            SootClass exceptionClass = Scene.v().getSootClass("java.io.IOException");
            /*Trap serverStartTrap = Jimple.v().newTrap(exceptionClass, getUnits().getPredOf(serverStartUnit), gotoStmt, caughtStmt);

            getJimpleBody().getTraps().add(serverStartTrap);*/
        } else {
            throw new RuntimeException("StartIt must include at least one server.start()!");
        }

        this.addNonDeterminism(this.getCurrentMethod());
    }

    @Override
    public int getMutantNumber() {
        return 1;
    }

    @Override
    public <T extends Unit> List<T> locateUnits() {
        return null;
    }

    public List<Unit> locateStart() {
        Chain<Unit> units = getUnits();
        Iterator<Unit> itr = units.snapshotIterator();
        List<Unit> suitableAssertStmts = new ArrayList<>();

        while(itr.hasNext()) {
            Unit currentUnit = itr.next();
            Stmt currentStmt = (Stmt) currentUnit;
            if (currentStmt.containsInvokeExpr() && currentStmt.getInvokeExpr() instanceof VirtualInvokeExpr) {
                VirtualInvokeExpr currentVirtualInvoke = (VirtualInvokeExpr) currentStmt.getInvokeExpr();
                // FIXME ASSUMPTION: PACKAGE NAME
                if (currentVirtualInvoke.getMethod().toString().contains("<org.eclipse.jetty.server.Server: void start()>")) {
                    suitableAssertStmts.add(currentUnit);
                }
            }
        }
        return suitableAssertStmts;
    }

    public List<Unit> locateInit() {
        Chain<Unit> units = getUnits();
        Iterator<Unit> itr = units.snapshotIterator();
        List<Unit> suitableAssertStmts = new ArrayList<>();

        while(itr.hasNext()) {
            Unit currentUnit = itr.next();
            Stmt currentStmt = (Stmt) currentUnit;
            if (currentStmt.containsInvokeExpr() && currentStmt.getInvokeExpr() instanceof SpecialInvokeExpr) {
                SpecialInvokeExpr currentSpecialInvoke = (SpecialInvokeExpr) currentStmt.getInvokeExpr();
                // FIXME ASSUMPTION: PACKAGE NAME
                if (currentSpecialInvoke.getMethod().toString().contains("<org.eclipse.jetty.server.Server: void <init>(int)>")) {
                    suitableAssertStmts.add(currentUnit);
                }
            }
        }
        return suitableAssertStmts;
    }

    @Override
    public boolean isApplicable() {
        return locateStart().size() > 0 && locateInit().size() > 0;
    }
}
