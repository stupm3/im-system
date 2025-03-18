package com.stupm.common.enums;

public enum UrlRouteWayEnum {

    /**
     * 随机
     */
    RAMDOM(1,"com.stupm.common.route.algorithm.random.RandomHandle"),


    /**
     * 1.轮训
     */
    LOOP(2,"com.stupm.common.route.algorithm.loop.LoopHandle"),

    /**
     * HASH
     */
    HASH(3,"com.stupm.common.route.algorithm.consistenthash.ConsistentHashHandle"),
    ;


    private int code;
    private String clazz;


    public static UrlRouteWayEnum getHandler(int ordinal) {
        for (int i = 0; i < UrlRouteWayEnum.values().length; i++) {
            if (UrlRouteWayEnum.values()[i].getCode() == ordinal) {
                return UrlRouteWayEnum.values()[i];
            }
        }
        return null;
    }

    UrlRouteWayEnum(int code, String clazz){
        this.code=code;
        this.clazz=clazz;
    }

    public String getClazz() {
        return clazz;
    }

    public int getCode() {
        return code;
    }
}
