package com.anujbohre.uqlab.service;

import com.anujbohre.uqlab.api.dto.DistributionSpec;
import com.anujbohre.uqlab.domain.distribution.ConstantDistribution;
import com.anujbohre.uqlab.domain.distribution.Distribution;
import com.anujbohre.uqlab.domain.distribution.LogNormalDistribution;
import com.anujbohre.uqlab.domain.distribution.NormalDistribution;
import com.anujbohre.uqlab.domain.distribution.UniformDistribution;
import org.springframework.stereotype.Component;

/** Converts the JSON description of a distribution into a domain {@link Distribution}. */
@Component
public class DistributionFactory {

    /**
     * @throws InvalidSimulationRequestException if a parameter required by the type is missing or invalid
     */
    public Distribution create(DistributionSpec spec) {
        try {
            return switch (spec.type()) {
                case NORMAL -> new NormalDistribution(
                        require(spec.mean(), "mean"), require(spec.stdDev(), "stdDev"));
                case LOGNORMAL -> new LogNormalDistribution(
                        require(spec.mean(), "mean"), require(spec.stdDev(), "stdDev"));
                case UNIFORM -> new UniformDistribution(require(spec.min(), "min"), require(spec.max(), "max"));
                case CONSTANT -> new ConstantDistribution(require(spec.value(), "value"));
            };
        } catch (IllegalArgumentException e) {
            throw new InvalidSimulationRequestException(e.getMessage(), e);
        }
    }

    private static double require(Double value, String field) {
        if (value == null) {
            throw new IllegalArgumentException("'" + field + "' is required for this distribution type");
        }
        return value;
    }
}
