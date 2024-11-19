package com.gmuprojects;

public class BasicPair<K,V> {
    private K key;
    private V value;

    public BasicPair(K key, V value){
        this.key = key;
        this.value = value;
    }

    public K getKey()
    {
        return key;
    }

    public V getValue()
    {
        return value;
    }
}
