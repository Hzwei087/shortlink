package com.nageoffer.shortlink.project.common.database;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.util.Date;

@Data
public class BaseDO {
    /**
     * id
     */
    private Long id;
    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 修改时间
     */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    private Date updateTime;

    /**
     * 删除标识 0：未删除 1:已删除
     */
    @TableField(value = "del_flag", fill = FieldFill.INSERT)
    private Integer delFlag;
}
