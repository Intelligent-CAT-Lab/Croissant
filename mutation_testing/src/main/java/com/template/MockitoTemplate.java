package com.template;

import java.util.List;

public class MockitoTemplate extends TemplateAdder{

    protected MockitoTemplate(List<String> units, boolean jupiter, String projectDir) {
        super(units, jupiter, projectDir);
    }

    @Override
    public void run() {
        String[] imports = new String[] {
            "import static org.mockito.Mockito.times;\n",
            "import static org.mockito.Mockito.verify;\n",
            "import org.mockito.Mock;\n",
            "import static org.mockito.Mockito.*;\n",
            "import org.mockito.Mockito;\n",
            "import org.mockito.junit.MockitoJUnit;\n",
            "import org.mockito.junit.MockitoRule;\n"
        };

        for (String importStmt : imports) {
            this.addImport(importStmt);
        }
        if (jupiter) {
            this.addImport("import org.junit.jupiter.api.Disabled;");
        } else {
            this.addImport("import org.junit.Ignore;");
        }

        this.addField("@Mock\n" +
            "    static List mockk = Mockito.mock(ArrayList.class);");

        this.addTest(
            "    @Test\n" +
                "    public void testMockito() {\n" +
                "        mockk.add(\"one\");\n" +
                "        mockk.add(\"two\");\n" +
                "        /*mock.add(\"three\");*/\n" +
                "\n" +
                "        Mockito.verify(mockk, times(2))\n" +
                "                .add(Mockito.anyString());\n"+
                "}"
        );

        this.addImport("import java.util.List;");
        this.addImport("import java.util.ArrayList;");

        this.addDependency("org.hamcrest", "hamcrest-all", "1.3", "hamcrest");
        this.addDependency("org.mockito", "mockito-all", "1.10.5", "mockito");
        this.addDependencyScope("org.mockito","mockito-inline","3.11.2", "test","mockito-inline");
        this.addDependencyScope("org.mockito","mockito-core","3.2.4", "test","mockito-core");
        this.addDependency("junit", "junit", "4.11", "junit");

    }
}
