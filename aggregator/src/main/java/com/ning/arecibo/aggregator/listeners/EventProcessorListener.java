package com.ning.arecibo.aggregator.listeners;

import java.util.Map;

public interface EventProcessorListener {
    void processEvent(Map<String, Object> map);
}
