package com.anujbohre.uqlab.domain.model;

import java.util.List;

/**
 * Euler critical buckling load of a slender column with a solid circular cross-section.
 *
 * <pre>
 *   I     = pi * d^4 / 64                (second moment of area)
 *   P_cr  = pi^2 * E * I / (K * L)^2     (critical load)
 * </pre>
 *
 * K is the effective-length factor: 1.0 pinned-pinned, 0.5 fixed-fixed, 0.7 fixed-pinned,
 * 2.0 fixed-free. The formula is valid only for slender columns (large L/d); this model does not
 * check slenderness.
 */
public final class EulerColumnBucklingModel implements SimulationModel {

    public static final String ID = "euler-column-buckling";

    private static final List<InputDescriptor> INPUTS = List.of(
            new InputDescriptor("youngsModulus", "Pa", "Young's modulus E of the material"),
            new InputDescriptor("diameter", "m", "Outer diameter d of the solid circular section"),
            new InputDescriptor("length", "m", "Column length L"),
            new InputDescriptor("effectiveLengthFactor", "-", "Effective-length factor K (1.0 = pinned-pinned)"));

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String title() {
        return "Euler column buckling (solid circular section)";
    }

    @Override
    public String outputName() {
        return "criticalLoad";
    }

    @Override
    public String outputUnit() {
        return "N";
    }

    @Override
    public List<InputDescriptor> inputs() {
        return INPUTS;
    }

    @Override
    public double evaluate(double[] x) {
        double e = x[0];
        double d = x[1];
        double length = x[2];
        double k = x[3];
        if (!(e > 0) || !(d > 0) || !(length > 0) || !(k > 0)) {
            return Double.NaN; // non-physical sample (also catches NaN inputs)
        }
        double inertia = Math.PI * Math.pow(d, 4) / 64.0;
        double effectiveLength = k * length;
        return Math.PI * Math.PI * e * inertia / (effectiveLength * effectiveLength);
    }
}
