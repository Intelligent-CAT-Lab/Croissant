current=$(date "+%Y-%m-%d %H:%M:%S")  
timeStamp=$(date -d "$current" +%s)
currentTimeStamp=$((timeStamp*1000+$(date "+%N")/1000000)) 

exec 3>&1 4>&2
trap $(exec 2>&4 1>&3) 0 1 2 3
exec 1>$logPath/idflakies_exp$currentTimeStamp.log 2>&1

echo VERSION $(git rev-parse HEAD)
echo STARTING at $(date)

currentPath=$(pwd)
echo $currentPath
cd ../../../
mkdir -p logs
cd logs
logPath=$(pwd)
echo $logPath

cd $currentPath
names=(
    "svd"
)
for name in ${names[@]}; do
    export SUBJECT_NAME=$name
    echo $name
    python3 idflakies_experiment.py
done
echo ENDING at $(date)
