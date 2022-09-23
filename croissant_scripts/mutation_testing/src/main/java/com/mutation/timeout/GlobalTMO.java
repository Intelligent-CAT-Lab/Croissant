package com.mutation.timeout;

import com.mutation.SootTestInjector;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.Stmt;
import soot.util.Chain;

import java.util.Iterator;
import java.util.List;

public class GlobalTMO extends SootTestInjector {

    SootField globalTimeoutField;
    private Stmt pivot;

    @Override
    public void mutateMethod() throws Exception {
        SpecialInvokeExpr timeOutInit = findTimeoutInitInConstructor();
        mutateArgTimeOutInit(timeOutInit);
        this.addNonDeterminism(this.getCurrentMethod(), this.pivot);
    }

    @Override
    public int getMutantNumber() {
        return 1;
    }


    public void mutateArgTimeOutInit(SpecialInvokeExpr timeOutInit) {
        IntConstant intConstant = (IntConstant) timeOutInit.getArg(0);
        int timeValue = intConstant.value;
        timeOutInit.setArg(0, IntConstant.v(timeValue / 10));
    }

    @Override
    public <T extends Unit> List<T> locateUnits() {
        return null;
    }


    private SpecialInvokeExpr findTimeoutInitInConstructor() {
        SpecialInvokeExpr timeOutInit = null;
        for (SootMethod method : this.getCurrentClass().getMethods()) {
            if (method.getName().contains("init")) {
                timeOutInit = findTimeoutInitInConstructor(method);
            }
        }
        return timeOutInit;
    }

    private SpecialInvokeExpr findTimeoutInitInConstructor(SootMethod constructor) {
        Chain<Unit> units = constructor.getActiveBody().getUnits();
        Iterator<Unit> unitIterator = units.snapshotIterator();

        while (unitIterator.hasNext()) {
            Stmt stmt = (Stmt) unitIterator.next();
            if (!stmt.containsInvokeExpr()) {
                continue;
            }

            InvokeExpr invokeExpr = stmt.getInvokeExpr();

            if (!(invokeExpr instanceof SpecialInvokeExpr)) {
                continue;
            }

            SpecialInvokeExpr specialInvokeExpr = (SpecialInvokeExpr) invokeExpr;

            SootMethod initMethod = specialInvokeExpr.getMethod();
            if (!initMethod.getSignature().contains("org.junit.rules.Timeout: void <init>")) {
                continue;
            }
            this.pivot = stmt;
            this.setCurrentMethod(constructor);
            return specialInvokeExpr;
        }
        return null;
    }

    @Override
    public boolean isApplicable() {
        for (SootField sootField : this.getCurrentClass().getFields()) {
            if (sootField.getName().equals("globalTimeout")) {
                this.globalTimeoutField = sootField;
                return true;
            }
        }
        return false;
    }

    @Override
    protected String getTestName() {
        return null;
    }
}
