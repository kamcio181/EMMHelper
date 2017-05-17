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
import android.os.FileObserver;
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ExportFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ExportFragment extends Fragment {
    private static final String TAG = ExportFragment.class.getSimpleName();
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_FILE_EXTENSION = "extension";
    private static final String ARG_ARRAY_TO_SAVE = "array";

    private String fileExtension;
    private ArrayList<String> arrayToSave;
    private String name;

    private Context context;
    private RecyclerView recyclerView;
    private String path;
    private FileObserver fileObserver;
    private boolean refreshList = false;


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
        path = Environment.getExternalStorageDirectory().getAbsolutePath();
        readFiles();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG,"onResume");
        if (refreshList){
            readFiles();
            Utils.displayToast(context, "Files changed. Refreshing list");
        }
    }

    public void permissionGranted(){
        if(name != null)
            new SaveToFile().execute(name);
        else readFiles();
    }

    public void moveUp(){
        path = path.substring(0, path.length()-1);
        path = path.substring(0, path.lastIndexOf("/"));
        readFiles();
    }

    public void writeToFile(){
        if(new File(path).canWrite())
            Utils.getEdiTextDialog(context, "Set file name", getString(R.string.untitled), "File name", new Utils.OnTextSet() {
                @Override
                public void onTextSet(String text) {
                    String name1 = text.length() == 0 ? getString(R.string.untitled) : text;
                        int i = 0;
                        String suffix = "";
                        while (new File(path, name1 + suffix + fileExtension).exists()) {
                            i++;
                            suffix = Integer.toString(i);
                        }
                        saveToFile(name1 + suffix + fileExtension);
                }
            }, null, true).show();
        else
            Utils.displayToast(context, context.getString(R.string.you_are_not_allowed_to_write_in_this_folder));
    }

