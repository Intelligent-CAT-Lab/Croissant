package com.mutation.resourceNotCreated;

import com.mutation.SootMutationOperator;
import com.mutation.SootTestInjector;
import soot.*;
import soot.jimple.*;
import soot.util.Chain;

import java.lang.reflect.Field;
import java.util.*;

public class newFileNullODMO extends SootMutationOperator {

    String fileName;

    @Override
    public List<InvokeStmt> locateUnits() {
        return null;
    }

    @Override
    public boolean isApplicable() {
        this.fileName = null;

        Chain<Unit> units = this.getUnits();
        Iterator<Unit> itr = units.snapshotIterator();

        while (itr.hasNext()) {
            Stmt stmt = (Stmt) itr.next();
            //specialinvoke $r0.<java.io.File: void <init>(java.lang.String)>("./file.txt");

            if (!(stmt instanceof InvokeStmt)) {
                continue;
            }

            InvokeStmt invokeStmt = (InvokeStmt) stmt;
            InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();

            if (!(invokeExpr instanceof SpecialInvokeExpr)) {
                continue;
            }

            SpecialInvokeExpr specialInvokeExpr = (SpecialInvokeExpr) invokeExpr;

            if (specialInvokeExpr.getMethod().getSignature().equals("<java.io.File: void <init>(java.lang.String)>")) {
                this.fileName = ((StringConstant) specialInvokeExpr.getArg(0)).value;
                System.out.println(this.fileName);
            }
        }

        return this.fileName != null;
    }

