package com.stupm.message.codec.pack.group;

import lombok.Data;


@Data
public class GroupMemberSpeakPack {

    private String groupId;

    private String memberId;

    private Long speakDate;

}
