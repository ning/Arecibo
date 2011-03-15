package com.ning.arecibo.dashboard.client.datahierarchy.types;

// TODO: enumerate these for now, eventually these should be dynamic
public enum DataHierarchyTokenType {

    type("TYPE:"),
    path("PATH:"),
    host("HOST:"),
    event("EVENT:"),
    attribute("ATTRIBUTE:"),
    end("END:");

    private String specifier;

    private DataHierarchyTokenType(String specifier) {
        this.specifier = specifier;
    }

    public String getSpecifier() {
        return this.specifier;
    }
}
