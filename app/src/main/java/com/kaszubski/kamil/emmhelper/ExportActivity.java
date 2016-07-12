package com.kaszubski.kamil.emmhelper;

import android.Manifest;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.kaszubski.kamil.emmhelper.utils.Constants;
import com.kaszubski.kamil.emmhelper.utils.DividerItemDecoration;
import com.kaszubski.kamil.emmhelper.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class ExportActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "ExportActivity";
    private Context context;
    private RecyclerView recyclerView;
    private String path;
    private boolean exit = false;
    private Handler handler = new Handler();
    private Runnable exitRunnable;
    private String name;
    private boolean hideFab = false;
    private ArrayList<String> arrayToSave = new ArrayList<>();
    private static String fileExtension;

    @Override
    protected void onCreate(Bundle savedInstanceState) { // TODO apk icon browser
        super.onCreate(savedInstanceState);
        context = this;
        setContentView(R.layout.activity_export);

        setResetExitFlagRunnable();

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        Intent intent = getIntent();

        fileExtension = intent.getStringExtra(Constants.FILE_FORMAT_KEY);

        if(!intent.hasExtra(Constants.FILE_FORMAT_KEY)){ //file browser mode
            hideFab = true;
        } else if((fileExtension = intent.getStringExtra(Constants.FILE_FORMAT_KEY)).equals(Constants.APK_FILE_EXTENSION)){ //find apk file to view manifest
            hideFab = true;
        } else if(intent.hasExtra(Constants.STRING_KEY)){ //save string i.e. manifest
            arrayToSave.addAll(Arrays.asList(intent.getStringExtra(Constants.STRING_KEY).split("\n")));
        } else if (intent.hasExtra(Constants.ARRAY_KEY)){ //save string array list i.e. packageNames
            arrayToSave = intent.getStringArrayListExtra(Constants.ARRAY_KEY);
        } else
            finish();

        setupToolbarAndFab();

        readFiles();
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
        if(hideFab && fab != null)
            fab.hide();
        else if(fab!=null)
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

    private DialogInterface.OnClickListener getFileOverrideAction(final String name){
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                saveNote(name);
            }
        };
    }

    private Dialog setFileName(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.dialog_edit_text, null);
        final EditText titleEditText = (EditText) layout.findViewById(R.id.titleEditText);
        titleEditText.setText("Untitled");
        titleEditText.setSelection(0, titleEditText.length());

        return builder.setTitle("Set file name").setView(layout)
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog1, int which) {
                        String name1 = titleEditText.getText().toString().length() == 0 ? "Untitled"
                                : titleEditText.getText().toString();
                        int i = 0;
                        String suffix = "";
                        while (new File(path, name1 + suffix + fileExtension).exists()) {
                            i++;
                            suffix = Integer.toString(i);
                        }
                        saveNote(name1 + suffix + fileExtension);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog1, int which) {
                        Utils.showToast(context, "Cancelled");
                    }
                }).create();
    }

    private void saveNote(String name){
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED)
            new SaveToFile().execute(name);
        else {
            this.name = name;
            ActivityCompat.requestPermissions(ExportActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    Constants.WRITE_PERMISSION);
        }
    }

    private void readFiles(){
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED){
            path = Environment.getExternalStorageDirectory().getAbsolutePath();
            new GetFiles().execute();
        }
        else {
            ActivityCompat.requestPermissions(ExportActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    Constants.WRITE_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.WRITE_PERMISSION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if(name != null)
                        new SaveToFile().execute(name);
                    else readFiles();

                } else {
                    Utils.showToast(context, "Write permission is required to export notes");
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
                if(new File(path).canWrite())
                    setFileName().show();
                else
                    Utils.showToast(this, "You are not allowed to write in this folder");
                break;
        }
    }

    private class GetFiles extends AsyncTask<Void, Void, Boolean>{
        private ArrayList<String> dirs;
        private ArrayList<String> files;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            path = path+"/";
            setTitle(path);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            File file = new File(path);
            String[] items = file.list();
            dirs = new ArrayList<>();
            files = new ArrayList<>();
            if(items != null) {
                for (String i : items) {
                    if (new File(path+i).isDirectory())
                        dirs.add(i);
                    else if(fileExtension == null)
                        files.add(i);
                    else if (i.endsWith(fileExtension))
                        files.add(i.substring(0, i.lastIndexOf(".")));
                }
                Collections.sort(dirs, String.CASE_INSENSITIVE_ORDER);
                Collections.sort(files, String.CASE_INSENSITIVE_ORDER);
                return true;
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (result) {
                final int dirsCount = dirs.size();
                dirs.addAll(files);
                if (recyclerView.getAdapter() == null) {
                    recyclerView.setLayoutManager(new LinearLayoutManager(context));
                    recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL_LIST));
                    recyclerView.setAdapter(new RecyclerViewAdapter(dirs, dirsCount, new RecyclerViewAdapter.OnItemClickListener() {
                        @Override
                        public void onItemClick(boolean isDir, String item) {
                            if (isDir) {
                                if (new File(path + item).canRead()) {
                                    path = path + item;
                                    new GetFiles().execute();
                                } else
                                    Utils.showToast(context, "You are not allowed to view this folder");
                            } else {
                                if(fileExtension == null){ //file browser
                                    File file = new File(path + item);
                                    Log.e(TAG, item.substring(0, item.lastIndexOf(".")));
                                    String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(item.substring(item.lastIndexOf(".")+1));

                                    Intent intent = new Intent();
                                    intent.setAction(android.content.Intent.ACTION_VIEW);
                                    intent.setDataAndType(Uri.fromFile(file), mime);
                                    try {
                                        startActivity(intent);
                                    } catch (ActivityNotFoundException e){
                                        Utils.showToast(context, "Unable to find application to open this type of file");
                                    }
                                }
                                else if(hideFab){ // view manifest xml
                                    Intent intent = new Intent(context, XmlViewerActivity.class);
                                    intent.putExtra(Constants.SOURCE_DIR, path + item + fileExtension);
                                    startActivity(intent);
                                } else {
                                    Utils.getConfirmationDialog(context, "Do you want to override this file?",
                                            getFileOverrideAction(item + fileExtension)).show();
                                }
                            }
                        }
                    }));
                } else {
                    ((RecyclerViewAdapter)recyclerView.getAdapter()).changeData(dirs, dirsCount);
                }
            }
        }
    }

    private class SaveToFile extends AsyncTask<String, Void, Boolean>{
        @Override
        protected Boolean doInBackground(String... params) {
            File file = new File(path, params[0]);
            FileOutputStream f;

            try {
                f = new FileOutputStream(file);
                PrintWriter p = new PrintWriter(f);
                for(String line : arrayToSave)
                    p.println(line);
                p.flush();
                p.close();
                f.close();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);

            if(aBoolean) {
                Utils.showToast(context, "Saving to " + fileExtension.substring(1) + " file");
                finish();
            }
            else
                Utils.showToast(context, "Failed");
        }
    }

    @Override
    public void onBackPressed() {
        if(path.equals("/"))
            super.onBackPressed();
        else{
            path = path.substring(0, path.length()-1);
            path = path.substring(0, path.lastIndexOf("/"));
            new GetFiles().execute();
        }
    }
}

