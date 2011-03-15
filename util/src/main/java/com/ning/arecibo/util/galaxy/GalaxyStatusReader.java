package com.ning.arecibo.util.galaxy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import com.ning.arecibo.util.Logger;

public class GalaxyStatusReader
{
    private static final Logger log = Logger.getLogger(GalaxyStatusReader.class);

    public static List<GalaxyCoreStatus> getCoreStatusList(Reader galaxyReader) throws GalaxyShowWrapperException
    {
        ArrayList<GalaxyCoreStatus> coreStatii = new ArrayList<GalaxyCoreStatus>();
        BufferedReader bReader = null;
        try {
            try {
                bReader = new BufferedReader(galaxyReader);
                String line;

                while ((line = bReader.readLine()) != null) {

                    GalaxyCoreStatus coreStatus = new GalaxyCoreStatus(line);
                    if (coreStatus.isRunning()) {
                        coreStatii.add(coreStatus);
                    }
                }

                if (log.isDebugEnabled()) {
                    for (GalaxyCoreStatus coreStatus : coreStatii) {
                        log.debug("Loaded core status for: " + coreStatus.getZoneHostName());
                    }
                }

                return coreStatii;
            }
            finally {
                if (bReader != null) {
                    bReader.close();
                }
            }
        }
        catch (IOException ioEx) {
            throw new GalaxyShowWrapperException("Got IOException", ioEx);
        }
    }

    public static List<GalaxyCoreStatus> parseGalaxyLines(Collection<String> list)
    {
        List<GalaxyCoreStatus> ret = new ArrayList<GalaxyCoreStatus>();
        for ( String line : list ) {
            ret.add(new GalaxyCoreStatus(line));
        }
        return ret ;
    }
}
