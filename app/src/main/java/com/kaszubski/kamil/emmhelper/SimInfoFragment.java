package com.kaszubski.kamil.emmhelper;


import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.kaszubski.kamil.emmhelper.utils.Constants;
import com.kaszubski.kamil.emmhelper.utils.ExportableContent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import static android.content.Context.TELEPHONY_SERVICE;

public class SimInfoFragment extends Fragment implements View.OnClickListener, ExportableContent{ //TODO sim state listener
    private static final String TAG = SimInfoFragment.class.getSimpleName();
    private static final String ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED";
    private LinearLayout linearLayout;
    private Context context;
    private StringBuilder info;
    private TelephonyManager telephonyManager;
    private SubscriptionManager subManager;
    private BroadcastReceiver receiver;

    public SimInfoFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        context = getActivity();
        ((AppCompatActivity)context).invalidateOptionsMenu();
        return inflater.inflate(R.layout.fragment_siminfo, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((MainActivity)context).closeDrawer();
        linearLayout = (LinearLayout) view.findViewById(R.id.root);

        if(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED){
            getSimInformation();
        }
        else {
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.READ_PHONE_STATE},
                    Constants.READ_PHONE_STATE_PERMISSION);
        }
    }

    public void permissionGranted(){
        getSimInformation();
    }

    private void getSimInformation(){
        info = new StringBuilder();
        linearLayout.removeAllViews();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            if(subManager == null) {
                subManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                subManager.addOnSubscriptionsChangedListener(new SubscriptionManager.OnSubscriptionsChangedListener(){
                    @Override
                    public void onSubscriptionsChanged() {
                        super.onSubscriptionsChanged();

                        getSimInformation();
                    }
                });
            }
//            List<SubscriptionInfo> subInfoList = subManager.getActiveSubscriptionInfoList();
            int simSlotCount = subManager.getActiveSubscriptionInfoCountMax();
            Log.d(TAG, String.valueOf(simSlotCount));
            if (simSlotCount > 0) {
                getInformationLollipop(simSlotCount);
            } else {
                getInformationPreLollipop(0);
            }
        } else {
            if(receiver == null){
                receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if(intent.getAction().equals(ACTION_SIM_STATE_CHANGED))
                            getSimInformation();
                    }
                };
                context.registerReceiver(receiver, new IntentFilter(ACTION_SIM_STATE_CHANGED));
            }
            getInformationPreLollipop(0);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        try {
            context.unregisterReceiver(receiver);
        } catch (Exception e){
            Log.w(TAG, "Unregister Receiver error " + e);
        }
    }

    private void getInformationLollipop(int simSlotCount){
        for (int i = 0; i < simSlotCount; i++) {
            SubscriptionInfo subscriptionInfo = subManager.getActiveSubscriptionInfoForSimSlotIndex(i);
            if(subscriptionInfo == null){
                getInformationPreLollipop(i);
            } else {

                Log.d(TAG, subscriptionInfo.toString());

                LayoutInflater inflater = ((MainActivity) context).getLayoutInflater();
                View view1 = inflater.inflate(R.layout.header_content_item, null);
                int mnc = subscriptionInfo.getMnc();
                String mncString = mnc < 10 ? "0" + mnc : String.valueOf(mnc);

                StringBuilder builder = new StringBuilder();
                builder.append("<b>").append("Sim state: ").append("</b>").append(getResources().getStringArray(R.array.sim_states)[getSimState(subscriptionInfo.getSimSlotIndex())]).append("<br/>")
                        .append("<b>").append("Country: ").append("</b>").append(checkNull(subscriptionInfo.getCountryIso())).append("<br/>")
                        .append("<b>").append("MCC: ").append("</b>").append(checkNull(String.valueOf(subscriptionInfo.getMcc()))).append("<br/>")
                        .append("<b>").append("MNC: ").append("</b>").append(checkNull(mncString)).append("<br/>")
                        .append("<b>").append("Operator name: ").append("</b>").append(checkNull(String.valueOf(subscriptionInfo.getCarrierName()))).append("<br/>")
                        .append("<b>").append("Serial Number: ").append("</b>").append(checkNull(subscriptionInfo.getIccId())).append("<br/>")
                        .append("<b>").append("Device Id: ").append("</b>").append(getImei(subscriptionInfo.getSimSlotIndex()));

                String currentInfo = builder.toString();

                TextView header = (TextView) view1.findViewById(R.id.header);
                header.setText("SIM slot " + subscriptionInfo.getSimSlotIndex());
                TextView textView = (TextView) view1.findViewById(R.id.content);
                textView.setText(Html.fromHtml(currentInfo));

                info.append("SIM slot ").append(subscriptionInfo.getSimSlotIndex()).append("<br/>").append(currentInfo.replaceAll("<b>", "").replaceAll("</b>", "")).append("<br/>");

                linearLayout.addView(view1);
            }
        }
    }

    private void getInformationPreLollipop(int simSlot){
        LayoutInflater inflater = ((MainActivity) context).getLayoutInflater();

        View view1 = inflater.inflate(R.layout.header_content_item, null);
        if(telephonyManager == null) {
            telephonyManager = (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);
        }
        StringBuilder builder = new StringBuilder();

        int state = getSimState(simSlot);
        builder.append("<b>").append("Sim state: ").append("</b>").append(getResources().getStringArray(R.array.sim_states)[state]).append("<br/>");
        if(state>1) {
            String plmn = telephonyManager.getSimOperator();
            String mcc = null, mnc = null;
            if(plmn != null && !plmn.equals("") && plmn.length() >= 5) {
                mcc = plmn.substring(0, 3);
                mnc = plmn.substring(3, plmn.length());
            }
            builder.append("<b>").append("Country: ").append("</b>").append(checkNull(telephonyManager.getSimCountryIso())).append("<br/>")
                    .append("<b>").append("MCC: ").append("</b>").append(checkNull(mcc)).append("<br/>")
                    .append("<b>").append("MNC: ").append("</b>").append(checkNull(mnc)).append("<br/>")
                    .append("<b>").append("Operator name: ").append("</b>").append(checkNull(telephonyManager.getSimOperatorName())).append("<br/>")
                    .append("<b>").append("Serial Number: ").append("</b>").append(checkNull(telephonyManager.getSimSerialNumber())).append("<br/>");
        }
        builder.append("<b>").append("Device Id: ").append("</b>").append(getImei(simSlot));

        String currentInfo = builder.toString();

        TextView header = (TextView) view1.findViewById(R.id.header);
        header.setText("SIM slot " + simSlot);
        TextView textView = (TextView) view1.findViewById(R.id.content);
        textView.setText(Html.fromHtml(currentInfo));

        info.append("SIM slot ").append(simSlot).append("<br/>").append(currentInfo.replaceAll("<b>", "").replaceAll("</b>", "")).append("<br/>");

        linearLayout.addView(view1);
    }

    private int getSimState(int simId){
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);

        try {
            Method getSimStateMethod = TelephonyManager.class.getMethod("getSimState", int.class);
            return (int) getSimStateMethod.invoke(telephonyManager, simId);
        } catch (NoSuchMethodException e) {
            return 0;
        } catch (InvocationTargetException e) {
            return 0;
        } catch (IllegalAccessException e) {
            return 0;
        }
    }

    private String getImei(int simId){
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);

        try {
            Method getDeviceIdMethod = TelephonyManager.class.getMethod("getDeviceId", int.class);
            return (String) getDeviceIdMethod.invoke(telephonyManager, simId);
        } catch (NoSuchMethodException e) {
            return telephonyManager.getDeviceId();
        } catch (InvocationTargetException e) {
            return telephonyManager.getDeviceId();
        } catch (IllegalAccessException e) {
            return telephonyManager.getDeviceId();
        }
    }

    private String checkNull(String string){
        return string == null? "" : string;
    }

    @Override
    public void onClick(View v) {

    }

    public ArrayList<String> getExportList(){
        if(info == null || info.length() <= 0)
            return null;

        ArrayList<String> list = new ArrayList<>();
        list.addAll(Arrays.asList(info.toString().split("<br/>")));


//        if(textView.getText().toString().trim().length() <= 0)
//            return null;
//
//        list.addAll(Arrays.asList(textView.getText().toString().split("\n")));
        return list;
    }
}
