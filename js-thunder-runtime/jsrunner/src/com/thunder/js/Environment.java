package com.thunder.js;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    private final Map<String, Object> vars = new HashMap<>();
    private final Environment parent;

    public Environment(Environment parent) {
        this.parent = parent;
    }

    public void define(String name, Object value) {
        vars.put(name, value);
    }

    public Object get(String name) {
        if (vars.containsKey(name)) return vars.get(name);
        if (parent != null) return parent.get(name);
        return Interpreter.UNDEFINED;
    }

    public boolean has(String name) {
        if (vars.containsKey(name)) return true;
        if (parent != null) return parent.has(name);
        return false;
    }

    public void set(String name, Object value) {
        if (vars.containsKey(name)) { vars.put(name, value); return; }
        if (parent != null && parent.has(name)) { parent.set(name, value); return; }
        // Global assignment
        vars.put(name, value);
    }

    public Environment getParent() { return parent; }
}
