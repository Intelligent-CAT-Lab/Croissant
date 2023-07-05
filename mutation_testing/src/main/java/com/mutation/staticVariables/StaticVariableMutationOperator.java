package com.mutation.staticVariables;

import com.mutation.SootMutationOperator;
import com.mutation.StringInjector;
import soot.*;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;
import soot.util.Chain;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

class StaticVariableMutationAnalyzer extends SootMutationOperator {

    public SootField fieldWithStatic;
    public SootClass staticClass;
    public SootMethod testWithStaticVarMethod;
    public SootMethod staticVarMethod;
    private final Set<String> visited;

    public StaticVariableMutationAnalyzer() {
        this.visited = new HashSet<>();
    }

    @Override
    public void mutateMethod() throws Exception {
    }

    @Override
    public int getMutantNumber() {
        return 0;
    }

    @Override
    public <T extends Unit> List<T> locateUnits() {
        return null;
    }

    @Override
    protected void retrieveActiveAllMethods(SootClass mainClass) {

        Pattern javaBasicClassPattern = Pattern.compile("java[x]?\\..*");

        for (SootMethod sm : mainClass.getMethods()) {
            if (!sm.isAbstract()) {
                if (javaBasicClassPattern.matcher(mainClass.getName()).matches()) {
                    continue;
                }
                sm.setActiveBody(sm.retrieveActiveBody());
            }
        }
    }

    private boolean checkHasStatic() {
        boolean result = false;
        Chain<SootField> fields = this.getCurrentClass().getFields();
        for (SootField field : fields) {
            String className = field.getType().toString();
            SootClass sootClass;

            sootClass = setUp(this.inputDir, className);
            if (sootClass.getMethods().isEmpty()) {
                sootClass = setUp(this.inputDir.replaceAll("test-classes", "classes"), className);
            }

            if (field.isStatic()) {
                this.fieldWithStatic = field;
                this.staticClass = Scene.v().loadClass(field.getType().toString(), SootClass.HIERARCHY);
                return true;
            }
            if (shouldBeSearched(sootClass)) {
                result = (result || checkHasStaticHelper(sootClass));
                if (result) {
                    this.fieldWithStatic = field;
                    this.getCurrentClass().setApplicationClass();
                    return true;
                }
            }
        }
        this.getCurrentClass().setApplicationClass();
        return false;
    }


    private boolean checkHasStaticHelper(SootClass sootClass) {
        System.out.println(sootClass);
        Scene.v().loadClassAndSupport(sootClass.getName());
        Chain<SootField> fields = sootClass.getFields();
        boolean result = false;
        for (SootField field : fields) {
            String className = field.getType().toString();
            Scene.v().forceResolve(className, SootClass.BODIES);

            SootClass instanceVariableClass;
            instanceVariableClass = setUp(this.inputDir, className);
            if (instanceVariableClass.getMethods().isEmpty()) {
                instanceVariableClass = setUp(this.inputDir.replaceAll("test-classes", "classes"), className);
            }

            if (field.isStatic()) {
                this.staticClass = Scene.v().loadClass(field.getType().toString(), SootClass.HIERARCHY);
                return true;
            }
            if (shouldBeSearched(instanceVariableClass)) {
                result = (result || checkHasStaticHelper(instanceVariableClass));
            }
        }
        return result;
    }

    public boolean staticVarMethod() {
        boolean result = false;
        for (SootMethod method : this.getCurrentClass().getMethods()) {
            result = (result || staticVarMethod(method));
        }
        return result;
    }

    public boolean staticVarMethod(SootMethod sootMethod) {
        Chain<Unit> units = sootMethod.getActiveBody().getUnits();
        Iterator<Unit> unitIterator = units.snapshotIterator();

        while (unitIterator.hasNext()) {
            Stmt stmt = (Stmt) unitIterator.next();

            //TODO: implement for assign stmt
            if (!stmt.containsInvokeExpr()) {
                continue;
            }

            if (!(stmt.getInvokeExpr() instanceof VirtualInvokeExpr)) {
                continue;
            }

            VirtualInvokeExpr virtualInvokeExpr = (VirtualInvokeExpr) stmt.getInvokeExpr();
            Value base = virtualInvokeExpr.getBase();

            if (!(base instanceof Local)) {
                continue;
            }

            Local baseLocal = (Local) base;

            if (baseLocal.getType().equals(this.fieldWithStatic.getType())) {
                SootMethod varMethod = virtualInvokeExpr.getMethod();

                this.staticVarMethod = virtualInvokeExpr.getMethod();
                this.testWithStaticVarMethod = sootMethod;
                return true;
            }
        }
        return false;
    }

    private boolean shouldBeSearched(SootClass sootClass) {
        if (this.visited.contains(sootClass.getName())) {
            return false;
        }
        this.visited.add(sootClass.getName());
        return (!sootClass.getName().contains("java"));
    }

