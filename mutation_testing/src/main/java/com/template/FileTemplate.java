package com.template;

import java.util.List;

public class FileTemplate extends TemplateAdder{
    String test ="    @Test\n" +
        "    public void fileTest() throws IOException {\n" +
        "        File file = new File(\"test.txt\");\n" +
        "    }";

    String setUpFileJUnit5 = "@BeforeAll\n" +
        "   public static void setUpFile() {\n" +
        "       File var3;\n" +
        "       var3 = new File(\"test.txt\");\n" +
        "       var3.delete();\n" +
        "}";



    String stateStter = "@Test\n" +
        "public void RA_stateSetterMutant() throws java.io.IOException {\n" +
        "   if (fileName!=null){ \n" +
        "       (new File(fileName)).createNewFile();\n" +
        "   }\n" +
        "   else {\n" +
        "         Timestamp timestamp = new Timestamp(System.currentTimeMillis());\n" +
        "         fileName = timestamp.toString();\n" +
        "         (new File(fileName)).createNewFile();\n" +
        "   }\n" +
        "}\n";


    protected FileTemplate(List<String> units, boolean jupiter, String projectDir) {
        super(units, jupiter, projectDir);
    }

    @Override
    public void run() {
        this.addImport("import java.io.File;");
        this.addImport("import java.io.FileWriter;");
        this.addImport("import java.io.IOException;");
        this.addImport("import java.sql.Timestamp;");
        this.addImport("import java.util.Properties;");
        this.addImport("import java.io.FileInputStream;");

        this.addField("static String fileName;");

        if (jupiter) {
            this.addImport("import org.junit.jupiter.api.Assertions;");
            //this.addImport("import org.junit.jupiter.api.Test;");
            this.addImport("import static org.junit.jupiter.api.Assertions.*;");
            String brittle = " @Test\n" +
                "public void CroissantMutant_OD_RA_brittle_newFileNullODMO() {\n" +
                "   assertNotNull(fileName);\n" +
                "   assertTrue((new File(fileName)).exists());\n" +
                "}\n";
            this.addTest(brittle);
        } else {
            this.addImport("import org.junit.Assert;\n");
            String brittle = " @Test\n" +
                "public void CroissantMutant_OD_RA_brittle_newFileNullODMO() {\n" +
                "   Assert.assertNotNull(fileName);\n" +
                "   Assert.assertTrue((new File(fileName)).exists());\n" +
                "}\n";
            this.addTest(brittle);
        }


        this.addTest(stateStter);
        this.addTest(  "static double cleanerCounterRA;" +
            "        public double getCleanerCountRA() {\n" +
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
            "   return -1; }   "
        );
        for (int i = 0;i < 50; i++){
            this.addTest("    @Test\n" +
                "    public void RA" + i + "_stateUnsetterMutant() throws Exception {\n" +
                "    int index = " + i +"; \n" +
                "    if (index >= getCleanerCountRA()) {return;}\n" +
                "    System.out.println(\"stateUnsetter count: \" + getCleanerCountRA());\n" +
                "    System.out.println(\"valid stateUnsetter: \" +" + i +");\n" +
                "    if (fileName != null) {\n" +
                "           (new File(fileName)).delete();\n" +
                "    }\n" +
                "  }");
        }
    }

}
