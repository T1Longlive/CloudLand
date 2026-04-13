package com.cloudland.pojo.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class ProductVO {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String productName;

    private String description;

    private Integer ordered;

    private Double price;

    private Integer status;

    private Double num;

    private String img;
    /**
     * 用地联系人
     */
    private String customerAUsername;

    private Integer aId;
    /**
     * 联系电话
     */
    private String customerAPhone;
}
