package com.framework;

import java.util.HashMap;
import java.util.Map;

public class Bundle {
    private final Map<String, Values> mutationOperatorMap;

    public Bundle() {
        this.mutationOperatorMap = new HashMap<String, Values>() {{
            put("SVD", new Values("StaticTemplate","ODStaticVariableMutationOperator")); //Static Variable Pollution
            put("TPFD", new Values("MockitoTemplate","MockitoMutationOperator")); //Third Party Framework Dependency
            put("CSD", new Values("CacheTemplate","CaffeineCDMO")); //Cached Status Dependency
            put("DSD", new Values("DatabaseTemplate","DatabaseMutationOperator")); //Database State Dependency
            put("FPD", new Values("FileTemplate","FileObjectFMO,FileOutputStreamFSFMO,FileStringFMO,PathFSFMO,RandomAccessFileFSFMO,StringFileWriterFSFMO,TempFileFSFMO")); //File Permission Dependency
            put("CCUD", new Values("TimeoutTemplate","GlobalTMO,LocalTMO")); //Compiled or Cached Unit Dependency
            put("RA", new Values("FileTemplate","newFileNullODMO")); //Resource Availability
            put("MD", new Values("","memoryBoundViolationMO")); //Memory Dependency
            put("PD", new Values("ServerTemplate","jettyServerSetHardcodedPortMO,jettyServerSetNonHardcodedPortMO")); //Platform Dependency
            put("STiD", new Values("TimezoneTemplate","TimeZoneDependencyMO")); //System Time Dependency
            put("ASW", new Values("LatchTemplate,ThreadSleepTemplate","ThreadSleepMO,VoidLatchAwaitMO,LatchObjectMO"));
            put("CT", new Values("DeadLockTemplate","DeadLockMutationOperator")); //Concurrency Timeout
            put("RC", new Values("","RaceConditionMutationOperator")); //Race Condition
            put("UCIA", new Values("","JsonUCIMO,MapUCIMO,SetUCIMO")); //Unordered Collection Index Access
            put("IUC", new Values("","JsonIUCMO,MapIUCMO,SetIUCMO")); //Iterating Unordered Collections
            put("UCC", new Values("","HashMapUAMO,HashSetUAMO,JsonUAMO")); //Unordered Collections Conversion
            put("RAM", new Values("ReflectionTemplate","UnorderedPopulationMutationOperator")); //Reflection API Misconception
        }};
    }

    public Values getValues(String mutationOperators) {
        String[] parts = mutationOperators.split(",");
        StringBuilder operatorInstances = new StringBuilder();
        StringBuilder templates = new StringBuilder();

        for (String part: parts) {
            Values values = getValue(part);
            operatorInstances.append(values.operatorInstances);
            templates.append(values.templates);
        }

        return new Values(templates.toString(), operatorInstances.toString());
    }

    private Values getValue(String mutationOperator) {
        if (this.mutationOperatorMap.containsKey(mutationOperator)) {
            return mutationOperatorMap.get(mutationOperator);
        } else {
            return new Values("","");
        }
    }
}


class Values {

    public Values(String templates, String operatorInstances) {
        this.templates = templates;
        this.operatorInstances = operatorInstances;
    }

    public String templates;
    public String operatorInstances;
}
