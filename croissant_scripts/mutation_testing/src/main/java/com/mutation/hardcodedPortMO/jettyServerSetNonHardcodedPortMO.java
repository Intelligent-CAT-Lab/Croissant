package com.mutation.hardcodedPortMO;

import com.mutation.SootMutationOperator;
import soot.*;
import soot.jimple.*;
import soot.util.Chain;

import java.util.*;

public class jettyServerSetNonHardcodedPortMO extends SootMutationOperator {
    SootClass phantomServerClass, phantomAssertClass;
    SootMethod phantomServerInit, phantomServerGetPort, phantomAssertFalse;

    @Override
    public void runOnceBefore() {
        // new ServerSocket(int);
        phantomServerClass = Scene.v().getSootClass("java.net.ServerSocket");

        phantomServerInit = new SootMethod("<init>",
            Collections.singletonList(RefType.v("java.net.ServerSocket")), VoidType.v(), Modifier.PUBLIC);
        phantomServerInit.setPhantom(true);
        List<Type> paramsServerInit = new ArrayList<>();
        paramsServerInit.add(IntType.v());
        phantomServerInit.setParameterTypes(paramsServerInit);

        phantomServerGetPort = new SootMethod("getLocalPort",
            new LinkedList<>(), IntType.v(), Modifier.PUBLIC);
        phantomServerGetPort.setPhantom(true);
        /*List<Type> paramsServerGetPort = new ArrayList<>();
        paramsServerGetPort.add(IntType.v());
        phantomServerGetPort.setParameterTypes(paramsServerGetPort);*/

        try {
            phantomServerClass.addMethod(phantomServerInit);
            phantomServerClass.addMethod(phantomServerGetPort);
        } catch (Exception e) {

        }

        // Assert.assertFalse(Boolean);
        phantomAssertFalse = this.getAssertFalse();

        Scene.v().forceResolve("java.io.IOException", SootClass.SIGNATURES);
        Scene.v().forceResolve("java.lang.String", SootClass.SIGNATURES);
    }

    @Override
    public void mutateMethod() throws Exception {
        Local pollutingServer = Jimple.v().newLocal("pollutingServer", RefType.v("java.net.ServerSocket"));
        getJimpleBody().getLocals().add(pollutingServer);

        Local portNumber = Jimple.v().newLocal("portNumber", IntType.v());
        getJimpleBody().getLocals().add(portNumber);

        Stmt newStmt = Jimple.v().newAssignStmt(pollutingServer, Jimple.v().newNewExpr(RefType.v("java.net.ServerSocket")));
        InsertStmtBeginning(newStmt);

        getUnits().insertBefore(Jimple.v().newAssignStmt(portNumber, IntConstant.v(0)), newStmt);

        //SootMethod socketInit = Scene.v().getMethod("<java.net.ServerSocket: void <init>(int)>");
        SootMethod socketInit = phantomServerInit;
        SpecialInvokeExpr specialInvokeExpr = Jimple.v().newSpecialInvokeExpr(pollutingServer, socketInit.makeRef(), portNumber);
        Stmt socketServerStmt = Jimple.v().newInvokeStmt(specialInvokeExpr);
        getUnits().insertAfter(socketServerStmt, newStmt);

        List<Unit> serverInitUnits = locateInit();
        ListIterator<Unit> InitIt = serverInitUnits.listIterator();

        if (InitIt.hasNext()) {
            Unit serverInitUnit = InitIt.next();
            Local ServerBase = (Local)((SpecialInvokeExpr)((Stmt)serverInitUnit).getInvokeExpr()).getBase();
            InvokeStmt mutatedInit = Jimple.v().newInvokeStmt(Jimple.v().newSpecialInvokeExpr(ServerBase, phantomServerInit.makeRef(), portNumber));
            getUnits().insertBefore(mutatedInit, serverInitUnit);
            getUnits().remove(serverInitUnit);
            VirtualInvokeExpr getLocalPort = Jimple.v().newVirtualInvokeExpr(pollutingServer, phantomServerGetPort.makeRef());
            getUnits().insertAfter(Jimple.v().newAssignStmt(portNumber, getLocalPort), socketServerStmt);
        } else {
            throw new RuntimeException("InitIt must include at least one new Server()!");
        }

        // TODO utility function
        Local testVar = Jimple.v().newLocal("testVar", IntType.v());
        getJimpleBody().getLocals().add(testVar);

        Local caughtL = Jimple.v().newLocal("caughtL", RefType.v("java.io.IOException"));
        getJimpleBody().getLocals().add(caughtL);

        List<Unit> serverStartUnits = locateStart();
        ListIterator<Unit> StartIt = serverStartUnits.listIterator();

        if (StartIt.hasNext()) {
            Unit serverStartUnit = StartIt.next();

            GotoStmt gotoStmt = Jimple.v().newGotoStmt(getUnits().getSuccOf(serverStartUnit));

            // caughtL := @caughtexception;
            CaughtExceptionRef caughtRef = Jimple.v().newCaughtExceptionRef();
            IdentityStmt caughtStmt = Jimple.v().newIdentityStmt(caughtL, caughtRef);
            getUnits().insertAfter(caughtStmt, serverStartUnit);

            // Assert.assertFalse(exception.getMessage().contains("Failed to bind to"));
            SootMethod exceptionGetMessageMethod = Scene.v().getSootClass("java.io.IOException").getSuperclass().getSuperclass().getMethod("java.lang.String getMessage()");
            VirtualInvokeExpr exceptionGetMessageExpr = Jimple.v().newVirtualInvokeExpr(caughtL, exceptionGetMessageMethod.makeRef());
            AssignStmt testStmt = Jimple.v().newAssignStmt(testVar, exceptionGetMessageExpr);
            getUnits().insertAfter(testStmt, caughtStmt);

            // TODO rename testX to something meaningful
            SootMethod stringContains = Scene.v().getMethod("<java.lang.String: boolean contains(java.lang.CharSequence)>");
            VirtualInvokeExpr stringContainsInvoke = Jimple.v().newVirtualInvokeExpr(testVar, stringContains.makeRef(), StringConstant.v("Failed to bind to"));
            AssignStmt testStmt2 = Jimple.v().newAssignStmt(testVar, stringContainsInvoke);
            getUnits().insertAfter(testStmt2, testStmt);

            StaticInvokeExpr assertFalseExpr = Jimple.v().newStaticInvokeExpr(phantomAssertFalse.makeRef(), testVar);
            Stmt assertFalseStmt = Jimple.v().newInvokeStmt(assertFalseExpr);
            getUnits().insertAfter(assertFalseStmt, testStmt2);

            Stmt throwStmt = Jimple.v().newThrowStmt(caughtL);
            getUnits().insertAfter(throwStmt, assertFalseStmt);

            // throw exception;

            getUnits().insertAfter(gotoStmt, serverStartUnit);

            SootClass exceptionClass = Scene.v().getSootClass("java.io.IOException");
            Trap serverStartTrap = Jimple.v().newTrap(exceptionClass, getUnits().getPredOf(serverStartUnit), gotoStmt, caughtStmt);

            getJimpleBody().getTraps().add(serverStartTrap);
        } else {
            throw new RuntimeException("StartIt must include at least one server.start()!");
        }
        this.addNonDeterminism(this.getCurrentMethod());
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
                if (currentSpecialInvoke.getMethod().toString().contains("<org.eclipse.jetty.server.Server: void <init>()>")) {
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


    @Override
    public int getMutantNumber() {
        return 1;
    }
}
