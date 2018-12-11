package com.kaszubski.kamil.emmhelper;


import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.kaszubski.kamil.emmhelper.utils.Constants;
import com.kaszubski.kamil.emmhelper.utils.Utils;

import java.util.List;

public class SecretCodesFragment extends Fragment {
    private static final String TAG = SecretCodesFragment.class.getSimpleName();
    private Context context;
    private SharedPreferences preferences;
    private RecyclerView recyclerView;
    private boolean newerDevice;


    public SecretCodesFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        context = getActivity();
        newerDevice = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
        ((AppCompatActivity)context).invalidateOptionsMenu();
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((MainActivity)context).closeDrawer();
        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);

        if (newerDevice && !isAccessibilityServiceEnabled()) {
            getEnableAccessibilityServiceDialog().show();
        }

        preferences = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE);

        if(preferences.getBoolean(Constants.SHOW_SECRET_CODES_WARNING_DIALOG, true))
            displayWarningDialog();
        setUpRecyclerView();
    }

    private boolean isAccessibilityServiceEnabled() {
        int accessibilityEnabled = 0;
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    context.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
            Log.d(TAG, "accessibilityEnabled = " + accessibilityEnabled);
        } catch (Settings.SettingNotFoundException e) {
            Log.d(TAG, "Error finding setting, default accessibility to not found: "
                    + e.getMessage());
        }

        if (accessibilityEnabled == 1) {
            String enabledServices = Settings.Secure.getString(
                    context.getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);

            if (enabledServices != null) {
                String[] services = enabledServices.split(":");
                final String myService = context.getPackageName() + "/" + SecretCodesAccessibilityService.class.getCanonicalName();
                for (String enabledService : services){
                    if(myService.equalsIgnoreCase(enabledService)) {
                        Log.d(TAG, "Accessibility service enabled");
                        return true;
                    }
                }
            }
        }
        Log.d(TAG, "Accessibility service disabled");
        return false;
    }

    private Dialog getEnableAccessibilityServiceDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        return builder.setTitle(R.string.enable_accessibility_service)
                .setMessage(R.string.enable_accessibility_service_confirmation)
                .setNegativeButton(R.string.later, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Utils.displayToast(context, getString(R.string.you_can_enable_it_from_menu));
                    }
                })
                .setPositiveButton(R.string.enable, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                    }
                }).create();

    }

    private void displayWarningDialog(){
        Utils.getMessageWithCheckBoxDialog(context, getString(R.string.warning),
                getString(R.string.secret_codes_warning_message), getString(R.string.don_t_show_again),
                new Utils.OnConfirmCheckBoxDialog() {
                    @Override
                    public void onConfirmCheckBoxDialog(boolean isChecked) {
                        if(isChecked)
                            preferences.edit().putBoolean(Constants.SHOW_SECRET_CODES_WARNING_DIALOG, false).apply();
                    }
                }, true).show();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (newerDevice) {
            inflater.inflate(R.menu.fragment_secret_codes, menu);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_enable_auto_run:
                if(!isAccessibilityServiceEnabled())
                    startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                else
                    Utils.displayToast(context, getString(R.string.accessibility_service_already_enabled));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void setUpRecyclerView(){
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(new RecyclerAdapter());
    }

    private class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.DoubleLineViewHolder> {
        private String[] codes;
        private String[] names;

        RecyclerAdapter() {
            codes = getResources().getStringArray(R.array.secretCodes);
            names = getResources().getStringArray(R.array.secretCodesNames);
        }


        @Override
        public RecyclerAdapter.DoubleLineViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new RecyclerAdapter.DoubleLineViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.double_line_recycler_view_item, parent, false));
        }

        @Override
        public void onBindViewHolder(RecyclerAdapter.DoubleLineViewHolder holder, int position) {
            holder.titleTextView.setText(names[position]);
            holder.subtitleTextView.setText(codes[position]);
        }

        @Override
        public int getItemCount() {
            return codes.length;
        }

        class DoubleLineViewHolder extends RecyclerView.ViewHolder {
            TextView titleTextView, subtitleTextView;
            View itemView;

            DoubleLineViewHolder(final View itemView) {
                super(itemView);
                this.itemView = itemView;
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText(Constants.FRAGMENT_SECRET_CODES + context.getPackageName(), codes[getLayoutPosition()]);
                        clipboard.setPrimaryClip(clip);

                        Intent intent = null;
                        if(newerDevice && isAccessibilityServiceEnabled()) {

                            intent = context.getPackageManager().getLaunchIntentForPackage("com.sec.android.app.popupcalculator");
                            if (intent == null) {
                                Log.d(TAG, "Empty calculator intent");
                                return;
                            }
                        } else {
                            Intent dialerIntent = new Intent(Intent.ACTION_DIAL);
                            dialerIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            List<ResolveInfo> activities = context.getPackageManager().queryIntentActivities(dialerIntent, 0);
                            for (ResolveInfo r : activities) {
                                Log.d(TAG, r.toString());
                                if ((r.activityInfo.packageName.equals(Constants.OLD_CONTACTS_PACKAGE) || r.activityInfo.packageName.equals(Constants.NEW_CONTACTS_PACKAGE))
                                        && !r.activityInfo.name.contains("Non")) {
                                    intent = new Intent(Intent.ACTION_DIAL, null);
                                    intent.setComponent(new ComponentName(r.activityInfo.packageName, r.activityInfo.name));
                                    Utils.displayToast(context, getString(R.string.secret_code_copied_to_clipboard));
                                    break;
                                }

                            }
                            if (intent == null){
                                Log.d(TAG, "Empty dialer intent");
                                return;
                            }

                        }

                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                });


                titleTextView = (TextView) itemView.findViewById(R.id.textView2);
                subtitleTextView = (TextView) itemView.findViewById(R.id.textView3);
            }
        }

        @Override
        public long getItemId(int position) {
            return position;
        }
    }
}
