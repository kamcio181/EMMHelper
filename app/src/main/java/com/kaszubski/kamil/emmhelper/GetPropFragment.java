package com.kaszubski.kamil.emmhelper;


import android.app.SearchManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
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

import com.kaszubski.kamil.emmhelper.utils.ExportableContent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;

public class GetPropFragment extends Fragment implements ExportableContent, android.support.v7.widget.SearchView.OnQueryTextListener {
    private static final String TAG = GetPropFragment.class.getSimpleName();
    private Context context;
    private ArrayList<String> properties = new ArrayList<>();
    private ArrayList<Integer> searchResults = new ArrayList<>();
    private SearchProperties searchPackagesTask;
    private RecyclerView recyclerView;
    private String previousQuery = "";
    private android.support.v7.widget.SearchView searchView;


    public GetPropFragment() {
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
        ((AppCompatActivity)context).invalidateOptionsMenu();
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((MainActivity)context).closeDrawer();
        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);

        new LoadProperties().execute();
    }

    public ArrayList<String> getExportList(){
//        if(info == null || info.length() <= 0)
//            return null;
//
//        ArrayList<String> list = new ArrayList<>();
//        list.addAll(Arrays.asList(info.toString().split("<br/>")));
//
//
////        if(textView.getText().toString().trim().length() <= 0)
////            return null;
////
////        list.addAll(Arrays.asList(textView.getText().toString().split("\n")));
        return properties;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        Log.d(TAG, "Query " + "\"" +newText + "\"");

        if(properties == null)
            return true;

        final String trimmedText = newText.trim();
        if(trimmedText.length()>0) {

            if(previousQuery.length() > 0 && trimmedText.contains(previousQuery)){
                final Handler handler = new Handler();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if(searchPackagesTask != null && searchPackagesTask.getStatus() == AsyncTask.Status.FINISHED){
                            searchPackagesTask = new SearchProperties(true);
                            searchPackagesTask.execute(trimmedText);
                        } else {
                            handler.postDelayed(this, 200);
                        }
                    }
                });
            } else {
                if(searchPackagesTask != null && searchPackagesTask.getStatus() != AsyncTask.Status.FINISHED){
                    searchPackagesTask.cancel(true);
                }
                searchPackagesTask = new SearchProperties();
                searchPackagesTask.execute(trimmedText);
            }

            previousQuery = trimmedText;








//            int previousIndex = 0;
//            while (true) {
//                int startIndex = properties.indexOf(trimmedText, previousIndex +1);
//                if (startIndex == -1) {
//                    propertiesToShow = "";
//                    break;
//                }
//
//                Log.d(TAG, "Start index " + startIndex);
//
//                int tempStart = properties.lastIndexOf("<br/>", startIndex);
//                int tempEnd = properties.indexOf("<br/>", startIndex);
//
//                Log.d(TAG, "temp S " + tempStart + " temp E " + tempEnd);
//                if (builder.length() != 0)
//                    builder.append("<br/>");
//                builder.append(properties.substring(tempStart < 0? 0 : tempStart + 5, tempEnd == -1 ? properties.length() : tempEnd));
//
//                if (previousIndex == startIndex)
//                    break;
//
//                previousIndex = tempEnd;
//            }
//
//            propertiesToShow = builder.toString();
        } else {
            searchResults = new ArrayList<>();
            previousQuery = trimmedText;
            showProperties();
        }

