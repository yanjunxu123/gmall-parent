/**
 * 产品列表接口
 * @author yjx
 * @date 2021年 12月25日 16:58:28
 */
package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.list.SearchParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ListController {
    @Autowired
    private ListFeignClient listFeignClient;
    @GetMapping("list.html")
    public String search(SearchParam searchParam, Model model){
        //  页面渲染需要的数据：searchParam propsParamList trademarkList urlParam attrsList orderMap goodsList pageNo totalPages
        //  SearchResponseVo = result.getData();中的数据  trademarkList attrsList goodsList pageNo totalPages
        //  springmvc 对象传值！如果传递的参数与实体类的属性名一样，则可以使用实体类获取！ 自动映射！
        Result<Map> result = listFeignClient.searchList(searchParam);
        model.addAllAttributes(result.getData());
        //拼接url，urlParam用来记录用户原来的请求
        String urlParam =this.makeUrlParam(searchParam);

        //品牌面包屑
        String trademarkParam = this.makeTrademarkParam(searchParam.getTrademark());

        //平台属性面包屑
        String[] props = searchParam.getProps();
        List<Map<String,Object>> propsParamList = this.makePropsParamList(props);

        //排序order=2:asc
        Map<String, Object> orderMap = this.makeOrderMap(searchParam.getOrder());



        //将整理的数据传给前端页面，用于渲染
        model.addAttribute("searchParam",searchParam);
        model.addAttribute("urlParam",urlParam);
        model.addAttribute("trademarkParam",trademarkParam);
        model.addAttribute("orderMap",orderMap);
        model.addAttribute("propsParamList",propsParamList);

        return "list/index";
    }

    /**
     * 排序参数的制作
     * @param order
     * @return
     */
    private Map<String, Object> makeOrderMap(String order) {
        Map<String, Object> map = new HashMap<>();
        if(!StringUtils.isEmpty(order)){
            String[] split = order.split(":");
            if (split.length == 2 && split != null){
                map.put("type",split[0]);
                map.put("sort",split[1]);
            }
        }else {
            //默认排序规则
            map.put("type",1);
            map.put("sort","desc");
        }
        return map;
    }

    /**
     * 制作平台属性面包屑
     * @param props
     * @return
     */
    private List<Map<String, Object>> makePropsParamList(String[] props) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (props != null && props.length > 0) {
            for (String prop : props) {
                String[] split = prop.split(":");
                if (split!= null && split.length == 3){
                    Map<String, Object> map = new HashMap<>();
                    map.put("attrId",split[0]);
                    map.put("attrValue",split[1]);
                    map.put("attrName",split[2]);
                    list.add(map);
                }
            }
        }
        return list;
    }

    /**
     * 制作品牌面包屑
     * @param trademark
     * @return
     */
    private String makeTrademarkParam(String trademark) {
        if(!StringUtils.isEmpty(trademark)){
            String[] split = trademark.split(":");
            if (split.length == 2 && split != null) {
                return "品牌："+split[1];
            }
        }
        return "";
    }

    /**
     * 制作UrlParam
     * @param searchParam
     * @return
     */
    private String makeUrlParam(SearchParam searchParam) {
        //主要使用append追加，来拼接urlParam
        StringBuilder urlParam  = new StringBuilder();
        //关键字：通过查询所需的参数
        if (searchParam.getKeyword()!=null) {
            urlParam.append("keyword=").append(searchParam.getKeyword());
        }

        //分类id：
        if (!StringUtils.isEmpty(searchParam.getCategory3Id())) {
            urlParam.append("category3Id=").append(searchParam.getCategory3Id());
        }
        if (!StringUtils.isEmpty(searchParam.getCategory2Id())) {
            urlParam.append("category2Id=").append(searchParam.getCategory2Id());
        }
        if (!StringUtils.isEmpty(searchParam.getCategory1Id())) {
            urlParam.append("category1Id=").append(searchParam.getCategory1Id());
        }

        //根据品牌：
//        http://list.gmall.com/list.html?category2Id=13&trademark=1:小米
        if (!StringUtils.isEmpty(searchParam.getTrademark())) {
            //只有通过分类id或关键字这两个入口进入检索，才能通过 品牌进行检索
            if (urlParam.length() > 0){
                urlParam.append("trademark=").append(searchParam.getTrademark());
            }
        }

        //根据平台属性：
        String[] props = searchParam.getProps();
//        http://list.gmall.com/list.html?category2Id=13&trademark=1:小米&props=23:4G:运行内存&props=24:256G:机身内存
        if (props!=null && props.length > 0) {
            for (String prop : props) {
                //只有通过分类id或关键字这两个入口进入检索，才能通过平台属性进行检索
                if (urlParam.length()>0){
                    urlParam.append("&props=").append(prop);
                }
            }
        }
        //返回拼接的url
        return "list.html?"+urlParam.toString();
    }
}

