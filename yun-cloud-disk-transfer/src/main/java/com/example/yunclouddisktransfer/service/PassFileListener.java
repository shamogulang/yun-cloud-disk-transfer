package com.example.yunclouddisktransfer.service;

import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

@Service
@RocketMQMessageListener(
        topic = "transfer_file",
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
        public String downloadUrl;
        public Map<String, String> thumbUploadUrls;
        public String baseName;
    }

    @Override
    public void onMessage(String message) {
        try {
            TransferFileEvent event = objectMapper.readValue(message, TransferFileEvent.class);
            String fileType = event.fileType;
            if (fileType.contains("image")  || fileType.contains("video")) {
                fileProcessor.processFile(event.baseName, event.fileId, fileType, event.downloadUrl, event.thumbUploadUrls);
            }

        } catch (Exception e) {
            e.printStackTrace();
            // 建议用 logger 记录异常
        }
    }
} 