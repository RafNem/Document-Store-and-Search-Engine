package edu.yu.cs.com1320.project.impl;


import edu.yu.cs.com1320.project.HashTable;

/**
 * Instances of HashTable should be constructed with two type parameters, one for the type of the keys in the table and one for the type of the values
 * @param <Key>
 * @param <Value>
 */
public class HashTableImpl<Key, Value> implements HashTable<Key, Value> {
	class HashEntry<Key, Value> { //entry to my hashmap and linked list
		Key key;
		Value value;
		HashEntry next;

		HashEntry(Key k, Value v, HashEntry n) {
			if (k == null) {
				throw new IllegalArgumentException();
			}
			this.key = k;
			this.value = v;
			this.next = n;
		}

		private Key getKey() {
			return key;
		}

		private HashEntry getNext() {
			return next;
		}

		private Value getValue() {

			return value;
		}

	}

	private HashEntry<Key, Value>[] hashTable;
	private int arraySize;
	private int sizeOfEntries;

	public HashTableImpl() {
		this.arraySize = 5; //init to array size of 5
		this.hashTable = new HashEntry[arraySize];
		this.sizeOfEntries = 0;
	}

	private int hashFunction(Key key) {
		return (key.hashCode() & 0x7fffffff) % arraySize;
	}

	/**
	 * @param k the key whose value should be returned
	 * @return the value that is stored in the HashTable for k, or null if there is no such key in the table
	 */
	@Override
	public Value get(Key k) {
		int index = this.hashFunction(k);
		if (hashTable[index] == null) {
			return null;
		}
		//create the head
		HashEntry currentEntry = hashTable[index];
		// cant use == for object equality !!!
		while (currentEntry != null && !(currentEntry.getKey().equals(k))) {
			currentEntry = currentEntry.getNext();
		}
		if (currentEntry == null) {
			return null;
		} else {
			return (Value) currentEntry.getValue();
		}
	}

	/**
	 * @param k the key at which to store the value
	 * @param v the value to store
	 *          To delete an entry, put a null value.
	 * @return if the key was already present in the HashTable, return the previous value stored for the key. If the key was not already present, return null.
	 */
	@Override
	public Value put(Key k, Value v) {
		//get the index in our array by taking the key and hashing it with the hash function
		int index = this.hashFunction(k);
		// We need to return a value
		Value returnValue = deleteKey(k);
		if (v == null) {
			return returnValue;
		}
		this.hashTable[index] = new HashEntry<Key, Value>(k, v, hashTable[index]);
		sizeOfEntries++;
		//array doubling
		if (((1.0 * sizeOfEntries) / arraySize) >= .75) {
			doubleArray();
		}
		return returnValue;
	}

	private void doubleArray() {
		this.sizeOfEntries = 0;
		HashEntry<Key, Value>[] temporary = this.hashTable;
		this.hashTable = new HashEntry[arraySize * 2];
		arraySize = hashTable.length;
		for (HashEntry<Key, Value> node : temporary) {
			while (node != null) {
				put(node.getKey(), node.getValue());
				node = node.getNext();
			}
		}
	}

	private Value deleteKey(Key k) {
		int index = this.hashFunction(k);
		HashEntry currentEntry = hashTable[index];
		HashEntry previousEntry = hashTable[index];
		if (currentEntry == null) {
			return null;
		}
		if (currentEntry.getKey().equals(k)) {
			hashTable[index] = currentEntry.getNext();
			sizeOfEntries--;
			return (Value) previousEntry.getValue();
		}
		while (currentEntry.getNext() != null && !(currentEntry.getNext().getKey().equals(k))) {
			currentEntry = currentEntry.getNext();
		}
		if (currentEntry.getNext() == null) {
			return null;
		}
		previousEntry = currentEntry.getNext();
		currentEntry.next = currentEntry.next.next;
		sizeOfEntries--;
		return (Value) previousEntry.getValue();
	}
}

