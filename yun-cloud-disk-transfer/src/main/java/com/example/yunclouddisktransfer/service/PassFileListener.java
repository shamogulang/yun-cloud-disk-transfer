package com.example.yunclouddisktransfer.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.context.annotation.Bean;

@Service
@RocketMQMessageListener(
        topic = "pass_file",
        consumerGroup = "transfer_file_group",
        messageModel = MessageModel.CLUSTERING
)
public class PassFileListener implements RocketMQListener<String> {

    @Autowired
    private FileProcessor fileProcessor;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 新的 TransferFileEvent 数据结构
    public static class TransferFileEvent {
        public String fullFileIdPath;
        public String fileHash;
        public String createdAt;
        public String parentFileId;
        public String filePosition;
        public String name;
        public String fileType;
        public String fileId;
        public String updatedAt;
        public String eventTime;
        public String eventType;
        public String messageId;
        public String userId;
    }

    @Override
    public void onMessage(String message) {
        try {
            TransferFileEvent event = objectMapper.readValue(message, TransferFileEvent.class);
            String objectName = event.fullFileIdPath;
            String fileType = event.fileType;
            String format = null;
            if (objectName != null && objectName.contains(".")) {
                format = objectName.substring(objectName.lastIndexOf('.') + 1);
            } else {
                format = "jpg";
            }
            if ("image".equalsIgnoreCase(fileType) || "video".equalsIgnoreCase(fileType)) {
                fileProcessor.processFile(objectName, fileType, format);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 建议用 logger 记录异常
        }
    }
} 