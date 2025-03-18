package com.stupm.service.user.moudles.req;

import com.stupm.common.model.RequestBase;
import com.stupm.service.user.dao.UserDataEntity;
import lombok.Data;

import java.util.List;

@Data
public class ImportUserReq extends RequestBase {
    private List<UserDataEntity> userData;
}
