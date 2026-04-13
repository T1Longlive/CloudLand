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
@TableName("user")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableField("username")
    private String username;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("password")
    private String password;

    @TableField("age")
    private Integer age;

    @TableField("address")
    private String address;

    @TableField("status")
    private Integer status;

    @TableField("img")
    private String img;

    @TableField("power")
    private Integer power;

    @TableField("phone")
    private String phone;

    @TableField("mail")
    private String mail;

    @TableField("debt")
    private Double debt;

    /**
     * 详细地址
     */
    @TableField("detailed_address")
    private String detailedAddress;
}
