# yun-cloud-disk-transfer

Spring Boot 项目：支持从 S3（MinIO）批量生成图片/视频多尺寸缩略图并上传回 S3。

## 依赖
- Spring Boot 2.7.x
- MinIO Java SDK
- Thumbnailator
- JavaCV (ffmpeg)

## 配置
见 `src/main/resources/application.yml`，需配置 S3 连接信息和缩略图参数。

## 主要类
- `S3Service`：S3 文件下载/上传
- `ThumbnailService`：图片/视频缩略图生成
- `FileProcessor`：调度处理

## 使用
1. 启动 MinIO 并创建 bucket
2. 修改 `application.yml` 配置
3. `mvn clean package`
4. 参考 `FileProcessor#processFile` 处理单个文件（可扩展为批量处理）

---
如需批量处理，可扩展 FileProcessor，遍历 S3 bucket 文件列表。 