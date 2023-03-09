package edu.yu.cs.com1320.project.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HashTableImplTest {

    @Test
    void get() {
        HashTableImpl<Integer, String> test1 = new HashTableImpl();
        int key = 28;
        test1.put(key, "This is Awesome");
        assertEquals("This is Awesome", test1.get(key));
        HashTableImpl<Integer, Integer> test2 = new HashTableImpl();
        int key1 = 82;
        int key2 = 45;
        test2.put(key1, key2);
        assertEquals(key2, test2.get(key1));
    }
    @Test
    void getAValueWithNull() {
        HashTableImpl<Integer, String> test1 = new HashTableImpl();
        int key = 28;
        int key1 = 30;
        test1.put(key, "This is Awesome");
        assertNull(null, test1.get(key1));
    }

    @Test
    void putAlreadyExisitingKeyUpdate() {
        HashTableImpl<Integer, String> test1 = new HashTableImpl();
        int key = 28;
        test1.put(key, "This is Awesome");
        assertEquals("This is Awesome", test1.put(key, "Update key"));
        test1.put(key, "What shall we return");
        assertEquals("What shall we return", test1.get(key));
    }

    @Test
    void putForFirstTime() {
        HashTableImpl<Integer, String> test1 = new HashTableImpl();
        int key = 28;
        assertEquals(null, test1.put(key, "This is Awesome"));
    }

        @Test
        void hashTableImplCollisionTest() {
            HashTableImpl<Integer, String> hashTable = new HashTableImpl();
            hashTable.put(2, "Hello World");
            hashTable.put(7, "Shalom World");
            hashTable.put(17, "Alo World");
            assertEquals("Hello World", hashTable.get(2));
            assertEquals("Shalom World", hashTable.get(7));
            assertEquals("Alo World", hashTable.get(17));
        }
    }