package com.framework;

import org.apache.commons.cli.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class App {

    public static void main(String[] args) throws Exception {

        String directory;
        String className;
        boolean jupiter = false;
        String outputDirectory;
        double threshold = 0;
        Bundle bundle = new Bundle();

        CommandLineParser commandLineParser = new BasicParser();
        Options options = new Options();

        options.addOption("d", "dir", true, "directory where the test suite is located in");
        options.addOption("t", "threshold", true, "threshold parameter (0 to deterministically pass, 1 to deterministically fail)");
        options.addOption("n", "name", true, "name of the test suite class");
        options.addOption("o", "output", true, "output directory");
        options.addOption("so", "sootOnly", false, "run only soot mutation operators");
        options.addOption("cb", "compileBefore", false, "compile tests before running");
        options.addOption("dr", "doNotRunTests", false, "do not run tests after mutation");
        options.addOption("v", "verbose", false, "execute tests for each mutation operator call");
        options.addOption("j", "jupiter", false, "use jupiter with junit");
        options.addOption("g", "groundTruth", false, "ground truth mode");
        options.addOption("mo", "operators", true, "mutation operators that will be used");
        options.addOption("tm", "templates", true, "templates that will be used");

        boolean sootOnly = false;
        boolean compileBefore = false;
        boolean doNotRunTests = false;
        boolean verbose = false;
        boolean groundTruth = false;
        List<String> templates = new ArrayList<>();
        List<String> mutationOperators = new ArrayList<>();

        try {
            CommandLine commandLine = commandLineParser.parse(options, args);
            directory = commandLine.getOptionValue("d");
            className = commandLine.getOptionValue("n");
            outputDirectory = commandLine.getOptionValue("o");
            if (commandLine.hasOption(("threshold"))) {
                threshold = Double.parseDouble(commandLine.getOptionValue("threshold"));
            }

            if (commandLine.hasOption("sootOnly")) {
                sootOnly = true;
            }

            if (commandLine.hasOption("compileBefore")) {
                compileBefore = true;
            }

            if (commandLine.hasOption("doNotRunTests")) {
                doNotRunTests = true;
            }

            if (commandLine.hasOption("verbose")) {
                verbose = true;
            }

            if (commandLine.hasOption("jupiter")) {
                jupiter = true;
            }
            if (commandLine.hasOption("groundTruth")) {
                groundTruth = true;
            }
            if (commandLine.hasOption("operators")) {
                String operatorInstances = commandLine.getOptionValue("operators");
                Values values = bundle.getValues(operatorInstances);
                operatorInstances += "," + values.operatorInstances;
                mutationOperators = Arrays.asList(operatorInstances.split(","));
            }
            if (commandLine.hasOption("templates")) {
                String templateInstances = commandLine.getOptionValue("templates");
                Values values = bundle.getValues(templateInstances);
                templateInstances += "," + values.templates;
                templates = Arrays.asList(templateInstances.split(","));
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return;
        }



        OperatorSelector operatorSelector = new OperatorSelector(directory, className, outputDirectory, sootOnly, compileBefore, doNotRunTests, verbose, jupiter, groundTruth, mutationOperators, templates);
        operatorSelector.start(threshold);
        operatorSelector.createConfigFile(threshold);


    }
}
