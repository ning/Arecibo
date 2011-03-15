package com.ning.arecibo.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory
{
    private final AtomicInteger count = new AtomicInteger(0);
    private final String        name;
    private final ThreadFactory delegate;

    public NamedThreadFactory(String name)
    {
        this(name, new ThreadFactory() {
            public Thread newThread(Runnable r)
            {
                return new Thread(r);
            }
        });
    }

    public NamedThreadFactory(String name, ThreadFactory delegate)
    {
        this.delegate = delegate;
        this.name = name;
    }

    public Thread newThread(final Runnable runnable)
    {
        Thread thread = delegate.newThread(runnable);
        thread.setName(name + "-" + count.incrementAndGet());

        return thread;
    }
}
