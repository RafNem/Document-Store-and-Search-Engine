package edu.yu.cs.com1320.project.stage2.impl;

import edu.yu.cs.com1320.project.Command;
import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.stage2.Document;
import edu.yu.cs.com1320.project.stage2.DocumentStore;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class DocumentStoreImpl implements DocumentStore {
    HashTableImpl<URI, Document> documentStore;
    StackImpl<Command> stack;
    //Constructor
    public DocumentStoreImpl(){
        this.documentStore = new HashTableImpl<>();
        this.stack = new StackImpl();
    }
    /**
     * @param input the document being put
     * @param uri unique identifier for the document
     * @param format indicates which type of document format is being passed
     * @return if there is no previous doc at the given URI, return 0. If there is a previous doc, return the hashCode of the previous doc. If InputStream is null, this is a delete, and thus return either the hashCode of the deleted doc or 0 if there is no doc to delete.
     * @throws IOException if there is an issue reading input
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
        Command command = new Command(uri, (undo) -> {
            this.documentStore.put(undo, oldDoc);
            return true;
        });
        this.stack.push(command);
        if(oldDoc == null){
            return 0;
        } else {
            return deletedOldDocumentHashCode.hashCode();
        }
    }

    /**
     * @param uri the unique identifier of the document to get
     * @return the given document
     */
    @Override
    public Document getDocument(URI uri) {
        return this.documentStore.get(uri);
    }

    /**
     * @param uri the unique identifier of the document to delete
     * @return true if the document is deleted, false if no document exists with that URI
     */
    @Override
    public boolean deleteDocument(URI uri) {
        if(uri == null) {
            throw new IllegalArgumentException();
        }
        boolean status = false;
        if(documentStore.get(uri) == null){
            status = false;
        } else {
            Document nullPut = documentStore.put(uri, null);
            Command command = new Command(uri, (undo) -> {
                this.documentStore.put(undo, nullPut);
                return true;
            });
            this.stack.push(command);
            status = true;
        }
        return status;
    }
    /**
     * undo the last put or delete command
     * @throws IllegalStateException if there are no actions to be undone, i.e. the command stack is empty
     */
    @Override
    public void undo() throws IllegalStateException {
        if (this.stack.size() == 0) {
            throw new IllegalStateException();
        } else {
            this.stack.pop().undo();
        }
    }

    /**
     * undo the last put or delete that was done with the given URI as its key
     * @param uri
     * @throws IllegalStateException if there are no actions on the command stack for the given URI
     */
    @Override
    public void undo(URI uri) throws IllegalStateException {
        if (uri == null) {
            throw new IllegalArgumentException();
        } else if (this.stack == null) {
            throw new IllegalStateException();
        } else {
        StackImpl temporaryStack = new StackImpl();
            while(this.stack.peek() != null && !(this.stack.peek()).getUri().equals(uri)) {
                temporaryStack.push(this.stack.pop());
            }
            Boolean noAction = false;
            if (this.stack.peek() != null) {
                this.stack.pop().undo();
            } else {
                noAction = true;
            }
            while(temporaryStack.size() > 0) {
                this.stack.push((Command)temporaryStack.pop());
            }
            if (noAction) {
                throw new IllegalStateException();
            }
        }
    }
}

