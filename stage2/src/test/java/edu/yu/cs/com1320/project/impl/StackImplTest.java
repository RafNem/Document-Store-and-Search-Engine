package edu.yu.cs.com1320.project.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StackImplTest {

    @Test
    void push() {
        StackImpl<String> stackTest1 = new StackImpl<>();
        stackTest1.push("Hello");
        stackTest1.push("World");
        stackTest1.push("Shalom");
        stackTest1.push("It Works");
        assertEquals("It Works", stackTest1.peek());
        assertEquals(4, stackTest1.size());
    }

    @Test
    void pop() {
        StackImpl<String> stackTest1 = new StackImpl<>();
        stackTest1.push("Hello");
        stackTest1.push("World");
        stackTest1.push("Shalom");
        stackTest1.push("It Works");
        assertEquals("It Works", stackTest1.peek());
        assertEquals(4, stackTest1.size());
        assertEquals("It Works", stackTest1.pop());
        stackTest1.pop();
        assertEquals(2, stackTest1.size());
        assertEquals("World", stackTest1.peek());
        stackTest1.pop();
        stackTest1.pop();
        stackTest1.pop();
        assertEquals(null,stackTest1.peek());
        assertEquals(null, stackTest1.pop());
        assertEquals(0, stackTest1.size());
    }

    @Test
    void peek() {
        StackImpl<String> stackTest1 = new StackImpl<>();
        stackTest1.push("Hello");
        stackTest1.push("World");
        stackTest1.push("Shalom");
        stackTest1.push("It Works");
        assertEquals("It Works", stackTest1.peek());
        stackTest1.pop();
        assertEquals("Shalom", stackTest1.peek());
    }

    @Test
    void size() {
        StackImpl<Character> stackTest = new StackImpl<>();
        for(char i = 'A'; i <= 'Z'; i++){
            stackTest.push(i);
        }
        assertEquals(26, stackTest.size());
    }
}