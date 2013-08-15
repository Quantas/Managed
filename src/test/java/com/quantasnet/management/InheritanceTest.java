/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 Andrew Landsverk
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.quantasnet.management;

import org.junit.Assert;
import org.junit.Test;

import javax.management.Attribute;
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


        server.setAttribute(testClassObjName, new Attribute("string", "A String"));
        Assert.assertEquals(server.getAttribute(testClassObjName, "string"), "A String");
    }

    private static abstract class TestParent
    {

        private String aString = "Strings!";

        @Managed
        public String getString()
        {
            return aString;
        }

        @Managed
        private void setString(String string)
        {
            this.aString = string;
        }


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