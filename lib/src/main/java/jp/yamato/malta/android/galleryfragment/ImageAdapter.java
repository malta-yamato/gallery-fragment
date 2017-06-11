package jp.yamato.malta.android.galleryfragment;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.media.ExifInterface;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by malta on 2017/05/24.
 * ImageAdapter
 */

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {
    @SuppressWarnings("unused")
    private static final String TAG = "ImageAdapter";

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

    private Context mContext;
    private int mResource;
    private int mEmptyResource = 0;
    private ArrayList<Uri> mAdapterData;
    private OnItemClickListener mListener;

    private final BitmapCache<Integer> mBitmaps = new BitmapCache<>();
    private final SparseArray<Integer> mBitmapOrientationMap = new SparseArray<>();

    private boolean mIsInfoSetup;
    private String[] mExifTags;
    private String[] mImageTags;
    private final SparseArray<String[]> mExifInfoMap = new SparseArray<>();
    private final SparseArray<String[]> mImageInfoMap = new SparseArray<>();

    private int mMaxTaskCount = LoadTask.MAX_TASK_COUNT;
    private LoadTask.BitmapLoader mBitmapLoader = null;

    private Map<String, Formatter> mFormatter = new HashMap<>();

    //
    //
    //

    public ImageAdapter(Context context, int resource, ArrayList<Uri> data,
            OnItemClickListener listener) {
        mContext = context;
        mResource = resource;
        mAdapterData = data;
        mListener = listener;
    }

    public void setEmptyResource(int emptyResource) {
        mEmptyResource = emptyResource;
    }

    public void setMaxTaskCount(int maxTaskCount) {
        mMaxTaskCount = maxTaskCount;
    }

    public void setAdapterData(ArrayList<Uri> data) {
        clearCache();
        LoadTask.cancelAll();
        mAdapterData = data;
        notifyDataSetChanged();
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

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//            Log.d(TAG, "onCreateViewHolder");

        View view = LayoutInflater.from(mContext).inflate(mResource, parent, false);
        if (!mIsInfoSetup) {
            setupInfo(view);
            mIsInfoSetup = true;
        }
        final ViewHolder holder = new ViewHolder(view, mImageTags, mExifTags);

        // item click mListener
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onItemClick(v, ImageAdapter.this, holder.getAdapterPosition());
                }
            }
        });

        return holder;
    }

    private void setupInfo(View view) {

        //
        // Image Tags
        //
        ArrayList<String> imageTagList = new ArrayList<>();
        addTagListWithViewTag(imageTagList, view, IMAGE_DATA);
        addTagListWithViewTag(imageTagList, view, IMAGE_DISPLAY_NAME);
        addTagListWithViewTag(imageTagList, view, IMAGE_DATE_TAKEN);
        addTagListWithViewTag(imageTagList, view, IMAGE_SIZE);
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
        mExifTags = exifTagList.toArray(new String[exifTagList.size()]);
    }

    private void addTagListWithViewTag(List<String> list, View view, String tagName) {
        View target = view.findViewWithTag(tagName);
        if (target != null) {
            list.add(tagName);
        }
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
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
                LoadTask.cancelAll();
                imageView.post(new Runnable() {
                    @Override
                    public void run() {
                        notifyDataSetChanged();
                    }
                });
                return;
            }

            if (mEmptyResource != 0) {
                imageView.setImageResource(mEmptyResource);
            } else {
                imageView.setImageResource(android.R.drawable.alert_light_frame);
            }

            holder.setImageTextNull();
            holder.setExifTextNull();

            boolean loadImageInfo = mImageInfoMap.get(position) == null;
            boolean loadExifInfo = mExifInfoMap.get(position) == null;

            LoadTask task = new LoadTask(mContext.getContentResolver(), uri, position, holder,
                    mBitmapOrientationMap.get(position), mImageTags, mExifTags,
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
                                if (result.imageInfo != null) {
                                    String[] imageInfo = result.imageInfo;
                                    for (int i = 0; i < imageInfo.length; i++) {
                                        Formatter formatter = mFormatter.get(mImageTags[i]);
                                        if (formatter != null) {
                                            imageInfo[i] = formatter.format(imageInfo[i]);
                                        }
                                    }
                                    mImageInfoMap.put(key, result.imageInfo);
                                }
                                if (result.exifInfo != null) {
                                    String[] exifInfo = result.exifInfo;
                                    for (int i = 0; i < exifInfo.length; i++) {
                                        Formatter formatter = mFormatter.get(mExifTags[i]);
                                        if (formatter != null) {
                                            exifInfo[i] = formatter.format(exifInfo[i]);
                                        }
                                    }
                                    mExifInfoMap.put(key, result.exifInfo);
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
            task.execute(loadImageInfo, loadExifInfo);

        } else {
            imageView.setImageBitmap(bitmap);

            String[] exifInfo = mExifInfoMap.get(position);
            holder.setExifText(exifInfo);

            String[] imageInfo = mImageInfoMap.get(position);
            holder.setImageText(imageInfo);

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
        mImageInfoMap.clear();
        mExifInfoMap.clear();
    }

    public void destroy() {
        clearCache();
        LoadTask.cancelAll();
    }

    public interface OnItemClickListener {
        void onItemClick(View view, ImageAdapter adapter, int position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder implements TaggingTask.TaggedObject {
        // itemView is in super class
        ImageView imageView;
        TextView[] textViews;

        int imageFromIndex;
        int imageToIndex;
        int exifFromIndex;
        int exifToIndex;

        Object tag;

        public ViewHolder(View itemView, String[] imageTags, String[] exifTags) {
            super(itemView);
            if (itemView instanceof ImageView) {
                imageView = (ImageView) itemView;
                return;
            }

            imageView = (ImageView) itemView.findViewById(R.id.image);
            if (imageView == null) {
                throw new IllegalArgumentException("cannot find image view");
            }

            int length = imageTags.length + exifTags.length;

            imageFromIndex = 0;
            imageToIndex = imageTags.length;
            exifFromIndex = imageTags.length;
            exifToIndex = imageTags.length + exifTags.length;

            textViews = new TextView[length];
            for (int i = imageFromIndex; i < imageToIndex; i++) {
                textViews[i] = (TextView) itemView.findViewWithTag(imageTags[i - imageFromIndex]);
            }
            for (int i = exifFromIndex; i < exifToIndex; i++) {
                textViews[i] = (TextView) itemView.findViewWithTag(exifTags[i - exifFromIndex]);
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

    public static class LoadTask extends TaggingTask<Integer, Boolean> {

        public static final int MAX_TASK_COUNT = 128;

        private static Deque<LoadTask> sTasks = new LinkedList<>();

        public static void cancelAll() {
            for (LoadTask task : sTasks) {
                task.cancel(true);
            }
            sTasks.clear();
        }

        private ContentResolver resolver;
        private Uri uri;
        private Integer bitmapOrientation;
        private String[] imageTags;
        private String[] exifTags;

        private BitmapLoader bitmapLoader = null;

        private LoadTask(ContentResolver resolver, Uri uri, Integer key, TaggedObject holder,
                Integer bitmapOrientation, String[] imageTags, String[] exifTags,
                OnApplyListener<Integer> listener) {
            super(key, holder, listener);

            this.resolver = resolver;
            this.uri = uri;
            this.bitmapOrientation = bitmapOrientation;
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

            final boolean loadImageInfo = flag[0];
            final boolean loadExifInfo = flag[1];

            // get bitmap
            Bitmap bitmap;
            if (bitmapLoader != null) {
                bitmap = bitmapLoader.loadBitmap(uri);
            } else {
                throw new IllegalStateException("need bitmap loader");
            }
            if (bitmap == null) {
                return new Result(false, null, null, null, null);
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

            return new Result(true, newBitmap, orientation, imageInfo, exifInfo);
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
            Bitmap loadBitmap(Uri uri);
        }

        public static class Result extends TaggingTask.Result {
            public Bitmap bitmap;
            public Integer orientation;
            public String[] imageInfo;
            public String[] exifInfo;

            public Result(boolean isSuccessful, Bitmap bitmap, Integer orientation,
                    String[] imageInfo, String[] exifInfo) {
                this.isSuccessful = isSuccessful;
                this.bitmap = bitmap;
                this.orientation = orientation;
                this.imageInfo = imageInfo;
                this.exifInfo = exifInfo;
            }
        }

    }

    public interface Formatter {
        String format(String str);
    }

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

    @SuppressWarnings("unused")
    private static Bitmap createRotatedBitmap(Bitmap src, float rotation, boolean filter) {
        Matrix m = new Matrix();
        int width = src.getWidth();
        int height = src.getHeight();
        float cx = width / 2.0f;
        float cy = height / 2.0f;
        m.postRotate(rotation, cx, cy);
        return Bitmap.createBitmap(src, 0, 0, width, height, m, filter);
    }

    @SuppressWarnings("unused")
    private static Bitmap createPreScaledBitmap(Bitmap src, float sx, float sy, boolean filter) {
        Matrix m = new Matrix();
        int width = src.getWidth();
        int height = src.getHeight();
        m.preScale(sx, sy);
        return Bitmap.createBitmap(src, 0, 0, width, height, m, filter);
    }

    @SuppressWarnings("unused")
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