//    private Dialog setFileName(){
//        AlertDialog.Builder builder = new AlertDialog.Builder(context);
//        LayoutInflater inflater = getLayoutInflater(null);
//        View layout = inflater.inflate(R.layout.dialog_edit_text, null);
//        final EditText titleEditText = (EditText) layout.findViewById(R.id.titleEditText);
//        titleEditText.setText(R.string.untitled);
//        titleEditText.setSelection(0, titleEditText.length());
//
//        return builder.setTitle(context.getString(R.string.set_file_name)).setView(layout)
//                .setPositiveButton(context.getString(R.string.confirm), new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog1, int which) {
//                        String name1 = titleEditText.getText().toString().length() == 0 ? getString(R.string.untitled)
//                                : titleEditText.getText().toString();
//                        int i = 0;
//                        String suffix = "";
//                        while (new File(path, name1 + suffix + fileExtension).exists()) {
//                            i++;
//                            suffix = Integer.toString(i);
//                        }
//                        saveToFile(name1 + suffix + fileExtension);
//                    }
//                })
//                .setNegativeButton(context.getString(R.string.cancel), new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog1, int which) {
//                        Utils.displayToast(context, context.getString(R.string.canceled));
//                    }
//                }).create();
//    }

    private void readFiles(){
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED){
            if(fileObserver != null){
                fileObserver.stopWatching();
            }
            new GetFiles().execute();
        }
        else {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    Constants.WRITE_EXTERNAL_STORAGE_PERMISSION);
        }
    }

    private DialogInterface.OnClickListener getFileOverrideAction(final String name){
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                saveToFile(name);
            }
        };
    }
    private void saveToFile(String name){
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED)
            new SaveToFile().execute(name);
        else {
            this.name = name;
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    Constants.WRITE_EXTERNAL_STORAGE_PERMISSION);
        }
    }

    private class GetFiles extends AsyncTask<Void, Void, Boolean> {
        private ArrayList<String> dirs;
        private ArrayList<String> files;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if(!path.endsWith("/"))
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
                        public void onItemClick(boolean longClick, boolean isDir, String item) {
                            if(longClick){
                                Utils.copyToClipboard(context, path + item);
                                Utils.displayToast(context, getString(R.string.path_copied_to_clipboard));
                                return;
                            }
                            if (isDir) {
                                if (new File(path + item).canRead()) {
                                    path = path + item;
                                    readFiles();
                                } else
                                    Utils.displayToast(context, context.getString(R.string.you_are_not_allowed_to_view_this_folder));
                            } else {
                                if(fileExtension == null){ //file browser
                                    File file = new File(path + item);
                                    if(!item.contains(".")) {
                                        Utils.displayToast(context, getString(R.string.unknown_extension));
                                        return;
                                    }
                                    String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(item.substring(item.lastIndexOf(".")+1));

                                    Intent intent = new Intent();
                                    intent.setAction(android.content.Intent.ACTION_VIEW);
                                    intent.setDataAndType(Uri.fromFile(file), mime);
                                    try {
                                        startActivity(intent);
                                    } catch (ActivityNotFoundException e){
                                        Utils.displayToast(context, context.getString(R.string.unable_to_find_application_to_open_this_type_of_file));
                                    }
                                }
                                else if(fileExtension.equals(Constants.APK_FILE_EXTENSION)){ // view manifest xml
                                    Intent intent = new Intent(context, ManifestViewerActivity.class);
                                    intent.putExtra(Constants.APK_PATH,path + item + fileExtension);
                                    startActivity(intent);

//                                    Intent intent = new Intent(context, XmlViewerActivity.class);
//                                    intent.putExtra(Constants.SOURCE_DIR, );
//                                    startActivity(intent);
                                } else {
                                    Utils.getConfirmationDialog(context, context.getString(R.string.do_you_want_to_override_this_file),
                                            getFileOverrideAction(item + fileExtension)).show();
                                }
                            }
                        }
                    }));
                } else {
                    ((RecyclerViewAdapter)recyclerView.getAdapter()).changeData(dirs, dirsCount);
                }
                fileObserver = new FileObserver(path+"/") {
                    @Override
                    public void onEvent(int i, String s) {
                        if(isResumed()){
                            readFiles();
                            Utils.displayToast(context, "Files changed. Refreshing list");
                        } else {
                            refreshList = true;
                        }
                    }
                };
                fileObserver.startWatching();
            }
        }
    }

    private class SaveToFile extends AsyncTask<String, Void, Boolean>{
        @Override
        protected Boolean doInBackground(String... params) {
            File file = new File(path, params[0]);
            FileOutputStream f;

            if(arrayToSave != null) {
                try {
                    f = new FileOutputStream(file);
                    PrintWriter p = new PrintWriter(f);
                    for (String line : arrayToSave)
                        p.println(line);
                    p.flush();
                    p.close();
                    f.close();
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            } else { //file in tem dir
                InputStream in;
                OutputStream out;
                try {

                    in = new FileInputStream(new File(context.getCacheDir(),Constants.TEMP_FILE));
                    out = new FileOutputStream(file);

                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    in.close();

                    // write the output file
                    out.flush();
                    out.close();

                    // delete the original file
                    new File(context.getCacheDir(), Constants.TEMP_FILE).delete();

                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);

            if(aBoolean) {
                Utils.displayToast(context, context.getString(R.string.saving_to) + " " + fileExtension.substring(1) + " " + context.getString(R.string.file));
                ((AppCompatActivity)context).finish();
            }
            else
                Utils.displayToast(context, context.getString(R.string.failed));
        }
    }

    static class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>{
        private static RecyclerViewAdapter.OnItemClickListener listener;
        private static ArrayList<String> items;
        private static int dirsCount;

        interface OnItemClickListener{
            void onItemClick(boolean longClick, boolean isDir, String item);
        }

        RecyclerViewAdapter(ArrayList<String> items, int dirsCount, OnItemClickListener listener) {
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

        void changeData(ArrayList<String> items, int dirsCount){
            RecyclerViewAdapter.items = items;
            RecyclerViewAdapter.dirsCount = dirsCount;
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder{
            TextView titleTextView;
            ImageView icon;

            ViewHolder(final View itemView){
                super(itemView);
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(listener != null)
                            listener.onItemClick(false, getLayoutPosition()<dirsCount, items.get(getLayoutPosition()));
                    }
                });

                itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        if(listener != null)
                            listener.onItemClick(true, getLayoutPosition()<dirsCount, items.get(getLayoutPosition()));
                        return true;
                    }
                });

                titleTextView = (TextView) itemView.findViewById(R.id.textView2);
                icon = (ImageView) itemView.findViewById(R.id.imageView);

            }
        }
    }
}


