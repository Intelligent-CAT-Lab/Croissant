package com.template;

import java.util.List;

public class TimeoutTemplate extends TemplateAdder {
    protected TimeoutTemplate(List<String> units, boolean jupiter, String projectDir) {
        super(units, jupiter, projectDir);
    }

    @Override
    public void run() {
        String test = "";

        if (jupiter) {
            test = "    @Test\n" +
                "    @Timeout(value = 2, unit = TimeUnit.MILLISECONDS)\n" +
                "    public void TMO_test() throws Exception {\n" +
                "        Thread.sleep(1);\n" +
                "    }";
            this.addImport("import org.junit.jupiter.api.*;\n");
        } else {
            test = "    @Test(timeout = 2)\n" +
                "    public void TMO_test() throws Exception {\n" +
                "        Thread.sleep(1);\n" +
                "    }";
            this.addImport("import org.junit.*;\n");
        }

        this.addImport("import java.util.concurrent.TimeUnit;\n");

        this.addTest(test);
    }
}
