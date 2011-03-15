package com.ning.arecibo.agent.guice;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@BindingAnnotation
public @interface ConfigUpdateInterval
{
    final static int FIVE_MINUTES = 60 * 5; 
    
	int DEFAULT = FIVE_MINUTES;
}
