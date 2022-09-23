package com.template;

import java.util.List;

public class ThreadSleepTemplate extends TemplateAdder{
    protected ThreadSleepTemplate(List<String> units, boolean jupiter, String projectDir) {
        super(units, jupiter, projectDir);
    }

    @Override
    public void run() {
        String test = "";
        if (jupiter) {
            test = "@Test\n" +
                "    public void threadSleepTest() throws Exception {\n" +
                "        Value value = new Value(1);\n" +
                "\n" +
                "\n" +
                "        ThreadObject testThread = new ThreadObject(value);\n" +
                "        testThread.start();\n" +
                "\n" +
                "        Thread.sleep(10);\n" +
                "\n" +
                "/*        System.out.println(\"Final value: \" + value[0]);*/\n" +
                "        int finalValue = value.returnValue();\n" +
                "        Assertions.assertEquals(finalValue, 2);\n" +
                "    }";
            this.addImport("import org.junit.jupiter.api.Assertions;");
        } else {
            test = "@Test\n" +
                "    public void threadSleepTest() throws Exception {\n" +
                "        Value value = new Value(1);\n" +
                "\n" +
                "\n" +
                "        ThreadObject testThread = new ThreadObject(value);\n" +
                "        testThread.start();\n" +
                "\n" +
                "        Thread.sleep(10);\n" +
                "\n" +
                "/*        System.out.println(\"Final value: \" + value[0]);*/\n" +
                "        int finalValue = value.returnValue();\n" +
                "        Assert.assertEquals(finalValue, 2);\n" +
                "    }";
            this.addImport("import org.junit.Assert;");
        }

        this.addTest(test);


        this.addClass("class Value {\n" +
            "    private Integer i;\n" +
            "\n" +
            "    public Value(Integer i) {\n" +
            "        this.i = i;\n" +
            "    }\n" +
            "\n" +
            "    public Integer returnValue() {\n" +
            "        return i;\n" +
            "    }\n" +
            "\n" +
            "    public void increment() {\n" +
            "        i++;\n" +
            "    }\n" +
            "}");

        this.addClass("class ThreadObject extends Thread {\n" +
            "    Value value;\n" +
            "\n" +
            "    public ThreadObject(Value value) {\n" +
            "        super();\n" +
            "        this.value = value;\n" +
            "    }\n" +
            "    @Override\n" +
            "    public void run() {\n" +
            "        try {\n" +
            "            Thread.sleep(1);\n" +
            "            this.value.increment();\n" +
            "        } catch (InterruptedException e) {\n" +
            "            e.printStackTrace();\n" +
            "        }\n" +
            "    }\n" +
            "}");
    }
}
