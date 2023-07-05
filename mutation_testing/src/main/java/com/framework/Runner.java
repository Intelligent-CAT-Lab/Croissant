package com.framework;

import soot.SourceLocator;
import soot.options.Options;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Runner {

    public Runner(String directory) {
        this.directory = directory;
    }

    String directory;
    String className;
    String operatorName;
    String fileName;
    String[] errorPatterns =  new String[] {

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

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    private String getMutantName() {
        String path;
        if (!fileName.contains(operatorName)) {
            path = fileName.replace(".class","") + "_" + operatorName + "Test" + ".class";
        } else {
            path =  fileName;
        }

        String[] parts = path.split(File.separator);
        StringJoiner stringJoiner = new StringJoiner(".");

        int i = 0;
        while(!parts[i].equals("test-classes")) {
            i++;
        } i++;

        while(i < parts.length) {
            stringJoiner.add(parts[i]);
            i++;
        }

        return stringJoiner.toString().replace(".class", "");

    }

    public void setDirectory(String directory) {
        String[] parts = directory.split(File.separator);
        StringJoiner stringJoiner = new StringJoiner(File.separator);

        int i = 0;
        while(!parts[i].equals("target")) {
            stringJoiner.add(parts[i]);
            i++;
        }

        this.directory = stringJoiner.toString();
    }


    public boolean verifyBytecode() throws IOException, InterruptedException {

        String mutantName = this.getMutantName();
        ProcessBuilder processBuilder = new ProcessBuilder("mvn","-f" ,this.directory , "surefire:test", "-Dtest=" + mutantName);
        processBuilder.directory(new File(this.directory));
        Process subProcess = processBuilder.start();

        BufferedReader stdInput = new BufferedReader(new
            InputStreamReader(subProcess.getInputStream()));

        BufferedReader stdError = new BufferedReader(new
            InputStreamReader(subProcess.getErrorStream()));

        String s = null;

        while ((s = stdInput.readLine()) != null) {
            if (s.contains("IO Failed to bind to 0.0.0.0/0.0.0.0:555")) {
                return true;
            }
            System.out.println(s);
        }
        while ((s = stdError.readLine()) != null) {
            if (s.contains("IO Failed to bind to 0.0.0.0/0.0.0.0:555")) {
                return true;
            }
            System.out.println(s);
        }

        subProcess.waitFor();

        return subProcess.exitValue() == 0;

    }

    public void runTest() throws IOException, InterruptedException {
        String mutantName = this.getMutantName();
        ProcessBuilder processBuilder = new ProcessBuilder("mvn","-f" ,this.directory , "surefire:test", "-Dtest=" + mutantName);
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


        for (String line : fullOutput) {

            for (String pattern : errorPatterns) {

                Pattern compiledPattern = Pattern.compile(pattern);
                Matcher matcher = compiledPattern.matcher(line);

                boolean res = matcher.find();
                if (res) {
                    removeTest();
                }

            }
        }
    }

    private String getMutantFile() {
        String[] parts = this.getMutantName().split("\\.");
        String mutantName = parts[parts.length-1];

        parts = this.fileName.split(File.separator);
        parts[parts.length-1] = mutantName;

        StringJoiner stringJoiner = new StringJoiner(File.separator);
        for (String part: parts) {
            stringJoiner.add(part);
        }
        return stringJoiner + ".class";
    }


    private void removeTest() {
        File myObj = new File(getMutantFile());

        if (myObj.delete()) {
            System.out.println("Removed test: " + myObj.getName());
        } else {
            System.out.println("Failed to remove the test.");
        }
    }


}
