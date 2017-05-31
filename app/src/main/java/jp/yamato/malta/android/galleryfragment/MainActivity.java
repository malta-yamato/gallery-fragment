package jp.yamato.malta.android.galleryfragment;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
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
import android.view.View;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.support.media.ExifInterface;

public class MainActivity extends AppCompatActivity
        implements ExceptionHandler.Callback, ImageAdapter.OnItemClickListener,
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
//            mFragment = GalleryFragment.newInstance(R.layout.simple_selectable_image_container, 2);
            mFragment = GalleryFragment
                    .newInstance(R.layout.simple_selectable_image_container_debug, 2);
//            mFragment = GalleryFragment
//                    .newInstance(R.layout.simple_selectable_image_container_debug_2, 2);
//            mFragment = GalleryFragment
//                    .newInstance(R.layout.simple_selectable_image_container_debug_3, 2);
//            mFragment = GalleryFragment.newInstance(R.layout.simple_selectable_image_item);
            getSupportFragmentManager().beginTransaction().replace(R.id.container, mFragment)
                    .commit();
        } else {
            mFragment =
                    (GalleryFragment) getSupportFragmentManager().findFragmentById(R.id.container);
        }

        // BitmapLoader setting
        mFragment.setBitmapLoader(new ImageAdapter.LoadTask.BitmapLoader() {
            @Override
            public Bitmap loadBitmap(Uri uri) {
                long id = Long.valueOf(uri.getLastPathSegment());
                return MediaStore.Images.Thumbnails.getThumbnail(getContentResolver(), id,
                        MediaStore.Images.Thumbnails.MINI_KIND, null);
            }
        });

        // Formatter setting
        mFragment.setFormatter(ImageAdapter.IMAGE_DATE_TAKEN, new ImageAdapter.Formatter() {
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
        mFragment.setFormatter(ImageAdapter.EXIF_MODEL, new ImageAdapter.Formatter() {
            @Override
            public String format(String str) {
                if (str != null) {
                    return "[" + str + "]";
                }
                return "";
            }
        });

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
