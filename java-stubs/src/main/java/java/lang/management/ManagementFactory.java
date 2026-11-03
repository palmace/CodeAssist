package java.lang.management;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.management.*;
import javax.management.loading.ClassLoaderRepository;

public class ManagementFactory {
    public static final String CLASS_LOADING_MXBEAN_NAME = "java.lang:type=ClassLoading";
    public static final String COMPILATION_MXBEAN_NAME = "java.lang:type=Compilation";
    public static final String MEMORY_MXBEAN_NAME = "java.lang:type=Memory";
    public static final String OPERATING_SYSTEM_MXBEAN_NAME = "java.lang:type=OperatingSystem";
    public static final String RUNTIME_MXBEAN_NAME = "java.lang:type=Runtime";
    public static final String THREAD_MXBEAN_NAME = "java.lang:type=Threading";

    private ManagementFactory() {}

    public static ClassLoadingMXBean getClassLoadingMXBean() {
        return new ClassLoadingMXBean() {
            public long getTotalLoadedClassCount() { return 0; }
            public int getLoadedClassCount() { return 0; }
            public long getUnloadedClassCount() { return 0; }
            public boolean isVerbose() { return false; }
            public void setVerbose(boolean value) {}
        };
    }

    public static MemoryMXBean getMemoryMXBean() {
        return new MemoryMXBean() {
            public int getObjectPendingFinalizationCount() { return 0; }
            public MemoryUsage getHeapMemoryUsage() { return null; }
            public MemoryUsage getNonHeapMemoryUsage() { return null; }
            public boolean isVerbose() { return false; }
            public void setVerbose(boolean value) {}
            public void gc() {}
        };
    }

    public static ThreadMXBean getThreadMXBean() {
        return new ThreadMXBean() {
            public int getThreadCount() { return 0; }
            public int getPeakThreadCount() { return 0; }
            public long getTotalStartedThreadCount() { return 0; }
            public int getDaemonThreadCount() { return 0; }
            public long[] getAllThreadIds() { return new long[0]; }
            public ThreadInfo getThreadInfo(long id) { return null; }
            public ThreadInfo[] getThreadInfo(long[] ids) { return new ThreadInfo[0]; }
            public ThreadInfo getThreadInfo(long id, int maxDepth) { return null; }
            public ThreadInfo[] getThreadInfo(long[] ids, int maxDepth) { return new ThreadInfo[0]; }
            public boolean isThreadContentionMonitoringSupported() { return false; }
            public boolean isThreadContentionMonitoringEnabled() { return false; }
            public void setThreadContentionMonitoringEnabled(boolean enable) {}
            public long getCurrentThreadCpuTime() { return 0; }
            public long getCurrentThreadUserTime() { return 0; }
            public long getThreadCpuTime(long id) { return 0; }
            public long getThreadUserTime(long id) { return 0; }
            public boolean isThreadCpuTimeSupported() { return false; }
            public boolean isCurrentThreadCpuTimeSupported() { return false; }
            public boolean isThreadCpuTimeEnabled() { return false; }
            public void setThreadCpuTimeEnabled(boolean enable) {}
            public long[] findMonitorDeadlockedThreads() { return new long[0]; }
            public void resetPeakThreadCount() {}
            public long[] findDeadlockedThreads() { return new long[0]; }
            public boolean isObjectMonitorUsageSupported() { return false; }
            public boolean isSynchronizerUsageSupported() { return false; }
            public ThreadInfo[] getThreadInfo(long[] ids, boolean lm, boolean ls) { return new ThreadInfo[0]; }
            public ThreadInfo[] dumpAllThreads(boolean lm, boolean ls) { return new ThreadInfo[0]; }
        };
    }

