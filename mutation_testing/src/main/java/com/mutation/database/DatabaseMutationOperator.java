package com.mutation.database;

import com.mutation.SootMutationOperator;
import soot.*;
import soot.jimple.*;
import soot.util.Chain;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatabaseMutationOperator extends SootMutationOperator {

    List<String> tableSchema = new ArrayList<>();
    List<String> insertedValues = new ArrayList<>();
    List<Integer> insertedIndices = new ArrayList<>();
    String tableName;
    String delimiter = "*";
    Local connectionLocal;

    private SootMethod createstateSetterTest(int stateSetterTestNumber) {

        SootMethod stateSetterTest = this.createTestMethod("c_cleanerTestMutant" +  stateSetterTestNumber + this.getCurrentMethodName());;
        stateSetterTest.setActiveBody((Body) this.getCurrentMethod().getActiveBody().clone());

        /*this.getCurrentClass().addMethod(polluterTest);*/
        // remove execute statement from the source test ( interfaceinvoke r4.<java.sql.PreparedStatement: boolean execute()>())

        Chain<Unit> units = stateSetterTest.getActiveBody().getUnits();
        Iterator<Unit> unitIterator = units.snapshotIterator();

        while(unitIterator.hasNext()) {
            Stmt stmt = (Stmt) unitIterator.next();

            if (!(stmt instanceof InvokeStmt)) {
                continue;
            }

            InvokeStmt invokeStmt = (InvokeStmt) stmt;
            InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();

            if (!(invokeExpr instanceof InterfaceInvokeExpr)) {
                continue;
            }

            InterfaceInvokeExpr interfaceInvokeExpr = (InterfaceInvokeExpr) invokeExpr;

            if (interfaceInvokeExpr.getMethod().getSignature().contains("java.sql.PreparedStatement: boolean execute")) {
                units.remove(stmt);
            }


        }
        //  create sql query checking if the selected values exist

        Map<Integer, String> pairList = new TreeMap<>();

        Iterator<Integer> indexIter = this.insertedIndices.iterator();
        Iterator<String> valueIter = this.insertedValues.iterator();

        while (indexIter.hasNext() && valueIter.hasNext()) {
            pairList.put(indexIter.next(), valueIter.next());
        }

        StringBuilder queryBuilder = new StringBuilder("DELETE FROM " + this.tableName + " WHERE ");

        StringJoiner requirementsBuilder = new StringJoiner(" AND ");

        for (Integer index : pairList.keySet()) {
            String field = this.tableSchema.get(index - 1);
            String value = pairList.get(index);

            StringBuilder requirementBuilder = new StringBuilder();
            requirementBuilder.append(field).append("=").append("'").append(value).append(this.delimiter).append("' ");
            requirementsBuilder.add(requirementBuilder);
        }

        queryBuilder.append(requirementsBuilder);
        String constructedQuery = queryBuilder.toString();

        //  get values

        Local connection = null;

        Chain<Local> locals = stateSetterTest.getActiveBody().getLocals();
        Iterator<Local> iterator = locals.snapshotIterator();

        while(iterator.hasNext()) {
            Local local = iterator.next();
            String localName = local.getName();

            if (localName.equals(this.connectionLocal.getName())) {
                connection = local;
            }
        }


        // java.sql.PreparedStatement r6 = interfaceinvoke $r5.<java.sql.Connection: java.sql.PreparedStatement prepareStatement(java.lang.String)>("SELECT * FROM user WHERE fName = \'Alperen\'");
        SootClass preparedStatementClass = Scene.v().forceResolve("java.sql.PreparedStatement", SootClass.SIGNATURES);
        Local preparedStatement = Jimple.v().newLocal("r6", RefType.v("java.sql.PreparedStatement"));
        stateSetterTest.getActiveBody().getLocals().add(preparedStatement);

        SootClass connectionClass = Scene.v().forceResolve("java.sql.Connection", SootClass.SIGNATURES);
        SootMethod preparedStatementMethod = connectionClass.getMethod("java.sql.PreparedStatement prepareStatement(java.lang.String)");
        InterfaceInvokeExpr createPreparedStatement = Jimple.v().newInterfaceInvokeExpr(connection, preparedStatementMethod.makeRef(), StringConstant.v(constructedQuery));
        AssignStmt preparedStatementAssignStmt = Jimple.v().newAssignStmt(preparedStatement, createPreparedStatement);
        this.InsertStmtEnd(stateSetterTest, preparedStatementAssignStmt);


        SootMethod executeQueryMethod = preparedStatementClass.getMethodByName("execute");
        InterfaceInvokeExpr executeQueryExpr = Jimple.v().newInterfaceInvokeExpr(preparedStatement, executeQueryMethod.makeRef());
        InvokeStmt invokeStmt = Jimple.v().newInvokeStmt(executeQueryExpr);
        this.InsertStmtEnd(stateSetterTest, invokeStmt);

        return this.makestateSetterTest(stateSetterTest,stateSetterTestNumber);

    }

    private void createstateSetters() {
        for (int i = 0; i < 5; i++) {
            SootMethod stateSetterTest = this.createstateSetterTest(i);
            this.getCurrentClass().addMethod(stateSetterTest);
        }
    }


    private void initializeTableSchema() {
        Chain<Unit> units = this.getUnits();
        Iterator<Unit> unitIterator = units.iterator();

        while (unitIterator.hasNext()) {
            Stmt stmt = (Stmt) unitIterator.next();
            //  r4 = interfaceinvoke $r3.<java.sql.Connection: java.sql.PreparedStatement prepareStatement(java.lang.String)>("INSERT INTO user(id, fName, lName) values(?, ?, ?)");
            //  find the PreparedStatement

            if (!(stmt instanceof AssignStmt)) {
                continue;
            }

            AssignStmt assignStmt = (AssignStmt) stmt;
            Value rightHandValue = assignStmt.getRightOp();

            if (!(rightHandValue instanceof InterfaceInvokeExpr)) {
                continue;
            }

            InterfaceInvokeExpr invokeExpr = (InterfaceInvokeExpr) rightHandValue;
            SootMethod sootMethod = invokeExpr.getMethod();

            if (!(sootMethod.getSignature().contains("prepareStatement"))) {
                continue;
            }

            StringConstant insertStringConstant = (StringConstant) invokeExpr.getArg(0);
            String insertString = insertStringConstant.value; // INSERT INTO user(id, fName, lName) values(?, ?, ?)
            Pattern pattern = Pattern.compile(".*INSERT INTO (.*)\\((.*)\\) values\\(.*\\)");
            Matcher matcher = pattern.matcher(insertString);
            if (matcher.find()) {
                tableName = matcher.group(1);
                String schema = matcher.group(2);
                this.tableSchema = Arrays.asList(schema.split("\\s*,\\s*"));
            }

            Value base = invokeExpr.getBase();

            if (!(base instanceof Local)) {
                continue;
            }

            Local baseLocal = (Local) base;

            if (!(baseLocal.getType().toString().equals("java.sql.Connection"))) {
                continue;
            }

            this.connectionLocal = baseLocal;


        }
    }

    private void initializeInsertedValues() {
        Chain<Unit> units = this.getUnits();
        Iterator<Unit> unitIterator = units.iterator();

        List<Integer> indices = new ArrayList<>();
        List<String> values = new ArrayList<>();

        while(unitIterator.hasNext()) {
            Stmt stmt = (Stmt) unitIterator.next();

            // interfaceinvoke r4.<java.sql.PreparedStatement: void setString(int,java.lang.String)>(2, "Alperen");

            if (!(stmt instanceof InvokeStmt)) {
                continue;
            }

            InvokeStmt invokeStmt = (InvokeStmt) stmt;
            InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();

            if (!(invokeExpr instanceof InterfaceInvokeExpr)) {
                continue;
            }

            InterfaceInvokeExpr interfaceInvokeExpr = (InterfaceInvokeExpr) invokeExpr;
            String methodSignature = invokeExpr.getMethod().getSignature();

            if (methodSignature.contains("setString")) {
                IntConstant indexIntConstant = (IntConstant) interfaceInvokeExpr.getArg(0);
                StringConstant valueStringConstant =  (StringConstant) interfaceInvokeExpr.getArg(1);

                Integer index = indexIntConstant.value;
                String value = valueStringConstant.value;

                insertedValues.add(value);
                insertedIndices.add(index);
            }
        }


    }

    public void reset() {
        this.tableSchema = new ArrayList<>();
        this.insertedValues = new ArrayList<>();
        this.insertedIndices = new ArrayList<>();
        this.tableName = null;
        this.connectionLocal = null;
    }

    @Override
    public boolean isApplicable() {
        reset();

        initializeTableSchema();
        initializeInsertedValues();

        return (this.connectionLocal != null && this.tableName != null && !this.insertedIndices.isEmpty() && !this.insertedValues.isEmpty() && !this.tableSchema.isEmpty());
    }

    @Override
    public void mutateMethod() throws Exception {
        //  find the insert query string  -> INSERT INTO user(id, fName, lName) values(?, ?, ?)
        //  find the table schema ->  user(id, fName, lName)
        //  find inserted values -> preparedStatement.setString(2, "Alperen") etc.
        //************************

        //  create victim test
        //createVictimTest();

        //  clone the test as polluter (same test but inserted values are different)
        //createPolluterTest();

        //create stateSetters
        //createstateSetters();

    }

    private void createPolluterTest() {
        SootMethod polluterTest = this.createTestMethod("a_PolluterMutant" + this.getCurrentMethodName());;
        polluterTest.setActiveBody((Body) this.getCurrentMethod().getActiveBody().clone());

        Chain<Unit> units = polluterTest.getActiveBody().getUnits();
        Iterator<Unit> unitIterator = units.snapshotIterator();

        while(unitIterator.hasNext()) {
            // interfaceinvoke r4.<java.sql.PreparedStatement: void setString(int,java.lang.String)>(2, "Alperen");
            Stmt stmt = (Stmt) unitIterator.next();
            if (!(stmt instanceof InvokeStmt)) {
                continue;
            }

            InvokeStmt invokeStmt = (InvokeStmt) stmt;
            InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();

            if (!(invokeExpr instanceof InterfaceInvokeExpr)) {
                continue;
            }

            InterfaceInvokeExpr interfaceInvokeExpr = (InterfaceInvokeExpr) invokeExpr;
            String interfaceMethodSignature = interfaceInvokeExpr.getMethod().getSignature();

            if (!(interfaceMethodSignature.contains("void setString(int,java.lang.String)"))) {
                continue;
            }

            StringConstant argConstant = (StringConstant) interfaceInvokeExpr.getArg(1);
            String arg = argConstant.value;

            interfaceInvokeExpr.setArg(1, StringConstant.v(arg+this.delimiter));

        }

        this.getCurrentClass().addMethod(polluterTest);
    }

    private void createVictimTest() {
        long startTime = System.currentTimeMillis();

        SootMethod polluterTest = this.createTestMethod("CroissantMutant_OD_DSD_b_VictimMutant" + this.getCurrentMethodName()+"DatabaseTemplate");;
        polluterTest.setActiveBody((Body) this.getCurrentMethod().getActiveBody().clone());

        this.getCurrentClass().addMethod(polluterTest);
        // remove execute statement from the source test ( interfaceinvoke r4.<java.sql.PreparedStatement: boolean execute()>())

        Chain<Unit> units = polluterTest.getActiveBody().getUnits();
        Iterator<Unit> unitIterator = units.snapshotIterator();

        while(unitIterator.hasNext()) {
            Stmt stmt = (Stmt) unitIterator.next();

            if (!(stmt instanceof InvokeStmt)) {
                continue;
            }

            InvokeStmt invokeStmt = (InvokeStmt) stmt;
            InvokeExpr invokeExpr = invokeStmt.getInvokeExpr();

            if (!(invokeExpr instanceof InterfaceInvokeExpr)) {
                continue;
            }

            InterfaceInvokeExpr interfaceInvokeExpr = (InterfaceInvokeExpr) invokeExpr;

            if (interfaceInvokeExpr.getMethod().getSignature().contains("java.sql.PreparedStatement: boolean execute")) {
                units.remove(stmt);
            }


        }
        //  create sql query checking if the selected values exist

        Map<Integer, String> pairList = new TreeMap<>();

        Iterator<Integer> indexIter = this.insertedIndices.iterator();
        Iterator<String> valueIter = this.insertedValues.iterator();

        while (indexIter.hasNext() && valueIter.hasNext()) {
            pairList.put(indexIter.next(), valueIter.next());
        }

        StringBuilder queryBuilder = new StringBuilder("SELECT * FROM " + this.tableName + " WHERE ");

        StringJoiner requirementsBuilder = new StringJoiner(" AND ");

        for (Integer index : pairList.keySet()) {
            String field = this.tableSchema.get(index - 1);
            String value = pairList.get(index);

            StringBuilder requirementBuilder = new StringBuilder();
            requirementBuilder.append(field).append("=").append("'").append(value).append(this.delimiter).append("' ");
            requirementsBuilder.add(requirementBuilder);
        }

        queryBuilder.append(requirementsBuilder);
        String constructedQuery = queryBuilder.toString();

        //  get values

        Local connection = null;

        Chain<Local> locals = polluterTest.getActiveBody().getLocals();
        Iterator<Local> iterator = locals.snapshotIterator();

        while(iterator.hasNext()) {
            Local local = iterator.next();
            String localName = local.getName();

            if (localName.equals(this.connectionLocal.getName())) {
                connection = local;
            }
        }


        // java.sql.PreparedStatement r6 = interfaceinvoke $r5.<java.sql.Connection: java.sql.PreparedStatement prepareStatement(java.lang.String)>("SELECT * FROM user WHERE fName = \'Alperen\'");
        SootClass preparedStatementClass = Scene.v().forceResolve("java.sql.PreparedStatement", SootClass.SIGNATURES);
        Local preparedStatement = Jimple.v().newLocal("r6", RefType.v("java.sql.PreparedStatement"));
        polluterTest.getActiveBody().getLocals().add(preparedStatement);

        SootClass connectionClass = Scene.v().forceResolve("java.sql.Connection", SootClass.SIGNATURES);
        SootMethod preparedStatementMethod = connectionClass.getMethod("java.sql.PreparedStatement prepareStatement(java.lang.String)");
        InterfaceInvokeExpr createPreparedStatement = Jimple.v().newInterfaceInvokeExpr(connection, preparedStatementMethod.makeRef(), StringConstant.v(constructedQuery));
        AssignStmt preparedStatementAssignStmt = Jimple.v().newAssignStmt(preparedStatement, createPreparedStatement);
        this.InsertStmtEnd(polluterTest, preparedStatementAssignStmt);


        //java.sql.ResultSet  r7 = interfaceinvoke r6.<java.sql.PreparedStatement: java.sql.ResultSet executeQuery()>();
        SootClass resultSetClass = Scene.v().forceResolve("java.sql.ResultSet", SootClass.SIGNATURES);
        Local resultSet = Jimple.v().newLocal("r7", RefType.v("java.sql.ResultSet"));
        locals.add(resultSet);

        SootMethod executeQueryMethod = preparedStatementClass.getMethod("java.sql.ResultSet executeQuery()");
        InterfaceInvokeExpr executeQueryExpr = Jimple.v().newInterfaceInvokeExpr(preparedStatement, executeQueryMethod.makeRef());
        AssignStmt assignResultSet = Jimple.v().newAssignStmt(resultSet, executeQueryExpr);
        this.InsertStmtEnd(polluterTest, assignResultSet);


        //boolean $z1 = interfaceinvoke r7.<java.sql.ResultSet: boolean next()>();
        Local nextMethodCallResult = Jimple.v().newLocal("$z1", BooleanType.v());
        locals.add(nextMethodCallResult);

        SootMethod nextMethod = resultSetClass.getMethod("boolean next()");
        InterfaceInvokeExpr nextMethodClassExpr = Jimple.v().newInterfaceInvokeExpr(resultSet, nextMethod.makeRef());
        AssignStmt nextMethodClassStmt = Jimple.v().newAssignStmt(nextMethodCallResult, nextMethodClassExpr);
        this.InsertStmtEnd(polluterTest, nextMethodClassStmt);


        //  assertTrue values list is empty
        SootMethod assertFalse = this.getAssertFalse();
        StaticInvokeExpr assertFalseExpr = Jimple.v().newStaticInvokeExpr(assertFalse.makeRef(), nextMethodCallResult);
        InvokeStmt assertFalseStmt = Jimple.v().newInvokeStmt(assertFalseExpr);
        this.InsertStmtEnd(polluterTest, assertFalseStmt);


        long endTime= System.currentTimeMillis();
        com.framework.OperatorSelector.MUTANT_TIME.put(polluterTest.getName(),endTime - startTime);
    }


    @Override
    public <T extends Unit> List<T> locateUnits() {
        return null;
    }



    @Override
    public int getMutantNumber() {
        return 1;
    }


}
