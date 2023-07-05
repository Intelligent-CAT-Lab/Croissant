package com.template;

import java.util.List;

public class ServerTemplate extends TemplateAdder{
    protected ServerTemplate(List<String> units, boolean jupiter, String projectDir) {
        super(units, jupiter, projectDir);
    }

    @Override
    public void run() {
        this.addDependency("org.eclipse.jetty", "jetty-server", "9.4.44.v20210927", "jetty");
        this.addField("private Server server;\n");

        this.addTest("    @Test\n" +
            "    public void serverTest() throws Exception {\n" +
            "        server = new Server(555);\n" +
            "        server.start();\n" +
            "    }");

        this.addImport("import org.eclipse.jetty.server.Server;");
    }
}
