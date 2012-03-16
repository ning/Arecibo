/*
 * Copyright 2010-2012 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.arecibo.util;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import org.weakref.jmx.MBeanExporter;
import org.weakref.jmx.ObjectNames;

import java.lang.annotation.Annotation;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

public class ArrayListProvider<T> implements Provider<List<T>>
{
    private Injector injector;
    private final List<Key<? extends T>> injectables = new ArrayList<Key<? extends T>>();
    private final List<Key<? extends T>> exportables = new ArrayList<Key<? extends T>>();
    private final MBeanExporter exporter = new MBeanExporter(ManagementFactory.getPlatformMBeanServer());

    @Inject
    public void configure(final Injector injector)
    {
        this.injector = injector;
    }

    public ArrayListProvider<T> add(final Class<? extends T> toBeIncluded)
    {
        injectables.add(Key.get(toBeIncluded));
        return this;
    }

    public ArrayListProvider<T> addExportable(final Class<? extends T> toBeIncluded)
    {
        exportables.add(Key.get(toBeIncluded));
        return add(toBeIncluded);
    }

    public ArrayListProvider<T> add(final Class<? extends Annotation> annotation, final Class<? extends T> toBeIncluded)
    {
        injectables.add(Key.get(toBeIncluded, annotation));
        return this;
    }

    public ArrayListProvider<T> addExportable(final Class<? extends Annotation> annotation, final Class<? extends T> toBeIncluded)
    {
        exportables.add(Key.get(toBeIncluded, annotation));
        return add(annotation, toBeIncluded);
    }

    public ArrayListProvider<T> add(final Annotation annotation, final Class<? extends T> toBeIncluded)
    {
        injectables.add(Key.get(toBeIncluded, annotation));
        return this;
    }

    public ArrayListProvider<T> addExportable(final Annotation annotation, final Class<? extends T> toBeIncluded)
    {
        exportables.add(Key.get(toBeIncluded, annotation));
        return add(annotation, toBeIncluded);
    }

    public List<T> get()
    {
        final ArrayList<T> retVal = new ArrayList<T>();
        for (final Key<? extends T> injectable : injectables) {
            final T instance = injector.getInstance(injectable);

            if (exportables.contains(injectable)) {
                exporter.export(ObjectNames.generatedNameOf(injectable.getTypeLiteral().getRawType()), instance);
            }

            retVal.add(instance);
        }

        return retVal;
    }
}
