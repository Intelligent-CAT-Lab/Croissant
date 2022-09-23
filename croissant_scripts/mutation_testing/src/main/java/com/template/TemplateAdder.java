package com.template;



import com.template.pom.PomModifier;

import java.util.List;

import java.util.regex.Pattern;

public abstract class TemplateAdder {
    protected boolean jupiter;
    private PomModifier pomModifier;
    private List<String> units;

    protected TemplateAdder(List<String> units, boolean jupiter, String projectDir) {
        this.jupiter = jupiter;
        this.pomModifier = new PomModifier(projectDir);
        this.units = units;
    }

    public TemplateAdder(String projectDir) {
    }



    public List<String> returnClass() {
        return units;
    }

    protected void addPlugin(String groupIdVal, String artifactIdVal, String artifactVersion, String identifier) {
        this.pomModifier.addPlugin(groupIdVal, artifactIdVal, artifactVersion, identifier);
    }

    protected void addDependency(String groupIdVal, String artifactIdVal, String artifactVersion, String identifier) {
        this.pomModifier.addDependency(groupIdVal, artifactIdVal, artifactVersion, identifier);
    }

    protected int findMainClassEnd() {
        int start = -1;
        Pattern mainClassPattern = Pattern.compile(" *public * .* *(?:final)? *class .*");

        do {
            start++;
        } while (start < this.units.size() - 1 && !mainClassPattern.matcher(this.units.get(start)).matches());

        int end = start + 1;
        int i = start + 1;

        Pattern classPattern = Pattern.compile(" *class *.*");
        Pattern closingBracketPattern = Pattern.compile(".*}.*");

        do {
            i++;
            if (closingBracketPattern.matcher(this.units.get(i)).matches()) {
                end = i;
            }
        } while (!classPattern.matcher(this.units.get(i)).matches() && i < this.units.size() - 1);
        return end;
    }

    protected void addTest(String templateTest) {
        Integer index = this.findMainClassEnd();
        this.units.add(index, templateTest);
    }

    protected void addField(String field) {
        Integer index = this.findMainClassEnd();
        this.units.add(index, field);
    }

    protected void addClass(String newClass) {
        this.units.add(newClass);
    }

    protected void addImport(String importStmt) {
        int i = 0;
        for (String unit: this.units) {
            if (unit.contains("package")) {
                break;
            }
            i++;
        }
        this.units.add(i+1, importStmt);
    }

    public abstract void run();



}

