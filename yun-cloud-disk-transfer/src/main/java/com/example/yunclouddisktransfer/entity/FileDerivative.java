package com.example.yunclouddisktransfer.entity;

import lombok.Data;
import java.util.Date;

@Data
public class FileDerivative {
    private Long id;
    private String originFileId;
    private String type; // thumbnail/video
    private Integer size; // 缩略图尺寸，转码视频可为null
    private String s3Path;
    private String format;
    private Date createdAt;
} 