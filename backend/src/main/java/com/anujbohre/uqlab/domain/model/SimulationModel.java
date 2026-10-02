package com.anujbohre.uqlab.domain.model;

import java.util.List;

/**
 * A deterministic engineering model: given exact input values, it returns one output value.
 * The Monte Carlo engine calls it once per random sample.
 *
 * <p>This is the Strategy pattern: new models (cantilever, spring-mass, ...) implement this
 * interface and the rest of the system does not change.
 */
public interface SimulationModel {

    /** Stable identifier used in the API, e.g. "euler-column-buckling". */
    String id();

    /** Human-readable title. */
    String title();

    /** Name of the output quantity, e.g. "criticalLoad". */
    String outputName();

    /** Unit of the output, e.g. "N". */
    String outputUnit();

    /** The inputs, in the order in which {@link #evaluate(double[])} expects them. */
    List<InputDescriptor> inputs();

    /**
     * Evaluates the model.
     *
     * @param x input values in the order given by {@link #inputs()}
     * @return the output, or {@link Double#NaN} if the point is outside the model's physical
     *     domain (for example a negative length). NaN results are rejected by the engine.
     */
    double evaluate(double[] x);
}
