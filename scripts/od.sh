urlshaCsv=$1
mo=$2
tm=$3

timeStamp=$(echo -n $(date "+%Y-%m-%d %H:%M:%S") | shasum | cut -f 1 -d " ")

export JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF8
export MAVEN_OPTS="-noverify"

mkdir -p ./output
mkdir -p ./output/$timeStamp
mkdir -p ./output/$timeStamp/logs
mkdir -p ./output/$timeStamp/results
mkdir -p ./output/$timeStamp/mutants
mkdir -p ./mutants
mkdir -p ./mutants/$timeStamp
mkdir -p ./copyMutants
mkdir -p ./copyMutants/$timeStamp
mkdir -p ./copyMutantsiDF
mkdir -p ./copyMutantsiDF/$timeStamp

cmdsdir=$(pwd)/cmds
cloneScriptPath=$(pwd)/clone.sh
projectsToRun=$(pwd)/projects
getAllTestsScriptPath=$(pwd)/getAllTests.py
addDependenciesScriptPath=$(pwd)/updateDependencies.py
mutationToolDir=$(pwd)/../mutation_testing
parseMutantSinglePath=$(pwd)/parseMutantSingleTest.py
parseSurefirePath=$(pwd)/parseSurefire.py
parseNondexPath=$(pwd)/parseNondex.py
parseiDFlakiesPath=$(pwd)/parseiDFlakiesMul.py
changeThresholdPath=$(pwd)/changeThresh.py 
changeCountPath=$(pwd)/changeCount.py
logDir=$(pwd)/output/$timeStamp/logs
resultDir=$(pwd)/output/$timeStamp/results
mutantInDir=$(pwd)/output/$timeStamp/mutants
csvDir=$(pwd)/output # dir of csv files for all tests
mutantsDir=$(pwd)/mutants/$timeStamp
copyMutants=$(pwd)/copyMutants/$timeStamp
copyMutantsiDF=$(pwd)/copyMutantsiDF/$timeStamp
skipTests=$(pwd)/exceptions.csv
thresholds=("0.10" "0.20" "0.30" "0.40" "0.50" "0.60" "0.70" "0.80" "0.90" "1.00")
counts=(0 10 20 30 35 40 45 50)
repeat=(1 2 3 4 5)

exec 3>&1 4>&2
trap $(exec 2>&4 1>&3) 0 1 2 3
exec 1>$logDir/$timeStamp.log 2>&1

echo Logs:$logDir
echo STARTING at $(date)
git rev-parse HEAD

#cp -r $projectsToRun/* $mutantsDir
bash $cloneScriptPath $urlshaCsv $mutantsDir


# $1=${testClass}

