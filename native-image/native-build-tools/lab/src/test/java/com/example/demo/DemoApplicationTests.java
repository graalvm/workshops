package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DemoApplicationTests {

	/**
	 * Test that the application throws an IllegalArgumentException when you fail to pass in any parameters
	 * @throws ReflectiveOperationException
	 * @throws Exception
	 */
	@Test
	void testNoParams() throws ReflectiveOperationException, Exception {
		String[] args = {};
		DemoApplication demo = new DemoApplication();
		assertThrows(IllegalArgumentException.class, () -> {demo.doSomething(args);});
	}


	/**
	 * Test calling the StringReverse class
	 * @throws IllegalArgumentException
	 * @throws ReflectiveOperationException
	 * @throws Exception
	 */
	@Test
	void testReverse() throws IllegalArgumentException, ReflectiveOperationException, Exception {
		String[] args = {"com.example.demo.StringReverser", "reverse", "java"};
		DemoApplication demo = new DemoApplication();
		assertEquals(demo.doSomething(args), "avaj", "Failed");
	}

	/**
	 * Test calling the StringCapitalise class
	 * @throws Exception
	 */
	@Test
	void testCapitalise() throws Exception {
		String[] args = {"com.example.demo.StringCapitalizer", "capitalize", "java"};
		DemoApplication demo = new DemoApplication();
		assertEquals(demo.doSomething(args), "JAVA", "Failed");
	}

	/**
	 * Test that we throw a ReflectiveOperationException when we try to call a class that doesn't exist
	 * @throws ReflectiveOperationException
	 * @throws Exception
	 */
	@Test
	void testNonExistantClass() throws ReflectiveOperationException, Exception {
		String[] args = {"com.example.demo.IDontExist", "reverse", "java"};
		DemoApplication demo = new DemoApplication();
		Throwable exception = assertThrows(ReflectiveOperationException.class, 
			() -> {demo.doSomething(args);});
	}

	/**
	 * Test that we throw a ReflectiveOperationException when we try to call a method that doesn't exist
	 * @throws ReflectiveOperationException
	 * @throws Exception
	 */
	@Test
	void testNonExistantMethod() throws ReflectiveOperationException, Exception {
		String[] args = {"com.example.demo.IDontExist", "iDontExist", "java"};
		DemoApplication demo = new DemoApplication();
		Throwable exception = assertThrows(ReflectiveOperationException.class, 
			() -> {demo.doSomething(args);});
	}
}
