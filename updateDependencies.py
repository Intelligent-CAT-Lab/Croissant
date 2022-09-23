import xml.etree.ElementTree as ET
import os



def addMockito(Element):
    mockito_root = ET.SubElement(Element,'dependency')
    mockito_groupID = ET.SubElement(mockito_root, 'groupId')
    mockito_groupID.text = 'org.mockito'
    mockito_artifactId = ET.SubElement(mockito_root, 'artifactId')
    mockito_artifactId.text = 'mockito-core'
    mockito_version = ET.SubElement(mockito_root, 'version')
    mockito_version.text = '4.6.1'
    mockito_scope = ET.SubElement(mockito_root, 'scope')
    mockito_scope.text = 'test'


def addJupiter(Element):
    jupiter_root = ET.SubElement(Element,'dependency')
    jupiter_groupID = ET.SubElement(jupiter_root, 'groupId')
    jupiter_groupID.text = 'org.junit.jupiter'
    jupiter_artifactId = ET.SubElement(jupiter_root, 'artifactId')
    jupiter_artifactId.text = 'junit-jupiter-engine'
    jupiter_version = ET.SubElement(jupiter_root, 'version')
    jupiter_version.text = '5.8.1'
    jupiter_sxope = ET.SubElement(jupiter_root, 'scope')
    jupiter_sxope.text = 'test'

def addSurefire(Element):
    surefire_root = ET.SubElement(Element,'dependency')
    surefire_groupID = ET.SubElement(surefire_root, 'groupId')
    surefire_groupID.text = 'org.apache.maven.plugins'
    surefire_artifactId = ET.SubElement(surefire_root, 'artifactId')
    surefire_artifactId.text = 'maven-surefire-plugin'
    surefire_version = ET.SubElement(surefire_root, 'version')
    surefire_version.text = '3.0.0-M5'
    surefire_type = ET.SubElement(surefire_root, 'type')
    surefire_type.text = 'maven-plugin'

def addCompiler(Element):
    compiler_root = ET.SubElement(Element, 'plugin')
    compiler_groupid = ET.SubElement(compiler_root, 'groupId')
    compiler_groupid.text = 'org.apache.maven.plugins'
    compiler_artifactId = ET.SubElement(compiler_root, 'artifactId')
    compiler_artifactId.text = 'maven-compiler-plugin'
    compiler_version = ET.SubElement(compiler_root, 'version')
    compiler_version.text = '2.3.2'
    compiler_excutions = ET.SubElement(compiler_root, 'executions')
    compiler_excution = ET.SubElement(compiler_excutions, 'execution')
    excution_id = ET.SubElement(compiler_excution, 'id')
    excution_id.text = 'default-testCompile'
    excution_phase = ET.SubElement(compiler_excution, 'phase')
    excution_phase.text = 'test-compile'
    excution_goals = ET.SubElement(compiler_excution, 'goals')
    excution_goal = ET.SubElement(excution_goals, 'goal')
    excution_goal.text = 'testCompile'
    excution_configuration = ET.SubElement(compiler_excution, 'configuration')
    excution_configuration.set('combine.self','override')
    configuration_skip = ET.SubElement(excution_configuration, 'skip')
    configuration_skip.text = 'true'

def addSkipCompiler(Element):
    executionsTag = False
    for eachSetting in Element:
        if "executions" in eachSetting.tag:
            executionsTag = True
            compiler_excution = ET.SubElement(eachSetting, 'execution')
            excution_id = ET.SubElement(compiler_excution, 'id')
            excution_id.text = 'default-testCompile'
            excution_phase = ET.SubElement(compiler_excution, 'phase')
            excution_phase.text = 'test-compile'
            excution_goals = ET.SubElement(compiler_excution, 'goals')
            excution_goal = ET.SubElement(excution_goals, 'goal')
            excution_goal.text = 'testCompile'
            excution_configuration = ET.SubElement(compiler_excution, 'configuration')
            excution_configuration.set('combine.self','override')
            configuration_skip = ET.SubElement(excution_configuration, 'skip')
            configuration_skip.text = 'true'
                        
    if  executionsTag == False:
        compiler_excutions = ET.SubElement(Element, 'executions')
        compiler_excution = ET.SubElement(compiler_excutions, 'execution')
        excution_id = ET.SubElement(compiler_excution, 'id')
        excution_id.text = 'default-testCompile'
        excution_phase = ET.SubElement(compiler_excution, 'phase')
        excution_phase.text = 'test-compile'
        excution_goals = ET.SubElement(compiler_excution, 'goals')
        excution_goal = ET.SubElement(excution_goals, 'goal')
        excution_goal.text = 'testCompile'
        excution_configuration = ET.SubElement(compiler_excution, 'configuration')
        excution_configuration.set('combine.self','override')
        configuration_skip = ET.SubElement(excution_configuration, 'skip')
        configuration_skip.text = 'true'
           
                        

def addDependencies():
    for dirpath, _, files in os.walk("/home/yangc9/latest_tool/croissant_tool/mutants/"):
        for file in files:
            if file == "pom.xml":
                try:
                    file_path = os.path.join(dirpath, file)
                    mytree = ET.parse(file_path)
                    myroot = mytree.getroot()
                    ET.register_namespace('','http://maven.apache.org/POM/4.0.0')

                    mockitoTag = False
                    jupiterTag = False
                    surefireTag = False
                    compilerTag = False
                    compilerSkipTag = False

                    for each in myroot:
                        #print(subroot.tag)
                        #for each in myroot:#list(subroot.iter()):
                            # add dependencies
                            
                            if "dependencies" in each.tag:
                                for eachDependency in each:
                                    for eachSetting in list(eachDependency.iter()):
                                            if "artifactId" in eachSetting.tag and eachSetting.text == "mockito-core" :
                                                mockitoTag = True
                                            if "artifactId" in eachSetting.tag and eachSetting.text == "junit-jupiter-engine" :
                                                jupiterTag = True
                                            if "artifactId" in eachSetting.tag and eachSetting.text == "maven-surefire-plugin":
                                                eachDependency.set('version','3.0.0-M5')
                                                surefireTag = True

                                if mockitoTag == False:
                                    addMockito(each)
                                if jupiterTag == False:
                                    addJupiter(each)
                                if surefireTag ==False:
                                    addSurefire(each)
                            
                            # add plugins
                            if "build" in each.tag:
                                for eachBuild in each:   
                                    if "plugins" in eachBuild.tag:
                                        for eachPlugin in eachBuild:
                                            for eachSetting in list(eachPlugin.iter()):
                                                if "artifactId" in eachSetting.tag and eachSetting.text == "maven-compiler-plugin" :
                                                    compilerTag = True
                                                    print(file_path,111)
                                                    addSkipCompiler(eachPlugin)
                                                        
                                                        
                                        if compilerTag == False:
                                            addCompiler(eachBuild)              
                                                             

                    mytree.write(file_path,xml_declaration=True,method='xml',encoding="utf8")
                    
                    print(file_path)
                                                                    
                except Exception:
                    pass


def main():
    addDependencies()

main()
