package com.flowring.laleents.model.room;


import com.flowring.laleents.model.HttpAfReturn;
import com.flowring.laleents.model.HttpReturn;
import com.flowring.laleents.model.user.UserControlCenter;
import com.flowring.laleents.tools.CallbackUtils;
import com.flowring.laleents.tools.StringUtils;
import com.flowring.laleents.tools.cloud.api.CloudUtils;
import com.flowring.laleents.tools.phone.AllData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

public class RoomSettingControlCenter {

    public static void SetRoomBg(String Roomid, String filePath) {
        RoomInfoInPhone roomInfoInPhone = AllData.getRoomInPhone(Roomid);
        roomInfoInPhone.bg = filePath;
        AllData.updateRoomInPhone(roomInfoInPhone);
    }

    public static void DelectMsg(String Roomid, CallbackUtils.ReturnHttp callback) {
        new Thread(() -> {
            HttpReturn httpReturn = CloudUtils.iCloudUtils.delRecord(Roomid);
            callback.Callback(httpReturn);

        }).start();


    }

    public static void getRoomSetting(String Roomid, CallbackUtils.ReturnData<RoomSetting> callback) {

        CloudUtils.iCloudUtils.getRoomSetting(Roomid, callback, RoomSetting.class);

    }

    public static void getRoomMembers(String Roomid, CallbackUtils.ReturnData<ArrayList<UserInRoom>> callback) {
        new Thread(() -> {
            HttpReturn httpReturn = CloudUtils.iCloudUtils.getRoomMembers(Roomid);
            if (httpReturn.status == 200) {
                ArrayList<UserInRoom> friendInfos = new Gson().fromJson(httpReturn.data.toString(), new TypeToken<ArrayList<UserInRoom>>() {
                }.getType());

                callback.Callback(httpReturn.status == 200, httpReturn.msg, friendInfos);

            } else
                callback.Callback(httpReturn.status == 200, httpReturn.msg, new ArrayList<UserInRoom>());
        }).start();

    }

    public static ArrayList<UserInRoom> getGroupMembers(String Roomid) {

        HttpReturn httpReturn = CloudUtils.iCloudUtils.getGroupMembers(Roomid);
        if (httpReturn.status == 200) {
            ArrayList<UserInRoom> friendInfos = new Gson().fromJson(httpReturn.data.toString(), new TypeToken<ArrayList<UserInRoom>>() {
            }.getType());
            return friendInfos;


        } else
            return null;
    }

