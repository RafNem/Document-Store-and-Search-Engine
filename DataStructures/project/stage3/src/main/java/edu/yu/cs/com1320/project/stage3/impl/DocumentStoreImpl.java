package edu.yu.cs.com1320.project.stage3.impl;
import edu.yu.cs.com1320.project.CommandSet;
import edu.yu.cs.com1320.project.GenericCommand;
import edu.yu.cs.com1320.project.Trie;
import edu.yu.cs.com1320.project.Undoable;
import edu.yu.cs.com1320.project.impl.HashTableImpl;
import edu.yu.cs.com1320.project.impl.StackImpl;
import edu.yu.cs.com1320.project.impl.TrieImpl;
import edu.yu.cs.com1320.project.stage3.Document;
import edu.yu.cs.com1320.project.stage3.DocumentStore;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DocumentStoreImpl implements DocumentStore {
	HashTableImpl<URI, Document> documentStore;
	StackImpl<Undoable> stack;
	Trie<Document> trie;
	//Constructor
	public DocumentStoreImpl() {
		this.documentStore = new HashTableImpl<>();
		this.stack = new StackImpl();
		this.trie = new TrieImpl<>();

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
			Document nullPut = documentStore.put(uri, null);
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
		return this.trie.getAllSorted(keyWord, (document1, document2) -> {
			if(document1.wordCount(keyWord) < document2.wordCount(keyWord)) {
				return 1;
			} else if (document1.wordCount(keyWord) > document2.wordCount(keyWord)) {
				return -1;
			} else {
				return 0;
			}
		});
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
		return trie.getAllWithPrefixSorted(keyWordPre, (document1, document2) -> {
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
			Document nullPut = documentStore.put(u, null);
			for (String word : nullPut.getWords()) {
				trie.delete(word, nullPut);
			}
			cSet.addCommand(new GenericCommand<URI>(u, (undo) -> {
				this.putOrDeleteDocumentIntoTrie(nullPut, undo);
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
			Document nullPut = documentStore.put(u, null);
			for (String word : nullPut.getWords()) {
				trie.delete(word, nullPut);
			}
			cSet.addCommand(new GenericCommand<URI>(u, (undo) -> {
				this.putOrDeleteDocumentIntoTrie(nullPut, undo);
				return true;
			}));
			set.add(u);
		}
		stack.push(cSet);
		return set;
	}

	private Document putOrDeleteDocumentIntoTrie(Document d, URI u) {
		Document oldDoc = documentStore.put(u, d);
		if (oldDoc != null) {
			for (String word : oldDoc.getWords()) {
				trie.delete(word, oldDoc);
			}
		}
		if (d != null) {
			for (String word : d.getWords()) {
				trie.put(word, d);
			}
		}
		return oldDoc;
	}

}

