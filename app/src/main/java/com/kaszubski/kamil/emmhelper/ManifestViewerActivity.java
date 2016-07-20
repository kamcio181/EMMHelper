package com.kaszubski.kamil.emmhelper;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.kaszubski.kamil.emmhelper.utils.Constants;
import com.kaszubski.kamil.emmhelper.utils.Utils;
import com.kaszubski.kamil.emmhelper.utils.VerticalSpaceItemDecoration;

import java.util.ArrayList;
import java.util.List;

public class ManifestViewerActivity extends AppCompatActivity {
    private static final String TAG = "ManifestViewerActivity";
    private PackageInfo packageInfo;
    private TextView appNameTV, packageNameTV, versionCodeTV, versionNameTV, launcherCapabilitiesTV;
    private ImageView iconIV;
    private RecyclerView recyclerView;
    private String sourceFile;
    private boolean isPackageInstalled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manifest_viewer);
        launcherCapabilitiesTV = (TextView) findViewById(R.id.textView8);
        appNameTV = (TextView) findViewById(R.id.textView2);
        iconIV = (ImageView) findViewById(R.id.imageView2);

        String packageName;
        if(getIntent()!= null && getIntent().hasExtra(Constants.PACKAGE_INFO_KEY)){
            packageName = getIntent().getStringExtra(Constants.PACKAGE_INFO_KEY);
            try {
                isPackageInstalled = true;
                packageInfo = getPackageManager().getPackageInfo(packageName, PackageManager.GET_ACTIVITIES
                        | PackageManager.GET_RECEIVERS |PackageManager.GET_PERMISSIONS
                        | PackageManager.GET_SERVICES | PackageManager.GET_PROVIDERS);
                sourceFile = packageInfo.applicationInfo.sourceDir;
                appNameTV.setText(packageInfo.applicationInfo.loadLabel(getPackageManager()));
                iconIV.setImageDrawable(packageInfo.applicationInfo.loadIcon(getPackageManager()));

            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

        }
        else if (getIntent()!= null && getIntent().hasExtra(Constants.APK_PATH)) {
            sourceFile = getIntent().getStringExtra(Constants.APK_PATH);
            isPackageInstalled = false;
            packageInfo = getPackageManager().getPackageArchiveInfo(sourceFile,
                    PackageManager.GET_ACTIVITIES | PackageManager.GET_RECEIVERS | PackageManager.GET_PERMISSIONS
                            | PackageManager.GET_SERVICES | PackageManager.GET_PROVIDERS);
            launcherCapabilitiesTV.setText(Html.fromHtml("<b>" + getString(R.string.launcher_kiosk_ready)
                    + ": </b>" + getString(R.string.loading)));
            appNameTV.setText(packageInfo.packageName); //not supported using default value
            iconIV.setImageResource(R.mipmap.ic_launcher); //not supported using default value
        }
        else
            finish();


        packageNameTV = (TextView) findViewById(R.id.textView3);
        versionCodeTV = (TextView) findViewById(R.id.textView6);
        versionNameTV = (TextView) findViewById(R.id.textView7);


        packageNameTV.setText(packageInfo.packageName);
        versionCodeTV.setText(Html.fromHtml("<b>" + getString(R.string.version_code) + ": </b>"
                + packageInfo.versionCode));
        versionNameTV.setText(Html.fromHtml("<b>" + getString(R.string.version_name) + ": </b>"
                + packageInfo.versionName));


        isLauncherReady();


        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new VerticalSpaceItemDecoration(this, 10));
        recyclerView.setAdapter(new ExpansionItemsAdapter(packageInfo));
    }

    private void isLauncherReady(){
        if(isPackageInstalled) {
            boolean isLauncherReady = false;
            ActivityInfo[] info = packageInfo.activities;

            PackageManager packageManager = getPackageManager();
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            List<ResolveInfo> homeList = packageManager.queryIntentActivities(intent, 0);


            if (homeList != null && homeList.size() > 0 && info != null && info.length > 0) {
                for (ResolveInfo i : homeList) {
                    for (ActivityInfo j : info) {
                        if (i.activityInfo.name.equals(j.name)) {
                            isLauncherReady =  true;
                        }
                    }
                }
            }
            setLauncherCapabilitiesTV(isLauncherReady);
        } else {
            Utils.decodeXmlFile(this, sourceFile, true, new Utils.OnDecodeFinishListener() {
                @Override
                public void onDecodeFinished(String xml, boolean launcherReady) {
                    setLauncherCapabilitiesTV(launcherReady);
                }
            });
        }
    }

    private void setLauncherCapabilitiesTV(boolean isReady){
        launcherCapabilitiesTV.setText(Html.fromHtml("<b>" + getString(R.string.launcher_kiosk_ready)
                + ": </b>" + (isReady ? getString(R.string.yes) : getString(R.string.no))));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_manifest_viewer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id){
            case R.id.action_view_as_xml:
                Intent intent = new Intent(this, XmlViewerActivity.class);
                intent.putExtra(Constants.SOURCE_DIR, sourceFile);
                startActivity(intent);
                break;
        }

        return super.onOptionsItemSelected(item);
    }


}
class ExpansionItemsAdapter extends RecyclerView.Adapter<ExpansionItemsAdapter.ViewHolder> {
    private static final String TAG = "ExpansionItemsAdapter";
    private static String packageName;
    private static String packageNameToReplace;
    private static ActivityInfo[] activities;
    private static ActivityInfo[] receivers;
    private static PermissionInfo[] permissions;
    private static ServiceInfo[] services;
    private static ProviderInfo[] providers;
    private static StringBuilder builder;
    private static final int ACTIVITIES = 0;
    private static final int RECEIVERS = 1;
    private static final int PERMISSIONS = 2;
    private static final int SERVICES = 3;
    private static final int PROVIDERS = 4;
    private static ArrayList<Integer> componentsList;
    private static boolean[] expandedArray;
    private static String[] contents;

