package com.apidocs.code.controller;

import com.apidocs.code.codeutil.CodeGenerator;
import com.apidocs.code.codeutil.Column;
import com.apidocs.code.util.StringUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * controller 暴力
 *
 * @author <a href="morse.jiang@foxmail.com">JiangWen</a>
 * @version 1.0.0, 2018/6/1 0001 16:11
 */
@RestController
@RequestMapping("/code")
public class CodeController {



  @GetMapping("/table")
  public List<Map<String, String>> getTables(@RequestParam Map<String, String> params) throws Exception {
    String userName = params.get("userName");
    String password = params.get("password");
    String driver = params.get("driver");
    String url = params.get("url");
    String databaseName = params.get("databaseName");
    Connection conn = getConnection(userName, password, driver, url);
    PreparedStatement ps = conn.prepareStatement(
      "select `table_name`,table_comment,create_time from information_schema.tables where table_schema='" + databaseName
        + "' and table_type='base table' order by `table_name` asc");
    ResultSet rs = ps.executeQuery();
    List<Map<String, String>> tables = new ArrayList<>();
    while (rs.next()) {
      Map<String, String> temp = new HashMap<>(3);
      temp.put("tableName", rs.getString("table_name"));
      temp.put("tableComment", rs.getString("table_comment"));
      temp.put("createTime", rs.getString("create_time"));
      tables.add(temp);
    }
    closeConnection(conn, ps, rs);
    return tables;
  }

  @GetMapping("/genaretorWithMybatis")
  public Map genaretorWithMybatis(@RequestParam Map<String, String> params) throws Exception {
    params.put("mybatis", "with");
    return genaretor(params);
  }

  @GetMapping("/genaretor")
  public Map genaretor(@RequestParam Map<String, String> params) throws Exception {
    Map res = new HashMap(2);
    String mybatis = params.get("mybatis");
    String tableNames = params.get("tableName");
    String module = params.get("module");
    String javaSource = params.get("javaSource");
    String userName = params.get("userName");
    String password = params.get("password");
    String driver = params.get("driver");
    String url = params.get("url");
    String databaseName = params.get("databaseName");
    String pkgPrefix = params.get("pkgPrefix");
    String tablePrefix = params.get("tablePrefix");
    String[] tables = tableNames.split(",");
    CodeGenerator code = new CodeGenerator();
    for (String tableName : tables) {
      tableName = tableName.trim();
      code.setTableName(tableName);
      code.setTablePrefix(tablePrefix);
      //数据库读取配置信息
      code.setTableComment(getTableComment(databaseName, tableName, userName, password, driver, url));
      code.initCodeTool(getLsColumns(tableName, userName, password, driver, url));
      code.createCodeByConf(params);
    }
    res.put("result", "success");
    res.put("msg", "success");
    return res;
  }



  private Connection getConnection(String userName, String password, String driver, String url) throws Exception {
    Properties localProperties = new Properties();
    localProperties.put("remarksReporting", "true");
    localProperties.put("user", userName);
    localProperties.put("password", password);
    //	   orcl为数据库的SID
    Class.forName(driver).newInstance();
    Connection conn = DriverManager.getConnection(url, localProperties);
    return conn;
  }

  private void closeConnection(Connection conn, PreparedStatement ps, ResultSet rs) throws Exception {
    if (rs != null) {
      rs.close();
    }
    if (ps != null) {
      ps.close();
    }
    if (conn != null) {
      conn.close();
    }
  }

  private List<Column> getLsColumns(String tableName, String userName, String password, String driver, String url)
    throws Exception {
    Connection conn = getConnection(userName, password, driver, url);
    List<Column> lsColumns = new ArrayList<Column>(10);
    PreparedStatement stmt = conn.prepareStatement("select *  from " + tableName + " where 1=0 ");
    ResultSet resultSet = stmt.executeQuery();
    ResultSetMetaData rsmd = resultSet.getMetaData();
    int n = rsmd.getColumnCount();
    for (int i = 1; i <= n; i++) {
      String colName = rsmd.getColumnName(i);
      String fieldName = StringUtil.toBeanPatternStr(colName);
      String dataType = rsmd.getColumnClassName(i);
      if ("java.math.BigDecimal".equals(dataType) && rsmd.getScale(i) == 0) {
        dataType = "Long";
      }
      if ("oracle.sql.CLOB".equals(dataType)) {
        dataType = "String";
      }
      dataType = dataType.contains(".") ? dataType.substring(dataType.lastIndexOf(".") + 1, dataType.length()): dataType;
      Column column = new Column();
      column.setName(colName);
      column.setJavaName(fieldName);
      column.setDataType(dataType.endsWith("Timestamp") || dataType.endsWith("Date") ? "java.util.Date" : dataType);
      column.setPrecision(String.valueOf(rsmd.getPrecision(i)));
      column.setScale(String.valueOf(rsmd.getScale(i)));
      column.setLength(String.valueOf(rsmd.getColumnDisplaySize(i)));
      column.setNullable(String.valueOf(1 == rsmd.isNullable(i)));

      //			获取列注释
      DatabaseMetaData dbmd = conn.getMetaData();
      ResultSet rs = dbmd.getColumns(null, null, tableName, null);
      while (rs.next()) {
        if (colName.equals(rs.getString("COLUMN_NAME"))) {
          String comments = rs.getString("REMARKS");
          column.setComments(StringUtil.asString(comments).trim());
        }
      }
      //				获取主键列
      ResultSet rs2 = dbmd.getPrimaryKeys(null, null, tableName);
      while (rs2.next()) {
        if (colName.equals(rs2.getString("COLUMN_NAME"))) {
          column.setColumnKey("TRUE");
        }
      }
      lsColumns.add(column);
    }
    closeConnection(conn, stmt, resultSet);
    return lsColumns;
  }

  private String getTableComment(String databaseName, String tableName, String userName, String password, String driver,
                                 String url) throws Exception {
    Connection conn = getConnection(userName, password, driver, url);
    PreparedStatement ps = conn.prepareStatement(
      "Select TABLE_COMMENT from INFORMATION_SCHEMA.TABLES Where  table_schema = '" + databaseName
        + "' AND table_name LIKE '" + tableName + "'");
    ResultSet rs = ps.executeQuery();
    String tableComment = null;
    while (rs.next()) {
      tableComment = rs.getString("TABLE_COMMENT");
    }
    closeConnection(conn, ps, rs);
    return tableComment == null ? "" : tableComment;
  }




}
