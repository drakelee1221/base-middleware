/*
 * Copyright (c) 2017.  mj.he800.com Inc. All rights reserved.
 */

package com.base.components.eureka.controller;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.netflix.appinfo.AmazonInfo;
import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.eureka.EurekaServerContext;
import com.netflix.eureka.EurekaServerContextHolder;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import com.netflix.eureka.resources.StatusResource;
import com.netflix.eureka.util.StatusInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.eureka.server.EurekaController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CustomEurekaController
 *
 * @author <a href="drakelee1221@gmail.com">LiGeng</a>
 * @version 1.0.0, 2017-12-08 13:56
 *
 */
@Controller
@RequestMapping("${eureka.dashboard.path:/}base")
public class CustomEurekaController extends EurekaController {
  public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

  @Value("${actuator.user.name:}")
  private String securityUserName;

  @Value("${actuator.user.password:}")
  private String securityUserPassword;

  @Value("#{'${base.config-server-id}'.equals('${spring.application.name}')}")
  private boolean isOpenConfigServer;

  @Value("${spring.boot.admin.server.enabled:false}")
  private boolean enabledAdminMonitor;

  @SuppressWarnings("all")
  public CustomEurekaController(ApplicationInfoManager applicationInfoManager) {
    super(applicationInfoManager);
  }

  @Override
  @RequestMapping(method = RequestMethod.GET)
  public String status(HttpServletRequest request, Map<String, Object> model) {
    populateBase(request, model);
    populateApps(model);
    StatusInfo statusInfo;
    try {
      statusInfo = new StatusResource().getStatusInfo();
    }
    catch (Exception e) {
      statusInfo = StatusInfo.Builder.newBuilder().isHealthy(false).build();
    }
    model.put("statusInfo", statusInfo);
    populateInstanceInfo(model, statusInfo);
    filterReplicas(model, statusInfo);
    model.put("isOpenConfigServer", isOpenConfigServer);
    model.put("enabledAdminMonitor", enabledAdminMonitor);
    return "eureka/base_status";
  }

  @RequestMapping(value = "/lastn", method = RequestMethod.GET)
  @Override
  public String lastn(HttpServletRequest request, Map<String, Object> model) {
    model.put("enabledAdminMonitor", enabledAdminMonitor);
    return super.lastn(request, model);
  }

  @RequestMapping(value = "/toRestart", method = RequestMethod.POST)
  @ResponseBody
  public ResponseEntity toRestart(HttpServletRequest request, @RequestParam Map<String, String> model) {
    String host = model.get("host");
    String result = sendCommand(host, "restart");
    return new ResponseEntity<>(result, HttpStatus.OK);
  }


  @RequestMapping(value = "/toShutdown", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity toShutdown(HttpServletRequest request, @RequestParam Map<String, String> model) {
		String host = model.get("host");
    String result = sendCommand(host, "shutdown");
		return new ResponseEntity<>(result, HttpStatus.OK);
	}


	private String sendCommand(String host, String command){
    String result = "host is null";
    if(StringUtils.isNotBlank(host)){

      host = host.replace("/info", "");
      String protocol = host.startsWith("http://") ? "http://" : "https://" ;
      host = host.replace("http://", "").replace("https://", "");
      result = host + " >>> ";
      if(StringUtils.isNoneBlank(securityUserName, securityUserPassword)){
        host = securityUserName + ":" + securityUserPassword + "@" + host;
      }
      String authHost = protocol + host + (host.endsWith("/") ? "":"/") + command;
      try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
        HttpPost post = new HttpPost(authHost);
        CloseableHttpResponse response = httpclient.execute(post);
        result += EntityUtils.toString(response.getEntity());
      }catch (Exception e){
        e.printStackTrace();
        result += Throwables.getRootCause(e).getMessage();
      }
    }
    return result;
  }







