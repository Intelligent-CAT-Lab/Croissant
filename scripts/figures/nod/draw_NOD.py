import csv
import codecs
import os
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

all_numbers_nod="nodAll.csv"
detection_folder="finalnod"
nod_numbers={}

with codecs.open(all_numbers_nod, encoding='utf-8-sig') as f:
    for row in csv.DictReader(f, skipinitialspace=True):
        nod_numbers[row['Project']]=row

print(nod_numbers)

per_results_nondex={}
per_results_surefire={}

for dirpath, _, files in os.walk(detection_folder):
        for file in files:
            filepath = os.path.join(dirpath, file)
            project = file.split("_")[1].split(".")[0]
            if "nondex" in file:
                per_results_nondex[project]={}
                with codecs.open(filepath, encoding='utf-8-sig') as f:
                    for row in csv.DictReader(f, skipinitialspace=True):
                        per_results_nondex[project][row["threshold"]]=row
            elif "surefire" in file:
                per_results_surefire[project]={}
                with codecs.open(filepath, encoding='utf-8-sig') as f:
                    for row in csv.DictReader(f, skipinitialspace=True):
                        per_results_surefire[project][row["threshold"]]=row

converted_per_results_nondex = per_results_nondex.copy()
for eachProject in per_results_nondex:
    for eachThreshold in per_results_nondex[eachProject]:
        for eachOp in per_results_nondex[eachProject][eachThreshold]:
            if eachOp in nod_numbers[eachProject]:
                allNumOfOp = int(nod_numbers[eachProject][eachOp])
                detected = int(per_results_nondex[eachProject][eachThreshold][eachOp])
                per = detected/allNumOfOp
                converted_per_results_nondex[eachProject][eachThreshold][eachOp] = per*100

converted_per_results_surefire = per_results_surefire.copy()
for eachProject in per_results_surefire:
    for eachThreshold in per_results_surefire[eachProject]:
        for eachOp in per_results_surefire[eachProject][eachThreshold]:
            if eachOp in nod_numbers[eachProject]:
                allNumOfOp = int(nod_numbers[eachProject][eachOp])
                detected = int(per_results_surefire[eachProject][eachThreshold][eachOp])
                per = detected/allNumOfOp
                converted_per_results_surefire[eachProject][eachThreshold][eachOp] = per*100

#print(converted_per_results_nondex[eachProject])

MD = {}
for eachProject in converted_per_results_nondex:
    for eachThreshold in converted_per_results_nondex[eachProject]:
        if eachThreshold not in MD:
            MD[eachThreshold] = []
        MD[eachThreshold].append(converted_per_results_nondex[eachProject][eachThreshold]["MD"])

MDs = {}
for eachProject in converted_per_results_surefire:
    for eachThreshold in converted_per_results_surefire[eachProject]:
        if eachThreshold not in MDs:
            MDs[eachThreshold] = []
        MDs[eachThreshold].append(converted_per_results_surefire[eachProject][eachThreshold]["MD"])

PD = {}
for eachProject in converted_per_results_nondex:
    for eachThreshold in converted_per_results_nondex[eachProject]:
        if eachThreshold not in PD:
            PD[eachThreshold] = []
        PD[eachThreshold].append(converted_per_results_nondex[eachProject][eachThreshold]["PD"])
PDs = {}
for eachProject in converted_per_results_surefire:
    for eachThreshold in converted_per_results_surefire[eachProject]:
        if eachThreshold not in PDs:
            PDs[eachThreshold] = []
        PDs[eachThreshold].append(converted_per_results_surefire[eachProject][eachThreshold]["PD"])

STZ = {}
for eachProject in converted_per_results_nondex:
    for eachThreshold in converted_per_results_nondex[eachProject]:
        if eachThreshold not in STZ:
            STZ[eachThreshold] = []
        STZ[eachThreshold].append(converted_per_results_nondex[eachProject][eachThreshold]["STiD"])
