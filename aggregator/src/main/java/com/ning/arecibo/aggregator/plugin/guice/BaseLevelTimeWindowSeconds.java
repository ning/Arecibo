package com.ning.arecibo.aggregator.plugin.guice;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@BindingAnnotation
public @interface BaseLevelTimeWindowSeconds
{
    String PROPERTY_NAME = "arecibo.aggregator.baseLevel.timeWindowSeconds";
    int DEFAULT = 150;
}
