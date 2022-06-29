#!/usr/bin/env python3

import os.path
import subprocess
import platform
import sys
import multiprocessing as mp
import threading

projects = list()



def get_modules(project_dir):
    modules = list()
    for path, _, _ in os.walk(project_dir):
        files = [f for f in os.listdir(path)]
        if "pom.xml" in files:
            modules.append(path)
    return modules


def compile(project_directory):
    os.chdir(project_directory)
    subprocess.run("mvn clean install -Drat.skip=true -U -Dspotless.check.skip=true -Dcheckstyle.skip", shell=True)
    subprocess.run("mvn surefire:test", shell=True)


def get_tests(project_directory):
    
    path = os.path.join(os.environ.get("JAVA_SUBJECTS_DIR"), project_directory, "target", "maven-status", "maven-compiler-plugin", "testCompile", "default-testCompile")
    if os.path.exists(path):

        if (platform.system() == "Darwin" or platform.system() == "Linux"):
            process = subprocess.run("cat createdFiles.lst", shell=True, capture_output=True, text=True)
        elif (platform.system() == "Windows"):
            process = subprocess.run("type createdFiles.lst", shell=True, capture_output=True, text=True)

        
        tests =  list(filter(lambda x: x != "", process.stdout.split("\n")))
        tests = list(filter(lambda x: "test" in x.lower(), tests))
        tests = [test.replace("/", ".") for test in tests]
        tests = [test.replace("\\", ".") for test in tests]
        tests = [test for test in tests if not "$" in test]

    if os.path.exists(path) == False or len(tests) == False:
        path = os.path.join(os.environ.get("JAVA_SUBJECTS_DIR"), project_directory, "target", "surefire-reports")
        
        if not os.path.exists(path):
            return []

        all_files = os.listdir(path)

        tests = list()

        for file in all_files:
            ext = os.path.splitext(file)[-1].lower()
            if (ext == ".txt"):
                tests.append(file.replace(".txt", ""))
        
    print("tests found: ")
    for test in tests:
        print(test)


    return tests


def mutate_module(module):

    global jupiter
    global ground_truth

    print("project_dir: ", os.environ.get("PROJECT_DIR"))

    tests = get_tests(module)
    
    command = 'mvn -f '+ os.environ.get("PROJECT_DIR")+' exec:java -Dexec.mainClass=com.framework.App  -Dexec.args=" -dir {} -name {}  -o {} -t 0.5 '

    if jupiter:
        command += " -j "
    if ground_truth:
        command += " -g " 
    command += '"'

    test_class_dir = os.path.join(module, "target", "test-classes")

    for test in tests:
        formatted_command = command.format(test_class_dir, test.replace(".class",""), test_class_dir)
        print("executed command:", formatted_command)
        process = subprocess.run(formatted_command, shell=True, capture_output=True, text=True)


        print(process.stdout)
        print(process.stderr)

def mutate_project(project):
    compile(project)
    modules = get_modules(project)

    mutate_module(project)

    for module in modules:
        mutate_module(module)


def main():

    project_directories = [os.path.join(os.environ.get("JAVA_SUBJECTS_DIR") ,directory) for directory in projects]
    project_directories.reverse()

    pool = mp.Pool()
    pool.map(mutate_project, project_directories)
    pool.close()
    pool.join()

 

if __name__ == "__main__":
    jupiter = False
    ground_truth = False

    os.environ["JAVA_SUBJECTS_DIR"] = "./3.1_subjects"
    os.environ["PROJECT_DIR"] = "./mutation_testing"

    main()
