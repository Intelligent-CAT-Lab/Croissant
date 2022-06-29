import os
import re
import templates_standard #import templates, imports, classes
import templates_jupiter
import subprocess
import util

global transformed_test_packages
global templates 
global imports 
global classes
global jupiter

def read_tests(project_dir):
    all_tests = list()
    for dirpath, _, files in os.walk(project_dir):
        for file in files:
            ext = os.path.splitext(file)[-1].lower()
            if ext == ".java" and "test" in file.lower():
                file_path = os.path.join(dirpath, file)
                with open(file_path, "r") as f:
                    all_tests.append((file_path, f.readlines()))
            elif ext == ".xml" and "pom" in file:
                file_path = os.path.join(dirpath, file)

                with open(file_path, "r") as f:
                    contents = f.readlines()
                
                
                
                index = [i for i, item in enumerate(contents) if re.search('.*<dependencies>.*', item)]
                if len(index) == 0:
                    continue

                dependencies = """
                <dependencies>
                <dependency>
                    <groupId>org.eclipse.jetty</groupId>
                    <artifactId>jetty-server</artifactId>
                    <version>9.4.44.v20210927</version>
                </dependency>
                <dependency>
                    <groupId>com.github.ben-manes.caffeine</groupId>
                    <artifactId>caffeine</artifactId>
                    <version>2.5.5</version>
                </dependency>
                    <dependency>
                    <groupId>com.github.ben-manes.caffeine</groupId>
                    <artifactId>caffeine</artifactId>
                    <version>2.5.5</version>
                </dependency>
                <dependency>
                    <groupId>org.mockito</groupId>
                    <artifactId>mockito-all</artifactId>
                    <version>1.10.19</version>
                </dependency>
                <!-- https://mvnrepository.com/artifact/org.xerial/sqlite-jdbc -->
                <dependency>
                    <groupId>org.xerial</groupId>
                    <artifactId>sqlite-jdbc</artifactId>
                    <version>3.36.0.3</version>
                </dependency>


                """

                for i in index:
                    contents[i] = contents[i].replace("<dependencies>", dependencies)

                # for i in index:
                #     new_contents.insert(
                #         index[i] + 1,
                #         """
                #         <dependency>
                #             <groupId>org.eclipse.jetty</groupId>
                #             <artifactId>jetty-server</artifactId>
                #             <version>9.4.44.v20210927</version>
                #             
                #         </dependency>
                #         """
                #     )

                #     new_contents.insert(
                #         index[i] + 1,
                #         """
                #             <dependency>
                #                 <groupId>org.mockito</groupId>
                #                 <artifactId>mockito-all</artifactId>
                #                 <version>1.10.19</version>
                #                 
                #             </dependency>
                #         """
                #     )
                #     new_contents.insert(
                #         index[i] + 1,
                #         """
                #         <dependency>
                #             <groupId>com.github.ben-manes.caffeine</groupId>
                #             <artifactId>caffeine</artifactId>
                #             <version>2.5.5</version>
                #         </dependency>

                #         """
                #     )

                with open(file_path, "w") as f:
                    f.writelines(contents)

    return all_tests


def transform_test(test):
    test_path, contents = test

    package_index = 0

    for i in range(len(contents)):
        line = contents[i]
        if "package" in line:
            package_index = i
            break

            
    contents.insert(package_index+1, imports)
    contents = "".join(contents)

    last_closing_bracket = contents.rfind("}")
    contents = contents[0:last_closing_bracket] + "\n" + contents[last_closing_bracket+1:]


    for template in templates:
        contents += template
    
    contents += "}\n"

    global transformed_test_packages
    test_package = "".join(test_path.split("/")[:-1])
    
    if not test_package in transformed_test_packages:
        transformed_test_packages.append(test_package)
        for class_ in classes:
            contents += class_

    with open(test_path, "w") as f:
        f.write(contents)
        print(f"wrote to {test_path}")

def get_jupiter(test):
    # os.chdir(project_dir)
    # process = subprocess.run("cat pom.xml | grep junit", shell=True, capture_output=True, text=True)
    # os.chdir("..")

    # result = process.stdout

    # return "jupiter" in result

    global jupiter

    if "UnmodifiableMapTest" in test[0]:
        print()

    with open(str(test[0]), "r") as f:
        contents = f.read()
    if "org.junit.jupiter" in contents:
        return True
    elif "org.junit" in contents:
        return False
    else:
        return jupiter


def main():

    util.change_working_dir()


    global transformed_test_packages
    
    global templates 
    global imports 
    global classes

    global jupiter

    jupiter = True

    transformed_test_packages = list()



    projects = util.get_immediate_subdirectories(".")

    if "done" in projects:
        projects.remove("done")
        
    for project in projects:
        tests = read_tests(project)
        for test in tests:
        

            jupiter = get_jupiter(test)
            if jupiter:
                templates = templates_jupiter.templates
                imports = templates_jupiter.imports
                classes = templates_jupiter.classes
            else:
                templates = templates_standard.templates
                imports = templates_standard.imports
                classes = templates_standard.classes


            transform_test(test)


if __name__ == "__main__":
    os.environ["WORKING_DIR"] = "./2.1_not_prepped"
    main()
        


        

        

