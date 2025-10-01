/*
 * Copyright © 2025 Oracle and/or its affiliates.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

package com.example.demo;

import java.lang.reflect.Method;

class StringReverser {
    static String reverse(String input) {
        return new StringBuilder(input).reverse().toString();
    }
}

class StringCapitalizer {
    static String capitalize(String input) {
        return input.toUpperCase();
    }
}

public class DemoApplication {
    public static void main(String[] args) throws ReflectiveOperationException, IllegalArgumentException {
        DemoApplication demo = new DemoApplication();
        System.out.println(demo.doSomething(args));
    }

    public DemoApplication() {
    }

    public String doSomething(String[] args) throws ReflectiveOperationException, IllegalArgumentException {
        if (args == null || args.length != 3) {
            //
            throw new IllegalArgumentException("Usage : Class Method InputString");
        }
        String className = args[0];
        String methodName = args[1];
        String input = args[2];

        Class<?> clazz = Class.forName(className);
        Method method = clazz.getDeclaredMethod(methodName, String.class);
        String result = (String)method.invoke(null, input);
        return result;
    }
}
