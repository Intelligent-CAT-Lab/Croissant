package com.mutation.multithreading.deadlock;

import com.mutation.StringInjector;


/*
    class -> Soot-based -> mutatedClass
    java -> String-based -> class
 */

public class DeadLockMutationOperator extends StringInjector {

    String assertMethodCall;
    String testAnnotation;

    @Override
    public boolean isApplicable() {
        /*Boolean hasTestAnnotationImport = false;
        Boolean hasAssertEqualsImport = false;
        for (String unit : this.getCurrentStateOfMutant()) {
            if (unit.contains("import org.junit.*")) {
                hasTestAnnotationImport = true;
                hasAssertEqualsImport = true;
                this.assertMethodCall = "Assert.assertTrue";
            }
            if (unit.contains("import org.junit.Test;")) {
                hasTestAnnotationImport = true;
            }
            if (unit.contains("import static org.junit.Assert.*;") || units.contains("import static org.junit.Assert.assertTrue;")) {
                hasAssertEqualsImport = true;
                this.assertMethodCall = "assertTrue";
            }
        }
        return hasAssertEqualsImport && hasTestAnnotationImport;*/

        this.assertMethodCall = null;

        if (this.jupiter) {
            this.assertMethodCall = "Assertions.";
        } else {
            this.assertMethodCall = "Assert.";
        }

        this.assertMethodCall += "assertTrue";


        if (this.jupiter) {
            this.testAnnotation = "@Test\n" +
                "@Timeout(value = 100, unit = TimeUnit.MILLISECONDS)\n";
        } else {
            this.testAnnotation = "@Test(timeout=5L)\n";
        }

        return true;
    }

    @Override
    public void mutateMethod() throws Exception {


        String newTest = this.testAnnotation +
            "  public void deadLockTest() throws IOException {\n" +
            "    double random = Math.random();\n" +
            "    double threshold = this.getThreshold();\n" +
            "    if (random <  threshold){\n" +
            "\n" +
            "    Object lock1 = new Object();\n" +
            "    Object lock2 = new Object();\n" +
            "\n" +
            "Thread thread1 = new Thread() {\n" +
            "      public void run() {\n" +
            "        synchronized (lock1) {\n" +
            "          try {\n" +
            "            Thread.sleep(100);\n" +
            "          }\n" +
            "          catch (Exception e) {\n" +
            "\n" +
            "          }\n" +
            "\n" +
            "          synchronized (lock2) {\n" +
            "\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "    };\n" +
            "\n" +
            "Thread thread2 = new Thread() {\n" +
            "      public void run() {\n" +
            "        synchronized (lock2) {\n" +
            "          try {\n" +
            "            Thread.sleep(100);\n" +
            "          }\n" +
            "          catch (Exception e) {\n" +
            "\n" +
            "          }\n" +
            "\n" +
            "          synchronized (lock1) {\n" +
            "\n" +
            "          }\n" +
            "\n" +
            "        }\n" +
            "      }\n" +
            "    };\n" +
            "\n" +
            "    thread1.run();\n" +
            "    thread2.run();\n" +
            "    " + this.assertMethodCall + "(true);\n" +
            "}\n" +
            this.assertMethodCall + "(true);\n" +
            "  }";

        Integer index = this.findMainClassEnd();
        inject(index, newTest);

    }


    @Override
    public int getMutantNumber() {
        return 1;
    }
}
