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

import com.google.inject.Provider;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;

import java.util.List;
import java.util.ArrayList;
import java.lang.annotation.Annotation;

public class ArrayListProvider<T> implements Provider<List<T>>
{
    private Injector injector;
    private List<Key<? extends T>> injectables = new ArrayList<Key<? extends T>>();

    @Inject
    public void configure(Injector injector)
    {
        this.injector = injector;
    }

    public ArrayListProvider<T> add(Class<? extends T> toBeIncluded)
    {
        injectables.add(Key.get(toBeIncluded));
        return this;
    }

    public ArrayListProvider<T> add(Class<? extends Annotation> annotation, Class<? extends T> toBeIncluded)
    {
        injectables.add(Key.get(toBeIncluded, annotation));
        return this;
    }

    public ArrayListProvider<T> add(Annotation annotation, Class<? extends T> toBeIncluded)
    {
        injectables.add(Key.get(toBeIncluded, annotation));
        return this;
    }

    public List<T> get()
    {
        ArrayList<T> retVal = new ArrayList<T>();
        for ( Key<? extends T> injectable : injectables ) {
            retVal.add(injector.getInstance(injectable));
        }
        return retVal;
    }
}
