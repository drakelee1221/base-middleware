/*
 * Copyright (c) 2017.  mj.he800.com Inc. All rights reserved.
 */

package com.base.components.eureka.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.eureka.EurekaServerContextHolder;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 全局刷新Endpoint
 *
 * @author <a href="drakelee1221@gmail.com">LiGeng</a>
 * @version 1.0.0, 2017-08-24 13:52
 */
@Controller
public class MyRefreshController {

  private static final Logger logger = LoggerFactory.getLogger(MyRefreshController.class);

  @Value("${spring.application.name}")
  private String moduleEureka;

  @Value("${base.config-server-id}")
  private String moduleConfig;

  @Value("${actuator.user.name}")
  private String securityUserName;

  @Value("${actuator.user.password}")
  private String securityUserPassword;

  @Value("${base.endpoint.serverRefresh.threadPoolSize:20}")
  private int threadPoolSize = 20;

  private ExecutorService executorService;

  /**
   * @param params -
   * <p> modules        - Nullable - Str  - 需要刷新的服务ID多个用英文逗号隔开，为空时刷新全部服务
   * <p> doEureka       - Nullable - bool - 是否需要刷新Eureka注册中心，默认false
   * <p> doConfig       - Nullable - bool - 是否需要刷新Config配置中心，默认false
   * @return
   */
  @RequestMapping(value = "/base/toRefresh", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  @ResponseBody
  public ResponseEntity toRefresh(@RequestParam Map<String, String> params) {
    boolean doEureka = parseBoolean(params.get("doEureka"), false);
    boolean doConfig = parseBoolean(params.get("doConfig"), false);
    Map<String, Object> moduleMap = Maps.newConcurrentMap();
    String moduleStr = params.get("modules");
    List<Application> modules = EurekaServerContextHolder.getInstance().getServerContext()
                                                         .getRegistry().getSortedApplications();
    if(StringUtils.isNotBlank(moduleStr)){
      Set<String> names = Sets.newHashSet(StringUtils.split(moduleStr.toUpperCase(), ","));
      if(names.size() > 0){
        modules = modules.stream().filter(new Predicate<Application>() {
          @Override
          public boolean test(Application application) {
            return names.contains(application.getName());
          }
        }).collect(Collectors.toList());
      }
    }
    if(modules != null && !modules.isEmpty()){
      List<Callable<String>> callableList = Lists.newArrayList();
      CountDownLatchWrapper countDownLatchWrapper = new CountDownLatchWrapper();
      for (Application module : modules) {
        if(isNotToRefresh(module, doConfig, doEureka)){
          continue;
        }
        //多个模块
        refresh(module, callableList, moduleMap, countDownLatchWrapper);
      }
      CountDownLatch countDownLatch = new CountDownLatch(callableList.size());
      countDownLatchWrapper.setCountDownLatch(countDownLatch);
      calls(countDownLatch, callableList);
    }
    return new ResponseEntity<>(moduleMap, HttpStatus.CREATED);
  }

  private void refresh(Application module, List<Callable<String>> callableList,
                       Map<String, Object> moduleMap, CountDownLatchWrapper countDownLatchWrapper){
    List<InstanceInfo> instances = module.getInstances();
    Map<String, Object> clients = Maps.newConcurrentMap();
    if(instances != null && !instances.isEmpty()){
      for (InstanceInfo instance : instances) {
        //单个模块，多个实例
        callableList.add(new Callable<String>() {
          @Override
          public String call()throws Exception{
            String re = "status > error";
            try {
              re =  doPost(
            addSchemeAfter(
                  instance.getHomePageUrl(), securityUserName + ":" + securityUserPassword + "@") + "/refresh");
              clients.put(instance.getId(), mapper.readValue(re, ArrayList.class));
            } catch (Exception e) {
              clients.put(instance.getId(), Lists.newArrayList(re));
            } finally {
              if(re.contains("status > error")){
                re = "ServerRefresh Fail    -> " + instance.getHomePageUrl() + ", " + re;
              }else{
                re = "ServerRefresh Success -> " + instance.getHomePageUrl() + ", " + re;
              }
              countDownLatchWrapper.getCountDownLatch().countDown();
            }
            return re;
          }
        });
      }
      moduleMap.put(module.getName(), clients);
    }
  }


  private void calls(CountDownLatch countDownLatch, List<Callable<String>> callableList){
    List<Future<String>> futures = Lists.newArrayList();
    for (Callable<String> call : callableList) {
      futures.add(executorService.submit(call));
    }
    try {
      countDownLatch.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    for (Future<String> future : futures) {
      try {
        String log = future.get();
        if(log != null && log.startsWith("ServerRefresh Success")){
          logger.info(future.get());
        }else{
          logger.error(future.get());
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private String doPost(String url){
    String re = "[\"status > error\"]";
    try (CloseableHttpClient hc = HttpClients.createDefault()) {
      HttpPost httpPost = new HttpPost(url);
      HttpResponse response = hc.execute(httpPost);
      if(response != null && response.getStatusLine() != null){
        int code = response.getStatusLine().getStatusCode();
        re = "[\"status > "+ code +"\"]";
        if( code == 200 ){
          HttpEntity entity = response.getEntity();
          if(entity != null){
            re = EntityUtils.toString(entity, "UTF-8");
          }
          EntityUtils.consume(entity);
        }
      }
    }catch (Exception e){
      re = "[\"status > error > "+ e.getMessage()+"\"]";
    }
    return re;
  }

  /**
   * 在uri地址的Scheme后面增加字符
   * @param uri
   * @param addStr
   * @return
   */
  private String addSchemeAfter(String uri, String addStr){
    if(uri == null || !uri.contains("//")){
      return "";
    }
    uri = uri.substring(0, uri.indexOf("//")) + "//" + addStr + uri.substring(uri.indexOf("//") + 2, uri.length());
    return uri.endsWith("/") ? uri.substring(0, uri.length()-1): uri;
  }

  private boolean parseBoolean(String src, boolean defaultValue){
    if(StringUtils.isBlank(src)){
     return defaultValue;
    }
    return "true".equalsIgnoreCase(src);
  }

  private boolean isNotToRefresh(Application module, boolean doConfig, boolean doEureka) {
    //不刷新配置中心
    return (!doConfig && moduleConfig.equalsIgnoreCase(module.getName()))
        || (!doEureka && moduleEureka.equalsIgnoreCase(module.getName()));
  }


  @PostConstruct
  public void init(){
    executorService = Executors.newFixedThreadPool(threadPoolSize);
  }

  @PreDestroy
  public void destroy() {
    if (executorService != null && !executorService.isTerminated()) {
      executorService.shutdown();
      executorService = null;
    }
  }

  @Override
  protected void finalize() throws Throwable {
    super.finalize();
    destroy();
  }

  private static ObjectMapper mapper = new ObjectMapper() {
    private static final long serialVersionUID = 1L;

    {
      this.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
      this.setSerializationInclusion(JsonInclude.Include.NON_NULL);
      this.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
      // 设置输入时忽略在JSON字符串中存在但Java对象实际没有的属性
      this.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
  };

  class CountDownLatchWrapper implements Serializable{
    private static final long serialVersionUID = -8605552341366365138L;
    CountDownLatch countDownLatch;

    CountDownLatch getCountDownLatch() {
      return countDownLatch;
    }

    void setCountDownLatch(CountDownLatch countDownLatch) {
      this.countDownLatch = countDownLatch;
    }
  }

}
