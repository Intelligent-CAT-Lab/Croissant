#!/usr/bin/env python3

import runtool
import parse
import clean
from env import EXPERIMENT_REPEATS, MAX_CLEANER_NUMBER, RESET, TEST_NAME
import os
import dependencies
def main():



    print(f"starting the experiment with repeats: {EXPERIMENT_REPEATS} and max cleaner number: {MAX_CLEANER_NUMBER}")
    runtool.change_working_directory()
    dependencies.add_dependencies()

    if RESET:
        clean.reset()

    runtool.remove_dtfixing_folder()
  
    for cleaner_number in range(0,MAX_CLEANER_NUMBER + 1):
        print(f"changing the cleaner number to: {cleaner_number}")
        runtool.change_cleaner_number(cleaner_number)
        
        for experiment_no in range(1,EXPERIMENT_REPEATS + 1):
            print(f"running experiment no: {experiment_no}")
            runtool.run_idflakies_and_redirect_output(f"idflakies_cleaner:{cleaner_number}_experiment:{experiment_no}.log", experiment_no)
            runtool.rename_dtfixingtools_folder(f"dtfixingtools_:{cleaner_number}_experiment:{experiment_no}")

    print("starting parsing")
    averages = dict()
    times = dict()
    rounds = dict()

    for cleaner_number in range(0, MAX_CLEANER_NUMBER+1):
        averages[cleaner_number] = parse.find_average_result_for_cleaner_number(cleaner_number)
        times[cleaner_number] = parse.find_average_time_for_cleaner_number(cleaner_number)
        rounds[cleaner_number] = parse.find_average_discovered_round(cleaner_number)
    print("experiment results:")

    for key in averages.keys():
        print(f"cleaner number: {key}, average found tests: {averages[key]}, average execution time: {times[key]} s, average discovery round: {rounds[key]}")
    
    print("done")

if __name__ == "__main__":
    main()