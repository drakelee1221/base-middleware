/*
 * Copyright (c) 2017.  mj.he800.com Inc. All rights reserved.
 */

package com.base.components.eureka.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.Serializable;

/**
 * ElasticSearchIndexInfo
 *
 * @author <a href="drakelee1221@gmail.com">LiGeng</a>
 * @version 1.0.0, 2018-01-03 16:44
 */
public class ElasticSearchIndexInfo implements Serializable,Comparable<ElasticSearchIndexInfo>{
  private static final long serialVersionUID = -2490334321100457016L;
  /**
   * current health status
   */
  private String health;
  /**
   * open/close status
   */
  private String status;
  /**
   * index name
   */
  private String index;
  /**
   * number of primary shards
   */
  private String shardsPrimary;

  /**
   * number of replica shards
   */
  private String shardsReplica;

  /**
   *  available docs
   */
  private String docsCount;
  /**
   *  deleted docs
   */
  private String docsDeleted;
  /**
   *  store size of primaries & replicas
   */
  private String storeSize;
  /**
   *  store size of primaries
   */
  private String primaryStoreSize;


  public String getHealth() {
    return health;
  }

  public void setHealth(String health) {
    this.health = health;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getIndex() {
    return index;
  }

  public void setIndex(String index) {
    this.index = index;
  }

  public String getShardsPrimary() {
    return shardsPrimary;
  }

  public void setShardsPrimary(String shardsPrimary) {
    this.shardsPrimary = shardsPrimary;
  }

  public String getShardsReplica() {
    return shardsReplica;
  }

  public void setShardsReplica(String shardsReplica) {
    this.shardsReplica = shardsReplica;
  }

  public String getDocsCount() {
    return docsCount;
  }

  public void setDocsCount(String docsCount) {
    this.docsCount = docsCount;
  }

  public String getDocsDeleted() {
    return docsDeleted;
  }

  public void setDocsDeleted(String docsDeleted) {
    this.docsDeleted = docsDeleted;
  }

  public String getStoreSize() {
    return storeSize;
  }

  public void setStoreSize(String storeSize) {
    this.storeSize = storeSize;
  }

  public String getPrimaryStoreSize() {
    return primaryStoreSize;
  }

  public void setPrimaryStoreSize(String primaryStoreSize) {
    this.primaryStoreSize = primaryStoreSize;
  }

  @Override
  public int compareTo(ElasticSearchIndexInfo o) {
    return o == null? 1 : (this.index == null ? -1 : (this.index.compareTo(o.getIndex())));
  }

  public static ElasticSearchIndexInfo build(JsonNode jsonNode){
    ElasticSearchIndexInfo info = null;
    if(jsonNode != null){
      info = new ElasticSearchIndexInfo();
      info.health = jsonNode.get("health").asText();
      info.status = jsonNode.get("status").asText();
      info.index = jsonNode.get("index").asText();
      info.shardsPrimary = jsonNode.get("pri").asText();
      info.shardsReplica = jsonNode.get("rep").asText();
      info.docsCount = jsonNode.get("docs.count").asText();
      info.docsDeleted = jsonNode.get("docs.deleted").asText();
      info.storeSize = jsonNode.get("store.size").asText();
      info.primaryStoreSize = jsonNode.get("pri.store.size").asText();
    }
    return info;
  }
}
