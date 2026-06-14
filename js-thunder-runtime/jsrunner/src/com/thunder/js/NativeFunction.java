package com.thunder.js;

@FunctionalInterface
public interface NativeFunction {
    Object call(Object[] args);
}
