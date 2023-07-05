import csv
import codecs
import os
import matplotlib.pyplot as plt
import pandas as pd
import copy

all_numbers_od = "OD2.csv"
detection_folder = "OD_all"
od_numbers = {}

with codecs.open(all_numbers_od, encoding='utf-8-sig') as f:
    for row in csv.DictReader(f, skipinitialspace=True):
        od_numbers[row['Project']]=row

per_results = {}

for dirpath, _, files in os.walk(detection_folder):
    for file in files:
        filepath = os.path.join(dirpath, file)
        if "idflakies" in filepath and filepath.endswith(".csv"):
            with codecs.open(filepath, encoding= 'unicode_escape') as f:
                    mop = None
                    for row in csv.DictReader(f, skipinitialspace=True):
                        string = row['Tests Detected']
                        detects = string.split(',')
                        vtests=[]
                        project = row['Project']
                        if 'module' in row:
                            module = row['module']
                            if module != row['Project']:
                                project = module
                        for each in detects:
                            if "Mutant_OD_" in each and "IVD" in each: 
                                vtests.append(each)
                                if "jsoup" in each:
                                    print(filepath)
                        if (detects[0]):
                            opList = detects[0].split("Mutant_OD_")
                            if (len(opList)>1):
                                op = opList[1].split("_")[0]
                                #print(op)
                                mop = op
                        if project not in per_results:
                            per_results[project]={}
                        if mop == None:
                            continue
                        if mop =="IVD":
                            Num = len(vtests)
                        else:
                            Num = len(detects)
                        if mop not in per_results[project]:
                            per_results[project][mop] = {}
                        if row['cleanerCounts'] not in per_results[project][mop]:
                            per_results[project][mop][row['cleanerCounts']] = []
                        per_results[project][mop][row['cleanerCounts']].append(Num)

#print(per_results)
converted_per_results = copy.deepcopy(per_results)

for eachProject in converted_per_results:
    for eachOp in converted_per_results[eachProject]:
        for eachCount in converted_per_results[eachProject][eachOp]:
            converted_per_results[eachProject][eachOp][eachCount]=[]

for eachProject in per_results:
    #print(per_results[eachProject])
    for eachOp in per_results[eachProject]:
        allNum = int(od_numbers[eachProject][eachOp])
        if allNum==0 or allNum ==None:
            continue
        #print(eachProject,eachOp,per_results[eachProject][eachOp])
        for eachcount in per_results[eachProject][eachOp]:
            for eachDetected in per_results[eachProject][eachOp][eachcount]:
                #print(eachDetected)
                detect = int(eachDetected)
                if (detect/allNum)*100>100:
                    print(eachProject,eachOp,eachCount)
                converted_per_results[eachProject][eachOp][eachcount].append((detect/allNum)*100)

IVD = {}
for eachProject in converted_per_results:   
    for eachCount in converted_per_results[eachProject]['IVD']:
        if eachCount not in IVD:
            IVD[eachCount] = []
        for eachPer in converted_per_results[eachProject]["IVD"][eachCount]:
            IVD[eachCount].append(eachPer)
            
for eachcount in IVD:
    for eachT in IVD[eachcount]:
        if (eachT>100):
            print(eachT)
            print(eachCount,eachT)

SVD = {}
for eachProject in converted_per_results:   
    for eachCount in converted_per_results[eachProject]['SVD']:
        if eachCount not in SVD:
            SVD[eachCount] = []
        for eachPer in converted_per_results[eachProject]["SVD"][eachCount]:
            if eachPer:
                SVD[eachCount].append(eachPer)
FPD = {}
for eachProject in converted_per_results:   
    for eachCount in converted_per_results[eachProject]['FPD']:
        if eachCount not in FPD:
            FPD[eachCount] = []
        for eachPer in converted_per_results[eachProject]["FPD"][eachCount]:
            FPD[eachCount].append(eachPer)


TPFD = {}
for eachProject in converted_per_results:   
    for eachCount in converted_per_results[eachProject]['TPFD']:
        if eachCount not in TPFD:
            TPFD[eachCount] = []
        for eachPer in converted_per_results[eachProject]["TPFD"][eachCount]:
            TPFD[eachCount].append(eachPer)
CSD = {}
for eachProject in converted_per_results:   
    for eachCount in converted_per_results[eachProject]['CSD']:
        if eachCount not in CSD:
            CSD[eachCount] = []
        for eachPer in converted_per_results[eachProject]["CSD"][eachCount]:
            CSD[eachCount].append(eachPer)

DSD = {}
for eachProject in converted_per_results:   
    for eachCount in converted_per_results[eachProject]['DSD']:
        if eachCount not in DSD:
            DSD[eachCount] = []
        for eachPer in converted_per_results[eachProject]["DSD"][eachCount]:
            DSD[eachCount].append(eachPer)

