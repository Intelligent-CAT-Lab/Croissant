input=$1
dir=$2

mkdir -p ./mutants

for info in $(cat $input); do
    url=$(echo $info | cut -d, -f1)
    sha=$(echo $info | cut -d, -f2)
    project=${url##*/}

    cd $dir
    git clone ${url}
    cd ${project}
    git checkout ${sha}
    cd ../..
    done
