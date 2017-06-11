package jp.yamato.malta.android.galleryfragment;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

/**
 * Created by malta on 2017/05/02.
 * GalleryFragment
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
public class GalleryDialogFragment extends DialogFragment {
    @SuppressWarnings("unused")
    private static final String TAG = "GalleryDialogFragment";

    GalleryFragmentDelegate mDelegate;

    public GalleryDialogFragment() {
        mDelegate = new GalleryFragmentDelegate();
    }

    public static GalleryDialogFragment newInstance(int resource) {
        GalleryDialogFragment instance = new GalleryDialogFragment();
        GalleryFragmentDelegate.setArguments(instance, resource);
        return instance;
    }

    public static GalleryDialogFragment newInstance(int resource, int spanCount) {
        GalleryDialogFragment instance = new GalleryDialogFragment();
        GalleryFragmentDelegate.setArguments(instance, resource, spanCount);
        return instance;
    }

    public static GalleryDialogFragment newInstance(int resource, int spanCount,
            ArrayList<Uri> data) {
        GalleryDialogFragment instance = new GalleryDialogFragment();
        GalleryFragmentDelegate.setArguments(instance, resource, spanCount, data);
        return instance;
    }

    public void setAdapterData(final ArrayList<Uri> data) {
        mDelegate.setAdapterData(data);
    }

    public ArrayList<Uri> getAdapterData() {
        return mDelegate.getAdapterData();
    }

    public Uri getAdapterDataItem(int position) {
        return mDelegate.getAdapterDataItem(position);
    }

    public void setTopResource(int topResource) {
        mDelegate.setTopResource(topResource);
    }

    public void setResource(int resource) {
        mDelegate.setResource(resource);
    }

    public void setEmptyResource(int resource) {
        mDelegate.setEmptyResource(resource);
    }

    public void setMaxTaskCount(int maxTaskCount) {
        mDelegate.setMaxTaskCount(maxTaskCount);
    }

    public void setLayout(int layout, int spanCount) {
        mDelegate.setLayout(layout, spanCount);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mDelegate.onAttach(context, this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mDelegate.onDetach();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return mDelegate.onCreateView(inflater, container, savedInstanceState, getArguments());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mDelegate.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDelegate.onDestroy();
    }

}
