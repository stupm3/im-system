package com.stupm.service.group.service;

import com.stupm.common.ResponseVO;
import com.stupm.common.model.SyncReq;
import com.stupm.service.group.dao.GroupEntity;
import com.stupm.service.group.model.req.*;

import javax.validation.Valid;

public interface GroupService {
    ResponseVO importGroup(@Valid ImportGroupReq importGroupReq);

    ResponseVO<GroupEntity> getGroup(String groupId , Integer appId);

    ResponseVO updateGroupInfo(UpdateGroupReq req);

    ResponseVO createGroup(CreateGroupReq req);

    ResponseVO getGroupInfo(GetGroupInfoReq req);

    ResponseVO getJoinedGroup(GetJoinedGroupReq req);

    ResponseVO destroyGroup(DestroyGroupReq req);

    ResponseVO transferGroup(TransferGroupReq req);

    ResponseVO muteGroup(MuteGroupReq req);

    ResponseVO syncJoinedGroup(SyncReq req);

    Long getMaxGroupSeq(String userId, Integer appId);
}
