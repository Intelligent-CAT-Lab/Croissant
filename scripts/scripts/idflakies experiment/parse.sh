
names=(
    "csud"
    "dsd"
    "tpfd"
    "ra"
    "svd"
)

tests=(
    "com.example.UselessTest.a_brittlecacheTestCaffeineCDMO"
    "com.example.UselessTest.a_PolluterMutanttestDBDatabaseMutationOperator"
    "com.example.UselessTest.b_victim_mockitotestMockitoMockitoMutationOperator"
    "com.example.UselessTest.b_victim_NewFileNull_fileTestnewFileNullODMO"
    "com.example.UselessTest.b_staticVariableVictim_fieldClass"
)


for i in "${!names[@]}"; do
    export SUBJECT_NAME=${names[i]}
    export TEST_NAME=${tests[i]}
    echo ${names[i]} ${tests[i]}
    python3 parse.py 
done
