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

package jp.yamato.malta.android.galleryfragment.demo;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;

import jp.yamato.malta.android.galleryfragment.GalleryFragment;
import jp.yamato.malta.android.galleryfragment.GalleryFragmentParams;
import jp.yamato.malta.android.galleryfragment.ImageAdapter;

public class GalleryFragmentDemo_A extends AppCompatActivity
        implements ImageAdapter.LoadTask.BitmapLoader, ImageAdapter.OnItemClickListener {
    @SuppressWarnings("unused")
    private static final String TAG = "GalleryFragmentDemo_A";

    private static final int IMAGE_COUNT = 10;

    private GalleryFragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_gallery_fragment_demo);

        // fragment
        if (savedInstanceState == null) {
            // create fragment
            mFragment = createFragment();

            // commit
            getSupportFragmentManager().beginTransaction().replace(R.id.container, mFragment)
                    .commit();

            // set images
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || checkPermission()) {
                setImages(mFragment);
            }
        } else {
            mFragment =
                    (GalleryFragment) getSupportFragmentManager().findFragmentById(R.id.container);
        }

    }

    private GalleryFragment createFragment() {
        GalleryFragment fragment = GalleryFragment.newInstance(
                R.layout.jp_yamato_malta_gallery_fragment_simple_selectable_image_item);
        fragment.setLayout(GalleryFragmentParams.LINEAR_LAYOUT_HORIZONTAL, 0);
        return fragment;
    }

    private void setImages(GalleryFragment fragment) {
        fragment.setAdapterData(DemoUtils.getSampleUriFromExternalContent(this, IMAGE_COUNT));
    }

    @Override
    public Bitmap loadBitmap(ContentResolver resolver, Uri uri) {
        long id = Long.valueOf(uri.getLastPathSegment());
        return MediaStore.Images.Thumbnails
                .getThumbnail(getContentResolver(), id, MediaStore.Images.Thumbnails.MINI_KIND,
                        null);
    }

    @Override
    public void onItemClick(View view, ImageAdapter adapter, int position) {
        DemoUtils.startActivity(this, adapter.getAdapterDataItem(position));
    }

    //
    // Runtime Permission
    //

    private static final int PERMISSIONS_REQUEST_ID = 100;

    @TargetApi(Build.VERSION_CODES.M)
    private boolean checkPermission() {
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_ID);
            return false;
        }
        return true;
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        if (requestCode == PERMISSIONS_REQUEST_ID &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setImages(mFragment);
        }
    }

}
