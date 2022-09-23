package com.template;

import java.util.List;

public class CacheTemplate extends TemplateAdder {
    String test;
    protected CacheTemplate(List<String> units, boolean jupiter, String projectDir) {
        super(units, jupiter, projectDir);
    }

    @Override
    public void run() {
        addDependency("com.github.ben-manes.caffeine","caffeine","2.5.5", "caffeine");
        addImport("import com.github.benmanes.caffeine.cache.Cache;\n");
        addImport("import com.github.benmanes.caffeine.cache.Caffeine;\n");
        addImport("import java.util.concurrent.TimeUnit;");

        if (jupiter) {
            addImport("import org.junit.jupiter.api.Test;");
        } else {
            addImport("import org.junit.Test;");
        }

        this.test = "static Cache<String, String> cache = Caffeine.newBuilder()\n" +
            "            .expireAfterWrite(1, TimeUnit.MINUTES)\n" +
            "            .maximumSize(100)\n" +
            "            .build();\n" +
            "    @Test\n" +
            "    public void cacheTest() {\n" +
            "        cache.put(\"a\", \"b\");\n" +
            "    }";

        this.addTest(test);
    }


}
