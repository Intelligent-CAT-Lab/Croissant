package com.framework;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Deprecated
public class Oracle {

    private String className;
    private String directory;
    private Boolean isSootOnly;
    private int nonCompileableStringMutants;
    private int brokenSootMutants;
    private List<String> faultyMutations;
    private Boolean fatalError;
    public List<Map.Entry<String,String>> faultyTestToOperator;

    public Oracle(String directory) {
        this.faultyTestToOperator = new ArrayList<Map.Entry<String, String>>();
        this.fatalError = false;
        this.directory = this.getWorkingDirectory(directory);
        this.nonCompileableStringMutants = 0;
        this.brokenSootMutants = 0;
        this.faultyMutations = new ArrayList<>();
    }

    public Boolean getFatalError() {
        return this.fatalError;
    }

    public List<String> getFaultyMutations() {
        return this.faultyMutations;
    }

    public void setBrokenSootMutants(int num) {
        this.brokenSootMutants = num;
    }

    public void setNonCompileableStringMutants(int num) {
        this.nonCompileableStringMutants = num;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public void setSootOnly(Boolean sootOnly) {
        isSootOnly = sootOnly;
    }

    public String getWorkingDirectory(String directory) {
        List<String> directoryParsed = Arrays.asList(directory.split(File.separator));
        directoryParsed = directoryParsed.subList(0, directoryParsed.indexOf("target"));

        StringJoiner stringJoiner = new StringJoiner(File.separator);
        for (String elem : directoryParsed) {
            stringJoiner.add(elem);
        }
        return stringJoiner.toString();
    }

    public void getTestResults(Integer mutantNumber) {
	//System.out.println("[Oracle] [DEBUG] this.mutantNumber: in function getTestResults####*********" + mutantNumber );

        try {
            this.runTests(mutantNumber);
        } catch (InterruptedException e) {
            System.out.println("subprocess got interrupted, testName: " + this.className);
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Something went wrong with directory: " + this.directory);
            e.printStackTrace();
        }
    }

    private void runTests(Integer mutantNumber) throws InterruptedException, IOException {

        this.faultyMutations = new ArrayList<>();
        /*List<String> failedMutationOperators = new ArrayList<>();*/
        Integer failures = 0;
        Integer errors = this.nonCompileableStringMutants + this.brokenSootMutants;

        Integer[] results = this.runMutant(this.className);
        failures += results[0];
        errors += results[1];

        if (!this.isSootOnly) {
            results = this.runMutant(this.className + "Mutant");
            failures += results[0];
            errors += results[1];
        }

	//System.out.println("[Oracle] [DEBUG] this.mutantNumber: " + mutantNumber );
        StringBuilder finalResultsBuilder = new StringBuilder();
        finalResultsBuilder
            .append("Mutation Results: \n")
            .append("Failures: ")
            .append(failures)
            .append("\n")
            .append("Errors: ")
            .append(errors)
            .append("\n")
            .append("Mutant number: ")
            .append(mutantNumber);

        System.out.println(finalResultsBuilder);

    }

    public void removeTest() {
        String fullpath = this.directory + "/" + this.className + ".java";
        System.out.println("fullpath:");
        System.out.println(fullpath);
    }

    private Integer[] runMutant(String mutantName) throws IOException, InterruptedException {

        ProcessBuilder processBuilder = new ProcessBuilder("mvn", "surefire:test", "-Dtest=" + mutantName);
        processBuilder.directory(new File(this.directory));
        Process subProcess = processBuilder.start();


        BufferedReader stdInput = new BufferedReader(new
            InputStreamReader(subProcess.getInputStream()));

        BufferedReader stdError = new BufferedReader(new
            InputStreamReader(subProcess.getErrorStream()));

        String s = null;
        List<String> fullOutput = new ArrayList<>();

        while ((s = stdInput.readLine()) != null) {
            fullOutput.add(s);
        }
        while ((s = stdError.readLine()) != null) {
            fullOutput.add(s);
        }

        String[] errorPatterns;
        errorPatterns = new String[] {

            ".* (.*)__(.*)__.* signature: .* Register [0-9]* contains wrong type.*",
            ".* signature: \\(\\)V\\) Expecting to find unitialized object on stack",
            "[a-zA-Z0-9\\[\\]\\(:\\/, ]+ ([a-zA-Z]*)__(.*)[__]?[0-9]? signature: \\(\\)V\\) Accessing value from uninitialized register .*",
            ".* .*\\.(.*)\\(\\)V @.*: ifge",
            "\\[ERROR\\] \\(class: .*, method: .*__(.*) signature: \\(\\)V\\) Expecting to find unitialized object on stack",
            "\\[ERROR\\] Truncated class file",
            ".*Expected stackmap frame at this location.*",
            ".*\\(?class: .*, method: (.*)Mutant__(.*)__.* signature: \\(?\\)?V\\) Register [0-9]* contains wrong type",
            "[\\[ERROR\\]]* \\(?class: [a-zA-Z0-9/]+, method: ([a-zA-Z0-9]*)__([a-zA-Z0-9]*) signature: \\(\\)V\\) Bad type in .*",
            "\\[ERROR\\] TestEngine with ID 'junit-jupiter' failed to discover tests",
            "\\[ERROR\\] \\(?class: .*, method: (.*)__(.*)__.* signature: \\(\\)V\\)? Expecting to find object/array on stack",
            ".* TestEngine with ID 'junit-jupiter' failed to discover tests",
            ".*java.lang.VerifyError: \\(class: (.*), method: .*__(.*)__.* signature:.*",
            ".*\\(?class: .*, method: (.*) signature: \\(?\\)?V\\) Register [0-9]* contains wrong type"
        };
        for (String line: fullOutput) {

            for (String pattern: errorPatterns) {

                if (pattern.contains("jupiter") && line.contains("jupiter")) {
                    System.out.println();
                }

                Pattern compiledPattern = Pattern.compile(pattern);
                Matcher matcher = compiledPattern.matcher(line);


                if (matcher.find()) {
                    if (matcher.groupCount() == 2) {
                        String test = matcher.group(1);
                        String operator = matcher.group(2);
                        operator = operator.replace("Instance", "");
                        test = test.replace("/", ".");
                        test = test.replace("mutant", "");
                        test = test.replace("testMain", "");
                        this.faultyTestToOperator.add(new AbstractMap.SimpleEntry<> (test, operator));
                    } else if (matcher.groupCount() == 1) {
                        String test = "all";
                        String operator = matcher.group(1);

                        if (operator.contains("IUCMO")) {
                            operator = "IUCMO";
                        }

                        this.faultyTestToOperator.add(new AbstractMap.SimpleEntry<> (test, operator));
                    } else {
                        String test = "all";
                        String operator = "all";
                        this.faultyTestToOperator.add(new AbstractMap.SimpleEntry<> (test, operator));
                    }

                }
            }
        }
        System.out.println("waiting for the subprocess to finish");
        subProcess.waitFor();
        Integer[] results = this.parseResults(fullOutput);
        return results;
    }

    private boolean isSpecialError(String line) {
        Pattern[] specialErrorPatterns = new Pattern[]{
            Pattern.compile(".*TestTimedOutException: test timed out.*")
        };
        for (Pattern specialErrorPattern : specialErrorPatterns) {
            if (specialErrorPattern.matcher(line).matches()) {
                return true;
            }
        }
        return false;
    }

    private Integer[] parseResults(List<String> fullOutput) {
        Pattern resultsPattern = Pattern.compile(".*Tests run: [0-9]*, Failures: ([0-9]*), Errors: ([0-9]*), Skipped: [0-9]*.*");

        int failures = 0;
        int errors = 0;
        int special_errors = 0;


        for (String line : fullOutput) {
            Matcher matcher = resultsPattern.matcher(line);
            if (this.isSpecialError(line)) {
                special_errors++;
            }
            if (matcher.find()) {
                failures = Integer.parseInt(matcher.group(1));
                errors = Integer.parseInt(matcher.group(2));
            }
        }

        for (int i = 0; i < special_errors; i++) {
            failures++;
            errors--;
        }
        return new Integer[]{
            failures,
            errors
        };
    }


    public void compileTests() throws IOException, InterruptedException {
        System.out.println(this.directory);
        String[] command = {"mvn", "-f", this.directory, "clean", "install"};
        Process shellP = Runtime.getRuntime().exec(command);
    }

}
