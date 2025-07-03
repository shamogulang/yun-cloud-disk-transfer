package com.example.yunclouddisktransfer.service;

import net.coobird.thumbnailator.Thumbnails;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class ThumbnailService {

    private List<Integer> sizeList = Arrays.asList(200, 400, 800);

    private int videoFrameSecond = 5;

    public List<File> createImageThumbnails(File imageFile) throws Exception {
        List<File> result = new ArrayList<>();
        for (Integer size : sizeList) {
            for (String format : Arrays.asList("jpg", "webp")) {
                File out = File.createTempFile("thumb-" + size + "-", "." + format);
                Thumbnails.of(imageFile)
                        .size(size, size)
                        .outputFormat(format)
                        .toFile(out);
                result.add(out);
            }
        }
        return result;
    }

    public List<File> createVideoThumbnails(File videoFile, String format) throws Exception {
        List<File> result = new ArrayList<>();
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoFile)) {
            grabber.start();
            int frameNumber = (int) (videoFrameSecond * grabber.getFrameRate());
            grabber.setFrameNumber(frameNumber);
            Java2DFrameConverter converter = new Java2DFrameConverter();
            BufferedImage frame = converter.getBufferedImage(grabber.grabImage());
            for (Integer size : sizeList) {
                BufferedImage thumb = Thumbnails.of(frame).size(size, size).asBufferedImage();
                File out = File.createTempFile("thumb-" + size + "-", "." + format);
                ImageIO.write(thumb, format, out);
                result.add(out);
            }
            grabber.stop();
        }
        return result;
    }
} 