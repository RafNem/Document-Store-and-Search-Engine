package edu.yu.cs.com1320.project.stage1.impl;
import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.stage1.Document;
import edu.yu.cs.com1320.project.stage1.DocumentStore;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class DocumentStoreImpl implements DocumentStore {
    HashTableImpl <URI, Document> documentStore;
    //Constructor
    public DocumentStoreImpl(){
        this.documentStore = new HashTableImpl<>();
    }

    /**
     * @param input the document being put
     * @param uri unique identifier for the document
     * @param format indicates which type of document format is being passed
     * @return if there is no previous doc at the given URI, return 0. If there is a previous doc, return
     * the hashCode of the previous doc. If InputStream is null, this is a delete, and thus return either
     * the hashCode of the deleted doc or 0 if there is no doc to delete.
     * @throws IOException if there is an issue reading input  === defensive programming
     * @throws IllegalArgumentException if uri or format are null
     */
    @Override
    public int putDocument(InputStream input, URI uri, DocumentFormat format) throws IOException {
        if(uri == null || format == null) {
            throw new IllegalArgumentException();
        }
        if(input == null) {
            if(documentStore.get(uri) == null){
                return 0;
            } else {
                Document deletedDocumentHashCode = documentStore.get(uri);
                deleteDocument(uri);
                return deletedDocumentHashCode.hashCode();
            }
        }
        byte[] contents = input.readAllBytes();
        Document document;
        if(format == DocumentFormat.TXT) {
            String txt = new String(contents);
            document = new DocumentImpl(uri, txt);
        } else {
            document = new DocumentImpl(uri, contents);
        }
        // check here the logic and fixed the put has to be outside the before the if-else statement
        // added a statement to hold the old value and then returned it
        Document deletedOldDocumentHashCode = documentStore.get(uri);
        Document oldDoc = documentStore.put(uri, document);
        if(oldDoc == null){
            return 0;
        } else {
            return deletedOldDocumentHashCode.hashCode();
        }
    }

    /* @param uri the unique identifier of the document to get
     * @return the given document
     */
    @Override
    public Document getDocument(URI uri) {
        return this.documentStore.get(uri);
    }

    /* @param uri the unique identifier of the document to delete
     * @return true if the document is deleted, false if no document exists with that URI
    */
    @Override
    public boolean deleteDocument(URI uri) {
        if(uri == null) {
            throw new IllegalArgumentException();
        }
        if(documentStore.get(uri) == null){
            return false;
        } else {
            documentStore.put(uri, null);
            return true;
        }
    }
}
