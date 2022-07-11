threshold_value=$1

current=$(date "+%Y-%m-%d %H:%M:%S")  
timeStamp=$(date -d "$current" +%s)
currentTimeStamp=$((timeStamp*1000+$(date "+%N")/1000000)) 

currentPath=$(pwd)
echo $currentPath
cd ../../../
mkdir -p logs
cd logs
logPath=$(pwd)
echo $logPath

cd $currentPath
exec 3>&1 4>&2
trap $(exec 2>&4 1>&3) 0 1 2 3
exec 1>$logPath/runExperiment$currentTimeStamp.log 2>&1

echo VERSION $(git rev-parse HEAD)
echo STARTING at $(date)

python3 experiment.py $1

echo ENDING at $(date)