    public static RuntimeMXBean getRuntimeMXBean() {
        return new RuntimeMXBean() {
            public String getName() { return ""; }
            public String getVmName() { return ""; }
            public String getVmVendor() { return ""; }
            public String getVmVersion() { return ""; }
            public String getSpecName() { return ""; }
            public String getSpecVendor() { return ""; }
            public String getSpecVersion() { return ""; }
            public String getManagementSpecVersion() { return ""; }
            public String getClassPath() { return ""; }
            public String getLibraryPath() { return ""; }
            public boolean isBootClassPathSupported() { return false; }
            public String getBootClassPath() { return null; }
            public List<String> getInputArguments() { return Collections.emptyList(); }
            public long getUptime() { return 0; }
            public long getStartTime() { return 0; }
            public Map<String, String> getSystemProperties() { return Collections.emptyMap(); }
        };
    }

    public static synchronized MBeanServer getPlatformMBeanServer() {
        return new MBeanServer() {
            public ObjectInstance createMBean(String v1, ObjectName v2) { return null; }
            public ObjectInstance createMBean(String v1, ObjectName v2, ObjectName v3) { return null; }
            public ObjectInstance createMBean(String v1, ObjectName v2, Object[] v3, String[] v4) { return null; }
            public ObjectInstance createMBean(String v1, ObjectName v2, ObjectName v3, Object[] v4, String[] v5) { return null; }
            public ObjectInstance registerMBean(Object v1, ObjectName v2) { return null; }
            public void unregisterMBean(ObjectName v1) {}
            public ObjectInstance getObjectInstance(ObjectName v1) { return null; }
            public Set<ObjectInstance> queryMBeans(ObjectName v1, QueryExp v2) { return Collections.emptySet(); }
            public Set<ObjectName> queryNames(ObjectName v1, QueryExp v2) { return Collections.emptySet(); }
            public boolean isRegistered(ObjectName v1) { return false; }
            public Integer getMBeanCount() { return 0; }
            public Object getAttribute(ObjectName v1, String v2) { return null; }
            public AttributeList getAttributes(ObjectName v1, String[] v2) { return null; }
            public void setAttribute(ObjectName v1, Attribute v2) {}
            public AttributeList setAttributes(ObjectName v1, AttributeList v2) { return null; }
            public Object invoke(ObjectName v1, String v2, Object[] v3, String[] v4) { return null; }
            public String getDefaultDomain() { return ""; }
            public String[] getDomains() { return new String[0]; }
            public void addNotificationListener(ObjectName v1, NotificationListener v2, NotificationFilter v3, Object v4) {}
            public void addNotificationListener(ObjectName v1, ObjectName v2, NotificationFilter v3, Object v4) {}
            public void removeNotificationListener(ObjectName v1, ObjectName v2) {}
            public void removeNotificationListener(ObjectName v1, ObjectName v2, NotificationFilter v3, Object v4) {}
            public void removeNotificationListener(ObjectName v1, NotificationListener v2) {}
            public void removeNotificationListener(ObjectName v1, NotificationListener v2, NotificationFilter v3, Object v4) {}
            public MBeanInfo getMBeanInfo(ObjectName v1) { return null; }
            public boolean isInstanceOf(ObjectName v1, String v2) { return false; }
            public Object instantiate(String v1) { return null; }
            public Object instantiate(String v1, ObjectName v2) { return null; }
            public Object instantiate(String v1, Object[] v2, String[] v3) { return null; }
            public Object instantiate(String v1, ObjectName v2, Object[] v3, String[] v4) { return null; }
            public ClassLoader getClassLoaderFor(ObjectName v1) { return null; }
            public ClassLoader getClassLoader(ObjectName v1) { return null; }
            public ClassLoaderRepository getClassLoaderRepository() { return null; }
            public java.io.ObjectInputStream deserialize(ObjectName n, byte[] d) { return null; }
            public java.io.ObjectInputStream deserialize(String c, byte[] d) { return null; }
            public java.io.ObjectInputStream deserialize(String c, ObjectName l, byte[] d) { return null; }
        };
    }
}