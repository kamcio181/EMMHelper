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

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class XmlViewerActivity extends AppCompatActivity {
    private static final String TAG = "XMLViewerActivity";
    private TextView textView;
    private String xml;
    private ProgressDialog progressDialog;

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
                XmlViewerActivity.this.xml = xml;
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

        switch (id){
            case R.id.action_export_to_txt:

                String manifest = textView.getText().toString().trim();

                if(manifest.length() > 0){
                    Intent intent = new Intent(this, ExportActivity.class);
                    intent.putExtra(Constants.STRING_KEY, manifest);
                    intent.putExtra(Constants.FILE_FORMAT_KEY, Constants.TXT_FILE_EXTENSION);
                    startActivity(intent);
                } else {
                    Utils.showToast(this, getString(R.string.manifest_is_empty));
                }
                break;
            case R.id.action_share:
                if(xml != null)
                    Utils.shareViaList(this, xml);
                 else
                    Utils.showToast(this, getString(R.string.manifest_file_is_empty));
                break;
        }

        return super.onOptionsItemSelected(item);
    }


}
