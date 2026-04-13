package com.cloudland.util;

import com.cloudland.config.StorageProperties;
import com.cloudland.mapper.LandMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
@Slf4j
public class FileUtil {
    private static final String FILE_FOLDER = "/CloudLandFile";
    private static final String IMAGE_FOLDER = "/Images";
    @Resource
    LandMapper landMapper;
    @Resource
    private StorageProperties storageProperties;

    public List<Object> defineDirectory(Integer parentId, MultipartFile[] landFiles, MultipartFile[] imageFiles, MultipartFile userIcon, MultipartFile productImg) {
        //设置返回数据,给录入数据库使用.
        List<Object> mixedList = new ArrayList<>();

        if (userIcon != null) {
            String originalFileName = userIcon.getOriginalFilename();
            //提取原始文件名的后缀
            if (originalFileName == null || originalFileName.isEmpty()) {
                throw new IllegalArgumentException("文件名为空");
            }
            String suffixName = originalFileName.substring(originalFileName.lastIndexOf("."));
            //开始重命名
            String newFileName = "userIcon_" + UUID.randomUUID() + suffixName;
            try {
                createDirectory(storageProperties.getUserIconDir(), "头像文件夹");
                //创建一个destFile对象,并使用renameFiles里面的指定名称
                File destFile = new File(storageProperties.getUserIconPath(newFileName));
                //把客户端传过来的文件从file复制或移动到destFile所表示的目标位置
                userIcon.transferTo(destFile);
            } catch (IOException e) {
                log.error("头像文件保存失败: {}", e.getMessage());
            }
            mixedList.add(newFileName);
            return mixedList;
        }
        if (productImg != null) {
            String originalFileName = productImg.getOriginalFilename();
            //提取原始文件名的后缀
            if (originalFileName == null || originalFileName.isEmpty()) {
                throw new IllegalArgumentException("文件名为空");
            }
            String suffixName = originalFileName.substring(originalFileName.lastIndexOf("."));
            //开始重命名
            String newFileName = "product_" + UUID.randomUUID() + suffixName;
            try {
                createDirectory(storageProperties.getProductDir(), "产品文件夹");
                //创建一个destFile对象,并使用renameFiles里面的指定名称
                File destFile = new File(storageProperties.getProductPath(newFileName));
                //把客户端传过来的文件从file复制或移动到destFile所表示的目标位置
                productImg.transferTo(destFile);
            } catch (IOException e) {
                log.error("产品文件保存失败: {}", e.getMessage());
            }
            mixedList.add(newFileName);
            return mixedList;
        }

        //资料路径
        String filePath = storageProperties.getLandCloudFileDir(parentId);
        //图片路径
        String imagesPath = storageProperties.getLandImagesDir(parentId);

        //重新命名前端传过来的文件,用一个大的集合封装这两个重命文件字符串集合
        List<List<String>> allPath = rename(parentId, landFiles, imageFiles);

        //
        if (allPath.get(0).size() > 0) {
            //创建资料文件夹文件夹
            createDirectory(filePath, "资料文件夹");
            //携带文件,存储地址,重命名数据集合开始保存资料文件
            saveFiles(landFiles, filePath, allPath.get(0));
            String code = String.valueOf(new Random().nextInt(899999) + 100000);
            //把用地资料文件夹压缩成压缩包
            try {
                //选择打包文件夹的路径,和打包后存储文件路径和命名
                String zipFileName = "云用地_" + parentId + code + ".zip";
                zipFolder(filePath, storageProperties.getLandZipPath(parentId, zipFileName));
                //打包后把原来的文件夹给删除
                String folderPath = storageProperties.getLandCloudFileDir(parentId);
                deleteFolder(new File(folderPath));
            } catch (IOException e) {
                e.printStackTrace();
            }
            //添加资料文件压缩包名称
            mixedList.add("云用地" + "_" + parentId + code + ".zip");
        } else {
            mixedList.add(0);
        }

        if (allPath.get(1).size() > 0) {
            //创建资料文件夹和图片文件夹
            createDirectory(imagesPath, "图片文件夹");
            //把文件资源保存到创建好的文件夹里面
            saveFiles(imageFiles, imagesPath, allPath.get(1));
            //添加图片路径集合
            mixedList.add(allPath.get(1));
        } else {
            mixedList.add(1);
        }
        return mixedList;
    }

