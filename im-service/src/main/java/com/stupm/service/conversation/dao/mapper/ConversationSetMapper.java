package com.stupm.service.conversation.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stupm.service.conversation.dao.ConversationSetEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;



@Mapper
public interface ConversationSetMapper extends BaseMapper<ConversationSetEntity> {

    @Update(" update im_conversation_set set read_sequence = #{readSequence},sequence = #{sequence} " +
    " where conversation_id = #{conversationId} and app_id = #{appId} AND read_sequence < #{readSequence}")
    public void readMark(ConversationSetEntity imConversationSetEntity);

    @Select(" select max(sequence) from im_conversation_set where app_id = #{appId} AND from_id = #{userId} ")
    Long getConversationSetMaxSeq(Integer appId, String userId);
}
