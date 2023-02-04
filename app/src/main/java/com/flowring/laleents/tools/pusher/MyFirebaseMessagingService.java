package com.flowring.laleents.tools.pusher;

import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE;
import static android.app.PendingIntent.FLAG_CANCEL_CURRENT;
import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.app.PendingIntent.FLAG_MUTABLE;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static com.flowring.laleents.model.msg.MsgControlCenter.receiveMsg;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.service.notification.StatusBarNotification;

import androidx.core.app.NotificationCompat;

import com.flowring.laleents.R;
import com.flowring.laleents.model.msg.MessageInfo;
import com.flowring.laleents.model.msg.MsgControlCenter;
import com.flowring.laleents.model.notifi.workNotifi;
import com.flowring.laleents.model.room.RoomControlCenter;
import com.flowring.laleents.model.room.RoomMinInfo;
import com.flowring.laleents.model.room.RoomSettingControlCenter;
import com.flowring.laleents.model.room.UserInRoom;
import com.flowring.laleents.model.user.UserControlCenter;
import com.flowring.laleents.tools.CommonUtils;
import com.flowring.laleents.tools.FileUtils;
import com.flowring.laleents.tools.StringUtils;
import com.flowring.laleents.tools.phone.AllData;
import com.flowring.laleents.tools.phone.LocalBroadcastControlCenter;
import com.flowring.laleents.ui.main.webBody.MainWebActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Comparator;
import java.util.Date;

public class MyFirebaseMessagingService extends FirebaseMessagingService {


    private static final String TAG = MyFirebaseMessagingService.class.getSimpleName();
    private boolean isShowRoom = false;
    static int afid = 5000;

    @Override
    public void onDeletedMessages() {
        super.onDeletedMessages();

    }

    public void onMessageReceived(RemoteMessage remoteMessage) {

        if (AllData.context == null)
            AllData.context = getApplicationContext();
        if (UserControlCenter.getUserMinInfo() != null) {
            if (UserControlCenter.getUserMinInfo().eimUserData.isLaleAppEim) {
                if (AllData.dbHelper == null)
                    AllData.initSQL(UserControlCenter.getUserMinInfo().userId);
                if (remoteMessage.getData() != null) {
                    MessageInfo messageInfo = receiveMsg(remoteMessage.getData().get("body"), MsgControlCenter.Source.notifi);
                    StringUtils.HaoLog("messageInfo result1=" + messageInfo);
                    //判斷是否在前景
                    boolean isAppForeground = CommonUtils.foregrounded();

//                    if (messageInfo.is_lale_call_group_status()) {
//
//                    } else
                        if (messageInfo.is_lale_call_request()) {

                    } else if (messageInfo.is_lale_call_response()) {
                        if (messageInfo.getCallRequest().result.equals("unavailable")) {
                            sendNotification(messageInfo, remoteMessage.getData().get("body"));
                        } else if (messageInfo.getCallRequest().result.equals("cancel")) {
                            sendNotification(messageInfo, remoteMessage.getData().get("body"));
                        } else if ((!messageInfo.isGroup()) && messageInfo.getCallRequest().result.equals("reject")) {
                            sendNotification(messageInfo, remoteMessage.getData().get("body"));
                        }
                    } else {
                            StringUtils.HaoLog("是否在前景"+isAppForeground);
                            if(messageInfo != null)
                            {    if (!isAppForeground ) {
                            sendNotification(messageInfo, remoteMessage.getData().get("body"));
                        }else
                        {
                            LocalBroadcastControlCenter.send(this, LocalBroadcastControlCenter.ACTION_NOTIFI_AF, remoteMessage.getData().get("body"));
                        }
                            }
                    }


                }
            } else {
                boolean isAppForeground = CommonUtils.foregrounded();
                StringUtils.HaoLog("是否在前景"+isAppForeground);
                if (!isAppForeground) {
                    sendNoChatNotification(remoteMessage);
                } else {
                    LocalBroadcastControlCenter.send(this, LocalBroadcastControlCenter.ACTION_NOTIFI_AF, remoteMessage.getData().get("body"));
                }

            }

        }

    }

