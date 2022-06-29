import re
import os
from util import change_working_dir

from sympy import ordered
import util
import coloredlogs
import logging
from collections import OrderedDict
from abc import ABC, abstractmethod

logger = logging.getLogger(__name__)
fh = logging.FileHandler('parsed_results.log')
fh.setLevel(logging.DEBUG)
formatter = coloredlogs.ColoredFormatter('%(asctime)s - %(message)s')
fh.setFormatter(formatter)
logger.addHandler(fh)
coloredlogs.install(level='DEBUG')

class Parser(ABC):
    def __init__(self, start_string, end_string):
        self.start_string = start_string
        self.end_string = end_string
        self.mutation_operator_instances = [
            "RaceConditionMutationOperator",
            "MapIUCMO",
            "SetUCIMO",
            "MapIVMO",
            "PathFSFMO",
            "CustomClassIVMO",
            "testTimeValueInitializationTwiceMutant",
            "HashMapUAMO",
            "PrimitiveIVMO",
            "ListIVMO",
            "HashSetUAMO",
            "SetIUCMO",
            "memoryBoundViolationMO",
            "LatchObjectMO",
            "ThreadSleepMO",
            "VoidLatchAwaitMO",
            "CaffeineCDMO",
            "TypedQueryDMO",
            "FileObjectFMO",
            "FileOutputStreamFSFMO",
            "RandomAccessFileFSFMO",
            "StringFileWriterFSFMO",
            "TempFileFSFMO",
            "MockitoMutationOperator:",
            "jettyServerSetNonHardcodedPortMO",
            "SetIVMO",
            "WrapperIVMO",
            "GlobalTMO",
            "LocalTMO",
            "newFileNullMO",
            "jettyServerSetHardcodedPortMO",
            "TRRInjectAssertLocal",
            "UnorderedPopulationMutationOperator",
            "TimeZoneDependencyMO",
            "WIPTRRInjectAssertInstance",
            "JsonUAMO",
            "JsonUCIMO",
            "JsonIUCMO",
            "HashMapStringMO",
            "HashSetStringMO",
            "JsonStringMO",
            "DatabaseMutationOperator",
            "deadLockTest",
            "newFileNull",
            "staticVariable"
        ]

        self.instance_to_operator = {
            "RaceConditionMutationOperator":"RCMO",
            "deadLockTest" : "DLMO",
            "MapIUCMO" : "IUCMO",
            "SetUCIMO" : "UCIMO",
            "MapIVMO" : "IVMO",
            "PathFSFMO" : "FPMO",
            "CustomClassIVMO" : "IVMO",
            "testTimeValueInitializationTwiceMutant" : "TVIMO",
            "HashMapUAMO" : "UAMO",
            "PrimitiveIVMO" : "IVMO",
            "ListIVMO" : "IVMO",
            "HashSetUAMO" : "UAMO",
            "SetIUCMO" : "IUCMO",
            "memoryBoundViolationMO" : "RBVMO",
            "LatchObjectMO" : "AWMO",
            "ThreadSleepMO" : "AWMO",
            "VoidLatchAwaitMO" : "AWMO",
            "CaffeineCDMO" : "CMO",
            "TypedQueryDMO" : "FPMO",
            "FileObjectFMO" : "FPMO",
            "FileOutputStreamFSFMO" : "FPMO",
            "RandomAccessFileFSFMO" : "FPMO",
            "StringFileWriterFSFMO" : "FPMO",
            "TempFileFSFMO" : "FPMO",
            "MockitoMutationOperator:" : "FWPMO",
            "jettyServerSetNonHardcodedPortMO" : "HPMO",
            "SetIVMO" : "IVMO",
            "WrapperIVMO" : "IVMO",
            "GlobalTMO" : "TMO",
            "newFileNullMO" : "RNCMO",
            "LocalTMO" : "TMO", 
            "jettyServerSetHardcodedPortMO" : "HPMO",
            "TRRInjectAssertLocal" : "TRRMO", 
            "UnorderedPopulationMutationOperator" : "UPMO",
            "TimeZoneDependencyMO" : "TZDMO",
            "WIPTRRInjectAssertInstance" : "TRRMO",
            "JsonUAMO" : "UAMO",
            "JsonUCIMO" : "UCIMO",
            "JsonIUCMO" : "IUCMO",
            "HashMapStringMO" : "SMO",
            "HashSetStringMO" : "SMO",
            "JsonStringMO" : "SMO",
            "TMO":"TMO",
            "DatabaseMutationOperator":"DMO",
            "newFileNull":"RNCMO",
            "staticVariable":"SVPMO",
        }

        self.mutation_operators = sorted(set(self.instance_to_operator.values()))

    def create_frequency_dict(self, list_of_elements):
        mydict = OrderedDict()
        
        for elem in list_of_elements:
            mydict[elem] = 0
        
        return mydict


    
    def parse(self, log_file_path):
        file_contents = list()
        frequency = self.create_frequency_dict(self.mutation_operator_instances)

        capture = False
        if self.start_string == None:
            capture = True

        with open(log_file_path, "r") as f:
            file_contents = f.readlines()
        
        ansi_re = re.compile(r'\x1b\[[0-9;]*m')
        for line in file_contents:
            line = re.sub(ansi_re, '', line)
            if not capture:
                if self.start_string in line:
                    capture = True
            
            elif self.end_string != None and self.end_string in line:
                    capture = False
                
            else:
                for instance in self.mutation_operator_instances:
                    if instance in line and self.is_rule_satisfied(line):
                            frequency[instance] += 1
        
        mutation_operator_frequency = self.create_frequency_dict(self.mutation_operators)

        for key in frequency.keys():
            mutation_operator_frequency[self.instance_to_operator[key]] += frequency[key]
        
        return mutation_operator_frequency
    
    @abstractmethod
    def is_rule_satisfied(self, line):
        pass
    
