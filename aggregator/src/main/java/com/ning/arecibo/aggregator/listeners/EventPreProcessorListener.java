package com.ning.arecibo.aggregator.listeners;

import java.util.Map;

public interface EventPreProcessorListener {
    void preProcessEvent(Map<String, Object> map);
}
