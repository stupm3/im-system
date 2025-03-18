package com.stupm.common.enums;

public enum RouteHashMethodEnum {

    TREE(1,"com.stupm.common.route.algorithm.consistenthash" +
            ".TreeMapConsistentHash"),


    CUSTOMER(2,"com.stupm.common.route.algorithm.consistenthash.xxxx"),

    ;


    private int code;
    private String clazz;


    public static RouteHashMethodEnum getHandler(int ordinal) {
        for (int i = 0; i < RouteHashMethodEnum.values().length; i++) {
            if (RouteHashMethodEnum.values()[i].getCode() == ordinal) {
                return RouteHashMethodEnum.values()[i];
            }
        }
        return null;
    }

    RouteHashMethodEnum(int code, String clazz){
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
