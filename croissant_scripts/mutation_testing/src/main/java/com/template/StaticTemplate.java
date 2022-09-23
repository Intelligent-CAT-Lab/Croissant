package com.template;

import java.util.List;

public class StaticTemplate extends TemplateAdder {
    String staticClass = "class FieldClass {\n" +
        "    public static int i = 0;\n" +
        "}";
    String staticField = "FieldClass fieldClass = new FieldClass();";
    protected StaticTemplate(List<String> units, boolean jupiter, String projectDir) {
        super(units, jupiter, projectDir);
    }

    @Override
    public void run() {
        this.addClass(staticClass);
        this.addField(staticField);

        if (jupiter) {
            this.addImport("import org.junit.jupiter.api.Assertions;\n");
        } else {
            this.addImport("import org.junit.Assert;\n");
        }
    }
}
