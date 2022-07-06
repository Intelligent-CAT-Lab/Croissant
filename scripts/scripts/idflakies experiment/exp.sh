
names=(
    "svd"
)
for name in ${names[@]}; do
    export SUBJECT_NAME=$name
    echo $name
    python3 idflakies_experiment.py 
done

