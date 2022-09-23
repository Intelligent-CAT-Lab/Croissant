package com.framework;

import com.mutation.*;
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
import com.mutation.reflection.UnorderedPopulationMutationOperator;
import com.mutation.resourceBoundViolation.memoryBoundViolationMO;
import com.mutation.resourceNotCreated.newFileNullMO;
import com.mutation.resourceNotCreated.newFileNullODMO;
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
import com.template.TemplatePool;
import soot.*;
import soot.jimple.toolkits.typing.fast.BottomType;
import soot.options.Options;
import soot.tagkit.*;
import soot.util.Chain;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Counter {
    Integer count;

    public Counter() {
        this.count = 0;
    }

    public void increment() {
        this.count++;
    }

    public Integer get() {
        return this.count;
    }

    public void reset() {
        this.count = 0;
    }
}

public class OperatorSelector {

    private HashMap<String, Integer> mutantCounter;
    private Set<MutationOperator> mutationOperators;
    private SootClass testSuite;
    private final String outputDir;
    private final boolean groundTruth;
    private final String inputDir;
    private int previousFaultyTests;
    private final String className;
    private final Boolean sootOnly;
    private final Boolean compileBefore;
    private final Oracle oracle;
    private final Boolean doNotRunTests;
    private int brokenSootMutants;
    private Integer mutantNumber;
    private final boolean verbose;
    private final List<MutationOperator> forbidden;
    private final boolean jupiter;
    private final List<String> allowedOperators;
    private final List<String> templates;
    // private static final Logger logger =
    // LoggerFactory.getLogger("com.framework.OperatorSelector");

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";

    public OperatorSelector(String dir, String testSuiteName, String outputDir, Boolean sootOnly, Boolean compileBefore,
            Boolean doNotRunTests, boolean verbose, boolean jupiter, boolean groundTruth, List<String> allowedOperators,
            List<String> templates) {

        this.inputDir = dir;
        this.className = testSuiteName;
        this.outputDir = outputDir;
        this.sootOnly = sootOnly;
        this.oracle = new Oracle(dir);
        this.compileBefore = compileBefore;
        this.doNotRunTests = doNotRunTests;
        this.mutantNumber = 0;
        this.brokenSootMutants = 0;
        this.verbose = verbose;
        this.forbidden = new ArrayList<>();
        this.jupiter = jupiter;
        this.previousFaultyTests = 0;
        this.groundTruth = groundTruth;
        this.allowedOperators = allowedOperators;
        this.templates = templates;

        mutationOperators = new HashSet<MutationOperator>(Arrays.asList(
                new SetUCIMO(), // UCIMO
                new MapIUCMO(), // UCIMO
                new MapIVMO(), // IVMO
                new PathFSFMO(), // FPMO
                new CustomClassIVMO(), // IVMO
                new TimeValueInitializationMutationOperator(), // TVIMO
                new HashMapUAMO(), // UAMOccff
                new PrimitiveIVMO(), // IVMO
                new ListIVMO(), // IVMO
                new HashSetUAMO(), // UAMO
                new SetIUCMO(), // IUCMO
                new memoryBoundViolationMO(), // RBVMO
                new LatchObjectMO(), // AWMO
                new ThreadSleepMO(), // AWMO
                new VoidLatchAwaitMO(), // AWMO
                new CaffeineCDMO(), // CMO
                new FileOutputStreamFSFMO(), // FPMO
                new RandomAccessFileFSFMO(), // FPMO
                new StringFileWriterFSFMO(), // FPMO
                new TempFileFSFMO(), // FPMO
                new jettyServerSetNonHardcodedPortMO(), // HPMO
                new SetIVMO(), // IVMO
                new WrapperIVMO(), // IVMO
                new GlobalTMO(), // TMO
                /* new newFileNullMO(), //RNCMO */
                new LocalTMO(), // TMO
                new jettyServerSetHardcodedPortMO(), // HPMO
                new TRRInjectAssertLocal(), // TRRMO
                new UnorderedPopulationMutationOperator(), // UPMO
                new TimeZoneDependencyMO(), // TZDMO
                new WIPTRRInjectAssertInstance(), // TRRMO
                new JsonUAMO(), // UAMO
                new JsonUCIMO(), // UCIMO
                new JsonIUCMO(), // UCIMO
                new HashMapStringMO(), // SMO
                new HashSetStringMO(), // SMO
                new JsonStringMO(), // SMO
                new DatabaseMutationOperator(),
                new FileObjectFMO(), // FPMO
                new MockitoMutationOperator(), // FWPMO
                new newFileNullODMO(), // RNCMO
                new memoryBoundViolationMO()));

        // HashMap<String, Integer> mutantCounter = new HashMap<String,Integer>();

        if (allowedOperators.size() != 0) {
            Set<MutationOperator> operators = new HashSet<>();

            for (MutationOperator mutationOperator : this.mutationOperators) {
                String name = mutationOperator.getClass().getSimpleName();
                if (allowedOperators.contains(name)) {
                    operators.add(mutationOperator);
                    // mutantcounter.put(mutationOperator.getClass().getSimpleName(), 0);

                }
            }
            // this.mutantCounter = mutantcounter;
            this.mutationOperators = operators;
            // System.out.println(mutantCounter);
        }
    }