//        textView.setText(Html.fromHtml(propertiesToShow));

        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_getprop, menu);
        SearchManager searchManager =
                (SearchManager) context.getSystemService(Context.SEARCH_SERVICE);
        searchView =
                (android.support.v7.widget.SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getActivity().getComponentName()));
        searchView.setQueryHint(getString(R.string.properties_query_hint));
        searchView.setOnQueryTextListener(this);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    public void showProperties(){
        if (recyclerView.getAdapter() == null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
//            recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL_LIST));
            recyclerView.setAdapter(new RecyclerAdapter());
//                recyclerView.setAdapter(new SearchFragment.SearchRecyclerAdapter(context, results, new SearchFragment.OnItemClickListener() {
//                    @Override
//                    public void onItemClick(View view, final PackageInfo packageInfo, boolean isLong) {
//                        final String packageName = packageInfo.packageName;
//                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
//                        builder.setItems(context.getResources().getStringArray(R.array.package_menu),
//                                new DialogInterface.OnClickListener() {
//                                    @Override
//                                    public void onClick(DialogInterface dialog, int which) {
//                                        switch (which) {
//                                            case 0:
//                                                Utils.showAppInfo(context, packageName);
//                                                break;
//                                            case 1:
//                                                //view manifest
//                                                Intent intent = new Intent(context, ManifestViewerActivity.class);
//                                                intent.putExtra(Constants.PACKAGE_INFO_KEY, packageName);
//                                                startActivity(intent);
//                                                break;
//                                            case 2:
//                                                Utils.copyToClipboard(context, packageName);
//                                                break;
//                                            case 3:
//                                                Utils.shareViaList(context, packageName);
//                                                break;
//                                            case 4:
//                                                if(ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                                                        == PackageManager.PERMISSION_GRANTED){
//                                                    Utils.extractAPK(context, packageInfo);
//                                                }
//                                                else {
//                                                    SearchFragment.this.packageInfo = packageInfo;
//                                                    ActivityCompat.requestPermissions((AppCompatActivity)context, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
//                                                            Constants.WRITE_EXTERNAL_STORAGE_PERMISSION);
//                                                }
//                                                break;
//                                        }
//                                    }
//                                }).create().show();
//                    }
//                }));
        } else {
            ((RecyclerAdapter)recyclerView.getAdapter()).refreshResults();
        }
    }

    //    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        Log.v(TAG, "onOptionsItemSelected");
//
//        switch (item.getItemId()){
//            case R.id.action_move_to_other_folder:
//                handleNoteMoveAction();
//                break;
//            case R.id.action_delete:
//                deleteNote();
//                break;
//            case R.id.action_discard_changes:
//                discardChanges();
//                break;
//            case R.id.action_save:
//                saveNote(true);
//                break;
//            case R.id.action_share:
//                Utils.sendShareIntent(context, getContent(),
//                        ((AppCompatActivity)context).getTitle().toString());
//                break;
//        }
//        return true;
//    }

    private class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.DoubleLineViewHolder> {
        private boolean showAll;
//        private SearchFragment.OnItemClickListener listener;
//        private ArrayList<String> properties;
//        private ArrayList<String> export = new ArrayList<>();

        RecyclerAdapter() {
            showAll = (searchResults.size() == 0 && previousQuery.length() == 0);
            setHasStableIds(true);
//            this.properties = properties;
//            this.listener = listener;
//            setHasStableIds(true);
        }

        void refreshResults() {
            showAll = (searchResults.size() == 0 && previousQuery.length() == 0);
            notifyDataSetChanged();
        }

        @Override
        public RecyclerAdapter.DoubleLineViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new RecyclerAdapter.DoubleLineViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.double_line_recycler_view_item, parent, false));
        }

        @Override
        public void onBindViewHolder(RecyclerAdapter.DoubleLineViewHolder holder, int position) {
//            holder.avatarImageView.setImageDrawable(packages.get(position).applicationInfo.loadIcon(context.getPackageManager()));
            String[] property = properties.get(showAll? position : searchResults.get(position)).split(":");
            holder.titleTextView.setText(property[0].trim());
            holder.subtitleTextView.setText(property[1].trim());
//            if(export.contains(packages.get(position).packageName))
//                holder.itemView.setBackgroundColor(Color.LTGRAY);
//            else
//                holder.itemView.setBackgroundColor(Color.WHITE);
        }

        @Override
        public int getItemCount() {
            if(showAll)
                return properties.size();
            else
                return searchResults.size();
        }

        class DoubleLineViewHolder extends RecyclerView.ViewHolder {
            TextView titleTextView, subtitleTextView;
//            ImageView avatarImageView;
            View itemView;

            DoubleLineViewHolder(final View itemView) {
                super(itemView);
                this.itemView = itemView;
//                itemView.setOnClickListener(new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        String packageName = packages.get(getLayoutPosition()).packageName;
//                        if(export.contains(packageName)){
//                            export.remove(packageName);
//                            itemView.setBackgroundColor(Color.WHITE);
//                            Utils.displayToast(context, context.getString(R.string.package_name_removed_from_export_list));
//                        } else {
//                            export.add(packageName);
//                            itemView.setBackgroundColor(Color.LTGRAY);
//                            Utils.displayToast(context, context.getString(R.string.package_name_added_to_export_list));
//                        }
//                    }
//                });
//                itemView.setOnLongClickListener(new View.OnLongClickListener() {
//                    @Override
//                    public boolean onLongClick(View v) {
//                        if(listener != null)
//                            listener.onItemClick(itemView, packages.get(getLayoutPosition()), true);
//                        return true;
//                    }
//                });

//                avatarImageView = (ImageView) itemView.findViewById(R.id.imageView2);
                titleTextView = (TextView) itemView.findViewById(R.id.textView2);
                subtitleTextView = (TextView) itemView.findViewById(R.id.textView3);
            }
        }

        @Override
        public long getItemId(int position) {
            if(showAll)
                return position;
            else
                return searchResults.get(position);
        }

