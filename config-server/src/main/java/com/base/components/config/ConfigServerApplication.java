/*
 * Copyright (c) 2017.  mj.he800.com Inc. All rights reserved.
 */

package com.base.components.config;

import com.base.components.common.boot.BannerPrinter;
import com.base.components.common.boot.SpringBootApplicationRunner;
import com.base.components.common.configuration.CustomSystemPropertiesConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

import java.io.File;

/**
 * Distributed/versioned configuration
 *
 * @author <a href="drakelee1221@gmail.com">LiGeng</a>
 * @version 1.0.0, 2017-07-03
 */

@EnableEurekaClient
@SpringBootApplication(scanBasePackages = {"com.base.components.*"})
@EnableConfigServer
public class ConfigServerApplication {
  private static final String APP_SHORT_NAME = "config-server";
  private static final String CONFIG_PATH_PREFIX_KEY = "base.config-path-prefix";

  public static void main(String[] args) {
    CustomSystemPropertiesConfiguration.INSTANCE
      .configApplicationServerPort(18888)
      .configApplicationShortName(APP_SHORT_NAME);
    checkConfigPathPrefix();
    SpringBootApplicationRunner.run(ConfigServerApplication.class, BannerPrinter.create(), args);
  }

  private static void checkConfigPathPrefix(){
    String s = new File("").getAbsolutePath();
    if(s.endsWith(APP_SHORT_NAME)){
      System.setProperty(CONFIG_PATH_PREFIX_KEY, "../");
    }else{
      System.setProperty(CONFIG_PATH_PREFIX_KEY, "");
    }
  }
}
