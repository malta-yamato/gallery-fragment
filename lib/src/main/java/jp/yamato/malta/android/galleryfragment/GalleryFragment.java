/*
 * Copyright (C) 2017 MALTA-YAMATO
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.yamato.malta.android.galleryfragment;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
public class GalleryFragment extends Fragment {
    @SuppressWarnings("unused")
    private static final String TAG = "GalleryFragment";

    GalleryFragmentDelegate mDelegate;

    public GalleryFragment() {
        mDelegate = new GalleryFragmentDelegate();
    }

    public static GalleryFragment newInstance(int resource) {
        GalleryFragment instance = new GalleryFragment();
        GalleryFragmentDelegate.setArguments(instance, resource);
        return instance;
    }

    public static GalleryFragment newInstance(int resource, int spanCount) {
        GalleryFragment instance = new GalleryFragment();
        GalleryFragmentDelegate.setArguments(instance, resource, spanCount);
        return instance;
    }

    public static GalleryFragment newInstance(int resource, int spanCount, ArrayList<Uri> data) {
        GalleryFragment instance = new GalleryFragment();
        GalleryFragmentDelegate.setArguments(instance, resource, spanCount, data);
        return instance;
    }

    public void setAdapterData(final ArrayList<Uri> data) {
        mDelegate.setAdapterData(data);
    }

    public void addToAdapter(Uri uri) {
        mDelegate.addToAdapter(uri);
    }

    public void addToAdapter(Uri uri, boolean scroll) {
        mDelegate.addToAdapter(uri, scroll);
    }

    public void insertToAdapter(int index, Uri uri) {
        mDelegate.insertToAdapter(index, uri);
    }

    public void insertToAdapter(int index, Uri uri, boolean scroll) {
        mDelegate.insertToAdapter(index, uri, scroll);
    }

    public void removeFromAdapter(Uri uri) {
        mDelegate.removeFromAdapter(uri);
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

    public void notifyDataSetChanged() {
        mDelegate.notifyDataSetChanged();
    }

    @Override
    public void onAttach(@NonNull Context context) {
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return mDelegate.onCreateView(inflater, container, savedInstanceState, getArguments());
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mDelegate.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDelegate.onDestroy();
    }

}
