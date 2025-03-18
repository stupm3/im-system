package com.stupm.service.friendship.model.resp;

import lombok.Data;

import java.util.List;

@Data
public class ImportFriendshipResp {
    private List<String> successIds;
    private List<String> errorIds;
}
