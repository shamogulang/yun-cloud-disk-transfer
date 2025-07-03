package com.example.yunclouddisktransfer.service;

import org.springframework.stereotype.Service;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service
public class TranscodeService {
    public List<File> transcodeVideoToMp4MultiRes(File videoFile) throws Exception {
        List<File> result = new ArrayList<>();
        int[][] resolutions = {{1920, 1080}, {1280, 720}, {854, 480}};
        for (int[] res : resolutions) {
            File out = File.createTempFile("transcoded-" + res[1] + "p-", ".mp4");
            String cmd = String.format(
                "ffmpeg -y -i \"%s\" -vf scale=%d:%d -c:v libx264 -crf 23 -c:a aac \"%s\"",
                videoFile.getAbsolutePath(), res[0], res[1], out.getAbsolutePath()
            );
            Process process = Runtime.getRuntime().exec(cmd);
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                result.add(out);
            } else {
                out.delete();
            }
        }
        return result;
    }
} 