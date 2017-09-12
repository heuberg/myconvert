package com.github.heuberg.myconvert;

/**
 * Class representing the definition of a result (name and formula).
 */
public class ConversionRes {
    private final String name;
    private final String formula;

    ConversionRes(String name, String formula) {
        this.name = name;
        this.formula = formula;
    }
    ConversionRes(ConversionRes r) {
        this.name = r.name;
        this.formula = r.formula;
    }

    public String getName() {
        return name;
    }
    public String getFormula() {
        return formula;
    }
}
