package edu.yu.cs.com1320.project.stage4.impl;
import edu.yu.cs.com1320.project.CommandSet;
import edu.yu.cs.com1320.project.GenericCommand;
import edu.yu.cs.com1320.project.Trie;
import edu.yu.cs.com1320.project.Undoable;
import edu.yu.cs.com1320.project.MinHeap;
import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.impl.TrieImpl;
import edu.yu.cs.com1320.project.impl.MinHeapImpl;
import edu.yu.cs.com1320.project.stage4.Document;
import edu.yu.cs.com1320.project.stage4.DocumentStore;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import static java.lang.System.nanoTime;

public class DocumentStoreImpl implements DocumentStore {
    private HashTableImpl<URI, Document> documentStore;
    private StackImpl<Undoable> stack;
    private Trie<Document> trie;
    private MinHeap<Document> minHeap;
    private int documentStoreCount;
    private int byteStoreCount;
    private int maxDocumentStoreCount;
    private int maxByteStoreCount;
    //Constructor
    public DocumentStoreImpl() {
        this.documentStore = new HashTableImpl<>();
        this.stack = new StackImpl();
        this.trie = new TrieImpl<>();
        this.minHeap = new MinHeapImpl<>();
        this.documentStoreCount = 0;
        this.byteStoreCount = 0;
        this.maxDocumentStoreCount = 0;
        this.maxByteStoreCount = 0;

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
        if (uri == null || format == null) {
            throw new IllegalArgumentException();
        }
        if (input == null) {
            if (documentStore.get(uri) == null) {
                return 0;
            } else {
                Document deletedDocumentHashCode = documentStore.get(uri);
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
        Document deletedOldDocumentHashCode = documentStore.get(uri);
        Document oldDoc = putOrDeleteDocumentIntoTrie(document, uri);
        GenericCommand command = new GenericCommand<URI>(uri, (undo) -> {
            this.putOrDeleteDocumentIntoTrie(oldDoc, undo);
            return true;
        });
        this.stack.push(command);
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
        if(documentStore.get(uri) != null){
            documentStore.get(uri).setLastUseTime(nanoTime());
            minHeap.reHeapify(documentStore.get(uri));
        }
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
        if(documentStore.get(uri) == null) {
            status = false;
        } else {
            Document nullPut = putOrDeleteDocumentIntoTrie(null, uri);
            GenericCommand command = new GenericCommand<URI>(uri, (undo) -> {
                this.putOrDeleteDocumentIntoTrie(nullPut, undo);
                return true;
            });
            this.stack.push(command);
            status = true;
            if (nullPut != null) {
                for (String word : nullPut.getWords()) {
                    trie.delete(word, nullPut);
                }
            }
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
        List<Document> documentList = this.trie.getAllSorted(keyWord, (document1, document2) -> {
            if(document1.wordCount(keyWord) < document2.wordCount(keyWord)) {
                return 1;
            } else if (document1.wordCount(keyWord) > document2.wordCount(keyWord)) {
                return -1;
            } else {
                return 0;
            }
        });
        setAndReheap(documentList);
        return documentList;
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
        List<Document> documentList = trie.getAllWithPrefixSorted(keyWordPre, (document1, document2) -> {
            int i = 0;
            for(String w: document1.getWords()) {
                if (w.startsWith(keyWordPre)) ;
                i += 1;
            }
            int j = 0;
            for(String w: document2.getWords()) {
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
        setAndReheap(documentList);
        return documentList;
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
            for (String word : nullPut.getWords()) {
                trie.delete(word, nullPut);
            }
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
    @Override
    public Set<URI> deleteAllWithPrefix(String keywordPrefix) {
        String keyWordPre = keywordPrefix.replace("[^a-zA-z0-9]", "");
        keyWordPre.toUpperCase();
        Set<URI> set = new HashSet<>();
        CommandSet<URI> cSet = new CommandSet<>();
        List<Document> words = searchByPrefix(keyWordPre);
        for(Document doc : words) {
            URI u = doc.getKey();
            Document nullPut = putOrDeleteDocumentIntoTrie(null, u);
            for (String word : nullPut.getWords()) {
                trie.delete(word, nullPut);
            }
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

    @Override
    public void setMaxDocumentCount(int limit) {
        // must be positive number
        if (limit < 0) {
            throw new IllegalArgumentException();
        } else {
            this.maxDocumentStoreCount = limit;
            while (documentStoreCount > maxDocumentStoreCount) {
                putAndRemove();
            }
        }
    }
    @Override
    public void setMaxDocumentBytes(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException();
        } else {
            this.maxByteStoreCount = limit;
            while (byteStoreCount > byteStoreCount) {
                putAndRemove();
            }
        }
    }


    private Document putOrDeleteDocumentIntoTrie(Document d, URI u) {
        if (d != null) {
            while ((maxDocumentStoreCount > 0) && (documentStoreCount+1 > maxDocumentStoreCount)) {
                putAndRemove();
            }
            while ((maxByteStoreCount > 0) && (byteStoreCount+d.getDocumentTxt().getBytes().length > maxByteStoreCount)){
                putAndRemove();
            }
        }
        Document previousDoc = documentStore.put(u, d);
        if (previousDoc != null) {
            for (String word : previousDoc.getWords()) {
                trie.delete(word, previousDoc);
            }
            try {
                deleteMinHeap(previousDoc);
            } catch (NoSuchElementException e) {
                System.out.println("Error");
            }
            documentStoreCount--;
            int getData;
            if(previousDoc.getDocumentTxt() != null){
                getData = previousDoc.getDocumentTxt().getBytes().length;
            } else {
                getData = previousDoc.getDocumentBinaryData().length;
            }
            byteStoreCount -= getData;
        }
        if (d != null) {
            for (String word : d.getWords()) {
                trie.put(word, d);
            }
            d.setLastUseTime(System.nanoTime());
            minHeap.insert(d);
            documentStoreCount++;
            int getData;
            if(d.getDocumentTxt() != null){
                getData = d.getDocumentTxt().getBytes().length;
            } else {
                getData = d.getDocumentBinaryData().length;
            }
            byteStoreCount += getData;
        }
        return previousDoc;
    }

    private boolean deleteMinHeap(Document document) {
        document.setLastUseTime(0xFFFFFFFF);
        minHeap.reHeapify(document);
        return document == minHeap.remove();
    }

    private void removeCommand(URI u) {
        StackImpl<Undoable> temporaryStack = new StackImpl<>();
        while (stack.size() > 0) {
            Undoable undo = this.stack.pop();
            temporaryStack.push(undo);
        }
        while (temporaryStack.peek() != null) {
            Undoable undoableTemp = temporaryStack.pop();
            if (undoableTemp instanceof CommandSet){
                if(((CommandSet<URI>) undoableTemp).containsTarget(u)) {
                    while (((CommandSet<URI>) undoableTemp).iterator().hasNext()) {
                        if (((CommandSet<URI>) undoableTemp).iterator().next().getTarget().equals(u)) {
                            ((CommandSet<URI>) undoableTemp).iterator().remove();
                        }
                    }
                    if ((((CommandSet<URI>) undoableTemp)).isEmpty()) {
                        continue;
                    }
                }
            } else if (undoableTemp instanceof GenericCommand) {
                if(((GenericCommand<URI>) undoableTemp).getTarget().equals(u)) {
                    continue;
                }
            }
            this.stack.push(undoableTemp);
        }
    }


    private void setAndReheap(List<Document> documentList) {
        long setTime = nanoTime();
        for (Document doc : documentList) {
            doc.setLastUseTime(setTime);
            minHeap.reHeapify(doc);
        }

    }

    private void putAndRemove() {
        Document deleteDoc = minHeap.remove();
        URI deleteDocKey = deleteDoc.getKey();
        putOrDeleteDocumentIntoTrie(null, deleteDocKey);
        removeCommand(deleteDocKey);
    }
}