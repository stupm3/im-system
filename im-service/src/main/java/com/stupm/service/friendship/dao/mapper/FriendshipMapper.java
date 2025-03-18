package com.stupm.service.friendship.dao.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stupm.service.friendship.dao.FriendshipEntity;
import com.stupm.service.friendship.model.req.CheckFriendShipReq;
import com.stupm.service.friendship.model.resp.CheckFriendShipResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FriendshipMapper extends BaseMapper<FriendshipEntity> {

    @Select("<script>"+
            "select from_id as fromId , to_id as toId ,if(status = 1,1,0) as status from im_friendship where from_id = #{fromId} and to_id in " +
            "<foreach collection='toIds' index='index' item='id' separator=',' close = ')' open='(' > " +
            "#{id}" +
            "</foreach>" +
            "</script>")
    public List<CheckFriendShipResp> checkFriendShipSingle(CheckFriendShipReq req);

    @Select("<script>" +
            " select a.fromId,a.toId , ( \n" +
            " case \n" +
            " when a.status = 1 and b.status = 1 then 1 \n" +
            " when a.status = 1 and b.status != 1 then 2 \n" +
            " when a.status != 1 and b.status = 1 then 3 \n" +
            " when a.status != 1 and b.status != 1 then 4 \n" +
            " end \n" +
            " ) \n " +
            " as status from "+
            " (select from_id as fromId , to_id as toId , if(status = 1,1,0) as status from im_friendship where app_id = #{appId} and from_id = #{fromId} AND to_id in " +
            "<foreach collection='toIds' index='index' item='id' separator=',' close=')' open='('>" +
            " #{id} " +
            "</foreach>" +
            " ) as a INNER join" +
            " (select from_id as fromId, to_id as toId , if(status = 1,1,0) as status from im_friendship where app_id = #{appId} and to_id = #{fromId} AND from_id in " +
            "<foreach collection='toIds' index='index' item='id' separator=',' close=')' open='('>" +
            " #{id} " +
            "</foreach>" +
            " ) as b " +
            " on a.fromId = b.toId and b.fromId = a.toId "+
            "</script>"
    )
    public List<CheckFriendShipResp> checkFriendBothWay(CheckFriendShipReq req);

    @Select("<script>"+
            "select from_id as fromId , to_id as toId ,if(black = 1,1,0) as black from im_friendship where from_id = #{fromId} and to_id in " +
            "<foreach collection='toIds' index='index' item='id' separator=',' close = ')' open='(' > " +
            "#{id}" +
            "</foreach>" +
            "</script>")
    public List<CheckFriendShipResp> checkBlackSingle(CheckFriendShipReq req);

    @Select("<script>" +
            " select a.fromId,a.toId , ( \n" +
            " case \n" +
            " when a.black = 1 and b.black = 1 then 1 \n" +
            " when a.black = 1 and b.black != 1 then 2 \n" +
            " when a.black != 1 and b.black = 1 then 3 \n" +
            " when a.black != 1 and b.black != 1 then 4 \n" +
            " end \n" +
            " ) \n " +
            " as status from "+
            " (select from_id as fromId , to_id as toId , if(black = 1,1,0) as black from im_friendship where app_id = #{appId} and from_id = #{fromId} AND to_id in " +
            "<foreach collection='toIds' index='index' item='id' separator=',' close=')' open='('>" +
            " #{id} " +
            "</foreach>" +
            " ) as a INNER join" +
            " (select from_id as fromId, to_id as toId , if(black = 1,1,0) as black from im_friendship where app_id = #{appId} and to_id = #{fromId} AND from_id in " +
            "<foreach collection='toIds' index='index' item='id' separator=',' close=')' open='('>" +
            " #{id} " +
            "</foreach>" +
            " ) as b " +
            " on a.fromId = b.toId and b.fromId = a.toId "+
            "</script>"
    )
    public List<CheckFriendShipResp> checkBlackBothWay(CheckFriendShipReq req);

    @Select("select max(friend_sequence) from im_friendship where app_id = #{appId} and from_id = #{userId}")
    public Long getMaxFriendSeq(Integer appId, String userId);


    @Select("select to_id from im_friendship where app_id = #{appId} and from_id = #{userId}")
    public List<String> getAllFriendsId(Integer appId,String userId);


}
