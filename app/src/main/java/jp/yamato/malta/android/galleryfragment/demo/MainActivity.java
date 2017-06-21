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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.support.media.ExifInterface;

import jp.yamato.malta.android.galleryfragment.BottomSheetGalleryDialogFragment;
import jp.yamato.malta.android.galleryfragment.FormatterPickable;
import jp.yamato.malta.android.galleryfragment.GalleryFragment;
import jp.yamato.malta.android.galleryfragment.GalleryFragmentDelegate;
import jp.yamato.malta.android.galleryfragment.GalleryFragmentParams;
import jp.yamato.malta.android.galleryfragment.ImageAdapter;

public class MainActivity extends AppCompatActivity
        implements ExceptionHandler.Callback, ImageAdapter.LoadTask.BitmapLoader,
        ImageAdapter.OnItemClickListener, ImageAdapter.OnItemLongClickListener, FormatterPickable,
        LoaderManager.LoaderCallbacks<Cursor> {
    @SuppressWarnings("unused")
    private static final String TAG = "MainActivity";

    private GalleryFragment mFragment;

    private File mTraceFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // uncaught exception handler (this work will cause some memory leak)
//        ExceptionHandler exceptionHandler = new ExceptionHandler(this);
//        Thread.setDefaultUncaughtExceptionHandler(exceptionHandler);

        setContentView(R.layout.activity_main);

        // fragment
        if (savedInstanceState == null) {
            mFragment = GalleryFragment.newInstance(
                    R.layout.jp_yamato_malta_gallery_fragment_simple_selectable_image_container);
            getSupportFragmentManager().beginTransaction().replace(R.id.container, mFragment)
                    .commit();
        } else {
            mFragment =
                    (GalleryFragment) getSupportFragmentManager().findFragmentById(R.id.container);
        }

        mFragment.setTopResource(R.layout.simple_recyclerview_debug);
        mFragment.setEmptyResource(R.mipmap.ic_launcher_round);
        mFragment.setMaxTaskCount(128);
        mFragment.setLayout(GalleryFragmentParams.LINEAR_LAYOUT_HORIZONTAL, 0);

        //
        // check permission
        //
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || checkPermission()) {
            initLoader();
            prepareFiles();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCanChangeAdapter = true;
    }

    private void initLoader() {
        getSupportLoaderManager().initLoader(0, null, this);
    }

    private void prepareFiles() {
        File dir = Environment.getExternalStorageDirectory();
        if (dir == null) {
            return;
        }
        File appDir = new File(dir, BuildConfig.APPLICATION_ID);
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        mTraceFile = new File(appDir, "stack-trace.txt");
//        Log.d(TAG, "trace file = " + mTraceFile.getPath());
    }

    @Override
    public Bitmap loadBitmap(ContentResolver resolver, Uri uri) {
        long id = Long.valueOf(uri.getLastPathSegment());
        return MediaStore.Images.Thumbnails
                .getThumbnail(getContentResolver(), id, MediaStore.Images.Thumbnails.MINI_KIND,
                        null);

//        int requestSquareSize = getResources().getDimensionPixelSize(R.dimen.size_128dp);
//        return createBitmapByDecodedStream(resolver, uri, requestSquareSize, requestSquareSize, 0);
    }

    @Override
    public Map<String, ImageAdapter.Formatter> pickFormatter() {
        Map<String, ImageAdapter.Formatter> map = new HashMap<>();
        map.put(ImageAdapter.IMAGE_DATE_TAKEN, new ImageAdapter.Formatter() {
            @Override
            public String format(String str) {
                try {
                    long time = Long.valueOf(str);
                    return SimpleDateFormat.getDateTimeInstance().format(new Date(time));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                return "";
            }
        });
        map.put(ImageAdapter.EXIF_MODEL, new ImageAdapter.Formatter() {
            @Override
            public String format(String str) {
                if (str != null) {
                    return "[" + str + "]";
                }
                return "";
            }
        });
        return map;
    }

    @Override
    public void onItemClick(View view, ImageAdapter adapter, int position) {
        Uri uri = adapter.getAdapterDataItem(position);
        if (uri == null) {
            return;
        }

        InputStream in = null;
        try {
            in = getContentResolver().openInputStream(uri);
            if (in == null) {
                return;
            }

            ExifInterface exifInterface = new ExifInterface(in);
            String model = exifInterface.getAttribute(ExifInterface.TAG_MODEL);
            String dateTimeOriginal =
                    exifInterface.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL);

            Toast.makeText(this, model + ", " + dateTimeOriginal, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public boolean onItemLongClick(View view, ImageAdapter imageAdapter, int position) {
        Uri uri = imageAdapter.getAdapterDataItem(position);
        if (uri != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "image/*");
            startActivity(intent);
            return true;
        }
        return false;
    }

    private int mResourceIndex = 0;
    private int[] mResources =
            new int[]{R.layout.jp_yamato_malta_gallery_fragment_simple_selectable_image_container,
                    R.layout.jp_yamato_malta_gallery_fragment_simple_selectable_image_item};

    public void onChangeResourceButtonClick(View view) {
        mFragment.setResource(mResources[mResourceIndex]);
        mResourceIndex = (mResourceIndex + 1) % mResources.length;
    }

    private int mCurrentLayoutMode = 0;

    public void onChangeLayoutButtonClick(View view) {
        mCurrentLayoutMode = (mCurrentLayoutMode + 1) % 5;
        switch (mCurrentLayoutMode) {
            case 0:
                mFragment.setLayout(GalleryFragmentParams.GRID_LAYOUT, 2);
                break;
            case 1:
                mFragment.setLayout(GalleryFragmentParams.GRID_LAYOUT, 3);
                break;
            case 2:
                mFragment.setLayout(GalleryFragmentParams.GRID_LAYOUT, 4);
                break;
            case 3:
                mFragment.setLayout(GalleryFragmentParams.LINEAR_LAYOUT_HORIZONTAL, 0);
                break;
            case 4:
                mFragment.setLayout(GalleryFragmentParams.LINEAR_LAYOUT_VERTICAL, 0);
                break;
        }
    }

    public void onDialogButtonClick(View view) {
        ArrayList<Uri> list = new ArrayList<>();

        Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media.DATA}, null, null,
                MediaStore.Images.Media.DATE_TAKEN + " DESC");
        if (cursor == null) {
            return;
        }

        for (int i = 0; i < 10; i++) {
            if (cursor.moveToNext()) {
                String data = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                list.add(Uri.fromFile(new File(data)));
            }
        }
        cursor.close();

        CustomBottomSheetGalleryDialogFragment fragment =
                new CustomBottomSheetGalleryDialogFragment();
        fragment.setResource(R.layout.gallery_image_container_256);
        fragment.setAdapterData(list);
        fragment.setLayout(GalleryFragmentParams.LINEAR_LAYOUT_HORIZONTAL, 0);
        fragment.show(getSupportFragmentManager(), "dialog");
    }

    //
    // Custom
    //
    public static class CustomBottomSheetGalleryDialogFragment
            extends BottomSheetGalleryDialogFragment
            implements ImageAdapter.LoadTask.BitmapLoader, FormatterPickable {

        @Override
        public Bitmap loadBitmap(ContentResolver resolver, Uri uri) {
            int requestSquareSize = getResources().getDimensionPixelSize(R.dimen.size_256dp);
            return createBitmapByDecodedStream(resolver, uri, requestSquareSize, requestSquareSize,
                    0);
        }

        @Override
        public Map<String, ImageAdapter.Formatter> pickFormatter() {
            Map<String, ImageAdapter.Formatter> map = new HashMap<>();
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
                            return parts[0].replace(':', '/');
                        }
                    }
                    return "no data";
                }
            });
            return map;
        }
    }

    private static final int IMAGE_BUF_SIZE = 8 * 1048576;

    private static Bitmap createBitmapByDecodedStream(ContentResolver resolver, Uri uri,
            int destWidth, int destHeight, int mode) {
        Bitmap bitmap = null;
        BufferedInputStream stream = null;
        try {
            // create buffered stream
            InputStream in = resolver.openInputStream(uri);
            if (in == null) {
                return null;
            }
            stream = new BufferedInputStream(in, IMAGE_BUF_SIZE);

            // check size
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(stream, null, options);
            stream.close();
            int orgWidth = options.outWidth;
            int orgHeight = options.outHeight;
            Log.v(TAG, "Original Image Size: " + orgWidth + " x " + orgHeight);

            // resolve inSampleSize
            int widthRatio;
            int heightRatio;
            int inSampleSize;
            if (mode == 0) {
//                widthRatio = (int) ((float) orgWidth / (float) destWidth);
//                heightRatio = (int) ((float) orgHeight / (float) destHeight);
//                inSampleSize = Math.min(widthRatio, heightRatio);

                inSampleSize =
                        getLeastRegulatedSampleSize(orgWidth, orgHeight, destWidth, destHeight);

                Log.v(TAG, "inSampleSize: " + inSampleSize);
            } else {
                inSampleSize =
                        getMostRegulatedSampleSize(orgWidth, orgHeight, destWidth, destHeight);
                Log.v(TAG, "inSampleSize: " + inSampleSize);
            }
            // create buffered stream
            in = resolver.openInputStream(uri);
            if (in == null) {
                return null;
            }
            stream = new BufferedInputStream(in, IMAGE_BUF_SIZE);

            // load bitmap
            options = new BitmapFactory.Options();
            options.inSampleSize = inSampleSize;
            options.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap decodedBitmap = BitmapFactory.decodeStream(stream, null, options);
            stream.close();
            int decodedWidth = decodedBitmap.getWidth();
            int decodedHeight = decodedBitmap.getHeight();
            Log.v(TAG, "Decoded Image Size: " + decodedWidth + " x " + decodedHeight);

            // clipping
            int clippingX = (decodedWidth - destWidth) / 2;
            int clippingY = (decodedHeight - destHeight) / 2;
            if (clippingX >= 0 && clippingY >= 0) {
                Log.v(TAG, "!!!---Case A---!!!");
                bitmap = Bitmap.createBitmap(decodedBitmap, clippingX, clippingY, destWidth,
                        destHeight);
                decodedBitmap.recycle();
            } else if (clippingX < 0 && clippingY < 0) {
                Log.v(TAG, "!!!---Case B---!!!");
                int offsetX = -1 * clippingX;
                int offsetY = -1 * clippingY;
                Bitmap baseBitmap =
                        Bitmap.createBitmap(destWidth, destHeight, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(baseBitmap);
                canvas.drawBitmap(decodedBitmap, offsetX, offsetY, null);
                bitmap = baseBitmap;
                decodedBitmap.recycle();
            } else if (clippingX < 0) {
                Log.v(TAG, "!!!---Case C---!!!");
                int offsetX = -1 * clippingX;
                Bitmap baseBitmap =
                        Bitmap.createBitmap(destWidth, destHeight, Bitmap.Config.ARGB_8888);
                Bitmap clippedBitmap =
                        Bitmap.createBitmap(decodedBitmap, 0, clippingY, decodedWidth, destHeight);
                Canvas canvas = new Canvas(baseBitmap);
                canvas.drawBitmap(clippedBitmap, offsetX, 0, null);
                bitmap = baseBitmap;
                decodedBitmap.recycle();
                clippedBitmap.recycle();
            } else {
                Log.v(TAG, "!!!---Case D---!!!");
                int offsetY = -1 * clippingY;
                Bitmap baseBitmap =
                        Bitmap.createBitmap(destWidth, destHeight, Bitmap.Config.ARGB_8888);
                Bitmap clippedBitmap =
                        Bitmap.createBitmap(decodedBitmap, clippingX, 0, destWidth, decodedHeight);
                Canvas canvas = new Canvas(baseBitmap);
                canvas.drawBitmap(clippedBitmap, 0, offsetY, null);
                bitmap = baseBitmap;
                decodedBitmap.recycle();
                clippedBitmap.recycle();
            }

            int lastWidth = bitmap.getWidth();
            int lastHeight = bitmap.getHeight();
            Log.v(TAG, "Last Image Size: " + lastWidth + " x " + lastHeight);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return bitmap;
    }

    public static int getMostRegulatedSampleSize(int width, int height, int tgtWidth,
            int tgtHeight) {
        int sampleSize;
        for (sampleSize = 1; sampleSize <= 16; sampleSize++) {
            if (width % sampleSize == 0 && height % sampleSize == 0) {
                if (width / sampleSize <= tgtWidth && height / sampleSize <= tgtHeight) {
                    break;
                }
            }
        }
        return sampleSize;
    }

    public static int getLeastRegulatedSampleSize(int width, int height, int tgtWidth,
            int tgtHeight) {
        int sampleSize;
        for (sampleSize = 16; sampleSize >= 1; sampleSize--) {
            if (width % sampleSize == 0 && height % sampleSize == 0) {
                if (width / sampleSize >= tgtWidth && height / sampleSize >= tgtHeight) {
                    break;
                }
            }
        }
        return sampleSize;
    }

    //
    // Loader
    //

    private boolean mCanChangeAdapter = false;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID}, null, null,
                MediaStore.Images.Media.DATE_TAKEN + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null) {
            return;
        }

        if (mFragment != null && mCanChangeAdapter) {
//            Log.d(TAG, "swap adapter");
            ArrayList<Uri> data = new ArrayList<>();
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToPosition(i);
                long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media._ID));
                Uri uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        String.valueOf(id));
                data.add(uri);
            }
            mFragment.setAdapterData(data);
            mCanChangeAdapter = false;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mFragment.setAdapterData(null);
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
            initLoader();
            prepareFiles();
        }
    }

    //
    // Uncaught Exception (for debug, memory leak will occur if use)
    //

    @Override
    public void onUncaughtExceptionThrown(Throwable ex) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new BufferedWriter(new FileWriter(mTraceFile)));
            ex.printStackTrace(writer);
            if (writer.checkError()) {
                throw new IOException();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

}
