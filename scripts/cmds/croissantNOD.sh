module=$1
testClass=$2
junit=$3

if [ $junit == '4' ]; then
    timeout 1200s mvn exec:java -Dexec.mainClass=com.framework.Croissant -Dexec.args="-dir ${module}/test-classes -o ${module}/test-classes -t 1 -n ${testClass} -all_nod_id"
else
    timeout 1200s mvn exec:java -Dexec.mainClass=com.framework.Croissant -Dexec.args="-dir ${module}/test-classes -o ${module}/test-classes -t 1 -n ${testClass} -all_nod_id -j"
fi