    public SootField cloneVariable(SootField field) {
        SootField clonedField = new SootField(field.getName() + "mutant", field.getType(), field.getModifiers());
        getCurrentClass().addField(clonedField);
        return clonedField;
    }

    public String getModifiers() {
        Boolean x = this.staticVarMethod.isPublic();
        return "";
    }

    @Override
    public boolean isApplicable() {
        boolean result = (checkHasStatic() && staticVarMethod());
        this.setCurrentClass(this.inputDir, getCurrentClass().getName());
        return result;
    }
}

public class StaticVariableMutationOperator extends StringInjector {

    String assertMethodCall;

    StaticVariableMutationAnalyzer staticVariableMutationAnalyzer;
    Integer constructorLocation;

    public StaticVariableMutationOperator() {
        this.staticVariableMutationAnalyzer = new StaticVariableMutationAnalyzer();
    }

    @Override
    public void setCurrentClass(String inputDir, String className) {
        this.inputDir = inputDir;
        this.className = className;

        List<String> inputDirParsed = Arrays.asList(inputDir.split(File.separator));
        StringJoiner inputDirJoiner = new StringJoiner(File.separator);

        inputDirParsed = inputDirParsed.subList(0, inputDirParsed.indexOf("src"));
        for (String elem : inputDirParsed) {
            inputDirJoiner.add(elem);
        }
        inputDirJoiner.add("target");
        inputDirJoiner.add("test-classes");

        this.staticVariableMutationAnalyzer.setCurrentClass(inputDirJoiner.toString(), className);
    }

    @Override
    public void setCurrentClass(SootClass currentClass) {
        this.staticVariableMutationAnalyzer.setCurrentClass(currentClass);
    }

    @Override
    public int getMutantNumber() {
        return 1;
    }

    @Override
    public void setCurrentMethod(String newMethod) {
        this.staticVariableMutationAnalyzer.setCurrentMethod(newMethod);
    }

    @Override
    public boolean isApplicable() {
        Boolean hasStaticVariable = this.staticVariableMutationAnalyzer.isApplicable();

        if (!hasStaticVariable) {
            return false;
        }

        Boolean hasAssertImport = false;
        for (String unit : this.getCurrentStateOfMutant()) {
            if (unit.contains("import org.junit.*")) {
                hasAssertImport = true;
                this.assertMethodCall = "Assert.assertTrue";
            }
            if (unit.contains("import static org.junit.Assert.*;") || units.contains("import static org.junit.Assert.assertTrue;")) {
                hasAssertImport = true;
                this.assertMethodCall = "assertTrue";
            }
        }

        if (!hasAssertImport) {
            return false;
        }

        String[] classNameParsed = this.staticVariableMutationAnalyzer.fieldWithStatic.getType().toString().split("\\.");
        String className = classNameParsed[classNameParsed.length - 1].replaceAll("\\[ *\\]", "\\\\[\\\\]");
        Pattern constructorPattern = Pattern.compile(".*=.*new.*" + className + " *\\(.*\\);");

        this.constructorLocation = 0;
        while (this.constructorLocation < this.units.size() - 1 && !constructorPattern.matcher(this.units.get(this.constructorLocation)).matches()) {
            this.constructorLocation++;
        }


        return hasStaticVariable && (this.constructorLocation != this.units.size() - 1);
    }

    @Override
    public void mutateMethod() throws Exception {
        long startTime = System.currentTimeMillis();

        String returnStmt = this.assertMethodCall + "(false);\n";
        switch (this.staticVariableMutationAnalyzer.staticVarMethod.getReturnType().toString()) {
            case "void":
                break;
            case "int":
            case "double":
            case "byte":
            case "long":
            case "float":
                returnStmt += "return 0;\n";
                break;
            case "char":
                returnStmt += "return 'a';\n";
                break;
            case "boolean":
                returnStmt += "return false;\n";
                break;
            default:
                returnStmt += "return null;\n";
        }

        SootMethod sm = this.staticVariableMutationAnalyzer.staticVarMethod;
        String methodSignature = "public " + sm.getSubSignature();

        String override = "{\n" +
            "@Override\n" +
            methodSignature + "\n" +
            "{\n" +
            returnStmt +
            "}\n" +
            "};\n";


        String s = this.units.get(this.constructorLocation).replaceAll(";", "");
        this.units.set(this.constructorLocation, s);
        this.units.add(this.constructorLocation, this.units.get(this.constructorLocation));
        this.constructorLocation++;
        this.units.remove(this.constructorLocation.intValue());


        String mutation = "double random = Math.random();\n" +
            "if (random < " + this.threshold + ") { \n" +
            s +
            override +
            "}";

        /*this.units.add(this.constructorLocation, mutation);*/
        this.units.add(this.constructorLocation, override);
        long endTime= System.currentTimeMillis();
        System.out.println(sm.getName());
        com.framework.OperatorSelector.MUTANT_TIME.put("CroissantMutant_OD_SVD_staticVariableVictim_StaticTemplatefieldClass();", endTime - startTime);

    }

}
