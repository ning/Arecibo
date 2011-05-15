package com.ning.arecibo.util.service;

import org.skife.config.Config;
import org.skife.config.Default;

public interface ConsistentHashingConfig
{
    @Config("arecibo.consistent.hash.nodes")
    @Default("100")
    int getVirtualNodes();
}
