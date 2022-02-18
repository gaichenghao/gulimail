package com.atguigu.gulimall.order.web;


import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.vo.OrderConfirmVo;
import com.atguigu.gulimall.order.vo.OrderSubmitVo;
import com.atguigu.gulimall.order.vo.SubmitOrderResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.concurrent.ExecutionException;

@Controller
public class OrderWebController {


    @Autowired
    OrderService orderService;

    @GetMapping("/toTrade")
    public String toTrade(Model model) throws ExecutionException, InterruptedException {
        OrderConfirmVo confirmVo=orderService.confirmOrder();

        model.addAttribute("orderConfirmData",confirmVo);
        return "confirm";
    }

    /**
     * 下单功能
     */
    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo vo,Model model){
        SubmitOrderResponseVo responseVo=orderService.submitOrder(vo);

        //下单 去创建订单 验令牌 验价格 锁库存
        //下单成功来到支付选择也
        //下单时报回到订单确认页面重新确认订单信息、
        System.out.println("订单提交的数据。。。。。"+vo);
        if(responseVo.getCode()==0){
            //下单成功来到支付选择页
            model.addAttribute("submitOrderResp",responseVo);

            return "pay";
        }else {
            return  "redirect:http://order.gulimall.com/toTrade";
        }
    }

}
