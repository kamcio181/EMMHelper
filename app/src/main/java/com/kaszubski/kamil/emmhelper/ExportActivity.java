package com.kaszubski.kamil.emmhelper;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.kaszubski.kamil.emmhelper.utils.Constants;
import com.kaszubski.kamil.emmhelper.utils.Utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;

public class ExportActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "ExportActivity";
    private Context context;
    private boolean exit = false;
    private Handler handler = new Handler();
    private Runnable exitRunnable;
    private ArrayList<String> arrayToSave = new ArrayList<>();
    private FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_main);

        setResetExitFlagRunnable();

        Intent intent = getIntent();

        String fileExtension = intent.getStringExtra(Constants.FILE_FORMAT_KEY);
        fragmentManager = getSupportFragmentManager();

        if(intent.hasExtra(Constants.STRING_KEY)){ //save string i.e. manifest
            arrayToSave.addAll(Arrays.asList(intent.getStringExtra(Constants.STRING_KEY).split("\n")));
        } else if (intent.hasExtra(Constants.ARRAY_KEY)){ //save string array list i.e. packageNames
            arrayToSave = intent.getStringArrayListExtra(Constants.ARRAY_KEY);
        } else
            finish();

        fragmentManager.beginTransaction().
                replace(R.id.container, ExportFragment.newInstance(fileExtension, arrayToSave), Constants.FRAGMENT_EXPORT).
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit();

        setupToolbarAndFab();

    }

    private void setupToolbarAndFab(){
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar()!=null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        try {
            Field titleField = Toolbar.class.getDeclaredField("mTitleTextView");
            titleField.setAccessible(true);
            TextView barTitleView = (TextView) titleField.get(toolbar);
            barTitleView.setEllipsize(TextUtils.TruncateAt.START);
            barTitleView.setFocusable(true);
            barTitleView.setFocusableInTouchMode(true);
            barTitleView.requestFocus();
            barTitleView.setSingleLine(true);
            barTitleView.setSelected(true);

        } catch (NoSuchFieldException e){
            Log.e(TAG, "" + e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, " " + e);
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if(fab!=null)
            fab.setOnClickListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if(!exit){
                exit = true;
                handler.postDelayed(exitRunnable, 5000);
                Utils.showToast(this, "Press back button again to exit");
            } else {
                finish();
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void setResetExitFlagRunnable(){
        exitRunnable = new Runnable() {
            @Override
            public void run() {
                exit = false;
            }
        };
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.WRITE_PERMISSION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Fragment fragment;
                    if ((fragment = fragmentManager.findFragmentByTag(Constants.FRAGMENT_EXPORT)) != null) {
                        ((ExportFragment) fragment).permissionGranted();
                    }
                } else {
                    Utils.showToast(context, "Write permission is required to perform this action");
                    finish();
                }
                break;
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.fab:
                Fragment fragment;
                if ((fragment = fragmentManager.findFragmentByTag(Constants.FRAGMENT_EXPORT)) != null) {
                    ((ExportFragment) fragment).writeToFile();
                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if(getTitle().equals("/"))
            super.onBackPressed();
        else{
            ((ExportFragment)fragmentManager.findFragmentByTag(Constants.FRAGMENT_EXPORT)).moveUp();
        }
    }
}