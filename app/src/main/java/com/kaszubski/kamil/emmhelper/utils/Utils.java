package com.kaszubski.kamil.emmhelper.utils;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class Utils {
    private static final String TAG = "Utils";
    private static Toast toast;


    public static void showToast(Context context, String message){
        if(toast != null)
            toast.cancel();
        toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toast.show();
    }

    public static void copyToClipboard(Context context, String text){
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                context.getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText
                ("Copied Text", text);
        clipboard.setPrimaryClip(clip);

        showToast(context, "Package name copied");
    }

    public static Dialog getConfirmationDialog(final Context context, String title, DialogInterface.OnClickListener action){
        return new AlertDialog.Builder(context).setMessage(title)
                .setPositiveButton("Confirm", action)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Utils.showToast(context, "Cancelled");
                    }
                }).create();
    }

    public static void showAppInfo(Context context, String packageName){
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", packageName, null);
        intent.setData(uri);
        context.startActivity(intent);
    }

    public static void sharePackageName(Context context, String packageName){
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, packageName);
        context.startActivity(Intent.createChooser(intent, "Share via"));
    }

    public static void extractAPK(Context context, PackageInfo packageInfo){
        showToast(context, "Extracting in background");
        new ExtractAPK(context, packageInfo).execute();
    }

    static class ExtractAPK extends AsyncTask<Void, Void, Boolean>{
        private Context context;
        private PackageInfo packageInfo;
        private String outputFile;

        public ExtractAPK(Context context, PackageInfo packageInfo){
            this.context = context;
            this.packageInfo = packageInfo;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            File source = new File(packageInfo.applicationInfo.sourceDir);

            InputStream in;
            OutputStream out;

            try {

                //create output directory if it doesn't exist
                File dir = new File (Environment.getExternalStorageDirectory(), "Extracted APKs");
                if (!dir.exists())
                {
                    if(!dir.mkdirs())
                        return false;
                }


                in = new FileInputStream(source);
                outputFile = dir.getPath() + "/" + source.getName();
                out = new FileOutputStream(outputFile);

                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                in.close();

                // write the output file (You have now copied the file)
                out.flush();
                out.close();
                return true;
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);

            if(aBoolean)
                showToast(context, "APK extracted to:\n"+outputFile);
            else
                showToast(context, "Extraction failed");
        }
    }
}
