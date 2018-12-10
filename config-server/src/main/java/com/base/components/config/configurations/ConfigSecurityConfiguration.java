/*
 * Copyright (c) 2017.  mj.he800.com Inc. All rights reserved.
 */

package com.base.components.config.configurations;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * EurekaSecurityConfiguration
 *
 * @author <a href="drakelee1221@gmail.com">LiGeng</a>
 * @version 1.0.0, 2018-02-02 10:12
 */
@Configuration
@EnableWebSecurity
public class ConfigSecurityConfiguration {

  private static final String PASSWORD_ENCODER_NAME = "{noop}";

  private static final String ACTUATOR_USER_ROLE_NAME = "ACTUATOR_USER";

  @Value("${actuator.user.name}")
  private String actuatorUserName;

  @Value("${actuator.user.password}")
  private String actuatorUserPassword;

  @Value("${actuator.user.match-path:}")
  private String actuatorUserMatchPath;

  @Bean
  public UserDetailsService userDetailsService() throws Exception {
    InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
    manager.createUser(User.withUsername(actuatorUserName)
                           .password(PASSWORD_ENCODER_NAME + actuatorUserPassword)
                           .roles(ACTUATOR_USER_ROLE_NAME).build());
    return manager;
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.addAllowedOrigin("*");
    configuration.addAllowedMethod("*");
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Configuration
  public class ActuatorSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
      ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry registry =
        http
        .csrf().disable()
        .cors()
        .and()
        //X-Frame-Options防止网页被Frame
        .headers().frameOptions().disable()
        .and()
        .authorizeRequests()
          .requestMatchers(EndpointRequest.to("status", "info"))
            .permitAll()
          .requestMatchers(EndpointRequest.toAnyEndpoint())
            .hasRole(ACTUATOR_USER_ROLE_NAME);
      customUserMatch(registry)
        .and()
        .httpBasic();
    }

    private ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry customUserMatch(
      ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry matcherRegistry){
      if(StringUtils.isNotBlank(actuatorUserMatchPath)){
        String[] path = StringUtils.split(actuatorUserMatchPath, ",");
        if(path != null && path.length > 0){
          for (int i = 0; i < path.length; i++) {
            path[i] = path[i].trim();
          }
          return matcherRegistry.antMatchers(path)
                                .hasRole(ACTUATOR_USER_ROLE_NAME);
        }
      }
      return matcherRegistry;
    }
  }

}
