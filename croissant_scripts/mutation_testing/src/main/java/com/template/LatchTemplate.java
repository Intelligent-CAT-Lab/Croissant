package com.template;

import java.util.List;

public class LatchTemplate extends TemplateAdder{
    protected LatchTemplate(List<String> units, boolean jupiter, String projectDir) {
        super(units, jupiter, projectDir);
    }

    @Override
    public void run() {
        String countDownRunnable = "class CountDownRunnable extends  Thread {\n" +
            "    private CountDownLatch countDownLatch;\n" +
            "    public CountDownRunnable(CountDownLatch countDownLatch) {\n" +
            "        this.countDownLatch = countDownLatch;\n" +
            "    }\n" +
            "    @Override\n" +
            "    public void run() {\n" +
            "        try {\n" +
            "            Thread.sleep(1);\n" +
            "        } catch (InterruptedException ex) {\n" +
            "            ex.printStackTrace();\n" +
            "        }\n" +
            "        countDownLatch.countDown();\n" +
            "    }\n" +
            "}";


        String test = "";

        if (jupiter) {
            test = "@Test\n" +
                "    public void AWMO_test() throws InterruptedException {\n" +
                "        final CountDownLatch latch = new CountDownLatch(1);\n" +
                "        Thread t = new Thread(new CountDownRunnable(latch));\n" +
                "        t.start();\n" +
                "        Assertions.assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));\n" +
                "    }";
        } else {
            test = "    @Test\n" +
                "    public void AWMO_test() throws InterruptedException {\n" +
                "        final CountDownLatch latch = new CountDownLatch(1);\n" +
                "        Thread t = new Thread(new CountDownRunnable(latch));\n" +
                "        t.start();\n" +
                "        Assert.assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));\n" +
                "    }";
        }

        this.addTest(test);

        this.addImport("import java.util.concurrent.TimeUnit;");

        if (jupiter) {
            this.addImport("import org.junit.jupiter.api.Assertions;");
        } else {
            this.addImport("import org.junit.Assert;");
        }

        this.addImport("import java.util.concurrent.CountDownLatch;\n");
        this.addClass(countDownRunnable);

    }
}
