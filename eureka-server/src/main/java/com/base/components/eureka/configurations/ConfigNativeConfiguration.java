package com.base.components.eureka.configurations;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.config.server.environment.NativeEnvironmentProperties;
import org.springframework.cloud.config.server.environment.NativeEnvironmentRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * ConfigNativeConfiguration
 *
 * @author <a href="drakelee1221@gmail.com">LiGeng</a>
 * @version v1.0.0
 * @date 2018-09-06 13:35
 */
@Configuration
@Profile("native")
public class ConfigNativeConfiguration {

  @Autowired
  private ConfigurableEnvironment environment;

  @Bean
  public NativeEnvironmentRepository nativeEnvironmentRepository(NativeEnvironmentProperties environmentProperties) {
    NativeEnvironmentRepository repository = new WildcardNativeEnvironmentRepository(environment, environmentProperties);
    repository.setDefaultLabel(environmentProperties.getDefaultLabel());
    return repository;
  }
}
