/**
 * @author yjx
 * @date 2021年 12月22日 16:27:33
 */
package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.FileWriter;
import java.io.IOException;

@Controller
public class IndexController {
    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private TemplateEngine templateEngine;

    @GetMapping({"/","index.html"})
    public String index(Model model) {
        // 获取首页分类数据
        Result result = productFeignClient.getBaseCategoryList();
        model.addAttribute("list",result.getData());
        return "index/index";
    }

    @GetMapping("createIndex")
    @ResponseBody
    public Result createIndex(){
        //  获取后台存储的数据
        Result result = productFeignClient.getBaseCategoryList();
        //  设置模板显示的内容
        Context context = new Context();
        context.setVariable("list",result.getData());

        //  定义文件输入位置
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter("D:\\index.html");
        } catch (IOException e) {
            e.printStackTrace();
        }
        //  调用process();方法创建模板
        templateEngine.process("index/index.html",context,fileWriter);
        return Result.ok();
    }

}

