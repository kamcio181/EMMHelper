package com.kaszubski.kamil.emmhelper;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.kaszubski.kamil.emmhelper.utils.ExportableContent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class GetPropFragment extends Fragment implements ExportableContent{
    private static final String TAG = GetPropFragment.class.getSimpleName();
    private Context context;
    private TextView textView;
    private String properties;

    public GetPropFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        context = getActivity();
        ((AppCompatActivity)context).invalidateOptionsMenu();
        return inflater.inflate(R.layout.fragment_getprop, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((MainActivity)context).closeDrawer();
        textView = (TextView) view.findViewById(R.id.textView);

        BufferedReader bufferedReader = null;
        try
        {
            Process process;
            process = Runtime.getRuntime().exec("getprop");
            bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder builder = new StringBuilder();
            while((line=bufferedReader.readLine()) != null)
            {
                line = line.replaceFirst("\\[", "<b>").replaceFirst("]", "</b>").replaceFirst("\\[","").replaceFirst("]", "");
                builder.append(line).append("<br/>");
                Log.d(TAG, line);
            }
            process.waitFor();
            properties = builder.toString().substring(0, builder.length()-5);
            textView.setText(Html.fromHtml(properties));
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
        return null;
    }
}
