package edu.yu.cs.com1320.project.stage5.impl;
import edu.yu.cs.com1320.project.*;
import edu.yu.cs.com1320.project.impl.BTreeImpl;
import java.io.File;
import edu.yu.cs.com1320.project.CommandSet;
import edu.yu.cs.com1320.project.GenericCommand;
import edu.yu.cs.com1320.project.Trie;
import edu.yu.cs.com1320.project.Undoable;
import edu.yu.cs.com1320.project.MinHeap;
//import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.impl.TrieImpl;
import edu.yu.cs.com1320.project.impl.MinHeapImpl;
import edu.yu.cs.com1320.project.stage5.Document;
import edu.yu.cs.com1320.project.stage5.DocumentStore;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;

import static java.lang.System.nanoTime;

public class DocumentStoreImpl implements DocumentStore {

    private BTree<URI, Document> bTreeDocumentStore;
    private StackImpl<Undoable> stack;
    private Trie<URI> trie;
    private MinHeap<HeapDoc> minHeap;
    private Set<URI> docSaved;
    private int documentStoreCount;
    private int byteStoreCount;
    private int maxDocumentStoreCount;
    private int maxByteStoreCount;



    public class HeapDoc implements Comparable<HeapDoc> {
        private URI uriD;

        public HeapDoc(URI uri) {
            uriD = uri;
        }

        public int compareTo(HeapDoc o) {
            return bTreeDocumentStore.get(uriD).compareTo(bTreeDocumentStore.get(o.uriD));
        }

