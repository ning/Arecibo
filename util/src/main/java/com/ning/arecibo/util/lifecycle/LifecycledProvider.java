package com.ning.arecibo.util.lifecycle;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

/**
 * The LifecycledProvider allows for the registration of objects against a Lifecycle object without creating a new provider type. It is constructed by either
 *
 * (1) Passing in a binder and a class that will be injected normally. I.e. in the case where you have a class that normally wouldn't require a provider, but you want it to be
 * "lifecycled," you would write something like:
 *
 * binder.bind(SomeClass.class).toProvider( new lifecycledProviderInstance(binder, SomeClass.class) )
 *
 * (2) Passing in a binder, the class to inject and the class of the provider to use for injection. I.e. in the case where you have a Guice-injected provider which provides the
 * instance and you want that instance to be "lifecycled," you would write something like:
 *
 * binder.bind(SomeClass.class).toProvider( new lifecycledProviderInstance(binder, SomeClass.class, SomeClassProvider.class) )
 *
 * (3) Passing in a Provider instance that will be used as a delegate provider. I.e. in the case where you are instatiating a provider instance which will then be used as a
 * provider for the object, you would write something like:
 *
 * binder.bind(SomeClass.class).toProvider( new lifecycledProviderInstance(new SomeClassProvider()) )
 *
 * Options 1 and 2 require a binder to work around some circular dependency issues internal to Guice. It might make sense to actually encapsulate this in a Module instead of in a
 * Provider, but I think the idea of composing/wrapping Providers is more intuitive than the magic of a Module.
 */
public class LifecycledProvider<T> implements Provider<T>
{
    private static final AtomicLong                              counter   = new AtomicLong(0);

    private final List<EventAction> listeners = new ArrayList<EventAction>();

    private volatile Key<T>                                      classKey;
    private volatile Provider<T>                                 delegate;
    private volatile Lifecycle                                   lifecycle;

    public LifecycledProvider(Provider<T> delegate)
    {
        this.delegate = delegate;
    }

    public LifecycledProvider(Binder binder, Class<T> classToProvide)
    {
        String injectedName = getInjectedNameForNamedAnnotation();
        final Named annotation = Names.named(injectedName);

        binder.bind(classToProvide).annotatedWith(annotation).toProvider(new GenericProvider<T>(TypeLiteral.get(classToProvide)));

        this.delegate = null;
        this.classKey = Key.get(classToProvide, annotation);
    }

    public LifecycledProvider(Binder binder, Class<T> classToProvide, Class<? extends Provider<T>> provider)
    {
        String injectedName = getInjectedNameForNamedAnnotation();
        final Named annotation = Names.named(injectedName);

        this.delegate = null;
        this.classKey = Key.get(classToProvide, annotation);

        binder.bind(classToProvide).annotatedWith(annotation).toProvider(provider);
    }

    public LifecycledProvider<T> addListener(LifecycleEvent event, LifecycleAction<T> action)
    {
        listeners.add(new EventAction(event, action));
        return this;
    }

    @Inject
    public void configure(Injector injector, Lifecycle lifecycle)
    {
        this.lifecycle = lifecycle;
        if (delegate == null) {
            delegate = injector.getProvider(classKey);
        }
        else {
            // We were given an instance, make sure the delegate gets stuff injected
            injector.injectMembers(delegate);
        }
    }

    public T get()
    {
        final T retVal = delegate.get();
        for (final EventAction listener : listeners) {
            lifecycle.addListener(listener.getEvent(), new LifecycleListener() {
                public void onEvent(final LifecycleEvent event)
                {
                    listener.getAction().doAction(retVal);
                }
            });
        }
        return retVal;
    }

    /**
     * Attempt to get a unique "name" that can be used for a
     *
     * @Named annotation.
     *
     * This is primarily used to add indirection that will allow this provider access to the base provider for the class it is actually providing.
     *
     * @return a (hopefully) unique string that can be used in a
     * @Named annotation
     */
    private String getInjectedNameForNamedAnnotation()
    {
        return String.format("%s_%s_%1$s", counter.getAndIncrement(), LifecycledProvider.class.getName());
    }

