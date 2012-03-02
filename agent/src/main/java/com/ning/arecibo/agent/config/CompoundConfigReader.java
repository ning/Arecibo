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

package com.ning.arecibo.agent.config;

import java.util.ArrayList;
import java.util.List;


public class CompoundConfigReader implements ConfigReader
{
   ArrayList<ConfigReader> configReaders = null; 
    
   public void addConfigReader(ConfigReader configReader) {
       
       if(configReaders == null) {
           configReaders = new ArrayList<ConfigReader>();
       }
       
       configReaders.add(configReader);
   }
   
   public List<Config> getConfigurations() {
       
       if(configReaders == null)
           return null;
       
       java.util.List<Config> retList = new ArrayList<Config>();
       
       for(ConfigReader configReader:configReaders) {
           List<Config> configs = configReader.getConfigurations();
           if(configs != null)
               retList.addAll(configs);
       }
       
       return retList;
   }
}
