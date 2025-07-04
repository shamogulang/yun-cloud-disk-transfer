package com.example.yunclouddisktransfer.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Map;

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
    public void processFile(String baseName, String fileId , String type,  String downloadUrl, Map<String,String> thumbnailUrls) throws Exception {
        if (type.contains("image")) {
            String format = "jpg";
            File temp = File.createTempFile("origin-"+fileId, "." + format);
            s3Service.downloadFile(downloadUrl, temp.getAbsolutePath());
            thumbnailUrls.forEach((sizeType,uploadUrl)->{
                try {
                    File thumbFile = thumbnailService.createImageThumbnails(temp, sizeType, format);
                    s3Service.uploadFile(uploadUrl, thumbFile, "image/"+format);
                    FileDerivative derivative = new FileDerivative();
                    derivative.setOriginFileId(fileId);
                    derivative.setType("thumbnail");
                    derivative.setS3Path(baseName+"-"+sizeType+"."+format);
                    derivative.setFormat(format);
                    fileDerivativeMapper.insert(derivative);
                    thumbFile.delete();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } else if (type.contains("video")) {
            String format = "mp4";
            File temp = File.createTempFile("origin-"+fileId, "." + format);
            List<File> transcodedVideos = transcodeService.transcodeVideoToMp4MultiRes(temp);
            for (File transcoded : transcodedVideos) {
                String res = transcoded.getName().replaceAll("\\D+", ""); // 提取分辨率高度
                String s3TranscodedPath = "transcoded/" + res + "p/" + fileId + "."+format;
                s3Service.uploadFile(null, transcoded, "video/"+format);
                FileDerivative videoDerivative = new FileDerivative();
                videoDerivative.setOriginFileId(fileId);
                videoDerivative.setType("video");
                videoDerivative.setSize(Integer.valueOf(res));
                videoDerivative.setS3Path(s3TranscodedPath);
                videoDerivative.setFormat(format);
                fileDerivativeMapper.insert(videoDerivative);
            }
            for (File transcoded : transcodedVideos) transcoded.delete();
        }
    }
} 