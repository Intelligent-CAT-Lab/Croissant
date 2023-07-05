apt-get update
apt-get -y install openjdk-8-jdk
apt update
apt install -y maven
apt install -y software-properties-common
add-apt-repository ppa:deadsnakes/ppa -y
apt install python3.8 -y
apt install git-all -y

update-alternatives --install /usr/bin/python3 python3 /usr/bin/python3.8 1
update-alternatives  --set python3 /usr/bin/python3.8

apt install python3-tk -y
apt install python3-pip -y
pip3 install BeautifulSoup4
pip3 install pandas