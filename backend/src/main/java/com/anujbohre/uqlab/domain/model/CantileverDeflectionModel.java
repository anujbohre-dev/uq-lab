package com.anujbohre.uqlab.domain.model;

import java.util.List;

/**
 * Tip deflection of a cantilever beam with a solid circular cross-section under a point load at
 * the free end (Euler-Bernoulli beam theory, small deflections).
 *
 * <pre>
 *   I      = pi * d^4 / 64        (second moment of area)
 *   delta  = P * L^3 / (3 * E * I)
 * </pre>
 */
public final class CantileverDeflectionModel implements SimulationModel {

    public static final String ID = "cantilever-tip-deflection";

    private static final List<InputDescriptor> INPUTS = List.of(
            new InputDescriptor("load", "N", "Point load P at the free end"),
            new InputDescriptor("length", "m", "Beam length L"),
            new InputDescriptor("youngsModulus", "Pa", "Young's modulus E of the material"),
            new InputDescriptor("diameter", "m", "Outer diameter d of the solid circular section"));

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String title() {
        return "Cantilever tip deflection (end load, solid circular section)";
    }

    @Override
    public String outputName() {
        return "tipDeflection";
    }

    @Override
    public String outputUnit() {
        return "m";
    }

    @Override
    public List<InputDescriptor> inputs() {
        return INPUTS;
    }

    @Override
    public double evaluate(double[] x) {
        double load = x[0];
        double length = x[1];
        double e = x[2];
        double d = x[3];
        // A zero load is physically valid (zero deflection); negative or NaN values are not.
        if (!(load >= 0) || !(length > 0) || !(e > 0) || !(d > 0)) {
            return Double.NaN;
        }
        double inertia = Math.PI * Math.pow(d, 4) / 64.0;
        return load * Math.pow(length, 3) / (3.0 * e * inertia);
    }
}