    private SootMethod createVictimTest() {
        Scene.v().forceResolve("java.io.File", SootClass.BODIES);
        SootMethod victimTest = this.createTestMethod("CroissantMutant_OD_RA_Brittle");
        victimTest.setActiveBody(
            (Body) this.getCurrentMethod().getActiveBody().clone()
        );

        Chain<Unit> units = victimTest.getActiveBody().getUnits();
        Iterator<Unit> itr = units.snapshotIterator();

        Local fileLocal = null;

        while(itr.hasNext()) {
            Stmt stmt = (Stmt) itr.next();
            //specialinvoke $r0.<java.io.File: void <init>(java.lang.String)>("./file.txt");

            if (!(stmt instanceof InvokeStmt)) {
                continue;
            }

            InvokeStmt invokeStmt = (InvokeStmt) stmt;
            InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();

            if (!(invokeExpr instanceof SpecialInvokeExpr)) {
                continue;
            }

            SpecialInvokeExpr specialInvokeExpr = (SpecialInvokeExpr) invokeExpr;

            if (specialInvokeExpr.getMethod().getSignature().equals("<java.io.File: void <init>(java.lang.String)>")) {
                String fileName = ((StringConstant) specialInvokeExpr.getArg(0)).value;

                String[] parts = fileName.split("\\.");
                parts[parts.length -2] += "_Mutant";
                StringJoiner stringJoiner = new StringJoiner(".");

                for (String string: parts) {
                    stringJoiner.add(string);
                }

                StringConstant stringConstant = StringConstant.v(stringJoiner.toString());
                specialInvokeExpr.setArg(0, stringConstant);

                fileLocal = (Local) specialInvokeExpr.getBase();
            }
        }

        Unit pivot = null;
        Iterator<Unit> unitIterator = victimTest.getActiveBody().getUnits().snapshotIterator();

        while(unitIterator.hasNext()) {
            Unit unit = unitIterator.next();
            Stmt stmt = (Stmt) unit;

            //specialinvoke $r0.<java.io.File: void <init>(java.lang.String)>("test_Mutant.txt")

            if (!(stmt instanceof InvokeStmt)) {
                continue;
            }

            InvokeStmt invokeStmt = (InvokeStmt) stmt;
            InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();

            if (!(invokeExpr instanceof SpecialInvokeExpr)) {
                continue;
            }

            if (invokeExpr.getMethod().getSignature().contains("<java.io.File: void <init>(java.lang.String)>")) {
                pivot = unit;
            }
        }

        List<Stmt> stmts = new ArrayList<>();

        Local newFileLocal = Jimple.v().newLocal("newFileLocal", RefType.v("java.io.File"));
        victimTest.getActiveBody().getLocals().add(newFileLocal);
        AssignStmt initNewFileLocal = Jimple.v().newAssignStmt(newFileLocal, fileLocal);
        /*this.InsertStmtEnd(victimTest, initNewFileLocal);*/
        stmts.add(initNewFileLocal);

        //$z0 = virtualinvoke $r0.<java.io.File: boolean exists()>();
        SootClass sootClass = Scene.v().loadClassAndSupport("java.io.File");
        SootMethod sootMethod = sootClass.getMethodByName("exists");
        SootMethod existsMethod = Scene.v().getMethod("<java.io.File: boolean exists()>");
        VirtualInvokeExpr virtualInvokeExpr = Jimple.v().newVirtualInvokeExpr(newFileLocal, existsMethod.makeRef());
        Local boolLocal = Jimple.v().newLocal("boolLocal", BooleanType.v());
        victimTest.getActiveBody().getLocals().add(boolLocal);
        AssignStmt assignStmt = Jimple.v().newAssignStmt(boolLocal, virtualInvokeExpr);
        /*this.InsertStmtEnd(victimTest, assignStmt); //FIXME*/
        stmts.add(assignStmt);


        SootMethod assertTrueMethod = this.getAssertTrue();
        StaticInvokeExpr assertFalseExpr = Jimple.v().newStaticInvokeExpr(assertTrueMethod.makeRef(), boolLocal);
        InvokeStmt assertInvokeStmt = Jimple.v().newInvokeStmt(assertFalseExpr);
        /*this.InsertStmtEnd(victimTest, assertInvokeStmt);*/
        stmts.add(assertInvokeStmt);

        ListIterator<Stmt> listIterator = stmts.listIterator(stmts.size());

        while(listIterator.hasPrevious()) {
            victimTest.getActiveBody().getUnits().insertAfter(listIterator.previous(), pivot);
        }


        /*$r0 = staticinvoke <java.lang.Boolean: java.lang.Boolean valueOf(boolean)>($z0)*//*
        Local boolean1 = Jimple.v().newLocal("boolean1", RefType.v("java.lang.Boolean"));
        victimTest.getActiveBody().getLocals().add(boolean1);
        SootMethod valueOf = Scene.v().getMethod("<java.lang.Boolean: java.lang.Boolean valueOf(boolean)>");
        StaticInvokeExpr staticInvokeExpr = Jimple.v().newStaticInvokeExpr(valueOf.makeRef(), boolLocal);
        AssignStmt assignStmt1 = Jimple.v().newAssignStmt(boolean1, staticInvokeExpr);
        this.InsertStmtEnd(victimTest, assignStmt1);

        //$z1 = virtualinvoke $r0.<java.lang.Boolean: boolean booleanValue()>()
        Local boolean2 = Jimple.v().newLocal("boolean2", RefType.v("java.lang.Boolean"));
        victimTest.getActiveBody().getLocals().add(boolean2);
        SootMethod booleanValue = Scene.v().getMethod("<java.lang.Boolean: boolean booleanValue()>");
        VirtualInvokeExpr virtualInvokeExpr1 = Jimple.v().newVirtualInvokeExpr(boolean1, booleanValue.makeRef());
        AssignStmt assignStmt2 = Jimple.v().newAssignStmt(boolean2, virtualInvokeExpr1);
        this.InsertStmtEnd(victimTest, assignStmt2);

        SootMethod assertTrueMethod = this.getAssertTrue();
        StaticInvokeExpr assertFalseExpr = Jimple.v().newStaticInvokeExpr(assertTrueMethod.makeRef(), boolean2);
        InvokeStmt assertInvokeStmt = Jimple.v().newInvokeStmt(assertFalseExpr);
        this.InsertStmtEnd(victimTest, assertInvokeStmt);*/

        return victimTest;
    }

