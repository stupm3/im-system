package com.stupm.messageStore.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stupm.messageStore.dao.MessageHistoryEntity;
import org.apache.ibatis.annotations.Mapper;


import java.util.Collection;

@Mapper
public interface MessageHistoryMapper extends BaseMapper<MessageHistoryEntity> {

    /**
     * 批量插入（mysql）
     * @param entityList
     * @return
     */
    Integer insertBatchSomeColumn(Collection<MessageHistoryEntity> entityList);
}
