package com.mutation;

import soot.SootClass;
import soot.SootMethod;

import java.io.IOException;

public interface MutationOperator {
    void setCurrentMethod(String newMethod);
    void setCurrentMethod(SootMethod newMethod);
    boolean isApplicable();
    void runOnceBefore();
    void runOnceAfter();
    void mutateMethod() throws Exception;
    void setCurrentClass(String inputDir, String className);
    void setCurrentClass(SootClass currentClass);
    void exportClass(String outputDir, String fileName) throws IOException;
    public void setJupiter(boolean jupiter);
    String getCurrentMethodName();

    void setThreshold(double threshold);

    int getMutantNumber();
}
