package com.template;

import java.util.List;

public class TimezoneTemplate extends TemplateAdder {
    protected TimezoneTemplate(List<String> units, boolean jupiter, String projectDir) {
        super(units, jupiter, projectDir);
    }

    @Override
    public void run() {
        String test = "";

        if (jupiter) {
            test = "@Test\n" +
                "    public void timeZoneDependencyTest() throws ParseException {\n" +
                "        final SimpleDateFormat dateFormat = new SimpleDateFormat(\"yyyy-MM-dd HH:mm:ss\");\n" +
                "        // Otherwise, it'll take the default timezone.\n" +
                "        dateFormat.setTimeZone(TimeZone.getTimeZone(\"UTC\"));\n" +
                "\n" +
                "        Date date1 = null, date2 = null;\n" +
                "        date2 = dateFormat.parse(\"9111-04-05 02:02:02\");\n" +
                "\n" +
                "        date1 = dateFormat.parse(\"2020-11-22 10:13:55\");\n" +
                "        date2 = dateFormat.parse(\"2020-11-22 10:13:55\");\n" +
                "\n" +
                "\n" +
                "        Assertions.assertNotNull(date1);\n" +
                "        Assertions.assertNotNull(date2);\n" +
                "        Assertions.assertEquals(date1, date2);\n" +
                "    }";
        } else {
            test = "@Test\n" +
                "    public void timeZoneDependencyTest() throws Exception {\n" +
                "        final SimpleDateFormat dateFormat = new SimpleDateFormat(\"yyyy-MM-dd HH:mm:ss\");\n" +
                "        // Otherwise, it'll take the default timezone.\n" +
                "        dateFormat.setTimeZone(TimeZone.getTimeZone(\"UTC\"));\n" +
                "\n" +
                "        Date date1 = null, date2 = null;\n" +
                "        date2 = dateFormat.parse(\"9111-04-05 02:02:02\");\n" +
                "\n" +
                "        date1 = dateFormat.parse(\"2020-11-22 10:13:55\");\n" +
                "        date2 = dateFormat.parse(\"2020-11-22 10:13:55\");\n" +
                "\n" +
                "\n" +
                "        Assert.assertNotNull(date1);\n" +
                "        Assert.assertNotNull(date2);\n" +
                "        Assert.assertEquals(date1, date2);\n" +
                "    }";
        }
        this.addImport("import java.text.SimpleDateFormat;");
        this.addImport("import java.util.TimeZone;");
        this.addImport("import java.util.Date;");
        this.addImport("import java.text.ParseException;");

        if (jupiter) {
            this.addImport("import org.junit.jupiter.api.Assertions;");
        } else {
            this.addImport("import org.junit.Assert;");
        }

        this.addTest(test);
    }
}
