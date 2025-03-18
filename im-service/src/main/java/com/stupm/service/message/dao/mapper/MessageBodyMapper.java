package com.stupm.service.message.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stupm.service.message.dao.MessageBodyEntity;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
public interface MessageBodyMapper extends BaseMapper<MessageBodyEntity> {
}
