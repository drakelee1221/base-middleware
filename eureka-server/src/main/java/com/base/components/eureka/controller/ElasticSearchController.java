/*
 * Copyright (c) 2017.  mj.he800.com Inc. All rights reserved.
 */

package com.base.components.eureka.controller;

import com.base.components.eureka.dto.ElasticSearchIndexInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Configs Server Controller
 *
 * @author <a href="drakelee1221@gmail.com">LiGeng</a>
 * @version 1.0.0, 2017-08-24 13:52
 */
@Controller
@SuppressWarnings("all")
@ConfigurationProperties("base")
public class ElasticSearchController {
  private static final Logger logger = LoggerFactory.getLogger(ElasticSearchController.class);
  public static final String INDEX_INFO_URI = "/_cat/indices?format=json";
  private static ObjectMapper mapper = new ObjectMapper() {
    {
      this.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
      this.setSerializationInclusion(JsonInclude.Include.NON_NULL);
      this.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
      // 设置输入时忽略在JSON字符串中存在但Java对象实际没有的属性
      this.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
  };
  @Value("#{'${base.config-server-id}'.equals('${spring.application.name}')}")
  private boolean isOpenConfigServer;
  private String esMasterHttpAddressKey;
  private String esTransportHttpAddressKey;
  private String esClusterNameKey;
  private String esFileName;
  private List<String> esAllowDeleteIndex = Lists.newArrayList();
  @Autowired(required = false)
  private EnvironmentRepository repository;

  private volatile String esMasterHttpAddress;
  private volatile String esTransportHttpAddress;
  private volatile String esClusterName;
  private volatile boolean initialization;

  @RequestMapping(value = "/base/es/ftl", method = RequestMethod.GET)
  public String getEsFtl(HttpServletRequest request, Map<String, Object> model) throws IOException {
    List<ElasticSearchIndexInfo> infoList = Collections.emptyList();
    if (StringUtils.isNotBlank(esMasterHttpAddress)) {
      String infos = client(new HttpGet(esMasterHttpAddress + INDEX_INFO_URI), HttpStatus.OK.value());
      if (StringUtils.isNotBlank(infos)) {
        infoList = Lists.newArrayList();
        ArrayNode array = mapper.readValue(infos, ArrayNode.class);
        for (JsonNode jsonNode : array) {
          infoList.add(ElasticSearchIndexInfo.build(jsonNode));
        }
        Collections.sort(infoList);
      }
    }
    model.put("infoList", infoList);
    model.put("masterHttpAddress", esMasterHttpAddress);
    model.put("allowDeleteIndex", esAllowDeleteIndex);
    model.put("transportHttpAddress", esTransportHttpAddress);
    model.put("clusterName", esClusterName);
    if(StringUtils.isNotBlank(esMasterHttpAddress)){
      String masterInfo = client(new HttpGet(esMasterHttpAddress), HttpStatus.OK.value());
      model.put("masterInfo", masterInfo);
    }
    return "eureka/es";
  }

  @DeleteMapping(value = "/base/es/index/{index}")
  @ResponseBody
  public ResponseEntity delIndex(@PathVariable("index") String index, Map<String, Object> model) throws IOException {
    Map<String, Integer> count = Maps.newHashMap();
    count.put("count", 0);
    if (StringUtils.isNotBlank(index) && checkAllowDelete(index)) {
      String reStr = client(new HttpDelete(esMasterHttpAddress + "/" + index), HttpStatus.OK.value());
      if (StringUtils.isNotBlank(reStr)) {
        count.put("count", 1);
      }
    }
    return new ResponseEntity<>(count, HttpStatus.CREATED);
  }

  private String client(HttpUriRequest httpUriRequest, int checkStatus) {
    String respStr = null;
    try (CloseableHttpClient client = HttpClients.createDefault()) {
      HttpResponse response = client.execute(httpUriRequest);
      if (response.getStatusLine().getStatusCode() == checkStatus) {
        HttpEntity entity = response.getEntity();
        if (entity != null) {
          respStr = EntityUtils.toString(entity, "UTF-8");
        }
        EntityUtils.consume(entity);
      }
    } catch (Exception e) {
      logger.error("client > "+httpUriRequest.getURI().toString(), e);
    }
    return respStr;
  }

  private boolean checkAllowDelete(String index) {
    for (String alloweDeleteIndex : esAllowDeleteIndex) {
      if (index.contains(alloweDeleteIndex)) {
        return true;
      }
    }
    return false;
  }

  @PostConstruct
  public void init() {
    if (!initialization && isOpenConfigServer && repository != null && esMasterHttpAddress == null && esClusterName == null) {
      initialization = true;
      Thread tempThread = new Thread(new Runnable() {
        @Override
        public void run() {
          String appName = esFileName.substring(0, esFileName.indexOf("-"));
          String fileName = esFileName.substring(esFileName.indexOf("-") + 1, esFileName.length());
          Environment environment = repository.findOne(appName, fileName, null);
          if (environment != null && environment.getPropertySources() != null) {
            List<PropertySource> propertySources = environment.getPropertySources();
            for (int i = propertySources.size() - 1; i >= 0; i--) {
              PropertySource propertySource = propertySources.get(i);
              Object address = propertySource.getSource().get(esMasterHttpAddressKey);
              if (address != null) {
                esMasterHttpAddress = toHttp(address.toString());
              }
              Object clusterName = propertySource.getSource().get(esClusterNameKey);
              if (clusterName != null) {
                esClusterName = clusterName.toString();
              }
              Object transport = propertySource.getSource().get(esTransportHttpAddressKey);
              if(transport != null){
                esTransportHttpAddress = transport.toString();
              }
            }
          }
        }
      });
      tempThread.start();
    }
  }

  @PreDestroy
  public void destroy(){
    initialization = false;
  }

  private String toHttp(String url){
    return "http://" + url.replace("http://", "").replace("https://", "");
  }

  public List<String> getEsAllowDeleteIndex() {
    return esAllowDeleteIndex;
  }

  public void setEsMasterHttpAddressKey(String esMasterHttpAddressKey) {
    this.esMasterHttpAddressKey = esMasterHttpAddressKey;
  }

  public void setEsClusterNameKey(String esClusterNameKey) {
    this.esClusterNameKey = esClusterNameKey;
  }

  public void setEsFileName(String esFileName) {
    this.esFileName = esFileName;
  }

  public void setEsTransportHttpAddressKey(String esTransportHttpAddressKey) {
    this.esTransportHttpAddressKey = esTransportHttpAddressKey;
  }
}

