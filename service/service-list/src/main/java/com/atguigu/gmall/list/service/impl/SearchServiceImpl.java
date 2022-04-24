/**
 * @author yjx
 * @date 2021年 12月22日 18:59:22
 */
package com.atguigu.gmall.list.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.list.repository.GoodsRepository;
import com.atguigu.gmall.list.service.SearchService;
import com.atguigu.gmall.model.list.*;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;
@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RestHighLevelClient restHighLevelClient;
    @Override
    public void upperGoods(Long skuId) {
        //创建一个商品对象，用于汇总数据，最后保存到es中
        Goods goods = new Goods();
        //获取sku对应平台属性
//        List<BaseAttrInfo> baseAttrInfoList = productFeignClient.getAttrList(skuId);
//        if (baseAttrInfoList != null){
//            List<SearchAttr> searchAttrList  = baseAttrInfoList.stream().map(baseAttrInfo -> {
//                SearchAttr searchAttr = new SearchAttr();
//                searchAttr.setAttrId(baseAttrInfo.getId());
//                searchAttr.setAttrName(baseAttrInfo.getAttrName());
//                List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
//                searchAttr.setAttrValue(attrValueList.get(0).getValueName());
//                return searchAttr;
//            }).collect(Collectors.toList());
//            goods.setAttrs(searchAttrList);
//        }

        CompletableFuture<Void> baseAttrInfoCompletableFuture = CompletableFuture.runAsync(() -> {
            List<BaseAttrInfo> baseAttrInfoList = productFeignClient.getAttrList(skuId);
            if (baseAttrInfoList != null) {
                List<SearchAttr> searchAttrList = baseAttrInfoList.stream().map(baseAttrInfo -> {
                    SearchAttr searchAttr = new SearchAttr();
                    searchAttr.setAttrId(baseAttrInfo.getId());
                    searchAttr.setAttrName(baseAttrInfo.getAttrName());
                    List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
                    searchAttr.setAttrValue(attrValueList.get(0).getValueName());
                    return searchAttr;
                }).collect(Collectors.toList());
                goods.setAttrs(searchAttrList);
            }
        }, threadPoolExecutor);

        //查询sku信息
        CompletableFuture<SkuInfo> skuInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            goods.setId(skuInfo.getId());
            goods.setTitle(skuInfo.getSkuName());
            goods.setDefaultImg(skuInfo.getSkuDefaultImg());
            goods.setPrice(skuInfo.getPrice().doubleValue());
            goods.setCreateTime(new Date());
            return skuInfo;
        }, threadPoolExecutor);

        //  获取品牌数据,通过skuInfo的线程返回结果
        //  thenAccept。接收任务的处理结果，并消费处理，是消费型方法，无返回值.
        CompletableFuture<Void> trademarkCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            BaseTrademark trademark = productFeignClient.getTrademarkByTmId(skuInfo.getTmId());
            goods.setTmId(trademark.getId());
            goods.setTmName(trademark.getTmName());
            goods.setTmLogoUrl(trademark.getLogoUrl());
        }, threadPoolExecutor);


        // 查询分类
        CompletableFuture<Void> baseCategoryViewCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            BaseCategoryView baseCategoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            if (baseCategoryView != null) {
                goods.setCategory1Id(baseCategoryView.getCategory1Id());
                goods.setCategory1Name(baseCategoryView.getCategory1Name());
                goods.setCategory2Id(baseCategoryView.getCategory2Id());
                goods.setCategory2Name(baseCategoryView.getCategory2Name());
                goods.setCategory3Id(baseCategoryView.getCategory3Id());
                goods.setCategory3Name(baseCategoryView.getCategory3Name());
            }
        }, threadPoolExecutor);

        //多线程，多任务组合
        CompletableFuture.allOf(
                baseAttrInfoCompletableFuture,
                skuInfoCompletableFuture,
                trademarkCompletableFuture,
                baseCategoryViewCompletableFuture
        ).join();
        goodsRepository.save(goods);
    }

    /**
     * 下架商品
     * @param skuId
     */
    @Override
    public void lowerGoods(Long skuId) {
        this.goodsRepository.deleteById(skuId);
    }

    @Override
    public void incrHotScore(Long skuId) {
    //使用缓存保存HotScore 的次数，当HotScore增加到一定的规定阈值时，将其保存到es中
    // 使用redis作为缓存注意的点
    // 1，何种数据类型的选择，及其命令
    // 2，key的定义一定要见名知意。
        //定义一个key
        String hotKey = "hotScore";
        Double hotScore = redisTemplate.opsForZSet().incrementScore(hotKey, "skuId:" + skuId, 1);
        if (hotScore %10==0) {
            //先取出来goods，再将改变了属性值的goods放入
            Goods goods = goodsRepository.findById(skuId).get();
            goods.setHotScore(hotScore.longValue());
            goodsRepository.save(goods);
        }
    }

    @Override
    public SearchResponseVo search(SearchParam searchParam) throws IOException {
        /*
          //声明一个查询请求的对象，该对象中有构建好的dsl语句
        SearchRequest searchRequest = this.searchDslBuilder();
        //使用客户端进行查询，返回查询的结果
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        searchRequest.source(searchSourceBuilder);
        try {
            restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
         */
        //声明一个查询请求的对象，该对象中有构建好的dsl语句,searchParam中封装的是前端传过来的查询条件
        SearchRequest searchRequest = this.searchDslBuilder(searchParam);
        //使用客户端进行查询，返回查询的结果
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        //将查出的数据进行赋值，用页面展示所需要的vo对象封装这些数据，并返回。
        SearchResponseVo searchResponseVo = this.parseResultSearchResponse(searchResponse);
        /*
            //品牌 此时vo对象中的id字段保留（不用写） name就是“品牌” value: [{id:100,name:华为,logo:xxx},{id:101,name:小米,log:yyy}]
            private List<SearchResponseTmVo> trademarkList;    //使用聚合中的品牌信息
                                                        //使用聚合中的信息是因为 一个商品的品牌唯一，搜索该商品时所对应的平台属性值也是唯一的。
            //所有商品的顶头显示的筛选属性
            private List<SearchResponseAttrVo> attrsList = new ArrayList<>(); //使用聚合中的平台属性值信息

            //检索出来的商品信息
            private List<Goods> goodsList = new ArrayList<>();//用goods封装skuInfo的信息

            private Long total;//总记录数
            以上的需用dsl语句来获取信息
            ························································································
            下面的直接赋值返回即可
            private Integer pageSize;//每页显示的内容
            private Integer pageNo;//当前页面
            private Long totalPages;
         */
        searchResponseVo.setPageNo(searchParam.getPageNo());
        searchResponseVo.setPageSize(searchParam.getPageSize());
        //总页数=（总条数+每页大小-1）/每页大小
        Long totalPages = (searchResponseVo.getTotal()+searchParam.getPageSize()-1)/ searchParam.getPageSize();
        searchResponseVo.setTotalPages(totalPages);
        return searchResponseVo;
    }

    /**
     * 创建动态的dsl语句
     * @param searchParam
     * @return
     */
    private SearchRequest searchDslBuilder(SearchParam searchParam) {
        //定义一个查询器
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //bool 查询  相当于{query --- bool}
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //用户查询有多个入口，
        // 1.关键词分查询入口
        if (!StringUtils.isEmpty(searchParam.getKeyword())){
            boolQueryBuilder.must(QueryBuilders.matchQuery("title",searchParam.getKeyword()).operator(Operator.AND));
        }
        //2.通过分类id查询入口//  {query --- bool --- filter --- term }
        if (!StringUtils.isEmpty(searchParam.getCategory3Id())) {
            boolQueryBuilder.must(QueryBuilders.termQuery("category3Id",searchParam.getCategory3Id()));
        }
        //  {query --- bool --- filter --- term }
        if (!StringUtils.isEmpty(searchParam.getCategory2Id())) {
            boolQueryBuilder.must(QueryBuilders.termQuery("category2Id",searchParam.getCategory2Id()));
        }
        //  {query --- bool --- filter --- term }
        if (!StringUtils.isEmpty(searchParam.getCategory1Id())) {
            boolQueryBuilder.must(QueryBuilders.termQuery("category1Id",searchParam.getCategory1Id()));
        }

        //   根据品牌Id 生成dsl 语句！ trademark=1:小米
        String trademark = searchParam.getTrademark();
        if (!StringUtils.isEmpty(trademark)) {
            String[] split = trademark.split(":");
            if (split.length == 2 && split != null) {
                boolQueryBuilder.filter(QueryBuilders.termQuery("tmId",split[0]));
            }
        }
        //  平台属性值过滤！//  props=24:256G:机身内存&props=107:小米:二级手机
        String[] props = searchParam.getProps();
        if (props != null && props.length > 0) {
            for (String prop : props) {
                String[] split = prop.split(":");
                if (split != null && split.length == 3) {
                    //外层的bool
                    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                    //内层的bool
                    BoolQueryBuilder subBoolQuery = QueryBuilders.boolQuery();
                    subBoolQuery.must(QueryBuilders.termQuery("attrs.attrId",split[0]));
                    subBoolQuery.must(QueryBuilders.termQuery("attrs.attrValue",split[1]));
                    boolQuery.must(QueryBuilders.nestedQuery("attrs",subBoolQuery, ScoreMode.None));
                    boolQueryBuilder.filter(boolQuery);
                }
            }
        }

        //{ query --- bool }
        searchSourceBuilder.query(boolQueryBuilder);

        //   排序  order=1:desc order=1:asc  价格： order=2:desc order=2:asc
        String order = searchParam.getOrder();
        if (!StringUtils.isEmpty(order)){
            String[] split = order.split(":");
            if (split != null && split.length == 2) {
                //声明一个用来判断情况的字段
                String field = null;
                switch (split[0]){
                    case "1":
                        field = "hotScore";
                        break;
                    case "2":
                        field = "price";
                        break;
                }
                //排序设置
                searchSourceBuilder.sort(field,"asc".equals(split[1])? SortOrder.ASC:SortOrder.DESC);
            }else {
                searchSourceBuilder.sort("hotScore", SortOrder.DESC);
            }
        }

        //分页 start = （当前页码-1）* 每页大小
        int from = (searchParam.getPageNo()-1)*searchParam.getPageSize();
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(searchParam.getPageSize());
        //高亮显示
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title");
        highlightBuilder.preTags("<span style=color:red>");
        highlightBuilder.postTags("</span>");
        searchSourceBuilder.highlighter(highlightBuilder);

        //聚合 ： 品牌
        searchSourceBuilder.aggregation(
                AggregationBuilders.terms("tmIdAgg").field("tmId")
                    .subAggregation(AggregationBuilders.terms("tmNameAgg").field("tmName"))
                    .subAggregation(AggregationBuilders.terms("tmLogoUrlAgg").field("tmLogoUrl"))
        );

        //聚合 ： 平台属性
        searchSourceBuilder.aggregation(
                AggregationBuilders.nested("attrAgg","attrs")
                        .subAggregation(AggregationBuilders.terms("attrIdAgg").field("attrs.attrId")
                                .subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName"))
                                .subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue"))
                        )
                 );
        //  打印出dsl 语句！
        System.out.println("dsl:\t"+searchSourceBuilder.toString());
        //  声明一个请求对象，里面有dsl语句
        SearchRequest searchRequest = new SearchRequest("goods");
        //  设置查询时需要显示的字段：
        searchSourceBuilder.fetchSource(new String[] {"id","defaultImg","title","price","createTime"},null);
        searchRequest.source(searchSourceBuilder);
        return searchRequest;
    }

    /**
     * 将查到的数据封装到SearchResponseVo
     * @param searchResponse
     * @return
     */
    private SearchResponseVo parseResultSearchResponse(SearchResponse searchResponse) {
        //声明一个对象用于存储返回的数据信息
        SearchResponseVo searchResponseVo = new SearchResponseVo();
        /*
        //品牌 此时vo对象中的id字段保留（不用写） name就是“品牌” value: [{id:100,name:华为,logo:xxx},{id:101,name:小米,log:yyy}]
        private List<SearchResponseTmVo> trademarkList;    //使用聚合中的品牌信息
                                                    //使用聚合中的信息是因为 一个商品的品牌唯一，搜索该商品时所对应的平台属性值也是唯一的。
        //所有商品的顶头显示的筛选属性
        private List<SearchResponseAttrVo> attrsList = new ArrayList<>(); //使用聚合中的平台属性值信息

        //检索出来的商品信息
        private List<Goods> goodsList = new ArrayList<>();//用goods封装skuInfo的信息

        private Long total;//总记录数
         */

        //获取总记录数
        SearchHits hits = searchResponse.getHits();
        searchResponseVo.setTotal(hits.getTotalHits().value);
        //商品集合数据来自于：
        SearchHit[] subHits = hits.getHits();
        ArrayList<Goods> goodsList = new ArrayList<>();
        for (SearchHit subHit : subHits) {
            String sourceAsString = subHit.getSourceAsString();
            //  sourceAsString JSON 变为Goods
            Goods goods = JSON.parseObject(sourceAsString, Goods.class);
            // 分类Id 不需要高亮， 关键词 需要高亮！
            if (subHit.getHighlightFields().get("title")!= null) {
                //  需要高亮：
                Text title = subHit.getHighlightFields().get("title").getFragments()[0];
                goods.setTitle(title.toString());

            }
            //  将商品添加到集合
            goodsList.add(goods);
        }
        // 赋值商品集合数据
        searchResponseVo.setGoodsList(goodsList);
        //从聚合中获取品牌信息
        Map<String, Aggregation> stringAggregationMap = searchResponse.getAggregations().asMap();

        ParsedLongTerms tmIdAgg = (ParsedLongTerms) stringAggregationMap.get("tmIdAgg");
        List<SearchResponseTmVo> trademarkList = tmIdAgg.getBuckets().stream().map(bucket -> {
            //声明对象用于存储和返回
            SearchResponseTmVo searchResponseTmVo = new SearchResponseTmVo();
            String keyAsString = ((Terms.Bucket) bucket).getKeyAsString();
            searchResponseTmVo.setTmId(Long.valueOf(keyAsString));
            //  赋值品牌tmName , 需要先获取tmNameAgg   //  Aggregation ---> ParsedStringTerms 目的获取到桶！
            ParsedStringTerms tmNameAgg = ((Terms.Bucket) bucket).getAggregations().get("tmNameAgg");
            String tmName = tmNameAgg.getBuckets().get(0).getKeyAsString();
            searchResponseTmVo.setTmName(tmName);
            //  赋值品牌tmLogoUrl
            ParsedStringTerms tmLogoUrlAgg = ((Terms.Bucket) bucket).getAggregations().get("tmLogoUrlAgg");
            String tmLogoUrl = tmLogoUrlAgg.getBuckets().get(0).getKeyAsString();
            searchResponseTmVo.setTmLogoUrl(tmLogoUrl);

            return searchResponseTmVo;
        }).collect(Collectors.toList());
        //给品牌赋值
        searchResponseVo.setTrademarkList(trademarkList);

        // 从聚合中获取数据：
        //  平台属性值 聚合 nested 需要获取到 attrs

        ParsedNested attrAgg = (ParsedNested) stringAggregationMap.get("attrAgg");
        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attrIdAgg");
        List<SearchResponseAttrVo> attrsList = attrIdAgg.getBuckets().stream().map(bucket -> {
            //
            SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();
            String keyAsString = ((Terms.Bucket) bucket).getKeyAsString();
            searchResponseAttrVo.setAttrId(Long.valueOf(keyAsString));

            ParsedStringTerms attrNameAgg = ((Terms.Bucket) bucket).getAggregations().get("attrNameAgg");
            String attrName = attrNameAgg.getBuckets().get(0).getKeyAsString();
            searchResponseAttrVo.setAttrName(attrName);

            ParsedStringTerms attrValueAgg = ((Terms.Bucket) bucket).getAggregations().get("attrValueAgg");
            List<String> valueNameList = attrValueAgg.getBuckets().stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
            searchResponseAttrVo.setAttrValueList(valueNameList);
            return searchResponseAttrVo;
        }).collect(Collectors.toList());
        searchResponseVo.setAttrsList(attrsList);
        return searchResponseVo;
    }

}

