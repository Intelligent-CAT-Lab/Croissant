package com.mutation.timeZoneDependency;


import com.mutation.SootMutationOperator;
import soot.*;
import soot.jimple.*;
import soot.util.Chain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

// TODO: Replace all TypeRef with XType.v()
// TODO: Make it abstract after designing multiple mutation operators
// TODO: Make util methods of frequent mutations

/**
 * Abstract class for mutation operators concerned with timezone dependency flakiness
 */
public class TimeZoneDependencyMO extends SootMutationOperator {

    ArrayList<Stmt> relevantStmts = new ArrayList<>();

    /**
     * Constructor for TimeZoneDependency Mutation Operator
     *
     * @param inputDir  Where the mutation operator will look for the compiled file
     * @param className Name of the class which will be mutated
     */
    public TimeZoneDependencyMO(String inputDir, String className, String startMethod) {
        super(inputDir, className, startMethod);
    }

    @Override
    public int getMutantNumber() {
        return relevantStmts.size();
    }

    public TimeZoneDependencyMO() {
        super();
    }

    /**
     * Locates assertion statements that are suitable for mutation
     *
     * @return list of suitable assertion statements
     */
    @Override
    public List<Unit> locateUnits() {
        Chain<Unit> units = getUnits();
        HashMap<Local, List<Stmt>> localToInvocation = this.localToStmtMap;
        Iterator<Unit> itr = units.snapshotIterator();
        List<Unit> suitableAssertStmts = new ArrayList<>();

        while(itr.hasNext()) {
            Unit currentUnit = itr.next();
            Stmt currentStmt = (Stmt) currentUnit;
            if (currentStmt.containsInvokeExpr() && currentStmt.getInvokeExpr() instanceof StaticInvokeExpr) {
                StaticInvokeExpr currentStaticInvoke = (StaticInvokeExpr) currentStmt.getInvokeExpr();
                // FIXME Make it more generic
                if (currentStaticInvoke.getMethod().toString().contains("void assertEquals(java.lang.Object,java.lang.Object)>")) {
                    //Located an assert statement


                    List<Stmt> statements = null;
                    if (currentStaticInvoke.getArg(1) instanceof Local) {
                        statements = localToInvocation.get((Local) currentStaticInvoke.getArg(1));
                    } else if (currentStaticInvoke.getArg(0) instanceof Local) {
                        statements = localToInvocation.get((Local) currentStaticInvoke.getArg(0));
                    }

                    if (statements == null) {
                        continue;
                    }

                    for (Stmt statement : statements) {
                        if (statement instanceof AssignStmt && ((AssignStmt) statement).getRightOp().toString().contains("<java.text.SimpleDateFormat: java.util.Date parse(java.lang.String)>")) {
                            // TODO: Investigate why add currentUnit for each statement?
                            suitableAssertStmts.add(currentUnit);
                        }
                    }

                }
            }
        }
        return suitableAssertStmts;
    }

    @Override
    public boolean isApplicable() {
        return locateUnits().size() > 0;
    }

