package com.anujbohre.uqlab.api;

import com.anujbohre.uqlab.api.dto.ModelInfo;
import com.anujbohre.uqlab.api.dto.SimulationRequest;
import com.anujbohre.uqlab.api.dto.SimulationResponse;
import com.anujbohre.uqlab.service.SimulationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HTTP layer: receives requests, hands them to the service, returns JSON. No logic here. */
@RestController
@RequestMapping("/api")
public class SimulationController {

    private final SimulationService service;

    public SimulationController(SimulationService service) {
        this.service = service;
    }

    /** Lists the available models and their inputs. */
    @GetMapping("/models")
    public List<ModelInfo> models() {
        return service.listModels();
    }

    /** Runs a Monte Carlo simulation synchronously and returns its statistics. */
    @PostMapping("/simulations")
    public SimulationResponse simulate(@Valid @RequestBody SimulationRequest request) {
        return service.run(request);
    }
}