STZs = {}
for eachProject in converted_per_results_surefire:
    for eachThreshold in converted_per_results_surefire[eachProject]:
        if eachThreshold not in STZs:
            STZs[eachThreshold] = []
        STZs[eachThreshold].append(converted_per_results_surefire[eachProject][eachThreshold]["STiD"])

STV = {}
for eachProject in converted_per_results_nondex:
    for eachThreshold in converted_per_results_nondex[eachProject]:
        if eachThreshold not in STV:
            STV[eachThreshold] = []
        STV[eachThreshold].append(converted_per_results_nondex[eachProject][eachThreshold]["TVIMO"])

STVs = {}
for eachProject in converted_per_results_surefire:
    for eachThreshold in converted_per_results_surefire[eachProject]:
        if eachThreshold not in STVs:
            STVs[eachThreshold] = []
        STVs[eachThreshold].append(converted_per_results_surefire[eachProject][eachThreshold]["TVIMO"])

ASW = {}
for eachProject in converted_per_results_nondex:
    for eachThreshold in converted_per_results_nondex[eachProject]:
        if eachThreshold not in ASW:
            ASW[eachThreshold] = []
        ASW[eachThreshold].append(converted_per_results_nondex[eachProject][eachThreshold]["ASW"])

ASWs = {}
for eachProject in converted_per_results_surefire:
    for eachThreshold in converted_per_results_surefire[eachProject]:
        if eachThreshold not in ASWs:
            ASWs[eachThreshold] = []
        ASWs[eachThreshold].append(converted_per_results_surefire[eachProject][eachThreshold]["ASW"])

RC = {}
for eachProject in converted_per_results_nondex:
    for eachThreshold in converted_per_results_nondex[eachProject]:
        if eachThreshold not in RC:
            RC[eachThreshold] = []
        RC[eachThreshold].append(converted_per_results_nondex[eachProject][eachThreshold]["RC"])

RCs= {}
for eachProject in converted_per_results_surefire:
    for eachThreshold in converted_per_results_surefire[eachProject]:
        if eachThreshold not in RCs:
            RCs[eachThreshold] = []
        RCs[eachThreshold].append(converted_per_results_surefire[eachProject][eachThreshold]["RC"])

CT = {}
for eachProject in converted_per_results_nondex:
    for eachThreshold in converted_per_results_nondex[eachProject]:
        if eachThreshold not in CT:
            CT[eachThreshold] = []
        CT[eachThreshold].append(converted_per_results_nondex[eachProject][eachThreshold]["CT"])

CTs = {}
for eachProject in converted_per_results_surefire:
    for eachThreshold in converted_per_results_surefire[eachProject]:
        if eachThreshold not in CTs:
            CTs[eachThreshold] = []
        CTs[eachThreshold].append(converted_per_results_surefire[eachProject][eachThreshold]["CT"])

TRRMO = {}
for eachProject in converted_per_results_nondex:
    for eachThreshold in converted_per_results_nondex[eachProject]:
        if eachThreshold not in TRRMO:
            TRRMO[eachThreshold] = []
        TRRMO[eachThreshold].append(converted_per_results_nondex[eachProject][eachThreshold]["TRRMO"])

TRRMOs = {}
for eachProject in converted_per_results_surefire:
    for eachThreshold in converted_per_results_surefire[eachProject]:
        if eachThreshold not in TRRMOs:
            TRRMOs[eachThreshold] = []
        TRRMOs[eachThreshold].append(converted_per_results_surefire[eachProject][eachThreshold]["TRRMO"])

UCC = {}
for eachProject in converted_per_results_nondex:
    for eachThreshold in converted_per_results_nondex[eachProject]:
        if eachThreshold not in UCC:
            UCC[eachThreshold] = []
        UCC[eachThreshold].append(converted_per_results_nondex[eachProject][eachThreshold]["UCC"])

UCCs = {}
for eachProject in converted_per_results_surefire:
    for eachThreshold in converted_per_results_surefire[eachProject]:
        if eachThreshold not in UCCs:
            UCCs[eachThreshold] = []
        UCCs[eachThreshold].append(converted_per_results_surefire[eachProject][eachThreshold]["UCC"])

