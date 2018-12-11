package com.kaszubski.kamil.emmhelper;

import android.accessibilityservice.AccessibilityService;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.kaszubski.kamil.emmhelper.utils.Constants;

/**
 * Created by k.kaszubski on 2/10/17.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class SecretCodesAccessibilityService extends AccessibilityService {
    private static final String TAG = SecretCodesAccessibilityService.class.getSimpleName();
    public static final int DELAY_MILLIS = 100;
    private ClipboardManager clipboardManager;
    private AccessibilityNodeInfo editText;
    private AccessibilityNodeInfo plusButton;
    private CharSequence code;
    private Handler handler;
    private boolean interrupt = false;
    private boolean editTextFound = false;
    private boolean plusButtonFound = false;


    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        Log.d(TAG, "onAccessibilityEvent " + accessibilityEvent.getPackageName());

        if (!hasClipboardSecretCode()) {
            return;
        }

        handler = new Handler();
        interrupt = false;
        editTextFound = false;
        plusButtonFound = false;

        String s = accessibilityEvent.getPackageName().toString();
        if (s.equals(Constants.CALCULATOR_PACKAGE)) {
            processCalculatorEvent(accessibilityEvent);
        } else if (s.equals(Constants.PARSER_PACKAGE)) {
            processParserEvent(accessibilityEvent);
        }
    }

    @Override
    public void onInterrupt() {
        //nothing
    }

    private void processCalculatorEvent(AccessibilityEvent accessibilityEvent) {
        AccessibilityNodeInfo source = accessibilityEvent.getSource();
        if (source == null) {
            Log.d(TAG, "Source null ");
            return;
        }

        AccessibilityNodeInfo parent = getRootInActiveWindow();
        if (parent == null) {
            Log.d(TAG, "Parent null ");
            if(source.getChildCount() > 0){
                findViewElementsInCalculatorView(source);
            }
        } else if (parent.getChildCount() > 0){
            findViewElementsInCalculatorView(parent);
        }
    }


    private void findViewElementsInCalculatorView(AccessibilityNodeInfo parent) {

        for(int i = 0; i < parent.getChildCount(); i++){
            if(interrupt){
                return;
            }

            final AccessibilityNodeInfo child = parent.getChild(i);
            if(child == null){
                continue;
            }

            String childClassName = child.getClassName().toString();
            Log.d(TAG, "child " + childClassName + " text " + (child.getText()));
            if(childClassName.contains("EditText") && !editTextFound){
                processEditTextFound(child);
            } else if(childClassName.contains("Button") && !plusButtonFound &&
                    child.getText() != null && child.getText().toString().equals("+")) {
                processPlusButtonFound(child);
            }else if(child.getChildCount() > 0){
                findViewElementsInCalculatorView(child);
            }
        }
    }

    private void processPlusButtonFound(AccessibilityNodeInfo child) {
        Log.d(TAG, "processPlusButtonFound");
        plusButton = child;
        if(editTextFound){
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    interrupt = true;
                    putCalculatorCode();
                }
            }, DELAY_MILLIS);
        } else {
            plusButtonFound = true;
        }
    }

    private void processEditTextFound(AccessibilityNodeInfo child) {
        Log.d(TAG, "processEditTextFound");
        editText = child;
        if(plusButtonFound){
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    interrupt = true;
                    putCalculatorCode();
                }
            }, DELAY_MILLIS);
        } else {
            editTextFound = true;
        }
    }

    private void processParserEvent(AccessibilityEvent accessibilityEvent) {
        Log.d(TAG, "processParserEvent");

        AccessibilityNodeInfo source = accessibilityEvent.getSource();
        if (source == null) {
            Log.d(TAG, "Source null ");
            return;
        }

        AccessibilityNodeInfo parent = getRootInActiveWindow();
        if (parent == null) {
            Log.d(TAG, "Parent null ");
            if(source.getChildCount() > 0){
                findEditTextInParserViews(source);
            }
        } else if (parent.getChildCount() > 0){
            findEditTextInParserViews(parent);
        }
    }

    private void findEditTextInParserViews(AccessibilityNodeInfo parent) {
        Log.d(TAG, "findEditTextInParserViews");
        interrupt = false;
        for(int i = 0; i < parent.getChildCount(); i++){
            if(interrupt){
                return;
            }

            final AccessibilityNodeInfo child = parent.getChild(i);
            if(child == null){
                continue;
            }

            String childClassName = child.getClassName().toString();
            Log.d(TAG, "child " + childClassName);
            if(childClassName.contains("EditText")){
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        interrupt = true;
                        code = getCodeFromClipboard();
                        clearClipboard();
                        putCodeFromClipboard(child);
                    }
                }, DELAY_MILLIS);
            } else if(child.getChildCount() > 0){
                    findEditTextInParserViews(child);
            }
        }
    }

    private boolean hasClipboardSecretCode(){
        clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if(clipboardManager == null){
            return false;
        }

        ClipDescription clipDescription = clipboardManager.getPrimaryClipDescription();
        if(!clipboardManager.hasPrimaryClip() || clipDescription == null){
            return false;
        }

        boolean hasClipboardSecretCode = (clipDescription.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) &&
                String.valueOf(clipDescription.getLabel()) != null &&
                String.valueOf(clipDescription.getLabel()).equals(Constants.FRAGMENT_SECRET_CODES + getPackageName()));
        Log.d(TAG, "hasClipboardSecretCode " + hasClipboardSecretCode);

        return hasClipboardSecretCode;
    }

    private CharSequence getCodeFromClipboard(){
        if(clipboardManager == null || clipboardManager.getPrimaryClip() == null){
            return "";
        }
        return clipboardManager.getPrimaryClip().getItemAt(0).getText();
    }


    private void clearClipboard(){
        clipboardManager.setPrimaryClip(ClipData.newPlainText("", ""));
    }

    private void putCodeFromClipboard(AccessibilityNodeInfo editText){
        Log.d(TAG, "Put code \"" + code + "\"");
        if(code.length()<=0)
            return;
        Bundle arguments = new Bundle();
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, code);
        editText.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
        editText.recycle();
    }

    private void putCalculatorCode(){
        Log.d(TAG, "putCalculatorCode");
        Bundle arguments = new Bundle();
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "(+30012012732");
        editText.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
        editText.recycle();
        plusButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        plusButton.recycle();
    }
}