class Nondex_Parser(Parser):
    def __init__(self):
        super().__init__(
            start_string="Across all seeds:",
            end_string="Test results can be found at:"
        )
    def is_rule_satisfied(self, line):
        return True

class Surefire_Parser(Parser):
    def __init__(self):
        super().__init__(
            start_string="[WARNING] Flakes: ",
            end_string="[ERROR] Tests run: "
        )
    def is_rule_satisfied(self, line):
        return "[WARNING]" in line


class Count_Parser(Parser):
    def __init__(self):
        super().__init__(
            start_string="[ERROR] Failures: ",
            end_string="[ERROR] Errors: "
        )
    def is_rule_satisfied(self, line):
        return True

class Count_After_Error_Parser(Parser):

    def __init__(self):
        self.mutation_operator_instances = [
            "jettyServerSetHardcodedPortMO",
            "GlobalTMO",
            "LocalTMO",
            "deadLockTest"
        ]
    def __init__(self):
        super().__init__(
            start_string="[ERROR] Errors: ",
            end_string=None
        )
    def is_rule_satisfied(self, line):
        return True
    

class iDFlakies_Parser(Parser):
    def __init__(self):
        super().__init__(
            start_string=None,
            end_string=None
        )
    def is_rule_satisfied(self, line):
        return True


def instance_to_frequency(parser, dirpath, file, file_path):

    
    tool = os.path.splitext(file)[0].split("_")[0]
    project_name = os.path.splitext(file)[0].split("_")[1]
    threshold = os.path.splitext(file)[0].split("_")[2]

    if not isinstance(parser, Count_After_Error_Parser):
        print(f"threshold: {threshold} - project: {project_name} - tool {tool}")

    frequency = parser.parse(file_path)

    return frequency
    

def parse(dirpath, file, parser):
    file_path = os.path.join(dirpath, file)
    file = file_path.split("/")[-1]

    frequency = instance_to_frequency(parser, dirpath, file, file_path)
    mutation_operators = frequency.keys()

    if isinstance(parser, Count_Parser):
        error_parser = Count_After_Error_Parser()
        error_frequency = instance_to_frequency(error_parser, dirpath, file, file_path)


        for key in error_frequency.keys():
            frequency[key] += error_frequency[key]

    for key in frequency.keys():
        print(key, end=" ")
    print()

    for mutation_operator in mutation_operators:
        print(frequency[mutation_operator], end=" ")
    print("\n")

def main():

    change_working_dir()

    projects = util.get_immediate_subdirectories(".")
    projects = ["commons-csv_od_experimentation"]

    nondex_parser = Nondex_Parser()
    surefire_parser = Surefire_Parser()
    count_parser = Count_Parser()
    idflakies_parser = iDFlakies_Parser()

    

    for project in projects:
        for dirpath, _,files in os.walk(project):
            for file in files:
                # if "nondex" in file and os.path.splitext(file)[1] == ".log" and "detection_results" in dirpath:
                #     parse(dirpath, file, nondex_parser)
                
                if "count" in file and os.path.splitext(file)[1] == ".log" and "detection_results" in dirpath:
                    parse(dirpath, file, count_parser)
                
                # elif "surefire" in file and os.path.splitext(file)[1] == ".log" and "detection_results" in dirpath:
                #    parse(dirpath, file, surefire_parser)

                # elif "idflakies" in file and os.path.splitext(file)[1] == ".log" and "detection_results" in dirpath:
                #     parse(dirpath, file, idflakies_parser)

if __name__ == "__main__":
    os.environ["WORKING_DIR"] = "./4_croissant_mutants"
    main()
    
    