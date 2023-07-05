import csv
import os
import sys

Fileds = ['SVD','TPFD','CSD','DSD','FPD','RA','MD','PD','STDZ','AW','CTD','RC','UCIA','UCC','RAM','IVD','STDV','TRR','NoOpKey']
headers = ['Project','Sha','Test Class','Original Test Num','Timestamp','Build Result','Total Time','Only Generation Time','Total Mutant Num','SVD','TPFD','CSD','DSD','FPD','RA','MD','PD','STDZ','AW','CTD','RC','UCIA','UCC','RAM','IVD','STDV','TRR','NoOpKey','Mutant with Time']

#headers=['Project','Sha','Test Class','Timestamp','Build Result','Total Time','Total Mutant Num','CaffeineCDMO','CustomClassIVMO','DatabaseMutationOperator','FileNullODMO','FileObjectFMO','FileOutputStreamFSFMO','GlobalTMO','HashMapStringMO','HashMapUAMO','HashSetStringMO','HashSetUAMO','JsonIUCMO','JsonStringMO','JsonUAMO','JsonUCIMO','LatchObjectMO','ListIVMO','LocalTMO','MapIUCMO','MapIVMO','MockitoMutationOperator','PathFSFMO','PrimitiveIVMO','RandomAccessFileFSFMO','SetIUCMO','SetIVMO','SetUCIMO','StringFileWriterFSFMO','TRRInjectAssertLocal','TempFileFSFMO','ThreadSleepMO','TimeValueInitializationMutationOperator','TimeZoneDependencyMO','UnorderedPopulationMutationOperator','VoidLatchAwaitMO','WIPTRRInjectAssertInstance','WrapperIVMO','jettyServerSetHardcodedPortMO','jettyServerSetNonHardcodedPortMO','memoryBoundViolationMO','newFileNullODMO']

def getMutantNum(project,sha,testClass,originNum,timeStamp,testClassMutationLog,testClassResultcsv):
    result=dict()
    buildTag = 'None'
    totalMutantNum = 0
    totalTime = 0
    mutationOpDict = dict()
    generationTime = 0

    f = open(testClassMutationLog,'r')
    lines = f.read().splitlines()
    f.close()

    acrossindex = max(lines.index(line) for line in lines if 'All mutants' in line)
    foundIndex = max(lines.index(line) for line in lines if 'Generation Time Only:' in line)
   
    for line in lines:
        if 'BUILD SUCCESS' in line:
            buildTag = 'BUILD SUCCESS'
        if 'BUILD FAILURE' in line:
            buildTag = 'BUILD FAILURE'
        if "Total Number of Mutants generated" in line:
            totalMutantNum = int(line.split('[0m')[-1])
        if "Total Time of Mutation" in line:
            totalTime = line.split(' ')[-2]
        if "Generation Time Only" in line:
            generationTime = int(line.split('[0m')[-1])
        if "Number of each Mutant Operator:" in line:
            allOpNum = line.split('[0m{')[-1].strip('}').split(',')
            for eachOp in allOpNum:
                opKey = eachOp.split('=')[0].strip()
                opVal = int(eachOp.split('=')[1])
                if opKey not in mutationOpDict:
                    mutationOpDict[opKey] = opVal
                else:
                    mutationOpDict[opKey] += opVal

    commonPart={
        'Project':project,
        'Sha':sha,
        'Test Class':testClass,
        'Original Test Num': originNum,
        'Timestamp':timeStamp,
        'Build Result':buildTag,
        'Total Time': totalTime,
        'Total Mutant Num': totalMutantNum,
        'Only Generation Time':generationTime,
        'Mutant with Time':lines[acrossindex+1:foundIndex]
    }
    for eachField in Fileds:
        if eachField not in mutationOpDict.keys():
            mutationOpDict[eachField] = 0

    result = dict(commonPart, **mutationOpDict)
    

    with open(testClassResultcsv, 'a', newline='',encoding='utf-8') as f:
        print(result)
        writer = csv.DictWriter(f,fieldnames=headers)
        writer.writerow(result)

if __name__ == "__main__":
    args = sys.argv[1:]

    project = args[0]
    sha = args[1]
    testClass = args[2]
    originNum = args[3]
    timeStamp = args[4]
    testClassMutationLog = args[5]
    testClassResultcsv = args[6]
    
    getMutantNum(project,sha,testClass,originNum,timeStamp,testClassMutationLog,testClassResultcsv)
