package com.cloudland.pojo.vo;

import com.cloudland.pojo.CloudLandFile;
import lombok.Data;

import java.util.List;

@Data
public class LandVO {
    /**
     * ID
     */
    private Integer id;
    /**
     * 名称
     */
    private String landName;
    /**
     * 类型
     */
    private Integer landType;
    /**
     * 描述
     */
    private String description;
    /**
     * 位置
     */
    private String address;
    /**
     * 资料文件
     */
    private CloudLandFile landFiles;
    /**
     * 图片文件
     */
    private List<CloudLandFile> imageFiles;
    /**
     * 排序权重
     */
    private Integer ordered;
    /**
     * 价格/平方米/天
     */
    private Double price;
    /**
     * 所有者ID
     */
    private Integer aId;
    /**
     * 状态
     */
    private Integer status;
    /**
     * 面积（平方米）
     */
    private Double area;
    /**
     * 土地代理人
     */
    private Integer employeeId;
    /**
     * 详细地址
     */
    private String detailedAddress;
    /**
     * 土地类型名
     */
    private String typeName;
    /**
     * 土地类型描述
     */
    private String typeDescription;
    /**
     * 用地联系人
     */
    private String customerAUsername;
    /**
     * 联系电话
     */
    private String customerAPhone;
    /**
     * 用地联系人
     */
    private String employeeUsername;
    /**
     * 联系电话
     */
    private String employeePhone;
}
