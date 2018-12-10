package com.base.components.config.configurations;

import com.google.common.collect.Sets;
import org.springframework.cloud.config.server.environment.NativeEnvironmentProperties;
import org.springframework.cloud.config.server.environment.NativeEnvironmentRepository;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.File;
import java.util.Set;

/**
 * WildcardNativeEnvironmentRepository
 *
 * @author <a href="drakelee1221@gmail.com">LiGeng</a>
 * @version v1.0.0
 * @date 2018-09-06 13:37
 */
public class WildcardNativeEnvironmentRepository extends NativeEnvironmentRepository {

  private static final String WILDCARD = "*";

  private static final String SPLIT = "/";

  private static final String WILDCARD_SUFFIX = WILDCARD + SPLIT;

  private static final String FILE_PREFIX = "file:";

  private ResourceLoader resourceLoader = new DefaultResourceLoader();

  public WildcardNativeEnvironmentRepository(ConfigurableEnvironment environment,
                                             NativeEnvironmentProperties properties) {
    super(environment, properties);
  }

  @Override
  public Locations getLocations(String application, String profile, String label) {
    Set<String> locations = Sets.newLinkedHashSet();
    for (String location : getSearchLocations()) {
      recursiveCheckDirectoryPath(locations, location);
    }
    setSearchLocations(locations.toArray(new String[0]));
    return super.getLocations(application, profile, label);
  }

  private void recursiveCheckDirectoryPath(Set<String> locations, String location){
    location = location.endsWith(SPLIT) ? location : location + SPLIT;
    if(location.endsWith(WILDCARD) || location.endsWith(WILDCARD_SUFFIX)) {
      location = location.replace(WILDCARD_SUFFIX, "").replace(WILDCARD, "");
    }
    Resource resource = resourceLoader.getResource(location);
    if(resource.exists()){
      locations.add(location);
      try {
        File[] files = resource.getFile().listFiles();
        if(files != null){
          for (File file : files) {
            if(file.isDirectory()){
              recursiveCheckDirectoryPath(locations, FILE_PREFIX + file.toString().replace("\\", SPLIT));
            }
          }
        }
      } catch (Exception ignore) {
      }
    }
  }
}
