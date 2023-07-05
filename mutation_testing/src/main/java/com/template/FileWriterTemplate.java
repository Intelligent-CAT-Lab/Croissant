package com.template;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

public class FileWriterTemplate extends TemplateAdder{

    String template ="    @Test\n" +
        "    public void fileWriterTest() throws IOException {\n" +
        "        File file = new File(\"testWriter.txt\");\n" +
        "        BufferedWriter br = new BufferedWriter(new FileWriter(file));\n"+
        "        br.write(\"hi\");\n"+
        "        br.close();\n"+
        "    }";

    String victim = "   @Test\n" +
        "   public void CroissantMutant_OD_FPD_victim_FileObjectFMO() throws java.io.IOException{\n" +
        "   File file = new File(writerFileName);\n" +
        "   FileWriter fw =  new FileWriter(file);\n" +
        "   fw.write(\"victim\\n\");\n" +
        "   fw.close();\n" +
        "}\n";

    String polluter = "@Test\n" +
        "   public void FPD_polluterMutant() throws java.io.IOException { \n" +
        "       File file = new File(writerFileName);\n" +
        "       file.setWritable(true);\n" +
        "       FileWriter fw =  new FileWriter(file);\n" +
        "       fw.write(\"pollute\\n\");" +
        "       fw.close();\n" +
        "       file.setWritable(false);\n" +
        "}\n";

    String setUpWriterJunit5 = "@BeforeAll\n" +
        "public static void setUpWriter()throws java.io.IOException {\n" +
        "   File var2 = new File(\"testWriter.txt\");\n" +
        "   var2.setWritable(true);\n" +
        "  }\n";
    String setUpWriterJunit4 = "@BeforeClass\n" +
        "public static void setUpWriter()throws java.io.IOException {\n" +
        "   File var2 = new File(\"testWriter.txt\");\n" +
        "   var2.setWritable(true);\n" +
        "  }\n";



    protected FileWriterTemplate(List<String> units, boolean jupiter, String projectDir) {
        super(units, jupiter, projectDir);
    }

    @Override
    public void run() {
        this.addImport("import java.io.File;");
        this.addImport("import java.io.FileWriter;");
        this.addImport("import java.io.IOException;");
        this.addImport("import java.io.BufferedWriter;");
        this.addImport("import java.sql.Timestamp;");
        this.addImport("import java.util.Properties;");
        this.addImport("import java.io.FileInputStream;");

        this.addField("static Timestamp writerTimestamp = new Timestamp(System.currentTimeMillis());");
        this.addField("static String writerFileName = writerTimestamp.toString();");

        if (jupiter) {
            this.addImport("import org.junit.jupiter.api.Assertions;");
	        //this.addImport("import org.junit.jupiter.api.Test;");
            //this.addImport("import org.junit.jupiter.api.BeforeAll;");
        } else {
            this.addImport("import org.junit.Assert;");
            //this.addImport("import org.junit.BeforeClass;");
            //this.addTest(setUpWriterJunit4);
        }

        //this.addTest(test);
        this.addTest(victim);
        this.addTest(polluter);

        //this.addTest(setUpWriter);
        for (int i = 0;i < 50; i++){
            this.addTest("    @Test\n" +
                "    public void FPD" + i + "_cleanerMutant() throws Exception {\n" +
                "    int index = " + i +"; \n" +
                "    if (index >= getCleanerCountFPD()) {return;}\n" +
                "    System.out.println(\"cleaner count: \" + getCleanerCountFPD());\n" +
                "    System.out.println(\"valid cleaner: \" +" + i +");\n" +
                "    File var3;\n" +
                "    var3 = new File(writerFileName);\n" +
                "    var3.setWritable(true);\n" +
                "    }");
        }
        int index = 1;
        this.addTest(  "static double cleanerCounterFPD;" +
            "        public double getCleanerCountFPD() {\n" +
            "        try {\n"+
            "        String path = System.getProperty(\"user.dir\");\n" +
            "        String var5 = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath() + \"mutation.config\";\n" +
            "        System.out.println(var5);\n" +
            "        Properties var3 = new Properties();\n" +
            "        FileInputStream var4 = new FileInputStream(var5);\n" +
            "        var3.load(var4);\n" +
            "        var5 = var3.getProperty(\"mutation.count\");\n" +
            "        Double var6 = Double.valueOf(var5);\n" +
            "        Double var1 = var6;\n" +
            "        return var1;" +
            "        } catch(Exception e) {\n" +
            "        System.out.println(\"configuration file cannot be accessed\");\n" +
            "        }\n"+
            "   return -1; }   "

            );
    }
}
