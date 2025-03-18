package com.stupm.common.route.algorithm.consistenthash;

import com.stupm.common.enums.UserErrorCode;
import com.stupm.common.exception.ApplicationException;

import java.util.SortedMap;
import java.util.TreeMap;

public class TreeMapConsistentHash extends AbstractConsistentHash {

    private TreeMap<Long , String> treeMap = new TreeMap<>();

    private static final int NODE_SIZE = 2;

    @Override
    protected void add(Long key, String value) {
        for(int i = 0 ; i < NODE_SIZE ; i++){
            treeMap.put(super.hash("node"+key+i), value);
        }
        treeMap.put(key, value);
    }

    @Override
    protected String getFirstNodeValue(String value) {
        Long hash = super.hash(value);
        SortedMap<Long, String> last = treeMap.tailMap(hash);
        if(!last.isEmpty()){
            return last.get(last.firstKey());
        }
        if(treeMap.size() == 0){
            throw new ApplicationException(UserErrorCode.SERVER_NOT_AVAILABLE);
        }
        return treeMap.firstEntry().getValue();

    }

    @Override
    protected void processBefore() {
        treeMap.clear();
    }
}
