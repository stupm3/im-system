package com.stupm.service.user.dao;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("im_user_data")
public class UserDataEntity {
    //主键1 用户id
    private String userId;

    //主键2
    private Integer appId;

    //昵称
    private String nickname;

    //密码
    private String password;

    //头像
    private String photo;

    //性别 0女 1男
    private Integer userSex;

    //生日
    private String birthDay;

    //个性签名
    private String selfSignature;

    //好友添加类型
    private Integer friendAllowType;

    //封禁状况 0 被封禁 1 未被封禁
    private Integer forbiddenFlag;

    private Integer type;

    private Integer silentFlag;

    private Integer delFlag;

    private String extra;

}
