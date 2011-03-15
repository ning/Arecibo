package com.ning.arecibo.util.lifecycle;

public interface LifecycleAction<T>
{
    public void doAction(T value);
}
