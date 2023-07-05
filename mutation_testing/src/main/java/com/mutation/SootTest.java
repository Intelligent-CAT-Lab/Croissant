package com.mutation;

import soot.Unit;

import java.util.List;

public class SootTest extends SootTestInjector{

    private static boolean variable;

    @Override
    public void mutateMethod() throws Exception {
        variable = true;
    }

    @Override
    public int getMutantNumber() {
        return 0;
    }

    @Override
    public <T extends Unit> List<T> locateUnits() {
        return null;
    }

    @Override
    public boolean isApplicable() {
        return !variable;
    }

    @Override
    protected String getTestName() {
        return "test";
    }
}
