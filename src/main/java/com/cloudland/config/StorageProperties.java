package com.cloudland.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@ConfigurationProperties(prefix = "access-file")
public class StorageProperties {
    private String location;

    public String getLocation() {
        return normalize(location);
    }

    public void setLocation(String location) {
        this.location = normalize(location);
    }

    public String getUserIconDir() {
        return resolve("UserIcon");
    }

    public String getUserIconPath(String fileName) {
        return resolve("UserIcon", fileName);
    }

    public String getProductDir() {
        return resolve("Product");
    }

    public String getProductPath(String fileName) {
        return resolve("Product", fileName);
    }

    public String getLandBaseDir() {
        return resolve("LandFile");
    }

    public String getLandDir(Integer landId) {
        return resolve("LandFile", "Land_" + landId);
    }

    public String getLandCloudFileDir(Integer landId) {
        return resolve("LandFile", "Land_" + landId, "CloudLandFile");
    }

    public String getLandImagesDir(Integer landId) {
        return resolve("LandFile", "Land_" + landId, "Images");
    }

    public String getLandZipPath(Integer landId, String zipFileName) {
        return resolve("LandFile", "Land_" + landId, zipFileName);
    }

    public String getExportPath(Integer type) {
        return resolve(type == 0 ? "云用地_用地订单.xlsx" : "云用地_产品订单.xlsx");
    }

    public String getRootFilePath(String fileName) {
        return resolve(fileName);
    }

    private String resolve(String... segments) {
        Path basePath = Paths.get(getLocation());
        for (String segment : segments) {
            basePath = basePath.resolve(segment);
        }
        return basePath.toString();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.replace('\\', '/').replaceAll("/+$", "");
    }
}
