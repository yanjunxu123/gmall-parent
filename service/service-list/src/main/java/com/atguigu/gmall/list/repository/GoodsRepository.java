/**
 * @author yjx
 * @date 2021年 12月22日 19:01:25
 */
package com.atguigu.gmall.list.repository;

import com.atguigu.gmall.model.list.Goods;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;


public interface GoodsRepository extends ElasticsearchRepository<Goods,Long> {
}

