package com.example.yunclouddisktransfer.service;

import net.coobird.thumbnailator.Thumbnails;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

@Service
public class ThumbnailService {

    private static Map<String, Integer> sizeMap = new HashMap<>();
    static {
        sizeMap.put("S", 200);
        sizeMap.put("M", 400);
        sizeMap.put("L", 600);
    }

    private int videoFrameSecond = 1;

    public File createImageThumbnails(File imageFile, String size, String format) throws Exception {
        Integer sizeInt = sizeMap.get(size);
        File out = File.createTempFile("thumb-" + sizeInt + "-", "."+format);
        Thumbnails.of(imageFile)
                .size(sizeInt, sizeInt)
                .outputFormat(format)
                .toFile(out);
        return out;
    }

    public List<File> createVideoThumbnails(File videoFile, String format) throws Exception {
        List<File> result = new ArrayList<>();
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoFile)) {
            grabber.start();
            int frameNumber = (int) (videoFrameSecond * grabber.getFrameRate());
            grabber.setFrameNumber(frameNumber);
            Java2DFrameConverter converter = new Java2DFrameConverter();
            BufferedImage frame = converter.getBufferedImage(grabber.grabImage());
            
            if (frame != null) {
                for (Map.Entry<String, Integer> entry : sizeMap.entrySet()) {
                    Integer size = entry.getValue();
                    BufferedImage thumb = Thumbnails.of(frame).size(size, size).asBufferedImage();
                    File out = File.createTempFile("thumb-" + size + "-", "." + format);
                    ImageIO.write(thumb, format, out);
                    result.add(out);
                }
            }
            grabber.stop();
        }
        return result;
    }
} 