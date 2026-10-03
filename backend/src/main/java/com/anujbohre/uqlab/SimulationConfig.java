package com.anujbohre.uqlab;

import com.anujbohre.uqlab.domain.model.CantileverDeflectionModel;
import com.anujbohre.uqlab.domain.model.EulerColumnBucklingModel;
import com.anujbohre.uqlab.domain.model.SimulationModel;
import com.anujbohre.uqlab.domain.model.SpringMassFrequencyModel;
import com.anujbohre.uqlab.domain.montecarlo.MonteCarloEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the plain-Java domain objects as Spring beans. The domain package itself contains no
 * Spring annotations, so it stays independent of the framework.
 *
 * <p>To add a model later, add another {@code @Bean} method returning {@link SimulationModel};
 * the {@code ModelRegistry} receives every such bean automatically.
 */
@Configuration
public class SimulationConfig {

    @Bean
    public SimulationModel eulerColumnBucklingModel() {
        return new EulerColumnBucklingModel();
    }

    @Bean
    public SimulationModel cantileverDeflectionModel() {
        return new CantileverDeflectionModel();
    }

    @Bean
    public SimulationModel springMassFrequencyModel() {
        return new SpringMassFrequencyModel();
    }

    @Bean
    public MonteCarloEngine monteCarloEngine() {
        return new MonteCarloEngine();
    }
}
