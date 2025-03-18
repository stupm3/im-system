package com.stupm.messageStore.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stupm.messageStore.dao.MessageBodyEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageBodyMapper extends BaseMapper<MessageBodyEntity> {
}
