package com.template;

import java.util.List;

public class ReflectionTemplate extends TemplateAdder{
    protected ReflectionTemplate(List<String> units, boolean jupiter, String projectDir) {
        super(units, jupiter, projectDir);
    }

    @Override
    public void run() {
        this.addImport("import java.lang.reflect.Method;");

        String test = "";

        if (jupiter) {
            test = "@Test\n" +
                "    public void UPMO_test() {\n" +
                "        HashMap<String, String> map = new HashMap<String, String>();\n" +
                "        List<String> methodNames = new ArrayList<>();\n" +
                "        Method[] methods = map.getClass().getMethods();\n" +
                "        for (Method method : methods)\n" +
                "            methodNames.add(method.getName());\n" +
                "\n" +
                "\n" +
                "        Assertions.assertEquals(methodNames.get(0), \"remove\");\n" +
                "    }";
        } else {
            test = "@Test\n" +
                "    public void UPMO_test() {\n" +
                "        HashMap<String, String> map = new HashMap<String, String>();\n" +
                "        List<String> methodNames = new ArrayList<>();\n" +
                "        Method[] methods = map.getClass().getMethods();\n" +
                "        for (Method method : methods)\n" +
                "            methodNames.add(method.getName());\n" +
                "        Assert.assertEquals(methodNames.get(0), \"remove\");\n" +
                "    }";
        }

        this.addImport("import java.util.HashMap;");
        this.addImport("import java.util.ArrayList;");
        this.addImport("import java.util.List;");

        if (jupiter) {
            this.addImport("import org.junit.jupiter.api.Assertions;");
        } else {
            this.addImport("import org.junit.Assert;");
        }

        this.addTest(test);
    }
}
