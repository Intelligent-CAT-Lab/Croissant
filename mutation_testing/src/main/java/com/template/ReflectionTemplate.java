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
        String test2 = "";

        if (jupiter) {
            test = "@Test\n" +
                "    public void UPMO_test() {\n" +
                "     HashMap<String, String> map = new HashMap<String, String>();   \n" +
                "     HashSet<String> set = new HashSet<String>(); \n" +
                "     List<String> methodNames = new ArrayList<>();\n" +
                "     Method[] methods = map.getClass().getMethods();\n"+
                "      for (Method method : methods)\n" +
                "        methodNames.add(method.getName());\n" +
                "      for (Method method : methods)\n" +
                "        set.add(method.getName());\n" +
                "     String s = set.toString();\n" +
                "    }";

        } else {
            test = "@Test\n" +
                "    public void UPMO_test() {\n" +
                "     HashMap<String, String> map = new HashMap<String, String>();   \n" +
                "     HashSet<String> set = new HashSet<String>(); \n" +
                "     List<String> methodNames = new ArrayList<>();\n" +
                "     Method[] methods = map.getClass().getMethods();\n"+
                "      for (Method method : methods)\n" +
                "        methodNames.add(method.getName());\n" +
                "      for (Method method : methods)\n" +
                "        set.add(method.getName());\n" +
                "     String s = set.toString();\n" +
                "    }";
        }

        this.addImport("import java.util.HashMap;");
        this.addImport("import java.util.HashSet;");
        this.addImport("import java.util.ArrayList;");
        this.addImport("import java.util.Arrays;");
        this.addImport("import java.util.List;");
        this.addImport("import java.util.AbstractCollection;");
        this.addImport("import java.util.Iterator;");

        if (jupiter) {
            this.addImport("import org.junit.jupiter.api.Assertions;");
        } else {
            this.addImport("import org.junit.Assert;");
        }


        this.addTest(test);
    }
}
