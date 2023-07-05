//package com.mutation.unorderedCollections.assertionInsert;
//
//import com.mutation.unorderedCollections.util.StringEditor;
//import soot.Local;
//import soot.jimple.InstanceInvokeExpr;
//import soot.jimple.Stmt;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//
//public class JsonUAMO extends UnorderedAssertionMO{
//    public JsonUAMO() {
//        super(Collections.singletonList("org.json.JSONObject"), "put");
//        this.stringEditor = new StringEditor("{","}",",");
//    }
//
//
//
//    @Override
//    public List<String> createContents() {
//        return findUnorderedCollectionContents(this.targetLocal, ":");
//    }
//}