class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>{
    private static RecyclerViewAdapter.OnItemClickListener listener;
    private static ArrayList<String> items;
    private static int dirsCount;

    public interface OnItemClickListener{
        void onItemClick(boolean isDir, String item);
    }

    public RecyclerViewAdapter(ArrayList<String> items, int dirsCount, OnItemClickListener listener) {
        RecyclerViewAdapter.items = items;
        RecyclerViewAdapter.listener = listener;
        RecyclerViewAdapter.dirsCount = dirsCount;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.single_line_with_icon_item, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.titleTextView.setText(items.get(position));
        if(position>=dirsCount)
            holder.icon.setImageResource(R.drawable.ic_exp_grey_file);
        else
            holder.icon.setImageResource(R.drawable.ic_exp_grey_folder);
    }

    public void changeData(ArrayList<String> items, int dirsCount){
        RecyclerViewAdapter.items = items;
        RecyclerViewAdapter.dirsCount = dirsCount;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder{
        public TextView titleTextView;
        public ImageView icon;

        public ViewHolder(final View itemView){
            super(itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                        if(listener != null)
                            listener.onItemClick(getLayoutPosition()<dirsCount, items.get(getLayoutPosition()));
                }
            });

            titleTextView = (TextView) itemView.findViewById(R.id.textView2);
            icon = (ImageView) itemView.findViewById(R.id.imageView);

        }
    }
}