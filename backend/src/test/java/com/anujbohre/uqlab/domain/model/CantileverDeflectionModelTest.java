package com.anujbohre.uqlab.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CantileverDeflectionModelTest {

    private final CantileverDeflectionModel model = new CantileverDeflectionModel();

    // Input order: load [N], length [m], youngsModulus [Pa], diameter [m]

    @Test
    void steelBeamMatchesHandCalculation() {
        // P = 1 kN, L = 1 m, E = 200 GPa, d = 50 mm -> I = 3.0680e-7 m^4, delta = 5.4325 mm
        double delta = model.evaluate(new double[] {1000.0, 1.0, 200e9, 0.05});
        assertEquals(0.0054324887242033605, delta, 1e-12);
    }

    @Test
    void aluminiumBeamMatchesHandCalculation() {
        double delta = model.evaluate(new double[] {500.0, 0.8, 70e9, 0.02});
        assertEquals(0.15521396354866748, delta, 1e-12);
    }

    @Test
    void doublingLengthMultipliesDeflectionByEight() {
        double d1 = model.evaluate(new double[] {1000.0, 1.0, 200e9, 0.05});
        double d2 = model.evaluate(new double[] {1000.0, 2.0, 200e9, 0.05});
        assertEquals(8.0 * d1, d2, 1e-12);
    }

    @Test
    void doublingDiameterDividesDeflectionBySixteen() {
        double d1 = model.evaluate(new double[] {1000.0, 1.0, 200e9, 0.05});
        double d2 = model.evaluate(new double[] {1000.0, 1.0, 200e9, 0.10});
        assertEquals(d1 / 16.0, d2, 1e-12);
    }

    @Test
    void zeroLoadGivesZeroDeflection() {
        assertEquals(0.0, model.evaluate(new double[] {0.0, 1.0, 200e9, 0.05}), 0.0);
    }

    @Test
    void nonPhysicalInputsGiveNaN() {
        assertTrue(Double.isNaN(model.evaluate(new double[] {-1.0, 1.0, 200e9, 0.05})));
        assertTrue(Double.isNaN(model.evaluate(new double[] {1000.0, 0.0, 200e9, 0.05})));
        assertTrue(Double.isNaN(model.evaluate(new double[] {1000.0, 1.0, -200e9, 0.05})));
        assertTrue(Double.isNaN(model.evaluate(new double[] {1000.0, 1.0, 200e9, 0.0})));
        assertTrue(Double.isNaN(model.evaluate(new double[] {Double.NaN, 1.0, 200e9, 0.05})));
    }

    @Test
    void describesItsInputsInEvaluationOrder() {
        assertEquals("cantilever-tip-deflection", model.id());
        assertEquals("tipDeflection", model.outputName());
        assertEquals("m", model.outputUnit());
        assertEquals(4, model.inputs().size());
        assertEquals("load", model.inputs().get(0).name());
        assertEquals("diameter", model.inputs().get(3).name());
    }
}
