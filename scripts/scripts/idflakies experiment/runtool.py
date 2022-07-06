#!/usr/bin/env python3

import os
import random
import shutil
import subprocess
from env import SUBJECT_DIR, SUBJECT_NAME, IDFLAKIES_ROUND, IDFLAKIES_OPTIONS


def change_working_directory():
    os.chdir(os.path.join(os.getcwd(), SUBJECT_DIR, SUBJECT_NAME))
    print(f"cwd: {os.getcwd()}")

def remove_dtfixing_folder():
    if (os.path.isdir(".dtfixingtools")):
        shutil.rmtree(".dtfixingtools")

def rename_dtfixingtools_folder(new_folder_name):
    if (os.path.isdir(".dtfixingtools")):
        shutil.move(".dtfixingtools", new_folder_name)
    else:
        raise Exception("cannot find dtfixingtools")

def run_idflakies_and_redirect_output(log_file_name, experiment_no):
    # subprocess.run(f"mvn surefire:test".split(" "), stdout=f, stderr=f)
    seed = f" -Ddt.seed={experiment_no * 101}"
    print(f"mvn idflakies:detect {IDFLAKIES_OPTIONS} -Ddt.randomize.rounds={IDFLAKIES_ROUND}" + seed)
    with open(log_file_name, "w") as file:
        subprocess.run(
        (f"mvn idflakies:detect {IDFLAKIES_OPTIONS} -Ddt.randomize.rounds={IDFLAKIES_ROUND}" + seed).split(" "),
        stdout=file,
        stderr=file
        )
    
def change_cleaner_number(cleaner_number):

    if (os.path.isfile(os.path.join(os.getcwd(),"target","test-classes", "mutation.config"))):
        with open(os.path.join("target","test-classes", "mutation.config"), "w") as config_file:
            config_file.write(
                f"""
                mutation.threshold=1
                mutation.count={cleaner_number}
                """
            )
    else:
        raise Exception("cannot find mutation.config, are you sure the subject has been mutated?")


