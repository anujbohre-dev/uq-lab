package com.anujbohre.uqlab.api.dto;

import jakarta.validation.constraints.NotNull;

/**
 * JSON description of one input's distribution. Which fields are required depends on the type:
 *
 * <ul>
 *   <li>NORMAL, LOGNORMAL: mean, stdDev
 *   <li>UNIFORM: min, max
 *   <li>CONSTANT: value
 * </ul>
 */
public record DistributionSpec(
        @NotNull DistributionType type, Double mean, Double stdDev, Double min, Double max, Double value) {}
