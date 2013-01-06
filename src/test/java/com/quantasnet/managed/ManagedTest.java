package com.quantasnet.managed;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.management.ManagementFactory;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

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
		assertTrue(testClassObjName.getCanonicalName().equals("com.quantasnet.managed:type=TestClass"));
	}
	
	@Test(expected=MBeanException.class)
	public void managementTest_noMethodException() throws InstanceNotFoundException, ReflectionException, MBeanException
	{
		server.invoke(testClassObjName, "unManaged", new Object[]{},new String[]{});
	}
	
	@Test
	public void managementTest_invoke() throws InstanceNotFoundException, ReflectionException, MBeanException, IOException
	{
		server.invoke(testClassObjName, "setBooleans", new Object[]{true, false},new String[]{"java.lang.Boolean", "boolean"});
		server.invoke(testClassObjName, "setIntegers", new Object[]{1, 2},new String[]{"java.lang.Integer", "int"});
		server.invoke(testClassObjName, "setCharacters", new Object[]{'a', 'b'},new String[]{"java.lang.Character", "char"});
		server.invoke(testClassObjName, "setLongs", new Object[]{0L, 1L},new String[]{"java.lang.Long", "long"});
		server.invoke(testClassObjName, "setDoubles", new Object[]{0.0, 0.1},new String[]{"java.lang.Double", "double"});
		server.invoke(testClassObjName, "setFloats", new Object[]{0.0f, 0.1f},new String[]{"java.lang.Float", "float"});
		server.invoke(testClassObjName, "setBytes", new Object[]{(byte)0x0, (byte)0x0},new String[]{"java.lang.Byte", "byte"});
	}
	
	@Test(expected=MBeanException.class)
	public void managementTest_invalidParamException() throws InstanceNotFoundException, ReflectionException, MBeanException
	{
		server.invoke(testClassObjName, "setBytes", new Object[]{0, 0},new String[]{"java.lang.Byte", "byte"});
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
	
	@Test(expected=MBeanException.class)
	public void managementTest_getAttributeException() throws AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException
	{
		server.getAttribute(testClassObjName, "fail");
	}
	
	@Test(expected=MBeanException.class)
	public void managementTest_getAttributeNotReadableException() throws AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException
	{
		server.getAttribute(testClassObjName, "notReadable");
	}
	
	@Test
	public void managementTest_setAttribute() throws AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException, InvalidAttributeValueException
	{
		server.setAttribute(testClassObjName, new Attribute("writableString", "Strings!"));
	}
	
	@Test(expected=MBeanException.class)
	public void managementTest_setAttributeException() throws AttributeNotFoundException, InstanceNotFoundException, MBeanException, ReflectionException, InvalidAttributeValueException
	{
		server.setAttribute(testClassObjName, new Attribute("unManagedString", 0L));
	}
	
	@Test(expected=MBeanException.class)
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
		@Managed
		public TestClass()
		{
			
		}
		
		@SuppressWarnings("unused")
		private String unManagedString;
		
		@Managed(writable=true)
		private String writableString;
		
		@Managed(readable=false)
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
		public void setBooleans(final Boolean big, final boolean little)
		{
			this.littleBoolean = little;
			this.bigBoolean = big;
		}
		
		@Managed
		public void setIntegers(final Integer big, final int little)
		{
			this.littleInteger = little;
			this.bigInteger = big;
		}
		
		@Managed
		public void setCharacters(final Character big, final char little)
		{
			this.littleCharacter = little;
			this.bigCharacter = big;
		}
		
		@Managed
		public void setLongs(final Long big, final long little)
		{
			this.littleLong = little;
			this.bigLong = big;
		}
		
		@Managed
		public void setDoubles(final Double big, final double little)
		{
			this.littleDouble = little;
			this.bigDouble = big;
		}
		
		@Managed
		public void setFloats(final Float big, final float little)
		{
			this.littleFloat = little;
			this.bigFloat = big;
		}
		
		@Managed
		public void setBytes(final Byte big, final byte little)
		{
			this.littleByte = little;
			this.bigByte = big;
		}
		
		@SuppressWarnings("unused")
		public void unManaged()
		{
			//no-op
		}
	}
}