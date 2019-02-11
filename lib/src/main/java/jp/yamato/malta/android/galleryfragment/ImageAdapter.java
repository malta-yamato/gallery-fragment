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

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.exifinterface.media.ExifInterface;
import androidx.recyclerview.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {
    @SuppressWarnings("unused")
    private static final String TAG = "ImageAdapter";

    // file
    public static final String FILE_NAME = "file_name";
    public static final String FILE_PATH = "file_path";
    public static final String FILE_SIZE = "file_size";

    // media provider
    public static final String IMAGE_DATA = "image_data";
    public static final String IMAGE_DISPLAY_NAME = "image_display_name";
    public static final String IMAGE_DATE_TAKEN = "image_date_taken";
    public static final String IMAGE_SIZE = "image_size";

    // exif
    public static final String EXIF_MODEL = "exif_model";
    public static final String EXIF_DATETIME_ORIGINAL = "exif_datetime_original";
    public static final String EXIF_IMAGE_WIDTH = "exif_image_width";
    public static final String EXIF_IMAGE_LENGTH = "exif_image_length";
    public static final String EXIF_GPS_LATITUDE = "exif_gps_latitude";
    public static final String EXIF_GPS_LONGITUDE = "exif_gps_longitude";
    public static final String EXIF_GPS_ALTITUDE = "exif_gps_altitude";

    private static final Map<String, String> sImageColumnsMap;
    private static final Map<String, String> sExifRealTagsMap;

    static {
        // image
        sImageColumnsMap = new HashMap<>();
        sImageColumnsMap.put(IMAGE_DATA, MediaStore.Images.Media.DATA);
        sImageColumnsMap.put(IMAGE_DISPLAY_NAME, MediaStore.Images.Media.DISPLAY_NAME);
        sImageColumnsMap.put(IMAGE_DATE_TAKEN, MediaStore.Images.Media.DATE_TAKEN);
        sImageColumnsMap.put(IMAGE_SIZE, MediaStore.Images.Media.SIZE);

        //exif
        sExifRealTagsMap = new HashMap<>();
        sExifRealTagsMap.put(EXIF_MODEL, ExifInterface.TAG_MODEL);
        sExifRealTagsMap.put(EXIF_DATETIME_ORIGINAL, ExifInterface.TAG_DATETIME_ORIGINAL);
        sExifRealTagsMap.put(EXIF_IMAGE_WIDTH, ExifInterface.TAG_IMAGE_WIDTH);
        sExifRealTagsMap.put(EXIF_IMAGE_LENGTH, ExifInterface.TAG_IMAGE_LENGTH);
        sExifRealTagsMap.put(EXIF_GPS_LATITUDE, ExifInterface.TAG_GPS_LATITUDE);
        sExifRealTagsMap.put(EXIF_GPS_LONGITUDE, ExifInterface.TAG_GPS_LONGITUDE);
        sExifRealTagsMap.put(EXIF_GPS_ALTITUDE, ExifInterface.TAG_GPS_ALTITUDE);
    }

    private Handler mHandler;

    private Context mContext;
    private int mResource;
    private int mEmptyResource = 0;
    private ArrayList<Uri> mAdapterData;
    private OnItemClickListener mOnItemClickListener;
    private OnItemLongClickListener mOnItemLongClickListener;

    private final BitmapCache<Integer> mBitmaps = new BitmapCache<>();
    @SuppressLint("UseSparseArrays")
    private final SparseArray<Integer> mBitmapOrientationMap = new SparseArray<>();

    private boolean mIsInfoSetup;
    private String[] mFileTags;
    private String[] mImageTags;
    private String[] mExifTags;
    private final SparseArray<String[]> mFileInfoMap = new SparseArray<>();
    private final SparseArray<String[]> mImageInfoMap = new SparseArray<>();
    private final SparseArray<String[]> mExifInfoMap = new SparseArray<>();

    private int mMaxTaskCount = LoadTask.MAX_TASK_COUNT;
    private LoadTask.BitmapLoader mBitmapLoader = null;

    private Map<String, Formatter> mFormatter = new HashMap<>();

    //
    //
    //

    public ImageAdapter(Context context, int resource, ArrayList<Uri> data,
            OnItemClickListener onItemClickListener,
            OnItemLongClickListener onItemLongClickListener) {
        mContext = context;
        mResource = resource;
        mAdapterData = data;
        mOnItemClickListener = onItemClickListener;
        mOnItemLongClickListener = onItemLongClickListener;

        mHandler = new Handler();
    }

    public void setEmptyResource(int emptyResource) {
        mEmptyResource = emptyResource;
    }

    public void setMaxTaskCount(int maxTaskCount) {
        mMaxTaskCount = maxTaskCount;
    }

    public void setAdapterData(ArrayList<Uri> data) {
        mAdapterData = data;

        clearCache();
        cancelAllTasks();
    }

    public void add(Uri uri) {
        mAdapterData.add(uri);

        if (LoadTask.hasTasks()) {
            cancelAllTasks();
        } else {
            notifyItemInserted(mAdapterData.size() - 1);
        }
    }

    public void insert(Uri uri, int index) {
        mAdapterData.add(index, uri);
        for (int pos = mAdapterData.size() - 1; pos > index; pos--) {
            moveCache(pos - 1, pos);
        }

        if (LoadTask.hasTasks()) {
            cancelAllTasks();
        } else {
            notifyItemInserted(index);
        }
    }

    public boolean remove(Uri uri) {
        int index = mAdapterData.indexOf(uri);
        if (index >= 0) {
            if (index == mAdapterData.size() - 1) {
                mAdapterData.remove(index);
                removeCache(index);
            } else {
                mAdapterData.remove(index);
                for (int pos = index; pos < mAdapterData.size(); pos++) {
                    moveCache(pos + 1, pos);
                }
            }

            if (LoadTask.hasTasks()) {
                cancelAllTasks();
            } else {
                notifyItemRemoved(index);
            }

            return true;
        }

        return false;
    }

    private void cancelAllTasks() {
        LoadTask.cancelAll();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });
    }

    public ArrayList<Uri> getAdapterData() {
        return mAdapterData;
    }

    public Uri getAdapterDataItem(int position) {
        return mAdapterData.get(position);
    }

    public void setBitmapLoader(LoadTask.BitmapLoader loader) {
        mBitmapLoader = loader;
    }

    public void addFormatter(String tag, Formatter formatter) {
        mFormatter.put(tag, formatter);
    }

    public void swapFormatter(Map<String, Formatter> formatter) {
        if (formatter == null) {
            throw new IllegalArgumentException("formatter is null");
        }
        mFormatter = formatter;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//            Log.d(TAG, "onCreateViewHolder");

        View view = LayoutInflater.from(mContext).inflate(mResource, parent, false);
        if (!mIsInfoSetup) {
            setupInfo(view);
            mIsInfoSetup = true;
        }
        final ViewHolder holder = new ViewHolder(view, mFileTags, mImageTags, mExifTags);

        // item click mOnItemClickListener
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener
                            .onItemClick(v, ImageAdapter.this, holder.getAdapterPosition());
                }
            }
        });

        // item click mOnItemClickListener
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return mOnItemLongClickListener != null && mOnItemLongClickListener
                        .onItemLongClick(v, ImageAdapter.this, holder.getAdapterPosition());
            }
        });

        return holder;
    }

    private void setupInfo(View view) {

        //
        // File Tags
        //
        ArrayList<String> fileTagList = new ArrayList<>();
        addTagListWithViewTag(fileTagList, view, FILE_NAME);
        addTagListWithViewTag(fileTagList, view, FILE_PATH);
        addTagListWithViewTag(fileTagList, view, FILE_SIZE);
        //noinspection ToArrayCallWithZeroLengthArrayArgument
        mFileTags = fileTagList.toArray(new String[fileTagList.size()]);

        //
        // Image Tags
        //
        ArrayList<String> imageTagList = new ArrayList<>();
        addTagListWithViewTag(imageTagList, view, IMAGE_DATA);
        addTagListWithViewTag(imageTagList, view, IMAGE_DISPLAY_NAME);
        addTagListWithViewTag(imageTagList, view, IMAGE_DATE_TAKEN);
        addTagListWithViewTag(imageTagList, view, IMAGE_SIZE);
        //noinspection ToArrayCallWithZeroLengthArrayArgument
        mImageTags = imageTagList.toArray(new String[imageTagList.size()]);

        //
        // EXIF Tags
        //
        ArrayList<String> exifTagList = new ArrayList<>();
        addTagListWithViewTag(exifTagList, view, EXIF_MODEL);
        addTagListWithViewTag(exifTagList, view, EXIF_DATETIME_ORIGINAL);
        addTagListWithViewTag(exifTagList, view, EXIF_IMAGE_WIDTH);
        addTagListWithViewTag(exifTagList, view, EXIF_IMAGE_LENGTH);
        addTagListWithViewTag(exifTagList, view, EXIF_GPS_LATITUDE);
        addTagListWithViewTag(exifTagList, view, EXIF_GPS_LONGITUDE);
        addTagListWithViewTag(exifTagList, view, EXIF_GPS_ALTITUDE);
        //noinspection ToArrayCallWithZeroLengthArrayArgument
        mExifTags = exifTagList.toArray(new String[exifTagList.size()]);
    }

    private void addTagListWithViewTag(List<String> list, View view, String tagName) {
        View target = view.findViewWithTag(tagName);
        if (target != null) {
            list.add(tagName);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
//            Log.d(TAG, "onBindViewHolder");

        // get uri
        Uri uri = mAdapterData.get(position);

        ImageView imageView = holder.imageView;
        holder.setTag(position);

        Bitmap bitmap = mBitmaps.get(position);

        if (bitmap == null) {

//            Log.d(TAG, "onBindViewHolder: taskCount = " + LoadTask.sTasks.size());
            if (LoadTask.sTasks.size() >= mMaxTaskCount ||
                    LoadTask.sTasks.size() >= LoadTask.MAX_TASK_COUNT) {
                cancelAllTasks();
                return;
            }

            if (mEmptyResource != 0) {
                imageView.setImageResource(mEmptyResource);
            } else {
                imageView.setImageResource(android.R.drawable.alert_light_frame);
            }

            holder.setFileTextNull();
            holder.setImageTextNull();
            holder.setExifTextNull();

            boolean loadFileInfo = mFileInfoMap.get(position) == null;
            boolean loadImageInfo = mImageInfoMap.get(position) == null;
            boolean loadExifInfo = mExifInfoMap.get(position) == null;

            LoadTask task = new LoadTask(mContext.getContentResolver(), uri, position, holder,
                    mBitmapOrientationMap.get(position), mFileTags, mImageTags, mExifTags,
                    new TaggingTask.OnApplyListener<Integer>() {
                        @Override
                        public TaggingTask.Result OnPostResult(Integer key,
                                TaggingTask.Result result0) {
                            LoadTask.Result result = (LoadTask.Result) result0;
                            if (result.isSuccessful) {
                                mBitmaps.put(key, result.bitmap);
                                //
                                if (result.orientation != null) {
                                    mBitmapOrientationMap.put(key, result.orientation);
                                }
                                if (result.fileInfo != null) {
                                    String[] fileInfo = result.fileInfo;
                                    for (int i = 0; i < fileInfo.length; i++) {
                                        Formatter formatter = mFormatter.get(mFileTags[i]);
                                        if (formatter != null) {
                                            fileInfo[i] = formatter.format(fileInfo[i]);
                                        }
                                    }
                                    mFileInfoMap.put(key, fileInfo);
                                }
                                if (result.imageInfo != null) {
                                    String[] imageInfo = result.imageInfo;
                                    for (int i = 0; i < imageInfo.length; i++) {
                                        Formatter formatter = mFormatter.get(mImageTags[i]);
                                        if (formatter != null) {
                                            imageInfo[i] = formatter.format(imageInfo[i]);
                                        }
                                    }
                                    mImageInfoMap.put(key, imageInfo);
                                }
                                if (result.exifInfo != null) {
                                    String[] exifInfo = result.exifInfo;
                                    for (int i = 0; i < exifInfo.length; i++) {
                                        Formatter formatter = mFormatter.get(mExifTags[i]);
                                        if (formatter != null) {
                                            exifInfo[i] = formatter.format(exifInfo[i]);
                                        }
                                    }
                                    mExifInfoMap.put(key, exifInfo);
                                }
                            }
                            return null;
                        }

                        @Override
                        public void OnApply(Integer key, TaggingTask.TaggedObject taggedObject,
                                TaggingTask.Result result0) {
                            LoadTask.Result result = (LoadTask.Result) result0;
                            ViewHolder holder = (ViewHolder) taggedObject;
                            holder.imageView.setImageBitmap(result.bitmap);
                            //
                            String[] fileInfo = result.fileInfo;
                            if (fileInfo == null) {
                                fileInfo = mFileInfoMap.get(key);
                            }
                            holder.setFileText(fileInfo);
                            //
                            String[] imageInfo = result.imageInfo;
                            if (imageInfo == null) {
                                imageInfo = mImageInfoMap.get(key);
                            }
                            holder.setImageText(imageInfo);
                            //
                            String[] exifInfo = result.exifInfo;
                            if (exifInfo == null) {
                                exifInfo = mExifInfoMap.get(key);
                            }
                            holder.setExifText(exifInfo);
                        }
                    });

            task.setBitmapLoader(mBitmapLoader);
            task.execute(loadFileInfo, loadImageInfo, loadExifInfo);

        } else {
            imageView.setImageBitmap(bitmap);

            String[] fileInfo = mFileInfoMap.get(position);
            holder.setFileText(fileInfo);

            String[] imageInfo = mImageInfoMap.get(position);
            holder.setImageText(imageInfo);

            String[] exifInfo = mExifInfoMap.get(position);
            holder.setExifText(exifInfo);

        }

    }

    @Override
    public int getItemCount() {
        if (mAdapterData != null) {
            return mAdapterData.size();
        }

        return 0;
    }

    public void clearCache() {
        mBitmaps.evictAll();
        mBitmapOrientationMap.clear();
        mFileInfoMap.clear();
        mImageInfoMap.clear();
        mExifInfoMap.clear();
    }

    public void destroy() {
        clearCache();
        LoadTask.cancelAll();
    }

    //
    //
    //

    private void moveCache(int from, int to) {
        // bitmap cache
        Bitmap bitmap = mBitmaps.remove(from);
        mBitmaps.put(to, bitmap);

        // bitmap orientation map
        Integer orientation = mBitmapOrientationMap.get(from);
        mBitmapOrientationMap.delete(from);
        mBitmapOrientationMap.put(to, orientation);

        // file info map
        String[] fileInfo = mFileInfoMap.get(from);
        mFileInfoMap.delete(from);
        mFileInfoMap.put(to, fileInfo);

        // image info map
        String[] imageInfo = mImageInfoMap.get(from);
        mImageInfoMap.delete(from);
        mImageInfoMap.put(to, imageInfo);

        // exif info map
        String[] exifInfo = mExifInfoMap.get(from);
        mExifInfoMap.delete(from);
        mExifInfoMap.put(to, exifInfo);
    }

    private void removeCache(int index) {
        // bitmap cache
        Bitmap bitmap = mBitmaps.remove(index);
//        bitmap.recycle(); // don't need it because bitmaps are in heap.

        // bitmap orientation map
        mBitmapOrientationMap.delete(index);

        // file info map
        mFileInfoMap.delete(index);

        // image info map
        mImageInfoMap.delete(index);

        // exif info map
        mExifInfoMap.delete(index);
    }

    //
    // Interface
    //

    public interface OnItemClickListener {
        void onItemClick(View view, ImageAdapter adapter, int position);
    }

    public interface OnItemLongClickListener {
        boolean onItemLongClick(View view, ImageAdapter adapter, int position);
    }

    public interface Formatter {
        String format(String str);
    }

    //
    // ViewHolder
    //

    static class ViewHolder extends RecyclerView.ViewHolder implements TaggingTask.TaggedObject {
        // itemView is in super class
        ImageView imageView;
        TextView[] textViews;

        int fileFromIndex;
        int fileToIndex;
        int imageFromIndex;
        int imageToIndex;
        int exifFromIndex;
        int exifToIndex;

        Object tag;

        public ViewHolder(View itemView, String[] fileTags, String[] imageTags, String[] exifTags) {
            super(itemView);
            if (itemView instanceof ImageView) {
                imageView = (ImageView) itemView;
                return;
            }

            imageView = itemView.findViewById(R.id.image);
            if (imageView == null) {
                throw new IllegalArgumentException("cannot find image view");
            }

            int length = fileTags.length + imageTags.length + exifTags.length;

            fileFromIndex = 0;
            fileToIndex = fileTags.length;
            imageFromIndex = fileTags.length;
            imageToIndex = fileTags.length + imageTags.length;
            exifFromIndex = fileTags.length + imageTags.length;
            exifToIndex = fileTags.length + imageTags.length + exifTags.length;

            textViews = new TextView[length];
            for (int i = fileFromIndex; i < fileToIndex; i++) {
                textViews[i] = itemView.findViewWithTag(fileTags[i - fileFromIndex]);
            }
            for (int i = imageFromIndex; i < imageToIndex; i++) {
                textViews[i] = itemView.findViewWithTag(imageTags[i - imageFromIndex]);
            }
            for (int i = exifFromIndex; i < exifToIndex; i++) {
                textViews[i] = itemView.findViewWithTag(exifTags[i - exifFromIndex]);
            }
        }

        public void setFileText(String[] fileInfo) {
            if (fileInfo == null) {
                return;
            }

            for (int i = fileFromIndex; i < fileToIndex; i++) {
                textViews[i].setText(fileInfo[i - fileFromIndex]);
            }
        }

        public void setFileTextNull() {
            for (int i = fileFromIndex; i < fileToIndex; i++) {
                textViews[i].setText("");
            }
        }

        public void setImageText(String[] imageInfo) {
            if (imageInfo == null) {
                return;
            }

            for (int i = imageFromIndex; i < imageToIndex; i++) {
                textViews[i].setText(imageInfo[i - imageFromIndex]);
            }
        }

        public void setImageTextNull() {
            for (int i = imageFromIndex; i < imageToIndex; i++) {
                textViews[i].setText("");
            }
        }

        public void setExifText(String[] exifInfo) {
            if (exifInfo == null) {
                return;
            }

            for (int i = exifFromIndex; i < exifToIndex; i++) {
                textViews[i].setText(exifInfo[i - exifFromIndex]);
            }
        }

        public void setExifTextNull() {
            for (int i = exifFromIndex; i < exifToIndex; i++) {
                textViews[i].setText("");
            }
        }

        @Override
        public void setTag(Object tag) {
            this.tag = tag;
        }

        @Override
        public Object getTag() {
            return tag;
        }
    }

    //
    // LoadTask
    //

    public static class LoadTask extends TaggingTask<Integer, Boolean> {

        public static final int MAX_TASK_COUNT = 128;

        private static Deque<LoadTask> sTasks = new LinkedList<>();

        public static boolean hasTasks() {
            return sTasks.size() > 0;
        }

        public static void cancelAll() {
            for (LoadTask task : sTasks) {
                task.cancel(true);
            }
            sTasks.clear();
        }

        private ContentResolver resolver;
        private Uri uri;
        private Integer bitmapOrientation;
        private String[] fileTags;
        private String[] imageTags;
        private String[] exifTags;

        private BitmapLoader bitmapLoader = null;

        private LoadTask(ContentResolver resolver, Uri uri, Integer key, TaggedObject holder,
                Integer bitmapOrientation, String[] fileTags, String[] imageTags, String[] exifTags,
                OnApplyListener<Integer> listener) {
            super(key, holder, listener);

            this.resolver = resolver;
            this.uri = uri;
            this.bitmapOrientation = bitmapOrientation;
            this.fileTags = fileTags;
            this.imageTags = imageTags;
            this.exifTags = exifTags;

            if (sTasks.size() >= MAX_TASK_COUNT) {
                throw new IllegalStateException("don't instantiate a task any more");
            }

            sTasks.add(this);
        }

        private void setBitmapLoader(BitmapLoader loader) {
            this.bitmapLoader = loader;
        }

        @Override
        protected Result doInBackground(Boolean... flag) {
            if (isCancelled()) {
                return null;
            }

            final boolean loadFileInfo = flag[0];
            final boolean loadImageInfo = flag[1];
            final boolean loadExifInfo = flag[2];

            // get bitmap
            Bitmap bitmap;
            if (bitmapLoader != null) {
                bitmap = bitmapLoader.loadBitmap(resolver, uri);
            } else {
                throw new IllegalStateException("need bitmap loader");
            }
            if (bitmap == null) {
                return new Result(false, null, null, null, null, null);
            }

            // file properties
            String[] fileInfo = null;
            if (loadFileInfo && fileTags.length > 0) {
//                Log.d(TAG, "file properties");
                fileInfo = new String[fileTags.length];

                String path = null;
                String scheme = uri.getScheme();
                if (scheme != null) {
                    scheme = scheme.trim().toLowerCase(Locale.ENGLISH);
                }
                if ("file".equals(scheme)) {
                    path = uri.getPath();
                } else if ("content".equals(scheme)) {
                    Cursor cursor =
                            resolver.query(uri, new String[]{MediaStore.Images.Media.DATA}, null,
                                    null, null);
                    if (cursor != null && cursor.getCount() > 0) {
                        int columnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                        if (columnIndex >= 0) {
                            cursor.moveToFirst();
                            path = cursor.getString(columnIndex);
                        }
                        cursor.close();
                    }
                }

                if (path != null) {
                    File file = new File(path);
                    for (int i = 0; i < fileInfo.length; i++) {
                        switch (fileTags[i]) {
                            case FILE_NAME:
                                fileInfo[i] = file.getName();
                                break;
                            case FILE_PATH:
                                fileInfo[i] = file.getPath();
                                break;
                            case FILE_SIZE:
                                fileInfo[i] = String.valueOf(file.length());
                                break;
                            default:
                                fileInfo[i] = "no data";
                        }
                    }
                } else {
                    for (int i = 0; i < fileInfo.length; i++) {
                        fileInfo[i] = "error";
                    }
                }
            }

            // read provider
            String[] imageInfo = null;
            if (loadImageInfo && imageTags.length > 0) {
//                Log.d(TAG, "load provider");
                imageInfo = new String[imageTags.length];
                String[] projection = new String[imageTags.length];
                for (int i = 0; i < projection.length; i++) {
                    projection[i] = sImageColumnsMap.get(imageTags[i]);
                }
                Cursor cursor = resolver.query(uri, projection, null, null, null);
                if (cursor != null) {
                    cursor.moveToFirst();
                    for (int i = 0; i < imageInfo.length; i++) {
                        int index = cursor.getColumnIndex(projection[i]);
                        if (index >= 0) {
                            imageInfo[i] = cursor.getString(index);
                        }
                    }
                    cursor.close();
                }
            }

            // read orientation and other exif
            Integer orientation = bitmapOrientation;
            String[] exifInfo = null;
            if (orientation == null || (loadExifInfo && exifTags.length > 0)) {
                InputStream stream = null;
                try {
                    InputStream in = resolver.openInputStream(uri);
                    if (in != null) {
//                        Log.d(TAG, "load exif");
                        stream = new BufferedInputStream(in);
                        ExifInterface exifInterface = new ExifInterface(stream);

                        // orientation
                        if (orientation == null) {
                            orientation = exifInterface
                                    .getAttributeInt(ExifInterface.TAG_ORIENTATION,
                                            ExifInterface.ORIENTATION_NORMAL);
                        }

                        // other exif
                        if (loadExifInfo && exifTags.length > 0) {
                            exifInfo = new String[exifTags.length];
                            for (int i = 0; i < exifInfo.length; i++) {
                                String realTag = sExifRealTagsMap.get(exifTags[i]);
                                if (realTag != null) {
                                    exifInfo[i] = exifInterface.getAttribute(realTag);
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (stream != null) {
                            stream.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            //
            // rotate
            //
            Bitmap newBitmap = bitmap;
            if (orientation != null) {
                switch (orientation) {
                    case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                        newBitmap = createPreScaledBitmap(bitmap, -1.0f, 1.0f, true);
                        bitmap.recycle();
                        break;
                    case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                        newBitmap =
                                createRotatedAndPreScaledBitmap(bitmap, 180.0f, -1.0f, 1.0f, true);
                        bitmap.recycle();
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        newBitmap = createRotatedBitmap(bitmap, 90.0f, true);
                        bitmap.recycle();
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        newBitmap = createRotatedBitmap(bitmap, 180.0f, true);
                        bitmap.recycle();
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        newBitmap = createRotatedBitmap(bitmap, 270.0f, true);
                        bitmap.recycle();
                        break;
                    case ExifInterface.ORIENTATION_TRANSPOSE:
                        newBitmap =
                                createRotatedAndPreScaledBitmap(bitmap, 90.0f, -1.0f, 1.0f, true);
                        bitmap.recycle();
                        break;
                    case ExifInterface.ORIENTATION_TRANSVERSE:
                        newBitmap =
                                createRotatedAndPreScaledBitmap(bitmap, 270.0f, -1.0f, 1.0f, true);
                        bitmap.recycle();
                        break;
                }
            }

            return new Result(true, newBitmap, orientation, fileInfo, imageInfo, exifInfo);
        }

        @Override
        protected void onPostExecute(TaggingTask.Result result) {
            super.onPostExecute(result);
            resolver = null;
            sTasks.remove(this);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            resolver = null;
            sTasks.remove(this);
        }

        public interface BitmapLoader {
            Bitmap loadBitmap(ContentResolver resolver, Uri uri);
        }

        public static class Result extends TaggingTask.Result {
            public Bitmap bitmap;
            public Integer orientation;
            public String[] fileInfo;
            public String[] imageInfo;
            public String[] exifInfo;

            public Result(boolean isSuccessful, Bitmap bitmap, Integer orientation,
                    String[] fileInfo, String[] imageInfo, String[] exifInfo) {
                this.isSuccessful = isSuccessful;
                this.bitmap = bitmap;
                this.orientation = orientation;
                this.fileInfo = fileInfo;
                this.imageInfo = imageInfo;
                this.exifInfo = exifInfo;
            }
        }

    }

    //
    //
    //

    @SuppressWarnings("unused")
    private static Bitmap createScaledAndRotatedBitmap(Bitmap src, int dstWidth, int dstHeight,
            float rotation, boolean filter) {
        Matrix m = new Matrix();
        int width = src.getWidth();
        int height = src.getHeight();
        float sx = dstWidth / (float) width;
        float sy = dstHeight / (float) height;
        m.setScale(sx, sy);
        float cx = width / 2.0f;
        float cy = height / 2.0f;
        m.postRotate(rotation, cx, cy);
        return Bitmap.createBitmap(src, 0, 0, width, height, m, filter);
    }

    @SuppressWarnings({"unused", "SameParameterValue"})
    private static Bitmap createRotatedBitmap(Bitmap src, float rotation, boolean filter) {
        Matrix m = new Matrix();
        int width = src.getWidth();
        int height = src.getHeight();
        float cx = width / 2.0f;
        float cy = height / 2.0f;
        m.postRotate(rotation, cx, cy);
        return Bitmap.createBitmap(src, 0, 0, width, height, m, filter);
    }

    @SuppressWarnings({"unused", "SameParameterValue"})
    private static Bitmap createPreScaledBitmap(Bitmap src, float sx, float sy, boolean filter) {
        Matrix m = new Matrix();
        int width = src.getWidth();
        int height = src.getHeight();
        m.preScale(sx, sy);
        return Bitmap.createBitmap(src, 0, 0, width, height, m, filter);
    }

    @SuppressWarnings({"unused", "SameParameterValue"})
    private static Bitmap createRotatedAndPreScaledBitmap(Bitmap src, float rotation, float sx,
            float sy, boolean filter) {
        Matrix m = new Matrix();
        int width = src.getWidth();
        int height = src.getHeight();
        float cx = width / 2.0f;
        float cy = height / 2.0f;
        m.postRotate(rotation, cx, cy);
        m.preScale(sx, sy);
        return Bitmap.createBitmap(src, 0, 0, width, height, m, filter);
    }

    @SuppressWarnings("unused")
    private static int dp2px(Context con, float dp) {
        return (int) (dp * con.getResources().getDisplayMetrics().density);
    }

}
