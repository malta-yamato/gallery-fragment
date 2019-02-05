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
import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import jp.yamato.malta.android.galleryfragment.BottomSheetGalleryDialogFragment;
import jp.yamato.malta.android.galleryfragment.FormatterPickable;
import jp.yamato.malta.android.galleryfragment.GalleryFragmentDelegate;
import jp.yamato.malta.android.galleryfragment.GalleryFragmentParams;
import jp.yamato.malta.android.galleryfragment.ImageAdapter;

public class GalleryFragmentDemo_D extends AppCompatActivity
        implements ImageAdapter.LoadTask.BitmapLoader, ImageAdapter.OnItemClickListener,
        FormatterPickable {
    @SuppressWarnings("unused")
    private static final String TAG = "GalleryFragmentDemo_A";

    private static final int IMAGE_COUNT = 1000;
    private static final String CAMERA_DIRECTORY = "DCIM/Camera/";
    // The location may be different depending on the device.

    private BottomSheetGalleryDialogFragment mFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_gallery_fragment_demo_with_two_buttons);
        ((TextView) findViewById(R.id.button1)).setText("Clear External Cache!");
        ((TextView) findViewById(R.id.button2)).setText("Launch Bottom Sheet");

    }

    public void onButton2Click(View view) {
        mFragment = createFragment();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || checkPermission()) {
            setImages(mFragment);
        }
    }

    private BottomSheetGalleryDialogFragment createFragment() {
        BottomSheetGalleryDialogFragment fragment =
                CustomBottomSheetFragment.newInstance(R.layout.gallery_image_container_128);
        fragment.setStyle(0, R.style.AppThemeBottomSheetDialog); // this is quite important!
        fragment.setLayout(GalleryFragmentParams.LINEAR_LAYOUT_HORIZONTAL, 0);
        fragment.setTopResource(R.layout.simple_top_recyclerview);
        fragment.setEmptyResource(R.mipmap.ic_launcher_round);
        return fragment;
    }

    private void setImages(BottomSheetGalleryDialogFragment fragment) {
        // external storage dir
        File externalDir = Environment.getExternalStorageDirectory();
        if (externalDir == null) {
            Toast.makeText(this, "can't use storage directory.", Toast.LENGTH_SHORT).show();
            return;
        }

        // camera dir
        File cameraDir = new File(externalDir, CAMERA_DIRECTORY);
        if (!cameraDir.exists()) {
            Toast.makeText(this, "can't use camera directory.", Toast.LENGTH_SHORT).show();
            return;
        }

        // image files
        File[] files = cameraDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".jpg") || pathname.getName().endsWith(".jpeg");
            }
        });

        // make uri
        ArrayList<Uri> list = new ArrayList<>();
        for (File file : files) {
            // The file path or name will be hidden on FileProvider.
            Uri uri = FileProvider
                    .getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", file);
            if (uri != null) {
                list.add(uri);
            }
        }

        // check list size
        if (list.size() <= 0) {
            Toast.makeText(this, "no images", Toast.LENGTH_SHORT).show();
            return;
        }

        // set adapter
        fragment.setAdapterData(list);
        fragment.show(getSupportFragmentManager(), "dialog");
    }

    @Override
    public Bitmap loadBitmap(ContentResolver resolver, Uri uri) {
        int requestSquareSize = getResources().getDimensionPixelSize(R.dimen.size_128dp);
        Bitmap bitmap = DemoUtils.readBitmapFromCache(this, uri, requestSquareSize);
        if (bitmap == null) {
            bitmap = DemoUtils.createBitmapByDecodedStream(resolver, uri, requestSquareSize,
                    requestSquareSize, 0);
            DemoUtils.writeBitmapToCache(this, uri, bitmap, requestSquareSize);
        }

        return bitmap;
    }

    @Override
    public void onItemClick(View view, ImageAdapter adapter, int position) {
        DemoUtils.startActivity(this, adapter.getAdapterDataItem(position));
    }

    public void onButton1Click(View view) {
        DemoUtils.clearCache(this);
    }

    //
    // Custom Bottom Sheet Gallery Fragment
    //

    public static class CustomBottomSheetFragment extends BottomSheetGalleryDialogFragment
            implements ImageAdapter.OnItemLongClickListener {

        public static BottomSheetGalleryDialogFragment newInstance(int resource) {
            BottomSheetGalleryDialogFragment instance = new CustomBottomSheetFragment();
            GalleryFragmentDelegate.setArguments(instance, resource);
            return instance;
        }

        @Override
        public boolean onItemLongClick(View view, ImageAdapter adapter, int position) {
            removeFromAdapter(adapter.getAdapterDataItem(position));
            return true;
        }

    }

    //
    // Formatter
    //
    @Override
    public Map<String, ImageAdapter.Formatter> pickFormatter() {
        Map<String, ImageAdapter.Formatter> map = new HashMap<>();
        map.put(ImageAdapter.FILE_NAME, new ImageAdapter.Formatter() {
            @Override
            public String format(String str) {
                if (str != null) {
                    return "[" + str + "]";
                }
                return "no data";
            }
        });
        map.put(ImageAdapter.EXIF_MODEL, new ImageAdapter.Formatter() {
            @Override
            public String format(String str) {
                if (str != null) {
                    return "[" + str + "]";
                }
                return "no data";
            }
        });
        map.put(ImageAdapter.EXIF_DATETIME_ORIGINAL, new ImageAdapter.Formatter() {
            @Override
            public String format(String str) {
                if (str != null) {
                    String[] parts = str.split("\\s+");
                    if (parts.length > 0) {
                        return "[" + parts[0].replace(':', '/') + "]";
                    }
                }
                return "no data";
            }
        });
        return map;
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