    public static ArrayList<UserInRoom> getRoomMembers(String Roomid) {

        HttpReturn httpReturn = CloudUtils.iCloudUtils.getRoomMembers(Roomid);
        StringUtils.HaoLog("getRoomMembers 1=", httpReturn);
        if (httpReturn.status == 200) {
            ArrayList<UserInRoom> friendInfos = new Gson().fromJson(httpReturn.data.toString(), new TypeToken<ArrayList<UserInRoom>>() {
            }.getType());
            String[] userIds = new String[friendInfos.size()];
            for (int i = 0; i < userIds.length; i++) {
                userIds[i] = friendInfos.get(i).userId;
            }
            HttpAfReturn httpReturn2 = CloudUtils.iCloudUtils.orgtreeuserimage(UserControlCenter.getUserMinInfo().eimUserData.af_url, userIds);
            StringUtils.HaoLog("getRoomMembers=", httpReturn2);
            if (httpReturn2.code == 200) {
                try {
                    JSONObject jsonObject = new JSONObject(new Gson().toJson(httpReturn2.data));
                    for (int i = 0; i < userIds.length; i++) {
                        friendInfos.get(i).avatarUrl = jsonObject.optString(friendInfos.get(i).userId);
                        friendInfos.get(i).avatarThumbnailUrl = jsonObject.optString(friendInfos.get(i).userId);
                        friendInfos.get(i).avatar = jsonObject.optString(friendInfos.get(i).userId);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
            return friendInfos;


        } else
            return null;
    }

    public static void setBg(String groupId, File file, CallbackUtils.ReturnHttp callback) {
        new Thread(() -> {
            HttpReturn h = CloudUtils.iCloudUtils.updateGroupBackground(groupId, file);
            if (h != null) {
                callback.Callback(h);
            } else
                callback.Callback(new HttpReturn());
        }).start();

    }

    public static void setStatus(RoomMinInfo roomMinInfo, int status, CallbackUtils.ReturnHttp callback) {
        new Thread(() -> {
            try {

                if (roomMinInfo.groupId == null || roomMinInfo.groupId.isEmpty()) {
                    HttpReturn h = CloudUtils.iCloudUtils.updateRoom(roomMinInfo.id, new JSONObject().put("roomId", roomMinInfo.id).put("status", status));
                    callback.Callback(h);
                } else {
                    HttpReturn h = CloudUtils.iCloudUtils.updateGroup(roomMinInfo.groupId, new JSONObject().put("groupId", roomMinInfo.groupId).put("roomId", roomMinInfo.id).put("status", status));
                    callback.Callback(h);
                }

            } catch (JSONException e) {
                e.printStackTrace();
                callback.Callback(new HttpReturn());
            }
        }).start();

    }

    public static void leaveGroup(String roomId, String groupId, CallbackUtils.ReturnHttp callback) {
        new Thread(() -> {
            HttpReturn h = CloudUtils.iCloudUtils.delGroupMember(roomId, groupId, new String[]{UserControlCenter.getUserMinInfo().userId});
            if (h != null) {
                callback.Callback(h);
            } else
                callback.Callback(new HttpReturn());
        }).start();
    }

    public static void setHead(String groupId, File file, CallbackUtils.ReturnHttp callback) {

        new Thread(() -> {
            HttpReturn h = CloudUtils.iCloudUtils.updateGroupAvatar(groupId, file);
            if (h != null) {
                callback.Callback(h);
            } else
                callback.Callback(new HttpReturn());
        }).start();
    }

    public static void setShowName(String groupId, Boolean showName, CallbackUtils.ReturnHttp callback) {
        new Thread(() -> {
            try {
                HttpReturn h = CloudUtils.iCloudUtils.updateGroup(groupId, new JSONObject().put("groupId", groupId).put("isShowDisplayName", showName));
                if (h != null) {
                    callback.Callback(h);
                } else
                    callback.Callback(new HttpReturn());
            } catch (JSONException e) {
                e.printStackTrace();
                callback.Callback(new HttpReturn());
            }
        }).start();
    }

    public static void setName(String roomId, String groupId, String Name, CallbackUtils.ReturnHttp callback) {
        new Thread(() -> {
            try {
                HttpReturn h = CloudUtils.iCloudUtils.updateGroupSetting(groupId, new JSONObject().put("name", Name).put("roomId", roomId).put("groupId", groupId));
                if (h != null) {
                    callback.Callback(h);
                } else
                    callback.Callback(new HttpReturn());
            } catch (JSONException e) {
                e.printStackTrace();
                callback.Callback(new HttpReturn());
            }
        }).start();
    }

    public static void addGroupMember(String roomId, String groupId, ArrayList<String> userArrayList, CallbackUtils.ReturnHttp callback) {
        new Thread(() -> {
            String[] user = new String[userArrayList.size()];
            userArrayList.toArray(user);
            HttpReturn h = CloudUtils.iCloudUtils.addGroupMember(roomId, groupId, user);
            if (h != null) {
                callback.Callback(h);
            } else
                callback.Callback(new HttpReturn());

        }).start();
    }

    public static void delGroupMember(String roomId, String groupId, ArrayList<String> userArrayList, CallbackUtils.ReturnHttp callback) {
        new Thread(() -> {
            String[] user = new String[userArrayList.size()];
            userArrayList.toArray(user);
            HttpReturn h = CloudUtils.iCloudUtils.delGroupMember(roomId, groupId, user);
            if (h != null) {
                callback.Callback(h);
            } else
                callback.Callback(new HttpReturn());

        }).start();
    }

    public static void addGroupByQrcode(String groupId, String verificationCode, CallbackUtils.ReturnHttp callback) {
        new Thread(() -> {
            HttpReturn h = CloudUtils.iCloudUtils.gotoGroup(groupId, verificationCode);
            callback.Callback(h);
        }).start();
    }

    public static void setIntro(String roomId, String groupId, String intro, CallbackUtils.ReturnHttp callback) {
        StringUtils.HaoLog("roomId=" + roomId + " groupId=" + groupId + " intro=" + intro);
        new Thread(() -> {
            try {
                HttpReturn h = CloudUtils.iCloudUtils.updateGroupSetting(groupId, new JSONObject().put("intro", intro).put("roomId", roomId).put("groupId", groupId));
                callback.Callback(h);
            } catch (JSONException e) {
                e.printStackTrace();
                callback.Callback(new HttpReturn());
            }
        }).start();
    }

    public static void getGroupSetting(String groupId, CallbackUtils.ReturnData<GroupSetting> callback) {
        new Thread(() -> {

            CloudUtils.CloundTask(CloudUtils.iCloudUtils.getGroupSetting(groupId), callback, GroupSetting.class);
        }).start();

    }

    public static void getGroupInfo(String groupId, CallbackUtils.ReturnData<GroupInfo> callback) {
        new Thread(() -> {
            CloudUtils.CloundTask(CloudUtils.iCloudUtils.getGroupInfo(groupId), callback, GroupInfo.class);

        }).start();
    }


}