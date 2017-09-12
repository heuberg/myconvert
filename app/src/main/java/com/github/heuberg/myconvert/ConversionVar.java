package com.github.heuberg.myconvert;

/**
 * Class representing the definition of a variable (name and default value).
 */
public class ConversionVar {
    private final String name;
    private final double defaultVal;

    ConversionVar(String name) {
        this(name, Conversion.DEFAULT_VARIABLE_VALUE);
    }
    ConversionVar(String name, double defaultVal) {
        this.name = name;
        this.defaultVal = defaultVal;
    }
    ConversionVar(ConversionVar v) {
        this.name = v.name;
        this.defaultVal = v.defaultVal;
    }

    public String getName() {
        return name;
    }
    public double getDefaultVal() {
        return defaultVal;
    }
}
