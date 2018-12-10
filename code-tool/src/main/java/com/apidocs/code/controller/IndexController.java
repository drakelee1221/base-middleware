package com.apidocs.code.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * IndexController
 *
 * @author <a href="morse.jiang@foxmail.com">JiangWen</a>
 * @version 1.0.0, 2018/6/4 0004 11:44
 */
@Controller
public class IndexController {

  @GetMapping("/")
  public String index(){
    return "index";
  }

}
