package com.kaszubski.kamil.emmhelper.utils;

import android.app.Activity;
import android.app.Dialog;
import android.app.enterprise.license.EnterpriseLicenseManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatTextView;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.kaszubski.kamil.emmhelper.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class Utils {
    private static final String TAG = "Utils";
    private static final SparseIntArray mapCodes = new SparseIntArray();

    /**
     Displays Toast message
     @param context    the calling component context
     @param message    the message displayed in Toast
     */
    public static void displayToast(Context context, String message) {
        displayToast(context, message, false);
    }

    /**
     Displays Toast message
     @param context         the calling component context
     @param message         the message displayed in Toast
     @param printInLogs     true to print message in logs
     */
    public static void displayToast(final Context context, final String message, boolean printInLogs) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        });
        if (printInLogs)
            Log.d(context.getClass().getSimpleName(), message);
    }

    public static void copyToClipboard(Context context, String text){
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                context.getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText
                (context.getString(R.string.copied_text), text);
        clipboard.setPrimaryClip(clip);

        displayToast(context, context.getString(R.string.package_name_copied));
    }

    public static Dialog getConfirmationDialog(final Context context, String title, DialogInterface.OnClickListener action){
        return new AlertDialog.Builder(context).setMessage(title)
                .setPositiveButton(context.getString(R.string.confirm), action)
                .setNegativeButton(context.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        displayToast(context, context.getString(R.string.canceled));
                    }
                }).create();
    }

    /**
     Interface to provide TextSet Callback
     */
    public interface OnTextSet {
        void onTextSet(String text);
    }

    /**
     Interface to provide CheckBoxChecked Callback
     */
    public interface OnConfirmCheckBoxDialog {
        void onConfirmCheckBoxDialog(boolean isChecked);
    }

    /**
     * Generate AlertDialog with EditText
     *
     * @param context           context of caller
     * @param title             title of AlertDialog
     * @param defaultText       default text for EditText
     * @param hint              hint for the user
     * @param confirmAction     action to be performed when confirm button is clicked
     * @param cancelAction      action to be performed when cancel button is clicked
     * @param dismissOnConfirm  true to dismiss dialog when confirm button is clicked, false otherwise
     * @return Composed Dialog
     */
    public static Dialog getEdiTextDialog(final Context context, final String title, String defaultText, String hint,
                                          final OnTextSet confirmAction, DialogInterface.OnClickListener cancelAction, boolean dismissOnConfirm) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = ((Activity)context).getLayoutInflater();
        View layout = inflater.inflate(R.layout.dialog_edit_text, null);
        final EditText editText = (EditText) layout.findViewById(R.id.editText);
        TextInputLayout editTextLayout = (TextInputLayout) layout.findViewById(R.id.editTextLayout);
        editText.setText(defaultText);
        editTextLayout.setHint(hint);

        editText.setSelection(0, editText.length());
        builder.setTitle(title).setView(layout)
                .setNegativeButton(R.string.cancel, cancelAction != null ? cancelAction : new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        displayToast(context, context.getString(R.string.canceled));
                    }
                }).setPositiveButton(R.string.confirm, dismissOnConfirm ? new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (confirmAction != null)
                    confirmAction.onTextSet(editText.getText().toString().trim());
            }
        } : null);

        final Dialog dialog = builder.create();
        if (!dismissOnConfirm) {
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogInterface) {
                    Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (confirmAction != null)
                                confirmAction.onTextSet(editText.getText().toString().trim());
                        }
                    });
                }
            });
        }

        return dialog;
    }

    public static Dialog getMessageWithCheckBoxDialog(final Context context, final String title, String message, String checkBoxText,
                                                      final OnConfirmCheckBoxDialog confirmAction,  boolean dismissOnConfirm){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = ((Activity)context).getLayoutInflater();
        View layout = inflater.inflate(R.layout.dialog_message_and_checkbox, null);
        AppCompatTextView messageTextView = (AppCompatTextView) layout.findViewById(R.id.message);
        final AppCompatCheckBox checkBox = (AppCompatCheckBox) layout.findViewById(R.id.checkBox);
        messageTextView.setText(message);
        checkBox.setText(checkBoxText);

        builder.setTitle(title).setView(layout)
                .setPositiveButton(R.string.confirm, dismissOnConfirm ? new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (confirmAction != null)
                    confirmAction.onConfirmCheckBoxDialog(checkBox.isChecked());
            }
        } : null);

        final Dialog dialog = builder.create();
        if (!dismissOnConfirm) {
            dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialogInterface) {
                    Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (confirmAction != null)
                                confirmAction.onConfirmCheckBoxDialog(checkBox.isChecked());
                        }
                    });
                }
            });
        }

        return dialog;
    }

    public static void showAppInfo(Context context, String packageName){
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", packageName, null);
        intent.setData(uri);
        context.startActivity(intent);
    }

    public static void shareViaList(Context context, String packageName){
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, packageName);
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_via)));
    }

    public static void extractAPK(final Context context, PackageInfo packageInfo){
        displayToast(context, context.getString(R.string.extracting_in_background));
        new ExtractAPK(packageInfo, new OnExtractionFinishListener() {
            @Override
            public void onExtractionFinished(String outputFile, boolean success) {
                if(success)
                    displayToast(context, context.getString(R.string.apk_extracted_to)+outputFile);
                else
                    displayToast(context, context.getString(R.string.extraction_failed));
            }
        }).execute();
    }

    //Maps error codes to error descriptions
    private static void populateCodes() {
        mapCodes.clear();
        mapCodes.put(EnterpriseLicenseManager.ERROR_INTERNAL,
                R.string.err_internal);

        mapCodes.put(EnterpriseLicenseManager.ERROR_INTERNAL_SERVER,
                R.string.err_internal_server);

        mapCodes.put(EnterpriseLicenseManager.ERROR_INVALID_LICENSE,
                R.string.err_licence_invalid_license);

        mapCodes.put(EnterpriseLicenseManager.ERROR_INVALID_PACKAGE_NAME,
                R.string.err_invalid_package_name);

        mapCodes.put(EnterpriseLicenseManager.ERROR_LICENSE_TERMINATED,
                R.string.err_licence_terminated);

        mapCodes.put(EnterpriseLicenseManager.ERROR_NETWORK_DISCONNECTED,
                R.string.err_network_disconnected);

        mapCodes.put(EnterpriseLicenseManager.ERROR_NETWORK_GENERAL,
                R.string.err_network_general);

        mapCodes.put(EnterpriseLicenseManager.ERROR_NOT_CURRENT_DATE,
                R.string.err_unknown);

        mapCodes.put(EnterpriseLicenseManager.ERROR_NULL_PARAMS,
                R.string.err_not_current_date);

        mapCodes.put(EnterpriseLicenseManager.ERROR_SIGNATURE_MISMATCH,
                R.string.err_signature_mismatch);

        mapCodes.put(EnterpriseLicenseManager.ERROR_UNKNOWN,
                R.string.err_null_params);

        mapCodes.put(
                EnterpriseLicenseManager.ERROR_USER_DISAGREES_LICENSE_AGREEMENT,
                R.string.err_user_disagrees_license_agreement);
        mapCodes.put(EnterpriseLicenseManager.ERROR_VERSION_CODE_MISMATCH,
                R.string.err_version_code_mismatch);
    }

    /**
     Gets the message to be displayed to user by passing in code
     @param  context the Activity context
     @param  code    the error code
     @return         the message corresponding to the code
     */
    public static String getMessage(Context context, int code) {
        if(mapCodes.size() == 0)
            populateCodes();
        if (mapCodes.get(code) != 0)
            return context.getString(mapCodes.get(code));
        else return "";
    }

    private static class ExtractAPK extends AsyncTask<Void, Void, Boolean>{
        private PackageInfo packageInfo;
        private String outputFile;
        private OnExtractionFinishListener listener;

        ExtractAPK(PackageInfo packageInfo, OnExtractionFinishListener listener){
            this.packageInfo = packageInfo;
            this.listener = listener;
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
                outputFile = dir.getPath() + "/" + packageInfo.packageName + ".apk";
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
            if(listener != null)
                listener.onExtractionFinished(outputFile, aBoolean);
        }
    }

    public interface OnExtractionFinishListener {
        void onExtractionFinished(String outputFile, boolean success);
    }

