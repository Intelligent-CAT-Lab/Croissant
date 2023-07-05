testClass=$1
submodule=$2
mvn surefire:test -Drat.skip -Denforcer.skip=true -Dcheckstyle.skip -Dspotbugs.skip -Dsurefire.rerunFailingTestsCount=5 -Dtest=${testClass} -pl ${submodule} -Djacoco.skip 
