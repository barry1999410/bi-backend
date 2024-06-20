package com.yupi.springbootinit.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户
 * @TableName user
 */
@TableName(value ="user")
@Data
public class User implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 电话
     */
    private String userPhone;

    /**
     * 用户账号
     */
    private String userAccount;

    /**
     * 头像
     */
    private String userAvatar;

    /**
     * 用户密码
     */
    private String userPassword;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 0 无删除 1删除
     */
    @TableLogic
    private Integer isDelete;

    /**
     * 用户角色: 0 普通用户 1 管理员
     */
    private String userRole;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
