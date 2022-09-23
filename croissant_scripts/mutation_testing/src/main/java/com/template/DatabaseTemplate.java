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
        addField("private static Connection con;\n");


        String getConnection = "public static void getConnection() throws ClassNotFoundException, SQLException {\n" +
            "        Class.forName(\"org.sqlite.JDBC\");\n" +
            "        con = DriverManager.getConnection(\"jdbc:sqlite:testdb.db\");\n" +
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
            "        PreparedStatement preparedStatement = con.prepareStatement(\"INSERT INTO user values(?, ?, ?)\");\n" +
            "        preparedStatement.setString(2, \"Alperen\");\n" +
            "        preparedStatement.execute();\n" +
            "    }";
        this.addTest(initialize);

        String createTable = " public void createTable() {\n" +
            "        try {\n" +
            "            Statement statement1 = con.createStatement();\n" +
            "            statement1.execute(\"CREATE TABLE user(id integer, fName text, lName text, primary key(id));\");\n" +
            "        } catch (SQLException throwables) {\n" +
            "            System.out.println(\"table already exists\");\n" +
            "        }\n" +
            "    }";
        this.addTest(createTable);

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
            "        preparedStatement.setString(3, \"Yıldız\");\n" +
            "        preparedStatement.setString(2, \"Alperen\");\n" +
            "        preparedStatement.execute();\n" +
            "        preparedStatement.close();\n" +
            "\n" +

            "    }";
        this.addTest(test);

    }
}
