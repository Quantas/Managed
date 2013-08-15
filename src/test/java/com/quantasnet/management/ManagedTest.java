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

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.management.*;
import java.io.IOException;
import java.lang.management.ManagementFactory;

import static org.junit.Assert.assertTrue;

public class ManagedTest
{
    private final ObjectName testClassObjName = ManagementProcessor.constructObjectName(TestClass.class);
    private final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    private static final TestClass testClass = new TestClass();

    @BeforeClass
    public static void beforeClass()
    {
        ManagementProcessor.register(testClass);
    }

    @AfterClass
    public static void afterClass()
    {
        ManagementProcessor.unregister(testClass);
    }

    @Test
    public void managementTest_constructObjectName()
    {
        assertTrue(testClassObjName.getCanonicalName().equals("com.quantasnet.management:type=TestClass"));
    }

    @Test(expected = MBeanException.class)
    public void managementTest_noMethodException() throws InstanceNotFoundException, ReflectionException, MBeanException
    {
        server.invoke(testClassObjName, "unManaged", new Object[]{}, new String[]{});
    }

    @Test
    public void managementTest_invoke() throws InstanceNotFoundException, ReflectionException, MBeanException, IOException
    {
        server.invoke(testClassObjName, "execBooleans", new Object[]{true, false}, new String[]{"java.lang.Boolean", "boolean"});
        server.invoke(testClassObjName, "execIntegers", new Object[]{1, 2}, new String[]{"java.lang.Integer", "int"});
        server.invoke(testClassObjName, "execCharacters", new Object[]{'a', 'b'}, new String[]{"java.lang.Character", "char"});
        server.invoke(testClassObjName, "execLongs", new Object[]{0L, 1L}, new String[]{"java.lang.Long", "long"});
        server.invoke(testClassObjName, "execDoubles", new Object[]{0.0, 0.1}, new String[]{"java.lang.Double", "double"});
        server.invoke(testClassObjName, "execFloats", new Object[]{0.0f, 0.1f}, new String[]{"java.lang.Float", "float"});
        server.invoke(testClassObjName, "execBytes", new Object[]{(byte) 0x0, (byte) 0x0}, new String[]{"java.lang.Byte", "byte"});
    }

    @Test
    public void managementTest_attributeMethods() throws AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException, InvalidAttributeValueException
    {
        server.getAttribute(testClassObjName, "someString");
        Assert.assertEquals(server.getAttribute(testClassObjName, "someInt"), 1);
        server.getAttribute(testClassObjName, "someBoolean");
        server.setAttribute(testClassObjName, new Attribute("someInt", 2));
        Assert.assertEquals(server.getAttribute(testClassObjName, "someInt"), 2);
    }

    @Test(expected = MBeanException.class)
    public void managementTest_invalidParamException() throws InstanceNotFoundException, ReflectionException, MBeanException
    {
        server.invoke(testClassObjName, "execBytes", new Object[]{0, 0}, new String[]{"java.lang.Byte", "byte"});
    }

    @Test
    public void managementTest_getAttributePublic() throws AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException
    {
        server.getAttribute(testClassObjName, "publicString");
    }

    @Test
    public void managementTest_getAttribute() throws AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException
    {
        server.getAttribute(testClassObjName, "littleBoolean");
    }

    @Test(expected = MBeanException.class)
    public void managementTest_getAttributeException() throws AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException
    {
        server.getAttribute(testClassObjName, "fail");
    }

    @Test(expected = MBeanException.class)
    public void managementTest_getAttributeNotReadableException() throws AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException
    {
        server.getAttribute(testClassObjName, "notReadable");
    }

    @Test
    public void managementTest_setAttribute() throws AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException, InvalidAttributeValueException
    {
        server.setAttribute(testClassObjName, new Attribute("writableString", "Strings!"));
    }

    @Test(expected = MBeanException.class)
    public void managementTest_setAttributeException() throws AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException, InvalidAttributeValueException
    {
        server.setAttribute(testClassObjName, new Attribute("unManagedString", 0L));
    }

    @Test(expected = MBeanException.class)
    public void managementTest_setAttributeNotWritableException() throws AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException, InvalidAttributeValueException
    {
        server.setAttribute(testClassObjName, new Attribute("littleBoolean", true));
    }

    @Test
    public void managementTest_getAttributes() throws InstanceNotFoundException, ReflectionException
    {
        final AttributeList list = server.getAttributes(testClassObjName, new String[]{"littleBoolean", "bigBoolean"});
        assertTrue(list.size() == 2);
    }