        public boolean equals(Object o) {
            if (o instanceof HeapDoc) {
                return uriD.equals(((HeapDoc)o).uriD);
            }
            return false;
        }
    }
    //Constructor
    public DocumentStoreImpl() {
        this.bTreeDocumentStore = new BTreeImpl<>();
        this.stack = new StackImpl();
        this.trie = new TrieImpl<>();
        this.minHeap = new MinHeapImpl<>();
        this.documentStoreCount = 0;
        this.byteStoreCount = 0;
        this.maxDocumentStoreCount = 0;
        this.maxByteStoreCount = 0;
        this.docSaved = new HashSet<>();
        this.bTreeDocumentStore.setPersistenceManager(new DocumentPersistenceManager(null));
    }
    public DocumentStoreImpl(File baseDir) {
        this.bTreeDocumentStore = new BTreeImpl<>();
        this.stack = new StackImpl();
        this.trie = new TrieImpl<>();
        this.minHeap = new MinHeapImpl<>();
        this.documentStoreCount = 0;
        this.byteStoreCount = 0;
        this.maxDocumentStoreCount = 0;
        this.maxByteStoreCount = 0;
        this.docSaved = new HashSet<>();
        this.bTreeDocumentStore.setPersistenceManager(new DocumentPersistenceManager(baseDir));
    }
    /**
     * @param input the document being put
     * @param uri unique identifier for the document
     * @param format indicates which type of document format is being passed
     * @return if there is no previous doc at the given URI, return 0. If there is a previous doc, return the hashCode of the previous doc. If InputStream is null, this is a delete, and thus return either the hashCode of the deleted doc or 0 if there is no doc to delete.
     * @throws IOException if there is an issue reading input
     * @throws IllegalArgumentException if uri or format are null
     */
    public int putDocument(InputStream input, URI uri, DocumentFormat format) throws IOException {
        if (uri == null || format == null) {
            throw new IllegalArgumentException();
        }
        if (input == null) {
            if (bTreeDocumentStore.get(uri) == null) {
                return 0;
            } else {
                Document deletedDocumentHashCode = bTreeDocumentStore.get(uri);
                deleteDocument(uri);
                return deletedDocumentHashCode.hashCode();
            }
        }
        byte[] contents = input.readAllBytes();
        Document document;
        if (format == DocumentFormat.TXT) {
            String txt = new String(contents);
            document = new DocumentImpl(uri, txt);
        } else {
            document = new DocumentImpl(uri, contents);
        }
        Document deletedOldDocumentHashCode = bTreeDocumentStore.get(uri);
        Document oldDoc = putOrDeleteDocumentIntoTrie(document, uri);
        GenericCommand command = new GenericCommand<URI>(uri, (undo) -> {
            this.putOrDeleteDocumentIntoTrie(oldDoc, undo);
            return true;
        });
        this.stack.push(command);
        setAndDelete();
        if (oldDoc == null) {
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
        if(uri == null){
            throw new IllegalArgumentException();
        }
        if(bTreeDocumentStore.get(uri) != null){
            if ((docSaved.remove(bTreeDocumentStore.get(uri).getKey()))) {
                documentStoreCount++;
                int getData;
                if (bTreeDocumentStore.get(uri).getDocumentTxt() != null) {
                    getData = bTreeDocumentStore.get(uri).getDocumentTxt().getBytes().length;
                } else {
                    getData = bTreeDocumentStore.get(uri).getDocumentBinaryData().length;
                }
                byteStoreCount += getData;
                try {
                    bTreeDocumentStore.get(uri).setLastUseTime(Long.MIN_VALUE);
                    minHeap.reHeapify(new HeapDoc(bTreeDocumentStore.get(uri).getKey()));
                    minHeap.remove();
                } catch (NoSuchElementException e) {
                    System.out.println("Error");
                }
            }
            bTreeDocumentStore.get(uri).setLastUseTime(nanoTime());
            minHeap.reHeapify(new HeapDoc(bTreeDocumentStore.get(uri).getKey()));
        }
        setAndDelete();
        return this.bTreeDocumentStore.get(uri);
    }
    /**
     * @param uri the unique identifier of the document to delete
     * @return true if the document is deleted, false if no document exists with that URI
     */
    @Override
    public boolean deleteDocument(URI uri) {
        if(uri == null){
            throw new IllegalArgumentException();
        }
        Document oldValue = putOrDeleteDocumentIntoTrie(null, uri);
        stack.push(new GenericCommand<URI>(uri, x -> {
            this.putOrDeleteDocumentIntoTrie(oldValue, uri);
            return true;
        }));
        return oldValue != null;
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
        setAndDelete();
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
            StackImpl<Undoable> temporaryStack = new StackImpl<>();
            Undoable undoable = this.stack.peek();
            int x = 0;
            while (undoable != null){
                if (undoable instanceof GenericCommand) {
                    if (((GenericCommand<URI>) undoable).getTarget().equals(uri)) {
                        break;
                    }
                } else {
                    if(((CommandSet<URI>) undoable).containsTarget(uri)){
                        break;
                    }
                }
                undoable = stack.pop();
                temporaryStack.push(undoable);
                undoable = stack.peek();
                x++;
            }
            Boolean noAction = false;
            if (this.stack.peek() != null) {
                if(stack.peek() instanceof GenericCommand) {
                    this.stack.pop().undo();
                } else {
                    CommandSet<URI> commandSet = (CommandSet<URI>) stack.pop();
                    commandSet.undo(uri);
                    if (!(commandSet.isEmpty())) {
                        this.stack.push(commandSet);
                    }
                }
            } else {
                noAction = true;
            }
            while(temporaryStack.size() > 0) {
                this.stack.push(temporaryStack.pop());
            }
            if (noAction) {
                throw new IllegalStateException();
            }
        }
        setAndDelete();
    }
    /**
     * Retrieve all documents whose text contains the given keyword.
     * Documents are returned in sorted, descending order, sorted by the number of times the keyword appears in the document.
     * Search is CASE INSENSITIVE.
     * @param keyword
     * @return a List of the matches. If there are no matches, return an empty list.
     */
    @Override
    public List<Document> search(String keyword) {
        String keyWord = keyword.replace("[^a-zA-z0-9]", "");
        keyWord.toUpperCase();
        List<URI> documentList = this.trie.getAllSorted(keyWord, (document1, document2) -> {
            if(bTreeDocumentStore.get(document1).wordCount(keyWord) < bTreeDocumentStore.get(document2).wordCount(keyWord)) {
                return 1;
            } else if (bTreeDocumentStore.get(document1).wordCount(keyWord) > bTreeDocumentStore.get(document2).wordCount(keyWord)) {
                return -1;
            } else {
                return 0;
            }
        });
        List<Document> documentListReturn = new ArrayList<>();
        long setTime = nanoTime();
        for (URI doc : documentList) {
            if ((docSaved.remove(bTreeDocumentStore.get(doc).getKey()))) {
                documentStoreCount++;
                int getData;
                if (bTreeDocumentStore.get(doc).getDocumentTxt() != null) {
                    getData = bTreeDocumentStore.get(doc).getDocumentTxt().getBytes().length;
                } else {
                    getData = bTreeDocumentStore.get(doc).getDocumentBinaryData().length;
                }
                byteStoreCount += getData;
                minHeap.insert(new HeapDoc(bTreeDocumentStore.get(doc).getKey()));
            }
            bTreeDocumentStore.get(doc).setLastUseTime(setTime);
            minHeap.reHeapify(new HeapDoc(bTreeDocumentStore.get(doc).getKey()));
            documentListReturn.add(bTreeDocumentStore.get(doc));
        }
        setAndDelete();
        return documentListReturn;
    }
    /**
     * Retrieve all documents whose text starts with the given prefix
     * Documents are returned in sorted, descending order, sorted by the number of times the prefix appears in the document.
     * Search is CASE INSENSITIVE.
     * @param keywordPrefix
     * @return a List of the matches. If there are no matches, return an empty list.
     */
    @Override
    public List<Document> searchByPrefix(String keywordPrefix) {
        String keyWordPre = keywordPrefix.replace("[^a-zA-z0-9]", "");
        keyWordPre.toUpperCase();
        List<URI> documentList = trie.getAllWithPrefixSorted(keyWordPre, (document1, document2) -> {
            int i = 0;
            for(String w: bTreeDocumentStore.get(document1).getWords()) {
                if (w.startsWith(keyWordPre)) ;
                i += 1;
            }
            int j = 0;
            for(String w: bTreeDocumentStore.get(document2).getWords()) {
                if(w.startsWith(keyWordPre));
                j += 1;
            }
            if(i < j) {
                return 1;
            } else if (i > j) {
                return -1;
            } else {
                return 0;
            }
        });
        List<Document> documentListReturn = new ArrayList<>();
        long setTime = nanoTime();
        for (URI doc : documentList) {
            if ((docSaved.remove(bTreeDocumentStore.get(doc).getKey()))) {
                documentStoreCount++;
                int getData;
                if (bTreeDocumentStore.get(doc).getDocumentTxt() != null) {
                    getData = bTreeDocumentStore.get(doc).getDocumentTxt().getBytes().length;
                } else {
                    getData = bTreeDocumentStore.get(doc).getDocumentBinaryData().length;
                }
                byteStoreCount += getData;
                minHeap.insert(new HeapDoc(bTreeDocumentStore.get(doc).getKey()));
            }
            bTreeDocumentStore.get(doc).setLastUseTime(setTime);
            minHeap.reHeapify(new HeapDoc(bTreeDocumentStore.get(doc).getKey()));
            documentListReturn.add(bTreeDocumentStore.get(doc));
        }
        setAndDelete();
        return documentListReturn;
    }
    /**
     * Completely remove any trace of any document which contains the given keyword
     * @param keyword
     * @return a Set of URIs of the documents that were deleted.
     */
    @Override
    public Set<URI> deleteAll(String keyword) {
        String keyWord = keyword.replace("[^a-zA-z0-9]", "");
        keyWord.toUpperCase();
        Set<URI> set = new HashSet<>();
        CommandSet<URI> cSet = new CommandSet<>();
        List<Document> words = search(keyWord);
        for(Document doc : words) {
            URI u = doc.getKey();
            Document nullPut = putOrDeleteDocumentIntoTrie(null, u);
            cSet.addCommand(new GenericCommand<URI>(u, (undo) -> {
                this.putOrDeleteDocumentIntoTrie(nullPut, undo);
                long timeToSet = nanoTime();
                nullPut.setLastUseTime(timeToSet);
                return true;
            }));
            set.add(u);
        }
        stack.push(cSet);
        return set;
    }
    /**
     * Completely remove any trace of any document which contains a word that has the given prefix
     * Search is CASE INSENSITIVE.
     * @param keywordPrefix
     * @return a Set of URIs of the documents that were deleted.
     */
    public Set<URI> deleteAllWithPrefix(String keywordPrefix) {
        String keyWordPre = keywordPrefix.replace("[^a-zA-z0-9]", "");
        keyWordPre.toUpperCase();
        Set<URI> set = new HashSet<>();
        CommandSet<URI> cSet = new CommandSet<>();
        List<Document> words = searchByPrefix(keyWordPre);
        for(Document doc : words) {
            URI u = doc.getKey();
            Document nullPut = putOrDeleteDocumentIntoTrie(null, u);
            cSet.addCommand(new GenericCommand<URI>(u, (undo) -> {
                this.putOrDeleteDocumentIntoTrie(nullPut, undo);
                long timeToSet = nanoTime();
                nullPut.setLastUseTime(timeToSet);
                return true;
            }));
            set.add(u);
        }
        stack.push(cSet);
        return set;
    }

