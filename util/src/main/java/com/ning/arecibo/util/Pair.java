package com.ning.arecibo.util;

import java.io.Serializable;

public class Pair<FirstType, SecondType> implements Serializable
{
    private static final long serialVersionUID = -4403264592023348398L;
    private final FirstType firstValue;
    private final SecondType secondValue;

    public Pair(FirstType v1, SecondType v2)
    {
        firstValue = v1;
        secondValue = v2;
    }

    public FirstType getFirst()
    {
        return firstValue;
    }

    public SecondType getSecond()
    {
        return secondValue;
    }

    public String toString()
    {
        return "Pair("+firstValue+", "+secondValue+")";
    }

    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Pair<?, ?> pair = (Pair<?, ?>) o;

        return (firstValue != null ? firstValue.equals(pair.firstValue) : pair.firstValue == null)
            && (secondValue != null ? secondValue.equals(pair.secondValue) : pair.secondValue == null);

    }

    public int hashCode()
    {
        int result;
        result = (firstValue != null ? firstValue.hashCode() : 0);
        result = 29 * result + (secondValue != null ? secondValue.hashCode() : 0);
        return result;
    }

    public static <K, V> Pair<K, V> of(K k, V v)
    {
        return new Pair<K, V>(k, v);
    }
}
