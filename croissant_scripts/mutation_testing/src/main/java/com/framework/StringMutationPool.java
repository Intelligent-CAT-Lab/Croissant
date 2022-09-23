package com.framework;

import com.mutation.MutationOperator;
import com.mutation.StringInjector;
import com.mutation.StringMutationOperator;
import com.mutation.asyncWait.LatchObjectMO;
import com.mutation.asyncWait.ThreadSleepMO;
import com.mutation.asyncWait.VoidLatchAwaitMO;
import com.mutation.cachedDataIsNotTearedDown.CaffeineCDMO;
import com.mutation.database.DatabaseMutationOperator;
import com.mutation.filepermission.*;
import com.mutation.framework.MockitoMutationOperator;
import com.mutation.hardcodedPortMO.jettyServerSetHardcodedPortMO;
import com.mutation.hardcodedPortMO.jettyServerSetNonHardcodedPortMO;
import com.mutation.initializationOfTimeValuesTwice.TimeValueInitializationMutationOperator;
import com.mutation.instance.*;
import com.mutation.multithreading.deadlock.DeadLockMutationOperator;
import com.mutation.multithreading.racecondition.RaceConditionMutationOperator;
import com.mutation.reflection.UnorderedPopulationMutationOperator;
import com.mutation.resourceBoundViolation.memoryBoundViolationMO;
import com.mutation.resourceNotCreated.newFileNullMO;
import com.mutation.resourceNotCreated.newFileNullODMO;
import com.mutation.staticVariables.ODStaticVariableMutationOperator;
import com.mutation.staticVariables.StaticVariableMutationOperator;
import com.mutation.timeZoneDependency.TimeZoneDependencyMO;
import com.mutation.timeout.GlobalTMO;
import com.mutation.timeout.LocalTMO;
import com.mutation.tooRestrictiveRange.TRRInjectAssertLocal;
import com.mutation.tooRestrictiveRange.WIPTRRInjectAssertInstance;
import com.mutation.unorderedCollections.assertionInsert.HashMapUAMO;
import com.mutation.unorderedCollections.assertionInsert.HashSetUAMO;
import com.mutation.unorderedCollections.assertionInsert.JsonUAMO;
import com.mutation.unorderedCollections.index.JsonUCIMO;
import com.mutation.unorderedCollections.index.SetUCIMO;
import com.mutation.unorderedCollections.iteration.JsonIUCMO;
import com.mutation.unorderedCollections.iteration.MapIUCMO;
import com.mutation.unorderedCollections.iteration.SetIUCMO;
import com.mutation.unorderedCollections.orderedStringConversion.HashMapStringMO;
import com.mutation.unorderedCollections.orderedStringConversion.HashSetStringMO;
import com.mutation.unorderedCollections.orderedStringConversion.JsonStringMO;
import fj.test.Bool;
import soot.SootClass;
import soot.SootMethod;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class BadClassException extends Exception {
    BadClassException(String message) {
        super(message);
    }
}

class CompilationException extends Exception {
    CompilationException(String message) {
        super(message);
    }
}

//TODO: implement the same for soot mutation operators
public class StringMutationPool {

    Boolean jupiter;
    List<String> allowedOperators;
    String inputDir;
    String className;
    String outputDir;
    ArrayList<String> mutant;
    Set<StringMutationOperator> stringMutationOperators;
    SootClass targetClass;
    File testSuiteFile;
    Boolean groundTruth;
    File mutantSourceFile;
    Integer mutantCount;
    double threshold;


