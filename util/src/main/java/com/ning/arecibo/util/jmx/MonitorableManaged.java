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
