<#import "/spring.ftl" as spring />
<!doctype html>
<!--[if lt IE 7]>      <html class="no-js lt-ie9 lt-ie8 lt-ie7"> <![endif]-->
<!--[if IE 7]>         <html class="no-js lt-ie9 lt-ie8"> <![endif]-->
<!--[if IE 8]>         <html class="no-js lt-ie9"> <![endif]-->
<!--[if gt IE 8]><!-->
<html class="no-js"> <!--<![endif]-->
<head>
  <base href="<@spring.url basePath/>">
  <meta charset="utf-8">
  <meta http-equiv="X-UA-Compatible" content="IE=edge">
  <title>Eureka</title>
  <meta name="description" content="">
  <meta name="viewport" content="width=device-width">

  <link rel="stylesheet" href="eureka/css/wro.css">

</head>

<body id="one">
<#include "header.ftl">
<div class="container-fluid xd-container">
<#include "navbar.ftl">
  <h1>Instances currently registered with Eureka</h1>
  <table id='instances' class="table table-striped table-hover">
    <thead>
    <tr>
      <th>Application</th>
      <th>AMIs</th>
      <th>Availability Zones</th>
      <th>Status</th>
      <th>
        <span style="vertical-align:bottom">Servers Instances</span>
        <span style="vertical-align:bottom;margin-left:20px">
          ( <input type="checkbox" style="vertical-align:top" id="show_detail" checked
                 onclick="base.showDetail(this)" />
          <label for="show_detail" style="margin-bottom:0">Show Details</label> )
        </span>
      </th>
    </tr>
    </thead>
    <tbody>
    <#if apps?has_content>
      <#list apps as app>
      <tr>
        <td><b>${app.name}</b></td>
        <td>
          <#list app.amiCounts as amiCount>
            <b>${amiCount.key}</b> (${amiCount.value})<#if amiCount_has_next>,</#if>
          </#list>
        </td>
        <td>
          <#list app.zoneCounts as zoneCount>
            <b>${zoneCount.key}</b> (${zoneCount.value})<#if zoneCount_has_next>,</#if>
          </#list>
        </td>
        <td>
          <#list app.instanceInfos as instanceInfo>
            <#if instanceInfo.isNotUp>
            <font color=red size=+1><b>
            </#if>
            <b>${instanceInfo.status}</b> (${instanceInfo.instances?size})
            <#if instanceInfo.isNotUp>
            </b></font>
            </#if>
          </#list>
        </td>
        <td>
          <#list app.instanceInfos as instanceInfo>
            <#list instanceInfo.instances as instance>
              <div style="padding:5px;border-bottom:1px solid #888">
                <div>
                  <#if instance.isHref>
                    <a href="${instance.url}" target="_blank" class="m-r-md" title="${instance['fuzzy.boot.path']}"><b>${instance.id}</b></a>
                  <#else>
                  ${instance.id}
                  </#if>
                </div>
                <div class="instance-detail">
                  <span class="m-r-xs" <#if instance.status != 'UP'>style="color:red"</#if> >[${instance.status}]</span>
                  <span class="m-r-md">[${instance.lastUpdateTime}]</span>
                  <#if instance.groupTagFilterRuleEnable == 'true'>
                    <#if instance.groupTagFilterRuleTags?size == 0 && instance.groupTagFilterRuleNoTagsLimit == 'true'>
                    <span class="m-r-md">
                      RPC-Rule-Tags: [
                        <span class="label default-label" title="Only Invoke “NONE” Tag">NONE</span>
                      ]
                    </span>
                    </#if>
                    <#if instance.groupTagFilterRuleTags?size gt 0>
                    <span class="m-r-md">
                      RPC-Rule-Tags: [
                      <#list instance.groupTagFilterRuleTags as tag>
                        <span class="label rule-tag" data-id="${tag}">${tag}</span>
                      </#list>
                      ]
                    </span>
                    </#if>
                  </#if>
                  <span class="option-label m-r-md">
                    <#if (instance.turbineGroup)?? && instance.turbineGroup?size gt 0>
                      <span class="m-r-md">
                        Turbine-Groups: [
                          <#list instance.turbineGroup as turbineGroup>
                            <a style="margin:5px" data-id="${turbineGroup}"
                               href="javascript:base.openTurbine('${instance.host}','${turbineGroup}');"
                               title="Turbine [${turbineGroup}] Dashboard">
                              ${turbineGroup}
                            </a>
                          </#list>
                        ]
                      </span>
                    </#if>
                    <a class="m-r-xs" href="javascript:window.open('${instance.host}health');" title="Health">
                      [Health]
                    </a>
                    <#if instance.isHystrixModule>
                      <a class="m-r-xs" href="javascript:base.openHystrix('${instance.host}');" title="Hystrix Dashboard">
                        [Hystrix]
                      </a>
                    </#if>
                    <a class="m-r-xs" href="javascript:base.toRestart('${instance.host}');" title="重启">
                      [重启]
                    </a>
                    <a class="m-r-xs" href="javascript:base.toShutdown('${instance.host}');" title="关闭">
                      [关闭]
                    </a>
                  </span>
                </div>
              </div>
            </#list>
          </#list>
        </td>
      </tr>
      </#list>
    <#else>
    <tr>
      <td colspan="4">No instances available</td>
    </tr>
    </#if>

    </tbody>
  </table>

  <h1>Refresh Modules</h1>
  <table class="table table-striped table-hover">
    <thead>
    <tr>
      <th style="text-align: center;">
        <div style="display:inline-block;vertical-align: top;">
          <div>需要刷新的服务名： </div>
          <div>（不会刷新Eureka和config）</div>
        </div>
        <div style="display:inline-block;padding-top: 8px;width:50%">
          <input id="refresh_modules" style="width: 100%">
        </div>
      </th>
      <th style="vertical-align:inherit">
        <labal class="option-label">
          <a href="javascript:base.toRefresh();">刷新Module</a>
        </labal>
      </th>
    </tr>
    </thead>
  </table>

  <h1>General Info</h1>

  <table id='generalInfo' class="table table-striped table-hover">
    <thead>
    <tr>
      <th>Name</th>
      <th>Value</th>
    </tr>
    </thead>
    <tbody>
    <#list statusInfo.generalStats?keys as stat>
    <tr>
      <td>${stat}</td>
      <td>${statusInfo.generalStats[stat]!""}</td>
    </tr>
    </#list>
    <#list statusInfo.applicationStats?keys as stat>
    <tr>
      <td>${stat}</td>
      <td>${statusInfo.applicationStats[stat]!""}</td>
    </tr>
    </#list>
    </tbody>
  </table>

  <h1>Instance Info</h1>

  <table id='instanceInfo' class="table table-striped table-hover">
    <thead>
    <tr>
      <th>Name</th>
      <th>Value</th>
    </tr>
    <thead>
    <tbody>
    <#list instanceInfo?keys as key>
    <tr>
      <td>${key}</td>
      <td>${instanceInfo[key]!""}</td>
    </tr>
    </#list>
    </tbody>
  </table>

  <#if isOpenConfigServer>
    <h1 class="option-label"><a href="javascript:" onclick="base.loadConfig()">Config Server</a></h1>
    <div  id="config_div"></div>
    <h1 class="option-label"><a href="javascript:" onclick="base.loadEs()">ElasticSearch Info</a></h1>
    <div  id="es_div"></div>
  </#if>

