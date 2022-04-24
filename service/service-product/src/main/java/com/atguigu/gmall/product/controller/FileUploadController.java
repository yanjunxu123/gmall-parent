/**
 * @author yjx
 * @date 2021年 12月14日 18:56:32
 */
package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import io.minio.*;
import io.minio.errors.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("admin/product")
public class FileUploadController {
    //引用的属性必须存在与spring容器中
    @Value("${minio.endpointUrl}")
    public String endpointUrl;

    @Value("${minio.accessKey}")
    public String accessKey;

    @Value("${minio.secreKey}")
    public String secretKey;

    @Value("${minio.bucketName}")
    public String bucketName;

    /**
     * 上传文件
     * @param file
     * @return
     */
    @PostMapping("/fileUpload")
    public Result FileUpload(MultipartFile file) {
        String fileName = "";
        String url = "";
        try {
            // Create a minioClient with the MinIO server playground, its access key and secret key.
            MinioClient minioClient =
                    MinioClient.builder()
                            .endpoint(endpointUrl)
                            .credentials(accessKey, secretKey)
                            .build();

            // Make 'bucketName' bucket if not exist.
            boolean found =
                    minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                // Make a new bucket called 'bucketName'.
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            } else {
                System.out.println("Bucket bucketName already exists.");
            }

            // Upload '/home/user/Photos/asiaphotos.zip' as object name 'asiaphotos-2015.zip' to bucket
            // 'asiatrip'.

          fileName =  System.currentTimeMillis()+ UUID.randomUUID().toString();
            minioClient.putObject(
                    PutObjectArgs
                            .builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .stream(file.getInputStream(),file.getSize(),-1) //-1代表存储的是已知大小的文件
                            .build()
            );
//            url用于返回保存的文件的地址,便于前端回显数据
            url = endpointUrl+"/"+bucketName + "/"+fileName;
            System.out.println("url:\t" + url);
            return Result.ok(url);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.fail();
        }
    }


}

