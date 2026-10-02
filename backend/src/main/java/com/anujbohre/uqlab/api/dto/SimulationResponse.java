package com.anujbohre.uqlab.api.dto;

import com.anujbohre.uqlab.domain.montecarlo.SimulationResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Body returned by {@code POST /api/simulations}. */
public record SimulationResponse(
        String modelId,
        String outputName,
        String outputUnit,
        int samplesRequested,
        int samplesUsed,
        int samplesRejected,
        long seed,
        long elapsedMillis,
        Summary summary,
        Map<String, Double> percentiles,
        Histogram histogram) {

    /** Central tendency and spread of the output. */
    public record Summary(double mean, double stdDev, double standardError, double min, double max) {}

    /** Histogram: {@code binEdges} has one more entry than {@code counts}. */
    public record Histogram(List<Double> binEdges, List<Long> counts) {}

    public static SimulationResponse from(
            String modelId, String outputName, String outputUnit, SimulationResult r, long elapsedMillis) {
        Map<String, Double> percentiles = new LinkedHashMap<>();
        r.percentiles().forEach((p, value) -> percentiles.put("p" + p, value));
        return new SimulationResponse(
                modelId,
                outputName,
                outputUnit,
                r.samplesRequested(),
                r.samplesUsed(),
                r.samplesRejected(),
                r.seed(),
                elapsedMillis,
                new Summary(r.mean(), r.stdDev(), r.standardError(), r.min(), r.max()),
                percentiles,
                new Histogram(r.binEdges(), r.binCounts()));
    }
}
