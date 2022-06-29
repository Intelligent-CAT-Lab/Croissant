import platform
import dependencies
from util import change_working_dir
import threshold
import os
import subprocess
import multiprocessing as mp
import shutil
import sys
import coloredlogs
import logging
import util


logger = logging.getLogger(__name__)
fh = logging.FileHandler('result.log')
fh.setLevel(logging.DEBUG)
formatter = coloredlogs.ColoredFormatter('%(asctime)s - %(message)s')
fh.setFormatter(formatter)
logger.addHandler(fh)
coloredlogs.install(level='DEBUG')

threshold_value = sys.argv[1]


def run_mutant_count_parallel(projects):
    pool = mp.Pool()
    pool.map(run_mutant_count, projects)
    pool.close()
    pool.join()

def run_surefire_sequential(projects):
    for project in projects:
        run_surefire(project)

def run_nondex_sequential(projects):
    for project in projects:
        run_nondex(project)

def run_idflakies_sequential(projects):
    for project in projects:
        run_idflakies(project)

def run_surefire_parallel(projects):
    pool = mp.Pool()
    pool.map(run_surefire, projects)
    pool.close()
    pool.join()

def run_nondex_parallel(projects):
    pool = mp.Pool()
    pool.map(run_nondex, projects)
    pool.close()
    pool.join()

def run_idflakies_parallel(projects):
    pool = mp.Pool()
    pool.map(run_idflakies, projects)
    pool.close()
    pool.join()




projects = util.get_immediate_subdirectories(".")


def create_detection_folder():
    print(os.getcwd())
    if (os.path.isdir("detection_results")):
        return
    else:
        os.mkdir("detection_results")


def run_surefire(project):
    logger.warning(f"running surefire on {project}")
    os.chdir(project)
    create_detection_folder()
    filename = f"detection_results/surefire_{project}_{threshold_value}.log"

    with open(filename, "w") as f:
        process = subprocess.run(f"mvn surefire:test -Dsurefire.rerunFailingTestsCount=3".split(" "), stdout=f, stderr=f)

    os.chdir("..")

def run_mutant_count(project):
    
    logger.warning(f"running mutant count on {project}")
    path = os.path.join(project, "target", "maven-status", "maven-compiler-plugin", "testCompile", "default-testCompile")
    os.chdir(path)

    if (platform.system() == "Darwin" or platform.system() == "Linux"):
        process = subprocess.run("cat createdFiles.lst".split(" "), capture_output=True, text=True)
    elif (platform.system() == "Windows"):
        process = subprocess.run("type createdFiles.lst".split(" "), capture_output=True, text=True)

    tests =  list(filter(lambda x: x != "", process.stdout.split("\n")))
    tests = list(filter(lambda x: "test" in x.lower(), tests))
    tests = [test.replace("/", ".") for test in tests]
    tests = [test.replace("\\", ".") for test in tests]
    tests = [test for test in tests if not "$" in test]
    stringMutationTests = [test.replace(".class", "Mutant.class") for test in tests]

    os.chdir("/")
    os.chdir(os.path.join(os.environ.get("WORKING_DIR"),project))
    create_detection_folder()
    filename = f"detection_results/mutant_count_{project}.log"
    with open(filename, "w") as f: 
        subprocess.run(f"mvn surefire:test".split(" "), stdout=f, stderr=f)

        for test in stringMutationTests:
            subprocess.run(f"mvn surefire:test -Dtest={test}".split(" "), stdout=f, stderr=f)




    os.chdir("..")

def run_nondex(project):
    logger.warning(f"running nondex on {project}")
    os.chdir(project)
    create_detection_folder()
    filename= f"detection_results/nondex_{project}_{threshold_value}.log"

    with open(filename, "w") as f:
        p = subprocess.run(f"mvn nondex:nondex -Drat.skip=true".split(" "), stderr=f, stdout=f)

    os.chdir("..")

def run_idflakies(project):
    logger.warning(f"running idflakies on {project}")
    os.chdir(project)
    
    create_detection_folder()

    dt_fixing_dir = ".dtfixingtools"
    if os.path.isdir(dt_fixing_dir):
        shutil.rmtree(dt_fixing_dir)
        print("dtfixingtools folder is removed")
    else:
        print("dtfixingtools folder does not exist")
    
    for dirpath, _, files in os.walk("."): #remove original-order file to clean the state
        for file in files:
            if file == "original-order":
                file_path = os.path.join(dirpath, file)
                os.remove(file_path)
    # detectors = ["original","original-order", "random-class", "random-class-method", "reverse-class", "reverse-class-method"]
    detectors = ["original"]


    for detector in detectors:
        logger.warning(f"running idflakies on {project} with detector: {detector}")
        filename = f"detection_results/idflakies-{detector}_{project}_{threshold_value}.log"

        with open(filename, "w") as f:
            p1 = subprocess.run(f"mvn testrunner:testplugin -Ddt.detector.original_order.all_must_pass=false -Ddetector.detector_type={detector} -Dmaven.test.skip=true -Ddt.randomize.rounds=1 -Denforcer.skip=true -Drat.skip=true -Dmdep.analyze.skip=true -Dmaven.javadoc.skip=true".split(" "), stderr=f, stdout=f)
        
        with open(filename, "a") as f:
            p2 = subprocess.run(f"cat .dtfixingtools/detection-results/list.txt".split(" "), stderr=f, stdout=f)


    os.chdir("..")

def main():

    change_working_dir()

    # if (len(sys.argv) < 2):
    #     raise ValueError("missing threshold value")

    projects = util.get_immediate_subdirectories(".")
    projects = ["commons-csv_od_experimentation"]

    cache = [
        "__pycache__",
        "__MACOSX"
    ]
    
    for folder in cache:
        if folder in projects:
            projects.remove(folder)


    

    logger.warning(f"starting the experiments with threshold value: {threshold_value}")

    logger.debug("***************************************")
    logger.debug("MUTANT COUNT\n")
    logger.info("changing the threshold values to 1")
    threshold.change_thresholds(1)
    logger.info("threshold values have been changed")
    logger.info("printing the threshold values")
    threshold.print_thresholds()


    run_mutant_count_parallel(projects)


    logger.debug("***************************************")

    logger.debug("***************************************")
    logger.debug("THRESHOLD\n")
    logger.warning(f"changing the threshold values to {threshold_value}")
    # threshold.change_thresholds(threshold_value)
    logger.info("threshold values have been changed")
    logger.info("printing the threshold values")
    # threshold.print_thresholds()
    logger.debug("***************************************")
    
    logger.debug("***************************************")
    logger.debug("DEPENDENCIES\n")
    logger.warning("adding dependencies")
    # dependencies.add_dependencies()
    logger.debug("***************************************")

    logger.debug("***************************************")
    logger.debug("DETECTION\n")
    logger.debug("running test flakiness detection software")
    # run_surefire_parallel(projects)
    # run_nondex_parallel(projects) 
    # run_idflakies_parallel(projects)
    logger.debug("***************************************")

    logger.warning("done")

if __name__ == "__main__":
    os.environ["WORKING_DIR"] = "./4_croissant_mutants"
    main()

    
