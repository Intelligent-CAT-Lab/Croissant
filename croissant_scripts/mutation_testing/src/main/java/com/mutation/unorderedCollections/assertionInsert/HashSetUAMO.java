package com.mutation.unorderedCollections.assertionInsert;

import com.mutation.unorderedCollections.util.StringEditor;
import soot.Local;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HashSetUAMO extends  UnorderedAssertionMO {

    public HashSetUAMO() {
        super(Arrays.asList("java.util.HashSet", "java.util.Set"), "add");
        this.stringEditor = new StringEditor("[","]",", ");
    }

    @Override
    public List<String> createContents() {

        return findUnorderedCollectionContents(this.targetLocal);
    }
}
