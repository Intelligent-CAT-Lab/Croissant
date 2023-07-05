package com.template.pom;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;

public class DetectionToolAdder {

    PomModifier pomModifier;
    List<DetectionTool> detectionTools;

    public DetectionToolAdder(String projectDir) {
        this.pomModifier = new PomModifier(projectDir);
        this.detectionTools = new ArrayList<>();

        detectionTools.add(new DetectionTool("junit", "junit", "4.12", "junit"));
        Node config = null;
        /*detectionTools.add(new DetectionTool("","","", config));*/
    }

    public void addDetectionTools() {
        for (DetectionTool detectionTool : this.detectionTools) {

            if (detectionTool.hasConfigNode()) {
                Document doc = null;
                try {
                    doc = this.pomModifier.getDocument();
                } catch (ParserConfigurationException | SAXException | IOException e) {
                    e.printStackTrace();
                }
                this.pomModifier.addPlugin(doc, detectionTool.groupId, detectionTool.artifactId, detectionTool.artifactVersion, detectionTool.config);
            } else {
                this.pomModifier.addPlugin(detectionTool.groupId, detectionTool.artifactId, detectionTool.artifactVersion, detectionTool.identifier);
            }
        }
    }
}


class DetectionTool {

    public DetectionTool(String groupId, String artifactId, String artifactVersion, String identifier) {
        this.artifactId = artifactId;
        this.groupId = groupId;
        this.artifactVersion = artifactVersion;
        this.identifier = identifier;
    }

    public DetectionTool(String groupId, String artifactId, String artifactVersion, Node config) {
        this.artifactId = artifactId;
        this.groupId = groupId;
        this.artifactVersion = artifactVersion;
        this.config = config;
    }

    public boolean hasConfigNode() {
        return this.config != null;
    }

    public String groupId;
    public String artifactId;
    public String artifactVersion;
    public Node config = null;
    public String identifier;
}

