package com.stupm.service.user.service;

import com.stupm.common.ResponseVO;
import com.stupm.service.user.dao.UserDataEntity;
import com.stupm.service.user.moudles.req.*;
import com.stupm.service.user.moudles.resp.GetUserInfoResp;


public interface UserService {
    ResponseVO importUser(ImportUserReq req);

    ResponseVO<GetUserInfoResp> getUserInfo(GetUserInfoReq req);

    ResponseVO<UserDataEntity> getSingleUserInfo(String userId , Integer appId);

    ResponseVO deleteUser(DeleteUserReq req);

    ResponseVO modifyUserInfo(ModifyUserInfoReq req);

    ResponseVO login(LoginReq req);

    ResponseVO getUserSequence(GetUserSequenceReq req);
}
