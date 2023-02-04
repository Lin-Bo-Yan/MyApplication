package com.flowring.laleents.ui.main.webBody;

import static java.security.AccessController.getContext;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.MimeTypeMap;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ShareCompat;
import androidx.core.content.FileProvider;
import androidx.core.content.PermissionChecker;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.dmcbig.mediapicker.PickerConfig;
import com.dmcbig.mediapicker.entity.Media;
import com.flowring.laleents.BuildConfig;
import com.flowring.laleents.R;
import com.flowring.laleents.model.HttpReturn;
import com.flowring.laleents.model.explore.Microapp;
import com.flowring.laleents.model.room.RoomControlCenter;
import com.flowring.laleents.model.room.RoomInfoInPhone;
import com.flowring.laleents.model.user.UserControlCenter;
import com.flowring.laleents.model.user.UserInfo;
import com.flowring.laleents.model.user.UserMin;
import com.flowring.laleents.tools.ActivityUtils;
import com.flowring.laleents.tools.CallbackUtils;
import com.flowring.laleents.tools.CommonUtils;
import com.flowring.laleents.tools.DialogUtils;
import com.flowring.laleents.tools.FileUtils;
import com.flowring.laleents.tools.FormatUtils;
import com.flowring.laleents.tools.StringUtils;
import com.flowring.laleents.tools.TimeUtils;
import com.flowring.laleents.tools.cloud.api.CloudUtils;
import com.flowring.laleents.tools.cloud.mqtt.MqttService;
import com.flowring.laleents.tools.phone.AllData;
import com.flowring.laleents.tools.phone.BootBroadcastReceiver;
import com.flowring.laleents.tools.phone.DefinedUtils;
import com.flowring.laleents.tools.phone.LocalBroadcastControlCenter;
import com.flowring.laleents.tools.phone.PermissionUtils;
import com.flowring.laleents.tools.phone.ServiceUtils;
import com.flowring.laleents.ui.model.MainAppCompatActivity;
import com.flowring.laleents.ui.widget.qrCode.ScanCaptureActivity;
import com.google.gson.Gson;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;