for info in $(cat $urlshaCsv); do
    project=$(echo $info | cut -d, -f1 | sed 's/.*\///')
    sha=$(echo $info | cut -d, -f2)
    junit=$(echo $info | cut -d, -f3)
    cd $mutantsDir/$project

    bash $cmdsdir/install.sh |tee $logDir/install$project.log
    python3 $getAllTestsScriptPath $(pwd) $csvDir
    #python3 $addDependenciesScriptPath

    echo Project,Sha,Test Class,Original Test Num,Timestamp,Build Result,Total Time,Only Generation Time,TotalNum,SVD,TPFD,CSD,DSD,FPD,RA,MD,PD,STiD,ASW,CT,RC,UCIA,UCC,RAM,IVD,TVIMO,TRRMO,NoOpKey,Mutant with Time >$resultDir/mutantResult_${project}_${timeStamp}.csv
    echo Project,Sha,Test Class,Timestamp,Threshold,Build Result,Total Time,Num of Test detected,Tests Detected >$resultDir/nondexResult_${project}_${timeStamp}.csv
    echo Project,Sha,Test Class,Timestamp,Threshold,Build Result,Total Time,Runs,Failures,Errors,Skipped,Flakes,Flaky Tests  >$resultDir/surefireResult_${project}_${timeStamp}.csv

    echo Project,Sha,Timestamp,Threshold,Build Result,Total Time,Num of Test detected,Tests Detected >$resultDir/nondexResult_${timeStamp}.csv
    echo Project,Sha,Timestamp,Build Result,Total Time,Num of Test detected,Tests Detected,cleanerCounts >$resultDir/idflakiesResult_${timeStamp}.csv
    echo Project,Sha,Timestamp,Threshold,Build Result,Total Time,Runs,Failures,Errors,Skipped,Flakes,Flaky Tests >$resultDir/surefireResult_${timeStamp}.csv
    echo Test class,log >$logDir/timeout.log


    for testInfo in `(cat $csvDir/all_tests_${project}.csv)`
    do
	    cd $mutationToolDir
        module_=$(echo $testInfo | cut -d, -f1)
        testClass=$(echo $testInfo | cut -d, -f2)
        originNum=$(echo $testInfo | cut -d, -f3)
        module=${module_::-1}
        deadlockTestClass=${testClass}_DeadLockMutationOperator_Test
        raceconditionTestClass=${testClass}_RaceConditionMutationOperator_Test
        #testClass=${testClass_::-1}
        echo $module $testClass originNum
        if grep -Fxq "${testClass}" $skipTests
        then 
        echo ====================================${testClass} was categorized as abnormal, skip mutation================
            continue #if found
    
        else
        echo ===========================================${testClass} mutation===========================================
        bash $cmdsdir/croissantSingle.sh ${module} ${testClass} ${mo} ${tm} ${junit} |tee $logDir/mutation_${testClass}_${timeStamp}.log
        exit_status=${PIPESTATUS[0]}
        if [[ ${exit_status} -eq 124 ]] || [[ ${exit_status} -eq 137 ]]; then
            echo ==========================================${testClass} mutation TIMEOUT========================================
            echo ${testClass},$logDir/mutation_${testClass}_${timeStamp}.log >>$logDir/timeout.log
            continue
        fi

        if grep -q "BUILD SUCCESS" $logDir/mutation_${testClass}_${timeStamp}.log 
        then
	    echo "GOOD"
        python3 $parseMutantSinglePath $project $sha $testClass $originNum $timeStamp $logDir/mutation_${testClass}_${timeStamp}.log $resultDir/mutantResult_${project}_${timeStamp}.csv

        else 
            echo ====================================${testClass} BUILD FAILURE===============================================================
        fi      
        fi
    
    done
    
    cp -r $mutantsDir/$project $copyMutants
    cp -r $mutantsDir/$project $copyMutantsiDF
    
    cd $copyMutantsiDF/$project
    for repeatTime in ${repeat[@]}; do
        mkdir -p ${logDir}/${repeatTime}
        for eachCount in ${counts[@]}; do
            python3 $changeCountPath ${copyMutantsiDF}/$project $eachCount
            countDir=${eachCount}
            mkdir -p ${logDir}/${repeatTime}/dtfixings${countDir}
            seed=$[$repeatTime*41444]
            echo =========================================== Seed =${seed} ${project} iDFlakies ${eachCount} cleaners===========================================
            bash $cmdsdir/idflakies.sh 5 $seed |tee ${logDir}/${repeatTime}/dtfixings${countDir}/idflakiesResult_${project}_${timeStamp}.log
            python3 $parseiDFlakiesPath $project $sha $timeStamp ${logDir}/${repeatTime}/dtfixings${countDir}/idflakiesResult_${project}_${timeStamp}.log $resultDir/idflakiesResult_${timeStamp}.csv $eachCount
	        md5sum ${copyMutantsiDF}/${project}/.dtfixingtools/original-order
            cat ${copyMutantsiDF}/${project}/.dtfixingtools/detection-results/list.txt
            mv ${copyMutantsiDF}/${project}/.dtfixingtools/ ${logDir}/${repeatTime}/dtfixings${countDir}
        done
    done
    
    cp -r $mutantsDir/$project $mutantInDir
    

done

echo ENDING at $(date)
