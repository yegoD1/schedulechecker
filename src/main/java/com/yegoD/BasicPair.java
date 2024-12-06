package com.yegoD;

/**
 * BasicPair assosciates one key to one value. Great for assosciating two unrelated class types.
 */
public class BasicPair<K,V> {
    private K key;
    private V value;

    /**
     * Constructs a pair using a key and an assosciated value.
     * @param key Key to set.
     * @param value Value to set.
     */
    public BasicPair(K key, V value){
        this.key = key;
        this.value = value;
    }

    /**
     * Gets the value of key in this pair.
     * @return Value of key.
     */
    public K getKey()
    {
        return key;
    }

    /**
     * Gets the value in this pair.
     * @return
     */
    public V getValue()
    {
        return value;
    }
}
