package com.atguigu.gulimall.thirdparty;


import com.alibaba.fastjson.JSON;
import com.aliyun.oss.OSS;
import com.atguigu.gulimall.thirdparty.component.SmsComponent;
import com.atguigu.gulimall.thirdparty.util.HttpUtils;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StringUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
@RunWith(SpringRunner.class)
public class GulimallThirdPartyApplicationTests {

    @Autowired
    OSS ossClient;

    @Autowired
    SmsComponent smsComponent;

    @Test
    public void contextLoads() {
    }



    @Test
    public void upload(){
//        // yourEndpoint填写Bucket所在地域对应的Endpoint。以华东1（杭州）为例，Endpoint填写为https://oss-cn-hangzhou.aliyuncs.com。
//        String endpoint = "oss-cn-beijing.aliyuncs.com";
//        // 阿里云账号AccessKey拥有所有API的访问权限，风险很高。强烈建议您创建并使用RAM用户进行API访问或日常运维，请登录RAM控制台创建RAM用户。
//                String accessKeyId = "";
//                String accessKeySecret = "";
//
//        // 创建OSSClient实例。
//                OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
//
//        // 填写字符串。
//                String content = "Hello OSS";

        // 创建PutObjectRequest对象。
        // 依次填写Bucket名称（例如examplebucket）和Object完整路径（例如exampledir/exampleobject.txt）。Object完整路径中不能包含Bucket名称。
        //PutObjectRequest putObjectRequest = new PutObjectRequest("examplebucket", "exampledir/exampleobject.txt", new ByteArrayInputStream(content.getBytes()));

        // 如果需要上传时设置存储类型和访问权限，请参考以下示例代码。
        // ObjectMetadata metadata = new ObjectMetadata();
        // metadata.setHeader(OSSHeaders.OSS_STORAGE_CLASS, StorageClass.Standard.toString());
        // metadata.setObjectAcl(CannedAccessControlList.Private);
        // putObjectRequest.setMetadata(metadata);

        // 上传字符串。
        //ossClient.putObject(putObjectRequest);

        try {
            FileInputStream fileInputStream = new FileInputStream("C:\\Users\\gaich\\Desktop\\test.png");
            ossClient.putObject("gulimall-gaich","test1.png",fileInputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


        // 关闭OSSClient。
        ossClient.shutdown();
        System.out.println("上传成功");

    }



    @Test
    public void sms(){
        String phone="13167590654";
        String code="222333";

        //判断手机号是否为空
        if(StringUtils.isEmpty(phone)){

            System.out.println("号码为空");
        }

        String host = "https://dfsns.market.alicloudapi.com";
        String path = "/data/send_sms";
        String method = "POST";
        String appcode = "f7dea730acf041da8c15f85899080114";
        Map<String, String> headers = new HashMap<String, String>();
        //最后在header中的格式(中间是英文空格)为Authorization:APPCODE 83359fd73fe94948385f570e3c139105
        headers.put("Authorization", "APPCODE " + appcode);
        //根据API的要求，定义相对应的Content-Type
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        Map<String, String> querys = new HashMap<String, String>();
        Map<String, String> bodys = new HashMap<String, String>();
        bodys.put("content", "code:"+code+",expire_at:5");
        bodys.put("phone_number", phone);
        bodys.put("template_id", "TPL_0001");


        try {
            /**
             * 重要提示如下:
             * HttpUtils请从
             * https://github.com/aliyun/api-gateway-demo-sign-java/blob/master/src/main/java/com/aliyun/api/gateway/demo/util/HttpUtils.java
             * 下载
             *
             * 相应的依赖请参照
             * https://github.com/aliyun/api-gateway-demo-sign-java/blob/master/pom.xml
             */
            HttpResponse response = HttpUtils.doPost(host, path, method, headers, querys, bodys);
            //System.out.println(response.toString());
            //获取response的body
            //System.out.println(EntityUtils.toString(response.getEntity()));
            Map<String,String> map=new HashMap<String, String>();
            Map<String, String> result = JSON.parseObject(EntityUtils.toString(response.getEntity()), map.getClass());
            //map.put("request_id","TID877a484f93ac48e09911f39fd3700081");
            //map.put("status","OK");
            if(result.get("status").toString().equals("OK")){
                System.out.println("发送成功");
            }else{
                System.out.println("发送失败");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testSendCode(){
        smsComponent.sendSmsCode("13167590654","123321");
    }

}
