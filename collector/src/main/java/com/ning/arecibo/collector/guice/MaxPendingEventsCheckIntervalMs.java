package com.ning.arecibo.collector.guice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.google.inject.BindingAnnotation;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@BindingAnnotation
public @interface MaxPendingEventsCheckIntervalMs
{
	long DEFAULT = 10000L;
	String PROPERTY_NAME = "arecibo.events.collector.maxPendingEventsCheckIntervalMs";
}
