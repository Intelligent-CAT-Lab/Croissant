testClass=$1
submodule=$2
mvn edu.illinois:nondex-maven-plugin:2.1.1:nondex -Dtest=${testClass} -pl ${submodule} -Drat.skip -Dspotbugs.skip -Denforcer.skip=true -Dcheckstyle.skip -Djacoco.skip -DnondexRuns=5