    public ExpansionItemsAdapter(PackageInfo packageInfo) {
        packageName = packageInfo.packageName;
        activities = packageInfo.activities;
        receivers = packageInfo.receivers;
        permissions = packageInfo.permissions;
        services = packageInfo.services;
        providers = packageInfo.providers;
        packageNameToReplace = packageName+".";

        componentsList = new ArrayList<>();
        if(activities != null && activities.length >0)
            componentsList.add(ACTIVITIES);
        if(receivers != null && receivers.length >0)
            componentsList.add(RECEIVERS);
        if(permissions != null && permissions.length >0)
            componentsList.add(PERMISSIONS);
        if(services != null && services.length >0)
            componentsList.add(SERVICES);
        if(providers != null && providers.length >0)
            componentsList.add(PROVIDERS);

        Log.d(TAG, componentsList.toString());

        expandedArray = new boolean[componentsList.size()];
        for(int i = 0; i < expandedArray.length; i++)
            expandedArray[i] = false;

        builder = new StringBuilder();
        contents = new String[componentsList.size()];
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.expansion_panel_recycler_view_item, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        switch (componentsList.get(position)) {
            case ACTIVITIES:
                holder.titleTV.setText(R.string.activities);
                break;
            case RECEIVERS:
                holder.titleTV.setText(R.string.receivers);
                break;
            case PERMISSIONS:
                holder.titleTV.setText(R.string.permissions);
                break;
            case SERVICES:
                holder.titleTV.setText(R.string.services);
                break;
            case PROVIDERS:
                holder.titleTV.setText(R.string.providers);
                break;
        }
        if(expandedArray[position]) {
            if(contents[position] == null) {
                builder.delete(0, builder.length());
                Log.d(TAG, "component " + componentsList.get(position));
                switch (componentsList.get(position)) {
                    case ACTIVITIES:
                        for (ActivityInfo info : activities)
                            builder.append(info.name.replace(packageNameToReplace, "")).append("\n");
                        builder.delete(builder.lastIndexOf("\n"), builder.length());
                        break;
                    case RECEIVERS:
                        for (ActivityInfo info : receivers)
                            builder.append(info.name.replace(packageNameToReplace, "")).append("\n");
                        builder.delete(builder.lastIndexOf("\n"), builder.length());
                        break;
                    case PERMISSIONS:
                        for (PermissionInfo info : permissions) {
                            String name = info.toString();
                            builder.append(name.substring(name.lastIndexOf(" ")+1, name.length() - 1)).append("\n");
                        }
                        builder.delete(builder.lastIndexOf("\n"), builder.length());
                        break;
                    case SERVICES:
                        for (ServiceInfo info : services) {
                            String name = info.toString();
                            builder.append(name.substring(name.lastIndexOf(" ")+1, name.length() - 1)).append("\n");
                        }
                        builder.delete(builder.lastIndexOf("\n"), builder.length());
                        break;
                    case PROVIDERS:
                        for (ProviderInfo info : providers) {
                            String name = info.toString();
                            builder.append(name.substring(name.lastIndexOf("=")+1, name.length() - 1)).append("\n");
                        }
                        builder.delete(builder.lastIndexOf("\n"), builder.length());
                        break;
                }
                contents[position] = builder.toString();
            }
            holder.contentTV.setText(contents[position]);
            holder.contentTV.setVisibility(View.VISIBLE);
            holder.iconIV.setRotation(180);
        } else {
            holder.contentTV.setVisibility(View.GONE);
            holder.iconIV.setRotation(0);
        }
    }

    @Override
    public int getItemCount() {
        return componentsList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        public TextView titleTV;
        public ImageView iconIV;
        public TextView contentTV;

        public ViewHolder(final View itemView) {
            super(itemView);
            titleTV = (TextView) itemView.findViewById(R.id.textView4);
            contentTV = (TextView) itemView.findViewById(R.id.textView5);
            iconIV = (ImageView) itemView.findViewById(R.id.imageView3);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    expandedArray[getLayoutPosition()] = !expandedArray[getLayoutPosition()];
                    ExpansionItemsAdapter.this.notifyItemChanged(getLayoutPosition());

                }
            });
        }
    }
}