UCIA = {}
for eachProject in converted_per_results_nondex:
    for eachThreshold in converted_per_results_nondex[eachProject]:
        if eachThreshold not in UCIA:
            UCIA[eachThreshold] = []
        UCIA[eachThreshold].append(converted_per_results_nondex[eachProject][eachThreshold]["UCIA"])

UCIAs = {}
for eachProject in converted_per_results_surefire:
    for eachThreshold in converted_per_results_surefire[eachProject]:
        if eachThreshold not in UCIAs:
            UCIAs[eachThreshold] = []
        UCIAs[eachThreshold].append(converted_per_results_surefire[eachProject][eachThreshold]["UCIA"])

RAM = {}
for eachProject in converted_per_results_nondex:
    for eachThreshold in converted_per_results_nondex[eachProject]:
        if eachThreshold not in RAM:
            RAM[eachThreshold] = []
        RAM[eachThreshold].append(converted_per_results_nondex[eachProject][eachThreshold]["RAM"])
RAMs = {}
for eachProject in converted_per_results_surefire:
    for eachThreshold in converted_per_results_surefire[eachProject]:
        if eachThreshold not in RAMs:
            RAMs[eachThreshold] = []
        RAMs[eachThreshold].append(converted_per_results_surefire[eachProject][eachThreshold]["RAM"])

fig = plt.figure(figsize=(10,8))

plt.subplot(2, 4, 1)
df = pd.DataFrame(MD)
dfs = pd.DataFrame(MDs)
plt.title("(a) MD",fontsize=12,y=-0.3)
labels = ["", "0.2", "", "0.4", "","0.6","","0.8","","1.0"]
y_ticks = np.arange(0, 80, 20)
plt.boxplot(df,patch_artist=True,showfliers=False,medianprops = {'linewidth':'1.3'},labels=labels)
boxes = plt.boxplot(dfs,showfliers=False,medianprops = {'linestyle':'--','color':'black'},patch_artist=True,labels=labels)
plt.ylim(0,90)
for box in boxes["boxes"]:
    box.set(facecolor = "#f0833a")

plt.tight_layout(pad=1.08)
plt.rcParams["figure.figsize"] = (2,4)
plt.ylabel('Flaky Tests detected (%)',fontsize=11)



plt.subplot(2, 4, 2)
df = pd.DataFrame(PD)
dfs = pd.DataFrame(PDs)
plt.title("(b) PD",fontsize=12,y=-0.3)
labels = ["", "0.2", "", "0.4", "","0.6","","0.8","","1.0"]
plt.boxplot(df,patch_artist=True,showfliers=False,medianprops = {'linewidth':'1.3'},labels=labels)
boxes = plt.boxplot(dfs,showfliers=False,medianprops = {'linestyle':'--','color':'black'},patch_artist=True,labels=labels)
for box in boxes["boxes"]:
    box.set(facecolor = "#f0833a")
plt.tight_layout(pad=1.08)
plt.rcParams["figure.figsize"] = (2,4)
plt.ylim(0,90)


plt.subplot(2, 4, 3)
df = pd.DataFrame(STZ)
dfs = pd.DataFrame(STZs)
plt.title("(c) STD-Z",fontsize=12,y=-0.3)
labels = ["", "0.2", "", "0.4", "","0.6","","0.8","","1.0"]
plt.boxplot(df,patch_artist=True,showfliers=False,medianprops = {'linewidth':'1.3'},labels=labels)
boxes = plt.boxplot(dfs,showfliers=False,medianprops = {'linestyle':'--','color':'black'},patch_artist=True,labels=labels)
for box in boxes["boxes"]:
    box.set(facecolor = "#f0833a")
plt.tight_layout(pad=1.08)
plt.rcParams["figure.figsize"] = (2,4)
plt.ylim(0,90)

