Overview
========

The goal of this library is to make it so you no longer need an interface with a name ending in MBean to add an object to the JMX console.  You simply add the annotation to the appropriate methods and instance variables.


## Adding Managed to your project

###Maven
```xml
<dependency>
  <groupId>com.quantasnet.management</groupId>
  <artifactId>managed</artifactId>
  <version>1.0.1</version>
</dependency>
```

###Manual Method
You may also download the jar and add it to your classpath, if you do this you will also need to make sure the slf4j-api library is available on the classpath as well.

## Usage

Consider the following class, you can see that the annotation has been added to a few instance variables and a method.

```java
package test;

import com.quantasnet.management.Managed;

public PlainObject
{
    // writable defaults to false
    @Managed(writable = true)
    private String testString;
    
    // readable defaults to true
    @Managed(readable = false)
    private String writeOnlyString;
    
    private String unManagedString;
    
    @Managed
    public void someMethod()
    {
    	// method body here
    }
    
    public void unManagedMethod()
    {
    	// not available from JMX
    }
}
```

The annoation does not act alone however as an instance of an object is requred when registering objects in the JMX console.  You will need to create an instance and register it as seen below:

```java
    // create an instance of your object
    final PlainObject object = new PlainObject();
    ManagementProcessor.register(object);
```

Now when you run your application you should see a new object registered with the automatically generated `ObjectName` of `test:type=PlainObject`
