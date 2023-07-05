package com.framework;

import soot.Value;

import java.util.HashMap;
import java.util.Map;

public class Bundle {
    private final Map<String, Values> mutationOperatorMap;

    public Bundle() {
        this.mutationOperatorMap = new HashMap<String, Values>() {{
             //put("CCUD", new Values("TimeoutTemplate","GlobalTMO,LocalTMO")); //Compiled or Cached Unit Dependency
            put("MD", new Values("","memoryBoundViolationMO")); //Memory Dependency
            put("PD", new Values("ServerTemplate","jettyServerSetHardcodedPortMO,jettyServerSetNonHardcodedPortMO")); //Platform Dependency
            put("STDZ", new Values("TimezoneTemplate","TimeZoneDependencyMO")); //System Time Dependency, STiD
            put("AW", new Values("LatchTemplate,ThreadSleepTemplate","ThreadSleepMO,VoidLatchAwaitMO,LatchObjectMO")); //ASW
            put("CTD", new Values("DeadLockTemplate","DeadLockMutationOperator")); //Concurrency Timeout, CT
            put("RC", new Values("","RaceConditionMutationOperator")); //Race Condition
            put("UCIA", new Values("","UnorderedCollectionIndexMutationOperator")); //Unordered Collection Index Access,"JsonUCIMO,MapUCIMO,SetUCIMO"
            put("UCC", new Values("","OrderedStringConversionMutationOperator")); //Unordered Collections Conversion
            put("RAM", new Values("ReflectionTemplate","UnorderedPopulationMutationOperator")); //Reflection API Misconception
            put("STDV", new Values("","TimeValueInitializationMutationOperator")); //TVIMO
            put("TRR", new Values("","TRRInjectAssertLocal,WIPTRRInjectAssertInstance")); //TRRMO
            // OD
            put("IVD",new Values("InstanceTemplate","CustomClassIVMO"));//,PrimitiveIVMO,ListIVMO,SetIVMO,WrapperIVMO,MapIVMO
            put("FPD", new Values("FileWriterTemplate","FileObjectFMO,FileOutputStreamFSFMO,FileStringFMO,PathFSFMO,RandomAccessFileFSFMO,StringFileWriterFSFMO,TempFileFSFMO")); //File Permission Dependency
            put("CSD", new Values("CacheTemplate","CaffeineCDMO")); //Cached Status Dependency
            put("SVD", new Values("StaticTemplate","StaticSVMO")); //Static Variable Pollution
            put("TPFD", new Values("MockitoTemplate","MockitoMutationOperator")); //Third Party Framework Dependency
            put("DSD", new Values("DatabaseTemplate","DatabaseMutationOperator")); //Database State Dependency
            put("RA", new Values("FileTemplate","newFileNullODMO")); //Resource Availability



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

    public String getTemplate(String mutationOperator){
        for (Object OpKey:this.mutationOperatorMap.keySet()){
            if((this.mutationOperatorMap.get(OpKey).operatorInstances).toString().contains(mutationOperator)) {
                return mutationOperatorMap.get(OpKey).templates;
            }
        }
        return "NoTemplate";
    }

    public String getKey(String mutationOperator) {
        for(Object OpKey:this.mutationOperatorMap.keySet()){
            if((this.mutationOperatorMap.get(OpKey).operatorInstances).toString().contains(mutationOperator)) {
                return OpKey.toString();
            }
        }
        return "NoOpKey";
    }

    public Values getValue(String mutationOperator) {
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
