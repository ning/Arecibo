package com.ning.arecibo.alert.objects;

import com.ning.arecibo.alert.conf.ConfigManager;
import com.ning.arecibo.alert.confdata.objects.ConfDataObject;
import com.ning.arecibo.util.Logger;

public class ConfigurableObjectUtils {
    private final static Logger log = Logger.getLogger(ConfigurableObjectUtils.class);

    public static boolean checkNonNullAndValid(ConfigurableObject obj, ConfigManager confManager) {
        if(obj == null || !obj.isValid(confManager))
            return false;
        else
            return true;
    }

    public static boolean checkNonNullAndLog(ConfigurableObject obj,Long objId,String typeName,ConfigManager confManager) {
        if(obj == null) {
            log.warn("Couldn't find '%s' object '%s' from ConfigManager",typeName,objId);
            return false;
        }
        else
            return true;
    }

    public static boolean updateConfigurableObject(ConfDataObject dstObj,ConfDataObject copyObj) {
        if(dstObj == null || copyObj == null)
            throw new IllegalStateException("Null objects passed to updateConfigurableObject");

        if(dstObj.equals((ConfDataObject)copyObj)) {
            return true;
        }
        else {
            dstObj.copyPropertiesMap((ConfDataObject)copyObj);
            log.info("Updating instance of '%s': (%d) %s",dstObj.getTypeName(),dstObj.getId(),dstObj.getLabel());
            return true;
        }
    }
}
