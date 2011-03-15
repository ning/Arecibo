package com.ning.arecibo.util.jmx;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.weakref.jmx.ManagedAnnotation;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD})
@ManagedAnnotation
public @interface MonitorableManaged
{
    String description() default "";

    /**
     * 
     * @return true if the property should be monitored, defaults to false
     */
    boolean monitored() default false;

    /**
     * 
     * @return monitored event attribute name
     */
    String eventAttributeName() default "";

    /**
     * 
     * @return monitored event name
     */
    String eventName() default "";

    /**
     * 
     * @return monitored event name pattern
     */
    String eventNamePattern() default ".*[Nn]ame=([a-zA-Z0-9_]*).*";

    /**
     * 
     * @return monitoring types
     */
    MonitoringType[] monitoringType() default { MonitoringType.VALUE };

    /**
     * Tags for this managed thing.
     * 
     * @return tags
     */
    String[] tags() default {};
}