    public StringMutationPool(String dir, String testSuiteName, String outputDir, SootClass targetClass, Boolean jupiter, Boolean groundTruth, List<String> allowedOperators) throws FileNotFoundException {
        this.inputDir = dir;
        this.className = testSuiteName;
        this.outputDir = outputDir;
        this.mutantCount = 0;
        this.jupiter = jupiter;
        this.groundTruth = groundTruth;
        this.allowedOperators = allowedOperators;

        this.stringMutationOperators = new HashSet<StringMutationOperator> (Arrays.asList(
            new RaceConditionMutationOperator(),
            new DeadLockMutationOperator(),
            new ODStaticVariableMutationOperator()
        ));


        if (allowedOperators.size() != 0) {
            Set<StringMutationOperator> operators = new HashSet<>();

            for (StringMutationOperator mutationOperator : this.stringMutationOperators) {
                String name = mutationOperator.getClass().getSimpleName();
                if (allowedOperators.contains(name)) {
                    operators.add(mutationOperator);
                }
            }
            this.stringMutationOperators = operators;
        }

        this.mutant = new ArrayList<>();
        this.targetClass = targetClass;

        read();

    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public String getSourceFile(String classFilePath) {
        //TODO: implement with java path
        return classFilePath.replace("target/test-classes", "src/test/java");
    }


    public void read() throws FileNotFoundException {
        String classPath = this.className.replaceAll("\\.", "/");
        Path filePath = Paths.get(this.inputDir, classPath);
        this.testSuiteFile = new File(getSourceFile(filePath + ".java"));
        Scanner s = new Scanner(new FileReader(this.testSuiteFile));
        while (s.hasNext()) {
            this.mutant.add(s.nextLine());
        }
    }

    public Integer start() throws Exception {
        /*this.tempFile = this.createTempFile(this.testSuiteFile);*/
        this.tempFile = this.getTempFile(this.testSuiteFile);
        addUtilityFunctions();
        for (StringMutationOperator stringMutationOperator : this.stringMutationOperators) {
            stringMutationOperator.setCurrentStateOfMutant(this.mutant);
            stringMutationOperator.setThreshold(this.threshold);
            stringMutationOperator.setJupiter(this.jupiter);

            stringMutationOperator.setCurrentClass(this.getSourceFile(this.inputDir), this.className);
            if (!stringMutationOperator.isApplicable()) {
                continue;
            }
            this.mutantCount++;
            if (stringMutationOperator instanceof StringInjector) {

		//                System.out.println(stringMutationOperator + " is applicable for " + stringMutationOperator.getMutantNumber() + " test(s)");
                ArrayList<String> originalState = new ArrayList<>();
                for (String elem: this.mutant) {
                    originalState.add(elem);
                }

                stringMutationOperator.mutateMethod();
                this.mutant = stringMutationOperator.getCurrentStateOfMutant();
                /*renameConstructor();*/
                this.renameClassToMutant(stringMutationOperator.getClass().getSimpleName());
                export(stringMutationOperator.getClass().getSimpleName());
                compile(stringMutationOperator.getClass().getSimpleName());
                 remove(stringMutationOperator.getClass().getSimpleName());

                this.mutant = new ArrayList<>();
                for (String elem: originalState) {
                    this.mutant.add(elem);
                }

                continue;
            }

            for (SootMethod sootMethod : this.targetClass.getMethods()) {
                ArrayList<String> originalState = new ArrayList<>();
                for (String elem: this.mutant) {
                    originalState.add(elem);
                }
		//                System.out.println(stringMutationOperator + " is applicable for " + sootMethod.getSignature() + " for " + stringMutationOperator.getMutantNumber() + "test(s)");
                stringMutationOperator.setCurrentMethod(sootMethod.getName());
                stringMutationOperator.mutateMethod();
                this.mutant = stringMutationOperator.getCurrentStateOfMutant();
                renameConstructor();
                this.renameClassToMutant(stringMutationOperator.getClass().getSimpleName());
                export(stringMutationOperator.getClass().getSimpleName());
                compile(stringMutationOperator.getClass().getSimpleName());
                remove(stringMutationOperator.getClass().getSimpleName());

                this.mutant = new ArrayList<>();
                for (String elem: originalState) {
                    this.mutant.add(elem);
                }
            }
        }
        return this.mutantCount;
    }

    private void renameConstructor() {
        String[] classNameSplitted = this.className.split("\\.");
        String testClassName = classNameSplitted[classNameSplitted.length - 1];
        for (int i = 0; i < this.mutant.size() - 1; i++) {
            String mutated = this.mutant.get(i).replaceAll("public " + testClassName + "\\(", "public " + testClassName + "Mutant" + "\\(");
            this.mutant.set(i, mutated);
        }
    }

    private void renameClassToMutant(String operatorName) throws BadClassException {
        String[] classNameParsed = this.className.split("\\.");
        String className = classNameParsed[classNameParsed.length - 1];
        String classDeclarationUnit = null;

        int index = -1;
        Pattern mainClassPattern = Pattern.compile(" *public * .* *(?:final)? *class .*");
        do {
            index++;
            classDeclarationUnit = this.mutant.get(index);
        } while (!mainClassPattern.matcher(classDeclarationUnit).matches() && index < this.mutant.size() - 1);

        if (index == -1) {
            throw new BadClassException("Class is empty");
        }

        if (classDeclarationUnit == null) {
            throw new BadClassException("Cannot find class declaration");
        }

        classDeclarationUnit = classDeclarationUnit.replace(className, className + "_" + operatorName + "_Test");

        this.mutant.remove(index);
        this.mutant.add(index, classDeclarationUnit);
    }

    private File createTempFile(File file) throws IOException {
        StringJoiner stringJoiner = new StringJoiner(File.separator);
        String[] classNameParsed = this.className.split("\\.");

        for (String elem : classNameParsed) {
            stringJoiner.add(elem);
        }

        File mutantSourceFile = new File(this.getSourceFile(this.inputDir), stringJoiner + ".temp");
        mutantSourceFile.createNewFile();

        String fileContent = "";

        try
        {
            byte[] bytes = Files.readAllBytes(file.toPath());
            fileContent = new String (bytes);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }


        BufferedWriter writer = new BufferedWriter(new FileWriter(mutantSourceFile));
        writer.write(fileContent);
        writer.close();



        return mutantSourceFile;
    }

    private File getTempFile(File file) throws IOException {
        StringJoiner stringJoiner = new StringJoiner(File.separator);
        String[] classNameParsed = this.className.split("\\.");

        for (String elem : classNameParsed) {
            stringJoiner.add(elem);
        }

        File mutantSourceFile = new File(this.getSourceFile(this.inputDir), stringJoiner + "_original.temp");

        return mutantSourceFile;
    }

    private File createMutantSourceFile(String operatorName) throws IOException {
        String className = this.className + "_" + operatorName + "_Test";
        StringJoiner stringJoiner = new StringJoiner(File.separator);
        String[] classNameParsed = className.split("\\.");

        for (String elem : classNameParsed) {
            stringJoiner.add(elem);
        }

        File mutantSourceFile = new File(this.getSourceFile(this.inputDir), stringJoiner + ".java");
        /*File mutantSourceFile = new File(name);*/
        mutantSourceFile.createNewFile();
        return mutantSourceFile;
    }

    private void exportMutant(File mutantSourceFile) throws IOException {
        FileWriter writer = new FileWriter(mutantSourceFile);
        for (String unit : this.mutant) {
            writer.write(unit + "\n");
        }
        writer.flush();
        writer.close();
    }

    private void cropInnerClasses() {

        String packageStmt = "\n";
        List<String> importStmts = new ArrayList<>();

        int mainClassStart = -1;

        String currentUnit;
        do {
            mainClassStart++;
            currentUnit = this.mutant.get(mainClassStart);
            if (currentUnit.contains("import ")) {
                importStmts.add(currentUnit);
            } else if (currentUnit.contains("package ")) {
                packageStmt = currentUnit;
            }
        } while (!currentUnit.contains("public class"));

        int mainClassEnd = mainClassStart + 1;


        Pattern innerClassPattern = Pattern.compile(" *class *.*");
        while (mainClassEnd < this.mutant.size()) {

            currentUnit = this.mutant.get(mainClassEnd);
            if (innerClassPattern.matcher(currentUnit).matches()) {
                break;
            }
            mainClassEnd++;
        }

        this.mutant = new ArrayList<String>(this.mutant.subList(mainClassStart, mainClassEnd));
        this.mutant.add(0, packageStmt);

        for (String importStmt : importStmts) {
            this.mutant.add(0, importStmt);
        }
    }

    private void addImport(String importStmt) {
        int i = 0;
        for (String unit: this.mutant) {
            if (unit.contains("package")) {
                break;
            }
            i++;
        }
        this.mutant.add(i+1, importStmt);
    }

    private void addUtilityFunctions() {
        String getThresholdMethod = "public double getThreshold() throws IOException {\n" +
            "        String fileName = \"" + Paths.get("target", "test-classes", "mutation.config") + "\";\n" +
            "        Properties prop = new Properties();\n" +
            "        FileInputStream fis = new FileInputStream(fileName);\n" +
            "        prop.load(fis);\n" +
            "        Double d = Double.valueOf(prop.getProperty(\"mutation.threshold\"));\n" +
            "        return d;\n" +
            "    }";

        String getMutantCountMethod = "static double stateSetterCounter;" +
            "        public double getstateSetterCount() {\n" +
            "        try {\n"+
            "        String var5 = \"target/test-classes/mutation.config\";\n" +
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
            "   return -1; }   " +
            "    public void incrementstateSetterCounter() {\n" +
            "        double var1 = stateSetterCounter;\n" +
            "        ++var1;\n" +
            "        stateSetterCounter = var1;\n" +
            "    }";

        String groundTruth = null;


        if (this.jupiter) {
            groundTruth = "@TestMethodOrder(MethodOrderer.MethodName.class)";

            addImport("import org.junit.jupiter.api.MethodOrderer;\n");
            addImport("import org.junit.jupiter.api.TestMethodOrder;\n");
            addImport("import java.io.FileInputStream;\n");

        } else {
            groundTruth = "@FixMethodOrder(MethodSorters.NAME_ASCENDING)\n";

            addImport("import org.junit.runners.MethodSorters;\n");
            addImport("import org.junit.*;\n");
            addImport("import java.io.FileInputStream;\n");
        }


        addImport("import java.util.Properties;");
        addImport("import java.io.IOException;");
        int startIndex = this.findMainClassStart();

        int index = this.findMainClassEnd();
        this.mutant.add(index, getThresholdMethod);
        this.mutant.add(index, getMutantCountMethod);

        if (this.groundTruth) {
            this.mutant.add(startIndex, groundTruth);
        }
    }

    public void export(String operatorName) throws BadClassException, IOException {
        /*this.renameClassToMutant(operatorName);*/
        /*this.renameConstructor();*/



        this.mutantSourceFile = this.createMutantSourceFile(operatorName);

        //this.cropInnerClasses();
        this.exportMutant(this.mutantSourceFile);
    }

    File tempFile;

    protected int findMainClassStart() {
        int start = -1;
        Pattern mainClassPattern = Pattern.compile(" *public * .* *(?:final)? *class .*");

        do {
            start++;
        } while (start < this.mutant.size() - 1 && !mainClassPattern.matcher(this.mutant.get(start)).matches());

        return start;
    }

    protected int findMainClassEnd() {
        int start = -1;
        Pattern mainClassPattern = Pattern.compile(" *public * .* *(?:final)? *class .*");

        do {
            start++;
        } while (start < this.mutant.size() - 1 && !mainClassPattern.matcher(this.mutant.get(start)).matches());

        int end = start + 1;
        int i = start + 1;

        Pattern classPattern = Pattern.compile(" *class *.*");
        Pattern closingBracketPattern = Pattern.compile(".*}.*");

        do {
            i++;
            if (closingBracketPattern.matcher(this.mutant.get(i)).matches()) {
                end = i;
            }
        } while (!classPattern.matcher(this.mutant.get(i)).matches() && i < this.mutant.size() - 1);
        return end;
    }

    public void remove(String mutationOperator) {

        this.mutantSourceFile.delete();
        this.tempFile.delete();
    }

    private String getJars() throws IOException {
        String sourceDir = this.getSourceFile(this.inputDir);
        String workingDir = sourceDir.substring(0, sourceDir.indexOf("src"));
        Process exec = Runtime.getRuntime().exec(new String[]{"mvn", "-f", workingDir, "dependency:build-classpath"});
        BufferedReader reader = new BufferedReader(new InputStreamReader(exec.getInputStream()));
        String line;
        String jars = null;
        while ((line = reader.readLine()) != null) {
            if (line.contains(".jar")) {
                jars = line;
            }
        }
        return jars;
    }

    public int compile(String operatorName) throws Exception {
        List<String> sourceDir = Arrays.asList(this.getSourceFile(this.inputDir).split(File.separator));
        /*classDeclarationUnit.replace(className, className + "_" + operatorName + "_Test");*/

        String[] classNameParsed = this.className.split("\\.");
        String oldClassName = classNameParsed[classNameParsed.length-1];
        String newClassName = oldClassName + "_" + operatorName + "_Test";
        classNameParsed[classNameParsed.length-1] = newClassName;
        StringJoiner stringJoiner = new StringJoiner(File.separator);

        sourceDir = new ArrayList<String>(sourceDir.subList(0, sourceDir.indexOf("src")));

        for (String elem : sourceDir) {
            stringJoiner.add(elem);
        }

        String rootDir = stringJoiner.toString();

        stringJoiner.add("target");
        stringJoiner.add("test-classes");
        String outputDir = stringJoiner.toString();
        stringJoiner = new StringJoiner(File.separator);

        stringJoiner.add(rootDir);
        stringJoiner.add("src");
        stringJoiner.add("test");
        stringJoiner.add("java");
        for (String elem : classNameParsed) {
            stringJoiner.add(elem);
        }

        String classDir = stringJoiner.toString().replace(classNameParsed[classNameParsed.length - 1], classNameParsed[classNameParsed.length - 1] + ".java");
        stringJoiner = new StringJoiner(File.separator);

        String[] outputDirParsed = this.outputDir.split(File.separator);

        for (int i = 0; i < outputDirParsed.length - 1; i++) {
            stringJoiner.add(outputDirParsed[i]);
        }
        stringJoiner.add("classes");

        String classPath = stringJoiner.toString();

        ProcessBuilder processBuilder = new ProcessBuilder("javac", "-cp", classPath + ":" + classPath.replaceAll("classes", "test-classes") + ":" + getJars(), "-d", outputDir, classDir);
        processBuilder.directory(new File(rootDir));
        processBuilder.redirectErrorStream(true);

        Process subprocess = processBuilder.start();

        int nonCompileableMutants = 0;
        Pattern errorPattern = Pattern.compile("([0-9][0-9]*) *error[s]?");
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(subprocess.getInputStream()))) {

            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                System.out.println(line);

                Matcher matcher = errorPattern.matcher(line);

                if (matcher.find()) {
                    nonCompileableMutants = Integer.parseInt(matcher.group(1));
                }
            }
        }

        nonCompileableMutants *= this.mutantCount;
        subprocess.waitFor();
        return nonCompileableMutants;
    }

}