public class MainWebActivity extends MainAppCompatActivity {
    public static String getBase64FromPath(String path) {
        String base64 = "";
        try {
            File file = new File(path);
            byte[] buffer = new byte[(int) file.length() + 100];
            @SuppressWarnings("resource")
            int length = new FileInputStream(file).read(buffer);
            base64 = Base64.encodeToString(buffer, 0, length,
                    Base64.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return base64;
    }

    //region  Permission
    AlertDialog requestDrawOverlaysDialog = null;

    public void checkPermission() {
        StringUtils.HaoLog("checkPermission");
        runOnUiThread(() -> {
            if (requestDrawOverlaysDialog != null) {
                requestDrawOverlaysDialog.dismiss();
                requestDrawOverlaysDialog = null;
            }
            requestDrawOverlaysDialog = PermissionUtils.requestDrawOverlays(this);
            if (!ServiceUtils.isIgnoringBatteryOptimizations(this)) {
                ServiceUtils.requestIgnoreBatteryOptimizations(this);
            }
            if (PermissionUtils.checkPermission(getApplicationContext(), Manifest.permission.FOREGROUND_SERVICE)) {

            } else {
                PermissionUtils.requestPermission(MainWebActivity.this, Manifest.permission.FOREGROUND_SERVICE, "背景執行權限");
            }
            if (PermissionUtils.checkPermission(getApplicationContext(), Manifest.permission.RECEIVE_BOOT_COMPLETED)) {
                StringUtils.HaoLog("可以使用重啟");
            } else {
                StringUtils.HaoLog("詢問使用重啟");
                PermissionUtils.requestPermission(MainWebActivity.this, Manifest.permission.RECEIVE_BOOT_COMPLETED, "開機後重新啟動背景服務");
            }
        });

    }

    private final static int FILE_CHOOSER_RESULT_CODE = 1234;

    private boolean checkContactaPermission() {
        boolean check = PermissionChecker.checkSelfPermission(MainWebActivity.this, Manifest.permission.READ_CONTACTS)
                == PermissionChecker.PERMISSION_GRANTED;
        return check;
    }
/*
* 登入 - token 傳遞, 保持 token 更新避免 mqtt 斷線及 api auth 失效
呼叫token更新-ok
呼叫相機拍照-ok
呼叫相機相簿-ok
呼叫 QRCode 掃描, 需整理目前各 qrCode 用途及資料格式
呼叫分享功能
呼叫交友邀請(手機聯絡人, 簡訊, 郵件, Line, Wechat)
呼叫信箱傳送
呼叫下載
聊天室背景, 跟手機db取得?-ok
點擊系統通知-訊息資料如何傳遞開啟 app 並導向聊天室
點擊系統通知-接聽電話資料如何傳遞並開啟 app 進入通話
點擊系統通知-掛斷電話是否可以直接發送 mqtt 掛斷電話 (不開啟 app)
通話擴音功能 - web 是否可以切換為手機喇叭並調整音量
登出
* */


    //endregion

    //region  test
    final Handler handler = new Handler();

    Runnable log = new Runnable() {
        @Override
        public void run() {
            StringUtils.HaoLog("還活著 " + webView.hashCode());
            sendToWebtest();
            handler.postDelayed(log, 5000);

        }
    };

    public void testOpenChrome() {
        Intent intent = CommonUtils.openChromeCustomTabs(this, "https://portal.flowring.com/WebAgenda/");
        startActivityForResult(intent, DefinedUtils.REQUEST_CHROME_TAB);
    }

    public void testGoNewActivity() {
        handler.post(log);
        new Thread(() -> {
            try {
                Thread.sleep(10000);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            runOnUiThread(() -> {
                ActivityUtils.gotoWebJitisiMeet(this, UserControlCenter.getUserMinInfo().displayName,
                        UserControlCenter.getUserMinInfo().userId,
                        UserControlCenter.getUserMinInfo().avatarThumbnailUrl,
                        UserControlCenter.getUserMinInfo().token, UserControlCenter.getUserMinInfo().externalServerSetting.mqttUrl,
                        UserControlCenter.getUserMinInfo().externalServerSetting.jitsiServerUrl, "video", "video", "video", "video", false
                );
            });
        }).start();
    }

    private void testDownloadFile(String url) {
        StringUtils.HaoLog("url=" + url);
        DownloadManager.Request request;
        try {
            request = new DownloadManager.Request(
                    Uri.parse(url));
        } catch (IllegalArgumentException e) {
            StringUtils.HaoLog("downloadFile Error=" + e);
            try {

                sendToWeb(new JSONObject().put("type", "downloadFile").put("data", new JSONObject().put("isSuccess", false)).toString());
            } catch (JSONException e2) {
                StringUtils.HaoLog("sendToWeb Error=" + e2);
                e.printStackTrace();
            }
            return;
        }

        String cookies = CookieManager.getInstance().getCookie(url);
        request.addRequestHeader("cookie", cookies);
        request.addRequestHeader("User-Agent", "");
        request.setDescription("Downloading File...");
        request.allowScanningByMediaScanner();

        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HHmmss SSS");
        request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, sdf.format(new Date().getDate()));
        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        new Thread(() -> {
            dm.enqueue(request);
        }).start();


//        try {
//
//            sendToWeb(new JSONObject().put("type", "downloadFile").put("data", new JSONObject().put("isSuccess", true)).toString());
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
        StringUtils.HaoLog("DownloadManager=end");
    }
    //endregion

    void initFireBaseMsgBroadcastReceiver() {
        FireBaseMsgBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                StringUtils.HaoLog("BroadcastReceiver=" + action + " " + intent.getStringExtra("data"));
                if (action.equals(LocalBroadcastControlCenter.ACTION_NOTIFI_AF)) {
                    String data = intent.getStringExtra("data");
                    if (data != null && data.contains("msgType") && (data.contains("AF_MEETING") || data.contains("AF_TASK")))
                        try {
                            sendToWeb("Notification", new JSONObject(data));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                } else if (action.equals(Intent.ACTION_SEND)) {
                    shareToWeb(intent);
                    StringUtils.HaoLog("testWebActivity ACTION_SEND");
                } else if (action.equals(Intent.ACTION_SEND_MULTIPLE)) {
                    StringUtils.HaoLog("testWebActivity ACTION_SEND_MULTIPLE");
                } else if (action.equals(LocalBroadcastControlCenter.ACTION_MQTT_FRIEND)) {
                    String user_id = intent.getStringExtra("user_id");
                    String user_name = intent.getStringExtra("user_name");
                    String user_avatar_url = intent.getStringExtra("user_avatar_url");
                } else if (action.equals(LocalBroadcastControlCenter.ACTION_MQTT_Error)) {
                    DialogUtils.showDialogMessage(MainWebActivity.this, "伺服器連線異常");
                }
            }
        };
        itFilter.addAction(DefinedUtils.ACTION_FIREBASE_MESSAGE);
        itFilter.addAction(DefinedUtils.ACTION_FRIEND_INVITE);
        itFilter.addAction(LocalBroadcastControlCenter.ACTION_NOTIFI_AF);
        itFilter.addAction(Intent.ACTION_SEND);
        itFilter.addAction(Intent.ACTION_SEND_MULTIPLE);
        LocalBroadcastManager.getInstance(this).registerReceiver(FireBaseMsgBroadcastReceiver, itFilter); //註冊廣播接收器
    }

    @SuppressLint({"JavascriptInterface", "SetJavaScriptEnabled"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);
        if (AllData.context == null)
            AllData.context = getApplicationContext();
        initFireBaseMsgBroadcastReceiver();
        com.flowring.laleents.tools.Log.setContext(getApplicationContext());
        AllData.init(getApplicationContext());
        if (!init) {
            BootBroadcastReceiver.setReToken(getApplicationContext());
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        checkUpApp(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAppNeedUpdate();
        UserMin userMin = UserControlCenter.getUserMinInfo();
        checkHasWebView();
        StringUtils.HaoLog("onResume=" + userMin);
        if (userMin != null && !userMin.userId.isEmpty()) {
            checkPermission();


//            testLogout();
        } else {
            goLogin();
        }

    }

    @Override
    protected void onDestroy() {
        StringUtils.HaoLog("onDestroy " + webView);
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.destroy();
            webView = null;
        }
        if (MqttService.mqttControlCenter != null)
            MqttService.mqttControlCenter.DisConnect();

        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (null == mUploadMessage) return;
            //如果沒選照片就設定收到的值為null，使下次可以再次選取
            if (resultCode != RESULT_OK) {
                mUploadMessage.onReceiveValue(null);
                mUploadMessage = null;
                return;
            }
            //若選擇拍照上傳data會是null
            Uri result = (data == null) ? null : data.getData();
            if (result == null) {
                if (data != null && data.getClipData() != null) {

                    ClipData clipData = data.getClipData();
                    if (clipData == null) {
                        mUploadMessage.onReceiveValue(null);
                        mUploadMessage = null;
                        return;
                    }
                    //取得相簿選取的相片
                    uploadImages(clipData);
                    return;
                } else {
                    if (TextUtils.isEmpty(currentPhotoPath)) {
                        mUploadMessage.onReceiveValue(null);
                        mUploadMessage = null;
                        return;
                    }
                    //取得拍照上傳的相片
                    Uri uri = Uri.fromFile(new File(currentPhotoPath));
                    if (mUploadMessage != null) {
                        mUploadMessage.onReceiveValue(new Uri[]{uri});
                        mUploadMessage = null;
                    }
                    return;
                }
            }
            if (mUploadMessage != null) {
                mUploadMessage.onReceiveValue(new Uri[]{result});
                mUploadMessage = null;
            }
        }
        if (requestCode == DefinedUtils.REQUEST_IMAGE_PICKER) {
            ArrayList<Media> images;
            images = data.getParcelableArrayListExtra(PickerConfig.EXTRA_RESULT);
            if (images != null) {
                Uri[] uris = new Uri[images.size()];
                for (int i = 0; i < images.size(); i++) {
                    uris[i] = Uri.fromFile(new File(images.get(i).path));
                }
                if (mUploadMessage != null) {
                    mUploadMessage.onReceiveValue(uris);
                    mUploadMessage = null;
                }
            }
        } else if (requestCode == DefinedUtils.REQUEST_CHROME_TAB) {
            if (resultCode == Activity.RESULT_CANCELED) {
                closeNativeBrowser();
            }
        }
    }

    public void checkAppNeedUpdate() {
        new Thread(() -> {
            if (CloudUtils.iCloudUtils.checkAppNeedUpdate())
                showUpgradeDialog();
        }).start();
    }

    public void showUpgradeDialog() {
        AlertDialog.Builder alertDialogBuilder =
                new AlertDialog.Builder(this)
                        .setMessage(getString(R.string.update_app_text))
                        .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                            dialog.dismiss();
                            CloudUtils.iCloudUtils.gotoGooglePlay(MainWebActivity.this);
                            finish();
                        })
                        .setNegativeButton(android.R.string.cancel, (dialog, i) -> {
                            dialog.dismiss();
                            finish();
                        })
                        .setCancelable(false);
        alertDialogBuilder.show();
    }

    //region   WebView
    void checkHasWebView() {
        StringUtils.HaoLog("checkHasWebView webView=" + webView);
        if (webView == null) {
            StringUtils.HaoLog("init webView");
            webView = new MyWebView(getApplicationContext());
            setWebView(webView, getMainWebUrl());
            backtoActivity();
        }
    }


    public String getMainWebUrl() {
        if (UserControlCenter.getUserMinInfo() != null && UserControlCenter.getUserMinInfo().eimUserData != null && UserControlCenter.getUserMinInfo().eimUserData.af_url != null)
            return UserControlCenter.getUserMinInfo().eimUserData.af_url + "/eimApp/index.html#/";

        return "";

    }


    private ValueCallback<Uri[]> mUploadMessage;
    boolean init = false;

    MyWebView webView;

    void cleanWebviewCache() {
        deleteDatabase("webview.db");
        deleteDatabase("webviewCache.db");
        //WebView 缓存文件
        File appCacheDir = new File(getFilesDir().getAbsolutePath() + "/webcache");


        File webviewCacheDir = new File(getCacheDir().getAbsolutePath() + "/webviewCache");

        //删除webview 缓存目录
        if (webviewCacheDir.exists()) {
            deleteFile(webviewCacheDir.getPath());
        }
        //删除webview 缓存 缓存目录
        if (appCacheDir.exists()) {
            deleteFile(appCacheDir.getPath());
        }


    }

    private void chooseCAMERA() {
        //Intent開啟相簿
        Intent intentFile = new Intent(Intent.ACTION_GET_CONTENT);
        intentFile.addCategory(Intent.CATEGORY_OPENABLE);
        intentFile.setType("*/*");

        //Intent開啟相機
        Intent intentCamera = null;
        //判斷是否有載入儲存空間
        String state = Environment.getExternalStorageState();
        if (state.equals(Environment.MEDIA_MOUNTED)) {
            if (!PermissionUtils.checkPermission(this, Manifest.permission.CAMERA)) {
                StringUtils.HaoLog("前往相機或相簿 0");
                PermissionUtils.requestPermission(this, Manifest.permission.CAMERA, "該功能需要相機權限");
                mUploadMessage.onReceiveValue(null);
                mUploadMessage = null;
                return;
            }
            intentCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (Exception ex) {

            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.flowring.laleents.fileprovider", photoFile);
                intentCamera.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intentCamera.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            }

        }
        //跳出選擇拍照或是相簿
        startActivityForResult(intentCamera, FILE_CHOOSER_RESULT_CODE);
    }

    private void chooseAlbum() {
        //Intent開啟相簿
        Intent intentFile = new Intent(Intent.ACTION_GET_CONTENT);
        intentFile.addCategory(Intent.CATEGORY_OPENABLE);
        intentFile.setType("*/*");

        //Intent開啟相機
        Intent intentCamera = null;
        //判斷是否有載入儲存空間
        String state = Environment.getExternalStorageState();
        if (state.equals(Environment.MEDIA_MOUNTED)) {
            if (!PermissionUtils.checkPermission(this, Manifest.permission.CAMERA)) {
                PermissionUtils.requestPermission(this, Manifest.permission.CAMERA, "該功能需要相機權限");
                return;
            }
            intentCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (Exception ex) {

            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.flowring.laleents.fileprovider", photoFile);
                intentCamera.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intentCamera.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            }

        }


        Intent chooser = new Intent(Intent.ACTION_CHOOSER);
        //設定title，放入兩種intent
        chooser.putExtra(Intent.EXTRA_TITLE, "選擇添加項目");
        chooser.putExtra(Intent.EXTRA_INTENT, intentFile);
        if (intentCamera != null) {
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{intentCamera});
        }
        //跳出選擇拍照或是相簿
        startActivityForResult(intentFile, FILE_CHOOSER_RESULT_CODE);
    }

    private void chooseFile() {
        //Intent開啟相簿
        Intent intentFile = new Intent(Intent.ACTION_GET_CONTENT);
        intentFile.addCategory(Intent.CATEGORY_OPENABLE);
        intentFile.setType("*/*");

        //Intent開啟相機
        Intent intentCamera = null;
        //判斷是否有載入儲存空間
        String state = Environment.getExternalStorageState();
        if (state.equals(Environment.MEDIA_MOUNTED)) {

            intentCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (Exception ex) {

            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.flowring.laleents.fileprovider", photoFile);
                intentCamera.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intentCamera.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            }

        }


        Intent chooser = new Intent(Intent.ACTION_CHOOSER);
        //設定title，放入兩種intent
        chooser.putExtra(Intent.EXTRA_TITLE, "選擇添加項目");
        chooser.putExtra(Intent.EXTRA_INTENT, intentFile);
        if (intentCamera != null) {
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{intentCamera});
        }
        //跳出選擇拍照或是相簿
        startActivityForResult(chooser, FILE_CHOOSER_RESULT_CODE);
    }

    private String currentPhotoPath = "";

    //創建照片路徑
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + ".jpg";
        File image = new File(getCacheDir(), imageFileName);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void uploadImages(ClipData clipData) {
        Uri[] uriList = new Uri[clipData.getItemCount()];
        for (int i = 0; i < clipData.getItemCount(); i++) {
            clipData.getItemCount();
            ClipData.Item item = clipData.getItemAt(i);
            StringUtils.HaoLog("item.getUri()=" + item.getUri());
            uriList[i] = item.getUri();
        }
        mUploadMessage.onReceiveValue(uriList);
        mUploadMessage = null;
    }

    void shareToWeb(Intent intent) {

        String type = intent.getType();
        String action = intent.getAction();
        StringUtils.HaoLog("BroadcastReceiver EXTRA_TEXT=" + intent.getStringExtra(Intent.EXTRA_TEXT));

        StringUtils.HaoLog("BroadcastReceiver EXTRA_STREAM=" + intent.getParcelableExtra(Intent.EXTRA_STREAM));
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            //從手機分享純文字
            if (type.startsWith("text/")) {
                String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
                try {
                    sendToWeb("gotoShare", new JSONObject().put("type", "text").put("data", sharedText));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
            //從手機分享單張圖片
            else if (type.startsWith("image/")) {
                Uri imageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);

                try {
                    sendToWeb("gotoShare", new JSONObject().put("type", "image").put("data", getBase64FromPath(imageUri.getPath())));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } else if (type.startsWith("video/")) {
                Uri videoUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                try {
                    sendToWeb("gotoShare", new JSONObject().put("type", "video").put("data", getBase64FromPath(videoUri.getPath())));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            //從手機其他檔案
            else {
                Uri fileUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
                try {
                    sendToWeb("gotoShare", new JSONObject().put("type", "file").put("data", getBase64FromPath(fileUri.getPath())));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    void initOnMainWebPageFinished() {


        if (!init) {
            init = true;

            StringUtils.HaoLog("initOnMainWebPageFinished");
//            if (UserControlCenter.getUserMinInfo() != null && !UserControlCenter.getUserMinInfo().userId.isEmpty()) {
//                StringUtils.HaoLog("initOnMainWebPageFinished " + UserControlCenter.getUserMinInfo());
//                Login();
//
//            }
            if (getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_SEND)) {
                shareToWeb(getIntent());
            } else if (getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_SEND_MULTIPLE)) {
            } else checkUpApp(getIntent());
        }


    }

    void checkUpApp(Intent intent) {

        if (init) {
            StringUtils.HaoLog("checkUpApp2 " +intent.hasExtra("isHome"));
            if (intent.getBooleanExtra("isHome", false)) {
                try {

                    JSONObject j = new JSONObject().put("type", "gotoWeb").put("data", new JSONObject().put("url",intent.getStringExtra("isHomeMICRO_APPurl")).put("title",intent.getStringExtra("isHomeMICRO_APPName")));
                    sendToWeb(j.toString());
                   intent.removeExtra("isHome");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } else if (intent.getBooleanExtra("bFromPhone", false) &&intent.getStringExtra("Notification") != null) {
                try {
                    sendToWeb("Notification", new JSONObject(intent.getStringExtra("Notification")));
                   intent.removeExtra("bFromPhone");
                   intent.removeExtra("Notification");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    DownloadListener mWebDownloadListener = new DownloadListener() {
        @Override
        public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
            StringUtils.HaoLog("onDownloadStart url=" + url);
            StringUtils.HaoLog("onDownloadStart userAgent=" + userAgent);
            StringUtils.HaoLog("onDownloadStart contentDisposition=" + contentDisposition);
            StringUtils.HaoLog("onDownloadStart mimeType=" + mimeType);
            Uri uri = Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
//            if (ContextCompat.checkSelfPermission(MainWebActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
//                    PackageManager.PERMISSION_GRANTED) {
//                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 50);
//            } else {
//                downloadFile(url, userAgent, contentDisposition, mimeType);
//            }
        }
    };

    @SuppressLint("JavascriptInterface")
    void setWebView(WebView webView, String url) {
        this.getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        if (webView == null)
            webView = new WebView(getApplicationContext());
        webView.setVisibility(View.INVISIBLE);
        webView.setDownloadListener(mWebDownloadListener);
        cleanWebviewCache();
        WebSettings webSettings = webView.getSettings();
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                findViewById(R.id.logo).setVisibility(View.GONE);
                view.setVisibility(View.VISIBLE);
                StringUtils.HaoLog("還活著 onPageFinished=" + url);

                super.onPageFinished(view, url);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                StringUtils.HaoLog("還活著 onReceivedError error=" + error.toString());
                super.onReceivedError(view, request, error);
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                StringUtils.HaoLog("還活著 onReceivedHttpError error getUrl=" + request.getUrl());
                StringUtils.HaoLog("還活著 onReceivedHttpError error=" + errorResponse.getData());
                StringUtils.HaoLog("還活著 onReceivedHttpError error getStatusCode=" + errorResponse.getStatusCode());
if(getMainWebUrl().equals(request.getUrl().toString())&&errorResponse.getStatusCode()>=500&&errorResponse.getStatusCode()<600)
{
    Logout();
}
                super.onReceivedHttpError(view, request, errorResponse);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                StringUtils.HaoLog("還活著 onReceivedSslError error=" + error.toString());
                super.onReceivedSslError(view, handler, error);
            }
        });
        webSettings.setAllowFileAccess(true);


        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        webSettings.setSupportZoom(true);
        webSettings.setDefaultTextEncodingName("utf-8");
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setTextZoom(100);
        webSettings.setUseWideViewPort(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setGeolocationEnabled(true);
        webSettings.setSupportMultipleWindows(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        WebChromeClient mWebChromeClient = new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest request) {

                request.grant(request.getResources());

            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                Log.e("hao", "onCreateWindow" + resultMsg);
                WebView newWebView = new WebView(view.getContext());

                newWebView.setWebViewClient(new WebViewClient());
                newWebView.setWebChromeClient(this);
                setWebView(newWebView, url);
                view.addView(newWebView);
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(newWebView);
                resultMsg.sendToTarget();
                return true;
            }

            // Android 5.0+
            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {

                StringUtils.HaoLog("onShowFileChooser");
                Intent intent = fileChooserParams.createIntent();

                MainWebActivity.this.webView = (MyWebView) webView;
                if (mUploadMessage != null)
                    mUploadMessage.onReceiveValue(null);
                mUploadMessage = filePathCallback;
                if (intent.getType().equals("image/*")) {
                    if (fileChooserParams.isCaptureEnabled()) {
                        chooseCAMERA();
                    } else {
                        CommonUtils.choosePicture(MainWebActivity.this, intent.getBooleanExtra("android.intent.extra.ALLOW_MULTIPLE", false) ? 9 : 1, PickerConfig.PICKER_IMAGE_VIDEO, new CallbackUtils.APIReturn() {
                            @Override
                            public void Callback(boolean isok, String DataOrErrorMsg) {
                                if (mUploadMessage != null) {
                                    mUploadMessage.onReceiveValue(null);
                                    mUploadMessage = null;
                                }
                            }
                        });


                    }
                } else {
                    chooseFile();
                }

                return true;
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
                Log.e("hao", "onJsAlert");
                if (getContext() == null) {
                    return false;
                }
                AlertDialog.Builder b = new AlertDialog.Builder(MainWebActivity.this);
                b.setTitle("Alert");
                b.setMessage(message);
                b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        result.confirm();
                    }
                });
                b.setCancelable(false);
                b.create().show();
                return true;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, final JsResult result) {
                Log.e("hao", "onJsConfirm");
                AlertDialog.Builder b = new AlertDialog.Builder(MainWebActivity.this);
                b.setTitle("Confirm");
                b.setMessage(message);
                b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        result.confirm();
                    }
                });
                b.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        result.cancel();
                    }
                });
                b.create().show();
                return true;
            }

            @Override
            public void onGeolocationPermissionsHidePrompt() {
                super.onGeolocationPermissionsHidePrompt();

            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {

            }
        };
        webView.addJavascriptInterface(this, "laleIm");
        webView.addJavascriptInterface(this, "LaleTon");
        webView.addJavascriptInterface(this, "FlowringLale");
        WebView.setWebContentsDebuggingEnabled(true);
        webView.setWebChromeClient(mWebChromeClient);
