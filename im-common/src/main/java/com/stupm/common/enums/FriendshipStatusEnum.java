package com.stupm.common.enums;

public enum FriendshipStatusEnum {
    /**
     * 0未添加 1正常 2删除
     */
    FRIEND_STATUS_NO_FRIEND(0),

    FRIEND_STATUS_NORMAL(1),

    FRIEND_STATUS_DELETE(2),

    /**
     * 0未添加 1未拉黑 2拉黑
     */
    BLACK_STATUS_NORMAL(1),

    BLACK_STATUS_BLACKED(2),
    ;


    private int code;


    FriendshipStatusEnum(int code){
        this.code=code;
    }

    public int getCode() {
        return code;
    }
}
