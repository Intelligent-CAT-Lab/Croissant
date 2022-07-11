#!/usr/bin/env bash
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
exec 1>$logPath/setupEnv$currentTimeStamp.log 2>&1

echo VERSION $(git rev-parse HEAD)
echo STARTING at $(date)

#   setup Java 8
sudo apt-get update
sudo apt-get -y install openjdk-8-jdk

#   setup Maven
sudo apt update
sudo apt install -y  maven

#   setup Python
sudo apt update
sudo apt install -y software-properties-common
sudo add-apt-repository ppa:deadsnakes/ppa -y
sudo apt update
sudo apt install python3.8 -y

#   setup Git
sudo dnf install git-all 
sudo apt install git-all -y

echo ENDING at $(date)
