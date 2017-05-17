package com.kaszubski.kamil.emmhelper;


import android.app.enterprise.license.EnterpriseLicenseManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.kaszubski.kamil.emmhelper.utils.Constants;
import com.kaszubski.kamil.emmhelper.utils.Utils;

public class LicenseCheckFragment extends Fragment implements View.OnClickListener{
    private static final String TAG = LicenseCheckFragment.class.getSimpleName();
    private TextInputEditText editText;
    private Button button, button2;
    private Context context;
    private BroadcastReceiver receiver;
    private IntentFilter filter;

    public LicenseCheckFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        context = getActivity();
        ((AppCompatActivity)context).invalidateOptionsMenu();
        return inflater.inflate(R.layout.fragment_license_check, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        editText = (TextInputEditText) view.findViewById(R.id.editText);
        button = (Button) view.findViewById(R.id.button);
        button2 = (Button) view.findViewById(R.id.button2);
        button.setOnClickListener(this);
        button2.setOnClickListener(this);

        ((MainActivity)context).closeDrawer();

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.button:
                String licenseKey = editText.getText().toString().trim();
                if(licenseKey.length() > 0){
                    activateLicense(licenseKey);
                }
                break;
            case R.id.button2:
                editText.setText("");
                break;
        }
    }

    private void activateLicense(String licenseKey){
        Utils.displayToast(context, getString(R.string.checking_license));
        EnterpriseLicenseManager elm = EnterpriseLicenseManager.getInstance(context);
        elm.activateLicense(licenseKey, context.getPackageName());
    }

    @Override
    public void onStart() {
        super.onStart();
        if(receiver == null){
            filter = new IntentFilter(Constants.SUCCESS_ACTION);
            filter.addAction(Constants.FAILURE_ACTION);
            receiver = new RefreshReceiver();
        }
        getActivity().registerReceiver(receiver, filter);
    }

    @Override
    public void onStop() {
        super.onStop();
        try {
            getActivity().unregisterReceiver(receiver);
        } catch (IllegalArgumentException e){
            Log.e(TAG, getString(R.string.receiver_was_unregistered));
        }
    }

    class RefreshReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(TAG, "RECEIVER " + intent.getAction());
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            switch (intent.getAction()){
                case Constants.SUCCESS_ACTION:
                    builder.setMessage("License activated successfully");
                    builder.create().show();
                    break;
                case Constants.FAILURE_ACTION:
                    int errorCode = intent.getIntExtra(Constants.ERROR_CODE, Constants.DEFAULT_ERROR);
                    Log.e(TAG, "" + errorCode);
                    builder.setMessage(Utils.getMessage(context, errorCode));
                    builder.create().show();
                    break;
            }
        }
    }
}
