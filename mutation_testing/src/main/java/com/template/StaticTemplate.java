package com.template;

import java.util.List;

public class StaticTemplate extends TemplateAdder {
    String staticClass = "static class FieldClass {\n" +
        "     static int i = 10;\n" +
        "}";
    String staticField = "FieldClass fieldClass = new FieldClass();";
    protected StaticTemplate(List<String> units, boolean jupiter, String projectDir) {
        super(units, jupiter, projectDir);
    }

    @Override
    public void run() {
        this.addField(staticClass);
        this.addField(staticField);
        this.addImport("import java.util.Properties;");
        this.addImport("import java.io.FileInputStream;");

        if (jupiter) {
            this.addImport("import org.junit.jupiter.api.Assertions;\n");
//            this.addImport("import static org.junit.jupiter.api.Assertions.assertEquals;\n");
        } else {
            this.addImport("import org.junit.Assert;\n");
        }
        this.addTest("    @Test\n" +
            "    public void CroissantMutant_OD_SVD_Victim_StaticSVMO() throws Exception {\n" +
            "        assertEquals(fieldClass.i,10);\n" +
            "    }");
        this.addTest("    @Test\n" +
            "    public void SVD_polluterMutant() throws Exception {\n" +
            "        fieldClass.i = 0;\n" +
            "    }");
        for (int i = 0;i < 50; i++){
            this.addTest("    @Test\n" +
                "    public void SVD" + i + "_cleanerMutant() throws Exception {\n" +
                "    int index = " + i +"; \n" +
                "    if (index >= getCleanerCountSVD()) {return;}\n" +
                "    System.out.println(\"cleaner count: \" + getCleanerCountSVD());\n" +
                "    System.out.println(\"valid cleaner: \" +" + i +");\n" +
                "    fieldClass.i = 10;\n" +
                "    }");
        }
        this.addTest(  "static double cleanerCounterSVD;" +
            "        public double getCleanerCountSVD() {\n" +
            "        try {\n"+
            "        String path = System.getProperty(\"user.dir\");\n" +
            "        String var5 = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath() + \"mutation.config\";\n" +
            "        System.out.println(var5);\n" +
            "        Properties var3 = new Properties();\n" +
            "        FileInputStream var4 = new FileInputStream(var5);\n" +
            "        var3.load(var4);\n" +
            "        var5 = var3.getProperty(\"mutation.count\");\n" +
            "        Double var6 = Double.valueOf(var5);\n" +
            "        double var1 = var6;\n" +
            "        return var1;" +
            "        } catch(Exception e) {\n" +
            "        System.out.println(\"configuration file cannot be accessed\");\n" +
            "        }\n"+
            "   return -1; }"
        );

    }
}
