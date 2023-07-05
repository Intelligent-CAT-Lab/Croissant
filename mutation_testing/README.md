
# Run the tool of mutation testing

## Environment

<details><summary><b> Requirements </b> <i>[click]</i></summary>
<div>

- Linux OS
- Java 8
- Maven 3.6.3
</div>
</details>

## Quick Start
To mutate a test class with Croissant, please see the following instructions:

<details><summary><b> Install Croissant mutation tool </b> <i>[click]</i></summary>
<div>

```
git clone https://github.com/dserfe/Croissant
cd Croissant/mutation_testing
mvn install
```
</div>
</details>

<details><summary><b> Run Croissant mutation tool </b> <i>[click]</i></summary>
<div>

1. Install a java project to be mutated.
   First, one can select a java project with Junit 4 or Junit 5 tests; then select a test class to mutate in the next step. For example, in the following steps, we will mutate a test class `org.apache.commons.csv.LexerTest` from `commons-csv`:
```
git clone https://github.com/apache/commons-csv
cd commons-csv
mvn install
```
2. run the tool
   One can run croissant mutation testing with the following commands:
```
cd Croissant/mutation_testing
mvn exec:java -Dexec.mainClass=com.framework.Croissant -Dexec.args="-dir InputTestClassPath -o OutputTestClassPath -t Threahold -n TestClassName [-mo MutationOperator] [-tm Template] <-j>"
```

Croissant mutation configuration options:

Required configuration:
- `-dir`: the input test class path `InputTestClassPath`
- `-o`: the output test class path `OutputTestClassPath`; In order to run detection tools on mutants, please configure the output path in original project test class path, which is the same as input path `InputTestClassPath`
- `-t`: the default `Threshold` to control NOD and ID test flakiness; `Threshold` can be a value from 0 to 1, a higher value can make a higher possibility that NOD/ID tests can be flaky, if `Threshold` is 0, all tests can pass; if `Threshold` is 1, all tests will fail.
- `-n`: the name of the test class `TestClassName` to mutate
- `-j`: The default mutation is on Junit4 tests. When mutate on Junit5 tests, please add the option `-j`

Optional configuration:

One can generate specific mutants by adding one of the following configuration, otherwise the default commands will generate all mutants:
- `-all_nod_id`: an option to run all NOD and ID mutation operators automatically
- `-all_od`: an option to run all OD mutation operators automatically
- `-mo [MutationOperator] -tm [Template]`: an option to run one specific mutation operator


To control the flakiness of existing mutants with no need to mutate again, one can change `mutation.threshold` to control the flakiness of NOD and ID mutants (the threshold can be from 0 to 1, a higher value can make a higher possibility that NOD/ID tests can be flaky, if the threshold is 0, all tests can pass; if it is 1, all tests will fail); or change `mutation.count` to control the number of cleaners for OD mutants (the number of cleaners can be from 0 to 50, a higher number of cleaners makes a higher possibility that victims can pass, since there is a higher chance that cleaners run between polluter and victim to clean the pollution). The two properties are configured in `target/test-classes/mutation.config`, which can be changed during runtime:
```
mutation.threshold=0.8 # a value from 0 to 1
mutation.count=5 # a value from 0 to 50
```
To run generated mutants with surefire, one can run with Surefire, e.g., to run mutants in test `org.apache.commons.csv.LexerTest` from `commons-csv`:
```
cd commons-csv
mvn surefire:test -Dtest=org.apache.commons.csv.LexerTest
```

Examples:
- An example to get `STDZ` mutants on test `org.apache.commons.csv.LexerTest` from `commons-csv` (Junit5):
```
mvn exec:java -Dexec.mainClass=com.framework.Croissant -Dexec.args="-dir ${path}/commons-csv/target/test-classes -o ${path}/commons-csv/target/test-classes -t 1 -n org.apache.commons.csv.LexerTest -mo TimeZoneDependencyMO -tm TimezoneTemplate -j"
```

- An example to run all NOD/ID mutation operators on test `org.apache.commons.csv.LexerTest` from `commons-csv` (Junit5):
```
mvn exec:java -Dexec.mainClass=com.framework.Croissant -Dexec.args="-dir ${path}/commons-csv/target/test-classes -o ${path}/commons-csv/target/test-classes -t 1 -n org.apache.commons.csv.LexerTest -all_nod_id -j"
```

- An example to run all NOD/ID mutation operators on test `org.apache.commons.cli.UtilTest` from `commons-cli` (Junit4):
```
mvn exec:java -Dexec.mainClass=com.framework.Croissant -Dexec.args="-dir ${path}/commons-cli/target/test-classes -o ${path}/commons-cli/target/test-classes -t 1 -n org.apache.commons.cli.UtilTest -all_nod_id"
```
  
A full list of mutation operators:
| Mutation operators      | mo | tm |
| ----------- | ----------- |----------- |
| STDZ | TimeZoneDependencyMO | TimezoneTemplate |
| STDV | TimeValueInitializationMutationOperator | - |
| MD | memoryBoundViolationMO | - |
| PD | jettyServerSetHardcodedPortMO,jettyServerSetNonHardcodedPortMO | ServerTemplate |
| AW | LatchObjectMO | LatchTemplate,ThreadSleepTemplate |
| CTD | DeadLockMutationOperator | DeadLockTemplate|
| RC | RaceConditionMutationOperator | - |
| UCIA | UnorderedCollectionIndexMutationOperator | - |
| UCC | OrderedStringConversionMutationOperator | - |
| RAM | UnorderedPopulationMutationOperator | ReflectionTemplate |
| TRR | TRRInjectAssertLocal,WIPTRRInjectAssertInstance | - |
| IVD | CustomClassIVMO | InstanceTemplate |
| FPD | FileObjectFMO | FileWriterTemplate |
| CSD | CaffeineCDMO | CacheTemplate |
| SVD | StaticSVMO | StaticTemplate |
| TPFD | MockitoMutationOperator | MockitoTemplate |
| DSD | DatabaseMutationOperator | DatabaseTemplate |
| RA | newFileNullODMO | FileTemplate |

</div>
</details>
