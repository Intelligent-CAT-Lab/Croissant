package com.mutation;

import soot.SootClass;
import soot.SootMethod;

import javax.tools.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Pattern;


public abstract class StringMutationOperator implements MutationOperator {
    protected ArrayList<String> units;
    protected String inputDir;
    protected String className;
    protected String currentMethod;
    protected boolean jupiter;
    protected double threshold;

    // FIXME remove when implemented in all MOs.
    public void runOnceBefore() {

    }

    public void setJupiter(boolean jupiter) {
        this.jupiter = jupiter;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public void runOnceAfter() {

    }

    StringMutationOperator() {
        this.units = new ArrayList<>();
    }

    @Override
    public String getCurrentMethodName() {
        return this.currentMethod;
    }

    @Override
    public void setCurrentClass(String inputDir, String className) {
        this.inputDir = inputDir;
        this.className = className;
    }

    @Override
    public void setCurrentClass(SootClass currentClass) {}

    public List<StringSource> dump() {
        StringBuilder stringBuilder = new StringBuilder();
        for (String unit: units) {
            stringBuilder.append(unit);
            stringBuilder.append("\n");
        }


        String code = stringBuilder.toString();
        System.out.println(code);


        String[] classNameParsed = this.className.split("\\.");

        List<StringSource> dumped = new ArrayList<>();
        dumped.add(new StringSource(classNameParsed[classNameParsed.length-1], code));

        return dumped;
    }

    public void changeClassName(String newName) {

        for (int i=0; i < this.units.size(); i++) {
            if (this.units.get(i).contains("public class")) {
                this.units.set(i, units.get(i).replace(this.className, newName));
            }
        }
    }

    public String getJars() throws IOException {
        String workingDir = this.inputDir.substring(0, this.inputDir.indexOf("src"));
        Process exec = Runtime.getRuntime().exec(new String[] {"mvn","-f",workingDir,"dependency:build-classpath"});
        BufferedReader reader = new BufferedReader(new InputStreamReader(exec.getInputStream()));
        String line;
        String jars = null;
        while ( (line = reader.readLine()) != null) {
            if (line.contains(".jar")) {
                jars = line;
            }
        }
        System.out.println(jars);
        return jars;
    }
    public String parseImportedClass(String importStmt) {
        //TODO: implement with native java path api
        String classPath = importStmt.split(" ")[1].replaceAll("\\.", "/").replace(";", "");
        String workingDirectory = this.inputDir.substring(0, this.inputDir.indexOf("src"));
        String absolutePath = (workingDirectory + classPath + ".class").replaceFirst("src/main/java", "target/classes");
        return absolutePath;
    }

    public String getImportedClasses() {
        StringJoiner stringJoiner = new StringJoiner(":");
        for (String unit: this.units) {
            if (unit.contains("import")) {
                stringJoiner.add(parseImportedClass(unit.replaceAll("src/main/java","target/classes")));
            }
        }
        return stringJoiner.toString();
    }


    public void exportClass(String outputDir, String fileName) throws IOException {

        changeClassName(fileName);
        this.className = fileName;
        exportClass(outputDir);

    }

    @Override
    public void setCurrentMethod(String newMethod) {
        this.currentMethod = newMethod;
    }


    public void exportClass(String outputDir) throws IOException {

        List<StringSource> dumped = dump();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();


        JavaCompiler.CompilationTask task = compiler.getTask(
            null,
            null,
            diagnostics,
            Arrays.asList("-d", "/Users/alperenyildiz/Desktop/defects4j/project_repos/gson/gson/target/test-classes", "-cp", "/Users/alperenyildiz/Desktop/defects4j/project_repos/gson/gson/src/main/java/com/google/gson/reflect/TypeToken.java"),
            null, dumped);
        boolean success = task.call();

        if (!success) {
            for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
                System.out.println(diagnostic.getCode());
                System.out.println(diagnostic.getKind());
                System.out.println(diagnostic.getPosition());
                System.out.println(diagnostic.getStartPosition());
                System.out.println(diagnostic.getEndPosition());
                System.out.println(diagnostic.getSource());
                System.out.println(diagnostic.getMessage(null));
            }
        }
    }

    public ArrayList<String> getCurrentStateOfMutant() {
        return this.units;
    }

    public void setCurrentStateOfMutant(ArrayList<String> units) {
        this.units = units;
    }

    public void insertCode(String code, Integer index) {
        this.units.add(index, code);
    }


    protected int findMainClassEnd() {
        int start = -1;
        Pattern mainClassPattern = Pattern.compile(" *public * .* *(?:final)? *class .*");

        do {
            start++;
        } while (start < this.units.size() - 1 && !mainClassPattern.matcher(this.units.get(start)).matches()&&!this.units.get(start).toString().contains("class PositionTest"));

        int end = start + 1;
        int i = start + 1;

        Pattern classPattern = Pattern.compile(" *class *.*");
        Pattern closingBracketPattern = Pattern.compile(".*}.*");

        do {
            i++;
            if (closingBracketPattern.matcher(this.units.get(i)).matches()) {
                end = i;
            }
        } while (!classPattern.matcher(this.units.get(i)).matches() && i < this.units.size() - 1&&!this.units.get(start).toString().contains("class PositionTest"));
        return end;
    }
    @Override
    public void setCurrentMethod(SootMethod newMethod) {

    }
}

