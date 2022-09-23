current=$(date "+%Y-%m-%d %H:%M:%S")  
timeStamp=$(date -d "$current" +%s)
currentTimeStamp=$((timeStamp*1000+$(date "+%N")/1000000)) 

mkdir -p logs
mkdir -p ./logs/$currentTimeStamp

getAllTestsPath=$(pwd)/getAllTests.py
addDependenciesPath=$(pwd)/updateDependencies.py
csvlogPath=$(pwd)/logs
logPath=$(pwd)/logs/$currentTimeStamp
mutationPath=$(pwd)/mutation_testing
echo logs:$logPath

exec 3>&1 4>&2
trap $(exec 2>&4 1>&3) 0 1 2 3
exec 1>$logPath/$currentTimeStamp.log 2>&1

echo STARTING at $(date)


#bash clone.sh url.csv

flakeRates=("0.10" "0.25" "0.50" "0.75" "1.00")
projects=(
#    "authy-java"
#    "cayenne"
    "commons-csv"
#     "commons-cli"
#     "commons-codec"
#    "commons-collections"
#    "jackson-core"
#    "commons-compress"
#    "jenkins"
#    "dropwizard"
#    "commons-math"
#    "dropwizard"
#    "jsoup"
#    "junit-quickcheck"
)

mkdir -p ./mutants
cd ./mutants
for project in ${projects[@]}; 
do
    for flakeRate in ${flakeRates[@]};
    do
	bash ../clone.sh ../url.csv
        dir=${flakeRate/./}
        mkdir -p $dir
        mv ./$project ./$dir/
        cd ./$dir/$project
        projectPath=$(pwd)

        echo flakeRate,Project,sha,testClass,Total_Number_of_Mutants,timestamp >>$logPath/result$project$dir.csv
        echo flakeRate,Project,sha,testClass,Pass,Error,Skip,Flake >>$logPath/surefire$project$dir.csv
        echo flakeRate,Project,sha,testClass,Number >>$logPath/nondex$project$dir.csv

        mvn install -Drat.skip -Dcheckstyle.skip -Denforcer.skip=true |tee $logPath/install${project}${dir}.log
        python3.8 $getAllTestsPath $(pwd) 
        python3.8 $addDependenciesPath
        cd $mutationPath

        all_test_classes=""
        for info in `(cat $csvlogPath/all_tests_${project}.csv)`
        do
            module=$(echo $info | cut -d, -f1)
            testClass=$(echo $info | cut -d, -f2)
            if [ ${testClass::-1} != 'org.apache.commons.csv.CSVPrinterTest' ] && [ ${testClass::-1} != 'org.apache.commons.csv.CSVRecordTest' ]; then
            echo -------------${testClass::-1} mutation-------------
            echo $(pwd)
            echo ${module::-1}
            timeout 1200s mvn exec:java -Dexec.mainClass=com.framework.App -Dexec.args="-dir ${module::-1}/test-classes -o ${module::-1}/test-classes -t $flakeRate -n ${testClass::-1} -j" |tee $logPath/mutation${testClass::-1}$currentTimeStamp$dir.log
            exit_status=${PIPESTATUS[0]}
            if [[ ${exit_status} -eq 124 ]] || [[ ${exit_status} -eq 137 ]]; then
            echo -------------${testClass::-1} mutation timeout-------------
            echo ${testClass::-1} >>$logPath/timeout.log
            continue
            fi

            cd $projectPath
            echo -------------${testClass::-1} surefire-------------
            mvn surefire:test -Drat.skip -Denforcer.skip=true -Dcheckstyle.skip -Dsurefire.rerunFailingTestsCount=3 -Dtest=${testClass::-1} |tee $logPath/${dir}surefire${testClass::-1}$currentTimeStamp.log
            echo -------------${testClass::-1} nondex---------------
            mvn edu.illinois:nondex-maven-plugin:nondex -Dtest=${testClass::-1} -Drat.skip -Denforcer.skip=true -Dcheckstyle.skip |tee $logPath/${dir}nondex${testClass::-1}$currentTimeStamp.log
            cd $mutationPath

            # parse the results
	    fi

        done

    done

done


echo ENDING at $(date)
