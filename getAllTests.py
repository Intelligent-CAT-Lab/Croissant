from posixpath import split
from bs4 import BeautifulSoup
import os
import sys
import csv


all_results=[]
surefire_dirs=[]
test_classes=[]
def get_all_xml(project_dir):
    project=project_dir.split('/')[-1]

    for dirs in os.walk(project_dir):
        if "target/surefire-reports" in dirs[0]:
               surefire_dirs.append(dirs[0])

    all_tests_save_csv='/home/yangc9/latest_tool/croissant_tool/logs/all_tests_'+project+'.csv'
    with open(all_tests_save_csv,"w",newline='') as tests_csv:
        writer = csv.writer(tests_csv)
        print(all_tests_save_csv)
        print(project)
    
        for eachsurefire_dir in surefire_dirs:
            files = os.listdir(eachsurefire_dir)
            for eachfile in files:
                if eachfile.endswith('.txt'):
                    file_path = os.path.join(eachsurefire_dir, eachfile)
                    #test_classes.append('.'.join(eachfile.split('.')[0:-1]))
                    module = eachsurefire_dir.replace('surefire-reports','').replace('.','')
                    testClass = '.'.join(eachfile.split('.')[0:-1])
                    writer.writerow([module,testClass])
                    #writer.writerow([eachsurefire_dir.replace('surefire-reports',''),'.'.join(eachfile.split('.')[0:-1])])
        #csv_name = eachsurefire_dir.replace('.','').replace('surefire-reports','').replace('/','_')
                
        #print(csv_name)
        #tests_module_save_csv = '/home/yangc9/latest_tool/croissant_tool/logs/'+csv_name+'.csv'
        #with open(tests_module_save_csv,"w",newline='') as tests_csv:
        #     writer = csv.writer(tests_csv)
        #     for eachclass in test_classes:
        #         writer.writerow([eachclass])
                
    
    #print(test_classes)

    #all_tests_save_csv='/home/yangc9/latest_tool/croissant_tool/logs/all_tests_'+project+'.csv'

#    with open(all_tests_save_csv,"w",newline='') as tests_csv:
#        writer = csv.writer(tests_csv)
#        for eachclass in test_classes:
#            writer.writerow([eachclass])

get_all_xml(sys.argv[1])
