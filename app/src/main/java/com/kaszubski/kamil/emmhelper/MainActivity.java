package com.kaszubski.kamil.emmhelper;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowInsets;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.kaszubski.kamil.emmhelper.utils.Constants;
import com.kaszubski.kamil.emmhelper.utils.ExportableContent;
import com.kaszubski.kamil.emmhelper.utils.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        SearchView.OnQueryTextListener{
    private static final String TAG = MainActivity.class.getSimpleName();
    private List<PackageInfo> list;
    private List<PackageInfo> searchResults;
    private PackageManager packageManager;
    private FragmentManager fragmentManager;
    private SearchView searchView;
    private ProgressDialog progressDialog;
    private ProgressBar progressBar;
    private SearchPackages searchPackages;
    private Toolbar toolbar;
    private String previousQuery;
    private DrawerLayout drawer;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        fragmentManager = getSupportFragmentManager();
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if(drawer != null)
            setDrawer();

        setNavigationView();

        fragmentManager.beginTransaction().
                replace(R.id.container, new SearchFragment(), Constants.FRAGMENT_SEARCH).
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit();
    }

    private void setDrawer(){
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void setNavigationView(){
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
            navigationView.setCheckedItem(R.id.nav_app_list);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                navigationView.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                    @Override
                    public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                        return insets;
                    }
                });
            }
        }
    }

    public void setProgressBarState(boolean enabled){
        progressBar.setVisibility(enabled? View.VISIBLE : View.GONE);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if(fragmentManager.findFragmentByTag(Constants.FRAGMENT_EXPORT) != null){
            if(getTitle().equals("/"))
                super.onBackPressed();
            else{
                ((ExportFragment)fragmentManager.findFragmentByTag(Constants.FRAGMENT_EXPORT)).moveUp();
            }
        } else
            super.onBackPressed();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if(fragmentManager.findFragmentById(R.id.container) != null) {
            SearchManager searchManager;
            switch (fragmentManager.findFragmentById(R.id.container).getTag()) {
                case Constants.FRAGMENT_SEARCH:
                    longTextTitleMode(false);
                    setTitle(getString(R.string.application_list));
                    getMenuInflater().inflate(R.menu.fragment_search, menu);
                    searchManager =
                            (SearchManager) getSystemService(Context.SEARCH_SERVICE);
                    searchView =
                            (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
                    searchView.setSearchableInfo(
                            searchManager.getSearchableInfo(getComponentName()));
                    searchView.setQueryHint(getString(R.string.package_query_hint));

                    progressDialog = new ProgressDialog(this, ProgressDialog.STYLE_SPINNER);
                    progressDialog.setMessage(getString(R.string.loading_packages));
                    progressDialog.setIndeterminate(true);
                    progressDialog.setCancelable(false);
                    progressDialog.show();

                    new LoadPackages(new OnLoadListener() {
                        @Override
                        public void onLoaded(List<PackageInfo> packageInfos) {
                            Fragment fragment;
                            if((fragment = fragmentManager.findFragmentByTag(Constants.FRAGMENT_SEARCH)) != null)
                                ((SearchFragment)fragment).setSearchResults(packageInfos);
                            searchView.setOnQueryTextListener(MainActivity.this);
                            progressDialog.dismiss();
                        }
                    }).execute();

                    break;
                case Constants.FRAGMENT_GETPROP:
                    longTextTitleMode(false);
                    setTitle(getString(R.string.get_device_properties));
                    return false;
//                    getMenuInflater().inflate(R.menu.fragment_getprop, menu);
//                    searchManager =
//                            (SearchManager) getSystemService(Context.SEARCH_SERVICE);
//                    searchView =
//                            (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
//                    searchView.setSearchableInfo(
//                            searchManager.getSearchableInfo(getComponentName()));
//                    searchView.setQueryHint(getString(R.string.properties_query_hint));
//                    searchView.setOnQueryTextListener((SearchView.OnQueryTextListener) getFragmentManager().findFragmentByTag(Constants.FRAGMENT_GETPROP));
//                    break;
                case Constants.FRAGMENT_IP_FIND:
                    longTextTitleMode(false);
                    setTitle(getString(R.string.hostname_ips));
                    getMenuInflater().inflate(R.menu.fragment_ip_finder, menu);
                    break;
                case Constants.FRAGMENT_EXPORT:
                    longTextTitleMode(true);
                    break;
                case Constants.FRAGMENT_LICENSE_CHECK:
                    longTextTitleMode(false);
                    setTitle(getString(R.string.check_elm_key));
                    break;
                case Constants.FRAGMENT_SIM_INFO:
                    longTextTitleMode(false);
                    setTitle(getString(R.string.sim_information));
                    getMenuInflater().inflate(R.menu.fragment_sim_info, menu);
                    break;
            }
        }
        return true;
    }

    public void reloadPackages(){
        new LoadPackages(new OnLoadListener() {
            @Override
            public void onLoaded(List<PackageInfo> packageInfos) {
                String query = previousQuery;
                previousQuery = null;
                onQueryTextChange(query);
            }
        }).execute();
    }

    private void longTextTitleMode(boolean enabled){
        try {
            Field titleField = Toolbar.class.getDeclaredField("mTitleTextView");
            titleField.setAccessible(enabled);
            TextView barTitleView = (TextView) titleField.get(toolbar);
            barTitleView.setEllipsize(enabled? TextUtils.TruncateAt.START: TextUtils.TruncateAt.START);
            barTitleView.setFocusable(enabled);
            barTitleView.setFocusableInTouchMode(enabled);
            barTitleView.requestFocus();
            barTitleView.setSingleLine(enabled);
            barTitleView.setSelected(enabled);

        } catch (NoSuchFieldException e){
            Log.e(TAG, "" + e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, " " + e);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        Fragment fragment = fragmentManager.findFragmentById(R.id.container);
//        if(fragment instanceof GetPropFragment)
//            return super.onOptionsItemSelected(item);

        switch (id){
            case R.id.action_select_all_visible:
                ((SearchFragment)fragment).selectAllVisible();
                return true;
            case R.id.action_clear_selection:
                ((SearchFragment)fragment).clearExportList();
                return true;
            case R.id.action_share_list:
                if(fragment instanceof ExportableContent) {
                    ArrayList<String> export = ((ExportableContent)fragment).getExportList();
                    if(export != null && export.size()>0){
                        StringBuilder builder = new StringBuilder();
                        for(String s : export)
                            builder.append(s).append("\n");
                        Utils.shareViaList(this, builder.toString().trim());
                    } else {
                        Utils.displayToast(this, getString(R.string.nothing_to_share));
                    }
                }
                return true;
            case R.id.action_export_to_csv:
                if(fragment instanceof ExportableContent) {
                    ArrayList<String> export = ((ExportableContent) fragment).getExportList();

                    if (export != null && export.size() > 0) {
                        Intent intent = new Intent(this, ExportActivity.class);
                        intent.putExtra(Constants.ARRAY_KEY, export);
                        intent.putExtra(Constants.FILE_FORMAT_KEY, Constants.CSV_FILE_EXTENSION);
                        startActivity(intent);
                    } else {
                        Utils.displayToast(this, getString(R.string.nothing_to_export));
                    }
                }
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        Fragment fragment = null;
        String tag = null;
        switch (id) {
            case R.id.nav_app_list:
                fragment = new SearchFragment();
                tag = Constants.FRAGMENT_SEARCH;
                break;
            case R.id.nav_manifest_viewer:
                Utils.displayToast(this, getString(R.string.find_apk_file_to_view_its_manifest));
                fragment = ExportFragment.newInstance(Constants.APK_FILE_EXTENSION);
                tag = Constants.FRAGMENT_EXPORT;
                break;
            case R.id.nav_root_explorer:
                fragment = new ExportFragment();
                tag = Constants.FRAGMENT_EXPORT;
                break;
            case R.id.nav_ip_for_hostname:
                fragment = new IPFinderFragment();
                tag = Constants.FRAGMENT_IP_FIND;
                break;
            case R.id.nav_license_checker:
                fragment = new LicenseCheckFragment();
                tag = Constants.FRAGMENT_LICENSE_CHECK;
                break;
            case R.id.nav_show_sim_info:
                fragment = new SimInfoFragment();
                tag = Constants.FRAGMENT_SIM_INFO;
                break;
            case R.id.nav_secret_codes:
                fragment = new SecretCodesFragment();
                tag = Constants.FRAGMENT_SECRET_CODES;
                break;
            case R.id.nav_get_prop:
                fragment = new GetPropFragment();
                tag = Constants.FRAGMENT_GETPROP;
                break;
        }
        if(fragment != null)
            fragmentManager.beginTransaction().replace(R.id.container, fragment, tag).
                    setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit();
        return true;
    }

    public void closeDrawer(){
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null) {
            drawer.closeDrawer(GravityCompat.START);
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(final String newText) {
        Log.v(TAG, "query \"" + newText +"\"");
        Fragment fragment = fragmentManager.findFragmentByTag(Constants.FRAGMENT_SEARCH);
        if(newText != null && newText.trim().length()>0) {
            final String trimmedText = newText.trim();

            if(previousQuery!= null && trimmedText.contains(previousQuery)){
                Log.i(TAG, "similar query true");
                final Handler handler = new Handler();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(searchPackages.getStatus() == AsyncTask.Status.FINISHED){
                            searchPackages = new SearchPackages(true);
                            searchPackages.execute(trimmedText);
                        } else {
                            handler.postDelayed(this, 200);
                        }
                    }
                });

            } else {
                Log.i(TAG, "similar query false");
                searchResults.clear();
                if(searchPackages != null && searchPackages.getStatus() != AsyncTask.Status.FINISHED) {
                    searchPackages.cancel(true);
                    setProgressBarState(false);
                }

                searchPackages = new SearchPackages();
                searchPackages.execute(trimmedText);
            }
            previousQuery = trimmedText;
            setProgressBarState(true);

        } else if(fragment!= null)
            ((SearchFragment)fragment).setSearchResults(list);
        return true;
    }

    private class PackagesComparator implements Comparator<PackageInfo> {

        @Override
        public int compare(PackageInfo lhs, PackageInfo rhs) {
            return lhs.packageName.compareTo(rhs.packageName);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.e(TAG, "code "+requestCode);
        switch (requestCode) {
            case Constants.WRITE_EXTERNAL_STORAGE_PERMISSION:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Fragment fragment;
                    if((fragment = fragmentManager.findFragmentByTag(Constants.FRAGMENT_SEARCH)) != null)
                        Utils.extractAPK(this, ((SearchFragment)fragment).getPackageInfo());
                    else if ((fragment = fragmentManager.findFragmentByTag(Constants.FRAGMENT_EXPORT)) != null){
                        ((ExportFragment)fragment).permissionGranted();
                    }
                } else {
                    Utils.displayToast(this, getString(R.string.write_permission_is_required_to_perform_this_action));
                }
                break;
            case Constants.INTERNET_PERMISSION:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Fragment fragment = fragmentManager.findFragmentByTag(Constants.FRAGMENT_IP_FIND);
                    if(fragment != null)
                        ((IPFinderFragment)fragment).searchIPs();
                } else {
                    Utils.displayToast(this, getString(R.string.internet_permission_is_required_to_perform_this_action));
                }
                break;
            case Constants.READ_PHONE_STATE_PERMISSION:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Fragment fragment = fragmentManager.findFragmentByTag(Constants.FRAGMENT_SIM_INFO);
                    if(fragment != null)
                        ((SimInfoFragment)fragment).permissionGranted();
                } else {
                    Utils.displayToast(this, getString(R.string.phone_permission_is_required_to_perform_this_action));
                }
                break;
        }
    }

    interface OnLoadListener{
        void onLoaded(List<PackageInfo> packageInfos);
    }

    private class LoadPackages extends AsyncTask<Void, Void, List<PackageInfo>>{
        private OnLoadListener listener;

        LoadPackages(OnLoadListener listener) {
            this.listener = listener;
        }

        @Override
        protected List<PackageInfo> doInBackground(Void... params) {
            packageManager = getPackageManager();
                list = packageManager.getInstalledPackages(0);
                if(list.size() < 50){
                    list = new ArrayList<>();
                    Process process;
                    BufferedReader bufferedReader = null;
                    try
                    {
                        process = Runtime.getRuntime().exec("pm list packages");
                        bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                        String line;
                        while((line=bufferedReader.readLine()) != null)
                        {
                            final String packageName = line.substring(line.indexOf(':')+1);
                            final PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
                            list.add(packageInfo);
                        }
                        process.waitFor();
                    }
                    catch(Exception f)
                    {
                        f.printStackTrace();
                    }
                    if(bufferedReader != null)
                        try {
                            bufferedReader.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                }

            searchResults = new ArrayList<>();

            return list;
        }

        @Override
        protected void onPostExecute(List<PackageInfo> packageInfos) {
            super.onPostExecute(packageInfos);
            if(listener != null){
                listener.onLoaded(packageInfos);
            }
        }
    }

    private class SearchPackages extends AsyncTask<String, Void, List<PackageInfo>>{
        private boolean searchInCurrentResults;

        SearchPackages(boolean searchInCurrentResults){
            this.searchInCurrentResults = searchInCurrentResults;
        }

        SearchPackages(){
            searchInCurrentResults = false;
        }

        @Override
        protected List<PackageInfo> doInBackground(String... params) {
            String textToFind = params[0].toLowerCase();

            ArrayList<PackageInfo> input;
            ArrayList<PackageInfo> results = new ArrayList<>();

            if(searchInCurrentResults){
                input = (ArrayList<PackageInfo>) searchResults;

            }
            else {
                input = (ArrayList<PackageInfo>) list;
            }
            Log.i(TAG, "similar query " + searchInCurrentResults + " " + input.size());
            for (PackageInfo p : input) {
                if (p.packageName.toLowerCase().contains(textToFind) ||
                        String.valueOf(p.applicationInfo.loadLabel(packageManager)).toLowerCase().
                                contains(textToFind)) {
                    results.add(p);
                }
                if (isCancelled())
                    break;
            }
            Log.i(TAG, "results size " + results.size());
            return results;
        }

        @Override
        protected void onPostExecute(List<PackageInfo> packageInfos) {
            super.onPostExecute(packageInfos);
            searchResults = packageInfos;
            setProgressBarState(false);
            Collections.sort(searchResults, new PackagesComparator());
            Fragment fragment;
            if((fragment = fragmentManager.findFragmentByTag(Constants.FRAGMENT_SEARCH)) != null)
                ((SearchFragment)fragment).setSearchResults(searchResults);

        }
    }
}
