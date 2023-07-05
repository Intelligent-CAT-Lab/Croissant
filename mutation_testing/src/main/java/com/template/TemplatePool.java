package com.template;

import com.mutation.MutationOperator;
import com.mutation.StringMutationOperator;
import com.mutation.framework.MockitoMutationOperator;
import com.mutation.multithreading.deadlock.DeadLockMutationOperator;
import com.mutation.multithreading.racecondition.RaceConditionMutationOperator;
import com.mutation.staticVariables.ODStaticVariableMutationOperator;
import com.template.pom.DetectionToolAdder;
import com.template.pom.PomModifier;
import soot.G;
import soot.Scene;
import soot.SootClass;
import soot.options.Options;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Ref;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplatePool {
    List<String> templates;
    SootClass testClass;
    List<String> testSuiteContents;
    DetectionToolAdder detectionToolAdder;
    Set<TemplateAdder> templateAdders;
    String projectDir;
    String className;
    String outputDir;
    File testSuiteFile;
    boolean jupiter;

    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_RESET = "\u001B[0m";
    public TemplatePool(SootClass testClass, String projectDir, boolean jupiter, String className, String outputDir, List<String> templates) {
        this.testClass = testClass;
        this.projectDir = projectDir;
        this.className = className;
        this.detectionToolAdder = new DetectionToolAdder(projectDir);
        this.jupiter = jupiter;
        this.outputDir = outputDir;
        this.templates = templates;
        read();
    }

    public void cloneOriginalFile() {
        String classPath = this.className.replaceAll("\\.", "/");
        Path filePath = Paths.get(this.projectDir, classPath);
        String sourcePath = getSourceFile(filePath + ".java");
        try {
            cloneFile(sourcePath.replace(".java", "_original.temp"),sourcePath.replace(".java", "_original2.temp"));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void restoreOriginal() {
        String classPath = this.className.replaceAll("\\.", "/");
        Path filePath = Paths.get(this.projectDir, classPath);
        String sourcePath = getSourceFile(filePath + ".java");
        try {
            cloneFile(sourcePath.replace(".java", "_original2.temp"),sourcePath.replace(".java", "_original.temp"));
            removeFile(sourcePath.replace(".java", "_original2.temp"));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void removeOriginalFile() {
        String classPath = this.className.replaceAll("\\.", "/");
        Path filePath = Paths.get(this.projectDir, classPath);
        String sourcePath = getSourceFile(filePath + ".java");
        try {
            removeFile(sourcePath.replace(".java", "_original.temp"));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public String getSourceFile(String classFilePath) {
        //TODO: implement with java path
        return classFilePath.replace("target/test-classes", "src/test/java");
    }


    public void read() {
        this.testSuiteContents = new ArrayList<>();
        String classPath = this.className.replaceAll("\\.", "/");
        Path filePath = Paths.get(this.projectDir, classPath);
        this.testSuiteFile = new File(getSourceFile(filePath + ".java"));
        Scanner s = null;
        try {
            s = new Scanner(new FileReader(this.testSuiteFile));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        while (s.hasNext()) {
            this.testSuiteContents.add(s.nextLine());
        }
    }


    public void copyJavaFileToTemp() {
        String classPath = this.className.replaceAll("\\.", "/");
        Path filePath = Paths.get(this.projectDir, classPath);
        String sourcePath = getSourceFile(filePath + ".java");
        try {
            cloneFile(sourcePath, sourcePath.replace(".java", ".temp"));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void copyJavaFileToOriginal() {
        String classPath = this.className.replaceAll("\\.", "/");
        Path filePath = Paths.get(this.projectDir, classPath);
        String sourcePath = getSourceFile(filePath + ".java");
        try {
            cloneFile(sourcePath, sourcePath.replace(".java", "_original.temp"));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void copyTempToJava() {
        String classPath = this.className.replaceAll("\\.", "/");
        Path filePath = Paths.get(this.projectDir, classPath);
        String sourcePath = getSourceFile(filePath + ".java");
        try {
            cloneFile(sourcePath.replace(".java", ".temp"), sourcePath);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void copyOriginalToJavaFile() {
        String classPath = this.className.replaceAll("\\.", "/");
        Path filePath = Paths.get(this.projectDir, classPath);
        String sourcePath = getSourceFile(filePath + ".java");
        try {
            cloneFile(sourcePath.replace(".java", "_original.temp"), sourcePath);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void exportTestSuite()  {
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
            new FileOutputStream(this.testSuiteFile), "utf-8"))) {
            for (String unit: this.testSuiteContents) {
                writer.write(unit + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addTemplates() {
        this.detectionToolAdder.addDetectionTools();
        this.templateAdders = new LinkedHashSet<TemplateAdder> (Arrays.asList(
	    new DeadLockTemplate(this.testSuiteContents, jupiter, this.projectDir),
	    new ReflectionTemplate(this.testSuiteContents, jupiter, this.projectDir),
            new LatchTemplate(this.testSuiteContents, jupiter, this.projectDir),
            new ServerTemplate(this.testSuiteContents, jupiter, this.projectDir),						       
            new TimezoneTemplate(this.testSuiteContents, jupiter, this.projectDir),
            new CacheTemplate(this.testSuiteContents, jupiter, this.projectDir),
            new TimeoutTemplate(this.testSuiteContents, jupiter, this.projectDir),
            new ThreadSleepTemplate(this.testSuiteContents, jupiter, this.projectDir),
            new MockitoTemplate(this.testSuiteContents, jupiter, this.projectDir),
            new FileWriterTemplate(this.testSuiteContents, jupiter, this.projectDir),
            new InstanceTemplate(this.testSuiteContents, jupiter, this.projectDir),
            new FileTemplate(this.testSuiteContents, jupiter, this.projectDir),
            new StaticTemplate(this.testSuiteContents, jupiter, this.projectDir),
            new DatabaseTemplate(this.testSuiteContents, jupiter, this.projectDir)
        ));

        if (this.templates.size() != 0) {
            Set<TemplateAdder> set = new LinkedHashSet<>();

            for (TemplateAdder templateAdder: this.templateAdders) {
                String name = templateAdder.getClass().getSimpleName();
                if (templates.contains(name)) {
                    set.add(templateAdder);
                }
            }
            this.templateAdders = set;
        }



        copyJavaFileToTemp();
        copyJavaFileToOriginal();


        for (TemplateAdder templateAdder: templateAdders) {
            read();
            templateAdder.run();
            this.testSuiteContents = templateAdder.returnClass();
            exportTestSuite();

            int compilationResult = compile();

            if (compilationResult == 0) {
                System.out.println(ANSI_GREEN+"[INFO] "+ ANSI_RESET+ "Mutation Template "+templateAdder.getClass().getName()+" Compilation Success");
                copyJavaFileToTemp();
            } else {
                copyTempToJava();
            }
        }
//        int compilationResult = compile();
//
//        if (compilationResult == 0) {
//            System.out.println(ANSI_GREEN+"[INFO] "+ ANSI_RESET+ "Mutation Templates Compilation Success");
//
//            //System.out.println(ANSI_GREEN+"[INFO] "+ ANSI_RESET+ "Mutation Template "+templateAdder.getClass().getName()+" Compilation Success");
//            copyJavaFileToTemp();
//        } else {
//            copyTempToJava();
//        }

/*        copyOriginalToJavaFile();
        removeTempFiles();*/

        copyTempToJava();
    }

    public void removeTempFiles() {
        String classPath = this.className.replaceAll("\\.", "/");
        Path filePath = Paths.get(this.projectDir, classPath);
        String sourcePath = getSourceFile(filePath + ".java");
        try {
            /*removeFile(sourcePath.replace(".java", "_original.temp"));*/
            removeFile(sourcePath.replace(".java", ".temp"));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public int compile() {
        List<String> sourceDir = Arrays.asList(this.getSourceFile(this.projectDir).split(File.separator));
        String[] classNameParsed = this.className.split("\\.");
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

        ProcessBuilder processBuilder = null;
        try {
            processBuilder = new ProcessBuilder("javac", "-cp", classPath + ":" + classPath.replaceAll("classes", "test-classes") + ":" + getJars(), "-d", outputDir, classDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        processBuilder.directory(new File(rootDir));
        processBuilder.redirectErrorStream(true);

        Process subprocess = null;
        try {
            subprocess = processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(subprocess.getInputStream()))) {

            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                System.out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            subprocess.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return subprocess.exitValue();
    }

    private String getJars() throws IOException {
        String sourceDir = this.getSourceFile(this.projectDir);
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

    private void removeFile(String fileName) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("rm", fileName);

        processBuilder.redirectErrorStream(true);

        Process subprocess = processBuilder.start();
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(subprocess.getInputStream()))) {

            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                System.out.println(line);
            }
        }
        subprocess.waitFor();
    }


    public void cloneFile(String fileName, String copiedFileName) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("cp", fileName, copiedFileName);

        processBuilder.redirectErrorStream(true);

        Process subprocess = processBuilder.start();
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(subprocess.getInputStream()))) {

            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                System.out.println(line);
            }
        }
        subprocess.waitFor();
    }


}
