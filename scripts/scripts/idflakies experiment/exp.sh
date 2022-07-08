current=`date "+%Y-%m-%d %H:%M:%S"`  
timeStamp=`date -d "$current" +%s`
currentTimeStamp=$((timeStamp*1000+`date "+%N"`/1000000)) 

if  [ ! -d  "logs"  ]; then
mkdir logs
fi    
echo VERSION $(git rev-parse HEAD) >>./logs/$currentTimeStamp.log
echo STARTING at $(date) >>./logs/$currentTimeStamp.log

names=(
    "svd"
)
for name in ${names[@]}; do
    export SUBJECT_NAME=$name
    echo $name
    python3 idflakies_experiment.py >>./logs/$currentTimeStamp.log 
done
echo ENDING at $(date) >>./logs/$currentTimeStamp.log