    // private Logger logger =
    // LoggerFactory.getLogger("com.framework.OperatorSelector");

    /**
     * Setup sets necessary settings, loads necessary classes, loads the class which
     * will be mutated.
     * Then it invokes retrieveActiveAllMethods to avoid linker errors
     * It also adds basic class java.lang.System for insertion methods such as
     * System.out.println
     * 
     * @param inputDir  The directory in which the class file exits (the one that
     *                  will be mutated)
     * @param className Name of the class that will be mutated
     * @return SootClass object that has been prepared to mutate
     */
    private SootClass setUp(String inputDir, String className) {
        // setting options
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
        Scene.v().addBasicClass("java.lang.System", SootClass.SIGNATURES);

        // loading soot class from scene
        SootClass mainClass = Scene.v().getSootClass(className);

        retrieveActiveAllMethods(mainClass);

        return mainClass;
    }

    /**
     * When loading a class, soot only retrieves the skeleton (method signatures
     * etc.) of the class.
     * Tying to execute a class mutated by soot throws a linker error because
     * methods are not implemented.
     * This method retrieves active bodies of all methods the class that is being
     * loaded
     * 
     * @param mainClass the mainClass loaded by the setUp method
     */
    private static void retrieveActiveAllMethods(SootClass mainClass) {
        for (SootMethod sm : mainClass.getMethods()) {
            if (!sm.isAbstract()) {
                sm.setActiveBody(sm.retrieveActiveBody());
            }
        }
    }

    private boolean validateMutant(SootMutationOperator mutationOperator) {
        return checkBottomType(mutationOperator);
    }

