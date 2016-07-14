package com.kaszubski.kamil.emmhelper;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.kaszubski.kamil.emmhelper.utils.Constants;
import com.kaszubski.kamil.emmhelper.utils.Utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;

public class IPFinderFragment extends Fragment implements View.OnClickListener{
    private static final String TAG = "IPFinderFragment";
    private ProgressBar progressBar;
    private EditText editText;
    private TextView textView, textView2;
    private Button button;
    private AsyncTask listAsync;
    private Context context;

    public IPFinderFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        context = getActivity();
        ((AppCompatActivity)context).invalidateOptionsMenu();
        return inflater.inflate(R.layout.fragment_ipfinder, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        editText = (EditText) view.findViewById(R.id.editText);
        textView = (TextView) view.findViewById(R.id.textView10);
        textView2 = (TextView) view.findViewById(R.id.textView11);
        button = (Button) view.findViewById(R.id.button);
        button.setOnClickListener(this);

        ((MainActivity)context).closeDrawer();

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.button:
                if(ContextCompat.checkSelfPermission(context, Manifest.permission.INTERNET)
                        == PackageManager.PERMISSION_GRANTED)
                    searchIPs();
                else {
                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            Constants.INTERNET_PERMISSION);
                }
                break;
        }
    }

    public ArrayList<String> getExportList(){
        ArrayList<String> list = new ArrayList<>();
        if(textView.getText().toString().trim().length() <= 0)
            return null;

        list.addAll(Arrays.asList(textView.getText().toString().split("\n")));
        return list;
    }

    public void searchIPs(){
        if(editText.getText().toString().trim().length()>0) {
            if (listAsync != null && listAsync.getStatus() != AsyncTask.Status.FINISHED) {
                listAsync.cancel(true);
                progressBar.setVisibility(View.INVISIBLE);
            }
            listAsync = new ListSync().execute(editText.getText().toString().trim());
        } else {
            Utils.showToast(context, context.getString(R.string.host_name_is_empty));
        }
    }

    class ListSync extends AsyncTask<String, String, Boolean>{
        String ipList = "";
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressBar.setVisibility(View.VISIBLE);
            textView.setText("");
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            progressBar.setVisibility(View.INVISIBLE);
            if(aBoolean){
                textView.setText(ipList.trim());
            } else {
                textView.setText("");
                textView2.setVisibility(View.INVISIBLE);
                Utils.showToast(getContext(), context.getString(R.string.error_occurred_check_host_name_and_internet_connection));
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            ipList = ipList +values[0] + "\n";
            textView.setText(ipList);
            if(!textView2.isShown())
                textView2.setVisibility(View.VISIBLE);
        }

        @Override
        protected Boolean doInBackground(String... params) {

            try {
                for (InetAddress addr : InetAddress.getAllByName(params[0]))
                    publishProgress(addr.getHostAddress());
            } catch (UnknownHostException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
    }
}
