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
import com.google.inject.TypeLiteral;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

public class HashMapProvider<KeyType, ValueType> implements Provider<HashMap<KeyType, ValueType>>
{
    HashMap<KeyType, ValueType> instancesMap = new HashMap<KeyType, ValueType>();
    HashMap<KeyType, Key<? extends ValueType>> valueInjectedMap = new HashMap<KeyType, Key<? extends ValueType>>();
    HashMap<Key<? extends KeyType>, Key<? extends ValueType>> keyAndValueInjectedMap = new HashMap<Key<? extends KeyType>, Key<? extends ValueType>>();

    private Injector injector;

    public HashMapProvider<KeyType, ValueType> put(KeyType key, ValueType value)
    {
        instancesMap.put(key, value);
        return this;
    }

    public HashMapProvider<KeyType, ValueType> put(KeyType key, Class<? extends ValueType> toInject)
    {
        valueInjectedMap.put(key, Key.get(toInject));
        return this;
    }

    public HashMapProvider<KeyType, ValueType> put(KeyType key, Class<? extends Annotation> annotation, Class<? extends ValueType> toInject)
    {
        valueInjectedMap.put(key, Key.get(toInject, annotation));
        return this;
    }

    public HashMapProvider<KeyType, ValueType> put(KeyType key, TypeLiteral<ValueType> toInject)
    {
        valueInjectedMap.put(key, Key.get(toInject));
        return this;
    }

    public HashMapProvider<KeyType, ValueType> put(KeyType key, Class<? extends Annotation> annotation, TypeLiteral<ValueType> toInject)
    {
        valueInjectedMap.put(key, Key.get(toInject, annotation));
        return this;
    }

    public HashMapProvider<KeyType, ValueType> put(Class<? extends KeyType> keyToInject, Class<? extends ValueType> valueToInject)
    {
        keyAndValueInjectedMap.put(Key.get(keyToInject), Key.get(valueToInject));
        return this;
    }

    public HashMapProvider<KeyType, ValueType> put(Class<? extends Annotation> keyAnnotation, Class<? extends KeyType> keyToInject, Class<? extends ValueType> valueToInject)
    {
        keyAndValueInjectedMap.put(Key.get(keyToInject, keyAnnotation), Key.get(valueToInject));
        return this;
    }

    public HashMapProvider<KeyType, ValueType> put(Class<? extends Annotation> keyAnnotation, Class<? extends KeyType> keyToInject, Class<? extends Annotation> valueAnnotation, Class<? extends ValueType> valueToInject)
    {
        keyAndValueInjectedMap.put(Key.get(keyToInject, keyAnnotation), Key.get(valueToInject, valueAnnotation));
        return this;
    }

    public HashMapProvider<KeyType, ValueType> put(Class<? extends KeyType> keyToInject, TypeLiteral<ValueType> valueToInject)
    {
        keyAndValueInjectedMap.put(Key.get(keyToInject), Key.get(valueToInject));
        return this;
    }

    public HashMapProvider<KeyType, ValueType> put(Class<? extends KeyType> keyToInject, Class<? extends Annotation> valueAnnotation, TypeLiteral<ValueType> valueToInject)
    {
        keyAndValueInjectedMap.put(Key.get(keyToInject), Key.get(valueToInject, valueAnnotation));
        return this;
    }

    public HashMapProvider<KeyType, ValueType> put(Key<? extends KeyType> keyToInject, Key<? extends ValueType> valueToInject)
    {
        keyAndValueInjectedMap.put(keyToInject, valueToInject);
        return this;
    }

    @Inject
    public void configure(Injector injector)
    {
        this.injector = injector;
    }

    public HashMap<KeyType, ValueType> get()
    {
        HashMap<KeyType, ValueType> retVal = new HashMap<KeyType, ValueType>();
        for (Map.Entry<KeyType, ValueType> instances : instancesMap.entrySet()) {
            retVal.put(instances.getKey(), instances.getValue());
        }
        for (Map.Entry<KeyType, Key<? extends ValueType>> entryToInject : valueInjectedMap.entrySet()) {
            retVal.put(entryToInject.getKey(), injector.getInstance(entryToInject.getValue()));
        }
        for (Map.Entry<Key<? extends KeyType>, Key<? extends ValueType>> entryToInject : keyAndValueInjectedMap.entrySet()) {
            retVal.put(injector.getInstance(entryToInject.getKey()), injector.getInstance(entryToInject.getValue()));
        }
        return retVal;
    }
}
