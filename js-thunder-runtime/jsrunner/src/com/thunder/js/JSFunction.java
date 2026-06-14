package com.thunder.js;

import java.util.List;

public class JSFunction {
    public final String name;
    public final List<String> params;
    public final Node body;
    public final Environment closure;
    public final boolean isArrow;

    public JSFunction(String name, List<String> params, Node body, Environment closure, boolean isArrow) {
        this.name = name;
        this.params = params;
        this.body = body;
        this.closure = closure;
        this.isArrow = isArrow;
    }

    @Override
    public String toString() {
        return "[Function: " + (name != null ? name : "anonymous") + "]";
    }
}
