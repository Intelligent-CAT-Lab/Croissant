import os
import coloredlogs
import logging

logger = logging.getLogger(__name__)
formatter = coloredlogs.ColoredFormatter('%(asctime)s - %(message)s')
coloredlogs.install(level='DEBUG')

def get_immediate_subdirectories(a_dir):
    return [name for name in os.listdir(a_dir)
            if os.path.isdir(os.path.join(a_dir, name))]


def print_thresholds():
    for dirpath, _, files in os.walk("."):
        for file in files:
            if file == "mutation.config":
                file_path = os.path.join(dirpath, file)
                with open(file_path, "r") as f:
                    logger.warning(f.read())

def change_thresholds(threshold):
    for dirpath, _, files in os.walk("."):
        for file in files:
            if file == "mutation.config":
                file_path = os.path.join(dirpath, file)
                with open(file_path, "w") as f:
                    f.write(f"mutation.threshold={threshold}")
                    f.write(f"\nmutation.count=5")


# projects = get_immediate_subdirectories(".")

# if (sys.argv[1] == "w"):
    
#     for project in projects:
#         print(f"running surefire on {project}")
#         os.chdir(project)
#         filename = project+":"+sys.argv[2]
#         subprocess.run(f"mvn surefire:test -Dsurefire.rerunFailingTestsCount=3 &> surefire{filename}.log", shell=True)
#         os.chdir("..")
    
#     print("running surefire has been completed\n")
    
#     for project in projects:
#         print(f"running nondex on {project}")
#         os.chdir(project)
#         filename = project+":"+sys.argv[2]
#         subprocess.run(f"mvn nondex:nondex &> nondex{filename}.log", shell=True)
#         os.chdir("..")
    
#     print("running nondex has been completed\n")

#     for project in projects: 
#         detectors = [""]
    