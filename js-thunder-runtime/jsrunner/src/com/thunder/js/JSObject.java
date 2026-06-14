package com.thunder.js;

import java.util.*;

public class JSObject {
    public final Map<String, Object> props;
    public JSObject prototype;

    public JSObject() {
        this.props = new LinkedHashMap<>();
    }

    public JSObject(Map<String, Object> props) {
        this.props = new LinkedHashMap<>(props);
    }

    public Object get(String key) {
        if (props.containsKey(key)) return props.get(key);
        if (prototype != null) return prototype.get(key);
        return Interpreter.UNDEFINED;
    }

    public void set(String key, Object value) {
        props.put(key, value);
    }

    public boolean has(String key) {
        if (props.containsKey(key)) return true;
        if (prototype != null) return prototype.has(key);
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (var entry : props.entrySet()) {
            if (!first) sb.append(", ");
            first = false;
            sb.append(entry.getKey()).append(": ").append(Interpreter.jsToString(entry.getValue()));
        }
        sb.append("}");
        return sb.toString();
    }
}
