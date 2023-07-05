package com.template;

import java.util.List;

public class DatabaseTemplate extends TemplateAdder{

    protected DatabaseTemplate(List<String> units, boolean jupiter, String projectDir) {
        super(units, jupiter, projectDir);
    }

    @Override
    public void run() {
        addDependency("org.xerial","sqlite-jdbc", "3.36.0.3", "sqlite");
        addImport("import java.sql.*;\n");

        addImport("import java.util.Properties;");
        addImport("import java.io.FileInputStream;");
        addImport("import java.io.File;");
        addImport("import java.io.FileWriter;");
        addImport("import java.io.IOException;");
        addImport("import java.sql.Timestamp;");


        if (jupiter) {
            addImport("import org.junit.jupiter.api.Assertions;");
            //addImport("import org.junit.jupiter.api.BeforeAll;");
            //addImport("import org.junit.jupiter.api.AfterAll;");
            //addImport("import org.junit.jupiter.api.Test;");
            addImport("import static org.junit.jupiter.api.Assertions.*;");
            String brittle = "@Test\n" +
                "public void CroissantMutant_OD_DSD_brittle_DatabaseMutationOperator() throws ClassNotFoundException,SQLException {\n" +
                "   if (con == null) {\n" +
                "       getConnection();}\n" +
                "   Connection var1 = con;\n" +
                "   Assertions.assertTrue(var1.prepareStatement(\"SELECT * FROM user WHERE fName='firstName*'  AND lName='lastName*' \").executeQuery().next());\n" +
                "   }\n";
            this.addTest(brittle);
        } else {
            addImport("import org.junit.Assert;\n");
            String brittle = "@Test\n" +
                "public void CroissantMutant_OD_DSD_brittle_DatabaseMutationOperator() throws ClassNotFoundException,SQLException {\n" +
                "   if (con == null) {\n" +
                "       getConnection();}\n" +
                "   Connection var1 = con;\n" +
                "   Assert.assertTrue(var1.prepareStatement(\"SELECT * FROM user WHERE fName='firstName*'  AND lName='lastName*' \").executeQuery().next());\n" +
                "   }\n";
            this.addTest(brittle);
        }

        addField("private static Connection con;\n");
        addField("private static String dbName;");


        String getConnection = "public static void getConnection() throws ClassNotFoundException, SQLException {\n" +
            "        Class.forName(\"org.sqlite.JDBC\");\n" +
            "         if (dbName == null){\n" +
            "               Timestamp timestamp = new Timestamp(System.currentTimeMillis());\n" +
            "               dbName = timestamp.toString();\n" +
            "           }\n" +
            "        con = DriverManager.getConnection(\"jdbc:sqlite:\" + dbName);\n" +
            "        initialize();\n" +
            "    }";
        this.addTest(getConnection);

        String initialize = "    public static void initialize() throws SQLException {\n" +
            "        try {\n" +
            "            Statement statement = con.createStatement();\n" +
            "\n" +
            "            Statement statement1 = con.createStatement();\n" +
            "            statement1.execute(\"CREATE TABLE user(id integer, fName text, lName text, primary key(id));\");\n" +
            "        } catch (SQLException throwables) {\n" +
            "            System.out.println(\"table already exists\");\n" +
            "        }\n" +
            "\n" +
            "    }";
        this.addTest(initialize);
//        String setUpSQL = "@BeforeAll\n" +
//            "public static void setUpSQL()throws ClassNotFoundException, SQLException{\n" +
//            "   getConnection();\n" +
//            "}\n";
//        this.addTest(setUpSQL);
//        String tearDownSQL = "@AfterAll\n" +
//            "public static void tearDownSQL()throws ClassNotFoundException, SQLException{\n" +
//            "   boolean result = new File(\"testdb.db\").delete();\n" +
//            "}\n";
//        this.addTest(tearDownSQL);


        String createTable = " public void createTable() {\n" +
            "        try {\n" +
            "            Statement statement1 = con.createStatement();\n" +
            "            statement1.execute(\"CREATE TABLE user(id integer, fName text, lName text, primary key(id));\");\n" +
            "        } catch (SQLException throwables) {\n" +
            "            System.out.println(\"table already exists\");\n" +
            "        }\n" +
            "    }";
        //this.addTest(createTable);



        String stateSetter = "@Test\n" +
            "public void DSD_stateSetterMutant() throws ClassNotFoundException,SQLException{\n" +
            "   if (con == null) {\n" +
            "      getConnection();}\n" +
            "   PreparedStatement var1 = con.prepareStatement(\"INSERT INTO user(id, fName, lName) values(?, ?, ?)\");\n" +
            "   var1.setString(3, \"lastName*\");\n" +
            "   var1.setString(2, \"firstName*\");\n" +
            "   var1.execute();\n" +
            //"   var1.close();\n" +
            "}\n";
        this.addTest(stateSetter);
        for (int i = 0;i < 50; i++){
            this.addTest("    @Test\n" +
                "    public void DSD" + i + "_stateUnsetterMutant() throws ClassNotFoundException,SQLException {\n" +
                "    int index = " + i +"; \n" +
                "    if (index >= getCleanerCountDSD()) {return;}\n" +
                "    System.out.println(\"stateUnsetter count: \" + getCleanerCountDSD());\n" +
                "    System.out.println(\"valid stateUnsetter: \" +" + i +");\n" +
                "   if (con == null) {\n" +
                "      getConnection();}\n" +
                "   Connection var3 = con;\n" +
                "   var3.prepareStatement(\"DELETE FROM user WHERE fName='firstName*'  AND lName='lastName*' \").execute();\n" +
                //"   var3.close();\n" +
                "    }");
        }



        String test = "    @Test\n" +
            "    public void testDB() throws SQLException, ClassNotFoundException {\n" +
            "        if (con == null) {\n" +
            "            getConnection();\n" +
            "        }\n" +
            "\n" +
            "        Statement statement = con.createStatement();\n" +
            "        ResultSet res = statement.executeQuery(\"SELECT fname, lname FROM user\");\n" +
            "        res.close();\n" +
            "        statement.close();\n" +
            "\n" +
            "        createTable();\n" +
            "\n" +
            "        PreparedStatement preparedStatement = con.prepareStatement(\"INSERT INTO user(id, fName, lName) values(?, ?, ?)\");\n" +
            "        preparedStatement.setString(3, \"lastName\");\n" +
            "        preparedStatement.setString(2, \"firstName\");\n" +
            "        preparedStatement.execute();\n" +
            "        preparedStatement.close();\n" +


            "    }";
        //this.addTest(test);
        this.addTest(  "static double cleanerCounterDSD;" +
            "        public double getCleanerCountDSD() {\n" +
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
		       "   return -1; }   ");

    }
}
