package com.example.yunclouddisktransfer.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.annotation.PostConstruct;
import java.io.File;
import java.net.URI;
import java.nio.file.Paths;

@Service
public class S3Service {

    @Value("${aws.s3.endpoint}")
    private String endpoint;
    @Value("${aws.s3.accessKey}")
    private String accessKey;
    @Value("${aws.s3.secretKey}")
    private String secretKey;
    @Value("${aws.s3.bucket}")
    private String bucket;
    @Value("${aws.s3.region}")
    private String region;

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .build();
    }

    public void downloadFile(String downloadUrl, String destPath) {
        // 通过HTTP下载文件
        java.io.InputStream in = null;
        java.io.FileOutputStream out = null;
        try {
            java.net.URL url = new java.net.URL(downloadUrl);
            in = url.openStream();
            out = new java.io.FileOutputStream(destPath);
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { if (in != null) in.close(); } catch (Exception e) {}
            try { if (out != null) out.close(); } catch (Exception e) {}
        }
    }

    public void uploadFile(String uploadUrl, File file, String contentType) {
        // 通过HTTP PUT上传文件
        java.io.OutputStream out = null;
        java.io.FileInputStream in = null;
        java.net.HttpURLConnection conn = null;
        try {
            java.net.URL url = new java.net.URL(uploadUrl);
            conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Content-Type", contentType);
            out = conn.getOutputStream();
            in = new java.io.FileInputStream(file);
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
            int responseCode = conn.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new RuntimeException("Upload failed, HTTP code: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getCause());
        } finally {
            try { if (in != null) in.close(); } catch (Exception e) {}
            try { if (out != null) out.close(); } catch (Exception e) {}
            if (conn != null) conn.disconnect();
        }
    }
} 