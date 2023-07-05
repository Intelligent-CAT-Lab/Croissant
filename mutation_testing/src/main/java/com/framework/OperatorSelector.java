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
import com.mutation.resourceNotCreated.newFileNullODMO;
import com.mutation.timeZoneDependency.TimeZoneDependencyMO;
import com.mutation.tooRestrictiveRange.TRRInjectAssertLocal;
import com.mutation.tooRestrictiveRange.WIPTRRInjectAssertInstance;
import com.mutation.unorderedCollections.index.UnorderedCollectionIndexMutationOperator;
import com.mutation.unorderedCollections.orderedStringConversion.OrderedStringConversionMutationOperator;
import com.template.TemplatePool;
import soot.*;
import soot.baf.BafASMBackend;
import soot.jimple.toolkits.typing.fast.BottomType;
import soot.options.Options;
import soot.tagkit.AnnotationTag;
import soot.tagkit.VisibilityAnnotationTag;
import soot.util.Chain;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static soot.SootClass.SIGNATURES;


class Counter {
    Integer count;
    public Counter() { this.count = 0;}
    public void increment() { this.count++; }

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
    //private static final Logger logger = LoggerFactory.getLogger("com.framework.OperatorSelector");
    Bundle bundle = new Bundle();

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String[] SET_OD_VALUES = new String[] { "IVD", "SVD", "TPFD", "CSD","DSD","FPD","CCUD","RA"};
    public static final Set<String> OD_SET = new HashSet<>(Arrays.asList(SET_OD_VALUES));
    public static final LinkedHashMap<String,Long> MUTANT_TIME = new LinkedHashMap<>();

    public OperatorSelector(String dir, String testSuiteName, String outputDir, Boolean sootOnly, Boolean compileBefore, Boolean doNotRunTests, boolean verbose, boolean jupiter, boolean groundTruth, List<String> allowedOperators, List<String> templates) {

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

        mutationOperators = new LinkedHashSet<MutationOperator> (Arrays.asList(

            //NOD
            new TimeValueInitializationMutationOperator(), //TVIMO
            new memoryBoundViolationMO(), //RBVMO
            new LatchObjectMO(), //AWMO
            new ThreadSleepMO(), //AWMO
            new VoidLatchAwaitMO(), //AWMO
            new jettyServerSetNonHardcodedPortMO(), //HPMO
            new jettyServerSetHardcodedPortMO(), //HPMO
            new TRRInjectAssertLocal(), //TRRMO
            new TimeZoneDependencyMO(), //TZDMO
            new WIPTRRInjectAssertInstance(), //TRRMO
            new UnorderedPopulationMutationOperator(), //UPMO //RAM
            new UnorderedCollectionIndexMutationOperator(), //UCIA
            new OrderedStringConversionMutationOperator(), //UCC

            //OD

            new CustomClassIVMO(), //IVD,OD
            new CaffeineCDMO(), //CSD,OD
            new DatabaseMutationOperator(), //DSD,OD
            new FileObjectFMO(), //FPD,OD
            new MockitoMutationOperator(), //TPFD,OD
            new newFileNullODMO() //RA,OD

        ));


        if (allowedOperators.size() != 0) {
            Set<MutationOperator> operators = new LinkedHashSet<>();

            for (MutationOperator mutationOperator : this.mutationOperators) {
                String name = mutationOperator.getClass().getSimpleName();
                if (allowedOperators.contains(name)) {
                    operators.add(mutationOperator);
                }
            }
            this.mutationOperators = operators;
        }
    }


    /**
     * Setup sets necessary settings, loads necessary classes, loads the class which will be mutated.
     * Then it invokes retrieveActiveAllMethods to avoid linker errors
     * It also adds basic class java.lang.System for insertion methods such as System.out.println
     * @param inputDir The directory in which the class file exits (the one that will be mutated)
     * @param className Name of the class that will be mutated
     * @return SootClass object that has been prepared to mutate
     */
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
        Scene.v().addBasicClass("java.lang.System", SIGNATURES);

        //loading soot class from scene
        SootClass mainClass = Scene.v().getSootClass(className);

        retrieveActiveAllMethods(mainClass);

