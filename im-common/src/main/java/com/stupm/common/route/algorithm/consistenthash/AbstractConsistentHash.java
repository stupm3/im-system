package com.stupm.common.route.algorithm.consistenthash;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public abstract class AbstractConsistentHash {
    //add
    protected abstract void add(Long key , String value);

    //get
    protected abstract String getFirstNodeValue( String value);

    //sort
    protected void sort(){

    }
    //hash
    public Long hash(String value){
        //初始化md5
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not supported", e);
        }
        md5.reset();
        //设置哈希对象的字符集
        byte[] keyBytes = null;
        try {
            keyBytes = value.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unknown string :" + value, e);
        }

        //生成md5哈希
        md5.update(keyBytes);
        byte[] digest = md5.digest();

        // 将哈希码组合成32位的整数
        long hashCode = ((long) (digest[3] & 0xFF) << 24)
                | ((long) (digest[2] & 0xFF) << 16)
                | ((long) (digest[1] & 0xFF) << 8)
                | (digest[0] & 0xFF);
        // 将hashcode的高32位清零
        long truncateHashCode = hashCode & 0xffffffffL;
        return truncateHashCode;
    }

    protected abstract void processBefore();

    public synchronized String process(List<String> values, String key){
        processBefore();
        for (String value : values) {
            add(hash(value), value);
        }
        sort();
        return getFirstNodeValue(key) ;
    }


}
