package com.flowring.laleents.tools;

import android.app.DownloadManager;
import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.webkit.CookieManager;

import com.flowring.laleents.R;
import com.flowring.laleents.tools.download.DownloadBroadcastReceiver;
import com.flowring.laleents.ui.main.webBody.MainWebActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class DownloadUtils {
    public static void shareFileTypeDownload(String inviteMessage, String oldFileName, Context context){
        String urlString = null;
        String onlyKey = null;
        try{
            JSONArray jsonArray = new JSONArray(inviteMessage);
            JSONObject jsonObject = jsonArray.getJSONObject(0);
            urlString = jsonObject.optString("url");
            onlyKey = jsonObject.optString("onlyKey");
        }catch (JSONException e){
            e.printStackTrace();
        }
        Resources resources = context.getResources();
        String app_name = resources.getString(R.string.app_name);
        String fileName = oldFileName == null ? StringUtils.getNewString(onlyKey) : oldFileName;
        String tableOfContents = Environment.DIRECTORY_PICTURES + File.separator + app_name + File.separator + "sharefile";
        File folder = new File(Environment.getExternalStoragePublicDirectory(tableOfContents), fileName);
        if(folder.exists()){
            StringUtils.HaoLog("檔案存在");
            new Handler(Looper.getMainLooper()).post(() -> {
                MainWebActivity.shareFileType(folder,context);
            });
        } else {
            String url = urlString;
            new Thread(() -> {
                DownloadManager.Request request;
                try{
                    request = new DownloadManager.Request(Uri.parse(url));
                }catch (IllegalArgumentException e){
                    StringUtils.HaoLog("cacheShareFileType requestError=" + e);
                    return;
                }
                String cookies = CookieManager.getInstance().getCookie(url);
                request.addRequestHeader("cookie", cookies);
                request.addRequestHeader("User-Agent", "");
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);

                request.setDestinationUri(Uri.fromFile(folder));
                DownloadManager downloadManager = (DownloadManager) context.getSystemService(context.DOWNLOAD_SERVICE);
                long id = downloadManager.enqueue(request);
                DownloadBroadcastReceiver.sharelistener(context,id,folder);
            }).start();
        }
    }
}
