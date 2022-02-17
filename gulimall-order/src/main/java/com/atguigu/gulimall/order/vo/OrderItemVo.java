package com.atguigu.gulimall.order.vo;


import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class OrderItemVo {

    private Long skuId;

    private String title;
    private String image;
    private List<String> skuAttr;
    private BigDecimal price;

    private Integer count;
    private BigDecimal totalPrice;

    // TODO: 2022/2/17 查询库存状态
    private boolean hasStock;

    private BigDecimal weight;



}
