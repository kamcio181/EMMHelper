package com.kaszubski.kamil.emmhelper;

import android.app.ProgressDialog;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class XmlViewerActivity extends AppCompatActivity {
    private static final String TAG = "XMLViewerActivity";
    private TextView textView;
    private ProgressDialog progressDialog;
    private String manifest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xml_viewer);
        textView = (TextView) findViewById(R.id.textView9);

        progressDialog = new ProgressDialog(this, ProgressDialog.STYLE_SPINNER);
        progressDialog.setMessage(getString(R.string.decoding_manifest_file));
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.show();

        Utils.decodeXmlFile(this, getIntent().getStringExtra(Constants.SOURCE_DIR), false, new Utils.OnDecodeFinishListener() {
            @Override
            public void onDecodeFinished(String xml, boolean launcherReady) {
                if(xml != null)
                    textView.setText(xml);
                XmlViewerActivity.this.manifest = xml;
                progressDialog.dismiss();
            }
        });
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
        switch (id){
            case R.id.action_export_to_txt:
                if(manifest.length() > 0){
                    new SaveToFile().execute();
                } else {
                    Utils.showToast(this, getString(R.string.manifest_is_empty));
                }
                break;
            case R.id.action_share:
                if(manifest.length() > 0) {
                    try {
                        Utils.shareViaList(this, manifest);
                    } catch (RuntimeException e){
                        Utils.showToast(this, "Manifest is too big. Please export it to file");
                    }
                } else
                    Utils.showToast(this, getString(R.string.manifest_file_is_empty));
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private class SaveToFile extends AsyncTask<String, Void, Boolean>{
        @Override
        protected Boolean doInBackground(String... params) {
            File file = new File(XmlViewerActivity.this.getCacheDir(), Constants.TEMP_FILE);
            FileOutputStream f;

            try {
                f = new FileOutputStream(file);
                PrintWriter p = new PrintWriter(f);
                String[] arrayToSave = manifest.split("\n");
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
                Intent intent = new Intent(XmlViewerActivity.this, ExportActivity.class);
                intent.putExtra(Constants.FILE_FORMAT_KEY, Constants.TXT_FILE_EXTENSION);
                startActivity(intent);
            }
            else
                Utils.showToast(XmlViewerActivity.this, XmlViewerActivity.this.getString(R.string.failed));
        }
    }
}