   private void sendNoChatNotification(RemoteMessage remoteMessage) {
        String channel_id = "lale_channel_id";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String body = remoteMessage.getData().get("body");
        StringUtils.HaoLog("body="+body);
        if (body != null) {
            if (body.contains("notifyType")) {
                workNotifi workNotifi = new Gson().fromJson(body, workNotifi.class);
                Intent intent = new Intent(this, MainWebActivity.class);
                intent.putExtra("bFromPhone", true);
                intent.putExtra("Notification", body);

                String title = remoteMessage.getData().get("title");

                if (workNotifi.notifyType != null && workNotifi.msgType.equals("AF_TASK")) {
                    title = workNotifi.frontUserName;
                    body = workNotifi.taskName + ":" + workNotifi.keyword + "\n您有一份工作需盡速處理";
                } else if (workNotifi.notifyType != null && workNotifi.msgType.equals("AF_MEETING")) {
                    title = workNotifi.title;
                    body = workNotifi.content;
                }
                StringUtils.HaoLog("body="+body);
                PendingIntent pendingIntent = PendingIntent.getActivity(this, afid, intent,  FLAG_IMMUTABLE);


                NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channel_id)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher))

                        .setContentTitle(title)
                        .setContentText(body)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setLights(Color.RED, 1000, 300)
                        .setDefaults(Notification.DEFAULT_LIGHTS);
                // Notification Channel is required for Android O and above
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    StringUtils.HaoLog("Build.VERSION.SDK_INT >= Build.VERSION_CODES.O");
                    NotificationChannel channel = new NotificationChannel(
                            "lale_channel_id", "新訊息通知", NotificationManager.IMPORTANCE_DEFAULT
                    );
                    channel.setDescription("Lale 收到新訊息時使用的通知類型 (請注意，若未開啟可能無法接收新訊息通知)");
                    channel.setShowBadge(true);
                    channel.canShowBadge();
                    channel.enableLights(true);