//        webView.loadData(testUrl, "text/html; charset=utf-8", "UTF-8");
        if (!init) {
            if (getIntent().getBooleanExtra("bFromPhone", false) && getIntent().getStringExtra("roomInfo") != null) {
                StringUtils.HaoLog("initOnMainWebPageFinished 前往" + getIntent().getStringExtra("roomInfo"));
                webView.loadUrl(getMainWebUrl() + "chatroom/" + getIntent().getStringExtra("roomInfo"));
                init = true;
            } else {
                webView.loadUrl(url);
                StringUtils.HaoLog("init setWebView");
            }
        } else {
            StringUtils.HaoLog("init setWebView 2");
            webView.loadUrl(url);
            StringUtils.HaoLog("init setWebView 3");
        }
    }

    void backtoActivity() {
        if (webView != null) {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT);
            if (webView.getParent() != null)
                ((ViewGroup) webView.getParent()).removeView(webView);
            ((ViewGroup) findViewById(R.id.all)).addView(webView, params);
        }
    }
    //endregion

    //region  postMessage
    @JavascriptInterface
    public String postMessage(String json) {
        StringUtils.HaoLog("jsp postMessage:" + TimeUtils.NowTimestamp() + "/" + json);
        if (json == null || json.isEmpty())
            return null;
        try {
            JSONObject jsonObject = new JSONObject(json);
            if (jsonObject.isNull("command"))
                return json;
            String command = jsonObject.optString("command");
            JSONObject data = null;
            if (!jsonObject.isNull("data"))
                data = jsonObject.optJSONObject("data");

            switch (command) {
                case "NewUrl":
                    NewUrl(data);
                    break;
                case "feedback":
                    feedback();
                    break;
                case "openWebView":
                    openWebView(data);
                    break;
                case "openQRcode":
                    openQRcode(data);
                    break;
                case "openPhoneWebView":
                    openPhoneWebView(data);
                    break;
                case "openChrome":
                    openChrome(data);
                    break;
                case "getAddressBook":
                    getAddressBook();
                    break;
                case "newLinkApp":
                    newLinkApp(data);
                    break;
                case "Logout":
                    Logout();
                    break;
                case "downloadByUrl":
                    downloadByUrl(data);
                    break;
                case "downloadByUrls":
                    downloadByUrls(data);
                    break;
                case "downloadByBytesBase64":
                    downloadByBytesBase64(data);
                    break;
                case "tokenRefresh":
                    tokenRefresh();
                    break;
                case "getRoomBackground":
                    getRoomBackground(data);
                    break;
                case "setRoomBackground":
                    setRoomBackground(data);
                    break;
                case "share":
                    share(data);
                    break;
                case "updateUser":
                    updateUser();
                    break;
                case "updateRooms":
                    updateRooms();
                    break;
                case "webOk":
                    webOk();
                    break;
                case "getAPPVersion":
                    getAPPVersion();
                    break;
                default:
                    unDo(json);
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    private void feedback() {

        ShareCompat.IntentBuilder sb = ShareCompat.IntentBuilder.from(this);
        String[] tos = {"lalereport@flowring.com"};
        sb.setEmailTo(tos);
        sb.setText("使用者Lale ID : " + UserControlCenter.getUserMinInfo().userId + "\n問題描述:");
        sb.setType("message/rfc822");
        File mLogFile = new File(FileUtils.getApplicationFolder(this, DefinedUtils.FOLDER_FILES) +
                "/" + "laletoB_logs.log");
        Uri NuriForFile = FileProvider.getUriForFile(this, "com.flowring.laleents.fileprovider", mLogFile);
        sb.setStream(NuriForFile);
        sb.setSubject("問題回報");
        sb.startChooser();
        Toast.makeText(this, "請選擇電子信箱進行傳送", Toast.LENGTH_LONG).show();

    }

    private void openWebView(JSONObject data) {
        ActivityUtils.gotoWebViewActivity(MainWebActivity.this, data.optString("url"));
    }

    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    private void downloadFile(String url, String fileName) {
        DownloadManager.Request request;
        try {
            request = new DownloadManager.Request(
                    Uri.parse(url));
        } catch (IllegalArgumentException e) {
            StringUtils.HaoLog("downloadFile Error=" + e);
            try {

                sendToWeb(new JSONObject().put("type", "downloadFile").put("data", new JSONObject().put("isSuccess", false)).toString());
            } catch (JSONException e2) {
                StringUtils.HaoLog("sendToWeb Error=" + e2);
                e.printStackTrace();
            }
            return;
        }

        String cookies = CookieManager.getInstance().getCookie(url);

        URLConnection conection = null;
        try {
            conection = new URL(url).openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }

        request.addRequestHeader("cookie", cookies);
        request.addRequestHeader("User-Agent", "");
        request.setDescription("Downloading File...");
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH mm ss SSS");
        request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, fileName == null ? sdf.format(new Date().getDate()) + StringUtils.toExtension(conection.getContentType()) : fileName);
        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        dm.enqueue(request);
        try {

            sendToWeb(new JSONObject().put("type", "downloadFile").put("data", new JSONObject().put("isSuccess", true)).toString());
        } catch (JSONException | NullPointerException e) {
            e.printStackTrace();
        }
        StringUtils.HaoLog("DownloadManager=end");
    }

    private void downloadFile(String[] urls, String fileName, downloadByUrlsCallback back) {
        DownloadManager.Request request;

        if (urlsNew < urlsMax) {
            try {
                String cookies = CookieManager.getInstance().getCookie(urls[urlsNew]);
                URLConnection conection = null;
                request = new DownloadManager.Request(
                        Uri.parse(urls[urlsNew]));
                conection = new URL(urls[urlsNew]).openConnection();
                request.addRequestHeader("cookie", cookies);
                request.addRequestHeader("User-Agent", "");
                request.setDescription("Downloading File...");
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH mm ss SSS");
                request.setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS, fileName == null ? sdf.format(new Date().getDate()) + StringUtils.toExtension(conection.getContentType()) : fileName);
                DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                dm.enqueue(request);
                downloadFile(urls, fileName, back);
            } catch (IOException | IllegalArgumentException e) {
                e.printStackTrace();
                downloadFile(urls, fileName, back);
                urlsError.add(urls[urlsNew]);
            }
            urlsNew++;
        } else {

            back.onEnd(urlsIsOk, urlsError.toArray(new String[0]));
        }

    }
    public static String getVersionName(Context context) {
        String versionName = "";
        try {
            //获取软件版本号，对应AndroidManifest.xml下android:versionName
            versionName = context.getPackageManager().
                    getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionName;
    }

    private void getAPPVersion() {
        try {

            sendToWeb(new JSONObject().put("type", "getAPPVersion").put("data", new JSONObject().put("Version", getVersionName(MainWebActivity.this))).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void webOk() {
        UserMin userMin = UserControlCenter.getUserMinInfo();

        StringUtils.HaoLog("webOk");
        if (userMin != null && !userMin.userId.isEmpty()) {
            checkPermission();
            Login();
        }
        initOnMainWebPageFinished();
    }

    private void updateRooms() {
        new Thread(() -> {
            RoomControlCenter.getAllRoom();
        }).start();

    }

    private void updateUser() {
        UserControlCenter.getMainUserInfo(new CallbackUtils.userReturn() {
            @Override
            public void Callback(UserInfo userInfo) {

            }
        });
    }

    private void unDo(String json) {
        sendToWeb(json);
    }

    boolean isLogin = false;

    private void Login() {
        if (!isLogin) {
            if (AllData.dbHelper == null) {
                StringUtils.HaoLog("userId=" + UserControlCenter.getUserMinInfo().userId);
                AllData.initSQL(UserControlCenter.getUserMinInfo().userId);
            }
            try {
                StringUtils.HaoLog("Login=" + new Gson().toJson(UserControlCenter.getUserMinInfo().eimUserData));
                sendToWeb(new JSONObject().put("type", "loginEim").put("data", new JSONObject(new Gson().toJson(UserControlCenter.getUserMinInfo().eimUserData))).toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (MqttService.mqttControlCenter == null) {
                Intent intentServer = new Intent(this, MqttService.class);
                intentServer.putExtra("data", "new");
                startService(intentServer);
            } else {
                MqttService.mqttControlCenter.NewConnect();
            }
            isLogin = true;
        }


    }

    private void setRoomBackground(JSONObject data) {
        String roomId = data.optString("roomId");

        String bytesBase64 = null;
        if (data.has("bytesBase64"))
            bytesBase64 = data.optString("bytesBase64");

        RoomInfoInPhone roomInfoInPhone = AllData.getRoomInPhone(roomId);
        if (roomInfoInPhone == null) {
            roomInfoInPhone = new RoomInfoInPhone();

        }
        roomInfoInPhone.id = roomId;
        roomInfoInPhone.bg = bytesBase64;
        AllData.updateRoomInPhone(roomInfoInPhone);

    }

    private void newLinkApp(JSONObject data) {
        CommonUtils.addShortcut(this, new Gson().fromJson(data.toString(), Microapp.class));
    }

    private void Logout() {
        StringUtils.HaoLog("登出");

        UserControlCenter.setLogout(new CallbackUtils.ReturnHttp() {
            @Override
            public void Callback(HttpReturn httpReturn) {
                NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                manager.cancelAll();
                runOnUiThread(() -> {
                    isLogin = false;
                    goLogin();
                });

            }
        });
    }

    public void goLogin() {
        if (webView != null) {
            webView.loadUrl("about:blank");
            webView.destroy();
            webView = null;
        }
        Intent intent = new Intent(MainWebActivity.this, EimLoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    private void share(JSONObject data) {
        if (data.has("type")) {
            String inviteMessage = "";
            if (data.has("data"))
                inviteMessage = data.optString("data");
            switch (data.optString("type")) {
                case "sms":
                    openMessage(inviteMessage);
                    break;
                case "mail":
                    openGmail("user Name", inviteMessage);
                    break;
                case "weChat":
                    openWechat(inviteMessage);
                    break;
                case "Line":
                    openLine(inviteMessage);
                case "QRcodeImage":
                    openQRcodeImage(inviteMessage);
                    break;

            }
        }
    }

    private void openQRcodeImage(String inviteMessage) {
        BarcodeEncoder encode = new BarcodeEncoder();
        try {
            int pixel = (int) CommonUtils.convertDpToPixel(200, this);
            Bitmap bitmap = encode.encodeBitmap(inviteMessage, BarcodeFormat.QR_CODE, pixel, pixel);
            if (bitmap != null) {
                File file = FileUtils.saveCachePic(this, bitmap, "分享Lale群組QrCode");
                if (file != null) {
                    Uri uri = FileProvider.getUriForFile(this, "com.flowring.laleents.fileprovider", file);
                    if (uri != null) {
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType("image/png");
                        intent.putExtra(Intent.EXTRA_STREAM, uri);
                        startActivity(Intent.createChooser(intent, "分享Lale群組Qr Code"));
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getRoomBackground(JSONObject data) {
        String roomId = data.optString("roomId");
        RoomInfoInPhone roomInfoInPhone = AllData.getRoomInPhone(roomId);
        if (roomInfoInPhone == null) {
            roomInfoInPhone = new RoomInfoInPhone();
            roomInfoInPhone.id = roomId;

        }
        AllData.updateRoomInPhone(roomInfoInPhone);
        try {
            JSONObject j = new JSONObject().put("type", "getRoomBackground").put("bytesBase64", roomInfoInPhone.bg);
            sendToWeb(j.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void tokenRefresh() {
        UserControlCenter.tokenRefresh(new CallbackUtils.ReturnHttp() {
            @Override
            public void Callback(HttpReturn httpReturn) {
                try {
                    if (httpReturn.status != 200) {
                        if (httpReturn.msg.equals("token 逾時")) {
                            Logout();
                        } else {
                            DialogUtils.showDialogMessage(MainWebActivity.this, httpReturn.msg, "連線狀態異常，是否要登出？", new CallbackUtils.noReturn() {
                                @Override
                                public void Callback() {
                                    Logout();
                                }
                            }, new CallbackUtils.noReturn() {
                                @Override
                                public void Callback() {

                                }
                            });
                        }
                    } else {
                        JSONObject j = new JSONObject().put("type", "tokenRefresh").put("data", new JSONObject(new Gson().toJson(httpReturn.data)));
                        sendToWeb(j.toString());
                    }

                } catch (JSONException e) {
                    StringUtils.HaoLog("錯誤：" + e);
                    e.printStackTrace();
                }
            }
        });
    }

    private void downloadByBytesBase64(JSONObject data) {

        String folder;
        String bytesBase64;
        unDo(data.toString());
    }

    private int urlsMax = 0;
    private boolean urlsIsOk = true;
    private int urlsNew = 0;
    private ArrayList<String> urlsError = new ArrayList<>();

    private void downloadByUrls(JSONObject data) {

        if (PermissionUtils.checkPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) || (Build.VERSION.SDK_INT > Build.VERSION_CODES.R)) {
            new Thread(() -> {

                StringUtils.HaoLog(data.toString());
                try {
                    JSONArray urls = data.getJSONArray("urls");
                    String[] urlsArray = new String[urls.length()];
                    for (int i = 0; i < urls.length(); i++) {
                        urlsArray[i] = urls.getString(i);
                    }

                    urlsMax = urls.length();
                    urlsNew = 0;
                    urlsError = new ArrayList<>();
                    downloadFile(urlsArray, null, new downloadByUrlsCallback() {
                        @Override
                        public void onEnd(boolean isSuccess, String[] errorUrl) {
                            downloadByUrlsReturn(isSuccess, errorUrl);
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }).start();
        } else {
            StringUtils.HaoLog("詢問權限");
            runOnUiThread(() -> {
                downloadByUrlsReturn(false, null);
                PermissionUtils.requestPermission(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, null, "該功能需要下載權限");

            });
        }

    }

    interface downloadByUrlsCallback {
        void onEnd(boolean isSuccess, String[] errorUrl);
    }

    private void downloadByUrlsReturn(boolean isSuccess, String[] errorUrl) {
        runOnUiThread(() -> {
            try {

                sendToWeb(new JSONObject().put("type", "downloadFiles").put("data", new JSONObject().put("isSuccess", isSuccess).put("errorUrl", errorUrl)).toString());
            } catch (JSONException e2) {
                StringUtils.HaoLog("sendToWeb Error=" + e2);
                e2.printStackTrace();
            }


        });
    }

    private void downloadByUrl(JSONObject data) {

        if (PermissionUtils.checkPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) || (Build.VERSION.SDK_INT > Build.VERSION_CODES.R)) {
            new Thread(() -> {
                StringUtils.HaoLog(data.toString());
                if (data.isNull("fileName"))
                    downloadFile(data.optString("url"), null);
                else
                    downloadFile(data.optString("url"), data.optString("fileName"));
            }).start();
        } else {
            StringUtils.HaoLog("詢問權限");
            try {

                sendToWeb(new JSONObject().put("type", "downloadFile").put("data", new JSONObject().put("isSuccess", false)).toString());
            } catch (JSONException e2) {
                StringUtils.HaoLog("sendToWeb Error=" + e2);
                e2.printStackTrace();
            }
            runOnUiThread(() -> {

                PermissionUtils.requestPermission(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, null, "該功能需要下載權限");

            });
        }

    }


    private void getAddressBook() {
        if (PermissionUtils.checkPermission(this, Manifest.permission.READ_CONTACTS)) {

            try {
                JSONArray jsonArray = new JSONArray();
                for (String phone : getPhoneContactsData()) {
                    jsonArray.put(phone);
                }
                sendToWeb(new JSONObject().put("type", "getAddressBook").put("data", new JSONObject().put("phones", jsonArray)).toString());


            } catch (JSONException e) {
                e.printStackTrace();
            }

        } else {
            try {
                sendToWeb(new JSONObject().put("type", "getAddressBook").put("data", new JSONObject().put("error", "NoPermission")).toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void openPhoneWebView(JSONObject data) {

        ActivityUtils.gotoWebJitisiMeet(this,
                data.optString("displayName"),
                data.optString("userId")
                , data.optString("avatar")
                , data.optString("laleToken")
                , data.optString("mqttHost")
                , data.optString("jitsiDomain")
                , data.optString("callType")
                , data.optString("msgId")
                , data.optString("roomId")
                , data.optString("roomName")
                , data.optBoolean("isGroupCall")
        );
    }

    void NewUrl(JSONObject data) {
        String sURL;
        if (data.has("url")) {
            sURL = data.optString("url");
            webView.loadUrl(sURL);
        }

    }

    void openChrome(JSONObject data) {
        String sURL;
        try {

            if (data.has("url")) {
                sURL = data.getString("url");
                chromeCallbackUrl = data.optString("callBackUrl");
                needBack = data.optBoolean("webBack", false);
                Intent intent = CommonUtils.openChromeCustomTabs(this, sURL);
                startActivityForResult(intent, DefinedUtils.REQUEST_CHROME_TAB);
            }
        } catch (Exception e) {
            StringUtils.HaoLog("JS openWebViewByChrome ERROR = " + e);
        }

    }

    private void openQRcode(JSONObject data) {
        activityReturn = new CallbackUtils.ActivityReturn() {
            @Override
            public void Callback(androidx.activity.result.ActivityResult activityResult) {
                String SCAN_QRCODE = null;
                if (activityResult.getData() != null)
                    SCAN_QRCODE = activityResult.getData().getStringExtra("SCAN_QRCODE");
                try {
                    JSONObject j = new JSONObject().put("type", "getQRcode").put("data", SCAN_QRCODE);
                    sendToWeb(j.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        };
        ActivityUtils.gotoQRcode(this, ScanCaptureActivity.ScanCaptureType.Bind, ActivityResult);
    }

    private void openLine(String inviteMessage) {
        try {
            String msg = URLEncoder.encode(inviteMessage, "UTF-8");
            String scheme = "line://msg/text/" + msg;
            Uri uri = Uri.parse(scheme);
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(MainWebActivity.this, "尚未安裝Line。", Toast.LENGTH_SHORT).show();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void openWechat(String inviteMessage) {
        try {
            Intent wechatIntent = new Intent(Intent.ACTION_SEND);
            wechatIntent.setPackage("com.tencent.mm");
            wechatIntent.setType("text/plain");
            wechatIntent.putExtra(Intent.EXTRA_TEXT, inviteMessage);
            startActivity(wechatIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(MainWebActivity.this, "尚未安裝Wechat。", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGmail(String displayName, String inviteEmaiMessage) {
        try {
            Intent wechatIntent = new Intent(Intent.ACTION_SEND);
            wechatIntent.setPackage("com.google.android.gm");
            wechatIntent.setType("text/plain");
            wechatIntent.putExtra(Intent.EXTRA_TEXT, inviteEmaiMessage);
            {
                Uri uri = CommonUtils.genQRcodeAndSave(MainWebActivity.this, displayName);
                if (uri != null) {
                    wechatIntent.putExtra(Intent.EXTRA_STREAM, uri);
                }
            }
            wechatIntent.putExtra(Intent.EXTRA_SUBJECT, "一起來用Lale吧!");
            startActivity(wechatIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(MainWebActivity.this, "尚未安裝Gmail。", Toast.LENGTH_SHORT).show();
        }
    }

    private void openMessage(String inviteMessage) {

        Intent smsIntent = new Intent(Intent.ACTION_VIEW);
        smsIntent.setType("vnd.android-dir/mms-sms");
        smsIntent.putExtra("sms_body", inviteMessage);
        startActivity(smsIntent);

    }

    private ArrayList<String> getPhoneContactsData() {

        ArrayList<String> phones = new ArrayList<>();

        ContentResolver reContentResolverol = getContentResolver();

        Uri contactData = Uri.parse("content://com.android.contacts/contacts");
        @SuppressWarnings("deprecation")
        Cursor cursor = reContentResolverol.query(contactData, null, null, null, null);
        try {
            while (cursor.moveToNext())  // 將資料讀到最後一筆時會回傳false
            {

                @SuppressLint("Range") String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                Cursor phone = reContentResolverol.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId, null, null);

                while (phone.moveToNext()) {
                    @SuppressLint("Range") String phoneNumber = phone.getString(phone.getColumnIndex(
                            ContactsContract.CommonDataKinds.Phone.NUMBER));
                    phones.add(phoneNumber);


                }
            }
        } catch (Exception e) {
        }
        return phones;
    }

    class DownloadFileFromURL extends AsyncTask<String, String, String> {

        /**
         * Before starting background thread Show Progress Bar Dialog
         **/
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        /**
         * Downloading file in background thread
         **/
        @Override
        protected String doInBackground(String... f_url) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
                int count;
                try {
                    URL url = new URL(f_url[0]);
                    URLConnection conection = url.openConnection();
                    conection.connect();

                    // this will be useful so that you can show a tipical 0-100%
                    // progress bar
                    int lenghtOfFile = conection.getContentLength();

                    // download the file
                    InputStream input = new BufferedInputStream(url.openStream(),
                            8192);
                    final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS");
                    StringUtils.HaoLog("getContentType=" + conection.getContentType());
                    StringUtils.HaoLog("getContent=" + conection.getContent());
                    String fileName = sdf.format(new Date().getDate()) + StringUtils.toExtension(conection.getContentType());
                    String filePath = getExternalFilesDir(null) + "/"
                            + fileName;
                    // Output stream
                    OutputStream output = new FileOutputStream(filePath);

                    final int CREATE_FILE = 1;


                    byte data[] = new byte[1024];

                    long total = 0;

                    while ((count = input.read(data)) != -1) {
                        total += count;
                        // publishing the progress....
                        // After this onProgressUpdate will be called
                        publishProgress("" + (int) ((total * 100) / lenghtOfFile));

                        // writing data to file
                        output.write(data, 0, count);
                    }

                    // flushing output
                    output.flush();

                    // closing streams
                    output.close();
                    input.close();
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    File file = new File(filePath);
                    intent.setType(conection.getContentType());
                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, file);
                    activityReturn = new CallbackUtils.ActivityReturn() {
                        @Override
                        public void Callback(androidx.activity.result.ActivityResult activityResult) {
                            if (activityResult.getResultCode() == Activity.RESULT_OK) {
                                Uri uri = activityResult.getData().getData();
                                StringUtils.HaoLog("uri=" + uri);
                                int compareTo = new File(fileName).compareTo(new File(uri.getPath()));
                                StringUtils.HaoLog("compareTo=" + compareTo);
                            } else {
                                try {
                                    StringUtils.HaoLog("DownloadManager=end nook");
                                    sendToWeb(new JSONObject().put("type", "downloadFile").put("data", new JSONObject().put("isSuccess", false)).toString());

                                } catch (JSONException e) {
                                    e.printStackTrace();

                                }
                            }


                        }
                    };
                    runOnUiThread(() -> {
                        ActivityResult.launch(intent);
                    });


                } catch (Exception e) {
                    Log.e("Error: ", e.getMessage());
                }


            } else {
                int count;
                try {
                    URL url = new URL(f_url[0]);
                    URLConnection conection = url.openConnection();
                    conection.connect();

                    // this will be useful so that you can show a tipical 0-100%
                    // progress bar
                    int lenghtOfFile = conection.getContentLength();

                    // download the file
                    InputStream input = new BufferedInputStream(url.openStream(),
                            8192);
                    final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS");
                    StringUtils.HaoLog("getContentType=" + conection.getContentType());
                    StringUtils.HaoLog("getContent=" + conection.getContent());

                    // Output stream
                    OutputStream output = new FileOutputStream(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + "/"
                            + sdf.format(new Date().getDate()) + StringUtils.toExtension(conection.getContentType()));
                    byte data[] = new byte[1024];

                    long total = 0;

                    while ((count = input.read(data)) != -1) {
                        total += count;
                        // publishing the progress....
                        // After this onProgressUpdate will be called
                        publishProgress("" + (int) ((total * 100) / lenghtOfFile));

                        // writing data to file
                        output.write(data, 0, count);
                    }

                    // flushing output
                    output.flush();

                    // closing streams
                    output.close();
                    input.close();
                    try {
                        StringUtils.HaoLog("DownloadManager=end nook");
                        sendToWeb(new JSONObject().put("type", "downloadFile").put("data", new JSONObject().put("isSuccess", true)).toString());
                        return null;
                    } catch (JSONException e) {
                        e.printStackTrace();

                    }
                } catch (Exception e) {
                    Log.e("Error: ", e.getMessage());
                }

                try {
                    StringUtils.HaoLog("DownloadManager=end nook");
                    sendToWeb(new JSONObject().put("type", "downloadFile").put("data", new JSONObject().put("isSuccess", false)).toString());

                } catch (JSONException e) {
                    e.printStackTrace();

                }
            }
            return null;
        }

        /**
         * Updating progress bar
         **/
        protected void onProgressUpdate(String... progress) {
            // setting progress percentage

        }

        /**
         * After completing background task Dismiss the progress dialog
         **/
        @Override
        protected void onPostExecute(String file_url) {
            // dismiss the dialog after the file was downloade
        }
    }
    //endregion

    //region  sendToWebtest

    void sendToWebtest() {

        webView.evaluateJavascript("callAndroid2()", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String s) {
                Log.d("hao", "發訊息回傳:" + s);
            }
        });
    }

    void sendToWeb(String type, JSONObject data) {

        try {

            sendToWeb(new JSONObject().put("type", type).put("data", data).toString());
        } catch (JSONException e2) {
            StringUtils.HaoLog("sendToWeb Error=" + e2);
        }
    }

    void sendToWeb(String json) {
        checkHasWebView();
        webView.post(new Runnable() {
            @Override
            public void run() {
                StringUtils.HaoLog("jsp sendToWeb:" + TimeUtils.NowTimestamp() + "/" + json);

                webView.evaluateJavascript("receiveAppMessage('" + json + "')", new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String s) {
                        Log.d("hao", "發訊息回傳:" + s);
                    }
                });

            }
        });
    }

    @Override
    public void onBackPressed() {
        try {

            sendToWeb(new JSONObject().put("type", "back").toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    //endregion

    //region  舊版webView JavascriptInterface

    private String chromeCallbackUrl = "";
    private boolean needBack = false;

    @JavascriptInterface
    public void openWebViewByChrome(String url) {
        try {
            JSONObject j = new JSONObject().put("type", "webviewJI").put("name", "openWebViewByChrome(String url)").put("url", url);
            sendToWeb(j.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    @JavascriptInterface
    public void startScanQRCode(String msg) {
        try {
            openQRcode(new JSONObject().put("title", "QR code"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void closeNativeBrowser() {

        webView.evaluateJavascript("closeNativeBrowser()", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String s) {
                if (!chromeCallbackUrl.isEmpty()) {
                    wvloadUrl(chromeCallbackUrl);

                    chromeCallbackUrl = "";
                }
                if (needBack) {
                    wvGoBack();
                    needBack = false;
                }
            }

            private void wvGoBack() {

            }

            private void wvloadUrl(String chromeCallbackUrl) {
            }
        });
    }

    @JavascriptInterface
    public void freeWorkGroup(String msg)//創建免費工作群組 不明-等免費工作能做時再做
    {
        try {
            sendToWeb(new JSONObject().put("type", "webViewJSI").put("data", new JSONObject().put("name", "freeWorkGroup(String msg)").put("data", new JSONObject().put("msg", msg))).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void showSettingIcon(String json)//友圈網頁的退回變成設定按鈕 不知道用途
    {
        try {
            sendToWeb(new JSONObject().put("type", "webViewJSI").put("data", new JSONObject().put("name", "showSettingIcon(String json)").put("data", new JSONObject().put("json", json))).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void setNotificationBell(String notifyCount)//修改為讀數量 可能已經因為改版失效了
    {
        try {
            sendToWeb(new JSONObject().put("type", "webViewJSI").put("data", new JSONObject().put("name", "setNotificationBell(String notifyCount)").put("data", new JSONObject().put("notifyCount", notifyCount))).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void downloadSticker(String text)//下載貼圖 疑似改版完後用不到
    {
        try {
            sendToWeb(new JSONObject().put("type", "webViewJSI").put("data", new JSONObject().put("name", "downloadSticker(String text)").put("data", new JSONObject().put("text", text))).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void deleteSticker(String text)//刪除貼圖 疑似改版完後用不到
    {
        try {
            sendToWeb(new JSONObject().put("type", "webViewJSI").put("data", new JSONObject().put("name", "deleteSticker(String text)").put("data", new JSONObject().put("text", text))).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void isInLaleApp()//回復 evaluateJavascript(String.format("isInLaleApp('true')") 不知道幹啥用的
    {
        try {
            sendToWeb(new JSONObject().put("type", "webViewJSI").put("data", new JSONObject().put("name", "isInLaleApp()")).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void gotoCompanyTab()//退回辦公的首頁 從程式碼裡面看是把webview關掉
    {
        try {
            sendToWeb(new JSONObject().put("type", "webViewJSI").put("data", new JSONObject().put("name", "gotoCompanyTab()")).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void setToolbarTitle(String title)//修改該頁的標題
    {
        try {
            sendToWeb(new JSONObject().put("type", "webViewJSI").put("data", new JSONObject().put("name", "setToolbarTitle(String title)").put("data", new JSONObject().put("title", title))).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void backToPrevious(String backDashboard)//關掉這頁退回前一頁並刷新前一頁
    {
        try {
            sendToWeb(new JSONObject().put("type", "webViewJSI").put("data", new JSONObject().put("name", "backToPrevious(String backDashboard)").put("data", new JSONObject().put("backDashboard", backDashboard))).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void testCallback(String url)//Toast文字
    {
        try {
            sendToWeb(new JSONObject().put("type", "webViewJSI").put("data", new JSONObject().put("name", "testCallback(String url)").put("data", new JSONObject().put("url", url))).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void changeNickName(String displayName)//使用者改暱稱
    {
        try {
            sendToWeb(new JSONObject().put("type", "webViewJSI").put("data", new JSONObject().put("name", "changeNickName(String displayName)").put("data", new JSONObject().put("displayName", displayName))).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void getMyFriendList(String json)//回傳好友清單-需要參考資料
    {
        try {
            sendToWeb(new JSONObject().put("type", "webViewJSI").put("data", new JSONObject().put("name", "getMyFriendList(String json)").put("data", new JSONObject().put("json", json))).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void openChooseMember(String userIds, String id)//前往好友選取頁面 並把id記錄起來之後發送回去-需要參考資料
    {
        try {
            sendToWeb(new JSONObject().put("type", "webViewJSI").put("data", new JSONObject().put("name", "openChooseMember(String userIds, String id)").put("data", new JSONObject().put("userIds", userIds).put("id", id))).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void showToolbarMoreIcon(String type)//修改右上角按鈕的按下事件類型和換圖-需要參考資料
    {
        try {
            sendToWeb(new JSONObject().put("type", "webViewJSI").put("data", new JSONObject().put("name", "showToolbarMoreIcon(String type)").put("data", new JSONObject().put("type", type))).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void showToolbarSearchIcon(String url)//修改右上角按鈕的按下事件類型和換圖-需要參考資料
    {
        try {
            sendToWeb(new JSONObject().put("type", "webViewJSI").put("data", new JSONObject().put("name", "showToolbarSearchIcon(String url)").put("data", new JSONObject().put("url", url))).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void gotoAddFriend()//前往加好友頁面
    {
        try {
            sendToWeb(new JSONObject().put("type", "webViewJSI").put("data", new JSONObject().put("name", "gotoAddFriend()")).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void gotoFriendTabSetting()//前往我的設定 好友設定
    {
        try {
            sendToWeb(new JSONObject().put("type", "webViewJSI").put("data", new JSONObject().put("name", "gotoFriendTabSetting()")).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void reloadWeb()//刷新網頁
    {
        try {
            sendToWeb(new JSONObject().put("type", "webViewJSI").put("data", new JSONObject().put("name", "reloadWeb()")).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void openLaleCard(String userID)//開啟該使用者的卡片頁面
    {
        try {
            sendToWeb(new JSONObject().put("type", "webViewJSI").put("data", new JSONObject().put("name", "openLaleCard(String userID)").put("data", new JSONObject().put("userID", userID))).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void openMemiaRoom(String msg)//前往指定房間
    {
        try {
            sendToWeb(new JSONObject().put("type", "webViewJSI").put("data", new JSONObject().put("name", "openMemiaRoom(String msg)").put("data", new JSONObject().put("msg", msg))).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void getLaleJWT(String msg)//取得token
    {
        try {
            sendToWeb(new JSONObject().put("type", "webViewJSI").put("data", new JSONObject().put("name", "getLaleJWT(String msg)").put("data", new JSONObject().put("msg", msg))).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void openChatRoom(String msg)//前往指定房間
    {
        try {
            sendToWeb(new JSONObject().put("type", "webViewJSI").put("data", new JSONObject().put("name", "openChatRoom(String msg)").put("data", new JSONObject().put("msg", msg))).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void joinGroup(String msg)//前往加入群組的頁面
    {
        try {
            sendToWeb(new JSONObject().put("type", "webViewJSI").put("data", new JSONObject().put("name", "joinGroup(String msg)").put("data", new JSONObject().put("msg", msg))).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void createGroup(String msg)//創建群組
    {
        try {
            sendToWeb(new JSONObject().put("type", "webViewJSI").put("data", new JSONObject().put("name", "createGroup(String msg)").put("data", new JSONObject().put("msg", msg))).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void goBackAndReload()//退回上一頁
    {
        try {
            sendToWeb(new JSONObject().put("type", "webViewJSI").put("data", new JSONObject().put("name", "goBackAndReload()")).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void cancelFollowSuccess()//關掉此頁
    {
        try {
            sendToWeb(new JSONObject().put("type", "webViewJSI").put("data", new JSONObject().put("name", "cancelFollowSuccess")).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void openNewBrowser(String URL, boolean isForm) //開啟一個新的web view頁面
    {
        try {
            sendToWeb(new JSONObject().put("type", "webViewJSI").put("data", new JSONObject().put("name", "openNewBrowser(String URL, boolean isForm)").put("data", new JSONObject().put("URL", URL).put("isForm", isForm))).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void changeTitle(String Title)//修改標題文字
    {
        try {
            sendToWeb(new JSONObject().put("type", "webViewJSI").put("data", new JSONObject().put("name", "changeTitle(String Title)").put("data", new JSONObject().put("Title", Title))).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void closeWebView(String msg)//到該webView的第一頁
    {
        try {
            sendToWeb(new JSONObject().put("type", "webViewJSI").put("data", new JSONObject().put("name", "closeWebView(String msg)").put("data", new JSONObject().put("msg", msg))).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void shareToChatRoom(String msg)//分享訊息->前往分享頁面
    {
        try {
            sendToWeb(new JSONObject().put("type", "webViewJSI").put("data", new JSONObject().put("name", "shareToChatRoom(String msg)").put("data", new JSONObject().put("msg", msg))).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void returnEnterpriseMemberIDs(String members)//以這些成員資料前往創建群組頁面
    {
        try {
            sendToWeb(new JSONObject().put("type", "webViewJSI").put("data", new JSONObject().put("name", "returnEnterpriseMemberIDs(String members)").put("data", new JSONObject().put("members", members))).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void isCanShare(boolean bShare)//該網頁是否可以分享 右上角的按鈕按下的彈跳視窗中的分享功能
    {
        try {
            sendToWeb(new JSONObject().put("type", "webViewJSI").put("data", new JSONObject().put("name", "isCanShare(boolean bShare)").put("data", new JSONObject().put("bShare", bShare))).toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @JavascriptInterface
    public void getBase64FromBlobData(String base64Data) throws IOException {
        convertBase64StringToPdfAndStoreIt(base64Data);
    }

    private void convertBase64StringToPdfAndStoreIt(String base64Data) throws IOException {
        String fileName = FormatUtils.getDateFormat(System.currentTimeMillis(), "yyyyMMdd_HHmm");
        final File downloadFile = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS) + "/TaiwanPay_" + fileName + ".jpg");
        String base64Img = base64Data.split(",")[1];
        byte[] imgBytesData = android.util.Base64.decode(base64Img, android.util.Base64.DEFAULT);
        FileOutputStream os;
        os = new FileOutputStream(downloadFile);
        os.write(imgBytesData);
        os.flush();

        if (downloadFile.exists()) {
            Toast.makeText(this, "下載成功", Toast.LENGTH_SHORT).show();
        }
    }

    @JavascriptInterface
    public void openWithBrowser(String url) {
        webView.post(new Runnable() {
            @Override
            public void run() {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
            }
        });
    }

    @JavascriptInterface
    public void openWithChrome(String url) {
        try {
            JSONObject j = new JSONObject().put("type", "webviewJI").put("name", "openWithChrome(String url)").put("url", url);
            sendToWeb(j.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private Bitmap mBitmap;

    @JavascriptInterface
    public void downloadPhotoFromWeb(String json) {
        StringUtils.HaoLog("JS downloadPhotoFromWeb()");
        showWait();
        mBitmap = null;
        List<String> photos = new ArrayList<>();
        webView.post(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject jo = new JSONObject(json);
                    com.flowring.laleents.tools.Log.d("downloadPhotoFromWeb", jo.toString());

                    JSONArray data = jo.optJSONArray("photos");
                    for (int i = 0; i < data.length(); i++) {
                        photos.add((String) data.opt(i));
                    }

                    AtomicInteger count = new AtomicInteger();
                    ArrayList<Observable<Boolean>> observables = new ArrayList<>();
                    for (String imagePath : photos) {
                        observables.add(Observable.create(new ObservableOnSubscribe<Boolean>() {
                            @Override
                            public void subscribe(ObservableEmitter<Boolean> emitter) throws Exception {
                                Glide.with(MainWebActivity.this)
                                        .asBitmap()
                                        .load(imagePath)
                                        .apply(new RequestOptions()
                                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                                .placeholder(R.drawable.img_default)
                                                .fitCenter())
                                        .into(new CustomTarget<Bitmap>() {
                                            @Override
                                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                                mBitmap = resource;
                                                if (mBitmap != null) {
                                                    //图片信息不为空时才保存
                                                    String fileName = FormatUtils.getDateFormat(System.currentTimeMillis(), "yyyyMMddHHmmss");
                                                    Uri uri = FileUtils.saveBitmapToGallery(MainWebActivity.this, fileName, mBitmap);
                                                    emitter.onNext(uri != null);
                                                }
                                            }

                                            @Override
                                            public void onLoadCleared(@Nullable Drawable placeholder) {
                                            }
                                        });
                            }
                        }).subscribeOn(AndroidSchedulers.mainThread()));
                    }
                    Observable.merge(observables).observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Consumer<Boolean>() {
                                @Override
                                public void accept(Boolean b) throws Exception {
                                    if (b) {
                                        count.addAndGet(1);
                                    }
                                    if (photos.size() == count.get()) {
                                        cancelWait();
                                        Toast.makeText(MainWebActivity.this, "已下載", Toast.LENGTH_SHORT).show();
                                    }

                                }
                            }, new Consumer<Throwable>() {
                                @Override
                                public void accept(Throwable throwable) throws Exception {
                                    Toast.makeText(MainWebActivity.this, "下載失敗", Toast.LENGTH_SHORT).show();
                                }
                            }, new Action() {
                                @Override
                                public void run() throws Exception {

                                }
                            });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    //endregion


}