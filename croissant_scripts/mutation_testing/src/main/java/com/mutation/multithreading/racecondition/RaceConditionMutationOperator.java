package com.mutation.multithreading.racecondition;

import com.mutation.StringInjector;

public class RaceConditionMutationOperator extends StringInjector {
    String assertMethodCall;
    @Override
    public boolean isApplicable() {
        /*Boolean hasTestAnnotationImport = false;
        Boolean hasAssertEqualsImport = false;
        for (String unit : this.getCurrentStateOfMutant()) {
            if (unit.contains("import org.junit.*")) {
                hasTestAnnotationImport = true;
                hasAssertEqualsImport = true;
                this.assertMethodCall = "Assert.assertEquals";
            }
            if (unit.contains("import org.junit.Test;")) {
                hasTestAnnotationImport = true;
            }
            if (unit.contains("import static org.junit.Assert.*;") || units.contains("import static org.junit.Assert.assertEquals;")) {
                hasAssertEqualsImport = true;
                this.assertMethodCall = "assertEquals";
            }
        }
        return hasAssertEqualsImport && hasTestAnnotationImport;*/


        this.assertMethodCall = null;

        if (this.jupiter) {
            this.assertMethodCall = "Assertions.";
        } else {
            this.assertMethodCall = "Assert.";
        }

        this.assertMethodCall += "assertEquals";
        return true;
    }

    @Override
    public void mutateMethod() throws Exception {


        // inject import java.util.ArrayList
        String importStatement = "import java.util.ArrayList;\nimport java.io.IOException;";
        Integer index = locate("import");
        inject(index, importStatement);


        // inject new class
        String newClass = "class nonSafeThread implements Runnable {\n" +
            "    ArrayList<Integer> list;\n" +
            "    nonSafeThread(ArrayList<Integer> list) {\n" +
            "        this.list = list;\n" +
            "    }\n" +
            "    public void run()\n" +
            "    {\n" +
            "        Integer x = this.list.get(0);\n" +
            "        x += 1;\n" +
            "        this.list.set(0,x);\n" +
            "    }\n" +
            "}\n";
        index = this.findMainClassEnd();
        inject(index, newClass);


        String newTest = "@Test\n" +
            "public void RaceConditionMutationOperator__test() throws IOException {\n" +
            "    double random = Math.random();\n" +
            "    double threshold = this.getThreshold();\n" +
            "    if (random < threshold ){\n" +
            "   ArrayList<Integer> list = new ArrayList<>();\n" +
            "   list.add(0);\n" +
            "   for (int i =0; i < 10000; i++) {\n" +
            "      Thread thread = new Thread(new nonSafeThread(list));\n" +
            "      thread.start();\n" +
            "    }\n" +
            "    Integer expected = 10000;\n" +
            "    " + this.assertMethodCall + "(expected, list.get(0));\n" +
            "}\n" +
            this.assertMethodCall + "(1,1);\n" +
            "}\n" +
            "\n";
        index = this.findMainClassEnd();
        inject(index, newTest);

    }


    @Override
    public int getMutantNumber() {
        return 1;
    }
}
