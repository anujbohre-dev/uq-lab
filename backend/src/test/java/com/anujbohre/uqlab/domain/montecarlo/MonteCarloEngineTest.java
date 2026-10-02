package com.anujbohre.uqlab.domain.montecarlo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.anujbohre.uqlab.domain.distribution.ConstantDistribution;
import com.anujbohre.uqlab.domain.distribution.Distribution;
import com.anujbohre.uqlab.domain.distribution.LogNormalDistribution;
import com.anujbohre.uqlab.domain.distribution.NormalDistribution;
import com.anujbohre.uqlab.domain.model.EulerColumnBucklingModel;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MonteCarloEngineTest {

    private static final double CLOSED_FORM = 151397.8353530265; // E=200 GPa, d=50 mm, L=2 m, K=1

    private final MonteCarloEngine engine = new MonteCarloEngine();
    private final EulerColumnBucklingModel model = new EulerColumnBucklingModel();

    private static Map<String, Distribution> certainInputs() {
        Map<String, Distribution> in = new LinkedHashMap<>();
        in.put("youngsModulus", new ConstantDistribution(200e9));
        in.put("diameter", new ConstantDistribution(0.05));
        in.put("length", new ConstantDistribution(2.0));
        in.put("effectiveLengthFactor", new ConstantDistribution(1.0));
        return in;
    }

    private static Map<String, Distribution> uncertainInputs() {
        Map<String, Distribution> in = new LinkedHashMap<>();
        in.put("youngsModulus", new LogNormalDistribution(200e9, 10e9)); // 5 % scatter
        in.put("diameter", new NormalDistribution(0.05, 0.0005)); // 1 %
        in.put("length", new NormalDistribution(2.0, 0.01)); // 0.5 %
        in.put("effectiveLengthFactor", new ConstantDistribution(1.0));
        return in;
    }

    @Test
    void zeroSpreadReproducesTheClosedFormSolutionExactly() {
        SimulationResult r = engine.run(model, certainInputs(), 1_000, 1L, 10);
        assertEquals(CLOSED_FORM, r.mean(), 1e-6);
        assertEquals(0.0, r.stdDev(), 1e-9);
        assertEquals(CLOSED_FORM, r.percentiles().get(50), 1e-6);
        assertEquals(CLOSED_FORM, r.min(), 1e-6);
        assertEquals(CLOSED_FORM, r.max(), 1e-6);
        assertEquals(1, r.binCounts().size());
        assertEquals(1_000L, r.binCounts().get(0).longValue());
    }

    @Test
    void sameSeedReproducesTheRunExactly() {
        SimulationResult a = engine.run(model, uncertainInputs(), 5_000, 123L, 20);
        SimulationResult b = engine.run(model, uncertainInputs(), 5_000, 123L, 20);
        assertEquals(a, b);
    }

    @Test
    void differentSeedsGiveDifferentResults() {
        SimulationResult a = engine.run(model, uncertainInputs(), 5_000, 1L, 20);
        SimulationResult b = engine.run(model, uncertainInputs(), 5_000, 2L, 20);
        assertTrue(a.mean() != b.mean());
    }

    @Test
    void meanIsWithinFourStandardErrorsOfALongReferenceRun() {
        SimulationResult shortRun = engine.run(model, uncertainInputs(), 20_000, 5L, 20);
        SimulationResult longRun = engine.run(model, uncertainInputs(), 400_000, 6L, 20);
        double allowed = 4.0 * (shortRun.standardError() + longRun.standardError());
        assertEquals(longRun.mean(), shortRun.mean(), allowed);
    }

    @Test
    void standardErrorShrinksWithMoreSamples() {
        SimulationResult small = engine.run(model, uncertainInputs(), 2_000, 9L, 20);
        SimulationResult large = engine.run(model, uncertainInputs(), 200_000, 9L, 20);
        assertTrue(large.standardError() < small.standardError() / 5.0); // ~ 1/sqrt(100) = 1/10
    }

    @Test
    void histogramCountsSumToSamplesUsedAndPercentilesAreOrdered() {
        SimulationResult r = engine.run(model, uncertainInputs(), 10_000, 3L, 25);
        long total = r.binCounts().stream().mapToLong(Long::longValue).sum();
        assertEquals(r.samplesUsed(), total);
        assertEquals(25, r.binCounts().size());
        assertEquals(26, r.binEdges().size());
        assertTrue(r.percentiles().get(1) <= r.percentiles().get(5));
        assertTrue(r.percentiles().get(5) <= r.percentiles().get(50));
        assertTrue(r.percentiles().get(50) <= r.percentiles().get(95));
        assertTrue(r.percentiles().get(95) <= r.percentiles().get(99));
        assertTrue(r.min() <= r.percentiles().get(1) && r.percentiles().get(99) <= r.max());
    }

    @Test
    void nonPhysicalSamplesAreRejectedAndCounted() {
        Map<String, Distribution> in = certainInputs();
        in.put("length", new NormalDistribution(2.0, 2.0)); // about 16 % of draws are negative
        SimulationResult r = engine.run(model, in, 10_000, 4L, 10);
        assertTrue(r.samplesRejected() > 0);
        assertEquals(10_000, r.samplesUsed() + r.samplesRejected());
    }

    @Test
    void failsClearlyWhenEverySampleIsRejected() {
        Map<String, Distribution> in = certainInputs();
        in.put("length", new ConstantDistribution(-2.0));
        assertThrows(NoValidSamplesException.class, () -> engine.run(model, in, 100, 1L, 10));
    }

    @Test
    void rejectsMissingAndUnknownInputs() {
        Map<String, Distribution> missing = certainInputs();
        missing.remove("diameter");
        assertThrows(IllegalArgumentException.class, () -> engine.run(model, missing, 100, 1L, 10));

        Map<String, Distribution> extra = certainInputs();
        extra.put("temperature", new ConstantDistribution(20.0));
        assertThrows(IllegalArgumentException.class, () -> engine.run(model, extra, 100, 1L, 10));
    }
}
