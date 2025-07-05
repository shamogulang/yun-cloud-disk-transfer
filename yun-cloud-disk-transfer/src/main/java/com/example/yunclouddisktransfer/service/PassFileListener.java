package com.example.yunclouddisktransfer.service;

import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RocketMQMessageListener(
        topic = "transfer_file",
        consumerGroup = "transfer_file_group",
        messageModel = MessageModel.CLUSTERING
)
public class PassFileListener implements RocketMQListener<String> {

    private static final Logger logger = LoggerFactory.getLogger(PassFileListener.class);

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
        public Map<String, String> transcodedVideoUploadUrls;
        public String baseName;
    }

    @Override
    public void onMessage(String message) {
        try {
            logger.info("Received message: {}", message);
            TransferFileEvent event = objectMapper.readValue(message, TransferFileEvent.class);
            String fileType = event.fileType;
            
            if (fileType.contains("image") || fileType.contains("video")) {
                logger.info("Processing file: {} (type: {})", event.name, fileType);
                fileProcessor.processFile(event.baseName, event.fileId, fileType, event.downloadUrl, 
                                        event.thumbUploadUrls, event.transcodedVideoUploadUrls);
                logger.info("Successfully processed file: {}", event.name);
            } else {
                logger.info("Skipping file: {} (unsupported type: {})", event.name, fileType);
            }

        } catch (Exception e) {
            logger.error("Error processing message: {}", message, e);
            // 不要重新抛出异常，避免消息重复消费
        }
    }
} 