    @Test
    public void managementTest_getAttributesNotReadableException() throws InstanceNotFoundException, ReflectionException
    {
        final AttributeList list = server.getAttributes(testClassObjName, new String[]{"littleBoolean", "notReadable"});
        assertTrue(list.size() == 1);
    }

    @Test
    public void managementTest_setAttributes() throws InstanceNotFoundException, ReflectionException
    {
        final AttributeList list = new AttributeList();
        list.add(new Attribute("writableString", "JUNIT"));

        final AttributeList afterList = server.setAttributes(testClassObjName, list);
        assertTrue(afterList.size() == 1);
    }

    @Test
    public void managementTest_setAttributesNotWritableException() throws InstanceNotFoundException, ReflectionException
    {
        final AttributeList list = new AttributeList();
        list.add(new Attribute("writableString", "JUNIT"));
        list.add(new Attribute("littleBoolean", false));

        final AttributeList afterList = server.setAttributes(testClassObjName, list);
        assertTrue(afterList.size() == 1);
    }

    @Test
    public void managementTest_noAnnotations()
    {
        ManagementProcessor.register(new TestClassUnManaged());
    }

    @Test
    public void managementTest_oneAnnotation()
    {
        ManagementProcessor.register(new TestClassOneAnnotation());
    }

    @Test
    public void managementTest_otherDynamic()
    {
        ManagementProcessor.register(new TestDynamic());
    }

    private static final class TestDynamic implements DynamicMBean
    {
        public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException
        {
            return null;
        }

        public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException
        {
        }

        public AttributeList getAttributes(String[] attributes)
        {
            return null;
        }

        public AttributeList setAttributes(AttributeList attributes)
        {
            return null;
        }

        public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException
        {
            return null;
        }

        public MBeanInfo getMBeanInfo()
        {
            return null;
        }

    }

    private static final class TestClassOneAnnotation
    {
        @Managed
        public TestClassOneAnnotation()
        {

        }
    }

    private static final class TestClassUnManaged
    {

    }

    private static final class TestClass
    {
        private String someString = "a String";

        private int someInt = 1;

        private boolean someBoolean = false;

        @Managed
        public TestClass()
        {

        }

        @Managed
        public boolean isSomeBoolean()
        {
            return someBoolean;
        }

        @Managed(writable=true)
        public void setSomeInt(final int integer)
        {
            this.someInt = integer;
        }

        @Managed
        public int getSomeInt()
        {
            return someInt;
        }

        @Managed(writable=true)
        public void setSomeString(final String string)
        {
            this.someString = string;
        }

        @Managed
        public String getSomeString()
        {
            return someString;
        }

        @SuppressWarnings("unused")
        private String unManagedString;

        @Managed(writable = true)
        private String writableString;

        @Managed(readable = false)
        private String notReadable;

        @Managed
        public String publicString;

        @Managed
        private boolean littleBoolean;

        @Managed
        private Boolean bigBoolean;

        @Managed
        private int littleInteger;

        @Managed
        private Integer bigInteger;

        @Managed
        private char littleCharacter;

        @Managed
        private Character bigCharacter;

        @Managed
        private long littleLong;

        @Managed
        private Long bigLong;

        @Managed
        private double littleDouble;

        @Managed
        private Double bigDouble;

        @Managed
        private float littleFloat;

        @Managed
        private Float bigFloat;

        @Managed
        private byte littleByte;

        @Managed
        private Byte bigByte;

        @Managed
        public void execBooleans(final Boolean big, final boolean little)
        {
            this.littleBoolean = little;
            this.bigBoolean = big;
        }

        @Managed
        public void execIntegers(final Integer big, final int little)
        {
            this.littleInteger = little;
            this.bigInteger = big;
        }

        @Managed
        public void execCharacters(final Character big, final char little)
        {
            this.littleCharacter = little;
            this.bigCharacter = big;
        }

        @Managed
        public void execLongs(final Long big, final long little)
        {
            this.littleLong = little;
            this.bigLong = big;
        }

        @Managed
        public void execDoubles(final Double big, final double little)
        {
            this.littleDouble = little;
            this.bigDouble = big;
        }

        @Managed
        public void execFloats(final Float big, final float little)
        {
            this.littleFloat = little;
            this.bigFloat = big;
        }

        @Managed
        public void execBytes(final Byte big, final byte little)
        {
            this.littleByte = little;
            this.bigByte = big;
        }

        public void unManaged()
        {
            //no-op
        }
    }
}