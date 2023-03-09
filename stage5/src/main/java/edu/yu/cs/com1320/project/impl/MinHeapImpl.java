package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.MinHeap;

import java.util.NoSuchElementException;

public class MinHeapImpl<E extends Comparable<E>> extends MinHeap <E> {

    public MinHeapImpl() {
        elements = (E[]) new Comparable[10];
    }

    @Override
    public void reHeapify(Comparable element) {
        int index = getArrayIndex(element);
        upHeap(index);
        downHeap(index);
    }

    @Override
    protected int getArrayIndex(Comparable element) {
        for (int i = 0; i < elements.length; i++) {
            if (element.equals(elements[i])) {
                return i;
            }
        }
        throw new NoSuchElementException();
    }

    @Override
    protected void doubleArraySize() {
        Comparable[] prevArray = this.elements;
        elements = (E[]) new Comparable[prevArray.length * 2];
        for (int i = 0; i < prevArray.length; i++) {
            elements[i] = (E) prevArray[i];
        }
    }

}