    /**
     * Ignore this class, you will be better off, trust me ;). At least, don't say I didn't warn you.
     *
     * This class is a generic provider. It basically gets an injector, looks through the constructors for the class that it is supposed to inject for the one labeled "@Inject,"
     * gets the parameters for that constructor and tries to get them from the injector, ending by constructing the object with those parameters. That is, it is basically the same
     * as what Guice does to figure out implicit construction. The big difference is that I'm not sure if this will hold up and work well with circular dependencies, hopefully
     * Guice's protections are enough...
     */
    private static class GenericProvider<T> implements Provider<T>
    {
        private final TypeLiteral<T> type;

        private volatile Injector    injector;

        public GenericProvider(TypeLiteral<T> type)
        {
            this.type = type;
        }

        @Inject
        public void setInjector(Injector injector)
        {
            this.injector = injector;
        }

        @SuppressWarnings("unchecked")
        public T get()
        {
            Class<? super T> classObject = (Class<? super T>) getRawType(type.getType());
            Constructor<?>[] constructors = classObject.getConstructors();
            for (Constructor<?> constructor : constructors) {
                if (constructor.getAnnotation(Inject.class) != null) { // I have my constructor
                    Class<?>[] parameterTypes = constructor.getParameterTypes();
                    Annotation[][] parameterAnnotations = constructor.getParameterAnnotations();
                    if (parameterTypes.length != parameterAnnotations.length) {
                        throw new RuntimeException("How is this possible?");
                    }

                    Object[] parameters = new Object[parameterTypes.length];
                    for (int i = 0; i < parameterTypes.length; ++i) {
                        if (parameterAnnotations[i].length != 1) {
                            parameters[i] = injector.getInstance(Key.get(parameterTypes[i]));
                        }
                        else {
                            parameters[i] = injector.getInstance(Key.get(parameterTypes[i], parameterAnnotations[i][0]));
                        }
                    }
                    try {
                        return (T) constructor.newInstance(parameters);
                    }
                    catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            throw new RuntimeException(String.format("No @Inject annotations found on class[%s]", classObject.getName()));
        }

        /**
         * This code is copied from Guice because they didn't have the foresite to make it public...
         *
         * @param type look at the source of the Guice 1.0 TypeLiteral class
         * @return look at the source of the Guice 1.0 TypeLiteral class
         */
        private static Class<?> getRawType(Type type)
        {
            if (type instanceof Class<?>) {
                return (Class<?>) type;
            }
            else {
                if (type instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) type;

                    Type rawType = parameterizedType.getRawType();
                    if (!(rawType instanceof Class<?>)) {
                        throw new RuntimeException("Bad things");
                    }
                    return (Class<?>) rawType;
                }

                if (type instanceof GenericArrayType) {
                    return Object[].class;
                }

                throw new RuntimeException("Bad things");
            }
        }
    }

    private class EventAction
    {
        private final LifecycleEvent     event;
        private final LifecycleAction<T> action;

        /**
         * @param event
         * @param action
         */
        public EventAction(LifecycleEvent event, LifecycleAction<T> action)
        {
            this.event = event;
            this.action = action;
        }

        /**
         * @return the event
         */
        public LifecycleEvent getEvent()
        {
            return event;
        }

        /**
         * @return the action
         */
        public LifecycleAction<T> getAction()
        {
            return action;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((action == null) ? 0 : action.hashCode());
            result = prime * result + ((event == null) ? 0 : event.hashCode());
            return result;
        }

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final EventAction other = (EventAction) obj;
            if (action == null) {
                if (other.action != null)
                    return false;
            }
            else if (!action.equals(other.action))
                return false;
            if (event == null) {
                if (other.event != null)
                    return false;
            }
            else if (!event.equals(other.event))
                return false;
            return true;
        }
    }
}
