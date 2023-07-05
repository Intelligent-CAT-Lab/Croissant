module=$1
testClass=$2
mo=$3
tm=$4
junit=$5

if [ $junit == '4' ]; then
    timeout 1200s mvn exec:java -Dexec.mainClass=com.framework.Croissant -Dexec.args="-dir ${module}/test-classes -o ${module}/test-classes -t 1 -n ${testClass} -mo $mo -tm $tm"
else
    timeout 1200s mvn exec:java -Dexec.mainClass=com.framework.Croissant -Dexec.args="-dir ${module}/test-classes -o ${module}/test-classes -t 1 -n ${testClass} -mo $mo -tm $tm -j"
fi

#-mo newFileNullODMO -tm FileTemplate
#-mo CaffeineCDMO -tm CacheTemplate