    public void setMaxDocumentCount(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException();
        } else {
            this.maxDocumentStoreCount = limit;
        }
        setAndDelete();
    }

    public void setMaxDocumentBytes(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException();
        } else {
            this.maxByteStoreCount = limit;
        }
        setAndDelete();
    }
    private Document putOrDeleteDocumentIntoTrie(Document doc, URI uri) {
        Document previousDoc = bTreeDocumentStore.get(uri);
        if (previousDoc != null) {
            for (String word : previousDoc.getWords()) {
                trie.delete(word, previousDoc.getKey());
            }
            if ((docSaved.remove(previousDoc.getKey())) == false) {
                documentStoreCount--;
                int getData;
                if (previousDoc.getDocumentTxt() == null) {
                    getData = previousDoc.getDocumentBinaryData().length;
                } else {
                    getData = previousDoc.getDocumentTxt().getBytes().length;
                }
                byteStoreCount -= getData;
                try {
                    previousDoc.setLastUseTime(0xFFFFFFFF);
                    minHeap.reHeapify((HeapDoc) new HeapDoc(previousDoc.getKey()));
                    minHeap.remove();
                } catch (NoSuchElementException e) {
                    System.out.println("Error");
                }
            }
        }
        bTreeDocumentStore.put(uri,doc);
        if (doc != null) {
            for (String word : doc.getWords()) {
                trie.put(word, doc.getKey());
            }
            doc.setLastUseTime(System.nanoTime());
            documentStoreCount++;
            int getData;
            if(doc.getDocumentTxt() != null){
                getData = doc.getDocumentTxt().getBytes().length;
            } else {
                getData = doc.getDocumentBinaryData().length;
            }
            byteStoreCount += getData;
            minHeap.insert(new HeapDoc(doc.getKey()));
        }
        return previousDoc;
    }
    private void putAndRemove(){
        Document previousDoc = bTreeDocumentStore.get(minHeap.remove().uriD);
        documentStoreCount--;
        int getData;
        if(previousDoc.getDocumentTxt() != null){
            getData = previousDoc.getDocumentTxt().getBytes().length;
        } else {
            getData = previousDoc.getDocumentBinaryData().length;
        }
        byteStoreCount -= getData;
        try {
            bTreeDocumentStore.moveToDisk(previousDoc.getKey());
        } catch (Exception e) {
            System.out.println("Error");
        }
        docSaved.add(previousDoc.getKey());
    }
    private void setAndDelete() {
        while ((maxDocumentStoreCount > 0) && (documentStoreCount > maxDocumentStoreCount)) {
            putAndRemove();
        }
        while ((maxByteStoreCount > 0) && (byteStoreCount > maxByteStoreCount)){
            putAndRemove();
        }
    }
}