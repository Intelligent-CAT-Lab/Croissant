package com.template;

import java.util.List;

public class DeadLockTemplate extends TemplateAdder {
    protected DeadLockTemplate(List<String> units, boolean jupiter, String projectDir) {
        super(units, jupiter, projectDir);
    }

    @Override
    public void run() {
        if (jupiter) {
            this.addImport("import org.junit.jupiter.api.Assertions;\n");
            this.addImport("import org.junit.jupiter.api.Timeout;\n");
        } else {
            this.addImport("import org.junit.Assert;\n");
	        this.addImport("import org.junit.Rule;\n");
	        this.addImport("import org.junit.rules.Timeout;\n");
        }

        this.addImport("import java.util.concurrent.TimeUnit;\n");
    }
}
