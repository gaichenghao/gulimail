package com.atguigu.gulimall.thirdparty;


import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

@SpringBootTest
@RunWith(SpringRunner.class)
public class GulimallThirdPartyApplicationTests {

    @Autowired
    OSS ossClient;

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

}