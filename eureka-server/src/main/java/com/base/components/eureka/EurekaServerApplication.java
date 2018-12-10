/*
 * Copyright (c) 2017.  mj.he800.com Inc. All rights reserved.
 */

package com.base.components.eureka;

import com.base.components.common.boot.BannerPrinter;
import com.base.components.common.boot.Profiles;
import com.base.components.common.boot.SpringBootApplicationRunner;
import com.base.components.common.configuration.CustomSystemPropertiesConfiguration;
import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

import java.io.File;


/**
 * EurekaServerApplication
 *
 * @author <a href="drakelee1221@gmail.com">LiGeng</a>
 * @version 1.0.0, 2018-02-02 11:00
 *
 */
@SpringBootApplication(scanBasePackages = {"com.base.components.*"})
@EnableEurekaServer
public class EurekaServerApplication {
  private static final String APP_SHORT_NAME = "eureka-server";
  private static final String APP_CLUSTER_NAME = "app.cluster.name";
  private static final String APP_CLUSTER_NAME_PREFIX = "peer";
  private static final String CONFIG_PATH_PREFIX_KEY = "base.config-path-prefix";

  @ConditionalOnExpression(value = "#{'${base.config-server-id}'.equals('${spring.application.name}')}")
  @EnableConfigServer
  public static class ConfigServerConfiguration{
  }

  @ConditionalOnProperty(name = "spring.boot.admin.server.enabled", havingValue = "true")
  @EnableAdminServer
  public static class SpringBootAdminServerConfiguration{
  }

  /**
   * The entry point of application.
   *
   * @param args the input arguments
   */
  public static void main(String[] args) {
    CustomSystemPropertiesConfiguration.INSTANCE
      .configApplicationShortName(APP_SHORT_NAME);
    checkConfigPathPrefix();
    checkClusterName();
    SpringBootApplicationRunner.run(EurekaServerApplication.class, BannerPrinter.create(), args);
  }

  private static void checkConfigPathPrefix(){
    String s = new File("").getAbsolutePath();
    if(s.endsWith(APP_SHORT_NAME)){
      System.setProperty(CONFIG_PATH_PREFIX_KEY, "../");
    }else{
      System.setProperty(CONFIG_PATH_PREFIX_KEY, "");
    }
  }

  private static void checkClusterName(){
    for (String s : Profiles.getProfilesSet()) {
      if(s != null && s.toLowerCase().startsWith(APP_CLUSTER_NAME_PREFIX)){
        System.setProperty(APP_CLUSTER_NAME, s);
        return;
      }
    }
  }
}