        return mainClass;
    }

    /**
     * When loading a class, soot only retrieves the skeleton (method signatures etc.) of the class.
     * Tying to execute a class mutated by soot throws a linker error because methods are not implemented.
     * This method retrieves active bodies of all methods the class that is being loaded
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
        VisibilityAnnotationTag tag = (VisibilityAnnotationTag) method.getTag("VisibilityAnnotationTag");
        if (tag != null) {
            for (AnnotationTag annotation : tag.getAnnotations()) {
                if ((method.getParameterCount() == 0)&&(annotation.getType().equals("Lorg/junit/jupiter/api/Test;") ||annotation.getType().contains("Test") )){
                    return true;
                }
            }
        }

        if (method.getParameterCount() > 0 || !method.getReturnType().toString().equals(VoidType.v().toString()) || method.isStatic() || method.getActiveBody().getTraps().size() > 0) {
            return false;
        }

        String testName = method.getName().toLowerCase();
        Pattern[] testNamePatterns = new Pattern[]{
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
	        System.out.println(mutationOperator + " is applicable for " + signifier + " for " + mutationOperator.getMutantNumber() + " test(s)");
    }

    public String generateNameAndExport(MutationOperator mutationOperator, StringBuilder mutantNameBuilder, Counter counter, HashMap<String, Integer> mutantCounter) throws IOException {

        if (mutationOperator instanceof SootMutationOperator) {
            SootMutationOperator mo = (SootMutationOperator) mutationOperator;
            if (this.groundTruth) {
                mo.addGroundTruthAnnotation();
            }
        }

	//	 HashMap<String, Integer> mutantCounter = new HashMap<String,Integer>();

        //counter.increment();
        if (counter.get().equals(2)){
            System.out.println(this.outputDir);
        }
        mutantNameBuilder.append("CroissantMutant");
        //mutantNameBuilder.append(counter.get());
        mutantNameBuilder.append("_");
        String countName = bundle.getKey(mutationOperator.getClass().getSimpleName());
        if (OD_SET.contains(countName))
            mutantNameBuilder.append("OD_");
        else mutantNameBuilder.append("NOD_");
        mutantNameBuilder.append(countName);
        mutantNameBuilder.append("_");
        mutantNameBuilder.append(mutationOperator.getCurrentMethodName());
        mutantNameBuilder.append("_");
        mutantNameBuilder.append(mutationOperator.getClass().getSimpleName());
        mutantNameBuilder.append("_");
        String template = bundle.getTemplate(mutationOperator.getClass().getSimpleName());
        if (template=="") mutantNameBuilder.append("NoTemplate");
        else  mutantNameBuilder.append(template.toString().replace(',','_'));
        String name = mutantNameBuilder.toString();

       //String countName = bundle.getKey(mutationOperator.getClass().getSimpleName());
        /*
	if (mutantCounter.get(countName) == null){
	    mutantCounter.put(countName,1);
	}
	else{
	    mutantCounter.put(countName,mutantCounter.get(countName)+1);
	}*/
        mutationOperator.exportClass(this.outputDir, name);
        //System.out.println(ANSI_GREEN + "[INFO] A New Mutant: " + ANSI_RESET+mutantNameBuilder);
        mutantNameBuilder.setLength(0);
        return name;

    }

    public void mutate(MutationOperator mutationOperator, StringBuilder mutantNameBuilder, Counter counter, String signifier,HashMap<String, Integer> mutantCounter) throws Exception {

	//	HashMap<String, Integer> mutantCounter = new HashMap<String,Integer>();
        long startTime = System.currentTimeMillis();
	if (mutationOperator.isApplicable()) {

	    //	    System.out.println("OperatorSelector[DEBUG] this.verbose" +this.verbose );
            if (this.verbose) {
                this.mutantNumber = 0;
            }
            this.mutantNumber += mutationOperator.getMutantNumber();
            //printApplicableMessage(mutationOperator, signifier);
            if (this.verbose) {
                mutationOperator.mutateMethod();
		//	System.out.println("OperatorSelector[DEBUG] this.mutantNumber" +this.mutantNumber );
                this.oracle.getTestResults(this.mutantNumber);
                this.oracle.compileTests();
            } else {
		//	System.out.println("OperatorSelector[DEBUG] this.mutantNumber in else branch" +this.mutantNumber );
                mutationOperator.mutateMethod();
            }

            if (mutationOperator instanceof SootMutationOperator) {
                if (!validateMutant((SootMutationOperator) mutationOperator)) {
                    this.brokenSootMutants++;
                    return;
                }
            }

            String mutantName = generateNameAndExport(mutationOperator, mutantNameBuilder, counter, mutantCounter);
        long endTime= System.currentTimeMillis();
        MUTANT_TIME.put(mutantName,endTime - startTime);
//        String executionTimeLogBuilder = mutantName + " generation time: " +
//            (endTime - startTime) +
//            " miliseconds";
//        System.out.println("[INFO] " +executionTimeLogBuilder);
    }
    }

    public void mutateStandard(MutationOperator mutationOperator, SootMethod test, StringBuilder mutantNameBuilder, Counter counter,HashMap<String, Integer> mutantCounter) throws Exception {
        mutationOperator.setCurrentMethod(test.getSubSignature());
        mutate(mutationOperator, mutantNameBuilder, counter, "test " + test.getSubSignature(), mutantCounter);
    }


    public void mutateInjector(MutationOperator mutationOperator, StringBuilder mutantNameBuilder, Counter counter,HashMap<String, Integer> mutantCounter) throws Exception {
        mutate(mutationOperator, mutantNameBuilder, counter, "class " + mutationOperator.getClass().getName(),mutantCounter);
    }

    public void setCurrentClass(MutationOperator mutationOperator) {
        this.oracle.setClassName(this.className);
        if (mutationOperator instanceof SootMutationOperator) {
            mutationOperator.setCurrentClass(this.inputDir, this.className);
        } else if (mutationOperator instanceof StringMutationOperator) {
            //TODO: use java path
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
        ProcessBuilder processBuilder = new ProcessBuilder("cp", fileName+".class", fileName+".temp");
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
        ProcessBuilder processBuilder = new ProcessBuilder("rm", fileName+".temp");
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

    private static int replaceFile(String root, String fileName)  throws IOException, InterruptedException {

        fileName = fileName.replace('.', '/');
        ProcessBuilder processBuilder = new ProcessBuilder("mv", fileName+".temp", fileName+".class");
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


    public void start(double threshold, int tries) throws Exception {

	HashMap<String, Integer> mutantCounter = new HashMap<String,Integer>();

        TemplatePool templatePool = new TemplatePool(this.testSuite, this.inputDir, this.jupiter, this.className, this.outputDir, this.templates);
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
        //startTime = System.currentTimeMillis();

        this.oracle.setSootOnly(sootOnly);
        if (this.testSuite.isAbstract()) {
            return;
        }

        int nonCompileableStringMutants = 0;

        if (sootOnly) {
            System.out.println("skipping string mutation operators");
        } else {
            try {
                StringMutationPool pool = new StringMutationPool(this.inputDir, this.className, this.outputDir, this.testSuite, this.jupiter, this.groundTruth, this.allowedOperators);
                pool.setThreshold(threshold);
                this.mutantNumber += pool.start();

                templatePool.restoreOriginal();
                templatePool.copyOriginalToJavaFile();
                templatePool.removeTempFiles();
                templatePool.removeOriginalFile();
/*                pool.export();
                nonCompileableStringMutants = pool.compile();*/
                /*pool.remove();*/
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
            System.out.println(ANSI_GREEN + "[INFO] "+ ANSI_RESET+"Bytecode Valid");
        } else {
            System.out.println("byte code is invalid");
            return;
        }

        startTime = System.currentTimeMillis();
        for (MutationOperator mutationOperator: this.mutationOperators) {

            mutationOperator.setJupiter(this.jupiter);

            /*System.out.println(i);
            System.out.println(mutationOperator.getClass().getSimpleName());
            System.out.println();*/
            /*if (this.oracle.getFatalError() && (mutationOperator instanceof IterationUnorderedCollectionMutationOperator || mutationOperator instanceof CustomClassIVMO)) {
                continue;
            }*/

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

            /*if (isFaulty(faultyMutations, mutationOperator.toString())) {*/
            /*    continue;*/
            /*}*/




            if (mutationOperator instanceof SootTestInjector || mutationOperator instanceof StringInjector) {
                /*try {*/
                AbstractMap.SimpleEntry<String, String> query = new AbstractMap.SimpleEntry<>("all", mutationOperator.getClass().getSimpleName());
                if (isContained(query)) {

                    System.out.println("defective mutation is removed: "+mutationOperator.getClass().getSimpleName());
                    continue;
                }
                try {
                    mutateInjector(mutationOperator, mutantNameBuilder, counter,mutantCounter);
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
                /*} finally {
                    continue;
                }*/
            }

            for (SootMethod test : this.testSuite.getMethods()) {
                if (!isTest(test) || test.getName().toLowerCase().contains("mutant")|| test.getName().toLowerCase().contains("Mutant")) {
                    continue;
                }

                AbstractMap.SimpleEntry<String, String> query = new AbstractMap.SimpleEntry<>(test.getName(), mutationOperator.getClass().getSimpleName());

                if (isContained(query)) {

                    System.out.println("defective mutation is removed: "+test.getName()+"-"+mutationOperator.getClass().getSimpleName());
                    continue;
                }

                try {
                    mutateStandard(mutationOperator, test, mutantNameBuilder, counter,mutantCounter);
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

	    //	    System.out.println("OperatorSelector[DEBUG] Final " +this.mutantNumber );

            mutationOperator.runOnceAfter();
            mutationOperator.setCurrentClass(null);
            this.testSuite = setUp(this.inputDir, this.className);
        }
	//System.out.println(ANSI_GREEN+"[INFO] Number of each Mutant Operator: "+ANSI_RESET +mutantCounter);
	//System.out.println(ANSI_GREEN+"[INFO] Total Number of Mutants generated: "+ANSI_RESET +counter.get() );
	//logger.info("OperatorSelector[DEBUG] Final ,{}", this.mutantNumber);
        endTime = System.currentTimeMillis();
        String executionTimeLogBuilder = "Mutation for " + this.inputDir + " Execution took time: " +
            (endTime - startTime) +" miliseconds";
        System.out.println(ANSI_GREEN+"[INFO] Total Time of Mutation: "+ANSI_RESET +executionTimeLogBuilder);
        if (!this.doNotRunTests) {
	    // System.out.println("_________________###############doNotRunTests");
            //this.oracle.getTestResults(0);

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
        removeTests(this.className);
        countFinalResults(this.className);

    }


    public void removeTests(String filePath) throws IOException {
        LinkedHashSet<String> testsToDelete = new LinkedHashSet<String>();
        testsToDelete.add("AWMO_test");
        testsToDelete.add("TMO_test");
        //testsToDelete.add("UPMO_test");
        testsToDelete.add("testMockito");
        testsToDelete.add("serverTest");
        testsToDelete.add("threadSleepTest");
        testsToDelete.add("timeZoneDependencyTest");
        testsToDelete.add("cacheTest");
        testsToDelete.add("testDB");
        testsToDelete.add("fileTest");
        testsToDelete.add("UPMO_testwhileLoopMapIUCMO");
        testsToDelete.add("testDBwhileLoopSetIUCMO");
        testsToDelete.add("testMainTMO_test");
        testsToDelete.add("mutant");
        testsToDelete.add("fileWriterTest");
//        testsToDelete.add("LocalTMOTMO_test");
        testsToDelete.add("CroissantMutant_OD_DSD_testDB_DatabaseMutationOperator_DatabaseTemplate");
        testsToDelete.add("CroissantMutant_OD_CSD_cacheTest_CaffeineCDMO_CacheTemplate");
        testsToDelete.add("CroissantMutant_OD_RA_fileTest_newFileNullODMO_FileTemplate");

        SootClass sootClass = Scene.v().getSootClass(filePath);
        List<String> methodNames = new LinkedList<String>();
        // can not rename or remove test method here, otherwise it will cause concurrency exception
        for (SootMethod sootMethod : sootClass.getMethods()) {
            String methodName = sootMethod.getName();
            methodNames.add(methodName);
        }
        //System.out.println(sootClass.getMethodCount());
        for (String testMethod : testsToDelete) {
            if (methodNames.contains(testMethod)) {
                sootClass.removeMethod(sootClass.getMethodByName(testMethod));
            }
        }
        for (String methodName:methodNames){
            if (methodName.contains("$")){
//               String[] temp = methodName.split("\\$");
//               String optimizedName = methodName.replace("$"+temp[temp.length-1],"");
//               if (methodNames.contains(optimizedName)){
//                   sootClass.getMethodByName(methodName).setName(methodName.replace("$","_"));
//               }
//               else sootClass.getMethodByName(methodName).setName(optimizedName);
                String newName = methodName.replace("$","_");
                sootClass.getMethodByName(methodName).setName(newName);
                MUTANT_TIME.put(newName.replace("$","_"),MUTANT_TIME.get(methodName.split("\\$")[0]));
            }
        }
//        for (String testName:MUTANT_TIME.keySet()){
//            if (testName.contains("$")){
//                MUTANT_TIME.put(testName.replace("$","_"),MUTANT_TIME.get(testName));
//            }
//
//        }

        //System.out.println(sootClass.getMethods().size());

//        Options.v().set_output_dir(outputDir);
//        String fileName = SourceLocator.v().getFileNameFor(sootClass, Options.output_format_class);
//        OutputStream streamOut = new JasminOutputStream(
//            new FileOutputStream(fileName));
//        PrintWriter writerOut = new PrintWriter(
//            new OutputStreamWriter(streamOut));
//        JasminClass jasminClass = new soot.jimple.JasminClass(sootClass);
//        jasminClass.print(writerOut);
//        writerOut.flush();
//        streamOut.close();

        Options.v().set_output_dir(outputDir);
        int java_version = Options.v().java_version();
        String fileName = SourceLocator.v().getFileNameFor(sootClass, Options.output_format_class);
        OutputStream streamOut = new FileOutputStream(fileName);
        BafASMBackend backend = new BafASMBackend(sootClass, java_version);
        backend.generateClassFile(streamOut);
        streamOut.close();


    }


    public void countFinalResults(String filePath) throws IOException, InterruptedException {

        int countMutants = 0;
        int ODMutants = 0;
        int NODMutants = 0;
        HashMap<String, Integer> finalMutantCounter = new HashMap<String,Integer>();
        SootClass sootClass = Scene.v().getSootClass(filePath);
        List<String> mutants = new LinkedList<String>();
        for (SootMethod sootMethod : sootClass.getMethods()) {
            String methodName = sootMethod.getName();
            if (methodName.contains("CroissantMutant")){
                countMutants++;
                mutants.add(methodName);
            }

        }

        LinkedHashSet<String> testClassesToClear = new LinkedHashSet<String>();
        String[] temp = filePath.split("\\.");
        StringBuilder builder = new StringBuilder();
        builder.append(outputDir);
        builder.append("/");
        for (int i=0;i<temp.length-1;i++){
            builder.append(temp[i]);
            builder.append("/");
        }

	    String fileterClassName = sootClass.getName().split("\\.")[sootClass.getName().split("\\.").length-1];
        File f = new File(builder.toString());
        String[] classes = f.list();
        for (String classname:classes) {
            if (classname.startsWith(fileterClassName+"_")&&(classname.contains("DeadLockMutationOperator")||classname.contains("ODStaticVariableMutationOperator")||classname.contains("RaceConditionMutationOperator"))){
                String className = filePath.replace(temp[temp.length-1],classname).replace(".class","");
                testClassesToClear.add(className);
            }
        }

        for (String classname:testClassesToClear){
            /*
            Scene.v().setSootClassPath(builder.toString()+classname.split("\\.")[classname.split("\\.").length-1]+".class");
            System.out.println(builder.toString()+classname+".class");
            Scene.v().loadNecessaryClasses();
            SootClass appclass = Scene.v().getSootClass(classname);
            System.out.println(appclass.getMethods());
            */
             //soot cannot get methods as expected, so use javap to analyze these test classes
            String fileName = classname.split("\\.")[classname.split("\\.").length-1];
            String classPath = builder.toString()+fileName+".class";
            //System.out.println(classname);
            ProcessBuilder process = new ProcessBuilder("javap",classPath);
            process.redirectErrorStream(true);
            Process p = process.start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while (true) {
                line = r.readLine();
                if (line != null) {
                    if (line.contains("CroissantMutant")) {
                        String mutantMethod = line.trim().split("\\s+")[2];
                        if (mutantMethod.contains(";")) mutantMethod.replace(";","");
                        if (mutantMethod.contains("(")) mutantMethod.replace("(","");
                        if (mutantMethod.contains(")")) mutantMethod.replace(")","");
                        mutants.add(mutantMethod);
                        //System.out.println(mutantMethod);
                    }
                }
                else { break; }
            }
        }
        for (String methodName:mutants){
            String splitChar = "_";
            String countName = methodName.split(splitChar)[2];
            String ON = methodName.split(splitChar)[1];

            if (finalMutantCounter.get(countName) == null){
                finalMutantCounter.put(countName,1);
            }
            else{
                finalMutantCounter.put(countName,finalMutantCounter.get(countName)+1);
            }
            if (ON.equals("OD")) ODMutants += 1;
            else NODMutants +=1;
        }

        System.out.println(ANSI_GREEN+"[INFO] Results:"+ANSI_RESET);
        System.out.println(ANSI_GREEN+"[INFO] Total Number of Mutants generated: "+ANSI_RESET +mutants.size() );
        System.out.println(ANSI_GREEN+"[INFO] Number of OD Mutants generated: "+ANSI_RESET +ODMutants );
        System.out.println(ANSI_GREEN+"[INFO] Number of NOD Mutants generated: "+ANSI_RESET +NODMutants );
        System.out.println(ANSI_GREEN+"[INFO] Number of each Mutant Operator: "+ANSI_RESET +finalMutantCounter);

        System.out.println(ANSI_GREEN+"[INFO] All mutants"+ANSI_RESET);
        Long all = Long.valueOf(0);
        for (String eachMutant:mutants){
            System.out.println(eachMutant );//+ " Genration time(ms): "+MUTANT_TIME.get(eachMutant)
            if (MUTANT_TIME.get(eachMutant)!=null )
                all += Long.valueOf(MUTANT_TIME.get(eachMutant));
        }

        System.out.println(ANSI_GREEN+"[INFO] Generation Time Only: "+ANSI_RESET+all);

	// System.out.println(MUTANT_TIME);



        //ProcessBuilder process = new ProcessBuilder("javap", "/home/yangc9/commons-csv/target/test-classes/org/apache/commons/csv/LexerTest_DeadLockMutationOperator_Test$2.class");

        //ByteArrayOutputStream stream = new ByteArrayOutputStream();
/*
        Scene.v().setSootClassPath("/home/yangc9/commons-csv/target/test-classes/org/apache/commons/csv/LexerTest_DeadLockMutationOperator_Test$2.class");
        Scene.v().loadNecessaryClasses();
        SootClass appclass = Scene.v().getSootClass("org.apache.commons.csv.LexerTest_DeadLockMutationOperator_Test$2");
     */
        //System.out.println(out);



    }


    public void createConfigFile(double threshold) {
        try {
            File configFile = new File(String.valueOf(Paths.get(this.outputDir, "mutation.config")));
            if (configFile.createNewFile()) {
                BufferedWriter writer = new BufferedWriter(new FileWriter(configFile));
                writer.write("mutation.threshold=" + threshold);
                writer.write("\nmutation.count=" + 5);
                writer.close();
                System.out.println(ANSI_GREEN + "[INFO] " + ANSI_RESET +"Config File Created");
            } else {
                System.out.println(ANSI_GREEN + "[INFO] " + ANSI_RESET +"Config File Exists.");
            }
        } catch (IOException e) {
            System.out.println("An error occurred while creating the config file");
            e.printStackTrace();
        }
    }



    private boolean isContained(AbstractMap.SimpleEntry<String, String> query) {
       /* System.out.println("query key: "+query.getKey());
        System.out.println("query value: "+query.getValue());*/
        for (Map.Entry<String, String> entry: this.oracle.faultyTestToOperator) {
            /*System.out.println("entry key: "+query.getKey());
            System.out.println("entry value: "+query.getValue());*/
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
        /*SootClass testClass = setUp(this.inputDir, this.className);
        SootMutationOperator sootMutationOperator = new SootTest();
        sootMutationOperator.setCurrentClass(testClass);

        sootMutationOperator.exportClass(this.outputDir);


        Runner runner = new Runner(this.outputDir);
        runner.setOperatorName(sootMutationOperator.getClass().getSimpleName());
        String fileName = SourceLocator.v().getFileNameFor(this.testSuite, Options.output_format_class);
        runner.setFileName(fileName);
        runner.setDirectory(this.outputDir);

        boolean result =  runner.verifyBytecode();

        File fileObject = new File(SourceLocator.v().getFileNameFor(testClass, Options.output_format_class));
        fileObject.delete();

        return result;*/

        /*return subprocess.exitValue();*/

    }



}


