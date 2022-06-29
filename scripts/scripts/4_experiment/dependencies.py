import os


def find_index(contents):
    tag = "<plugins>"
    index = contents.find(tag)

    if index == -1:
        raise ValueError("plugins tag cannot be found")
    
    index += len(tag)

    return index


def add_dependencies():
    for dirpath, _, files in os.walk("."):
        for file in files:
            if file == "pom.xml":
                try:
                    file_path = os.path.join(dirpath, file)
                    with open(file_path, "r") as f:
                        contents = f.read()
                    

                    index = find_index(contents)
                    plugins=""

                    surefire = """
                    <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-compiler-plugin</artifactId>
                                <version>2.3.2</version>
                                <executions>
                                    <execution>
                                        <id>default-testCompile</id>
                                        <phase>test-compile</phase>
                                        <goals>
                                            <goal>testCompile</goal>
                                        </goals>
                                        <configuration combine.self="override">
                                            <skip>true</skip>
                                        </configuration>
                                    </execution>
                                </executions>
                            </plugin>"""
                    if surefire not in contents:
                        plugins += surefire

                    if "idflakies" not in contents:
                        plugins += """
                        <plugin>
                            <groupId>edu.illinois.cs</groupId>
                            <artifactId>testrunner-maven-plugin</artifactId>
                            <version>1.2</version>
                            <dependencies>
                                <dependency>
                                    <groupId>edu.illinois.cs</groupId>
                                    <artifactId>idflakies</artifactId>
                                    <version>1.2.0-SNAPSHOT</version>
                                </dependency>
                            </dependencies>
                            <configuration>
                                <className>edu.illinois.cs.dt.tools.detection.DetectorPlugin</className>
                            </configuration>
                        </plugin>
                        """
                    
                    if "nondex" not in contents:
                        plugins += """
                        <plugin>
                            <groupId>edu.illinois</groupId>
                            <artifactId>nondex-maven-plugin</artifactId>
                            <version>1.1.2</version>
                        </plugin>
                        """
                    

                    contents = contents[0:index] + plugins + contents[index:]

                    with open(file_path, "w") as f:
                        f.write(contents)
                except Exception:
                    pass






