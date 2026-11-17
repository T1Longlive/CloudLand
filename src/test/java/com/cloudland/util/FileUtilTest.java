package com.cloudland.util;

import com.cloudland.config.StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 文件落盘/打包/删除契约测试。
 *
 * <p>用 {@code @TempDir} 把存储根目录隔离到临时目录，不碰真实的 D:/CloudLandFile，
 * 因此可以在 CI/Docker 构建里安全运行。
 *
 * <p>已知 quirks（只记录、不当作正确契约去断言）：
 * {@code rename2} 接收 prefix 参数却未使用，土地资料文件也会在 zip 内被命名为
 * {@code Img_} 前缀。这里只断言"zip 内存在资料条目"，命名前缀的正确性留待产品决策。
 */
class FileUtilTest {

    @TempDir
    Path tempDir;

    private StorageProperties storageProperties;
    private FileUtil fileUtil;

    @BeforeEach
    void setUp() {
        storageProperties = new StorageProperties();
        storageProperties.setLocation(tempDir.toString());
        fileUtil = new FileUtil();
        ReflectionTestUtils.setField(fileUtil, "storageProperties", storageProperties);
    }

    private MockMultipartFile file(String name, String originalName, String content) {
        return new MockMultipartFile(name, originalName, "application/octet-stream", content.getBytes());
    }

    @Test
    @DisplayName("头像重命名为 userIcon_{UUID} 并落盘到 UserIcon 目录")
    void userIconIsRenamedAndSaved() {
        MockMultipartFile icon = file("userIcon", "avatar.PNG", "png-bytes");

        List<Object> result = fileUtil.defineDirectory(null, null, null, icon, null);

        assertThat(result).hasSize(1);
        String savedName = (String) result.get(0);
        assertThat(savedName).startsWith("userIcon_").endsWith(".PNG");
        assertThat(Paths.get(storageProperties.getUserIconPath(savedName))).exists();
    }

    @Test
    @DisplayName("产品图重命名为 product_{UUID} 并落盘到 Product 目录")
    void productImageIsRenamedAndSaved() {
        MockMultipartFile img = file("productImg", "p.jpg", "jpg-bytes");

        List<Object> result = fileUtil.defineDirectory(null, null, null, null, img);

        String savedName = (String) result.get(0);
        assertThat(savedName).startsWith("product_").endsWith(".jpg");
        assertThat(Paths.get(storageProperties.getProductPath(savedName))).exists();
    }

    @Test
    @DisplayName("原始文件名为空时明确拒绝，而不是拼出坏路径")
    void emptyOriginalFilenameIsRejected() {
        MockMultipartFile icon = file("userIcon", "", "x");

        assertThatThrownBy(() -> fileUtil.defineDirectory(null, null, null, icon, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("土地资料打包成 zip、图片逐张落盘，临时资料目录被清理")
    void landFilesAreZippedAndImagesSaved() throws Exception {
        MockMultipartFile[] landFiles = {file("landFiles", "合同.pdf", "pdf-bytes")};
        MockMultipartFile[] imageFiles = {
                file("imageFiles", "a.jpg", "a"),
                file("imageFiles", "b.png", "b"),
        };

        List<Object> result = fileUtil.defineDirectory(7, landFiles, imageFiles, null, null);

        String zipName = (String) result.get(0);
        assertThat(zipName).startsWith("云用地_7").endsWith(".zip");
        Path zipPath = Paths.get(storageProperties.getLandZipPath(7, zipName));
        assertThat(zipPath).exists();

        @SuppressWarnings("unchecked")
        List<String> images = (List<String>) result.get(1);
        assertThat(images).hasSize(2);
        for (String image : images) {
            assertThat(image).startsWith("Img_7_");
            assertThat(Paths.get(storageProperties.getLandImagesDir(7), image)).exists();
        }

        assertThat(Paths.get(storageProperties.getLandCloudFileDir(7))).doesNotExist();

        try (ZipFile zip = new ZipFile(zipPath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            assertThat(entries.hasMoreElements()).isTrue();
            ZipEntry entry = entries.nextElement();
            assertThat(entry.getName()).startsWith("CloudLandFile/");
        }
    }

    @Test
    @DisplayName("只传图片时不产生 zip，哨兵值 0 占位")
    void imagesOnlyProducesNoZip() {
        MockMultipartFile[] imageFiles = {file("imageFiles", "a.jpg", "a")};

        List<Object> result = fileUtil.defineDirectory(7, null, imageFiles, null, null);

        assertThat(result.get(0)).isEqualTo(0);
        assertThat((List<?>) result.get(1)).hasSize(1);
        assertThat(Paths.get(storageProperties.getLandDir(7), "Images")).exists();
    }

    @Test
    @DisplayName("无任何文件时返回哨兵值 0/1，不创建目录")
    void noFilesReturnsSentinels() {
        List<Object> result = fileUtil.defineDirectory(7, null, null, null, null);

        assertThat(result.get(0)).isEqualTo(0);
        assertThat(result.get(1)).isEqualTo(1);
        assertThat(Paths.get(storageProperties.getLandDir(7))).doesNotExist();
    }

    @Test
    @DisplayName("空文件数组等价于无文件，返回哨兵值")
    void emptyArraysReturnSentinels() {
        List<Object> result = fileUtil.defineDirectory(7, new MockMultipartFile[0], new MockMultipartFile[0], null, null);

        assertThat(result.get(0)).isEqualTo(0);
        assertThat(result.get(1)).isEqualTo(1);
    }

    @Test
    @DisplayName("deleteFolder 递归删除整棵目录树")
    void deleteFolderRecurses() throws Exception {
        Path tree = tempDir.resolve("tree");
        Files.createDirectories(tree.resolve("sub"));
        Files.write(tree.resolve("sub").resolve("a.txt"), "a".getBytes());
        Files.write(tree.resolve("b.txt"), "b".getBytes());

        fileUtil.deleteFolder(tree.toFile());

        assertThat(tree).doesNotExist();
    }

    @Test
    @DisplayName("deleteFolder 也能删除单个文件")
    void deleteFolderHandlesSingleFile() throws Exception {
        Path single = tempDir.resolve("single.txt");
        Files.write(single, "x".getBytes());

        fileUtil.deleteFolder(single.toFile());

        assertThat(single).doesNotExist();
    }
}