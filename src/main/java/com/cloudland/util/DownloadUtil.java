package com.cloudland.util;

import com.cloudland.config.StorageProperties;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

@Component
public class DownloadUtil {
    @Resource
    private StorageProperties storageProperties;

    public void getFile(String name, HttpServletResponse response) throws IOException {
        String filePath = storageProperties.getRootFilePath(name);
        File file = new File(filePath);
        if (file.exists()) {
            String extension = FilenameUtils.getExtension(name);
            String mimeType = Files.probeContentType(file.toPath());
            if (mimeType == null) {
                switch (extension.toLowerCase()) {
                    case "pdf":
                        mimeType = "application/pdf";
                        break;
                    case "xls":
                    case "xlsx":
                        mimeType = "application/vnd.ms-excel";
                        break;
                    case "doc":
                    case "docx":
                        mimeType = "application/msword";
                        break;
                    default:
                        mimeType = "application/octet-stream";
                        break;
                }
            }

            response.setContentType(mimeType);
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition", "attachment;fileName=" + java.net.URLEncoder.encode(name, "UTF-8"));
            byte[] buffer = new byte[1024];
            try (FileInputStream fis = new FileInputStream(file);
                 BufferedInputStream bis = new BufferedInputStream(fis);
                 OutputStream os = response.getOutputStream()) {

                int i;
                while ((i = bis.read(buffer)) != -1) {
                    os.write(buffer, 0, i);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("不存在此文件");
        }
    }
}
