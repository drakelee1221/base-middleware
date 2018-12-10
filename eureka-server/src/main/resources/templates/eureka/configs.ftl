<#import "/spring.ftl" as spring />
<h4>
  <label style="width:200px;text-align:center">Search Path：</label>
  <a href="javascript:" onclick="$(this).replaceWith('${configSearchPath}')">………</a>
</h4>
<#if !nativeConfig>
  <h4>
    <label style="width:200px;text-align:center">Git Repository：</label>
    <a href="javascript:" onclick="$(this).replaceWith('${gitUri}')">………</a>
  </h4>
</#if>
<h4>
  <label style="width:200px;text-align:center">Environment Label：</label>
  <#if nativeConfig>
    native
  </#if>
  <#if !nativeConfig>
    <select id="config_label" onchange="$('#config_div').html('');base.loadConfig(this.value);">
      <#list configEnvironments as configEnv>
        <option value="${configEnv}" <#if configLabel == configEnv>selected</#if> >${configEnv}</option>
      </#list>
    </select>
  </#if>
</h4>
<#if nativeConfig>
<#--<h4>-->
  <#--<label style="width:200px;text-align:center">Project Label：</label>-->
  <#--<select id="config_label" onchange="$('#config_div').html('');base.loadConfig(this.value);">-->
      <#--<#list configEnvironments as configEnv>-->
        <#--<option value="${configEnv}" <#if configLabel == configEnv>selected</#if> >${configEnv}</option>-->
      <#--</#list>-->
  <#--</select>-->
<#--</h4>-->
</#if>
<table id="config_table" class="table table-striped table-hover">
  <thead>
  <tr>
    <th>dir</th>
    <th>files</th>
    <th style="width:50%">property</th>
  </tr>
  <thead>
  <tbody>
  <#--<#list configDirMap?keys as key>-->
  <#assign  groupNameKeys=configDirMap?keys/>
  <#list groupNameKeys as groupNameKey>
  <tr>
    <#--<td><a href="javascript:" onclick="configServer.expandConfigByGroup('${configDirMap["${groupNameKey}"]}');">${groupNameKey}</a></td>-->
      <td><a href="javascript:" onclick="configServer.expandConfigByGroup('${groupNameKey}');"><h4>- ${groupNameKey} -</h4></a></td>
    <#--<td>-->
      <#--<#list configDirMap[key] as configFile>-->
        <#--&lt;#&ndash;<div><a href="javascript:" onclick="configServer.openConfig('${configFile}', this);" target="_blank"><b>${configFile}</b></a></div>&ndash;&gt;-->
      <#--</#list>-->
    <#--</td>-->
    <td>
    </td>
    <td style="font-weight:bold">
    </td>
  </tr>
  <#assign  pathMap =configDirMap[groupNameKey]/>
  <#assign  pathKeys =configDirMap[groupNameKey]?keys/>
  <#list pathKeys as pathKey>
    <tr class="${groupNameKey}" style="display: none;">
      <td>${pathKey}</td>
      <td>
      <#list pathMap[pathKey] as configFile>
      <div><a href="javascript:" onclick="configServer.openConfig('${configFile}', this);" target="_blank"><b>${configFile}</b></a></div>
      </#list>
      </td>
      <td style="font-weight:bold">
      </td>
    </tr>
  </#list>
</#list>
  </tbody>
</table>

<script type="text/javascript">
  var configServer = {
    opening:false,
    remove:function(e){
      $(e).parents(".content").remove();
    },
    openConfig:function(fileName, e){
      if(!configServer.opening){
        configServer.opening = true;
        var td = $(e).parents("td").next();
        $.get("/base/config", {fileName: fileName, label: $("#config_label").val()}, function(re){
          configServer.opening = false;
          if(re){
            for(var name in re){
              var div = td.find("div[class='"+name+"']");
              var pr = re[name];
              if(div.length === 0){
                var html = "<div class='content'><h4><label>"+name+"&nbsp;&nbsp;<a href='javascript:' onclick='configServer.remove(this)'>×</a></label></h4><div class='"+name+"'>";
                for(var key in pr){
                  html += "<div>"+key+" : <span style='color:#5fa134'>"+pr[key]+"</span></div>";
                }
                td.append(html + "</div></div>");
              }else{
                div.html("");
                for(var key in pr){
                  div.append("<div>"+key+" : <span style='color:#5fa134'>"+pr[key]+"</span></div>");
                }
              }
              td.append("<br/><br/>");
            }
          }
        }, "json");
      }
    },
    expandConfigByGroup:function (groupName) {
      $("#config_table ."+groupName).toggle();
    }
  };
</script>