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
        final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        final ObjectName testClassObjName = ManagementProcessor.constructObjectName(TestChild.class);

        final TestChild child = new TestChild();
        ManagementProcessor.register(child);

        server.invoke(testClassObjName, "childStuff", new Object[]{}, new String[]{});
        server.invoke(testClassObjName, "doStuff", new Object[]{}, new String[]{});
    }

    private static abstract class TestParent
    {
        @Managed
        public void doStuff()
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