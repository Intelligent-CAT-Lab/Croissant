#!/usr/bin/env python3

from env import SUBJECT_DIR, SUBJECT_NAME
import shutil
from glob import glob
import os

def reset():
    dt_fixing_tools_pattern = "dtfixingtools*"
    idflakies_pattern = "idflakies*"

    # for p in Path(".").glob(dt_fixing_tools_pattern):
    #     p.rmdir()
    
    # for p in Path(".").glob(idflakies_pattern):
    #     p.unlink()

    for match in glob(dt_fixing_tools_pattern):
        shutil.rmtree(match)
    
    for f in glob(idflakies_pattern):
        os.remove(f)