//    public interface OnDecodeFinishListener {
//        void onDecodeFinished(String xml, boolean launcherReady);
//    }
//
//    public static void decodeXmlFile(Context context, String source, boolean lookingForLauncher, OnDecodeFinishListener listener){
//        new DecodeXML(context, source, lookingForLauncher, listener).execute();
//    }

//    private static class DecodeXML extends AsyncTask<Void, Void, String> {
//        private Context context;
//        private String source;
//        private boolean lookingForLauncher;
//        private OnDecodeFinishListener listener;
//        private static SparseArray<String> configMap;
//        private static int mapSize;
//        private static boolean launcherReady;
//
//        DecodeXML(Context context, String source, boolean lookingForLauncher, OnDecodeFinishListener listener) {
//            this.context = context;
//            this.source = source;
//            this.lookingForLauncher = lookingForLauncher;
//            this.listener = listener;
//        }
//
//        @Override
//        protected String doInBackground(Void... params) {
//            try {
//                String fileName = source;
//                InputStream is;
//                ZipFile zip = null;
//                if (fileName.endsWith(".apk") || fileName.endsWith(".zip")) {
//                    zip = new ZipFile(fileName);
//                    ZipEntry mft = zip.getEntry("AndroidManifest.xml");
//                    is = zip.getInputStream(mft);
//                } else {
//                    is = new FileInputStream(fileName);
//                }
//                byte[] buf = new byte[500000];
//                int bytesRead = is.read(buf);
//                is.close();
//                if (zip != null) {
//                    zip.close();
//                }
//                String xml;
//                if (bytesRead > 0) {
//                    if(lookingForLauncher)
//                        launcherReady = false;
//                    xml = decompressXML(buf, lookingForLauncher);
////                    System.out.println(xml);
//                } else {
//                    xml = context.getString(R.string.reading_failed);
//                }
//                return xml != null ? xml.trim() : null;
//            } catch (Exception e) {
//                System.out.println(e.toString());
//                return null;
//            }
//        }
//
//        @Override
//        protected void onPostExecute(String s) {
//            super.onPostExecute(s);
//            if (listener != null)
//                listener.onDecodeFinished(s, launcherReady);
//        }
//
//
//        private String decompressXML(byte[] xml, boolean lookingForLauncher) {
//            int launcherCapabilities = 0;
//            StringBuilder finalXML = new StringBuilder();
//            int level = 0;
//            int numbStrings = LEW(xml, 4 * 4);
//            int sitOff = 0x24; // Offset of start of StringIndexTable
//            int stOff = sitOff + numbStrings * 4; // StringTable follows
//            int xmlTagOff = LEW(xml, 3 * 4); // Start from the offset in the 3rd
//
//            int startTag = 0x00100102;
//            for (int ii = xmlTagOff; ii < xml.length - 4; ii += 4) {
//                if (LEW(xml, ii) == startTag) {
//                    xmlTagOff = ii;
//                    break;
//                }
//            } // end of hack, scanning for start of first start tag
//            boolean wasEndTagPreviously = false;
//            boolean firstLine = true;
//            int off = xmlTagOff;
//            int indent = 0;
//            int startTagLineNo = -2;
//            String tab = "      ";
//            int k = 0;
//            while (off < xml.length) {
//                k++;
//                Log.e(TAG, "While "+k);
//                Log.e(TAG, "off " + off + " length " + xml.length);
//                int tag0 = LEW(xml, off);
//                int lineNo = LEW(xml, off + 2 * 4);
//                int nameNsSi = LEW(xml, off + 4 * 4);
//                int nameSi = LEW(xml, off + 5 * 4);
//                int endDocTag = 0x00100101;
//                int endTag = 0x00100103;
//                if (tag0 == startTag) { // XML START TAG
//                    Log.e(TAG, "start "+k);
//                    if (!firstLine) {
//                        if (!wasEndTagPreviously)
//                            finalXML.append(">").append("\n").append("\n");
//                        else
//                            finalXML.append("\n");
//                    } else {
//                        firstLine = false;
//                    }
//
//                    int tag6 = LEW(xml, off + 6 * 4); // Expected to be 14001400
//                    int numbAttrs = LEW(xml, off + 7 * 4); // Number of Attributes
//                    off += 9 * 4; // Skip over 6+3 words of startTag data
//                    String name = compXmlString(xml, sitOff, stOff, nameSi);
//
//                    if (name != null && name.equals("intent-filter"))
//                        launcherCapabilities = 0;
//
//                    startTagLineNo = lineNo;
//                    // Look for the Attributes
//                    StringBuilder sb = new StringBuilder();
//                    boolean firstAttr = true;
//                    for (int ii = 0; ii < numbAttrs; ii++) {
//                        int attrNameNsSi = LEW(xml, off); // AttrName Namespace Str
//                        int attrNameSi = LEW(xml, off + 4); // AttrName String
//                        int attrValueSi = LEW(xml, off + 2 * 4); // AttrValue Str
//                        int attrFlags = LEW(xml, off + 3 * 4);
//                        int attrResId = LEW(xml, off + 4 * 4); // AttrValue
//                        off += 5 * 4; // Skip over the 5 words of an attribute
//                        String attrName = compXmlString(xml, sitOff, stOff, attrNameSi);
//
//                        Log.e(TAG, "name " + attrName + " flag " + attrFlags + " val " + attrResId + " bin " + Integer.toBinaryString(attrResId));
//
//                        String attrValue = attrValueSi != -1 ? compXmlString(xml,
//                                sitOff, stOff, attrValueSi) : decodeAttrValue(attrName, attrFlags, attrResId);
//
//                        if (!firstAttr) {
//                            sb.append("\n");
//                            for (int i = 0; i < level + 1; i++)
//                                sb.append(tab);
//                        } else {
//                            firstAttr = false;
//                            sb.append(" ");
//                        }
//
//                        sb.append(attrName).append("=\"").append(attrValue).append("\"");
////                        Log.e(TAG, "name " + name);
//                        if (lookingForLauncher && name != null && attrValue != null && name.equals("category")) {
////                            Log.e(TAG, "LOOKING " + launcherCapabilities + " atr " + attrValue);
//                            if (attrValue.equals("android.intent.category.HOME")) {//looking for launcher category
//                                launcherCapabilities++;
////                                Log.e(TAG, "HOME HIT " + launcherCapabilities);
//                            } else if (attrValue.equals("android.intent.category.DEFAULT")) {//looking for launcher category
//                                launcherCapabilities++;
////                                Log.e(TAG, "DEFAULT HIT " + launcherCapabilities);
//                            }
//                            if (launcherCapabilities == 2) {
////                                Log.e(TAG, "READY ");
//                                launcherReady = true;
//                                return null;
//                            }
//                        }
//                    }
//                    for (int i = 0; i < level; i++)
//                        finalXML.append(tab);
//                    level++;
//
//                    //finalXML.append("<").append(name).append(sb).append(">");
//                    finalXML.append("<").append(name).append(sb);
////                    prtIndent(indent, "<" + name + sb + ">");
//                    indent++;
//                    wasEndTagPreviously = false;
//                    Log.e(TAG, "start end "+k);
//                } else if (tag0 == endTag) { // XML END TAG
//                    Log.e(TAG, "end "+k);
//                    indent--;
//                    level--;
//                    off += 6 * 4; // Skip over 6 words of endTag data
//                    String name = compXmlString(xml, sitOff, stOff, nameSi);
//                    if (wasEndTagPreviously) {
//                        finalXML.append("\n");
//                        for (int i = 0; i < level; i++)
//                            finalXML.append(tab);
//                        finalXML.append("</").append(name).append(">").append("\n");
//                    } else
//                        finalXML.append("/>").append("\n");
////                finalXML.append("</").append(name).append(">");
////                    prtIndent(indent, "</" + name + "> (line " + startTagLineNo + "-" + lineNo + ")");
//                    wasEndTagPreviously = true;
//                    Log.e(TAG, "end end "+k);
//                } else if (tag0 == endDocTag) { // END OF XML DOC TAG
//                    Log.e(TAG, "end of doc "+k);
//                    break;
//                } else {
//                    Log.e(TAG, "unrecognized "+k);
//                    wasEndTagPreviously = false;
//                    off ++;
//                    //break;
//                }
//            } // end of while loop scanning tags and attributes of XML tree
//            return finalXML.toString();
//        } // end of decompressXML
//
//        private static String decodeAttrValue(String attrName, int type, int value) {
//            switch (type) {
//                case 268435464: //number
//                    switch (attrName) {
//                        case "screenOrientation":
//                            return getScreenOrientationName(value);
//                        case "launchMode":
//                            return getLaunchModeName(value);
//                        default:
//                            return String.valueOf(value);
////                        return  "resourceID 0x" + Integer.toHexString(value);
//                    }
//                case 285212680:
//                    switch (attrName) {
//                        case "windowSoftInputMode":
//                            return getWindowSoftInputModeName(value);
//                        case "protectionLevel":
//                            return getProtectionLevelName(value);
//                        case "configChanges":
//                            return getConfigChangesName(value);
//                        default:
////                        return String.valueOf(value);
//                            return "resourceID 0x" + Integer.toHexString(value);
//                    }
//
//                case 301989896: //boolean
//                    return value == -1 ? "true" : "false";
//                default:
//                    return "resourceID 0x" + Integer.toHexString(value);
//            }
//        }
//
//        private static String getScreenOrientationName(int value) {
//            switch (value) {
//                case -1:
//                    return "unspecified";
//                case 0:
//                    return "landscape";
//                case 1:
//                    return "portrait";
//                case 2:
//                    return "user";
//                case 3:
//                    return "behind";
//                case 4:
//                    return "sensor";
//                case 5:
//                    return "nosensor";
//                case 6:
//                    return "sensorLandscape";
//                case 7:
//                    return "sensorPortrait";
//                case 8:
//                    return "reverseLandscape";
//                case 9:
//                    return "reversePortrait";
//                case 10:
//                    return "fullSensor";
//                case 11:
//                    return "userLandscape";
//                case 12:
//                    return "userPortrait";
//                case 13:
//                    return "fullUser";
//                case 14:
//                    return "locked";
//                default:
//                    return "unspecified";
//            }
//        }
//
//        private static String getLaunchModeName(int value) {
//            switch (value) {
//                case 0:
//                    return "standard";
//                case 1:
//                    return "singleTop";
//                case 2:
//                    return "singleTask";
//                case 3:
//                    return "singleInstance";
//                default:
//                    return "standard";
//            }
//        }
//
//        private static String getWindowSoftInputModeName(int value) {
//            switch (value) {
//                case 0:
//                    return "stateUnspecified";
//                case 1:
//                    return "stateUnchanged";
//                case 2:
//                    return "stateHidden";
//                case 3:
//                    return "stateAlwaysHidden";
//                case 4:
//                    return "stateVisible";
//                case 5:
//                    return "stateAlwaysVisible";
//                case 16:
//                    return "adjustResize";
//                case 32:
//                    return "adjustPan";
//                default:
//                    return "adjustUnspecified";
//            }
//        }
//
//        private static String getProtectionLevelName(int value) {
//            switch (value) {
//                case 0:
//                    return "normal";
//                case 1:
//                    return "dangerous";
//                case 2:
//                    return "signature";
//                case 3:
//                    return "signatureOrSystem";
//                case 16:
//                    return "privileged";
//                default:
//                    return "normal";
//            }
//        }
//
//        private static String getConfigChangesName(int value) {
//            if (configMap == null)
//                initConfigMap();
//
//            int valueToCheck = value;
//            StringBuilder builder = new StringBuilder();
//
//            while (valueToCheck != 0) {
//                for (int i = 0; i < mapSize; i++) {
//
//                    if (valueToCheck < configMap.keyAt(i)) {
//
//                        valueToCheck -= configMap.keyAt(i - 1);
//                        if(builder.toString().contains(configMap.valueAt(i -1))) //prevent wrong values and loop
//                            return "Unable to decode";
//                        builder.append(configMap.valueAt(i - 1)).append(" | ");
//                        break;
//                    }
//                    if (i == mapSize - 1) {
//                        valueToCheck -= configMap.keyAt(mapSize - 1);
//                        if(builder.toString().contains(configMap.valueAt(i - 1)))
//                            return "Unable to decode";
//                        builder.append(configMap.valueAt(mapSize - 1)).append(" | ");
//                        break;
//                    }
//                }
//            }
//            return builder.toString().substring(0, builder.length() - 3);
//        }
//
//        private static void initConfigMap() {
//            configMap = new SparseArray<>(15);
//            configMap.put(1, "mcc");
//            configMap.put(2, "mnc");
//            configMap.put(4, "locale");
//            configMap.put(8, "touchscreen");
//            configMap.put(16, "keyboard");
//            configMap.put(32, "keyboardHidden");
//            configMap.put(64, "navigation");
//            configMap.put(128, "orientation");
//            configMap.put(256, "screenLayout");
//            configMap.put(512, "uiMode");
//            configMap.put(1024, "screenSize");
//            configMap.put(2048, "smallestScreenSize");
//            configMap.put(4096, "density");
//            configMap.put(8192, "layoutDirection");
//            configMap.put(1073741824, "fontScale");
//
//            mapSize = configMap.size();
//        }
//
//        private static String compXmlString(byte[] xml, int sitOff, int stOff, int strInd) {
//            if (strInd < 0)
//                return null;
//            int strOff = stOff + LEW(xml, sitOff + strInd * 4);
//            return compXmlStringAt(xml, strOff);
//        }
//
//        private static String compXmlStringAt(byte[] arr, int strOff) {
//            int strLen = arr[strOff + 1] << 8 & 0xff00 | arr[strOff] & 0xff;
//            byte[] chars = new byte[strLen];
//            for (int ii = 0; ii < strLen; ii++) {
//                chars[ii] = arr[strOff + 2 + ii * 2];
//            }
//            return new String(chars); // Hack, just use 8 byte chars
//        } // end of compXmlStringAt
//
//        private static int LEW(byte[] arr, int off) {
//            return arr[off + 3] << 24 & 0xff000000 | arr[off + 2] << 16 & 0xff0000 | arr[off + 1] << 8 & 0xff00 | arr[off] & 0xFF;
//        } // end of LEW
//    }
}