</div>
<style>
  .option-label-blocking a{
    color:#999;
  }
  .option-label a{
    color:#5fa134;
  }
  .white-pre{
    border:0;margin:0;background:none
  }
  .label{
    margin:3px;
    color: #fff;
    padding: 3px;
    border-radius: 5px;
  }
  .label.default-label{
    background: #b4b4b4;
  }
  .label.warn-label{
    background: #ff6e14;
  }
  .m-r-xs{
    margin-right:10px;
  }
  .m-r-md{
    margin-right:20px;
  }
</style>
<script type="text/javascript" src="eureka/js/wro.js"></script>
<script type="text/javascript">
  $(document).ready(function () {
    $('table.stripeable tr:odd').addClass('odd');
    $('table.stripeable tr:even').addClass('even');
    base.randomTagColor(24);
  });
  var base = {
    doing : false,
    gateWay: "http://127.0.0.1:61111",
    blocking: function(doing){
      base.doing = doing;
      if(doing){
        $(".option-label").attr("class", "option-label-blocking");
      }else{
        $(".option-label-blocking").attr("class", "option-label");
      }
    },
    openHystrix: function (hostParam) {
      if (!base.doing) {
        var re = prompt("输入可以直接访问的Zuul或Turbine服务的Host地址", base.gateWay);
        if (re === null) {
          return;
        }
        var auth = prompt("输入目标服务的授权验证字符（如：user:password）", "user:password");
        if (auth === null) {
          return;
        }
        base.gateWay = re;
        if(base.gateWay.substr(base.gateWay.length-1, base.gateWay.length) === "/"){
          base.gateWay = base.gateWay.substr(0, base.gateWay.length -1 );
        }
        window.open(base.gateWay + "/hystrix/monitor?stream=" + encodeURIComponent(base.inserAuth(hostParam, auth) + "hystrix.stream"));
      }
    },
    openTurbine: function (host, group) {
      if (!base.doing) {
        window.open(host + "hystrix/monitor?stream=" + encodeURIComponent(host + "turbine.stream?cluster=" + group));
      }
    },
    toRestart: function (host) {
      if (!base.doing) {
        base.blocking(true);
        if (confirm("restart > " + host)) {
          $.post("/base/toRestart", {host: host}, function (re) {
            alert(re);
            base.blocking(false);
          }, "text");
        } else {
          base.blocking(false);
        }
      }
    },
    toShutdown: function (host) {
      if (!base.doing) {
        base.blocking(true);
        if (confirm("shutdown > " + host)) {
          $.post("/base/toShutdown", {host: host}, function (re) {
            alert(re);
            base.blocking(false);
          }, "text");
        } else {
          base.blocking(false);
        }
      }
    },
    toRefresh:function(){
      if (!base.doing) {
        var modules = $("#refresh_modules").val().trim();
        var text = modules === "" ? "是否要刷新全部module？": "是否要刷新：" + modules;
        base.blocking(true);
        if (confirm(text)) {
          $.post("/base/toRefresh", {modules: modules}, function (re) {
            alert(re);
            base.blocking(false);
          }, "text");
        } else {
          base.blocking(false);
        }
      }
    },
    loadConfig:function(label){
      if (!base.doing) {
        base.blocking(true);
        var table = $("#config_table");
        if(table.length === 0 ){
          $.get("/base/config/ftl", {label: label?label:""}, function(re){
            $("#config_div").html(re);
            base.blocking(false);
          },"text");
        }else{
          $("#config_div").toggle();
          base.blocking(false);
        }
      }
    },
    loadEs:function(){
      if (!base.doing) {
        base.blocking(true);
        var table = $("#es_table");
        if(table.length === 0 ){
          $.get("/base/es/ftl",function(re){
            $("#es_div").html(re);
            base.blocking(false);
          },"text");
        }else{
          $("#es_div").toggle();
          base.blocking(false);
        }
      }
    },
    inserAuth:function(url, auth){
      return url.substr(0, url.indexOf("//")+2) + auth + "@" + url.substr(url.indexOf("//")+2);
    },
    shuffleArray:function(array){
      var len = array.length;
      for(var i = 0 ; i < len -1 ; i++){
        var idx = Math.floor(Math.random() * (len - i));
        var tp = array[idx];
        array[idx] = array[len - i - 1];
        array[len - i - 1] = tp;
      }
      return array;
    },
    randomTagColor:function(bgCount){
      var arr = [];
      for(var i = 0 ; i < bgCount; i++){
        arr[i] = i;
      }
      var rules = {};
      $('.rule-tag').each(function(){
        rules[$(this).data("id")] = 0;
      });
      for(var id in rules){
        arr = base.shuffleArray(arr);
        var s = arr.shift();
        $(".rule-tag[data-id='"+id+"']").addClass("bg-"+(s === undefined? "0" : s));
      }
    },
    showDetail:function(e){
      if(e.checked){
        $(".instance-detail").show();
      }else{
        $(".instance-detail").hide();
      }
    }
  };
