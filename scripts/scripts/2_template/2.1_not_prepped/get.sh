current=$(date "+%Y-%m-%d %H:%M:%S")  
timeStamp=$(date -d "$current" +%s)
currentTimeStamp=$((timeStamp*1000+$(date "+%N")/1000000)) 

currentPath=$(pwd)
echo $currentPath
cd ../../../../
mkdir -p logs
cd logs
logPath=$(pwd)
echo $logPath

cd $currentPath
exec 3>&1 4>&2
trap $(exec 2>&4 1>&3) 0 1 2 3
exec 1>$logPath/cloneRepos$currentTimeStamp.log 2>&1

echo VERSION $(git rev-parse HEAD)
echo STARTING at $(date)

# where is the work derectory for these projects?
# mkdir -p projects
# cd projects

projects=(
    "https://github.com/apache/commons-csv.git"
    "https://github.com/apache/commons-cli.git"
    "https://github.com/FasterXML/jackson-core.git"
    "https://github.com/jhy/jsoup.git"
    "https://github.com/google/gson.git"
    "https://github.com/apache/commons-codec.git"
    "https://github.com/apache/commons-compress.git"
)
for project in ${projects[@]}; do
    repo_pre=${project##*/}
    repo=${repo_pre%%.*}
    echo $repo
    if [ -x "$repo" ]; then
        echo $repo cloned.
	continue
    fi

    #keep attemps to clone until success when meet network issues
    while($true);do
        git clone ${project}
        if [[ $? ]]
        then
            break
        fi
    done

done


echo ENDING at $(date)

