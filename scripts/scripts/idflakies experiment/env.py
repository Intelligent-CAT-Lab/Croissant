#!/usr/bin/env python3

import os
from random import randint

MAX_CLEANER_NUMBER = 5
EXPERIMENT_REPEATS = 5
SUBJECT_DIR = "subject"
SUBJECT_NAME = os.environ.get("SUBJECT_NAME")
IDFLAKIES_ROUND = 10
RESET = True
IDFLAKIES_OPTIONS = " ".join([
    "-Ddetector.detector_type=random-class-method",
    "-Ddt.detector.original_order.all_must_pass=false",
    "-Drat.skip",
    "-Ddt.detector.roundsemantics.total"
    ])
TEST_NAME = os.environ.get("TEST_NAME")