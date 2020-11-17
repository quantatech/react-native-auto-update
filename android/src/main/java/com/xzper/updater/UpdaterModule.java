package com.xzper.updater;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import ezy.boost.update.*;

import android.content.Context;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.text.format.Formatter;
import androidx.appcompat.app.AlertDialog;
import android.widget.TextView;
import android.text.method.ScrollingMovementMethod;
import android.content.DialogInterface;
import android.app.ProgressDialog;

public class UpdaterModule extends ReactContextBaseJavaModule {

    class MyPromptClickListener implements DialogInterface.OnClickListener {
        private final IUpdateAgent mAgent;
        private final boolean mIsAutoDismiss;

        public MyPromptClickListener(IUpdateAgent agent, boolean isAutoDismiss) {
            mAgent = agent;
            mIsAutoDismiss = isAutoDismiss;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {

            switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
            {
                String url = mAgent.getInfo().url;

                if (url.startsWith("http") || url.startsWith("ftp")) {
                    mAgent.update();
                } else {
                    Uri uri = Uri.parse(url);
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    getCurrentActivity().startActivity(intent);
                }
                break;
            }
            case DialogInterface.BUTTON_NEUTRAL:
                mAgent.ignore();
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                // not now
                break;
            }
            if (mIsAutoDismiss) {
                dialog.dismiss();
            }
        }
    }

    class MyUpdatePrompter implements IUpdatePrompter {

        private Context mContext;
        private String mLanguage;

        public MyUpdatePrompter(Context context, String language) {
            mContext = context;
            mLanguage = language;
        }

        // zh-CN en-US
        private boolean isChinese(){
            return mLanguage.startsWith("zh");
        }

        @Override
        public void prompt(IUpdateAgent agent) {
            if (mContext instanceof Activity && ((Activity) mContext).isFinishing()) {
                return;
            }
            final UpdateInfo info = agent.getInfo();
            String size = Formatter.formatShortFileSize(mContext, info.size);

            String versionStr = mContext.getString(R.string.auto_update_new_version);

            String content = String.format(versionStr, info.versionName, size, info.updateContent);

            final AlertDialog dialog = new AlertDialog.Builder(mContext).create();

            String strTitle = mContext.getString(R.string.auto_update_version_update);

            dialog.setTitle(strTitle);
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);


            float density = mContext.getResources().getDisplayMetrics().density;
            TextView tv = new TextView(mContext);
            tv.setMovementMethod(new ScrollingMovementMethod());
            tv.setVerticalScrollBarEnabled(true);
            tv.setTextSize(14);
            tv.setMaxHeight((int) (250 * density));

            dialog.setView(tv, (int) (25 * density), (int) (15 * density), (int) (25 * density), 0);


            DialogInterface.OnClickListener listener = new MyPromptClickListener(agent, true);

            if (info.isForce) {
                String strTips = mContext.getString(R.string.auto_update_force_update);
                tv.setText(strTips + content);

                String strOk = mContext.getString(R.string.auto_update_ok);
                dialog.setButton(DialogInterface.BUTTON_POSITIVE, strOk, listener);
            } else {
                tv.setText(content);

                String strImmediately = mContext.getString(R.string.auto_update_immediately);
                dialog.setButton(DialogInterface.BUTTON_POSITIVE, strImmediately, listener);

                String strLater = mContext.getString(R.string.auto_update_later);
                dialog.setButton(DialogInterface.BUTTON_NEGATIVE, strLater, listener);
                if (info.isIgnorable) {

                    String strIgnore = mContext.getString(R.string.auto_update_ignore);
                    dialog.setButton(DialogInterface.BUTTON_NEUTRAL, strIgnore, listener);
                }
            }
            dialog.show();
        }
    }

    class MyDialogDownloadListener implements OnDownloadListener {
        private Context mContext;
        private ProgressDialog mDialog;
        private String mLanguage;

        public MyDialogDownloadListener(Context context, String language) {
            mContext = context;
            mLanguage = language;
        }

        // zh-CN en-US
        private boolean isChinese(){
            return mLanguage.startsWith("zh");
        }

        @Override
        public void onStart() {
            if (mContext instanceof Activity && !((Activity) mContext).isFinishing()) {

                String strLoading = mContext.getString(R.string.auto_update_loading);

                ProgressDialog dialog = new ProgressDialog(mContext);
                dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                dialog.setMessage(strLoading);
                dialog.setIndeterminate(false);
                dialog.setCancelable(false);
                dialog.show();
                mDialog = dialog;
            }
        }

        @Override
        public void onProgress(int i) {
            if (mDialog != null) {
                mDialog.setProgress(i);
            }
        }

        @Override
        public void onFinish() {
            if (mDialog != null) {
                mDialog.dismiss();
                mDialog = null;
            }
        }
    }

    private ReactApplicationContext context;
    private String language;

    public UpdaterModule(ReactApplicationContext reactContext) {
        super(reactContext);
        context = reactContext;

    }

    @Override
    public String getName() {
        return "Updater";
    }

    @ReactMethod
    public void setUrl(String url, String channel, String language) {
        UpdateManager.setUrl(url, channel);
        this.language = language;
    }

    @ReactMethod
    public void check() {
        UpdateManager.create(getCurrentActivity())
                .setPrompter(new MyUpdatePrompter(getCurrentActivity(), language))
                .setOnDownloadListener(new MyDialogDownloadListener(getCurrentActivity(), language))
                .check();
    }

    @ReactMethod
    public void checkManual() {
        UpdateManager.create(getCurrentActivity())
                .setPrompter(new MyUpdatePrompter(getCurrentActivity(), language))
                .setOnDownloadListener(new MyDialogDownloadListener(getCurrentActivity(), language))
                .setManual(true)
                .check();
    }
}