</script>

<style>
  .bg-0{
    background: #10d2bd;
  }
  .bg-1{
    background: #44c3ff;
  }
  .bg-2{
    background: #4084ff;
  }
  .bg-3{
    background: #675eff;
  }
  .bg-4{
    background: #af4bff;
  }
  .bg-5{
    background: #ff3dfa;
  }
  .bg-6{
    background: #ff3e91;
  }
  .bg-7{
    background: #e4160c;
  }
  .bg-8{
    background: #ffc10a;
  }
  .bg-9{
    background: #98cf17;
  }
  .bg-10{
    background: #5ed921;
  }
  .bg-11{
    background: #40d883;
  }
  .bg-12{
    background: #0f917f;
  }
  .bg-13{
    background: #2c6e96;
  }
  .bg-14{
    background: #2a59a8;
  }
  .bg-15{
    background: #453fab;
  }
  .bg-16{
    background: #6c2e9d;
  }
  .bg-17{
    background: #9c2599;
  }
  .bg-18{
    background: #992557;
  }
  .bg-19{
    background: #870d08;
  }
  .bg-20{
    background: #916e06;
  }
  .bg-21{
    background: #5c7e0e;
  }
  .bg-22{
    background: #3f9116;
  }
  .bg-23{
    background: #26814e;
  }
</style>
</body>
</html>
