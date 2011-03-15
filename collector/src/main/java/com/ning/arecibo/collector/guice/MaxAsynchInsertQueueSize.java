package com.ning.arecibo.collector.guice;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@BindingAnnotation
public @interface MaxAsynchInsertQueueSize
{
	int DEFAULT = 100;
	String PROPERTY_NAME = "arecibo.events.collector.maxAsynchInsertQueueSize";
}
