import csv
import sys

headers=['Project','Sha','Test Class','Timestamp','Threshold','Build Result','Total Time','run','Failures','Errors','Skipped','Flakes','Flaky Tests']
def parseSurefireLog(project,sha,testClass,timeStamp,surefireLog,testClassResultcsv,thres,timeoutLog):
    buildTag = None
    totalTime = 0
    detectedTests = []

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
        'Total Time' : 0,
        'run':0,
        'Failures':0,
        'Errors':0,
        'Skipped':0,
        'Flakes':0,
        'Flaky Tests':detectedTests
        }
        
        with open(testClassResultcsv,'a', newline='',encoding='utf-8') as f:
            writer = csv.DictWriter(f,fieldnames=headers)
            writer.writerow(result)
        
        return

    testResultDict = dict()

    file = open(surefireLog,"r")
    lines = file.read().splitlines()
    file.close()

    flakeTag = 0

    for line in lines:
        if 'Flakes:' in line:
            flakeTag = 1

    if flakeTag == 1:
        acrossindex = min(lines.index(line) for line in lines if 'Flakes:' in line)
        foundIndex = max(lines.index(line) for line in lines if 'Finished at:' in line)

        for each in lines[acrossindex+1:foundIndex]:    
            if "WARNING" in each:
                print(each)
                detectedTests.append(each.split('\x1b[1;33m')[-1].replace('\x1b[m',''))

    final_index = max(lines.index(line) for line in lines if 'Tests run:' in line and 'Failures:' in line and 'Errors:' in line and 'Skipped:' in line)
    testResultList = lines[final_index].split('Tests')[-1].replace(' ','').replace('\x1b[m','').split(',')
    for each in testResultList:
        testResultDict[each.split(':')[0]] = each.split(':')[1]
    for line in lines:
        if "BUILD FAILURE" in line:
            buildTag = "BUILD FAILURE"
        if "BUILD SUCCESS" in line:
            buildTag = "BUILD SUCCESS"
        if "Total time:" in line:
            totalTime = line.split(' ')[-2]
    commonPart = {
        'Project':project,
        'Sha':sha,
        'Test Class':testClass,
        'Timestamp':timeStamp,
        'Threshold': thres,
        'Build Result':buildTag,
        'Total Time':totalTime,
        'Flaky Tests':detectedTests
    }
    result = dict(commonPart,**testResultDict)
    for eachField in headers:
        if eachField not in testResultDict.keys():
            testResultDict[eachField] = 0
    
    print(result)

    with open(testClassResultcsv,'a', newline='',encoding='utf-8') as f:
        writer = csv.DictWriter(f,fieldnames=headers)
        writer.writerow(result)

if __name__ == "__main__":
    args = sys.argv[1:]

    project = args[0]
    sha = args[1]
    testClass = args[2]
    timeStamp = args[3]
    surefireLog = args[4]
    testClassResultcsv = args[5]
    thres = args[6]
    timeoutLog = args[7]

    parseSurefireLog(project,sha,testClass,timeStamp,surefireLog,testClassResultcsv,thres,timeoutLog)