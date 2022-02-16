package com.atguigu.gulimall.cart.controller;


import com.atguigu.gulimall.cart.service.CartService;
import com.atguigu.gulimall.cart.vo.Cart;
import com.atguigu.gulimall.cart.vo.CartItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.concurrent.ExecutionException;

@Controller
public class CartController {



    @Autowired
    CartService cartService;

    @GetMapping("/deleteItem")
    public String deleteItem(@RequestParam("skuId") Long skuId){

        cartService.deleteItem(skuId);
        return "redirect:http://cart.gulimall.com/cart.html";

    }


    @GetMapping("/countItem")
    public String countItem(@RequestParam("skuId") Long skuId,
                            @RequestParam("num") Integer num){

        cartService.changeItemCount(skuId,num);
        return "redirect:http://cart.gulimall.com/cart.html";

    }



    @GetMapping("/checkItem")
    public String checkItem(@RequestParam("skuId") Long skuId,
                            @RequestParam("check") Integer check){
        cartService.checkItem(skuId,check);

        return "redirect:http://cart.gulimall.com/cart.html";

    }




    /**
     * 浏览器有一个cookie user-key 标识用户信息 一个月以后过期
     * 如果第一次使用jd的购物车功能 都会给一个临时的用户信息
     * 浏览器以后保存 每次访问都会带上这个cookie
     *
     * 登录 session有
     * 没登录 按照cookie里面带来user-key 来做
     * 第一次 如果没有临时用户 帮忙创建一个临时用户
     *
     *
     * @param session
     * @return
     */
    @GetMapping("/cartList.html")
    public String cartListPage(HttpSession session,Model model) throws ExecutionException, InterruptedException {

        //1\快速等到用户信息 id user-key
        //UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        //System.out.println(userInfoTo);
        Cart cart=cartService.getCart();
        model.addAttribute("cart",cart);
        return "cartList";
    }

    /**
     * 添加商品到购物车
     *
     * RedirectAttributes ra
     *  ra.addFlashAttribute(); 将数据放在session里面可以在页面取出 但是只能去一次
     *  ra.addAttribute("skuId",skuid);将数据放在url后面
     *
     *
     * @return
     */
    @GetMapping("/addToCart")
    public String addToCart(@RequestParam("skuId") Long skuId,
                            @RequestParam("num")Integer num,
                            RedirectAttributes ra) throws ExecutionException, InterruptedException {
        cartService.addToCart(skuId,num);
        ra.addAttribute("skuId",skuId);
        return "redirect:http://cart.gulimall.com/addToCartSuccess.html";
    }

    /**
     * 跳转到成功页
     * @param skuId
     * @param model
     * @return
     */
    @GetMapping("/addToCartSuccess.html")
    public String addToCartSuccessPage(@RequestParam("skuId") Long skuId,Model model){
        //重定向到成功页面 再次查询购物车数据即可
        CartItem cartItem=cartService.getCartItem(skuId);
        model.addAttribute("cartItem",cartItem);
        return "success";
    }



}
