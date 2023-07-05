import os
import sys

def changeThreshold(testClass,newThreshold):
    updatedThreshold = "mutation.count=" + newThreshold +'\n'
    for dirpath, _, files in os.walk(testClass):
        for file in files:
            if file == "mutation.config":
                file_path = os.path.join(dirpath, file)
                with open(file_path, "r") as f:
                    lines = f.readlines()
                    index = [lines.index(line) for line in lines if "mutation.count" in line]
                    lines[index[0]] = updatedThreshold
                with open(file_path, "w") as f:
                    f.writelines(lines)
                    
if __name__ == "__main__":
    changeThreshold(sys.argv[1],sys.argv[2])