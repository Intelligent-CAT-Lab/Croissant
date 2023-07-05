# Reproduce the results

## Reproduce RQs using existing results

<details><summary><b> 1. Effectiveness of NOD mutants in challenging NonDex and Surefire running with thresholds from 0.1 to 1 </b> <i>[click]</i></summary>
<div>

To generate Figure 1 in the paper, please run the following commands:
```
cd scripts/figures/nod
python3 draw_NOD.py 
```
</div>
</details>

<details><summary><b> 2. Effectiveness of ID mutants in challenging NonDex running with thresholds from 0.1 to 1
 </b> <i>[click]</i></summary>
<div>

To generate Figure 2 in the paper, please run the following commands:
```
cd scripts/figures/nod
python3 draw_ID.py 
```
</div>
</details>

<details><summary><b> 3. Effectiveness of OD mutants in challenging iDFlakies running with the number of cleaners from 0 to 50 </b> <i>[click]</i></summary>
<div>

To generate Figure 3 in the paper, please run the following commands:
```
cd scripts/figures/od
python3 draw_OD.py 
```
</div>
</details>

## Reproduce results for one project from scratch
This is a demo to generate NOD/ID and OD mutants for 1 project, and then run Surefire, NonDex and iDFlakies on them. It will take around 3 hours to finish the experiments of NOD/ID mutants, and 7 hours for experiments of OD mutants.

<details><summary><b> Setup </b> <i>[click]</i></summary>
<div>
To set up the experiment environment, please run the following command:

```
bash scripts/setup.sh
```
</div>
</details>

<details><summary><b> Demo to generate NOD and ID mutants </b> <i>[click]</i></summary>
<div>

This is a demo to generate NOD and ID mutants on `commons-cli`, and then run NonDex and Surefire on the mutants:
```
cd scripts
bash all_nod.sh projects/cli.csv
```
</div>
</details>


<details><summary><b> Demo to generate OD mutants </b> <i>[click]</i></summary>
<div>

This is a demo to generate OD mutants on `commons-cli`, and then run iDFlakies on the mutants:
```
cd scripts
bash all_od.sh projects/cli.csv
```
</div>
</details>

## Reproduce all results for 15 projects from scratch

<details><summary><b> Setup </b> <i>[click]</i></summary>
<div>
To set up the experiment environment, please run the following command:

```
bash scripts/setup.sh
```
</div>
</details>

<details><summary><b> Reproduce NOD/ID results </b> <i>[click]</i></summary>
<div>

This section is to reproduce the results of evaluating Surefire/NonDex with NOD/ID mutants. The following commands will 1) run all NOD and ID mutation operators on each project 2) run Surefire and NonDex on NOD/ID mutants with thresholds changing from 0.1 to 1:

- Input

`input.csv` contains `url`, `sha`, and `junit version` of 15 projects, e.g.,
```
https://github.com/apache/commons-cli,8adbf64def81ee3e812e802a398ef5afbbfc69ee,4
...
```
- Run the commands:

```
cd scripts
bash all_nod.sh projects/input.csv 
```
- Output

An `output` folder will be generated, for each run, a `TimeStamp1` folder will be generated which includes `logs`, `results`, `mutant` for each project
```
output
   ├── TimeStamp1
    │   ├── results
    │   ├── logs
    │   └── mutant
   ├── TimeStamp2 
    │   ├── results
    │   ├── logs
    │   └── mutant
    └── ...
```

</div>
</details>

<details><summary><b> Reproduce OD results </b> <i>[click]</i></summary>
<div>

This section is to reproduce the results of evaluating iDFlakies with OD mutants. The following commands will 1) run all OD mutation operators on each project 2) run iDFlakies on OD mutants with the number of cleaners chaning from 0 to 50:

- Input

`input.csv` contains `url`, `sha`, and `junit version` of 15 projects, e.g.,
```
https://github.com/apache/commons-cli,8adbf64def81ee3e812e802a398ef5afbbfc69ee,4
...
```
- Run the commands:

```
cd scripts
bash all_od.sh projects/input.csv 
```
- Output

An `output` folder will be generated, for each run, a `TimeStamp1` folder will be generated which includes `logs`, `results`, `mutant` for each project
```
output
   ├── TimeStamp1
    │   ├── results
    │   ├── logs
    │   └── mutant
   ├── TimeStamp2 
    │   ├── results
    │   ├── logs
    │   └── mutant
    └── ...
```
</div>
</details>
