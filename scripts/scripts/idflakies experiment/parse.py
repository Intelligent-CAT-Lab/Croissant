#!/usr/bin/env python3
import re
from statistics import mean, StatisticsError
import os
import runtool
from env import EXPERIMENT_REPEATS, MAX_CLEANER_NUMBER, TEST_NAME


def find_discovered_test(cleaner_number, experiment_no):
    return find_number_with_regex(cleaner_number, experiment_no, ".*Found ([0-9]+) tests, writing list to.*")

def find_execution_time(cleaner_number, experiment_no):
    return find_number_with_regex(cleaner_number, experiment_no, ".*Total time: *([0-9]*).*")

def find_number_with_regex(cleaner_number, experiment_no, pattern):
    if (os.path.isfile(f"idflakies_cleaner:{cleaner_number}_experiment:{experiment_no}.log")):
        with open(f"idflakies_cleaner:{cleaner_number}_experiment:{experiment_no}.log", "r") as log_file:
            # for line in log_file.readlines():
            #     result = re.search(pattern, line)

            #     if result:
            #         found_tests = result.group(1)
            #         return float(found_tests)
            log_file_contents = log_file.read()
            result = re.search(pattern, log_file_contents)

            if result:
                found_tests = result.group(1)
                return float(found_tests)

                
            raise Exception(f"cannot find pattern: \"{pattern}\" tests in: idflakies_cleaner:{cleaner_number}_experiment:{experiment_no}")
    else:
        raise FileNotFoundError(f"cannot find file: idflakies_cleaner:{cleaner_number}_experiment:{experiment_no}")

def find_average_result_for_cleaner_number(cleaner_number):
    results = list()

    for experiment_no in range(1, EXPERIMENT_REPEATS + 1):
        try:
            results.append(find_discovered_test(cleaner_number, experiment_no))
        except StatisticsError:
            return 0
    
    return mean(results)

def find_max_round(cleaner_number):
    results = list()

    for experiment_no in range(1, EXPERIMENT_REPEATS + 1):
        try:
            results.append(find_discovered_test(cleaner_number, experiment_no))
        except StatisticsError:
            return 0
    
    return max(results)

def find_min_round(cleaner_number):
    results = list()

    for experiment_no in range(1, EXPERIMENT_REPEATS + 1):
        try:
            results.append(find_discovered_test(cleaner_number, experiment_no))
        except StatisticsError:
            return 0
    
    return min(results)


def find_average_time_for_cleaner_number(cleaner_number):
    results = list()

    for experiment_no in range(1, EXPERIMENT_REPEATS + 1):
        results.append(find_execution_time(cleaner_number, experiment_no))
    
    return mean(results)

def find_average_discovered_round(cleaner_number):
    results = list()
    for experiment_no in range(1, EXPERIMENT_REPEATS + 1):
        results.append(find_discovered_round(cleaner_number, experiment_no, TEST_NAME))
    
    try:
        return mean(filter(lambda result: result > 0, results))
    except StatisticsError:
        return -1

def find_discovered_round(cleaner_number, experiment_no, test_name):
    verified_pattern = f"Verified {test_name}, status: expected [a-zA-Z]+, got [a-zA-Z]+"
    round_pattern = "Found [0-9]+ tests in round ([0-9]+) of [0-9]+"
    verified_line_found = False

    with open(f"idflakies_cleaner:{cleaner_number}_experiment:{experiment_no}.log", "r") as log_file:
        for line in log_file.readlines():

            if not verified_line_found:
                result = re.search(verified_pattern, line)
                if result:
                    verified_line_found = True
            else:
                result = re.search(round_pattern, line)
                if result:
                    return int(result.group(1))
    
    return -1
if __name__ == "__main__":
    runtool.change_working_directory()
    print("starting parsing")
    averages = dict()
    times = dict()
    rounds = dict()
    max_round = dict()
    min_round = dict()
    for cleaner_number in range(0, MAX_CLEANER_NUMBER+1):
        averages[cleaner_number] = find_average_result_for_cleaner_number(cleaner_number)
        times[cleaner_number] = find_average_time_for_cleaner_number(cleaner_number)
        rounds[cleaner_number] = find_average_discovered_round(cleaner_number)
        max_round[cleaner_number] = find_max_round(cleaner_number)
        min_round[cleaner_number] = find_min_round(cleaner_number)

    print("experiment results:")

    for key in averages.keys():
        print(f"cleaner number: {key}, average found tests: {averages[key]}, average execution time: {times[key]} s, average discovery round: {rounds[key]}, max: {max_round[key]}, min: {min_round[key]}")
    
    print("done")