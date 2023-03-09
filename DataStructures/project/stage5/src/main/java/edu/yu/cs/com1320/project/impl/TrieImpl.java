package edu.yu.cs.com1320.project.impl;
import edu.yu.cs.com1320.project.Trie;
import java.util.*;
public class TrieImpl<Value> implements Trie<Value> {
	private final int alphabetSize = 256;
	private Node<Value> root; // root of trie
	class Node<Value> {
		private List<Value> val = new ArrayList<>();
		private Node[] links = new Node[alphabetSize];
	}
	//the constructor
	public TrieImpl() {}

	/**
	 * add the given value at the given key
	 * @param key
	 * @param val
	 */
	@Override
	public void put(String key, Value val) {
		if(key == null) {
			throw new IllegalArgumentException();
		}
		if(val == null || key.isEmpty()) {
			return;
		} else {
			key = stringCapsOnly(key);
			this.root = put(this.root, key, val, 0);
		}
	}
	//private method for put
	private Node<Value> put(Node<Value> x, String key, Value val, int d) {
		if (x == null) {
			x = new Node<Value>();
		}
		if (d == key.length()) {
			if(!(x.val.contains(val))) {
				x.val.add(val);
			}
			return x;
		}
		char c = key.charAt(d);
		x.links[c] = this.put(x.links[c], key, val, d + 1);
		return x;
	}
	// private mehod to convert string into only capital letters
	private String stringCapsOnly(String string) {
		return string.replaceAll("[^a-zA-Z0-9\\s]", "").toUpperCase();
	}
	@Override
	public List<Value> getAllSorted(String key, Comparator<Value> comparator) {
		if (key == null || comparator == null) {
			throw new IllegalArgumentException();
		}
		List<Value> listToReturn = new ArrayList<>();
		key = stringCapsOnly(key);
		if (key.isEmpty()) {
			return listToReturn;
		}
		Node x = this.get(this.root, key, 0);
		if (x == null) {
			return listToReturn;
		}
		Set<Value> setOfList = new HashSet<Value>();
		setOfList = collectWithSetAll(x, setOfList);
		if (setOfList == null) {
			return listToReturn;
		}
		listToReturn = new ArrayList<Value>(setOfList);
		listToReturn.sort(comparator);
		return listToReturn;
	}
	//private get method
	private Node get(Node x, String key, int d) {
		//link was null - return null, indicating a miss
		if (x == null) {
			return null;
		}
		//we've reached the last node in the key,
		//return the node
		if (d == key.length()) {
			return x;
		}
		if(key.isEmpty()) {
			return x;
		}
		//proceed to the next node in the chain of nodes that
		//forms the desired key
		char c = key.charAt(d);
		return this.get(x.links[c], key, d + 1);
	}
	@Override
	public List<Value> getAllWithPrefixSorted(String prefix, Comparator<Value> comparator) {
		if (prefix == null || comparator == null) {
			throw new IllegalArgumentException();
		}
		List<Value> results = new ArrayList<Value>();
		prefix = stringCapsOnly(prefix);
		if(prefix.isEmpty()) {
			return results;
		}
		Node<Value> x = this.get(this.root, prefix, 0);
		if (x == null) {
			return results;
		}
		HashSet<Value> s = new HashSet<Value>();
		Set<Value> set = collectWithSet(x, s);
		if (set != null) {
			results = new ArrayList<Value>(set);
			results.sort(comparator);
			return results;
		} else {
			return results;
		}
	}
	// private method to collect keys with set collection
	private Set<Value> collectWithSet(Node<Value> x, Set<Value> s) {
		if (x.val != null && !(x.val.isEmpty())) {
			s.addAll(x.val);
		}
		for (char c = 0; c < alphabetSize; c++) {
			if(x.links[c] != null) {
				this.collectWithSet(x.links[c], s);
			}
		}
		return s;
	}

	// private method to collect ALL keys with set collection
	private Set<Value> collectWithSetAll(Node<Value> x, Set<Value> s) {
		if (x.val != null && !(x.val.isEmpty())) {
			s.addAll(x.val);
		}
		return s;
	}

	@Override
	public Set<Value> deleteAllWithPrefix(String prefix) {
		if (prefix == null) {
			throw new IllegalArgumentException();
		}
		Set<Value> setToReturn = new HashSet<Value>();
		if (prefix.isEmpty()) {
			return setToReturn;
		} else {
			prefix = stringCapsOnly(prefix);
			Node<Value> x = this.get(this.root, prefix, 0);
			setToReturn = collectWithSet(x, setToReturn);
			x.links = new Node[alphabetSize];
			deleteAll(prefix);
			return setToReturn;
		}

	}
	@Override
	public Set<Value> deleteAll(String key) {
		if (key == null) {
			throw new IllegalArgumentException();
		}
		key = stringCapsOnly(key);
		HashSet<Value> setIsEmpty = new HashSet<Value>();
		if (key.isEmpty()) {
			return setIsEmpty;
		}
		Node<Value> x = this.get(this.root, key, 0);
		if (x == null) {
			return setIsEmpty;
		}
		HashSet<Value> newSet = new HashSet<Value>();
		Set<Value> set = collectWithSet(x, newSet);
		this.root = deleteAll(this.root, key, 0);
		return set;
	}
	private Node deleteAll(Node x, String key, int d) {
		if (x == null) {
			return null;
		}
		if (d == key.length()) {
			x.val = null;
		} else {
			char c = key.charAt(d);
			x.links[c] = this.deleteAll(x.links[c], key, d + 1);
		}
		if (x.val != null) {
			return x;
		}
		for (int c = 0; c < alphabetSize; c++) {
			if (x.links[c] != null) {
				return x;
			}
		}
		return null;
	}
	@Override
	public Value delete(String key, Value val) {
		if (key == null || val == null) {
			throw new IllegalArgumentException();
		}
		if (key.isEmpty()) {
			return null;
		}
		key = stringCapsOnly(key);
		Value returnVal = null;
		Node<Value> x = this.get(this.root, key, 0);
		for(Value vals : x.val) {
			if (val.equals(vals)) {
				returnVal = vals;
			}
		}
		delete(this.root, key, val, 0);
		return returnVal;
	}
	private Node delete(Node x, String key, Value val, int d) {
		if (x == null) {
			return null;
		}
		if (d == key.length()) {
			x.val.remove(val);
		} else {
			char c = key.charAt(d);
			x.links[c] = this.delete(x.links[c], key, val, d + 1);
		}
		if (x.val != null) {
			return x;
		}
		for (int c = 0; c < alphabetSize; c++) {
			if (x.links[c] != null) {
				return x;
			}
		}
		return null;
	}
}