    /* Add before each "<java.text.SimpleDateFormat: java.util.Date parse(java.lang.String)>":
        $r2 = staticinvoke <java.util.TimeZone: java.util.TimeZone getDefault()>()
        $i0 = virtualinvoke $r2.<java.util.TimeZone: int getRawOffset()>()
        $i1 = $i0 + 10800000
        $i2 = $i1 % 43200000
        $r3 = staticinvoke <java.util.TimeZone: java.lang.String[] getAvailableIDs(int)>($i2)
        $r4 = $r3[0]
        $r5 = staticinvoke <java.util.TimeZone: java.util.TimeZone getTimeZone(java.lang.String)>($r4)
        virtualinvoke r1.<java.text.SimpleDateFormat: void setTimeZone(java.util.TimeZone)>($r5)
    */
    /**
     * TODO
     */
    public void mutateMethod() {
        List<Unit> SuitableUnits = locateUnits();

        StaticInvokeExpr invokeExpr = (StaticInvokeExpr)(((InvokeStmt)SuitableUnits.get(0)).getInvokeExpr());
        Local dateParse = ((Local)invokeExpr.getArg(1));
        this.relevantStmts = (ArrayList<Stmt>) this.localToStmtMap.get(dateParse);
        AssignStmt latestDateParse = null;
        // TODO: Turn into function in mutation operator class
        for (Stmt relevantStmt : this.relevantStmts) {
            if (relevantStmt instanceof AssignStmt && ((AssignStmt) relevantStmt).getLeftOp().equivTo(dateParse)) {
                latestDateParse = (AssignStmt) relevantStmt;
            } else if (relevantStmt.containsInvokeExpr() && relevantStmt.getInvokeExpr().equivTo(invokeExpr)) {
                break;
            }
        }

        // $r2 = staticinvoke <java.util.TimeZone: java.util.TimeZone getDefault()>()
        Local tzGetDefaultL = Jimple.v().newLocal("varGetDefault", RefType.v("java.util.TimeZone"));
        getJimpleBody().getLocals().addFirst(tzGetDefaultL);
        SootMethod tzGetDefaultR = Scene.v().getMethod("<java.util.TimeZone: java.util.TimeZone getDefault()>");
        StaticInvokeExpr tzGetDefaultExpr = Jimple.v().newStaticInvokeExpr(tzGetDefaultR.makeRef());
        Stmt tzGetDefaultStmt = Jimple.v().newAssignStmt(tzGetDefaultL, tzGetDefaultExpr);
        getUnits().insertBefore(tzGetDefaultStmt, latestDateParse);

        // $i0 = virtualinvoke $r2.<java.util.TimeZone: int getRawOffset()>()
        Local tzGetRawOffsetL = Jimple.v().newLocal("varGetRawOffset", IntType.v());
        getJimpleBody().getLocals().addFirst(tzGetRawOffsetL);
        SootMethod tzGetRawOffsetR = Scene.v().getMethod("<java.util.TimeZone: int getRawOffset()>");
        VirtualInvokeExpr tzGetRawOffsetExpr = Jimple.v().newVirtualInvokeExpr(tzGetDefaultL, tzGetRawOffsetR.makeRef());
        Stmt tzGetRawOffsetStmt = Jimple.v().newAssignStmt(tzGetRawOffsetL, tzGetRawOffsetExpr);
        getUnits().insertBefore(tzGetRawOffsetStmt, latestDateParse);

        // $i1 = $i0 + 10800000
        Local mutatedOffsetL = Jimple.v().newLocal("mutatedOffset", IntType.v());
        getJimpleBody().getLocals().addFirst(mutatedOffsetL);
        AddExpr mutatedOffsetExpr = Jimple.v().newAddExpr(tzGetRawOffsetL, IntConstant.v(5*3600000));
        Stmt mutatedOffsetStmt = Jimple.v().newAssignStmt(mutatedOffsetL, mutatedOffsetExpr);
        getUnits().insertBefore(mutatedOffsetStmt, latestDateParse);

        // $i2 = $i1 % 43200000
        getUnits().insertBefore(Jimple.v().newAssignStmt(mutatedOffsetL, Jimple.v().newRemExpr(mutatedOffsetL, IntConstant.v(43200000))), latestDateParse);

        // $r3 = staticinvoke <java.util.TimeZone: java.lang.String[] getAvailableIDs(int)>($i2)
        Local tzGetAvailableIDsL = Jimple.v().newLocal("varGetAvailableIDs", ArrayType.v(RefType.v("Java.lang.String"), 1));
        getJimpleBody().getLocals().addFirst(tzGetAvailableIDsL);
        SootMethod tzGetAvailableIDsR = Scene.v().getMethod("<java.util.TimeZone: java.lang.String[] getAvailableIDs(int)>");
        StaticInvokeExpr tzGetAvailableIDsExpr = Jimple.v().newStaticInvokeExpr(tzGetAvailableIDsR.makeRef(), mutatedOffsetL);
        Stmt tzGetAvailableIDsStmt = Jimple.v().newAssignStmt(tzGetAvailableIDsL, tzGetAvailableIDsExpr);
        getUnits().insertBefore(tzGetAvailableIDsStmt, latestDateParse);

        // $r4 = $r3[0]
        Local tzFirstIDL = Jimple.v().newLocal("varFirstID", RefType.v("java.lang.Integer"));
        getJimpleBody().getLocals().addFirst(tzFirstIDL);
        Stmt tzFirstIDStmt = Jimple.v().newAssignStmt(tzFirstIDL, Jimple.v().newArrayRef(tzGetAvailableIDsL, IntConstant.v(0)));
        getUnits().insertBefore(tzFirstIDStmt, latestDateParse);

        //$r5 = staticinvoke <java.util.TimeZone: java.util.TimeZone getTimeZone(java.lang.String)>($r4)
        Local tzGetTimeZoneL = Jimple.v().newLocal("varGetTimeZone", RefType.v("java.util.TimeZone"));
        getJimpleBody().getLocals().addFirst(tzGetTimeZoneL);
        SootMethod tzGetTimeZoneR = Scene.v().getMethod("<java.util.TimeZone: java.util.TimeZone getTimeZone(java.lang.String)>");
        StaticInvokeExpr tzGetTimeZoneExpr = Jimple.v().newStaticInvokeExpr(tzGetTimeZoneR.makeRef(), tzFirstIDL);
        Stmt tzGetTimeZoneStmt = Jimple.v().newAssignStmt(tzGetTimeZoneL, tzGetTimeZoneExpr);
        getUnits().insertBefore(tzGetTimeZoneStmt, latestDateParse);

        // virtualinvoke r1.<java.text.SimpleDateFormat: void setTimeZone(java.util.TimeZone)>($r5)
        assert latestDateParse != null;
        Local SimpleDateFormat = (Local) (((VirtualInvokeExpr) (latestDateParse.getRightOp())).getBase());
        SootMethod setTimeZoneMethod = Scene.v().getMethod("<java.text.DateFormat: void setTimeZone(java.util.TimeZone)>");
        VirtualInvokeExpr setTimeZoneExpr = Jimple.v().newVirtualInvokeExpr(SimpleDateFormat, setTimeZoneMethod.makeRef(), tzGetTimeZoneL);
        Stmt setTimeZoneStmt = Jimple.v().newInvokeStmt(setTimeZoneExpr);
        getUnits().insertBefore(setTimeZoneStmt, latestDateParse);

        this.addNonDeterminism(this.getCurrentMethod());
    }
}