    public void deleteFolder(File folder) {
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteFolder(file);
                }
            }
        }
        if (!folder.delete()) {
            System.out.println("无法删除文件夹: " + folder);
        } else {
            System.out.println("已删除文件夹: " + folder);
        }
    }

    private void createDirectory(String path, String name) {
        File directory = new File(path);
        if (directory.exists()) {
            log.info("{} 目录已存在!", name);
        } else {
            if (directory.mkdirs()) {
                log.info("{} 目录创建成功!", name);
            } else {
                log.error("{} 目录创建失败! {}", name, path);
            }
        }
    }

    private void saveFiles(MultipartFile[] files, String path, List<String> renameFiles) {
        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            if (file.isEmpty()) {
                continue; // 跳过空文件
            }
            try {
                //创建一个destFile对象,并使用renameFiles里面的指定名称
                File destFile = new File(path + File.separator, renameFiles.get(i));
                //把客户端传过来的文件从file复制或移动到destFile所表示的目标位置
                file.transferTo(destFile);
            } catch (IOException e) {
                log.error("文件保存失败: {}", e.getMessage());
            }
        }
    }

    private List<List<String>> rename(Integer parentId, MultipartFile[] landFiles, MultipartFile[] imageFiles) {
        //定义一个大的集合用来封装图片重命名集合和资料重命名集合
        List<List<String>> allArray = new ArrayList<>();
        //资料重命名集合
        List<String> landArray = new ArrayList<>();
        //图片重命名集合
        List<String> imgArray = new ArrayList<>();

        if (landFiles != null) {
            //资料重命名携带参数"Land_"进入重命名方法
            landArray = rename2(parentId, landFiles, "Land_");
        }
        if (imageFiles != null) {
            //图片重命名携带参数"Land_"进入重命名方法
            imgArray = rename2(parentId, imageFiles, "Img_");
        }
        //图片、资料重命名后的集合都封装到大集合里面
        allArray.add(landArray);
        allArray.add(imgArray);
        return allArray;
    }

    private List<String> rename2(Integer parentId, MultipartFile[] cloudLandFiles, String prefix) {
        //定义封装重命名的集合
        List<String> renamedFiles = new ArrayList<>();
        //遍历传入的文件集合
        for (MultipartFile file : cloudLandFiles) {
            String originalFileName = file.getOriginalFilename();
            if (originalFileName == null || originalFileName.isEmpty()) {
                throw new IllegalArgumentException("文件名为空");
            }
            //提取原始文件名的后缀
            String suffixName = originalFileName.substring(originalFileName.lastIndexOf("."));
            //开始重命名
            String code = String.valueOf(new Random().nextInt(899999) + 100000);
            //开始重命名
            String newFileName = "Img_" + parentId + "_" + code + suffixName;
            //把重命名的字符串加入集合
            renamedFiles.add(newFileName);
        }
        return renamedFiles;
    }

    public static void zipFolder(String sourceFolderPath, String zipFilePath) throws IOException {
        FileOutputStream fos = new FileOutputStream(zipFilePath);
        ZipOutputStream zos = new ZipOutputStream(fos);

        File sourceFolder = new File(sourceFolderPath);

        addFolderToZip(sourceFolder, sourceFolder.getName(), zos);

        zos.close();
        fos.close();
    }

    private static void addFolderToZip(File folder, String parentFolder, ZipOutputStream zos) throws IOException {
        File[] files = folder.listFiles();
        byte[] buffer = new byte[1024];

        assert files != null;
        for (File file : files) {
            if (file.isDirectory()) {
                addFolderToZip(file, parentFolder + "/" + file.getName(), zos);
                continue;
            }

            FileInputStream fis = new FileInputStream(file);
            zos.putNextEntry(new ZipEntry(parentFolder + "/" + file.getName()));

            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }

            zos.closeEntry();
            fis.close();
        }
    }
}


