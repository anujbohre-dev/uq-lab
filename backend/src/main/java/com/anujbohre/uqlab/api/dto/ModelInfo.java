package com.anujbohre.uqlab.api.dto;

import com.anujbohre.uqlab.domain.model.InputDescriptor;
import com.anujbohre.uqlab.domain.model.SimulationModel;
import java.util.List;

/** Description of an available model, returned by {@code GET /api/models}. */
public record ModelInfo(
        String id, String title, String outputName, String outputUnit, List<InputDescriptor> inputs) {

    public static ModelInfo from(SimulationModel model) {
        return new ModelInfo(
                model.id(), model.title(), model.outputName(), model.outputUnit(), model.inputs());
    }
}
