/**
 * service-product对微服务提供的接口
 *
 * @author yjx
 * @date 2021年 12月17日 16:44:25
 */
package com.atguigu.gmall.product.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/product")
public class ProductApiController {
    @Autowired
    private ManageService manageService;

    //    价格需要单独查询：保证数据是实时价格!
    /**
     * 根据skuId返回商品价格
     * @param skuId
     * @return
     */
    @GetMapping("inner/getSkuPrice/{skuId}")
    public BigDecimal getSkuPrice(@PathVariable Long skuId){
        return manageService.getSkuPrice(skuId);
    }

    //商品的基本信息 skuInfo + skuImageList
    /**
     * 根据skuId查询skuInfo同时查询skuImag信息 放入skuInfo中并返回
     * @param skuId
     * @return
     */
    @GetMapping("inner/getSkuInfo/{skuId}")
    public SkuInfo getSkuInfo(@PathVariable Long skuId){
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        return skuInfo;
    }
    /**
     *  分类信息数据
     *     两种实现方式：
     *            ① 单表查询：分别在 category1 category2 category3;
     * 								select category3_id from sku_info where id = 21; # category3_id = 61
     * 								select name,category2_id from base_category3 where id = 61; #
     * 								select name,category1_id from base_category2 where id = 13; #
     * 								select name from base_category1 where id = 2; #
     * 		   ② 视图的方式：
     * 		        create view v_name as sql语句；
     * 		        # 创建分类信息的视图
     *                 create view test_base_category_view as
     *                 select
     *                        bc3.id id,
     *                        bc1.id category1_id,bc1.name as category1_name,
     *                        bc2.id category2_id,bc2.name as category2_name,
     *                        bc3.id category3_id,bc3.name as category3_name
     *                 from base_category1 bc1
     *                 inner join base_category2 bc2 on bc2.category1_id = bc1.id
     *                 inner join base_category3 bc3 on bc3.category2_id = bc2.id;
     *
     * 通过category3Id获取分类信息（获取视图的方式）
     * @param category3Id
     * @return
     */
    @GetMapping("inner/getCategoryView/{category3Id}")
    public BaseCategoryView getCategoryView(@PathVariable Long category3Id){
        return manageService.getCategoryViewByCategory3Id(category3Id);
    }

    /*
        回显销售属性 + 销售属性值 + 锁定！

   */
    @GetMapping("inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable Long skuId,
                                                          @PathVariable Long spuId){
            return  manageService.getSpuSaleAttrListCheckBySku(skuId,spuId);
    }

    /**
     * 返回锁定用的Map的json字符串
     * @param spuId
     * @return
     */
    @GetMapping("inner/getSkuValueIdsMap/{spuId}")
    public Map getSkuValueIdsMap(@PathVariable("spuId") Long spuId){
        return manageService.getSkuValueIdsMap(spuId);
    }

    /**
     * 获取海报信息
     * @param spuId
     * @return
     */
    @GetMapping("inner/findSpuPosterBySpuId/{spuId}")
    public List<SpuPoster> findSpuPosterBySpuId(@PathVariable Long spuId){
      return   manageService.findSpuPosterBySpuId(spuId);
    }

    /**
     * 获取规格信息
     * @param skuId
     * @return
     */
    @GetMapping("inner/getAttrList/{skuId}")
    public List<BaseAttrInfo> getAttrList(@PathVariable Long skuId){
        return manageService.getAttrList(skuId);
    }

    /**
     * 首页获取分类信息
     * @return
     */
    @GetMapping("inner/getBaseCategoryList")
    public Result getBaseCategoryList(){
        List<JSONObject> list = manageService.getBaseCategoryList();
        return Result.ok(list);
    }

    @GetMapping("inner/getTrademark/{tmId}")
    public BaseTrademark getTrademark(@PathVariable Long tmId){
        return manageService.getTrademarkByTmId(tmId);
    }
}

