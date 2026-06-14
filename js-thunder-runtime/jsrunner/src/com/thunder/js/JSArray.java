package com.thunder.js;

import java.util.*;
import java.util.stream.*;

public class JSArray extends JSObject {
    public final List<Object> elements;

    public JSArray() {
        this.elements = new ArrayList<>();
    }

    public JSArray(List<Object> elements) {
        this.elements = new ArrayList<>(elements);
    }

    @Override
    public Object get(String key) {
        if (key.equals("length")) return (double) elements.size();
        // Numeric index
        try {
            int idx = Integer.parseInt(key);
            if (idx >= 0 && idx < elements.size()) return elements.get(idx);
            return Interpreter.UNDEFINED;
        } catch (NumberFormatException e) {
            // check props
            if (props.containsKey(key)) return props.get(key);
            return Interpreter.UNDEFINED;
        }
    }

    @Override
    public void set(String key, Object value) {
        if (key.equals("length")) {
            int newLen = (int) Interpreter.toNumber(value);
            while (elements.size() > newLen) elements.remove(elements.size() - 1);
            while (elements.size() < newLen) elements.add(Interpreter.UNDEFINED);
            return;
        }
        try {
            int idx = Integer.parseInt(key);
            while (elements.size() <= idx) elements.add(Interpreter.UNDEFINED);
            elements.set(idx, value);
        } catch (NumberFormatException e) {
            props.put(key, value);
        }
    }

    @Override
    public boolean has(String key) {
        try {
            int idx = Integer.parseInt(key);
            return idx >= 0 && idx < elements.size();
        } catch (NumberFormatException e) {
            return props.containsKey(key) || key.equals("length");
        }
    }

    public String join(String sep) {
        return elements.stream()
            .map(e -> (e == null || e == Interpreter.UNDEFINED) ? "" : Interpreter.jsToString(e))
            .collect(Collectors.joining(sep));
    }

    @Override
    public String toString() {
        return join(",");
    }
}
