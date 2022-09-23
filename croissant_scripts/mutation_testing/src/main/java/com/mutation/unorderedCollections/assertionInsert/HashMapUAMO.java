package com.mutation.unorderedCollections.assertionInsert;

import com.mutation.unorderedCollections.util.StringEditor;
import soot.Local;
import soot.Unit;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.Stmt;
import soot.util.Chain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/*
    HashSet mp = ...;

   mp.add(1);
   mp.add(2);
   mp.add(3);

   AssertEqual("[1,2,3]", mp.toString());
   AssertEqual("[1,3,2]", mp.toString());

   ------

   HashSet mp = ...;

   mp.add(1);
   mp.add(2);
   mp.add(3);

   AssertEqual("[435,1234,653]", mp.toString());


 */


public class HashMapUAMO extends UnorderedAssertionMO{
    public HashMapUAMO() {
        //TODO: skip LinkedHashMap
        super(Arrays.asList("java.util.HashMap", "java.util.Map"), "put");
        this.stringEditor = new StringEditor("{","}",", ");
    }

    public List<String> createContents() {
        return findUnorderedCollectionContents(this.targetLocal, "=");
    }
}
