input=$1

#mkdir -p ./mutants

for info in $(cat $input); do
    url=$(echo $info | cut -d, -f1)
    sha=$(echo $info | cut -d, -f2)
    project=${url##*/}

    #cd mutants
    git clone ${url}
    cd ${project}
    git checkout ${sha}
    cd ../..
    done
