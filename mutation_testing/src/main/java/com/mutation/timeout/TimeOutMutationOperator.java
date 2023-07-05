package com.mutation.timeout;

import com.mutation.SootMutationOperator;
import soot.*;
import soot.jimple.JasminClass;
import soot.jimple.Stmt;
import soot.jimple.internal.JIdentityStmt;
import soot.options.Options;
import soot.util.Chain;
import soot.util.JasminOutputStream;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;

public abstract class TimeOutMutationOperator extends SootMutationOperator {
    SootMethod cloneTest;
    SootMethod mainTest;

    @Override
    public <T extends Unit> List<T> locateUnits() {
        return null;
    }

    public Local getIdentityLocal(SootMethod test) {
        //TODO: add to SootMutationOperator
        Body body = test.getActiveBody();
        Chain<Unit> units = body.getUnits();
        Iterator<Unit> iterator = units.snapshotIterator();

        while (iterator.hasNext()) {
            Stmt stmt = (Stmt) iterator.next();
            if (!(stmt instanceof JIdentityStmt)) {
                continue;
            }
            JIdentityStmt jIdentityStmt = (JIdentityStmt) stmt;
            return (Local) jIdentityStmt.getLeftOp();
        }
        return null;
    }

    public void cloneTest() {
        SootMethod originalTest = this.getCurrentMethod();
        SootMethod cloneTest = new SootMethod(
            this.getClass().getSimpleName() + originalTest.getName() ,
            originalTest.getParameterTypes(),
            originalTest.getReturnType(),
            originalTest.getModifiers(),
            originalTest.getExceptions()
        );
        cloneTest.setActiveBody((Body) originalTest.retrieveActiveBody().clone());
        try {
            this.getCurrentClass().addMethod(cloneTest);
        } catch (Exception e) {

        }
        cloneTest.addTag(this.createTestAnnotation());
        this.cloneTest = cloneTest;
    }

    public long getAppropriateTimeoutValue() throws IOException, InterruptedException {
        Path targetProjectPath = Paths.get(this.inputDir);

        while (true) {
            File file = new File(new File(targetProjectPath.toString()), "pom.xml");
            if (file.exists()) {
                break;
            } else {
                targetProjectPath = targetProjectPath.getParent();
            }
        }
        String command = "mvn -f "+targetProjectPath+" surefire:test -Dtest="+this.getCurrentClass().getName()+"#"+this.mainTest.getName();
        //String[] command = new String[]{"mvn", "-f", targetProjectPath.toString(), "surefire:test", "-Dtest=" + this.getClassName() + "#" + "testMainTMO_test" + this.mainTest.getName()};
        //String[] command = new String[]{""};
        //System.out.println(command);
        long startTime = System.currentTimeMillis();
        Process exec = Runtime.getRuntime().exec(command);
        BufferedReader input = new BufferedReader(new InputStreamReader(exec.getInputStream()));
        String line;
        while ((line = input.readLine()) != null) {
            //System.out.println(line);
        }
        input.close();
        exec.waitFor();
        long endTime = System.currentTimeMillis();
        long duration = (endTime - startTime ) / 4000;

        if (duration == 0) {
            duration = 1;
        }
        return duration;
    }

    @Override
    public void exportClass(String outputDir, String methodName) throws IOException {
        this.setCurrentClass(this.getCurrentClass());
        super.exportClass(outputDir, methodName);
        /*exportSubclasses(outputDir);
        Options.v().set_output_dir(outputDir);
        String fileName = SourceLocator.v().getFileNameFor(this.getCurrentClass(), Options.output_format_class);
        OutputStream streamOut = new JasminOutputStream(
            new FileOutputStream(fileName));
        PrintWriter writerOut = new PrintWriter(
            new OutputStreamWriter(streamOut));
        JasminClass jasminClass = new soot.jimple.JasminClass(this.getCurrentClass());
        jasminClass.print(writerOut);
        writerOut.flush();
        streamOut.close();*/
    }

}
