package com.anujbohre.uqlab.service;

import com.anujbohre.uqlab.domain.model.SimulationModel;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Looks up simulation models by id. Spring injects every {@link SimulationModel} bean into the
 * constructor (dependency injection), so adding a model needs no change here.
 */
@Component
public class ModelRegistry {

    private final Map<String, SimulationModel> byId = new LinkedHashMap<>();

    public ModelRegistry(List<SimulationModel> models) {
        for (SimulationModel model : models) {
            if (byId.putIfAbsent(model.id(), model) != null) {
                throw new IllegalStateException("Duplicate model id: " + model.id());
            }
        }
    }

    public Optional<SimulationModel> find(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public List<SimulationModel> all() {
        return List.copyOf(byId.values());
    }
}
