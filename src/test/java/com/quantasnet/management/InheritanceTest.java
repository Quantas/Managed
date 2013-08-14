package com.quantasnet.management;

import org.junit.Test;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

public class InheritanceTest
{
    @Test
    public void testInheritance() throws Exception
    {
        final Object[] objArray = new Object[0];
        final String[] stringArray = new String[0];
        
        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        final ObjectName testClassObjName = ManagementProcessor.constructObjectName(TestChild.class);

        final TestChild child = new TestChild();
        ManagementProcessor.register(child);

        server.invoke(testClassObjName, "childStuff", objArray, stringArray);
        server.invoke(testClassObjName, "doStuff", objArray, stringArray);
        server.invoke(testClassObjName, "protectedStuff", objArray, stringArray);
    }

    private static abstract class TestParent
    {
        @Managed
        public void doStuff()
        {
        }

        @Managed
        protected void protectedStuff()
        {
        }
    }

    private static class TestChild extends TestParent
    {
        @Managed
        public void childStuff()
        {
        }
    }
}