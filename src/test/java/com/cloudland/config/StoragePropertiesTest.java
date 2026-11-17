package com.cloudland.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 存储路径契约测试。
 *
 * <p>StorageProperties 是全部文件落盘/静态映射/导出路径的唯一来源，
 * 它的归一化与拼接规则一旦变化，上传、下载、资源映射会一起错位。
 * 纯函数、零依赖，适合放在最快门禁里。
 *
 * <p>断言用 {@code Paths.get(...).toString()} 构造期望值，
 * 因为 resolve 的结果带平台分隔符（Windows 为反斜杠），写死 "/" 会在本机上误红。
 */
class StoragePropertiesTest {

    private StorageProperties props(String location) {
        StorageProperties properties = new StorageProperties();
        properties.setLocation(location);
        return properties;
    }

    @Test
    @DisplayName("反斜杠归一化为正斜杠，尾部斜杠被去掉")
    void normalizesSeparatorsAndTrailingSlashes() {
        assertThat(props("D:\\CloudLandFile\\").getLocation()).isEqualTo("D:/CloudLandFile");
        assertThat(props("/tmp/root///").getLocation()).isEqualTo("/tmp/root");
    }

    @Test
    @DisplayName("null location 保持 null，不抛异常")
    void nullLocationStaysNull() {
        assertThat(props(null).getLocation()).isNull();
    }

    @Test
    @DisplayName("头像目录与文件路径拼接正确")
    void userIconPaths() {
        StorageProperties properties = props("/tmp/root");

        assertThat(properties.getUserIconDir())
                .isEqualTo(Paths.get("/tmp/root", "UserIcon").toString());
        assertThat(properties.getUserIconPath("a.png"))
                .isEqualTo(Paths.get("/tmp/root", "UserIcon", "a.png").toString());
    }

    @Test
    @DisplayName("产品目录与文件路径拼接正确")
    void productPaths() {
        StorageProperties properties = props("/tmp/root");

        assertThat(properties.getProductDir())
                .isEqualTo(Paths.get("/tmp/root", "Product").toString());
        assertThat(properties.getProductPath("p.jpg"))
                .isEqualTo(Paths.get("/tmp/root", "Product", "p.jpg").toString());
    }

    @Test
    @DisplayName("土地目录族：根目录 / 资料临时目录 / 图片目录 / zip 路径")
    void landDirectoryFamily() {
        StorageProperties properties = props("/tmp/root");

        assertThat(properties.getLandDir(3))
                .isEqualTo(Paths.get("/tmp/root", "LandFile", "Land_3").toString());
        assertThat(properties.getLandCloudFileDir(3))
                .isEqualTo(Paths.get("/tmp/root", "LandFile", "Land_3", "CloudLandFile").toString());
        assertThat(properties.getLandImagesDir(3))
                .isEqualTo(Paths.get("/tmp/root", "LandFile", "Land_3", "Images").toString());
        assertThat(properties.getLandZipPath(3, "云用地_3.zip"))
                .isEqualTo(Paths.get("/tmp/root", "LandFile", "Land_3", "云用地_3.zip").toString());
    }

    @Test
    @DisplayName("导出文件名按类型区分用地/产品订单")
    void exportNamesFollowType() {
        StorageProperties properties = props("/tmp/root");

        assertThat(properties.getExportPath(0))
                .isEqualTo(Paths.get("/tmp/root", "云用地_用地订单.xlsx").toString());
        assertThat(properties.getExportPath(1))
                .isEqualTo(Paths.get("/tmp/root", "云用地_产品订单.xlsx").toString());
    }

    @Test
    @DisplayName("根目录文件路径用于 Excel 下载")
    void rootFilePath() {
        assertThat(props("/tmp/root").getRootFilePath("a.xlsx"))
                .isEqualTo(Paths.get("/tmp/root", "a.xlsx").toString());
    }
}