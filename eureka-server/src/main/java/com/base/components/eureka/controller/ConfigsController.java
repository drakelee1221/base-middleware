/*
 * Copyright (c) 2017.  mj.he800.com Inc. All rights reserved.
 */

package com.base.components.eureka.controller;

import com.base.components.common.boot.Profiles;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.env.EnvironmentEndpointProperties;
import org.springframework.boot.actuate.endpoint.Sanitizer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.cloud.config.server.environment.SearchPathLocator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Configs Server Controller
 *
 * @author <a href="drakelee1221@gmail.com">LiGeng</a>
 * @version 1.0.0, 2017-08-24 13:52
 */
@Controller
@SuppressWarnings("unchecked")
@ConfigurationProperties("base.config-server")
public class ConfigsController {

  public static final String GIT_CONFIG_REPO_PREFIX = "config-repo-";

  private static final Logger logger = LoggerFactory.getLogger(ConfigsController.class);

  @Value("${base.config-name-separator:-}")
  private String configNameSeparator;
  @Autowired(required = false)
  private EnvironmentRepository repository;

  private boolean isProduction;

  private Sanitizer sanitizer = new Sanitizer();

  @Autowired
  private EnvironmentEndpointProperties properties;

  @Value("#{'${base.config-server-id}'.equals('${spring.application.name}')}")
  private boolean isOpenConfigServer;

  @Autowired(required = false)
  private SearchPathLocator searchPathLocator;

  @Value("${base.config-native:false}")
  private Boolean nativeConfig;

  @Value("${spring.cloud.config.server.default-label}")
  private String configLabel;

  @Value("#{${base.config-native:false}?'${system.config.native.path:${base.config-search-native-path}}':'${base.config-search-path}'}")
  private String configSearchPath;

  @Value("${spring.cloud.config.server.git.uri:}")
  private String gitUri;

  private Set<String> configEnvironments;

  @PostConstruct
  public void init() {
    isProduction = Profiles.contains(Profiles.PROD);
    sanitizer.setKeysToSanitize(properties.getKeysToSanitize());
  }

  @RequestMapping(value = "/base/config/ftl", method = RequestMethod.GET)
  public String getConfigFtl(HttpServletRequest request, Map<String, Object> model) {
    String label = request.getParameter("label");
    label = configEnvironments.contains(label) && Boolean.FALSE.equals(nativeConfig)? label : configLabel;
    populateConfigs(model, label);
    model.put("configLabel", configEnvironments.contains(label) ? label : configLabel);
    model.put("configEnvironments", configEnvironments);
    model.put("nativeConfig", nativeConfig);
    model.put("configSearchPath", configSearchPath);
    model.put("gitUri", gitUri);
    return "eureka/configs";
  }

