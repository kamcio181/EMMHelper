package com.kaszubski.kamil.emmhelper;

import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ServiceInfo;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.kaszubski.kamil.emmhelper.utils.Constants;
import com.kaszubski.kamil.emmhelper.utils.DividerItemDecoration;

public class ManifestViewerActivity extends AppCompatActivity {
    private static final String TAG = "ManifestViewerActivity";
    private PackageInfo packageInfo;
    private TextView appNameTV, packageNameTV;
    private ImageView iconIV;
    private RecyclerView recyclerView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String packageName = "";
        if(getIntent()!= null && getIntent().hasExtra(Constants.PACKAGE_INFO_KEY)){
            packageName = getIntent().getStringExtra(Constants.PACKAGE_INFO_KEY);
            Log.e(TAG, "has info");
            Log.e(TAG, "package info " +( packageInfo.packageName != null));
            Log.e(TAG, "packageManager "+(getPackageManager() != null));
        }
        else
            finish();

        try {
            packageInfo = getPackageManager().getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        appNameTV = (TextView) findViewById(R.id.textView2);
        packageNameTV = (TextView) findViewById(R.id.textView3);
        iconIV = (ImageView) findViewById(R.id.imageView2);

        appNameTV.setText(packageInfo.applicationInfo.loadLabel(getPackageManager()));
        packageNameTV.setText(packageInfo.packageName);
        iconIV.setImageDrawable(packageInfo.applicationInfo.loadIcon(getPackageManager()));

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));
        recyclerView.setAdapter(new ExpansionItemsAdapter(packageInfo));
    }
}
class ExpansionItemsAdapter extends RecyclerView.Adapter<ExpansionItemsAdapter.ViewHolder> {
    private static ActivityInfo[] activities;
    private static ActivityInfo[] receivers;
    private static PermissionInfo[] permissions;
    private static ServiceInfo[] services;
    private static ProviderInfo[] providers;//TODO
    private static int viewCount = 0;


    public interface OnItemClickListener {
        void onItemClick(boolean isDir, String item);
    }

    public ExpansionItemsAdapter(PackageInfo packageInfo) {
        activities = packageInfo.activities;
        receivers = packageInfo.receivers;
        permissions = packageInfo.permissions;
        services = packageInfo.services;
        providers = packageInfo.providers;//TODO

//        if(activities.length > 0)
//            viewCount++;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.expansion_panel_recycler_view_item, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        switch (position) {
            case 0:
                if (activities.length > 0) {
                    holder.titleTV.setText("Activities");

                }
                else
                    holder.itemView.setVisibility(View.GONE);
                break;
        }

    }

    @Override
    public int getItemCount() {
        return viewCount;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView titleTV;
        public ImageView iconIV;
        public TextView contentTV;
        public boolean expanded = false;

        public ViewHolder(final View itemView) {
            super(itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    expanded = !expanded;
                    StringBuilder builder = new StringBuilder();
                    if(expanded) {
                        for (ActivityInfo info : activities)
                            builder.append(info.name).append("\n");
                        builder.delete(builder.lastIndexOf("\n"), builder.length());
                        contentTV.setText(builder.toString());
                        contentTV.setVisibility(View.VISIBLE);
                        //TODO rotate arrow
                    } else {
                        contentTV.setVisibility(View.GONE);
                    }
//                    if(listener != null)
//                        listener.onItemClick(getLayoutPosition()<dirsCount, items.get(getLayoutPosition()));
                    //TODO expand/collapse
                }
            });

            titleTV = (TextView) itemView.findViewById(R.id.textView4);
            contentTV = (TextView) itemView.findViewById(R.id.textView5);
            iconIV = (ImageView) itemView.findViewById(R.id.imageView3);
        }
    }
}