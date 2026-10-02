package com.anujbohre.uqlab.service;

import com.anujbohre.uqlab.api.dto.DistributionSpec;
import com.anujbohre.uqlab.api.dto.ModelInfo;
import com.anujbohre.uqlab.api.dto.SimulationRequest;
import com.anujbohre.uqlab.api.dto.SimulationResponse;
import com.anujbohre.uqlab.domain.distribution.Distribution;
import com.anujbohre.uqlab.domain.model.SimulationModel;
import com.anujbohre.uqlab.domain.montecarlo.MonteCarloEngine;
import com.anujbohre.uqlab.domain.montecarlo.SimulationResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;

/** Application logic: validates the request, builds the domain objects, runs the engine. */
@Service
public class SimulationService {

    static final int DEFAULT_SAMPLES = 10_000;
    static final int DEFAULT_BINS = 20;

    private final ModelRegistry registry;
    private final DistributionFactory distributionFactory;
    private final MonteCarloEngine engine;

    // Spring supplies all three through this single constructor (constructor injection).
    public SimulationService(
            ModelRegistry registry, DistributionFactory distributionFactory, MonteCarloEngine engine) {
        this.registry = registry;
        this.distributionFactory = distributionFactory;
        this.engine = engine;
    }

    public List<ModelInfo> listModels() {
        return registry.all().stream().map(ModelInfo::from).toList();
    }

    public SimulationResponse run(SimulationRequest request) {
        SimulationModel model = registry.find(request.modelId())
                .orElseThrow(() -> new ModelNotFoundException(request.modelId()));

        Map<String, Distribution> distributions = new LinkedHashMap<>();
        for (Map.Entry<String, DistributionSpec> entry : request.inputs().entrySet()) {
            try {
                distributions.put(entry.getKey(), distributionFactory.create(entry.getValue()));
            } catch (InvalidSimulationRequestException e) {
                throw new InvalidSimulationRequestException(
                        "Input '" + entry.getKey() + "': " + e.getMessage(), e);
            }
        }

        int samples = request.samples() != null ? request.samples() : DEFAULT_SAMPLES;
        int bins = request.bins() != null ? request.bins() : DEFAULT_BINS;
        // Kept below 2^31 so the value survives a round trip through JavaScript numbers.
        long seed = request.seed() != null ? request.seed() : ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);

        long start = System.nanoTime();
        SimulationResult result;
        try {
            result = engine.run(model, distributions, samples, seed, bins);
        } catch (IllegalArgumentException e) {
            // e.g. a missing or unknown input name
            throw new InvalidSimulationRequestException(e.getMessage(), e);
        }
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        return SimulationResponse.from(model.id(), model.outputName(), model.outputUnit(), result, elapsedMillis);
    }
}
