import csv
import re
import sys
from statistics import mean

headers=['Project','Sha','Timestamp','Build Result','Total Time','Num of Test detected','Tests Detected','Module','cleanerCounts']

def parseiDFlakiesLog(project,sha,timeStamp,idflakiesLog,Resultcsv,cleanerCounts):
    buildTag = None
    totalTime = 0
    detectedTestsNum = 0
    detectedTests = []
    detectionFile = None
    result = {}
    module=''


    file = open(idflakiesLog,"r")
    lines = file.read().splitlines()
    file.close()

    for line in lines:
        if "BUILD FAILURE" in line:
            buildTag = "BUILD FAILURE"
        if "BUILD SUCCESS" in line:
            buildTag = "BUILD SUCCESS"
        if "Total time" in line:
            totalTime = line.split(' ')[-2]

    for line in lines:
        if "writing list to" in line:
            detectedTestsNum = line.split(' ')[2]
            detectionFile = line.split(' ')[7]

        if detectionFile:
            module = '/'.join(detectionFile.split('copyMutantsiDF/')[1].split('/.dtfixingtools')[0].split('/')[1:])
            with open(detectionFile,'r') as file:
                detectedTests = file.read().splitlines()
            
            result = {
            'Project': project,
            'Sha': sha,
            'Timestamp': timeStamp,
            'Build Result': buildTag,
            'Total Time' : totalTime,
            'Num of Test detected' : detectedTestsNum,
            'Tests Detected' : detectedTests,
            'Module' : module,
            'cleanerCounts':cleanerCounts
            }

            with open(Resultcsv,'a', newline='',encoding='utf-8') as f:
                writer = csv.DictWriter(f,fieldnames=headers)
                writer.writerow(result)
            detectionFile = None

if __name__ == "__main__":
    args = sys.argv[1:]

    project = args[0]
    sha = args[1]
    timeStamp = args[2]
    idflakiesLog = args[3]
    Resultcsv = args[4]
    cleanerCounts = args[5]
    
    parseiDFlakiesLog(project,sha,timeStamp,idflakiesLog,Resultcsv,cleanerCounts)