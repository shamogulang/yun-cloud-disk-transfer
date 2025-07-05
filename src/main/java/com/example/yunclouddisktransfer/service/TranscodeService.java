package com.example.yunclouddisktransfer.service;

import org.springframework.stereotype.Service;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TranscodeService {
    public Map<String, File> transcodeVideoToMp4MultiRes(File videoFile) throws Exception {
        int[][] resolutions = {{1920, 1080}, {1280, 720}, {854, 480}};
        Map<String, File>  rsp = new HashMap<>();
        for (int[] res : resolutions) {
            File out = File.createTempFile("transcoded-" + res[1] + "p-", ".mp4");
            String cmd = String.format(
                "ffmpeg -y -i \"%s\" -vf scale=%d:%d -c:v libx264 -crf 23 -c:a aac \"%s\"",
                videoFile.getAbsolutePath(), res[0], res[1], out.getAbsolutePath()
            );
            
            System.out.println("Executing: " + cmd);
            
            Process process = Runtime.getRuntime().exec(cmd);
            
            // 创建线程来读取输出，防止阻塞
            Thread outputThread = new Thread(() -> {
                try (InputStream is = process.getInputStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // 可以选择是否打印输出
                         System.out.println("FFmpeg output: " + line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            
            Thread errorThread = new Thread(() -> {
                try (InputStream is = process.getErrorStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // 可以选择是否打印错误输出
                        // System.out.println("FFmpeg error: " + line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            
            // 启动输出读取线程
            outputThread.start();
            errorThread.start();
            
            // 等待进程完成
            int exitCode = process.waitFor();
            
            // 等待输出线程完成
            outputThread.join();
            errorThread.join();
            
            if (exitCode == 0) {
                rsp.put(String.valueOf(res[1]), out);
                System.out.println("Successfully transcoded to " + res[1] + "p");
            } else {
                System.out.println("FFmpeg failed with exit code: " + exitCode);
                out.delete();
            }
        }
        return rsp;
    }
} 