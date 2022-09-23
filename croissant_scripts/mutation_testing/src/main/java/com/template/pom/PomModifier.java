package com.template.pom;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.StringJoiner;

public class PomModifier {

    String pom;
    public PomModifier(String projectDir) {
        try {
            this.pom = findPom(projectDir);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public Document getDocument() throws ParserConfigurationException, IOException, SAXException {
        File pomFile = new File(this.pom);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(false);
        dbFactory.setValidating(false);
        DocumentBuilder dBuilder;
        dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(pomFile);
        return doc;
    }

    public void addPlugin(Document doc, String groupIdVal, String artifactIdVal, String artifactVersion, Node config) {
        Node element = createElement(doc, groupIdVal, artifactIdVal, artifactVersion, ElementType.PLUGIN);
        element.appendChild(config);
    }



    public void addPlugin(String groupIdVal, String artifactIdVal, String artifactVersion, String identifier) {
        addElement(groupIdVal, artifactIdVal, artifactVersion, identifier, ElementType.PLUGIN);
    }



    public void addDependency(String groupIdVal, String artifactIdVal, String artifactVersion, String identifier) {
        addElement(groupIdVal, artifactIdVal, artifactVersion, identifier, ElementType.DEPENDENCY);
    }


    private Node createElement(Document doc, String groupIdVal, String artifactIdVal, String artifactVersion, ElementType elementType) {
        String tagName;

        if (elementType == ElementType.PLUGIN) {
            tagName = "plugin";
        } else {
            tagName = "dependency";
        }

        Node plugin = doc.createElement(tagName);
        {
            Node groupId = doc.createElement("groupId");
            groupId.setTextContent(groupIdVal);
            plugin.appendChild(groupId);
        }
        {
            Node artifactId = doc.createElement("artifactId");
            artifactId.setTextContent(artifactIdVal);
            plugin.appendChild(artifactId);
        }
        {
            Node version = doc.createElement("version");
            version.setTextContent(artifactVersion);
            plugin.appendChild(version);
        }

        return plugin;
    }

    private void addElement(Node plugins, Document doc, String groupIdVal, String artifactIdVal, String artifactVersion, String identifier, ElementType elementType) {
        Node plugin = createElement(doc, groupIdVal, artifactIdVal, artifactVersion,elementType);

        NodeList nodeList = plugins.getChildNodes();

        for (int i = 0; i < nodeList.getLength(); i++) {
            Node childNode = nodeList.item(i);
            if (childNode.isEqualNode(plugin)) {
                return;
            }
        }

        plugins.appendChild(plugin);
    }

    private boolean alreadyExists(String identifier) {

        File pomFile = new File(pom);
        Scanner myReader = null;
        try {
            myReader = new Scanner(pomFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        while (true) {
            assert myReader != null;
            if (!myReader.hasNextLine()) break;
            String data = myReader.nextLine();

            if (data.contains(identifier)) {
                return true;
            }
        }

        return false;
    }

    private void addElement(String groupIdVal, String artifactIdVal, String artifactVersion, String identifier, ElementType elementType) {
        String elementTag;

        if (alreadyExists(identifier)) {
            return;
        }

        if (elementType == ElementType.PLUGIN) {
            elementTag = "plugins";
        } else {
            elementTag = "dependencies";
        }

        File pomFile = new File(this.pom);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(false);
        dbFactory.setValidating(false);
        DocumentBuilder dBuilder;

        try {
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(pomFile);

            // Get high-level project node to find <build> tag
            Node project = doc.getElementsByTagName("project").item(0);
            NodeList projectChildren = project.getChildNodes();

            if (elementType == ElementType.PLUGIN) {
                // Check if <build> tag exists; if not have to make one
                Node build = null;
                for (int i = 0; i < projectChildren.getLength(); i++) {
                    if (projectChildren.item(i).getNodeName().equals("build")) {
                        build = projectChildren.item(i);
                        break;
                    }
                }
                if (build == null) {
                    build = doc.createElement("build");
                    project.appendChild(build);
                }

                NodeList buildChildren = build.getChildNodes();


                // Search for <plugins>
                Node plugins = null;
                for (int i = 0; i < buildChildren.getLength(); i++) {
                    if (buildChildren.item(i).getNodeName().equals(elementTag)) {
                        plugins = buildChildren.item(i);
                        break;
                    }
                }
                // Add new <plugins> if non-existant
                if (plugins == null) {
                    plugins = doc.createElement(elementTag);
                    build.appendChild(plugins);
                }
                addElement(plugins, doc, groupIdVal, artifactIdVal, artifactVersion, identifier, elementType);
            } else {
                Node dependencies = null;
                for (int i = 0; i < projectChildren.getLength(); i++) {
                    if (projectChildren.item(i).getNodeName().equals("dependencies")) {
                        dependencies = projectChildren.item(i);
                    }
                }
                if (dependencies == null) {
                    dependencies = doc.createElement(elementTag);
                    project.appendChild(dependencies);
                }
                addElement(dependencies, doc, groupIdVal, artifactIdVal, artifactVersion, identifier, elementType);
            }

            // Construct string representation of the file
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            String output = writer.getBuffer().toString();

            // Rewrite the pom file with this string
            PrintWriter fileWriter = new PrintWriter(this.pom);
            fileWriter.println(output);
            fileWriter.close();

        } catch (FileNotFoundException e) {
            System.out.println("File does not exit: " + this.pom);
        } catch (ParserConfigurationException | SAXException | TransformerException | IOException e) {
            e.printStackTrace();
        }
    }
    private String runProcess(String[] process, String projectDir) throws IOException, InterruptedException {
/*        ProcessBuilder   ps=new ProcessBuilder(process);
        ps.redirectErrorStream(true);

        Process pr = ps.start();

        BufferedReader in = new BufferedReader(new InputStreamReader(pr.getInputStream()));
        String line;
        StringBuilder output = new StringBuilder();
        while ((line = in.readLine()) != null) {
            output.append(line);
        }
        pr.waitFor();
        in.close();*/



        ProcessBuilder processBuilder = new ProcessBuilder(process);
        processBuilder.directory(new File("."));
        processBuilder.redirectErrorStream(true);

        Process subprocess = processBuilder.start();
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(subprocess.getInputStream()))) {

            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                output.append(line);
            }
        }
        subprocess.waitFor();
        return output.toString();
    }

    private String findPom(String projectDir) throws IOException, InterruptedException {
        String[] parts = projectDir.split(File.separator);
        int targetIndex = Arrays.asList(parts).indexOf("target");
        List<String> subArray = Arrays.asList(parts).subList(0, targetIndex);
        StringJoiner stringJoiner = new StringJoiner(File.separator);

        for (String elem: subArray) {
            stringJoiner.add(elem);
        }
        return runProcess(new String[] {"/bin/sh", "-c", "find "+stringJoiner+" -name pom.xml "}, projectDir);
    }
}

enum ElementType {
    PLUGIN,
    DEPENDENCY,
}