    private boolean checkBottomType(SootMutationOperator mutationOperator) {
        List<SootMethod> methods = mutationOperator.getCurrentClass().getMethods();
        for (SootMethod method : methods) {
            Chain<Local> locals = method.getActiveBody().getLocals();
            for (Local local : locals) {
                if (local.getType().toString().equals(BottomType.v().toString())) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean isTest(SootMethod method) {

        if (method.getParameterCount() > 0 || !method.getReturnType().toString().equals(VoidType.v().toString())
                || method.isStatic() || method.getActiveBody().getTraps().size() > 0) {
            return false;
        }

        String testName = method.getName().toLowerCase();
        Pattern[] testNamePatterns = new Pattern[] {
                Pattern.compile("test.*"),
                Pattern.compile(".*test")
        };

        for (Pattern testNamePattern : testNamePatterns) {
            Matcher testNamePatternMatcher = testNamePattern.matcher(testName);
            if (testNamePatternMatcher.matches()) {
                return true;
            }
        }
        return false;
    }

    public void printErrorMessage(MutationOperator mutationOperator) throws IOException, InterruptedException {
        System.out.println("****************** ERROR *****************");
        System.out.println("An error occured with mutation operator: " + mutationOperator);
        compileTest();
    }

    public void printApplicableMessage(MutationOperator mutationOperator, String signifier) {
        // System.out.println(ANSI_GREEN + "Applicable Operator " + mutationOperator + "
        // is applicable for " + signifier + " for " +
        // mutationOperator.getMutantNumber() + " test(s)"+ ANSI_RESET);
    }

    public void generateNameAndExport(MutationOperator mutationOperator, StringBuilder mutantNameBuilder,
            Counter counter, HashMap<String, Integer> mutantCounter) throws IOException {

        if (mutationOperator instanceof SootMutationOperator) {
            SootMutationOperator mo = (SootMutationOperator) mutationOperator;
            if (this.groundTruth) {
                mo.addGroundTruthAnnotation();
            }
        }

        // HashMap<String, Integer> mutantCounter = new HashMap<String,Integer>();
        counter.increment();
        mutantNameBuilder.append(mutationOperator.getCurrentMethodName());
        mutantNameBuilder.append("Mutant");
        mutantNameBuilder.append("__");
        mutantNameBuilder.append(mutationOperator.getClass().getSimpleName());
        mutantNameBuilder.append("__");
        mutantNameBuilder.append(counter.get());
        String name = mutantNameBuilder.toString();

        if (mutantCounter.get(mutationOperator.getClass().getSimpleName()) == null) {

            mutantCounter.put(mutationOperator.getClass().getSimpleName(), 1);
        } else {
            mutantCounter.put(mutationOperator.getClass().getSimpleName(),
                    mutantCounter.get(mutationOperator.getClass().getSimpleName()) + 1);
        }

        // System.out.println(mutantCounter);
        mutationOperator.exportClass(this.outputDir, name);
        System.out.println(ANSI_GREEN + "[INFO] A New Mutant: " + ANSI_RESET + mutantNameBuilder);
        mutantNameBuilder.setLength(0);
    }

    public void mutate(MutationOperator mutationOperator, StringBuilder mutantNameBuilder, Counter counter,
            String signifier, HashMap<String, Integer> mutantCounter) throws Exception {

        // HashMap<String, Integer> mutantCounter = new HashMap<String,Integer>();
        if (mutationOperator.isApplicable()) {
            // System.out.println("OperatorSelector[DEBUG] this.verbose" +this.verbose );
            if (this.verbose) {
                this.mutantNumber = 0;
            }
            this.mutantNumber += mutationOperator.getMutantNumber();
            printApplicableMessage(mutationOperator, signifier);
            if (this.verbose) {
                mutationOperator.mutateMethod();
                // System.out.println("OperatorSelector[DEBUG] this.mutantNumber"
                // +this.mutantNumber );
                this.oracle.getTestResults(this.mutantNumber);
                this.oracle.compileTests();
            } else {
                // System.out.println("OperatorSelector[DEBUG] this.mutantNumber in else branch"
                // +this.mutantNumber );
                mutationOperator.mutateMethod();
            }

            if (mutationOperator instanceof SootMutationOperator) {
                if (!validateMutant((SootMutationOperator) mutationOperator)) {
                    this.brokenSootMutants++;
                    return;
                }
            }

            generateNameAndExport(mutationOperator, mutantNameBuilder, counter, mutantCounter);
        }
    }

    public void mutateStandard(MutationOperator mutationOperator, SootMethod test, StringBuilder mutantNameBuilder,
            Counter counter, HashMap<String, Integer> mutantCounter) throws Exception {
        mutationOperator.setCurrentMethod(test.getSubSignature());
        mutate(mutationOperator, mutantNameBuilder, counter, "test " + test.getSubSignature(), mutantCounter);
    }

    public void mutateInjector(MutationOperator mutationOperator, StringBuilder mutantNameBuilder, Counter counter,
            HashMap<String, Integer> mutantCounter) throws Exception {
        mutate(mutationOperator, mutantNameBuilder, counter, "class " + mutationOperator.getClass().getName(),
                mutantCounter);
    }

    public void setCurrentClass(MutationOperator mutationOperator) {
        this.oracle.setClassName(this.className);
        if (mutationOperator instanceof SootMutationOperator) {
            mutationOperator.setCurrentClass(this.inputDir, this.className);
        } else if (mutationOperator instanceof StringMutationOperator) {
            // TODO: use java path
            String srcInputDir = this.inputDir.replace("target/test-classes", "src/test/java");
            mutationOperator.setCurrentClass(srcInputDir, this.className);
        }
    }

    public boolean isFaulty(List<String> faultyMutations, String mutationOperatorName) {
        for (String faultyMutation : faultyMutations) {
            if (mutationOperatorName.contains(faultyMutation)) {
                return true;
            }
        }
        return false;
    }

    public void start(double threshold) throws Exception {
        start(threshold, 0);
    }

    private int compileTest() throws IOException, InterruptedException {

        int result1 = replaceFile(this.inputDir, this.className);
        int result2 = this.duplicateFile(this.inputDir, this.className);

        return result1 - result2;
    }

    private int duplicateFile(String root, String fileName) throws IOException, InterruptedException {
        fileName = fileName.replace('.', '/');
        ProcessBuilder processBuilder = new ProcessBuilder("cp", fileName + ".class", fileName + ".temp");
        processBuilder.directory(new File(root));
        processBuilder.redirectErrorStream(true);

        Process subprocess = processBuilder.start();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(subprocess.getInputStream()))) {

            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                System.out.println(line);
            }
        }
        subprocess.waitFor();
        return subprocess.exitValue();
    }

