package com.kaszubski.kamil.emmhelper;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.TransactionTooLargeException;
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
import android.widget.ProgressBar;
import android.widget.TextView;

import com.kaszubski.kamil.emmhelper.utils.Constants;
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
    private static final String TAG = "MainActivity";
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
        }
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
            switch (fragmentManager.findFragmentById(R.id.container).getTag()) {
                case Constants.FRAGMENT_SEARCH:
                    longTextTitleMode(false);
                    setTitle(getString(R.string.application_list));
                    getMenuInflater().inflate(R.menu.fragment_search, menu);
                    SearchManager searchManager =
                            (SearchManager) getSystemService(Context.SEARCH_SERVICE);
                    searchView =
                            (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
                    searchView.setSearchableInfo(
                            searchManager.getSearchableInfo(getComponentName()));
                    searchView.setQueryHint(getString(R.string.query_hint));

                    progressDialog = new ProgressDialog(this, ProgressDialog.STYLE_SPINNER);
                    progressDialog.setMessage(getString(R.string.loading_packages));
                    progressDialog.setIndeterminate(true);
                    progressDialog.setCancelable(false);
                    progressDialog.show();

                    new LoadPackages().execute();

                    break;
                case Constants.FRAGMENT_IP_FINDER:
                    longTextTitleMode(false);
                    setTitle(getString(R.string.hostname_ips));
                    getMenuInflater().inflate(R.menu.fragment_ip_finder, menu);
                    break;
                case Constants.FRAGMENT_EXPORT:
                    longTextTitleMode(true);
                    break;
            }
        }
        return true;
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

        switch (id){
            case R.id.action_share_list:
                if(fragmentManager.findFragmentByTag(Constants.FRAGMENT_SEARCH) != null) {
                    ArrayList<String> export = ((SearchFragment) fragmentManager.findFragmentByTag(Constants.FRAGMENT_SEARCH)).getExportList();
                    if(export != null && export.size()>0){
                        StringBuilder builder = new StringBuilder();
                        for(String s : export)
                            builder.append(s).append("\n");
                        Utils.shareViaList(this, builder.toString().trim());
                    } else {
                        Utils.showToast(this, getString(R.string.export_list_is_empty));
                    }
                }
                break;
            case R.id.action_export_to_csv:
                ArrayList<String> export;
                if(fragmentManager.findFragmentByTag(Constants.FRAGMENT_SEARCH) != null)
                    export = ((SearchFragment)fragmentManager.findFragmentByTag(Constants.FRAGMENT_SEARCH)).getExportList();
                else
                    export = ((IPFinderFragment)fragmentManager.findFragmentByTag(Constants.FRAGMENT_IP_FINDER)).getExportList();
                if(export != null && export.size()>0){
                    Intent intent = new Intent(this, ExportActivity.class);
                    intent.putExtra(Constants.ARRAY_KEY, export);
                    intent.putExtra(Constants.FILE_FORMAT_KEY, Constants.CSV_FILE_EXTENSION);
                    startActivity(intent);
                } else {
                    Utils.showToast(this, getString(R.string.export_list_is_empty));
                }
                break;
            case R.id.action_clear_export_list:
                ((SearchFragment)fragmentManager.findFragmentByTag(Constants.FRAGMENT_SEARCH)).clearExportList();
                break;
            case R.id.action_share:
                if(fragmentManager.findFragmentByTag(Constants.FRAGMENT_IP_FINDER) != null){
                    ArrayList<String> list = ((IPFinderFragment)fragmentManager.findFragmentByTag(Constants.FRAGMENT_IP_FINDER)).getExportList();
                    if(list != null && list.size()>0){
                        StringBuilder builder = new StringBuilder();
                        for(String s : list)
                            builder.append(s).append("\n");
                        Utils.shareViaList(this, builder.toString().trim());
                    } else {
                        Utils.showToast(this, "IP list is empty");
                    }
                }
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        switch (id) {
            case R.id.nav_app_list:
                fragmentManager.beginTransaction().
                        replace(R.id.container, new SearchFragment(), Constants.FRAGMENT_SEARCH).
                        setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit();
                break;
            case R.id.nav_manifest_viewer:
                Utils.showToast(this, getString(R.string.find_apk_file_to_view_its_manifest));
                fragmentManager.beginTransaction().
                        replace(R.id.container, ExportFragment.newInstance(Constants.APK_FILE_EXTENSION), Constants.FRAGMENT_EXPORT).
                        setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit();
                break;
            case R.id.nav_root_explorer:
                fragmentManager.beginTransaction().
                        replace(R.id.container, new ExportFragment(), Constants.FRAGMENT_EXPORT).
                        setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit();
                break;
            case R.id.nav_ip_for_hostname:
                fragmentManager.beginTransaction().
                        replace(R.id.container, new IPFinderFragment(), Constants.FRAGMENT_IP_FINDER).
                        setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit();
                break;
        }
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
        if(newText.trim().length()>0) {
            if(progressBar == null && fragment!= null)
                progressBar = ((SearchFragment)fragment).getProgressBar();

            if(previousQuery!= null && newText.contains(previousQuery)){
                Log.i(TAG, "similar query true");
                final Handler handler = new Handler();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(searchPackages.getStatus() == AsyncTask.Status.FINISHED){
                            searchPackages = new SearchPackages(true);
                            searchPackages.execute(newText);
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
                    progressBar.setVisibility(View.INVISIBLE);
                }

                searchPackages = new SearchPackages();
                searchPackages.execute(newText);
            }
            previousQuery = newText;
            progressBar.setVisibility(View.VISIBLE);

        } else if(fragment!= null)
            ((SearchFragment)fragment).setSearchResults(list);
        return true;
    }

    class PackagesComparator implements Comparator<PackageInfo> {

        @Override
        public int compare(PackageInfo lhs, PackageInfo rhs) {
            return lhs.packageName.compareTo(rhs.packageName);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.e(TAG, "code "+requestCode);
        switch (requestCode) {
            case Constants.WRITE_PERMISSION:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Fragment fragment;
                    if((fragment = fragmentManager.findFragmentByTag(Constants.FRAGMENT_SEARCH)) != null)
                        Utils.extractAPK(this, ((SearchFragment)fragment).getPackageInfo());
                    else if ((fragment = fragmentManager.findFragmentByTag(Constants.FRAGMENT_EXPORT)) != null){
                        ((ExportFragment)fragment).permissionGranted();
                    }
                } else {
                    Utils.showToast(this, getString(R.string.write_permission_is_required_to_perform_this_action));
                }
                break;
            case Constants.INTERNET_PERMISSION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Fragment fragment = fragmentManager.findFragmentByTag(Constants.FRAGMENT_IP_FINDER);
                    if(fragment != null)
                        ((IPFinderFragment)fragment).searchIPs();
                } else {
                    Utils.showToast(this, getString(R.string.internet_permission_is_required_to_perform_this_action));
                }
                break;
            }
        }
    }

    class LoadPackages extends AsyncTask<Void, Void, List<PackageInfo>>{

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
            Fragment fragment;
            if((fragment = fragmentManager.findFragmentByTag(Constants.FRAGMENT_SEARCH)) != null)
                ((SearchFragment)fragment).setSearchResults(packageInfos);
            searchView.setOnQueryTextListener(MainActivity.this);
            progressDialog.dismiss();
        }
    }

    class SearchPackages extends AsyncTask<String, Void, List<PackageInfo>>{
        private boolean searchInCurrentResults;

        public SearchPackages(boolean searchInCurrentResults){
            this.searchInCurrentResults = searchInCurrentResults;
        }

        public SearchPackages(){
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
            progressBar.setVisibility(View.INVISIBLE);
            Collections.sort(searchResults, new PackagesComparator());
            Fragment fragment;
            if((fragment = fragmentManager.findFragmentByTag(Constants.FRAGMENT_SEARCH)) != null)
                ((SearchFragment)fragment).setSearchResults(searchResults);

        }
    }
}
