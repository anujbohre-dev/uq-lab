package com.anujbohre.uqlab.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SpringMassFrequencyModelTest {

    private final SpringMassFrequencyModel model = new SpringMassFrequencyModel();

    // Input order: stiffness [N/m], mass [kg]

    @Test
    void unitMassMatchesHandCalculation() {
        // f = sqrt(1000 / 1) / (2 pi) = 5.0329 Hz
        assertEquals(5.032921210448704, model.evaluate(new double[] {1000.0, 1.0}), 1e-12);
    }

    @Test
    void secondExampleMatchesHandCalculation() {
        // f = sqrt(4000 / 2.5) / (2 pi) = 40 / (2 pi) = 6.3662 Hz
        assertEquals(6.366197723675814, model.evaluate(new double[] {4000.0, 2.5}), 1e-12);
    }

    @Test
    void quadruplingStiffnessDoublesFrequency() {
        double f1 = model.evaluate(new double[] {1000.0, 1.0});
        double f2 = model.evaluate(new double[] {4000.0, 1.0});
        assertEquals(2.0 * f1, f2, 1e-12);
    }

    @Test
    void nonPhysicalInputsGiveNaN() {
        assertTrue(Double.isNaN(model.evaluate(new double[] {0.0, 1.0})));
        assertTrue(Double.isNaN(model.evaluate(new double[] {1000.0, -1.0})));
        assertTrue(Double.isNaN(model.evaluate(new double[] {Double.NaN, 1.0})));
    }

    @Test
    void describesItsInputsInEvaluationOrder() {
        assertEquals("spring-mass-natural-frequency", model.id());
        assertEquals("naturalFrequency", model.outputName());
        assertEquals("Hz", model.outputUnit());
        assertEquals("stiffness", model.inputs().get(0).name());
        assertEquals("mass", model.inputs().get(1).name());
    }
}
