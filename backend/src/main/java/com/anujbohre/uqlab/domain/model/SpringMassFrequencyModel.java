package com.anujbohre.uqlab.domain.model;

import java.util.List;

/**
 * Undamped natural frequency of a single-degree-of-freedom spring-mass system.
 *
 * <pre>
 *   f = (1 / (2 * pi)) * sqrt(k / m)       (hertz)
 * </pre>
 */
public final class SpringMassFrequencyModel implements SimulationModel {

    public static final String ID = "spring-mass-natural-frequency";

    private static final List<InputDescriptor> INPUTS = List.of(
            new InputDescriptor("stiffness", "N/m", "Spring stiffness k"),
            new InputDescriptor("mass", "kg", "Mass m"));

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String title() {
        return "Spring-mass natural frequency (undamped, 1 degree of freedom)";
    }

    @Override
    public String outputName() {
        return "naturalFrequency";
    }

    @Override
    public String outputUnit() {
        return "Hz";
    }

    @Override
    public List<InputDescriptor> inputs() {
        return INPUTS;
    }

    @Override
    public double evaluate(double[] x) {
        double k = x[0];
        double m = x[1];
        if (!(k > 0) || !(m > 0)) {
            return Double.NaN;
        }
        return Math.sqrt(k / m) / (2.0 * Math.PI);
    }
}
