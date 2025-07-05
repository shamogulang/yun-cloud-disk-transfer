package com.example.yunclouddisktransfer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.Map;

import com.example.yunclouddisktransfer.entity.FileDerivative;
import com.example.yunclouddisktransfer.mapper.FileDerivativeMapper;

@Service
public class FileProcessor {
    private static final Logger logger = LoggerFactory.getLogger(FileProcessor.class);
    
    @Autowired
    private S3Service s3Service;
    @Autowired
    private ThumbnailService thumbnailService;
    @Autowired
    private FileDerivativeMapper fileDerivativeMapper;
    @Autowired
    private TranscodeService transcodeService;

    // 简单示例：处理单个 S3 文件
    public void processFile(String baseName, String fileId, String type, String downloadUrl, 
                          Map<String,String> thumbnailUrls, Map<String,String> transcodedVideoUploadUrls) throws Exception {
        File temp = null;
        try {
            if (type.contains("image")) {
                logger.info("Processing image file: {}", fileId);
                String format = "jpg";
                temp = File.createTempFile("origin-"+fileId, "." + format);
                s3Service.downloadFile(downloadUrl, temp.getAbsolutePath());
                logger.info("Downloaded image file to: {}", temp.getAbsolutePath());
                
                if (thumbnailUrls != null && !thumbnailUrls.isEmpty()) {
                    File finalTemp = temp;
                    thumbnailUrls.forEach((sizeType, uploadUrl)->{
                        try {
                            logger.info("Creating thumbnail for size: {}", sizeType);
                            File thumbFile = thumbnailService.createImageThumbnails(finalTemp, sizeType, format);
                            s3Service.uploadFile(uploadUrl, thumbFile, "image/"+format);
                            FileDerivative derivative = new FileDerivative();
                            derivative.setOriginFileId(fileId);
                            derivative.setType("thumbnail");
                            derivative.setS3Path(baseName+"-"+sizeType+"."+format);
                            derivative.setFormat(format);
                            fileDerivativeMapper.insert(derivative);
                            thumbFile.delete();
                            logger.info("Successfully created and uploaded thumbnail for size: {}", sizeType);
                        } catch (Exception e) {
                            logger.error("Error creating thumbnail for size: {}", sizeType, e);
                            throw new RuntimeException(e);
                        }
                    });
                }
            } else if (type.contains("video")) {
                logger.info("Processing video file: {}", fileId);
                String format = "mp4";
                temp = File.createTempFile("origin-"+fileId, "." + format);
                s3Service.downloadFile(downloadUrl, temp.getAbsolutePath());
                logger.info("Downloaded video file to: {}", temp.getAbsolutePath());
                
                // 生成视频缩略图
                if (thumbnailUrls != null && !thumbnailUrls.isEmpty()) {
                    logger.info("Creating video thumbnails");
                    List<File> videoThumbnails = thumbnailService.createVideoThumbnails(temp, "jpg");
                    // 按照 S, M, L 的顺序处理缩略图
                    String[] sizeOrder = {"S", "M", "L"};
                    for (int i = 0; i < videoThumbnails.size() && i < sizeOrder.length; i++) {
                        String sizeType = sizeOrder[i];
                        String uploadUrl = thumbnailUrls.get(sizeType);
                        if (uploadUrl != null) {
                            try {
                                File thumbFile = videoThumbnails.get(i);
                                s3Service.uploadFile(uploadUrl, thumbFile, "image/jpg");
                                FileDerivative derivative = new FileDerivative();
                                derivative.setOriginFileId(fileId);
                                derivative.setType("thumbnail");
                                derivative.setS3Path(baseName+"-"+sizeType+".jpg");
                                derivative.setFormat("jpg");
                                fileDerivativeMapper.insert(derivative);
                                thumbFile.delete();
                                logger.info("Successfully created and uploaded video thumbnail for size: {}", sizeType);
                            } catch (Exception e) {
                                logger.error("Error creating video thumbnail for size: {}", sizeType, e);
                            }
                        }
                    }
                }
                
                // 转码视频到不同分辨率
                if (transcodedVideoUploadUrls != null && !transcodedVideoUploadUrls.isEmpty()) {
                    logger.info("Transcoding video to multiple resolutions");
                    Map<String, File> transcodedVideos = transcodeService.transcodeVideoToMp4MultiRes(temp);
                    for (String code : transcodedVideos.keySet()) {
                        String uploadUrl = transcodedVideoUploadUrls.get(code);
                        File tranFile = transcodedVideos.get(code);
                        if (uploadUrl != null) {
                            try {
                                s3Service.uploadFile(uploadUrl, tranFile, "video/"+format);
                                FileDerivative videoDerivative = new FileDerivative();
                                videoDerivative.setOriginFileId(fileId);
                                videoDerivative.setType("video");
                                videoDerivative.setSize(Integer.valueOf(code));
                                videoDerivative.setS3Path(baseName+"-"+code+".mp4");
                                videoDerivative.setFormat(format);
                                fileDerivativeMapper.insert(videoDerivative);
                                logger.info("Successfully transcoded and uploaded video for resolution: {}p", code);
                            } catch (Exception e) {
                                logger.error("Error transcoding video for resolution: {}p", code, e);
                            }
                        }
                        tranFile.delete();
                    }
                }
            }
        } finally {
            if (temp != null && temp.exists()) {
                temp.delete();
                logger.info("Cleaned up temporary file: {}", temp.getAbsolutePath());
            }
        }
    }
} 