    public SootMethod createstateSetterTest() {
        SootMethod stateSetterTest = this.createTestMethod("stateSetterMutant"+"_NewFileNull_" + this.getCurrentMethod().getName());
        stateSetterTest.setActiveBody(
            (Body) this.getCurrentMethod().getActiveBody().clone()
        );

        Chain<Unit> units = stateSetterTest.getActiveBody().getUnits();
        Iterator<Unit> itr = units.snapshotIterator();

        Local fileLocal = null;

        while(itr.hasNext()) {
            Stmt stmt = (Stmt) itr.next();
            //specialinvoke $r0.<java.io.File: void <init>(java.lang.String)>("./file.txt");

            if (!(stmt instanceof InvokeStmt)) {
                continue;
            }

            InvokeStmt invokeStmt = (InvokeStmt) stmt;
            InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();

            if (!(invokeExpr instanceof SpecialInvokeExpr)) {
                continue;
            }

            SpecialInvokeExpr specialInvokeExpr = (SpecialInvokeExpr) invokeExpr;

            if (specialInvokeExpr.getMethod().getSignature().equals("<java.io.File: void <init>(java.lang.String)>")) {
                String fileName = ((StringConstant) specialInvokeExpr.getArg(0)).value;

                String[] parts = fileName.split("\\.");
                parts[parts.length -2] += "_Mutant";
                StringJoiner stringJoiner = new StringJoiner(".");

                for (String string: parts) {
                    stringJoiner.add(string);
                }

                StringConstant stringConstant = StringConstant.v(stringJoiner.toString());
                specialInvokeExpr.setArg(0, stringConstant);

                fileLocal = (Local) specialInvokeExpr.getBase();
            }
        }
        //virtualinvoke r1.<java.io.File: boolean createNewFile()>();

        SootMethod createNewFileMethod = Scene.v().getMethod("<java.io.File: boolean createNewFile()>");
        VirtualInvokeExpr createNewFileExpr = Jimple.v().newVirtualInvokeExpr(fileLocal , createNewFileMethod.makeRef());
        InvokeStmt invokeStmt = Jimple.v().newInvokeStmt(createNewFileExpr);

        this.InsertStmtEnd(stateSetterTest, invokeStmt);

        return stateSetterTest;

    }
    // 50 stateunsetters
    public SootMethod createstateUnsetterTest(int stateSetterTestNumber) {
        // virtualinvoke r1.<java.io.File: boolean delete()>();
        SootMethod polluterTest = this.createTestMethod("stateUnsetterMutant_NewFileNull_" +stateSetterTestNumber + this.getCurrentMethod().getName());
        polluterTest.setActiveBody(
            (Body) this.getCurrentMethod().getActiveBody().clone()
        );

        Chain<Unit> units = polluterTest.getActiveBody().getUnits();
        Iterator<Unit> itr = units.snapshotIterator();

        Local fileLocal = null;

        while(itr.hasNext()) {
            Stmt stmt = (Stmt) itr.next();
            //specialinvoke $r0.<java.io.File: void <init>(java.lang.String)>("./file.txt");

            if (!(stmt instanceof InvokeStmt)) {
                continue;
            }

            InvokeStmt invokeStmt = (InvokeStmt) stmt;
            InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();

            if (!(invokeExpr instanceof SpecialInvokeExpr)) {
                continue;
            }

            SpecialInvokeExpr specialInvokeExpr = (SpecialInvokeExpr) invokeExpr;

            if (specialInvokeExpr.getMethod().getSignature().equals("<java.io.File: void <init>(java.lang.String)>")) {
                String fileName = ((StringConstant) specialInvokeExpr.getArg(0)).value;

                String[] parts = fileName.split("\\.");
                parts[parts.length -2] += "_Mutant";
                StringJoiner stringJoiner = new StringJoiner(".");

                for (String string: parts) {
                    stringJoiner.add(string);
                }

                StringConstant stringConstant = StringConstant.v(stringJoiner.toString());
                specialInvokeExpr.setArg(0, stringConstant);

                fileLocal = (Local) specialInvokeExpr.getBase();
            }
        }

        // virtualinvoke r1.<java.io.File: boolean delete()>();
        SootMethod deleteMethod = Scene.v().getMethod("<java.io.File: boolean delete()>");
        VirtualInvokeExpr virtualInvokeExpr = Jimple.v().newVirtualInvokeExpr(fileLocal, deleteMethod.makeRef());
        InvokeStmt invokeStmt = Jimple.v().newInvokeStmt(virtualInvokeExpr);

        Chain<SootField> fields = this.getCurrentClass().getFields();
//        SootField sootField = new SootField("helper",RefType.v("java.lang.String"));
//        this.getCurrentClass().addField(sootField);


        this.InsertStmtEnd(polluterTest, invokeStmt);

        return this.makestateSetterTest(polluterTest,stateSetterTestNumber);
        //return polluterTest;
    }
//    public SootMethod createstateSetterTest() {
//
//        SootMethod stateSetterTest = this.createTestMethod("stateSetterMutant"+"_NewFileNull_" + this.getCurrentMethod().getName());
//        stateSetterTest.setActiveBody(
//            (Body) this.getCurrentMethod().getActiveBody().clone()
//        );
//
//        Chain<Unit> units = stateSetterTest.getActiveBody().getUnits();
//        Iterator<Unit> itr = units.snapshotIterator();
//
//        Local fileLocal = null;
//
//        while(itr.hasNext()) {
//            Stmt stmt = (Stmt) itr.next();
//            //specialinvoke $r0.<java.io.File: void <init>(java.lang.String)>("./file.txt");
//
//            if (!(stmt instanceof InvokeStmt)) {
//                continue;
//            }
//
//            InvokeStmt invokeStmt = (InvokeStmt) stmt;
//            InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();
//
//            if (!(invokeExpr instanceof SpecialInvokeExpr)) {
//                continue;
//            }
//
//            SpecialInvokeExpr specialInvokeExpr = (SpecialInvokeExpr) invokeExpr;
//
//            if (specialInvokeExpr.getMethod().getSignature().equals("<java.io.File: void <init>(java.lang.String)>")) {
//                String fileName = ((StringConstant) specialInvokeExpr.getArg(0)).value;
//
//                String[] parts = fileName.split("\\.");
//                parts[parts.length -2] += "_Mutant";
//                StringJoiner stringJoiner = new StringJoiner(".");
//
//                for (String string: parts) {
//                    stringJoiner.add(string);
//                }
//
//                StringConstant stringConstant = StringConstant.v(stringJoiner.toString());
//                specialInvokeExpr.setArg(0, stringConstant);
//
//                fileLocal = (Local) specialInvokeExpr.getBase();
//            }
//        }
//        //virtualinvoke r1.<java.io.File: boolean createNewFile()>();
//
//        SootMethod createNewFileMethod = Scene.v().getMethod("<java.io.File: boolean createNewFile()>");
//        VirtualInvokeExpr createNewFileExpr = Jimple.v().newVirtualInvokeExpr(fileLocal , createNewFileMethod.makeRef());
//        InvokeStmt invokeStmt = Jimple.v().newInvokeStmt(createNewFileExpr);
//
//        this.InsertStmtEnd(stateSetterTest, invokeStmt);
//
//        return stateSetterTest;
//    }

    @Override
    public void mutateMethod() throws Exception { //FIXME
        long startTime = System.currentTimeMillis();
//        SootMethod brittle = this.createVictimTest();
//        this.getCurrentClass().addMethod(brittle); //FIXME
//
//        SootMethod stateStterTest = this.createstateSetterTest();
//        this.getCurrentClass().addMethod(stateStterTest); //FIXME
//
//        for (int i = 0; i < 50; i++) {
//            SootMethod stateUnsetterTest = this.createstateUnsetterTest(i);
//            this.getCurrentClass().addMethod(stateUnsetterTest);
//        }

        long endTime= System.currentTimeMillis();
        com.framework.OperatorSelector.MUTANT_TIME.put("CroissantMutant_OD_RA_Brittle", endTime - startTime);
    }

    @Override
    public int getMutantNumber() {
        return 0;
    }


}
