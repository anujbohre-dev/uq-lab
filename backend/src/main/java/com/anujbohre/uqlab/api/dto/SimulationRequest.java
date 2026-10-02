package com.anujbohre.uqlab.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.Map;

/**
 * Body of {@code POST /api/simulations}.
 *
 * @param modelId which model to run, e.g. "euler-column-buckling"
 * @param inputs a distribution for every model input, keyed by input name
 * @param samples number of Monte Carlo samples (optional, default 10 000, range 100 to 1 000 000)
 * @param seed random seed (optional; if omitted one is chosen and returned so the run can be repeated)
 * @param bins histogram bins (optional, default 20, range 2 to 200)
 */
public record SimulationRequest(
        @NotBlank String modelId,
        @NotEmpty Map<@NotBlank String, @NotNull @Valid DistributionSpec> inputs,
        @Min(100) @Max(1_000_000) Integer samples,
        @PositiveOrZero Long seed,
        @Min(2) @Max(200) Integer bins) {}