  private void populateInstanceInfo(Map<String, Object> model, StatusInfo statusInfo) {
    InstanceInfo instanceInfo = statusInfo.getInstanceInfo();
    Map<String, String> instanceMap = new HashMap<>();
    instanceMap.put("ipAddr", instanceInfo.getIPAddr());
    instanceMap.put("status", instanceInfo.getStatus().toString());
    if (instanceInfo.getDataCenterInfo().getName() == DataCenterInfo.Name.Amazon) {
      AmazonInfo info = (AmazonInfo) instanceInfo.getDataCenterInfo();
      instanceMap.put("availability-zone",
                      info.get(AmazonInfo.MetaDataKey.availabilityZone));
      instanceMap.put("public-ipv4", info.get(AmazonInfo.MetaDataKey.publicIpv4));
      instanceMap.put("instance-id", info.get(AmazonInfo.MetaDataKey.instanceId));
      instanceMap.put("public-hostname",
                      info.get(AmazonInfo.MetaDataKey.publicHostname));
      instanceMap.put("ami-id", info.get(AmazonInfo.MetaDataKey.amiId));
      instanceMap.put("instance-type",
                      info.get(AmazonInfo.MetaDataKey.instanceType));
    }
    model.put("instanceInfo", instanceMap);
  }

  private void populateApps(Map<String, Object> model) {
    List<Application> sortedApplications = getRegistry().getSortedApplications();
    ArrayList<Map<String, Object>> apps = new ArrayList<>();
    for (Application app : sortedApplications) {
      LinkedHashMap<String, Object> appData = new LinkedHashMap<>();
      apps.add(appData);
      appData.put("name", app.getName());
      Map<String, Integer> amiCounts = new HashMap<>();
      Map<InstanceInfo.InstanceStatus, List<Map<String, String>>> instancesByStatus = new HashMap<>();
      Map<String, Integer> zoneCounts = new HashMap<>();
      app.getInstances().sort(Comparator.comparingLong(InstanceInfo::getLastUpdatedTimestamp));
      for (InstanceInfo info : app.getInstances()) {
        String id = info.getId();
        String url = info.getStatusPageUrl();
        InstanceInfo.InstanceStatus status = info.getStatus();
        String ami = "n/a";
        String zone = "";
        if (info.getDataCenterInfo().getName() == DataCenterInfo.Name.Amazon) {
          AmazonInfo dcInfo = (AmazonInfo) info.getDataCenterInfo();
          ami = dcInfo.get(AmazonInfo.MetaDataKey.amiId);
          zone = dcInfo.get(AmazonInfo.MetaDataKey.availabilityZone);
        }
        Integer count = amiCounts.get(ami);
        if (count != null) {
          amiCounts.put(ami, count + 1);
        }
        else {
          amiCounts.put(ami, 1);
        }
        count = zoneCounts.get(zone);
        if (count != null) {
          zoneCounts.put(zone, count + 1);
        }
        else {
          zoneCounts.put(zone, 1);
        }
        List<Map<String, String>> list = instancesByStatus.get(status);
        if (list == null) {
          list = new ArrayList<>();
          instancesByStatus.put(status, list);
        }
        Map<String, String> row = new HashMap<String, String>();
        row.put("id", id);
        row.put("url", url);
        row.put("host", info.getHomePageUrl());
        row.put("status", String.valueOf(info.getStatus()));
        row.put("turbineGroup", getMetadataValue(info.getMetadata(),"turbine-group"));
        row.put("fuzzy.boot.path", getMetadataValue(info.getMetadata(),"fuzzy.boot.path"));
        row.put("isHystrixModule", String.valueOf(StringUtils.isNotBlank(getMetadataValue(info.getMetadata(),"turbine-cluster"))));
        row.put("lastUpdateTime", new DateTime(info.getLastUpdatedTimestamp()).toString(DATE_TIME_PATTERN));
        row.put("groupTagFilterRuleEnable", getMetadataValue(info.getMetadata(),"group-tag-filter-rule-enable"));
        row.put("groupTagFilterRuleTags", getMetadataValue(info.getMetadata(),"group-tag-filter-rule-tags"));
        row.put("groupTagFilterRuleNoTagsLimit", getMetadataValue(info.getMetadata(),"group-tag-filter-rule-no-tags-limit"));
        list.add(row);
      }
      appData.put("amiCounts", amiCounts.entrySet());
      appData.put("zoneCounts", zoneCounts.entrySet());
      ArrayList<Map<String, Object>> instanceInfos = new ArrayList<>();
      appData.put("instanceInfos", instanceInfos);
      for (Iterator<Map.Entry<InstanceInfo.InstanceStatus, List<Map<String, String>>>> iter = instancesByStatus
        .entrySet().iterator(); iter.hasNext();) {
        Map.Entry<InstanceInfo.InstanceStatus, List<Map<String, String>>> entry = iter
          .next();
        List<Map<String, String>> value = entry.getValue();
        InstanceInfo.InstanceStatus status = entry.getKey();
        LinkedHashMap<String, Object> instanceData = new LinkedHashMap<>();
        instanceInfos.add(instanceData);
        instanceData.put("status", entry.getKey());
        ArrayList<Map<String, Object>> instances = new ArrayList<>();
        instanceData.put("instances", instances);
        instanceData.put("isNotUp", status != InstanceInfo.InstanceStatus.UP);


				/*
				 * if(status != InstanceInfo.InstanceStatus.UP){
				 * buf.append("<font color=red size=+1><b>"); }
				 * buf.append("<b>").append(status
				 * .name()).append("</b> (").append(value.size()).append(") - ");
				 * if(status != InstanceInfo.InstanceStatus.UP){
				 * buf.append("</font></b>"); }
				 */

        for (Map<String, String> p : value) {
          LinkedHashMap<String, Object> instance = new LinkedHashMap<>();
          instances.add(instance);
          instance.put("id", p.get("id"));
          String url = p.get("url");
          instance.put("url", url);
          instance.put("host", p.get("host"));
          instance.put("status", p.get("status"));
          boolean isHref = url != null && url.startsWith("http");
          instance.put("isHref", isHref);
          instance.put("lastUpdateTime", p.get("lastUpdateTime"));
          instance.put("isHystrixModule", Boolean.valueOf(p.get("isHystrixModule")));
          String turbineGroup = p.get("turbineGroup");
          if(StringUtils.isNotBlank(turbineGroup)){
            instance.put("turbineGroup", Lists.newArrayList(StringUtils.split(turbineGroup, ",")));
          }
          instance.put("groupTagFilterRuleTags",
                       Lists.newArrayList(StringUtils.split(p.get("groupTagFilterRuleTags"), ",")));
          instance.put("groupTagFilterRuleNoTagsLimit", p.get("groupTagFilterRuleNoTagsLimit"));
          instance.put("groupTagFilterRuleEnable", p.get("groupTagFilterRuleEnable"));
          instance.put("fuzzy.boot.path", p.getOrDefault("fuzzy.boot.path", ""));
          /*
					 * String id = p.first(); String url = p.second(); if(url != null &&
					 * url.startsWith("http")){
					 * buf.append("<a href=\"").append(url).append("\">"); }else { url =
					 * null; } buf.append(id); if(url != null){ buf.append("</a>"); }
					 * buf.append(", ");
					 */
        }
      }
      // out.println("<td>" + buf.toString() + "</td></tr>");
    }
    model.put("apps", apps);
  }

  private PeerAwareInstanceRegistry getRegistry() {
    return getServerContext().getRegistry();
  }

  private EurekaServerContext getServerContext() {
    return EurekaServerContextHolder.getInstance().getServerContext();
  }


  private String getMetadataValue(Map<String, String> metadata, String key){
    if(metadata != null){
      String val = metadata.get(key);
      if(val != null){
        return val;
      }
    }
    return "";
  }

}