plt.subplot(2, 4, 4)
df = pd.DataFrame(STV)
dfs = pd.DataFrame(STVs)
plt.title("(d) STD-V",fontsize=12,y=-0.3)
labels = ["", "0.2", "", "0.4", "","0.6","","0.8","","1.0"]
plt.boxplot(df,patch_artist=True,showfliers=False,medianprops = {'linewidth':'1.3'},labels=labels)
boxes = plt.boxplot(dfs,showfliers=False,medianprops = {'linestyle':'--','color':'black'},patch_artist=True,labels=labels)
for box in boxes["boxes"]:
    box.set(facecolor = "#f0833a")
plt.tight_layout(pad=1.08)
plt.rcParams["figure.figsize"] =(2,4)
plt.ylim(0,90)


plt.subplot(2, 4, 5)
df = pd.DataFrame(ASW)
dfs = pd.DataFrame(ASWs)
plt.title("(e) AW",fontsize=12,y=-0.3)
labels = ["", "0.2", "", "0.4", "","0.6","","0.8","","1.0"]
plt.boxplot(df,patch_artist=True,showfliers=False,medianprops = {'linewidth':'1.3'},labels=labels)
boxes = plt.boxplot(dfs,showfliers=False,medianprops = {'linestyle':'--','color':'black'},patch_artist=True,labels=labels)
for box in boxes["boxes"]:
    box.set(facecolor = "#f0833a")
plt.tight_layout(pad=1.08)
plt.rcParams["figure.figsize"] = (2,4)
plt.ylim(0,90)
plt.ylabel('Flaky Tests detected (%)',fontsize=11)


plt.subplot(2, 4, 6)
df = pd.DataFrame(RC)
dfs = pd.DataFrame(RCs)
plt.title("(f) RC",fontsize=12,y=-0.3)
labels = ["", "0.2", "", "0.4", "","0.6","","0.8","","1.0"]
plt.boxplot(df,patch_artist=True,showfliers=False,medianprops = {'linewidth':'1.3'},labels=labels)
boxes = plt.boxplot(dfs,showfliers=False,medianprops = {'linestyle':'--','color':'black'},patch_artist=True,labels=labels)
for box in boxes["boxes"]:
    box.set(facecolor = "#f0833a")
plt.tight_layout(pad=1.08)
plt.rcParams["figure.figsize"] = (2,4)
plt.ylim(0,90)


plt.subplot(2, 4, 7)
df = pd.DataFrame(CT)
dfs = pd.DataFrame(CTs)
plt.title("(g) CTD",fontsize=12,y=-0.3)
labels = ["", "0.2", "", "0.4", "","0.6","","0.8","","1.0"]
plt.boxplot(df,patch_artist=True,showfliers=False,medianprops = {'linewidth':'1.3'},labels=labels)
boxes = plt.boxplot(dfs,showfliers=False,medianprops = {'linestyle':'--','color':'black'},patch_artist=True,labels=labels)
for box in boxes["boxes"]:
    box.set(facecolor = "#f0833a")
plt.tight_layout(pad=1.08)
plt.rcParams["figure.figsize"] = (2,4)
plt.ylim(0,90)


plt.subplot(2, 4, 8)
df = pd.DataFrame(TRRMO)
dfs = pd.DataFrame(TRRMOs)
plt.title("(h) TRR",fontsize=12,y=-0.3)
labels = ["", "0.2", "", "0.4", "","0.6","","0.8","","1.0"]
plt.boxplot(df,patch_artist=True,showfliers=False, medianprops = {'linewidth':'1.3'},labels=labels)
boxes = plt.boxplot(dfs,showfliers=False,medianprops = {'linestyle':'--','color':'black'},patch_artist=True,labels=labels)
for box in boxes["boxes"]:
    box.set(facecolor = "#f0833a")
plt.tight_layout(pad=1.08)
plt.rcParams["figure.figsize"] = (2,4)
plt.scatter([],[],s=150,label='NonDex')
plt.scatter([],[],c="#f0833a",s=150,label='Surefire')
plt.legend(frameon=False,loc='upper right')
plt.ylim(0,90)
plt.show()
