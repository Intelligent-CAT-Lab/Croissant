rounds=$1
seed=$2
#2.0.0
mvn edu.illinois.cs:idflakies-maven-plugin:2.0.1-SNAPSHOT:detect \
  -Denforcer.skip -Dmaven.antrun.skip -Dcheckstyle.skip -Drat.skip -Dmaven.test.skip \
  -Ddetector.detector_type=random-class-method -Ddt.detector.original_order.all_must_pass=false \
  -Ddt.detector.roundsemantics.total -Ddt.randomize.rounds=$rounds -Ddt.verify.rounds=0 -Ddt.seed=$seed
