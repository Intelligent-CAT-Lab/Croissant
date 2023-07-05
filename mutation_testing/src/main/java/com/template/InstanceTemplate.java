package com.template;
import java.util.List;

public class InstanceTemplate extends TemplateAdder{

    protected InstanceTemplate(List<String> units, boolean jupiter, String projectDir) {
        super(units, jupiter, projectDir);
    }

    @Override
    public void run() {
        String jobRegistry =
            "final class JobRegistry {\n" +
            "    private static volatile JobRegistry instance;\n" +
            "    private static final Map<String, JobInstance> jobInstanceMap = new ConcurrentHashMap<>();\n" +
            "    private Map<String, JobScheduleController> schedulerMap = new ConcurrentHashMap<>();\n" +
            "    private Map<String, CoordinatorRegistryCenter> regCenterMap = new ConcurrentHashMap<>();\n" +
            "    private Map<String, Boolean> jobRunningMap = new ConcurrentHashMap<>();\n" +
            "    private Map<String, Integer> currentShardingTotalCountMap = new ConcurrentHashMap<>();\n" +
            "\n" +
            "    public void addJobInstance(final String jobName, final JobInstance jobInstance) {\n" +
            "        jobInstanceMap.put(jobName, jobInstance);\n" +
            "    }\n" +
            "\n" +
            "    public static JobInstance getJobInstance(final String jobName) {\n" +
            "        return jobInstanceMap.get(jobName);\n" +
            "    }\n" +
            "    public static JobRegistry getInstance() {\n" +
            "        if (null == instance) {\n" +
            "            synchronized (JobRegistry.class) {\n" +
            "                if (null == instance) {\n" +
            "                    instance = new JobRegistry();\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "        return instance;\n" +
            "    }\n" +
            "    public void registerJob(final String jobName, final JobScheduleController jobScheduleController, final CoordinatorRegistryCenter regCenter) {\n" +
            "        schedulerMap.put(jobName, jobScheduleController);\n" +
            "        regCenterMap.put(jobName, regCenter);\n" +
            "        regCenter.addCacheData(\"/\" + jobName);\n" +
            "    }\n" +
            "    public void shutdown(final String jobName) {\n" +
            "        JobScheduleController scheduleController = schedulerMap.remove(jobName);\n" +
            "        if (null != scheduleController) {\n" +
            "            scheduleController.shutdown();\n" +
            "        }\n" +
            "        CoordinatorRegistryCenter regCenter = regCenterMap.remove(jobName);\n" +
            "        if (null != regCenter) {\n" +
            "            regCenter.evictCacheData(\"/\" + jobName);\n" +
            "        }\n" +
            "        jobInstanceMap.remove(jobName);\n" +
            "        jobRunningMap.remove(jobName);\n" +
            "        currentShardingTotalCountMap.remove(jobName);\n" +
            "    }\n" +
            "}";


        this.addImport("import java.util.Properties;");
        this.addImport("import java.io.FileInputStream;");
        this.addImport("import java.sql.Timestamp;");
        this.addImport("import java.util.Map;");
        this.addImport("import java.util.concurrent.ConcurrentHashMap;");
        this.addImport("import org.apache.shardingsphere.elasticjob.lite.internal.schedule.JobScheduleController;");
        this.addImport("import org.apache.shardingsphere.elasticjob.reg.base.CoordinatorRegistryCenter;");
        this.addImport("import org.springframework.batch.core.*;");
        this.addField("static Timestamp instanceTimestamp = new Timestamp(System.currentTimeMillis());");
        this.addField("static String jobInstanceName = instanceTimestamp.toString();");


        if (jupiter) {
            this.addImport("import org.junit.jupiter.api.Assertions;\n");
//            this.addImport("import static org.junit.jupiter.api.Assertions.assertEquals;\n");
            this.addTest("    @Test\n" +
            "    public void CroissantMutant_OD_IVD_Victim_CustomClassIVMO() throws Exception {\n" +
            "        assertEquals(JobRegistry.getJobInstance(jobInstanceName),null);\n" +
            "    }");
        } else {
            this.addImport("import org.junit.Assert;\n");
            this.addImport("import static org.junit.Assert.assertEquals;\n");
            this.addTest("    @Test\n" +
            "    public void CroissantMutant_OD_IVD_Victim_CustomClassIVMO() throws Exception {\n" +
            "        Assert.assertEquals(JobRegistry.getJobInstance(jobInstanceName),null);\n" +
            "    }");
        }

        this.addField("static int defaultSharedVar = 0;\n");
        this.addField("static int newSharedVar = 10;\n");


        this.addTest("    @Test\n" +
            "    public void IVD_polluterMutant() throws Exception {\n" +
            "        JobRegistry.getInstance().addJobInstance(jobInstanceName, new JobInstance(1L,jobInstanceName));\n" +
            "    }");



        for (int i = 0;i < 50; i++){
            this.addTest("    @Test\n" +
                "    public void IVD" + i + "_cleanerMutant() throws Exception {\n" +
                "    int index = " + i +"; \n" +
                "    if (index >= getCleanerCountIVD()) {return;}\n" +
		        "    System.out.println(\"cleaner count: \" + getCleanerCountIVD());\n" +
	            "    System.out.println(\"valid cleaner: \" +" + i +");\n" +
                "    JobRegistry.getInstance().shutdown(jobInstanceName);\n" +
                "    }");
        }


        this.addTest(  "static double cleanerCounterIVD;" +
            "        public double getCleanerCountIVD() {\n" +
            "        try {\n"+
	        "        String path = System.getProperty(\"user.dir\");\n" +
            "        String var5 = this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath() + \"mutation.config\";\n" +
                "        System.out.println(var5);\n" +
            "        Properties var3 = new Properties();\n" +
            "        FileInputStream var4 = new FileInputStream(var5);\n" +
            "        var3.load(var4);\n" +
            "        var5 = var3.getProperty(\"mutation.count\");\n" +
            "        Double var6 = Double.valueOf(var5);\n" +
            "        double var1 = var6;\n" +
            "        return var1;" +
            "        } catch(Exception e) {\n" +
            "        System.out.println(\"configuration file cannot be accessed\");\n" +
            "        }\n"+
            "   return -1; }"
            );
        this.addClass(jobRegistry);

        this.addDependency("org.springframework", "spring-oxm", "5.3.0", "spring-oxm");
        this.addDependency("org.springframework", "spring-jdbc", "5.3.0", "spring-jdbc");
        this.addDependency("org.springframework.batch", "spring-batch-core", "4.3.0", "spring-batch-core");
        this.addDependency("org.springframework", "spring-core", "5.3.25", "spring-core");
        this.addDependency("org.apache.shardingsphere.elasticjob", "elasticjob-lite-core", "3.0.2", "elasticjob-lite-core");







    }
}
