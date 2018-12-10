package com.apidocs.configurarion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * MyApplicationRunner
 *
 * @author <a href="morse.jiang@foxmail.com">JiangWen</a>
 * @version 1.0.0, 2018/6/7 0007 14:43
 */
@Component
public class MyApplicationRunner implements ApplicationRunner {

  @Value("${server.port:9999}")
  private Integer port;

  @Override
  public void run(ApplicationArguments args) throws Exception {
    System.out.println("点击打开：http://localhost:" + port);
  }
}
