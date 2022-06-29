#!/usr/bin/env python3

import subprocess
import os
import platform
import sys
import re
import util


errors_list = list()

def compile(project_directory):
    os.chdir(project_directory)
    process = subprocess.run("mvn clean install -Drat.skip=true -U", shell=True)

    if process.returncode != 0:
        process = subprocess.run("mvn clean install -Drat.skip=true", shell=True)
    
    subprocess.run("mvn surefire:test", shell=True)
    
def get_tests(project_directory):
    path = os.path.join(project_directory, "target", "maven-status", "maven-compiler-plugin", "testCompile", "default-testCompile")
    os.chdir(path)

    if (platform.system() == "Darwin" or platform.system() == "Linux"):
        process = subprocess.run("cat createdFiles.lst", shell=True, capture_output=True, text=True)
    elif (platform.system() == "Windows"):
        process = subprocess.run("type createdFiles.lst", shell=True, capture_output=True, text=True)

    
    tests =  list(filter(lambda x: x != "", process.stdout.split("\n")))
    tests = list(filter(lambda x: "test" in x.lower(), tests))
    tests = [test.replace("/", ".") for test in tests]
    tests = [test.replace("\\", ".") for test in tests]
    tests = [test for test in tests if not "$" in test]


    print("tests found: ")
    for test in tests:
        print(test)

   
    
    return tests


def log_error(error):
    errors_list.append(error)


def parse_mutation_result(output):
    outputList = output.split("\n")
    failure = 0
    error = 0
    mutants = 0
    execution_time = 0

    for elem in outputList:

        if ("Failures:" in elem):
            parsed = elem.split(": ")
            failure = int(parsed[1])
        
        if ("Errors:" in elem):
            parsed = elem.split(": ")
            error = int(parsed[1])
        
        if ("Mutant number:" in elem):
            parsed = elem.split(": ")
            mutants = int(parsed[1])
        
        if ("Execution took:" in elem):
            parsed = elem.split(": ")
            execution_time = int(parsed[1].replace(" miliseconds",""))


            
    result = [failure, error, mutants, execution_time]
    return result
    

            


  
def run_mutation_operators(mutation_framework, project_directory, tests):

    
    command = 'mvn exec:java -Dexec.mainClass=com.framework.App  -Dexec.args=" -dir {} -name {}  -o {} -t 0.5"'    
    test_class_dir = os.path.join(project_directory, "target", "test-classes")

    failure = 0
    error = 0
    mutant_number = 0
    execution_time = 0
    test_count = len(get_tests(project_directory))

    
    os.chdir(mutation_framework)

    progress = 0
    for test in tests:


        formatted_command = command.format(test_class_dir, test.replace(".class",""), test_class_dir)
        print("executed command:", formatted_command)
        process = subprocess.run(formatted_command, shell=True, capture_output=True, text=True)
        
        
        #assert (process.returncode == 0)
        
        print(process.stdout)
        print(process.stderr)


        if (process.returncode == 0):
            result = parse_mutation_result(process.stdout)
            failure += result[0]
            error += result[1]
            mutant_number += result[2]
            execution_time += result[3] / test_count

            
            parse_mutation_result(process.stderr)
            failure += result[0]
            error += result[1]
            mutant_number += result[2]
            execution_time += result[3] / test_count
            
            print("approximate time for finishing experiment for this project: ", (test_count-progress)*result[3]/60000, "minutes")
        else:
            log_error(process.stdout)
            log_error(process.stderr)
            error += 1

        progress += 1
        print("progress:", str(progress)+"/"+str(test_count))
        

    return [failure, error, mutant_number, execution_time]

def run_tests(project_directory):
    os.chdir(project_directory)
    command = "mvn surefire:test"
    process = subprocess.run(command, shell=True, capture_output=True, text=True)
    return process.stdout


def find_test_failures(stdout_output):
  regex = "Tests run: ([0-9]*), Failures: ([0-9]*), Errors: ([0-9]*), Skipped: ([0-9]*).*"
  search_result = re.findall(regex, stdout_output)[-1]
  return int(search_result[1])

def find_test_errors(stdout_output):
  regex = "Tests run: ([0-9]*), Failures: ([0-9]*), Errors: ([0-9]*), Skipped: ([0-9]*).*"
  search_result = re.findall(regex, stdout_output)[-1]

  return int(search_result[2])

def find_test_run(stdout_output):
  regex = "Tests run: ([0-9]*), Failures: ([0-9]*), Errors: ([0-9]*), Skipped: ([0-9]*).*"
  search_result = re.findall(regex, stdout_output)[-1]
  return int(search_result[0])

def analyze(project_directory):

    #compile(project_directory)
    tests = get_tests(project_directory)
    mutation_framework = os.environ.get("PROJECT_DIR")
    results =  run_mutation_operators(mutation_framework, project_directory, tests)

    return {
        "created": results[2],
        "fails": results[0],
        "errors": results[1],
        "survived": results[2] - results[0],
        "execution_time": results[3]
    }

def main():
    working_dir = os.getcwd()
    if os.environ.get("SUPRESS_EXCEPTION"):
        print("supressing exceptions")
    else:
        print("showing all exceptions")
    if os.environ.get("PROJECT_DIR") is None or os.environ.get("JAVA_SUBJECTS_DIR") is None:
        print("directory not found")
        sys.exit(1)


    project_directories = [
    "commons-csv"
    ]

    project_directories = [os.path.join(os.environ.get("JAVA_SUBJECTS_DIR"), directory) for directory in project_directories]

    print(project_directories)
    results = list()
    for project_directory in project_directories:
        try:
            analysis = analyze(project_directory)
            results.append(analysis)
        except Exception as e:
            print(e)






    created = sum(map(lambda x: x["created"], results))
    fails = sum(map(lambda x: x["fails"], results))
    errors = sum(map(lambda x: x["errors"], results))
    survived = sum(map(lambda x: x["survived"], results))
    execution_time = sum(map(lambda x: x["execution_time"], results))

    print(created, "mutants created")
    print(fails, "mutants killed")
    print(errors, "mutants resulted in error")
    print(survived, "mutants survived")
    print(execution_time, "miliseconds / test suite")

    os.chdir(working_dir)

if __name__ == "__main__":
    os.environ["PROJECT_DIR"] = "/Users/alperenyildiz/Documents/myprojects/Mutation-testing-for-test-flakiness-exp/mutation_testing"
    os.environ["JAVA_SUBJECTS_DIR"] = "/Users/alperenyildiz/Desktop/flaky/subjects/prepped"
    main()



