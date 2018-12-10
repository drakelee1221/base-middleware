<#import "/spring.ftl" as spring />
<table class="table table-striped table-hover">
  <thead>
    <tr>
      <th>Cluster Name</th>
      <th>Master Info</th>
      <th>Transport HTTP Address</th>
    </tr>
  </thead>
  <tr>
    <td>${clusterName!}</td>
    <td>
      <pre class="white-pre">Address: ${masterHttpAddress!}</pre>
      <pre class="white-pre">${masterInfo!}</pre>
    </td>
    <td>${transportHttpAddress!}</td>
  </tr>
</table>
<table id="es_table" class="table table-striped table-hover">
  <thead>
  <tr>
    <th title="index name">index</th>
    <th title="health status">health</th>
    <th title="open/close status">status</th>
    <th title="number of primary shards">shardsPrimary</th>
    <th title="number of replica shards">shardsReplica</th>
    <th title="available docs">docsCount</th>
    <th title="deleted docs">docsDeleted</th>
    <th title="store size of primaries & replicas">storeSize</th>
    <th title="store size of primaries">primaryStoreSize</th>
  </tr>
  <thead>
  <tbody>
  <#list infoList as info>
  <tr id="index_${info.index}">
    <td>
      ${info.index}
      <#list allowDeleteIndex as allow>
        <#if info.index?contains('${allow}')>
          <a href="javascript:es.delIndex('${info.index}');"><b style="font-size:18px;color:red">×</b></a>
          <#break>
        </#if>
      </#list>
    </td>
    <td>${info.health}</td>
    <td>${info.status}</td>
    <td>${info.shardsPrimary}</td>
    <td>${info.shardsReplica}</td>
    <td>${info.docsCount}</td>
    <td>${info.docsDeleted}</td>
    <td>${info.storeSize}</td>
    <td>${info.primaryStoreSize}</td>
  </tr>
  </#list>
  </tbody>
</table>

<script type="text/javascript">
  var es = {
    opening:false,
    delIndex:function(index){
      if (!es.doing) {
        es.doing = true;
        var text = "是否要删除索引文档: ["+index+"] ？";
        if (confirm(text)) {
          $.ajax({
            url:"/base/es/index/"+index,
            method:"DELETE",
            dataType:"json",
            success:function(re){
              es.doing = false;
              if(re.count > 0){
                $("#index_"+index).remove();
              }
              alert("delete count = " + re.count);
            },
            error:function(e){
              es.doing = false;
              alert(e);
            }
          });
        }
      }
    }
  };
</script>