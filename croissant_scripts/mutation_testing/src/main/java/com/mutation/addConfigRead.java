package com.mutation;

import soot.*;
import soot.jimple.*;

import java.util.ArrayList;
import java.util.List;

public class addConfigRead extends SootMutationOperator {

    public SootMethod createGetThresholdMethod() {
        List<Stmt> execStmts = new ArrayList<>();

        SootClass propertiesClass = Scene.v().forceResolve("java.util.Properties", SootClass.BODIES);
        SootMethod propertiesInitMethod = propertiesClass.getMethod("void <init>()");


        try {
            propertiesClass.addMethod(propertiesInitMethod);
        } catch (Exception e) {

        }


        SootClass fileInputStreamClass = Scene.v().forceResolve("java.io.FileInputStream", SootClass.SIGNATURES);

        SootMethod fileInputStreamInitMethod = null;
        List<Type> params = new ArrayList<>();
        params.add(RefType.v("java.lang.String"));
        fileInputStreamInitMethod = fileInputStreamClass.getMethod("void <init>(java.lang.String)");
        /*fileInputStreamInitMethod.setPhantom(true);*/
        /*try {

            fileInputStreamClass.addMethod(fileInputStreamInitMethod);
        } catch (Exception e) {
            e.printStackTrace();
        }*/


        Local stringLocalFileName = Jimple.v().newLocal("fileName", RefType.v("java.lang.String"));
        Local propertyString = Jimple.v().newLocal("propertyString", RefType.v("java.lang.String"));
        Local properties = Jimple.v().newLocal("properties", propertiesClass.getType());
        Local $properties = Jimple.v().newLocal("$properties", propertiesClass.getType());
        Local fileInputStream = Jimple.v().newLocal("fileInputStream", fileInputStreamClass.getType());
        Local $fileInputStream = Jimple.v().newLocal("$fileInputStream", fileInputStreamClass.getType());
        Local threshold = Jimple.v().newLocal("threshold", RefType.v("java.lang.Double"));
        Local primThreshold = Jimple.v().newLocal("primThreshold", DoubleType.v());

        //r0 = "mutation.config";
        AssignStmt initStringLocalFileName = Jimple.v().newAssignStmt(stringLocalFileName, StringConstant.v("mutation.config"));
        execStmts.add(initStringLocalFileName);

        //$r1 = new java.util.Properties;
        NewExpr newProperties = Jimple.v().newNewExpr(propertiesClass.getType());
        AssignStmt init$Properties = Jimple.v().newAssignStmt($properties, newProperties);
        execStmts.add(init$Properties);

        //specialinvoke $r1.<java.util.Properties: void <init>()>();

        SpecialInvokeExpr propertiesSpecialInvokeExpr = Jimple.v().newSpecialInvokeExpr($properties, propertiesInitMethod.makeRef());
        InvokeStmt propertiesSpecialInvokeStmt = Jimple.v().newInvokeStmt(propertiesSpecialInvokeExpr); //FIXME
        execStmts.add(propertiesSpecialInvokeStmt);

        //r2 = $r1;
        AssignStmt initProperties = Jimple.v().newAssignStmt(properties, $properties);
        execStmts.add(initProperties);

        //$r3 = new java.io.FileInputStream;
        NewExpr newFileInputStream = Jimple.v().newNewExpr(fileInputStreamClass.getType());
        AssignStmt init$FileInputStream = Jimple.v().newAssignStmt($fileInputStream, newFileInputStream);
        execStmts.add(init$FileInputStream);

        //specialinvoke $r3.<java.io.FileInputStream: void <init>(java.lang.String)>(r0);
        SpecialInvokeExpr fileInputStreamInitExpr = Jimple.v().newSpecialInvokeExpr($fileInputStream, fileInputStreamInitMethod.makeRef(), stringLocalFileName);
        InvokeStmt fileInputStreamStmt = Jimple.v().newInvokeStmt(fileInputStreamInitExpr);
        execStmts.add(fileInputStreamStmt);

        //r4 = $r3;
        AssignStmt fileInputStreamAssignStmt = Jimple.v().newAssignStmt(fileInputStream, $fileInputStream);
        execStmts.add(fileInputStreamAssignStmt);

        //virtualinvoke r2.<java.util.Properties: void load(java.io.InputStream)>(r4);
        SootMethod loadMethod = propertiesClass.getMethod("void load(java.io.InputStream)");
        VirtualInvokeExpr loadInputStream = Jimple.v().newVirtualInvokeExpr(properties, loadMethod.makeRef(), fileInputStream);
        InvokeStmt loadInputStmt = Jimple.v().newInvokeStmt(loadInputStream);
        execStmts.add(loadInputStmt);

        //$r5 = virtualinvoke r2.<java.util.Properties: java.lang.String getProperty(java.lang.String)>("mutation.threshold");
        SootMethod getPropertyMethod = propertiesClass.getMethod("java.lang.String getProperty(java.lang.String)");
        VirtualInvokeExpr virtualInvokeExpr = Jimple.v().newVirtualInvokeExpr(properties, getPropertyMethod.makeRef(), StringConstant.v("mutation.threshold"));
        AssignStmt initPropertyString = Jimple.v().newAssignStmt(propertyString, virtualInvokeExpr);
        execStmts.add(initPropertyString);

        //r6 = staticinvoke <java.lang.Double: java.lang.Double valueOf(java.lang.String)>($r5);
        SootClass doubleClass = Scene.v().getSootClass("java.lang.Double");
        SootMethod doubleValueOfMethod = doubleClass.getMethod("java.lang.Double valueOf(java.lang.String)");
        StaticInvokeExpr doubleValueOfExpr = Jimple.v().newStaticInvokeExpr(doubleValueOfMethod.makeRef(), propertyString);
        AssignStmt doubleAssignStmt = Jimple.v().newAssignStmt(threshold, doubleValueOfExpr);
        execStmts.add(doubleAssignStmt);

        //$d0 = virtualinvoke r6.<java.lang.Double: double doubleValue()>();
        SootMethod doubleValueMethod = doubleClass.getMethod("double doubleValue()");
        VirtualInvokeExpr doubleValueInvokeExpr = Jimple.v().newVirtualInvokeExpr(threshold, doubleValueMethod.makeRef());
        AssignStmt doubleValueAssignStmt = Jimple.v().newAssignStmt(primThreshold, doubleValueInvokeExpr);
        execStmts.add(doubleValueAssignStmt);

        /*execStmts = new ArrayList<>();*/

        /*AssignStmt debug = Jimple.v().newAssignStmt(primThreshold, DoubleConstant.v(1.0D));
        execStmts.add(debug);*/

        SootMethod method = this.createMethod("getThreshold", execStmts, primThreshold, new ArrayList<>());

        method.getActiveBody().getLocals().add(stringLocalFileName);
        method.getActiveBody().getLocals().add(propertyString);
        method.getActiveBody().getLocals().add(properties);
        method.getActiveBody().getLocals().add($properties);
        method.getActiveBody().getLocals().add(fileInputStream);
        method.getActiveBody().getLocals().add($fileInputStream);
        method.getActiveBody().getLocals().add(threshold);

        //method.getActiveBody().getLocals().add(primThreshold);

        /*SootClass exceptionClass = Scene.v().loadClassAndSupport("java.io.Exception");
        exceptionClass.setName("IOException");
        method.addException(exceptionClass);*/

        return method;
    }

    @Override
    public void mutateMethod() throws Exception {

        try {
            this.getCurrentClass().getMethodByName("getThreshold");
        } catch (Exception e) {
            SootMethod getThresholdMethod = this.createGetThresholdMethod();
            this.getCurrentClass().addMethod(getThresholdMethod);
        }
    }

    @Override
    public int getMutantNumber() {
        return 0;
    }

    @Override
    public <T extends Unit> List<T> locateUnits() {
        return null;
    }

    @Override
    public boolean isApplicable() {
        return true;
    }
}
