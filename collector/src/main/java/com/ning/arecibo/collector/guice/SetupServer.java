/*
 * Copyright 2011 Ning, Inc.
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

package com.ning.arecibo.collector.guice;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.servlet.GuiceServletContextListener;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import javax.servlet.ServletContextEvent;

/**
 * Start up the base module when the server comes up. This gets configured in web.xml.
 */
public class SetupServer extends GuiceServletContextListener
{
    private static final Logger log = Logger.getLogger(SetupServer.class);

    private Module guiceModule = null;

    @Override
    public void contextInitialized(final ServletContextEvent event)
    {
        final String moduleClassName = event.getServletContext().getInitParameter("guiceModuleClassName");
        if (StringUtils.isEmpty(moduleClassName)) {
            throw new IllegalStateException("Missing parameter for the base Guice module!");
        }

        try {
            final Class<?> moduleClass = Class.forName(moduleClassName);
            if (!Module.class.isAssignableFrom(moduleClass)) {
                throw new IllegalStateException(String.format("%s exists but is not a Guice Module!", moduleClassName));
            }

            guiceModule = (Module) moduleClass.newInstance();
            log.info("Instantiated " + moduleClassName + " as the main guice module.");
        }
        catch (ClassNotFoundException cnfe) {
            throw new IllegalStateException(cnfe);
        }
        catch (InstantiationException ie) {
            throw new IllegalStateException(ie);
        }
        catch (IllegalAccessException iae) {
            throw new IllegalStateException(iae);
        }

        super.contextInitialized(event);
    }

    /**
     * Do *not* use this method to retrieve the injector. It actually creates a new instance
     * of a Guice injector which in turn will upset Guice.
     */
    @Override
    protected Injector getInjector()
    {
        if (guiceModule == null) {
            throw new IllegalStateException("Never found the Guice Module to use!");
        }

        log.info("Returning injector from " + guiceModule.getClass().getName());
        return Guice.createInjector(Stage.PRODUCTION, guiceModule);
    }

    /**
     * This method can be called by classes extending SetupServer to retrieve
     * the actual injector. This requires some inside knowledge on where it is
     * stored, but the actual key is not visible outside the guice packages.
     */
    Injector injector(final ServletContextEvent event)
    {
        return (Injector) event.getServletContext().getAttribute(Injector.class.getName());
    }
}
