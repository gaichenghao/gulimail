package com.atguigu.gulimall.ware.vo;


import lombok.Data;

import java.util.List;

@Data
public class PurchaseDoneVo {

    //@NonNull
    private Long id;

    private List<PurchaseItemDoneVo> items;
}
