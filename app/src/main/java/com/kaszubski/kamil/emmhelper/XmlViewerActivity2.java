package com.kaszubski.kamil.emmhelper;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.kaszubski.kamil.emmhelper.utils.Constants;
import com.kaszubski.kamil.emmhelper.utils.Utils;

import net.dongliu.apk.parser.ApkFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class XmlViewerActivity2 extends AppCompatActivity {
    private static final String TAG = XmlViewerActivity2.class.getSimpleName();
    private TextView textView;
    private ProgressDialog progressDialog;
    private String manifest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xml_viewer);
        textView = (TextView) findViewById(R.id.textView9);

        new LoadManifest().execute();

//        Utils.decodeXmlFile(this, getIntent().getStringExtra(Constants.SOURCE_DIR), false, new Utils.OnDecodeFinishListener() {
//            @Override
//            public void onDecodeFinished(String xml, boolean launcherReady) {
//                if(xml != null)
//                    textView.setText(xml);
//                XmlViewerActivity2.this.manifest = xml;
//                progressDialog.dismiss();
//            }
//        });
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        getMenuInflater().inflate(R.menu.activity_xml_viewer, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if(manifest == null)
            manifest = textView.getText().toString().trim();
        final int selectionStart = textView.getSelectionStart();
        final int selectionEnd = textView.getSelectionEnd();

        switch (id){
            case R.id.action_export_to_txt:
                if(manifest.length() > 0){
                    if(selectionStart != selectionEnd){
                        getSelectionAlertDialog(item.getTitle().toString(), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                new SaveToFile().execute(i == 0? manifest.substring(selectionStart, selectionEnd) : manifest);
                            }
                        }).show();
                    } else
                        new SaveToFile().execute(manifest);
                } else {
                    Utils.displayToast(this, getString(R.string.manifest_is_empty));
                }
                break;
            case R.id.action_share:
                if(manifest.length() > 0) {
                    if(selectionStart != selectionEnd){
                        getSelectionAlertDialog(item.getTitle().toString(), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                try {
                                    Utils.shareViaList(XmlViewerActivity2.this, i == 0? manifest.substring(selectionStart, selectionEnd) : manifest);
                                } catch (RuntimeException e) {
                                    Utils.displayToast(XmlViewerActivity2.this, "Manifest is too big. Please export it to file");
                                }
                            }
                        }).show();
                    } else{
                        try {
                            Utils.shareViaList(this, manifest);
                        } catch (RuntimeException e){
                            Utils.displayToast(this, "Manifest is too big. Please export it to file");
                        }
                    }
                } else
                    Utils.displayToast(this, getString(R.string.manifest_file_is_empty));
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private Dialog getSelectionAlertDialog(String title, DialogInterface.OnClickListener action) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title).setItems(R.array.export, action);
        return builder.create();
    }

    private class SaveToFile extends AsyncTask<String, Void, Boolean>{
        @Override
        protected Boolean doInBackground(String... params) {
            File file = new File(XmlViewerActivity2.this.getCacheDir(), Constants.TEMP_FILE);
            FileOutputStream f;

            try {
                f = new FileOutputStream(file);
                PrintWriter p = new PrintWriter(f);
                String[] arrayToSave = params[0].split("\n");
                Log.e(TAG, "Lines " + arrayToSave.length);
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

        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);

            if(aBoolean) {
                Intent intent = new Intent(XmlViewerActivity2.this, ExportActivity.class);
                intent.putExtra(Constants.FILE_FORMAT_KEY, Constants.TXT_FILE_EXTENSION);
                startActivity(intent);
            }
            else
                Utils.displayToast(XmlViewerActivity2.this, XmlViewerActivity2.this.getString(R.string.failed));
        }
    }

    private class LoadManifest extends AsyncTask<Void, Void, String>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = new ProgressDialog(XmlViewerActivity2.this, ProgressDialog.STYLE_SPINNER);
            progressDialog.setMessage(getString(R.string.decoding_manifest_file));
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if(s != null)
                textView.setText(s);
            progressDialog.dismiss();
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                ApkFile apkFile = new ApkFile(new File(getIntent().getStringExtra(Constants.SOURCE_DIR)));
                String manifestXml = apkFile.getManifestXml();
                if(manifestXml != null) {
                    XmlViewerActivity2.this.manifest = manifestXml.trim();
                    return XmlViewerActivity2.this.manifest;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}



