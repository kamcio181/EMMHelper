package com.kaszubski.kamil.emmhelper;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.kaszubski.kamil.emmhelper.utils.Constants;
import com.kaszubski.kamil.emmhelper.utils.DividerItemDecoration;
import com.kaszubski.kamil.emmhelper.utils.Utils;

import java.util.ArrayList;
import java.util.List;


public class SearchFragment extends Fragment {
    private static final String TAG = "SearchFragment";
    private RecyclerView recyclerView;
    private Context context;
    private PackageInfo packageInfo;
    private ProgressBar progressBar;
    public SearchFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        context = getActivity();
        ((AppCompatActivity)context).invalidateOptionsMenu();

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        ((MainActivity)context).closeDrawer();
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public void setSearchResults(List<PackageInfo> results){
        if(results != null) {
            if (recyclerView.getAdapter() == null) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
                recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL_LIST));
                recyclerView.setAdapter(new SearchRecyclerAdapter(context, results, new SearchRecyclerAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, final PackageInfo packageInfo, boolean isLong) {
                        final String packageName = packageInfo.packageName;
                            AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            builder.setItems(context.getResources().getStringArray(R.array.package_menu),
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            switch (which) {
                                                case 0:
                                                    Utils.showAppInfo(context, packageName);
                                                    break;
                                                case 1:
                                                    //view manifest
                                                    Intent intent = new Intent(context, ManifestViewerActivity.class);
                                                    intent.putExtra(Constants.PACKAGE_INFO_KEY, packageName);
                                                    startActivity(intent);
                                                    break;
                                                case 2:
                                                    Utils.copyToClipboard(context, packageName);
                                                    break;
                                                case 3:
                                                    Utils.sharePackageName(context, packageName);
                                                    break;
                                                case 4:
                                                    if(ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                                            == PackageManager.PERMISSION_GRANTED){
                                                        Utils.extractAPK(context, packageInfo);
                                                    }
                                                    else {
                                                        SearchFragment.this.packageInfo = packageInfo;
                                                        ActivityCompat.requestPermissions((AppCompatActivity)context, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                                Constants.WRITE_PERMISSION);
                                                    }
                                                    break;
                                            }
                                        }
                                    }).create().show();
                    }
                }));
            } else {
                ((SearchRecyclerAdapter) recyclerView.getAdapter()).setResults(results);
            }
        }
        else
            recyclerView.setAdapter(null);
    }

    public void clearExportList(){
        ((SearchRecyclerAdapter) recyclerView.getAdapter()).clearExport();
    }

    public PackageInfo getPackageInfo() {
        return packageInfo;
    }

    public ArrayList<String> getExportList(){
        return SearchRecyclerAdapter.getExport();
    }

    public ProgressBar getProgressBar() {
        return progressBar;
    }
}
class SearchRecyclerAdapter extends RecyclerView.Adapter<SearchRecyclerAdapter.DoubleLineAvatarViewHolder> {
    private static OnItemClickListener listener;
    private static List<PackageInfo> packages;
    private static Context context;
    private static ArrayList<String> export = new ArrayList<>();

    public interface OnItemClickListener {
        void onItemClick(View view, PackageInfo packageInfo, boolean isLong);
    }

    public SearchRecyclerAdapter(Context context, List<PackageInfo> packages,
                                 OnItemClickListener listener) {
        SearchRecyclerAdapter.context = context;
        SearchRecyclerAdapter.packages = packages;
        SearchRecyclerAdapter.listener = listener;
        setHasStableIds(true);
    }

    public void setResults(List<PackageInfo> packages) {
        SearchRecyclerAdapter.packages = packages;
        notifyDataSetChanged();
    }

    @Override
    public DoubleLineAvatarViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new DoubleLineAvatarViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.double_line_avatar_recycler_view_item, parent, false));
    }

    @Override
    public void onBindViewHolder(DoubleLineAvatarViewHolder holder, int position) {
        holder.avatarImageView.setImageDrawable(packages.get(position).applicationInfo.loadIcon(context.getPackageManager()));
        holder.titleTextView.setText(String.valueOf(packages.get(position).applicationInfo.loadLabel(context.getPackageManager())));
        holder.subtitleTextView.setText(packages.get(position).packageName);
        if(export.contains(packages.get(position).packageName))
            holder.itemView.setBackgroundColor(Color.LTGRAY);
        else
            holder.itemView.setBackgroundColor(Color.WHITE);
    }

    @Override
    public int getItemCount() {
        return packages.size();
    }

    static class DoubleLineAvatarViewHolder extends RecyclerView.ViewHolder {
        public TextView titleTextView, subtitleTextView;
        public ImageView avatarImageView;
        public View itemView;

        public DoubleLineAvatarViewHolder(final View itemView) {
            super(itemView);
            this.itemView = itemView;
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String packageName = packages.get(getLayoutPosition()).packageName;
                    if(export.contains(packageName)){
                        export.remove(packageName);
                        itemView.setBackgroundColor(Color.WHITE);
                        Utils.showToast(context, "Package name removed to export list");
                    } else {
                        export.add(packageName);
                        itemView.setBackgroundColor(Color.LTGRAY);
                        Utils.showToast(context, "Package name added to export list");
                    }
                }
            });
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if(listener != null)
                        listener.onItemClick(itemView, packages.get(getLayoutPosition()), true);
                    return true;
                }
            });

            avatarImageView = (ImageView) itemView.findViewById(R.id.imageView2);
            titleTextView = (TextView) itemView.findViewById(R.id.textView2);
            subtitleTextView = (TextView) itemView.findViewById(R.id.textView3);
        }
    }

    @Override
    public long getItemId(int position) {
        return packages.get(position).applicationInfo.uid;
    }

    public static ArrayList<String> getExport() {
        return export;
    }

    public void clearExport() {
        SearchRecyclerAdapter.export = new ArrayList<>();
        notifyDataSetChanged();
    }
}
