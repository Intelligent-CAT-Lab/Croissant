package com.template;

import java.util.List;

public class FileTemplate extends TemplateAdder{
    String test ="    @Test\n" +
        "    public void fileTest() throws IOException {\n" +
        "        File file = new File(\"test.txt\");\n" +
        "        FileWriter myWriter = new FileWriter(file);\n" +
        "        myWriter.write(\"write\");\n" +
        "    }";

    protected FileTemplate(List<String> units, boolean jupiter, String projectDir) {
        super(units, jupiter, projectDir);
    }

    @Override
    public void run() {
        this.addImport("import java.io.File;");
        this.addImport("import java.io.FileWriter;");
        this.addImport("import java.io.IOException;");
        this.addTest(test);
    }

}
