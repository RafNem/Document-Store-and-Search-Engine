package edu.yu.cs.com1320.project.stage1.impl;

import edu.yu.cs.com1320.project.stage1.Document;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class DocumentImplTest {
    @Test
    void getDocumentTxt() {
        URI uri = URI.create("www.microsoft.com");
        Document doc1 = new DocumentImpl(uri, "Hello World");
        assertEquals("Hello World", doc1.getDocumentTxt());
        URI uri1 = URI.create("www.cs.com");
        Document doc2 = new DocumentImpl(uri1, "The little brown fox jumped over the river");
        assertEquals("The little brown fox jumped over the river", doc2.getDocumentTxt());
    }

    @Test
    void getDocumentBinaryData() {
        URI uri = URI.create("www.microsoft.com");
        String str = "Hello World";
        byte[] byte1 = str.getBytes();
        System.out.println(byte1);
        Document binary = new DocumentImpl(uri, byte1);
        assertEquals(byte1, binary.getDocumentBinaryData());
        URI uri1 = URI.create("www.Rafael.com");
        String str1 = "Shalom World";
        byte[] byte2 = str1.getBytes(StandardCharsets.UTF_8);
        Document binary1 = new DocumentImpl(uri1, byte2);
        assertEquals(byte2, binary1.getDocumentBinaryData());
    }

    @Test
    void getKey() {

    }

    @Test
    void testHashCode() {
    }

    @Test
    void testEquals() {

    }
}