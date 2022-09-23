package com.mutation.staticVariables;


import com.mutation.StringInjector;
import com.template.TemplatePool;
import soot.*;
import soot.options.Options;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

class Analyzer {

    private SootClass currentClass;
    private String inputDir;
    private String className;
    private final Set<String> visited;
    public SootField instanceVarWithStatic;
    public SootField staticField;

    public Analyzer(String inputDir, String className) {
        this.visited = new HashSet<>();
        this.inputDir = inputDir;
        this.className = className;

    }


    private SootClass setUp(String inputDir, String className) {
        //setting options
        G.reset();
        Options.v().setPhaseOption("jb.tr", "ignore-wrong-staticness:true");
        Options.v().set_prepend_classpath(true);
        //
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_soot_classpath(inputDir);
        // if you do not use set_output_dir setting,
        // soot creates a sootOutput folder and puts exported class files there
        SootClass sc = Scene.v().loadClassAndSupport(className);
        sc.setApplicationClass();
        Scene.v().loadNecessaryClasses();
        Scene.v().addBasicClass("java.lang.System",SootClass.SIGNATURES);

        //loading soot class from scene
        SootClass mainClass = Scene.v().getSootClass(className);

        retrieveActiveAllMethods(mainClass);

        return mainClass;
    }

    private void retrieveActiveAllMethods(SootClass mainClass) {

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

    public void setClass(String dir, String className) {
        setClass(setUp(dir, className));
    }

    public void setClass(SootClass sootClass) {
        this.currentClass = sootClass;
    }

    private boolean shouldBeSearched(SootClass sootClass) {
        if (this.visited.contains(sootClass.getName())) {
            return false;
        }
        this.visited.add(sootClass.getName());
        return (!sootClass.getType().toString().contains("java"));
    }

    private SootField getStaticField(SootClass sootClass) {

/*        if (sootClass.getType().toString().contains("java")) {
            return null;
        }*/

        for (SootField sootField : sootClass.getFields()) {
            if (sootField.isStatic() && sootField.isPublic() && !sootField.isFinal()) {
                return sootField;
            }
        }
        return null;
    }


    public SootField getInstanceVarWithStatic() {
        /*setClass(inputDir, className);*/
        for (SootField sootField : this.currentClass.getFields()) {
            String className = sootField.getType().toString();
            SootClass sootClass;

            sootClass = setUp(this.inputDir, className);
            if (sootClass.getMethods().isEmpty()) {
                sootClass = setUp(this.inputDir.replaceAll("test-classes", "classes"), className);
            }

            SootField staticField = this.getStaticField(sootClass);

            if (staticField != null) {
                this.staticField = staticField;
                this.instanceVarWithStatic = sootField;

                return instanceVarWithStatic;
            }

        }
        return null;
    }

    public boolean isApplicable() {
        return this.getInstanceVarWithStatic() != null;
    }

}

public class ODStaticVariableMutationOperator extends StringInjector {

    Analyzer analyzer;
    TemplatePool templatePool;

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

        this.analyzer = new Analyzer(inputDirJoiner.toString(), className);
        this.analyzer.setClass(inputDirJoiner.toString(), className);

    }

    @Override
    public boolean isApplicable() {
        return this.analyzer.isApplicable();
    }

    private int getFieldLineNumber(SootField sootField) {
        String fieldName = sootField.getName();
        String[] typeNameArray = sootField.getType().toString().split("\\.");
        String typeName = typeNameArray[typeNameArray.length-1];

        int i = 0;
        for (String unit: this.units) {
            if (unit.contains(fieldName) && unit.contains(typeName)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    private void cloneField(SootField field) {
        int index = getFieldLineNumber(field);

        if (index > 0) {
            String newField = this.units.get(index).replaceAll(field.getName(), field.getName()+"Mutant");
            this.units.add(index, newField);
        }

    }

    private String createTest(String testName) {
        return "@Test\n" +
                "public void " + testName + "()" + "{\n";
    }

    private String createPolluterTest(SootField field, SootField innerField) {


        String returnStatement = null;
        switch(innerField.getType().toString()) {
            case "byte":
            case "long":
            case "float":
            case "double":
            case "int":
            case "shoty":
                returnStatement = "0";
                break;
            case "char":
                returnStatement = "a";
                break;
            default:
                returnStatement = "null";
        }

        String polluterTest = createTest("a_staticVariablePolluter_" + field.getName()) +
            "   " + field.getName() + "Mutant" + "." + innerField.getName() + " = "+returnStatement+";\n" +
            "   " + "}\n";


        int classEndIndex = this.findMainClassEnd();
        this.units.add(classEndIndex, polluterTest);
        return polluterTest;
    }

    private String createVictimTest(SootField field, SootField innerField) {

        String assertStmt = null;

        if (this.jupiter) {
            assertStmt = "Assertions.";
        } else {
            assertStmt = "Assert.";
        }

        switch(innerField.getType().toString()) {
            case "byte":
            case "long":
            case "float":
            case "double":
            case "int":
            case "shoty":
                assertStmt += "assertNotEquals(0,";
                break;
            case "char":
                assertStmt = "a";
                break;
            default:
                assertStmt += "assertNotNull(";
        }

        String victimTest = createTest("b_staticVariableVictim_" + field.getName()) +
            "   " + assertStmt+ field.getName()+"Mutant." + innerField.getName() +");\n" +
            "}\n";

        int classEndIndex = this.findMainClassEnd();
        this.units.add(classEndIndex, victimTest);
        return victimTest;
    }

    private  String createstateSetterTest(SootField field, SootField innerField, int stateSetterNumber) {
        String stateSetterTest = createTest("c_staticVariablestateSetter_" + field.getName() + "__" + stateSetterNumber) +
            "incrementstateSetterCounter();\n"+
            "if (stateSetterCounter >= getstateSetterCount()) {return;}\n" +
            "   " + field.getName()+"Mutant." + innerField.getName() + " = " + field.getName() + "." + innerField.getName() + ";\n" +

            "   }";
        int classEndIndex = this.findMainClassEnd();
        this.units.add(classEndIndex, stateSetterTest);
        return stateSetterTest;
    }



    @Override
    public void mutateMethod() throws Exception {

        // find appropriate field
        System.out.println(this.analyzer.instanceVarWithStatic);
        System.out.println(this.analyzer.staticField);

        cloneField(this.analyzer.instanceVarWithStatic);
        this.createPolluterTest(this.analyzer.instanceVarWithStatic, this.analyzer.staticField);
        this.createVictimTest(this.analyzer.instanceVarWithStatic, this.analyzer.staticField);
        for (int i = 0; i < 5; i++) {
            this.createstateSetterTest(this.analyzer.instanceVarWithStatic, this.analyzer.staticField, i);
        }



    }

    @Override
    public int getMutantNumber() {
        return 1;
    }
}

