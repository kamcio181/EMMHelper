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
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
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
import java.util.ArrayList;
import java.util.Collections;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ExportFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ExportFragment extends Fragment {
    private static final String TAG = "ExportFragment";
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_FILE_EXTENSION = "extension";
    private static final String ARG_ARRAY_TO_SAVE = "array";

    private String fileExtension;
    private ArrayList<String> arrayToSave;
    private String name;

    private Context context;
    private RecyclerView recyclerView;
    private String path;


    public ExportFragment() {
        // Required empty public constructor
    }

    public static ExportFragment newInstance(String fileExtension) {
        ExportFragment fragment = new ExportFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FILE_EXTENSION, fileExtension);
        fragment.setArguments(args);
        return fragment;
    }

    public static ExportFragment newInstance(String fileExtension, ArrayList<String> arrayToSave) {
        ExportFragment fragment = new ExportFragment();
        Bundle args = new Bundle();
        args.putString(ARG_FILE_EXTENSION, fileExtension);
        args.putStringArrayList(ARG_ARRAY_TO_SAVE, arrayToSave);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            fileExtension = getArguments().getString(ARG_FILE_EXTENSION);
            if (getArguments().getStringArrayList(ARG_ARRAY_TO_SAVE) != null){
                arrayToSave = getArguments().getStringArrayList(ARG_ARRAY_TO_SAVE);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        context = getActivity();
        ((AppCompatActivity)context).invalidateOptionsMenu();
        return inflater.inflate(R.layout.fragment_export, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(context instanceof MainActivity)
            ((MainActivity)context).closeDrawer();
        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        readFiles();
    }

    public void permissionGranted(){
        if(name != null)
            new SaveToFile().execute(name);
        else readFiles();
    }

    public void moveUp(){
        path = path.substring(0, path.length()-1);
        path = path.substring(0, path.lastIndexOf("/"));
        new GetFiles().execute();
    }

    public void writeToFile(){
        if(new File(path).canWrite())
            setFileName().show();
        else
            Utils.showToast(context, "You are not allowed to write in this folder");
    }

    private Dialog setFileName(){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = getLayoutInflater(null);
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

    private void readFiles(){
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED){
            path = Environment.getExternalStorageDirectory().getAbsolutePath();
            new GetFiles().execute();
        }
        else {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    Constants.WRITE_PERMISSION);
        }
    }

    private DialogInterface.OnClickListener getFileOverrideAction(final String name){
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                saveNote(name);
            }
        };
    }
    private void saveNote(String name){
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED)
            new SaveToFile().execute(name);
        else {
            this.name = name;
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    Constants.WRITE_PERMISSION);
        }
    }

    private class GetFiles extends AsyncTask<Void, Void, Boolean> {
        private ArrayList<String> dirs;
        private ArrayList<String> files;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            path = path+"/";
            ((AppCompatActivity)context).setTitle(path);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            File file = new File(path);
            String[] items = file.canRead()? file.list() : new String[0];
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
                                else if(fileExtension.equals(Constants.APK_FILE_EXTENSION)){ // view manifest xml
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
                ((AppCompatActivity)context).finish();
            }
            else
                Utils.showToast(context, "Failed");
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