//        ArrayList<String> getExport() {
//            return export;
//        }
//
//        void clearExport() {
//            this.export = new ArrayList<>();
//            notifyDataSetChanged();
//        }
//
//        void selectAllVisible() {
//            this.export = new ArrayList<>(packages.size());
//            for(PackageInfo packageInfo : packages)
//                export.add(packageInfo.packageName);
//            notifyDataSetChanged();
//        }
    }

//    private void packageStateChanged(boolean added){
//        ((MainActivity)context).reloadPackages();
//        Utils.displayToast(context, added? getString(R.string.application_installed_refreshing_list) : getString(R.string.application_uninstalled_refreshing_list));
//    }

    private class LoadProperties extends AsyncTask<Void, Void, Void>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

//            progressDialog = new ProgressDialog(context.getApplicationContext(), ProgressDialog.STYLE_SPINNER);
//            progressDialog.setMessage("Loading Properties...");
//            progressDialog.setIndeterminate(true);
//            progressDialog.setCancelable(false);
//            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            BufferedReader bufferedReader = null;

            try
            {
                Process process;
                process = Runtime.getRuntime().exec("getprop");
                bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while((line=bufferedReader.readLine()) != null)
                {
                    line = line.replaceAll("\\[", "").replaceAll("]", "");
                    properties.add(line);
                    Log.d(TAG, line);
                }
                process.waitFor();
            }
            catch(Exception f)
            {
                f.printStackTrace();
            }
            if(bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            Collections.sort(properties);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

//            progressDialog.dismiss();
            showProperties();

        }
    }

    private class SearchProperties extends AsyncTask<String, Void, Void>{
        private boolean searchInCurrentResults;

        SearchProperties(boolean searchInCurrentResults){
            this.searchInCurrentResults = searchInCurrentResults;
        }

        SearchProperties(){
            searchInCurrentResults = false;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            ((MainActivity)context).setProgressBarState(true);
        }

        @Override
        protected Void doInBackground(String... strings) {
            String textToFind = strings[0].toLowerCase();

            ArrayList<Integer> results = new ArrayList<>();

            if(searchInCurrentResults){
                for (int i = 0; i < searchResults.size(); i++){
                    if(properties.get(searchResults.get(i)).toLowerCase().contains(textToFind))
                        results.add(searchResults.get(i));
                    if(isCancelled())
                        return null;
                }

            } else {
                for (int i = 0; i < properties.size(); i++){
                    if(properties.get(i).toLowerCase().contains(textToFind))
                        results.add(i);
                    if(isCancelled())
                        return null;
                }
            }

            searchResults = results;

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            ((MainActivity)context).setProgressBarState(false);
            showProperties();

        }
    }
}
