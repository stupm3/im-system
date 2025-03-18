package com.stupm.service.user.moudles.resp;


import com.stupm.service.user.dao.UserDataEntity;
import lombok.Data;

import java.util.List;


@Data
public class GetUserInfoResp {

    private List<UserDataEntity> userDataItem;

    private List<String> failUser;


}
