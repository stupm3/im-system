package com.stupm.service.user.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stupm.service.user.dao.UserDataEntity;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Service;

@Mapper
public interface UserDataMapper extends BaseMapper<UserDataEntity> {

}