  /**
   * 查看配置文件
   *
   * @param params -
   * <p> fileName        - Nonnull - Str  - 配置文件名，多个以英文逗号隔开，如文件名：xxx-zuul-dev.yml 或 profile名zuul-dev
   *
   * @return ResponseEntity
   */
  @RequestMapping(value = "/base/config", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
  @ResponseBody
  public ResponseEntity getConfigs(@RequestParam Map<String, String> params) {
    Map<String, Map> property = Maps.newLinkedHashMap();
    if (isOpenConfigServer) {
      String fileName = params.get("fileName");
      //<editor-fold desc="重构configNamePrefix的值,通过文件的分割符的第一个作为应用名称">
      int index = fileName.indexOf(configNameSeparator);
      if(index > 0){
        String configNamePrefix = fileName.substring(0, index);
        //</editor-fold>
        String profiles = getProfiles(configNamePrefix, fileName);
        if (StringUtils.isNotBlank(profiles) && repository != null) {
          String label = params.get("label");
          if (nativeConfig) {
            label = null;
          }
          Environment environment = repository
            .findOne(configNamePrefix, profiles, configEnvironments.contains(label) ? label : null);
          if (environment != null && environment.getPropertySources() != null) {
            List<PropertySource> propertySources = environment.getPropertySources();
            for (int i = propertySources.size() - 1; i >= 0; i--) {
              PropertySource propertySource = propertySources.get(i);
              Map map = Maps.newLinkedHashMap();
              property.put(getFileName(propertySource.getName()), map);
              for (Map.Entry<?, ?> entry : propertySource.getSource().entrySet()) {
                String k = entry.getKey().toString();
                if (isProduction) {
                  map.put(k, sanitizer.sanitize(k, entry.getValue()));
                } else {
                  map.put(k, entry.getValue());
                }
              }
            }
          }
        }
      }
    }
    return new ResponseEntity<>(property, HttpStatus.OK);
  }

  private String getFileName(String uri) {
    String[] arr = StringUtils.split(uri, "/");
    if (arr == null || arr.length == 0) {
      return "";
    }
    return arr[arr.length - 1];
  }

  private String getProfiles(String configNamePrefix, String fileName) {
    StringBuilder profiles = new StringBuilder();
    if (StringUtils.isNotBlank(fileName)) {
      String[] arr = StringUtils.split(fileName, ",");
      for (String s : arr) {
        int b = s.startsWith(configNamePrefix + configNameSeparator)
                ? (configNamePrefix + configNameSeparator).length()
                : 0;
        int hasSuffix = s.indexOf(".");
        int e = hasSuffix >= 0 ? hasSuffix : s.length();
        profiles.append(s.substring(b, e)).append(",");
      }
    }
    return profiles.length() > 0 ? profiles.substring(0, profiles.length() - 1) : profiles.toString();
  }

  private void populateConfigs(Map<String, Object> model, String label) {
    if (isOpenConfigServer && searchPathLocator != null && StringUtils.isNotBlank(configSearchPath)) {
      Map<String, Map<String, List<String>>> returnGroupMap = Maps.newLinkedHashMap();
      try {
        SearchPathLocator.Locations locations = searchPathLocator.getLocations(null, null, label);

        // 根据一级目录进行分组
        String[] locationsArray = locations.getLocations();
        Map<String, List<String>> groupMap = Arrays.stream(locationsArray).filter(s -> s.contains(configSearchPath)).collect(Collectors.groupingBy(path -> {
          String newPath = path.replace("file:", "");
          String name = newPath.substring(newPath.indexOf(configSearchPath) + configSearchPath.length());
          String newName = name.endsWith("/") ? name.substring(0, name.length() - 1):name;
          int groupIndex = newName.indexOf("/",1);

          String groupName = groupIndex != -1 ? newName.substring(1, groupIndex):newName;
          groupName = groupName.startsWith("/")?groupName.substring(1, name.length()-1):groupName;
          return groupName.startsWith(GIT_CONFIG_REPO_PREFIX) ? GIT_CONFIG_REPO_PREFIX : groupName;
        }));

        groupMap.remove(GIT_CONFIG_REPO_PREFIX);

        // 通过组下的数量对组名进行排序
        List<String> groupNames = Lists.newArrayList(groupMap.keySet());
        Collections.sort(groupNames,(oldGroupName,newGroupName) -> {
          int oldGroupNum = groupMap.get(oldGroupName).size();
          int newGroupNum = groupMap.get(newGroupName).size();
          return oldGroupNum-newGroupNum;
        });

        // 通过组名获取其下的配置文件
        for (String groupName : groupNames) {
          Map<String, List<String>> configDirMap = Maps.newLinkedHashMap();
          List<String> locationList = groupMap.get(groupName);
          for (String location : locationList) {
            String path = location.replace("file:", "");
            String name = path.substring(path.indexOf(configSearchPath) + configSearchPath.length());
            String newName = name.endsWith("/") ? name.substring(0, name.length() - 1):name;
            File dir = new File(path);
            List<String> fileList = findFile(dir);
            if (fileList != null && fileList.size() > 0) {
              configDirMap.put(newName, fileList);
            }
          }
          if(configDirMap.size()>0){
            returnGroupMap.put(groupName,configDirMap);
          }
        }
      } catch (Exception e) {
        logger.error("", e);
      }
      model.put("isOpenConfigServer", isOpenConfigServer);
      model.put("configDirMap", returnGroupMap);
    }
  }

  private List<String> findFile(File parent) {
    List<String> fileNames = Lists.newArrayList();
    if (parent != null && parent.exists()) {
      File[] files = parent.listFiles();
      if (files != null) {
        for (File file : files) {
          if (!file.isDirectory()) {
            fileNames.add(file.getName());
          }
        }
      }
    }
    Collections.sort(fileNames);
    return fileNames;
  }

  public Set<String> getConfigEnvironments() {
    return configEnvironments;
  }

  public void setConfigEnvironments(Set<String> configEnvironments) {
    this.configEnvironments = configEnvironments;
  }

}
