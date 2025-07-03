package com.example.yunclouddisktransfer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

import com.example.yunclouddisktransfer.entity.FileDerivative;
import com.example.yunclouddisktransfer.mapper.FileDerivativeMapper;

@Service
public class FileProcessor {
    @Autowired
    private S3Service s3Service;
    @Autowired
    private ThumbnailService thumbnailService;
    @Autowired
    private FileDerivativeMapper fileDerivativeMapper;
    @Autowired
    private TranscodeService transcodeService;

    // 简单示例：处理单个 S3 文件
    public void processFile(String objectName, String type, String format) throws Exception {
        File temp = File.createTempFile("origin-", "." + format);
        s3Service.downloadFile(objectName, temp.getAbsolutePath());
        if (type.equalsIgnoreCase("image")) {
            List<File> thumbs = thumbnailService.createImageThumbnails(temp);
            for (File thumb : thumbs) {
                String[] parts = thumb.getName().split("-");
                String size = parts[1];
                String thumbFormat = thumb.getName().substring(thumb.getName().lastIndexOf('.') + 1);
                String s3ThumbPath = "thumbnails/" + size + "/" + objectName + "." + thumbFormat;
                s3Service.uploadFile(s3ThumbPath, thumb, "image/" + thumbFormat);

                FileDerivative derivative = new FileDerivative();
                derivative.setOriginFileId(objectName);
                derivative.setType("thumbnail");
                derivative.setSize(Integer.valueOf(size));
                derivative.setS3Path(s3ThumbPath);
                derivative.setFormat(thumbFormat);
                fileDerivativeMapper.insert(derivative);
            }
            for (File thumb : thumbs) thumb.delete();
        } else if (type.equalsIgnoreCase("video")) {
            List<File> transcodedVideos = transcodeService.transcodeVideoToMp4MultiRes(temp);
            for (File transcoded : transcodedVideos) {
                String res = transcoded.getName().replaceAll("\\D+", ""); // 提取分辨率高度
                String s3TranscodedPath = "transcoded/" + res + "p/" + objectName + ".mp4";
                s3Service.uploadFile(s3TranscodedPath, transcoded, "video/mp4");

                FileDerivative videoDerivative = new FileDerivative();
                videoDerivative.setOriginFileId(objectName);
                videoDerivative.setType("video");
                videoDerivative.setSize(Integer.valueOf(res));
                videoDerivative.setS3Path(s3TranscodedPath);
                videoDerivative.setFormat("mp4");
                fileDerivativeMapper.insert(videoDerivative);
            }
            for (File transcoded : transcodedVideos) transcoded.delete();
        }
        temp.delete();
    }
} 