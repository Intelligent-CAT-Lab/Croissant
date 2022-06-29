import os


def get_immediate_subdirectories(a_dir):
    return [name for name in os.listdir(a_dir)
            if os.path.isdir(os.path.join(a_dir, name))]

def change_working_dir():
    working_dir = os.environ.get("WORKING_DIR")

    if not working_dir:
        os.environ["WORKING_DIR"] = input("working dir: ")
        change_working_dir()
    else:
        os.chdir(working_dir)