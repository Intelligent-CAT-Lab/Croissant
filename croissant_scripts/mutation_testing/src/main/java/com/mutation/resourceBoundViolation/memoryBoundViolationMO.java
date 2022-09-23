package com.mutation.resourceBoundViolation;

import com.mutation.SootMutationOperator;
import soot.*;
import soot.jimple.*;

import java.util.ArrayList;
import java.util.List;

public class memoryBoundViolationMO extends SootMutationOperator {

    private SootMethod getGcMethod() {
        SootClass systemClass = Scene.v().forceResolve("java.lang.System", SootClass.SIGNATURES);

        for (SootMethod sootMethod : systemClass.getMethods()) {
            if (sootMethod.getSignature().equals("<java.lang.System: void gc()>")) {
                return sootMethod;
            }
        }
        SootMethod gcBefore = Scene.v().makeSootMethod("gc", new ArrayList<Type>(), VoidType.v(), Modifier.PUBLIC | Modifier.STATIC);
        gcBefore.setPhantom(true);
        systemClass.addMethod(gcBefore);
        return gcBefore;
    }

    public InvokeStmt gcStmt() {
        SootMethod gcBefore = this.getGcMethod();
        /*SootMethod gcBefore = systemClass.getMethod("<java.lang.System: void gc()>");*/
        StaticInvokeExpr gcBeforeInvoke = Jimple.v().newStaticInvokeExpr(gcBefore.makeRef());
        return Jimple.v().newInvokeStmt(gcBeforeInvoke);
    }

    @Override
    public void mutateMethod() throws Exception {
        Scene.v().forceResolve("java.lang.Runtime", SootClass.SIGNATURES);

        Local runtime = Jimple.v().newLocal("runtime", RefType.v("java.lang.Runtime"));
        getJimpleBody().getLocals().add(runtime);

        SootMethod getRuntime = Scene.v().getMethod("<java.lang.Runtime: java.lang.Runtime getRuntime()>");
        StaticInvokeExpr getRuntimeInvoke = Jimple.v().newStaticInvokeExpr(getRuntime.makeRef());
        AssignStmt getRuntimeStmt = Jimple.v().newAssignStmt(runtime, getRuntimeInvoke);
        InsertStmtBeginning(getRuntimeStmt);

        InvokeStmt gcStmtBefore = gcStmt();
        getUnits().insertAfter(gcStmtBefore, getRuntimeStmt);

        Local memoryBefore = Jimple.v().newLocal("memoryBefore", LongType.v());
        getJimpleBody().getLocals().add(memoryBefore);

        Local totalMemoryL = Jimple.v().newLocal("totalMemory", LongType.v());
        getJimpleBody().getLocals().add(totalMemoryL);
        Local freeMemoryL = Jimple.v().newLocal("freeMemory", LongType.v());
        getJimpleBody().getLocals().add(freeMemoryL);

        AssignStmt totalMemoryBefore = Jimple.v().newAssignStmt(totalMemoryL, Jimple.v().newVirtualInvokeExpr(runtime, Scene.v().getMethod("<java.lang.Runtime: long totalMemory()>").makeRef()));
        getUnits().insertAfter(totalMemoryBefore, gcStmtBefore);

        AssignStmt freeMemoryBefore = Jimple.v().newAssignStmt(freeMemoryL, Jimple.v().newVirtualInvokeExpr(runtime, Scene.v().getMethod("<java.lang.Runtime: long freeMemory()>").makeRef()));
        getUnits().insertAfter(freeMemoryBefore, totalMemoryBefore);

        Expr memoryBeforeExpr = Jimple.v().newSubExpr(totalMemoryL, freeMemoryL);
        AssignStmt memoryBeforeStmt = Jimple.v().newAssignStmt(memoryBefore, memoryBeforeExpr);
        getUnits().insertAfter(memoryBeforeStmt, freeMemoryBefore);

        InvokeStmt gcStmtAfter = gcStmt();
        InsertStmtEnd(gcStmtAfter);

        Local memoryAfter = Jimple.v().newLocal("memoryAfter", LongType.v());
        getJimpleBody().getLocals().add(memoryAfter);

        AssignStmt totalMemoryAfter = Jimple.v().newAssignStmt(totalMemoryL, Jimple.v().newVirtualInvokeExpr(runtime, Scene.v().getMethod("<java.lang.Runtime: long totalMemory()>").makeRef()));
        getUnits().insertAfter(totalMemoryAfter, gcStmtAfter);

        AssignStmt freeMemoryAfter = Jimple.v().newAssignStmt(freeMemoryL, Jimple.v().newVirtualInvokeExpr(runtime, Scene.v().getMethod("<java.lang.Runtime: long freeMemory()>").makeRef()));
        getUnits().insertAfter(freeMemoryAfter, totalMemoryAfter);

        Expr memoryAfterExpr = Jimple.v().newSubExpr(totalMemoryL, freeMemoryL);
        AssignStmt memoryAfterStmt = Jimple.v().newAssignStmt(memoryAfter, memoryAfterExpr);
        getUnits().insertAfter(memoryAfterStmt, freeMemoryAfter);
        getUnits().insertAfter(
            Jimple.v().newInvokeStmt(
                Jimple.v().newStaticInvokeExpr(getAssert(LongType.v()).makeRef(), memoryBefore, memoryAfter)
            ), memoryAfterStmt);

        this.addNonDeterminism(this.getCurrentMethod());

    }

    @Override
    public <T extends Unit> List<T> locateUnits() {
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
