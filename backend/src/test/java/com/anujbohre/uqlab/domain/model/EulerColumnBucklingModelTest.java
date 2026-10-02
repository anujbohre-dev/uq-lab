package com.anujbohre.uqlab.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EulerColumnBucklingModelTest {

    private final EulerColumnBucklingModel model = new EulerColumnBucklingModel();

    @Test
    void steelPinnedPinnedColumnMatchesHandCalculation() {
        // E = 200 GPa, d = 50 mm, L = 2 m, K = 1  ->  I = 3.0680e-7 m^4, P_cr = 151 397.835 N
        double p = model.evaluate(new double[] {200e9, 0.05, 2.0, 1.0});
        assertEquals(151397.8353530265, p, 1e-6);
    }

    @Test
    void aluminiumFixedPinnedColumnMatchesHandCalculation() {
        double p = model.evaluate(new double[] {70e9, 0.03, 1.5, 0.7});
        assertEquals(24915.7580466695, p, 1e-6);
    }

    @Test
    void doublingLengthQuartersTheLoad() {
        double p1 = model.evaluate(new double[] {200e9, 0.05, 2.0, 1.0});
        double p2 = model.evaluate(new double[] {200e9, 0.05, 4.0, 1.0});
        assertEquals(p1 / 4.0, p2, 1e-6);
    }

    @Test
    void nonPhysicalInputsGiveNaN() {
        assertTrue(Double.isNaN(model.evaluate(new double[] {-1.0, 0.05, 2.0, 1.0})));
        assertTrue(Double.isNaN(model.evaluate(new double[] {200e9, 0.0, 2.0, 1.0})));
        assertTrue(Double.isNaN(model.evaluate(new double[] {200e9, 0.05, -2.0, 1.0})));
        assertTrue(Double.isNaN(model.evaluate(new double[] {200e9, 0.05, 2.0, 0.0})));
    }

    @Test
    void declaresFourInputsInEvaluationOrder() {
        assertEquals(4, model.inputs().size());
        assertEquals("youngsModulus", model.inputs().get(0).name());
        assertEquals("diameter", model.inputs().get(1).name());
        assertEquals("length", model.inputs().get(2).name());
        assertEquals("effectiveLengthFactor", model.inputs().get(3).name());
    }
}
