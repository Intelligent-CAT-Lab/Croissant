

#!/usr/bin/env bash

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

