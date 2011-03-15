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
