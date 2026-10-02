package com.anujbohre.uqlab.domain.montecarlo;

import com.anujbohre.uqlab.domain.distribution.Distribution;
import com.anujbohre.uqlab.domain.model.InputDescriptor;
import com.anujbohre.uqlab.domain.model.SimulationModel;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

/**
 * Monte Carlo uncertainty propagation.
 *
 * <p>Algorithm: for each of N samples, draw one random value for every input (in the model's
 * input order), evaluate the model, and keep the output if it is finite. Then summarise the kept
 * outputs. The estimate of the mean has standard error about stdDev / sqrt(N).
 *
 * <p>The engine is plain Java (no Spring) so it can be unit-tested and reused anywhere.
 */
public final class MonteCarloEngine {

    private static final int[] PERCENTILES = {1, 5, 25, 50, 75, 95, 99};

    /**
     * Runs a simulation.
     *
     * @param model the deterministic model to evaluate
     * @param inputs one distribution per model input, keyed by input name
     * @param samples number of samples, at least 1
     * @param seed random seed; the same seed and inputs always give the same result
     * @param bins number of histogram bins, at least 1
     * @throws IllegalArgumentException if arguments are inconsistent with the model
     * @throws NoValidSamplesException if every sample was rejected
     */
    public SimulationResult run(
            SimulationModel model, Map<String, Distribution> inputs, int samples, long seed, int bins) {
        if (samples < 1) {
            throw new IllegalArgumentException("samples must be at least 1");
        }
        if (bins < 1) {
            throw new IllegalArgumentException("bins must be at least 1");
        }

        List<InputDescriptor> descriptors = model.inputs();
        Distribution[] distributions = new Distribution[descriptors.size()];
        for (int j = 0; j < descriptors.size(); j++) {
            String name = descriptors.get(j).name();
            Distribution d = inputs.get(name);
            if (d == null) {
                throw new IllegalArgumentException("Missing distribution for input '" + name + "'");
            }
            distributions[j] = d;
        }
        for (String given : inputs.keySet()) {
            boolean known = descriptors.stream().anyMatch(desc -> desc.name().equals(given));
            if (!known) {
                throw new IllegalArgumentException(
                        "Unknown input '" + given + "' for model '" + model.id() + "'");
            }
        }

        SplittableRandom rng = new SplittableRandom(seed);
        double[] x = new double[distributions.length];
        double[] outputs = new double[samples];
        int used = 0;
        for (int i = 0; i < samples; i++) {
            for (int j = 0; j < distributions.length; j++) {
                x[j] = distributions[j].sample(rng);
            }
            double y = model.evaluate(x);
            if (Double.isFinite(y)) {
                outputs[used++] = y;
            }
        }
        if (used == 0) {
            throw new NoValidSamplesException(
                    "All " + samples + " samples were outside the model's physical domain; "
                            + "check the input distributions (for example, negative lengths).");
        }

        double[] valid = java.util.Arrays.copyOf(outputs, used);
        double[] sorted = Statistics.sortedCopy(valid);
        double mean = Statistics.mean(valid);
        double std = Statistics.sampleStdDev(valid);
        double min = sorted[0];
        double max = sorted[used - 1];

        Map<Integer, Double> percentiles = new LinkedHashMap<>();
        for (int p : PERCENTILES) {
            percentiles.put(p, Statistics.percentile(sorted, p));
        }

        double[] edges = Statistics.binEdges(min, max, bins);
        long[] counts = Statistics.histogramCounts(valid, edges);

        List<Double> edgeList = new ArrayList<>(edges.length);
        for (double e : edges) {
            edgeList.add(e);
        }
        List<Long> countList = new ArrayList<>(counts.length);
        for (long c : counts) {
            countList.add(c);
        }

        return new SimulationResult(
                samples,
                used,
                samples - used,
                seed,
                mean,
                std,
                std / Math.sqrt(used),
                min,
                max,
                percentiles,
                List.copyOf(edgeList),
                List.copyOf(countList));
    }
}
