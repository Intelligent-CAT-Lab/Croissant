package com.mutation.filepermission;

import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.util.Chain;

import java.util.Iterator;

public class PathFSFMO extends FileStringFMO {
    public PathFSFMO() {
        super("java.nio.file.Path get(java.lang.String,java.lang.String[])");
    }

    @Override
    public boolean isApplicable() {

        this.fileName = null;

        Chain<Unit> units = this.getJimpleBody().getUnits();
        Iterator<Unit> iterator = units.snapshotIterator();

        this.fileName = null;
        this.victimTest = null;

        while (iterator.hasNext()) {
            Stmt stmt = (Stmt) iterator.next();

            if (!(stmt instanceof AssignStmt)) {
                continue;
            }

            AssignStmt assignStmt = (AssignStmt) stmt;
            Value rightOp = assignStmt.getRightOp();

            if (!(rightOp instanceof StaticInvokeExpr)) {
                continue;
            }

            StaticInvokeExpr staticInvokeExpr = (StaticInvokeExpr) rightOp;

            if (staticInvokeExpr.getMethod().getSignature().contains(this.methodSignature)) {
                this.fileName = ((StringConstant) staticInvokeExpr.getArg(0)).value;
                this.victimTest = this.getCurrentMethod();
            }

        }
        return (this.fileName != null);
    }
}