                    channel.setLightColor(Color.RED);
                    channel.enableVibration(true);
                    channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500});

                    channel.setImportance(NotificationManager.IMPORTANCE_HIGH);
                    notificationManager.createNotificationChannel(channel);

                }

                notificationManager.notify(afid++, notificationBuilder.build());
            }
        }
    }

    private void sendNotification(MessageInfo data, String notificationBody) {
        StringUtils.HaoLog("data=" + data);
        StringUtils.HaoLog("dbHelper=" + AllData.dbHelper);
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        String channel_id = "lale_channel_id";

        int id = CommonUtils.letterToNumber(data.id);
        StringUtils.HaoLog("id=" + id);
        if (id < 0) {
            id = -id;
        }

        //處理點選訊息的跳轉
        Intent intent = new Intent(this, MainWebActivity.class);
        intent.putExtra("bFromPhone", true);
        workNotifi workNotifi = null;
        Bitmap bitmap = null;
        StringUtils.HaoLog("data.content=" + data.content);
        if (data.content == null)
            return;
        StringUtils.HaoLog("is_lale_ecosystem_af_notify()=" + data.is_lale_ecosystem_af_notify());
        if (data.is_lale_ecosystem_af_notify()) {

            try {
                workNotifi = new Gson().fromJson(new JSONObject(data.content).optString("data"), workNotifi.class);
                intent.putExtra("Notification", notificationBody);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        } else {

            intent.putExtra("roomInfo", data.room_id);
        }
        StringUtils.HaoLog("workNotifi =" + new Gson().toJson(workNotifi));
        PendingIntent pendingIntent = PendingIntent.getActivity(this, id, intent, FLAG_IMMUTABLE );

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);


        //設定標題 內文

        RoomMinInfo room = AllData.getRoomMinInfo(data.room_id);
        if (room == null) {
            RoomControlCenter.getAllRoom();
            room = AllData.getRoomMinInfo(data.room_id);
        }
        String body = data.getText();

        UserInRoom userInRoom = null;
        String title = data.type;
        String avatar_url = "";
        if (workNotifi == null) {

            if (room != null) {
                avatar_url = room.avatarUrl;
                title = room.name;
                userInRoom = AllData.getUserInRoom(room.id, data.sender);
                if (userInRoom != null) {
                    StringUtils.HaoLog("userInRoom !=null");
                } else {
                    StringUtils.HaoLog("room.type =" + room.type);
                    if (room.type == 6 || room.type == 4)
                        AllData.setUserInRoom(room.id, RoomSettingControlCenter.getGroupMembers(room.groupId));
                    else
                        AllData.setUserInRoom(room.id, RoomSettingControlCenter.getRoomMembers(room.id));
                    userInRoom = AllData.getUserInRoom(room.id, data.sender);
                }

            }
            StringUtils.HaoLog("room=" + room);
            StringUtils.HaoLog("userInRoom=" + userInRoom);
            //知道對方名稱
            if (userInRoom != null) {
                if (room.type != 1) {
                    body = userInRoom.displayName + ": " + body;
                }
                if (room.type != 6)
                    avatar_url = userInRoom.avatarUrl;
            }

            if (room.type == 4) {
                bitmap = StringUtils.drawBitmap(room.name);
            } else {
                if (avatar_url.isEmpty()) {
                    bitmap = BitmapFactory.decodeResource(getResources(),
                            room.type == 6 ? R.drawable.default_group : R.drawable.default_person);
                } else {
                    bitmap = FileUtils.getBitmapFromURL(avatar_url);
                }
            }
        } else {
            StringUtils.HaoLog("道觀通知" + workNotifi.msgType);

            if (workNotifi.msgType.contains("AF_TASK")) {

                title = workNotifi.frontUserName;
                body = workNotifi.taskName + ":" + workNotifi.keyword + "\n您有一份工作需盡速處理";
            }
            if (workNotifi.msgType.contains("AF_MEETING")) {
                title = workNotifi.title;
                body = workNotifi.content;
            }

        }


        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channel_id)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(uri)
                .setContentIntent(pendingIntent)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(Notification.PRIORITY_HIGH)
                .setLights(Color.RED, 1000, 300)
                .setDefaults(Notification.DEFAULT_LIGHTS);
        if (workNotifi == null) {
            notificationBuilder.setLargeIcon(bitmap);
        } else {
            notificationBuilder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));

        }
        // Notification Channel is required for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            StringUtils.HaoLog("Build.VERSION.SDK_INT >= Build.VERSION_CODES.O");
            NotificationChannel channel = new NotificationChannel(
                    "lale_channel_id", "新訊息通知", NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Lale 收到新訊息時使用的通知類型 (請注意，若未開啟可能無法接收新訊息通知)");
            channel.setShowBadge(true);
            channel.canShowBadge();
            channel.enableLights(true);

            channel.setLightColor(Color.RED);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{100, 200, 300, 400, 500});
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();
            channel.setSound(uri, audioAttributes);
            channel.setImportance(NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);

        }

        notificationManager.notify(id, notificationBuilder.build());
        StringUtils.HaoLog("發送通知  ");

    }

    private boolean foregrounded() {
        ActivityManager.RunningAppProcessInfo appProcessInfo = new ActivityManager.RunningAppProcessInfo();
        ActivityManager.getMyMemoryState(appProcessInfo);
        return (appProcessInfo.importance == IMPORTANCE_FOREGROUND || appProcessInfo.importance == IMPORTANCE_VISIBLE);
    }


    @Override
    public void onNewToken(String token) {
        //sendRegistrationToServer(token);
    }

    private static class SortByPostTime implements Comparator<StatusBarNotification> {
        public int compare(StatusBarNotification msg0, StatusBarNotification msg1) {
            Date date0 = new Date(msg0.getPostTime());
            Date date1 = new Date(msg1.getPostTime());
            int flag = date0.compareTo(date1);
            return flag;
        }
    }
}