RA = {}
for eachProject in converted_per_results:   
    for eachOp in converted_per_results[eachProject]:
        if eachOp =='RA':
            for eachCount in converted_per_results[eachProject][eachOp]:
                if eachCount not in RA:
                    RA[eachCount] = []
                for eachPer in converted_per_results[eachProject][eachOp][eachCount]:
                    RA[eachCount].append(eachPer)
    
    
fig = plt.figure(figsize=(14,10))

for each in SVD:
    if len(SVD[each])>105:
        SVD[each].pop()
    
##95d0fc
plt.subplot(1, 7,1)
df = pd.DataFrame(IVD)
plt.title("(a) IVD",fontsize=10,y=-0.2)
# labels = ["0", "10", "20", "30", "35","40","45","50"]
boxes = plt.boxplot(df,showfliers=False,medianprops = {'linestyle':'--','color':'black'},patch_artist=True)
for box in boxes["boxes"]:
    box.set(facecolor = "#95d0fc")
plt.tight_layout(pad=1.08)
plt.rcParams["figure.figsize"] = (2, 2)
plt.ylabel('Flaky Tests detected (%)',fontsize=10)
ax = plt.gca()
ax.axes.xaxis.set_ticklabels([])


plt.subplot(1,7, 2)
df = pd.DataFrame(FPD)
plt.title("(b) FPD",fontsize=10,y=-0.2)
# labels = ["0", "10", "20", "30", "35","40","45","50"]
boxes = plt.boxplot(df,showfliers=False,medianprops = {'linestyle':'--','color':'black'},patch_artist=True)
for box in boxes["boxes"]:
    box.set(facecolor = "#95d0fc")
plt.tight_layout(pad=1.08)
plt.rcParams["figure.figsize"] = (2, 2)
ax = plt.gca()
ax.axes.xaxis.set_ticklabels([])
ax.axes.yaxis.set_ticklabels([])

plt.subplot(1, 7,3)
df = pd.DataFrame(TPFD)
plt.title("(c) TPFD",fontsize=10,y=-0.2)
# labels = ["0", "10", "20", "30", "35","40","45","50"]
boxes = plt.boxplot(df,showfliers=False,medianprops = {'linestyle':'--','color':'black'},patch_artist=True)
for box in boxes["boxes"]:
    box.set(facecolor = "#95d0fc")
plt.tight_layout(pad=1.08)
plt.rcParams["figure.figsize"] = (2, 2)
ax = plt.gca()
ax.axes.xaxis.set_ticklabels([])
ax.axes.yaxis.set_ticklabels([])

plt.subplot(1, 7,4)
df = pd.DataFrame(CSD)
plt.title("(d) CSD",fontsize=10,y=-0.2)
# labels = ["0", "10", "20", "30", "35","40","45","50"]
boxes = plt.boxplot(df,showfliers=False,medianprops = {'linestyle':'--','color':'black'},patch_artist=True)
for box in boxes["boxes"]:
    box.set(facecolor = "#95d0fc")
plt.tight_layout(pad=1.08)
ax = plt.gca()
ax.axes.xaxis.set_ticklabels([])
ax.axes.yaxis.set_ticklabels([])


plt.subplot(1,7, 5)
df = pd.DataFrame(DSD)
plt.title("(e) DSD",fontsize=10,y=-0.2)
# labels = ["0", "10", "20", "30", "35","40","45","50"]
boxes = plt.boxplot(df,showfliers=False,medianprops = {'linestyle':'--','color':'black'},patch_artist=True)
for box in boxes["boxes"]:
    box.set(facecolor = "#95d0fc")
plt.tight_layout(pad=1.08)
plt.rcParams["figure.figsize"] = (2, 2)
ax = plt.gca()
ax.axes.xaxis.set_ticklabels([])
ax.axes.yaxis.set_ticklabels([])

plt.subplot(1,7, 6)
df = pd.DataFrame(SVD)
plt.title("(f) SVD",fontsize=10,y=-0.2)
# labels = ["0", "10", "20", "30", "35","40","45","50"]
boxes = plt.boxplot(df,showfliers=False,medianprops = {'linestyle':'--','color':'black'},patch_artist=True)
for box in boxes["boxes"]:
    box.set(facecolor = "#95d0fc")
plt.tight_layout(pad=1.08)
plt.rcParams["figure.figsize"] = (2, 2)
ax = plt.gca()
ax.axes.xaxis.set_ticklabels([])
ax.axes.yaxis.set_ticklabels([])

plt.subplot( 1,7, 7)
df = pd.DataFrame(RA)
plt.title("(g) RA",fontsize=10,y=-0.2)
# labels = ["0", "10", "20", "30", "35","40","45","50"]
boxes = plt.boxplot(df,showfliers=False,medianprops = {'linestyle':'--','color':'black'},patch_artist=True)
for box in boxes["boxes"]:
    box.set(facecolor = "#95d0fc")
plt.tight_layout(pad=1.08)
plt.rcParams["figure.figsize"] = (2, 2)
ax = plt.gca()
ax.axes.xaxis.set_ticklabels([])
ax.axes.yaxis.set_ticklabels([])

plt.show()                       