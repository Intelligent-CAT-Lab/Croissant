package com.mutation.resourceNotCreated;

import com.mutation.SootMutationOperator;
import soot.*;
import soot.jimple.*;
import soot.util.Chain;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class newFileNullMO extends SootMutationOperator {
    SootClass phantomAssertClass;
    SootMethod phantomAssertTrue;
    Integer mutantNumber;

    @Override
    public void runOnceBefore() {
        phantomAssertTrue = this.getAssertTrue();
    }

    @Override
    public void mutateMethod() throws Exception {
        Iterator<InvokeStmt> It = locateUnits().listIterator();
        while (It.hasNext()) {
            InvokeStmt currentStmt = It.next();
            Local newFileL = (Local)((SpecialInvokeExpr)((Stmt)currentStmt).getInvokeExpr()).getBase();
            currentStmt.getInvokeExpr().setArg(0, StringConstant.v(""));
            VirtualInvokeExpr getFileExpr = Jimple.v().newVirtualInvokeExpr(newFileL, Scene.v().getMethod("<java.io.File: boolean isFile()>").makeRef());
            Local getFileL = Jimple.v().newLocal("getFileL", BooleanType.v());
            getJimpleBody().getLocals().add(getFileL);
            Stmt getFileStmt = Jimple.v().newAssignStmt(getFileL, getFileExpr);
            getUnits().insertAfter(getFileStmt, currentStmt);

            StaticInvokeExpr assertTrueExpr = Jimple.v().newStaticInvokeExpr(phantomAssertTrue.makeRef(), getFileL);
            Stmt assertTrueStmt = Jimple.v().newInvokeStmt(assertTrueExpr);
            getUnits().insertAfter(assertTrueStmt, getFileStmt);
        }
        this.addNonDeterminism(this.getCurrentMethod());
    }

    @Override
    public List<InvokeStmt> locateUnits() {
        Chain<Unit> units = getUnits();
        Iterator<Unit> itr = units.snapshotIterator();
        List<InvokeStmt> suitableStmts = new ArrayList<>();
        while(itr.hasNext()) {
            Unit currentUnit = itr.next();
            Stmt currentStmt = (Stmt) currentUnit;
            if (currentStmt instanceof InvokeStmt && currentStmt.getInvokeExpr() instanceof SpecialInvokeExpr) {
                SpecialInvokeExpr currentSpecialInvoke = (SpecialInvokeExpr) currentStmt.getInvokeExpr();
                // FIXME ASSUMPTION: PACKAGE NAME
                if (currentSpecialInvoke.getMethod().toString().contains("<java.io.File: void <init>(java.lang.String)>")) {
                    suitableStmts.add((InvokeStmt) currentUnit);
                }
            }
        }
        return suitableStmts;
    }

    @Override
    public boolean isApplicable() {
        this.mutantNumber = locateUnits().size();
        return this.mutantNumber > 0;
        /*return locateUnits().size() > 0;*/
    }


    @Override
    public int getMutantNumber() {
        return this.mutantNumber;
    }
}
