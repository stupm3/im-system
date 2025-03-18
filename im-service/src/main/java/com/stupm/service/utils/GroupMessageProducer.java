package com.stupm.service.utils;

import com.alibaba.fastjson.JSONObject;
import com.stupm.common.ClientType;
import com.stupm.common.enums.command.Command;
import com.stupm.common.enums.command.GroupEventCommand;
import com.stupm.common.model.ClientInfo;
import com.stupm.message.codec.pack.group.AddGroupMemberPack;
import com.stupm.message.codec.pack.group.RemoveGroupMemberPack;
import com.stupm.message.codec.pack.group.UpdateGroupMemberPack;
import com.stupm.service.group.model.dto.GroupMemberDTO;
import com.stupm.service.group.service.GroupMemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GroupMessageProducer {
    @Autowired
    private MessageProducer producer;

    @Autowired
    private GroupMemberService groupMemberService;

    public void producer(String userId, Command command, Object data, ClientInfo clientInfo){
        JSONObject json = (JSONObject)JSONObject.toJSON(data);
        String groupId = json.getString("groupId");
        List<String> groupMemberId = groupMemberService.getGroupMemberId(groupId, clientInfo.getAppId());

        if(command.equals(GroupEventCommand.ADDED_MEMBER)){
            List<GroupMemberDTO> groupManager = groupMemberService.getGroupManager(groupId, clientInfo.getAppId());
            AddGroupMemberPack pack = json.toJavaObject(json, AddGroupMemberPack.class);
            List<String> members = pack.getMembers();
            groupManager.forEach(member -> {
                if(clientInfo.getClientType() != ClientType.WEB.getCode()){
                    producer.sendToUserAnotherClient(member.getMemberId(),command,pack,clientInfo);
                }else{
                    producer.sendToUser(clientInfo.getAppId(),member.getMemberId(),command,pack);
                }
            });
            for(String member : members){
                if(clientInfo.getClientType() != ClientType.WEB.getCode()){
                    producer.sendToUserAnotherClient(member,command,pack,clientInfo);
                }else{
                    producer.sendToUser(clientInfo.getAppId(),member,command,pack);
                }
            }

        }else if(command.equals(GroupEventCommand.DELETED_MEMBER)){
            RemoveGroupMemberPack pack = json.toJavaObject(RemoveGroupMemberPack.class);
            String member = pack.getMember();
            List<String> groupMemberId1 = groupMemberService.getGroupMemberId(groupId, clientInfo.getAppId());
            groupMemberId1.add(member);
            for(String mb : groupMemberId1){
                if(clientInfo.getClientType() != ClientType.WEB.getCode()){
                    producer.sendToUserAnotherClient(mb,command,pack,clientInfo);
                }else{
                    producer.sendToUser(clientInfo.getAppId(),mb,command,pack);
                }
            }
        }else if(command.equals(GroupEventCommand.UPDATED_MEMBER)){
            UpdateGroupMemberPack pack = json.toJavaObject(UpdateGroupMemberPack.class);
            String memberId = pack.getMemberId();
            List<GroupMemberDTO> groupManager = groupMemberService.getGroupManager(groupId, clientInfo.getAppId());
            GroupMemberDTO groupMemberDTO = new GroupMemberDTO();
            groupMemberDTO.setMemberId(memberId);
            groupManager.add(groupMemberDTO);
            for(GroupMemberDTO member : groupManager){
                if(clientInfo.getClientType() != ClientType.WEB.getCode()){
                    producer.sendToUserAnotherClient(member.getMemberId(),command,pack,clientInfo);
                }else{
                    producer.sendToUser(clientInfo.getAppId(),member.getMemberId(),command,pack);
                }
            }
        }else{
            for(String memberId : groupMemberId){
                if(clientInfo.getClientType() != null && clientInfo.getClientType().equals(clientInfo.getClientType()) && memberId.equals(userId)){
                    producer.sendToUserAnotherClient(memberId,command,data,clientInfo);
                }
                producer.sendToUser(clientInfo.getAppId(),memberId,command,data);
            }
        }




    }

}
