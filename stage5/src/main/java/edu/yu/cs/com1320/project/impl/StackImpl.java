package edu.yu.cs.com1320.project.impl;

import edu.yu.cs.com1320.project.Stack;

public class StackImpl<T> implements Stack<T> {
    private class Node<T> {
        T value;
        Node<T> next;
        Node(T value) {
            this.value = value;
            next = null;
        }
    }

    private Node<T> topOfStack;
    private int counter;

    public StackImpl() {
    }

    @Override
    public void push(T element) {
        if (element == null) {
            throw new IllegalArgumentException();
        } else {
            Node<T> current = new Node(element);
            if (isStackEmpty()) {
                this.topOfStack = current;
                this.counter++;
            } else {
                current.next = this.topOfStack;
                this.topOfStack = current;
                this.counter++;
            }

        }
    }

    @Override
    public T pop() {
        T valueToReturn = null;
        if (!isStackEmpty()) {
            valueToReturn = (T) this.topOfStack.value;
            this.topOfStack = this.topOfStack.next;
            this.counter--;
        }
        return valueToReturn;
    }


    @Override
    public T peek() {
        if(!isStackEmpty()) {
            return topOfStack.value;
        } else {
            return null;
        }
    }

    @Override
    public int size() {
        return this.counter;
    }

    private boolean isStackEmpty() {
        return this.topOfStack == null;
    }
}