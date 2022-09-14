package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DemoApplicationTests {

	@Test
	void testNoParams() throws ReflectiveOperationException, Exception {
		String[] args = {};
		DemoApplication demo = new DemoApplication();
		assertThrows(IllegalArgumentException.class, () -> {demo.doSomething(args);});
	}


	@Test
	void testReverse() throws IllegalArgumentException, ReflectiveOperationException, Exception {
		String[] args = {"com.example.demo.StringReverser", "reverse", "java"};
		DemoApplication demo = new DemoApplication();
		assertEquals(demo.doSomething(args), "avaj", "Failed");
	}

	@Test
	void testCapitalise() throws Exception {
		String[] args = {"com.example.demo.StringCapitalizer", "capitalize", "java"};
		DemoApplication demo = new DemoApplication();
		assertEquals(demo.doSomething(args), "JAVA", "Failed");
	}

	@Test
	void testNonExistantClass() throws ReflectiveOperationException, Exception {
		String[] args = {"com.example.demo.IDontExist", "reverse", "java"};
		DemoApplication demo = new DemoApplication();
		Throwable exception = assertThrows(ReflectiveOperationException.class, 
			() -> {demo.doSomething(args);});
	}

	@Test
	void testNonExistantMethod() throws ReflectiveOperationException, Exception {
		String[] args = {"com.example.demo.IDontExist", "iDontExist", "java"};
		DemoApplication demo = new DemoApplication();
		Throwable exception = assertThrows(ReflectiveOperationException.class, 
			() -> {demo.doSomething(args);});
	}}
