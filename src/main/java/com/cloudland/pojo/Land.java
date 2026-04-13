package com.cloudland.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 
 * </p>
 *
 * @author longlive
 * @since 2023-09-02
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("land")
public class Land implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 名称
     */
    @TableField("land_name")
    private String landName;

    /**
     * 类型
     */
    @TableField("land_type")
    private Integer landType;

    /**
     * 描述
     */
    @TableField("description")
    private String description;

    /**
     * 位置
     */
    @TableField("address")
    private String address;

    /**
     * 排序权重
     */
    @TableField("ordered")
    private Integer ordered;

    /**
     * 价格/平方米/天
     */
    @TableField("price")
    private Double price;

    /**
     * 所有者ID
     */
    @TableField("a_id")
    private Integer aId;

    /**
     * 状态
     */
    @TableField("status")
    private Integer status;

    /**
     * 面积（平方米）
     */
    @TableField("area")
    private Double area;

    /**
     * 土地代理人
     */
    @TableField("employee_id")
    private Integer employeeId;

    /**
     * 详细地址
     */
    @TableField("detailed_address")
    private String detailedAddress;
}
