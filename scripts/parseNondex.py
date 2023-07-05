import csv
import sys

headers=['Project','Sha','Test Class','Timestamp','Threshold','Build Result','Total Time','Num of Test detected','Tests Detected']

def parseNondexLog(project,sha,testClass,timeStamp,nondexLog,testClassResultcsv,thres,timeoutLog):
    buildTag = None
    totalTime = 0
    detectedTestsNum = 0
    detectedTests = []
    result = {}

    file = open(nondexLog,"r")
    lines = file.read().splitlines()
    file.close()

    timeoutfile = open(timeoutLog,"r")
    timeout = timeoutfile.read()
    timeoutfile.close()
    if testClass in timeout:
        result = {
        'Project': project,
        'Sha': sha,
        'Test Class': testClass,
        'Timestamp': timeStamp,
        'Threshold': thres,
        'Build Result': 'Timeout',
        'Total Time' :0,
        'Num of Test detected' :0,
        'Tests Detected' :0
        }
        
        with open(testClassResultcsv,'a', newline='',encoding='utf-8') as f:
            writer = csv.DictWriter(f,fieldnames=headers)
            writer.writerow(result)
        
        return

    acrossindex = max(lines.index(line) for line in lines if 'Across all seeds:' in line)
    foundIndex = max(lines.index(line) for line in lines if 'Test results can be found at:' in line)
    
    for each  in lines[acrossindex+1:foundIndex]:
        detectedTests.append(each.split(' ')[-1])
    
    detectedTestsNum = len(detectedTests)

    for line in lines:
        if "BUILD FAILURE" in line:
            buildTag = "BUILD FAILURE"
        if "BUILD SUCCESS" in line:
            buildTag = "BUILD SUCCESS"
        if "Total time" in line:
            totalTime = line.split(' ')[-2]

    result = {
        'Project': project,
        'Sha': sha,
        'Test Class': testClass,
        'Timestamp': timeStamp,
        'Threshold': thres,
        'Build Result': buildTag,
        'Total Time' : totalTime,
        'Num of Test detected' : detectedTestsNum,
        'Tests Detected' :detectedTests
    }

    with open(testClassResultcsv,'a', newline='',encoding='utf-8') as f:
        writer = csv.DictWriter(f,fieldnames=headers)
        writer.writerow(result)

if __name__ == "__main__":
    args = sys.argv[1:]

    project = args[0]
    sha = args[1]
    testClass = args[2]
    timeStamp = args[3]
    nondexLog = args[4]
    testClassResultcsv = args[5]
    thres = args[6]
    timeoutLog = args[7]

    parseNondexLog(project,sha,testClass,timeStamp,nondexLog,testClassResultcsv,thres,timeoutLog)