    private int removeTempFile(String root, String fileName) {
        fileName = fileName.replace('.', '/');
        ProcessBuilder processBuilder = new ProcessBuilder("rm", fileName + ".temp");
        processBuilder.directory(new File(root));
        processBuilder.redirectErrorStream(true);

        Process subprocess = null;
        try {
            subprocess = processBuilder.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(subprocess.getInputStream()))) {

                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    System.out.println(line);
                }
            }
            subprocess.waitFor();
        } catch (IOException e) {
            System.out.println("could not remove the temp file");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return subprocess.exitValue();
    }

    private static int replaceFile(String root, String fileName) throws IOException, InterruptedException {

        fileName = fileName.replace('.', '/');
        ProcessBuilder processBuilder = new ProcessBuilder("mv", fileName + ".temp", fileName + ".class");
        processBuilder.directory(new File(root));
        processBuilder.redirectErrorStream(true);

        Process subprocess = processBuilder.start();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(subprocess.getInputStream()))) {

            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                System.out.println(line);
            }
        }
        subprocess.waitFor();
        return subprocess.exitValue();
    }

    private String getJars() throws IOException {
        String sourceDir = this.inputDir;
        String workingDir = sourceDir.substring(0, sourceDir.indexOf("target"));
        Process exec = Runtime.getRuntime()
                .exec(new String[] { "mvn", "-f", workingDir, "dependency:build-classpath" });
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

    public void start(double threshold, int tries) throws Exception {

        HashMap<String, Integer> mutantCounter = new HashMap<String, Integer>();

        TemplatePool templatePool = new TemplatePool(this.testSuite, this.inputDir, this.jupiter, this.className,
                this.outputDir, this.templates);
        templatePool.addTemplates();
        templatePool.cloneOriginalFile();
        this.testSuite = setUp(this.inputDir, this.className);

        createConfigFile(threshold);

        long startTime;
        long endTime;

        duplicateFile(this.inputDir, this.className.replaceAll("\\.", "/"));

        if (compileBefore) {
            oracle.compileTests();
        }

        StringBuilder mutantNameBuilder = new StringBuilder();
        Counter counter = new Counter();
        startTime = System.currentTimeMillis();

        this.oracle.setSootOnly(sootOnly);
        if (this.testSuite.isAbstract()) {
            return;
        }

        int nonCompileableStringMutants = 0;

        if (sootOnly) {
            System.out.println("skipping string mutation operators");
        } else {
            try {
                StringMutationPool pool = new StringMutationPool(this.inputDir, this.className, this.outputDir,
                        this.testSuite, this.jupiter, this.groundTruth, this.allowedOperators);
                pool.setThreshold(threshold);
                this.mutantNumber += pool.start();

                templatePool.restoreOriginal();
                templatePool.copyOriginalToJavaFile();
                templatePool.removeTempFiles();
                templatePool.removeOriginalFile();
                /*
                 * pool.export();
                 * nonCompileableStringMutants = pool.compile();
                 */
                /* pool.remove(); */
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                System.out.println("corresponding test source file for: " + this.className + " could not be found");
                System.out.println("make sure " + this.className + " is a legitimate test suite");
                System.out.println("Failures: 0");
                System.out.println("Errors: 0");
                System.out.println("Mutant number: 0");
                return;
            }
        }

        if (verifyByteCode(this.testSuite)) {
            System.out.println(ANSI_GREEN + "[INFO] " + ANSI_RESET + "Bytecode Valid");
        } else {
            System.out.println("byte code is invalid");
            return;
        }

        for (MutationOperator mutationOperator : this.mutationOperators) {

            mutationOperator.setJupiter(this.jupiter);

            /*
             * System.out.println(i);
             * System.out.println(mutationOperator.getClass().getSimpleName());
             * System.out.println();
             */
            /*
             * if (this.oracle.getFatalError() && (mutationOperator instanceof
             * IterationUnorderedCollectionMutationOperator || mutationOperator instanceof
             * CustomClassIVMO)) {
             * continue;
             * }
             */

            mutationOperator.setThreshold(threshold);
            try {
                setCurrentClass(mutationOperator);
            } catch (Exception e) {
                e.printStackTrace();
                File yourFile = new File("errors.txt");
                yourFile.createNewFile(); // if file already exists will do nothing
                FileOutputStream oFile = new FileOutputStream(yourFile, true);
                oFile.write(e.getMessage().getBytes(StandardCharsets.UTF_8));
                oFile.write("\n".getBytes(StandardCharsets.UTF_8));
                oFile.flush();
                oFile.close();
                previousFaultyTests = this.oracle.faultyTestToOperator.size();
                tries++;
                compileTest();
                start(threshold, tries);
                return;
            }

            mutationOperator.runOnceBefore();

            /* if (isFaulty(faultyMutations, mutationOperator.toString())) { */
            /* continue; */
            /* } */

            if (mutationOperator instanceof SootTestInjector || mutationOperator instanceof StringInjector) {
                /* try { */
                AbstractMap.SimpleEntry<String, String> query = new AbstractMap.SimpleEntry<>("all",
                        mutationOperator.getClass().getSimpleName());
                if (isContained(query)) {

                    System.out.println("defective mutation is removed: " + mutationOperator.getClass().getSimpleName());
                    continue;
                }
                try {
                    mutateInjector(mutationOperator, mutantNameBuilder, counter, mutantCounter);
                } catch (Exception e) {
                    e.printStackTrace();
                    File yourFile = new File("errors.txt");
                    yourFile.createNewFile(); // if file already exists will do nothing
                    FileOutputStream oFile = new FileOutputStream(yourFile, true);
                    oFile.write(e.getMessage().getBytes(StandardCharsets.UTF_8));
                    oFile.write("\n".getBytes(StandardCharsets.UTF_8));
                    oFile.flush();
                    oFile.close();
                    previousFaultyTests = this.oracle.faultyTestToOperator.size();
                    tries++;
                    compileTest();
                    start(threshold, tries);
                    return;
                }
                continue;
                /*
                 * } finally {
                 * continue;
                 * }
                 */
            }

            for (SootMethod test : this.testSuite.getMethods()) {
                if (!isTest(test) || test.getName().toLowerCase().contains("mutant")) {
                    continue;
                }

                AbstractMap.SimpleEntry<String, String> query = new AbstractMap.SimpleEntry<>(test.getName(),
                        mutationOperator.getClass().getSimpleName());

                if (isContained(query)) {

                    System.out.println("defective mutation is removed: " + test.getName() + "-"
                            + mutationOperator.getClass().getSimpleName());
                    continue;
                }

                try {
                    mutateStandard(mutationOperator, test, mutantNameBuilder, counter, mutantCounter);
                } catch (Exception e) {
                    e.printStackTrace();
                    File yourFile = new File("errors.txt");
                    yourFile.createNewFile(); // if file already exists will do nothing
                    FileOutputStream oFile = new FileOutputStream(yourFile, true);
                    oFile.write(e.getMessage().getBytes(StandardCharsets.UTF_8));
                    oFile.write("\n".getBytes(StandardCharsets.UTF_8));
                    oFile.flush();
                    oFile.close();
                    previousFaultyTests = this.oracle.faultyTestToOperator.size();
                    tries++;
                    compileTest();
                    start(threshold, tries);
                    return;
                }
            }

            // System.out.println("OperatorSelector[DEBUG] Final " +this.mutantNumber );

            mutationOperator.runOnceAfter();
            mutationOperator.setCurrentClass(null);
            this.testSuite = setUp(this.inputDir, this.className);
        }
        System.out.println(ANSI_GREEN + "[INFO] Number of each Mutant Operator: " + ANSI_RESET + mutantCounter);
        System.out.println(ANSI_GREEN + "[INFO] Total Number of Mutants generated: " + ANSI_RESET + counter.get());
        // logger.info("OperatorSelector[DEBUG] Final ,{}", this.mutantNumber);
        endTime = System.currentTimeMillis();
        String executionTimeLogBuilder = "Mutation for " + this.inputDir + " Execution took time: " +
                (endTime - startTime) +
                " miliseconds";
        System.out.println(ANSI_GREEN + "[INFO] Total Time for Mutation: " + ANSI_RESET + executionTimeLogBuilder);
        if (!this.doNotRunTests) {

            // this.oracle.getTestResults(0);

            if (this.oracle.faultyTestToOperator.size() != previousFaultyTests) {
                try {
                    previousFaultyTests = this.oracle.faultyTestToOperator.size();
                    tries++;
                    compileTest();
                    start(threshold, tries);
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                    compileTest();
                    return;
                }
            }
        }

        removeTempFile(this.inputDir, this.className);
    }

    public void createConfigFile(double threshold) {
        try {
            File configFile = new File(String.valueOf(Paths.get(this.outputDir, "mutation.config")));
            if (configFile.createNewFile()) {
                BufferedWriter writer = new BufferedWriter(new FileWriter(configFile));
                writer.write("mutation.threshold=" + threshold);
                writer.write("\nmutation.count=" + 5);
                writer.close();
                System.out.println(ANSI_GREEN + "[INFO] " + ANSI_RESET + "Config File Created");
            } else {
                System.out.println(ANSI_GREEN + "[INFO] " + ANSI_RESET + "Config File Exists.");
            }
        } catch (IOException e) {
            System.out.println("An error occurred while creating the config file");
            e.printStackTrace();
        }
    }

    private boolean isContained(AbstractMap.SimpleEntry<String, String> query) {
        /*
         * System.out.println("query key: "+query.getKey());
         * System.out.println("query value: "+query.getValue());
         */
        for (Map.Entry<String, String> entry : this.oracle.faultyTestToOperator) {
            /*
             * System.out.println("entry key: "+query.getKey());
             * System.out.println("entry value: "+query.getValue());
             */
            if (entry.getKey().contains(query.getKey()) && entry.getValue().contains(query.getValue())) {

                return true;
            }
        }
        return false;
    }

    private void checkMutant(String operatorName) {
        if (!this.doNotRunTests) {
            Runner runner = new Runner(this.inputDir);
            runner.setOperatorName(operatorName);
            String fileName = SourceLocator.v().getFileNameFor(this.testSuite, Options.output_format_class);
            runner.setFileName(fileName);
            runner.setDirectory(this.outputDir);

            try {
                runner.runTest();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean verifyByteCode(SootClass testSuite) throws IOException, InterruptedException {
        return true;
        /*
         * SootClass testClass = setUp(this.inputDir, this.className);
         * SootMutationOperator sootMutationOperator = new SootTest();
         * sootMutationOperator.setCurrentClass(testClass);
         * 
         * sootMutationOperator.exportClass(this.outputDir);
         * 
         * 
         * Runner runner = new Runner(this.outputDir);
         * runner.setOperatorName(sootMutationOperator.getClass().getSimpleName());
         * String fileName = SourceLocator.v().getFileNameFor(this.testSuite,
         * Options.output_format_class);
         * runner.setFileName(fileName);
         * runner.setDirectory(this.outputDir);
         * 
         * boolean result = runner.verifyBytecode();
         * 
         * File fileObject = new File(SourceLocator.v().getFileNameFor(testClass,
         * Options.output_format_class));
         * fileObject.delete();
         * 
         * return result;
         */

        /* return subprocess.exitValue(); */

    }

}
