package com.kaszubski.kamil.emmhelper;

import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
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
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.kaszubski.kamil.emmhelper.utils.Constants;
import com.kaszubski.kamil.emmhelper.utils.Utils;

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
    private FloatingActionButton fab;
    private SearchPackages searchPackages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        fragmentManager = getSupportFragmentManager();

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.nav_app_list);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if(fragmentManager.findFragmentById(R.id.container) != null) {
            switch (fragmentManager.findFragmentById(R.id.container).getTag()) {
                case Constants.FRAGMENT_SEARCH:
                    getMenuInflater().inflate(R.menu.search_bar, menu);

                    SearchManager searchManager =
                            (SearchManager) getSystemService(Context.SEARCH_SERVICE);
                    searchView =
                            (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
                    searchView.setSearchableInfo(
                            searchManager.getSearchableInfo(getComponentName()));
                    searchView.setQueryHint("Package name...");
                    searchView.setIconified(false);
                    searchView.requestFocus();
                    searchView.setMaxWidth((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 260, getResources().getDisplayMetrics()));

                    progressDialog = new ProgressDialog(this, ProgressDialog.STYLE_SPINNER);
                    progressDialog.setMessage("Loading packages...");
                    progressDialog.setIndeterminate(true);
                    progressDialog.show();

                    new LoadPackages().execute();

                    break;
//            case 1:
//                getMenuInflater().inflate(R.menu.main, menu);
//                break;
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id){
            case R.id.action_export_to_csv:
                ArrayList<String> export = ((SearchFragment)fragmentManager.findFragmentByTag(Constants.FRAGMENT_SEARCH)).getExportList();
                if(export != null && export.size()>0){
                    Intent intent = new Intent(this, ExportActivity.class);
                    intent.putExtra(Constants.PACKAGES_KEY, export);
                    startActivity(intent);
                } else {
                    Utils.showToast(this, "Export list is empty");
                }
                break;
            case R.id.action_clear_export_list:
                ((SearchFragment)fragmentManager.findFragmentByTag(Constants.FRAGMENT_SEARCH)).clearExportList();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();


        switch (id){
            case R.id.nav_app_list:
                fragmentManager.beginTransaction().replace(R.id.container, new SearchFragment(), Constants.FRAGMENT_SEARCH).setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).commit();
                fab.hide();
                break;
            case R.id.nav_manifest_viewer:
                break;
            case R.id.nav_root_explorer:

                break;
            case R.id.nav_ip_for_hostname:

                break;
        }
        return true;
    }

    public void closeDrawer(){
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        Log.v(TAG, "query \"" + newText +"\"");
        Fragment fragment;
        if(newText.trim().length()>0) {
            searchResults.clear();
            if(searchPackages != null )
                searchPackages.cancel(true);
            searchPackages = new SearchPackages();
            searchPackages.execute(newText);

        } else if((fragment = fragmentManager.findFragmentByTag(Constants.FRAGMENT_SEARCH)) != null)
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
            case Constants.WRITE_PERMISSION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Fragment fragment;
                    if((fragment = fragmentManager.findFragmentByTag(Constants.FRAGMENT_SEARCH)) != null)
                        Utils.extractAPK(this, ((SearchFragment)fragment).getPackageInfo());
                } else {
                    Utils.showToast(this, "Write permission is required extract APKs");
                }
                break;
            }
        }
    }

    class LoadPackages extends AsyncTask<Void, Void, List<PackageInfo>>{

        @Override
        protected List<PackageInfo> doInBackground(Void... params) {
            packageManager = getPackageManager();
            list = packageManager.getInstalledPackages(PackageManager.GET_ACTIVITIES);
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

        @Override
        protected List<PackageInfo> doInBackground(String... params) {
            for (PackageInfo p : list) {
                if (p.packageName.contains(params[0]) || String.valueOf(p.applicationInfo.loadLabel(packageManager)).contains(params[0])) {
                    searchResults.add(p);
                }
            }

            return searchResults;
        }

        @Override
        protected void onPostExecute(List<PackageInfo> packageInfos) {
            super.onPostExecute(packageInfos);

            Collections.sort(searchResults, new PackagesComparator());
            Fragment fragment;
            if((fragment = fragmentManager.findFragmentByTag(Constants.FRAGMENT_SEARCH)) != null)
                ((SearchFragment)fragment).setSearchResults(searchResults);

        }
    }
}
