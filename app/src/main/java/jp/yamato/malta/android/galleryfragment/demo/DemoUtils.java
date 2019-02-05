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

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class DemoUtils {
    @SuppressWarnings("unused")
    private static final String TAG = "DemoUtils";

    private static final int IMAGE_BUF_SIZE = 8 * 1048576;

    public static void startActivity(Context context, Uri uri) {
        if (uri == null) {
            Toast.makeText(context, "We can't show it because of null uri ", Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(intent);
    }

    @NonNull
    public static ArrayList<Uri> getSampleUriFromExternalContent(Context context, int count) {
        // get image url
        Cursor cursor = context.getContentResolver()
                .query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        new String[]{MediaStore.Images.Media._ID}, null, null,
                        MediaStore.Images.Media.DATE_TAKEN + " DESC");
        if (cursor == null) {
            throw new IllegalStateException("We couldn't obtain media cursor!");
        }

        // check count
        int cursorCount = cursor.getCount();
        if (count > cursorCount) {
            Toast.makeText(context, "We don't have enough images requested number. ",
                    Toast.LENGTH_SHORT).show();
        }

        // create uri list
        ArrayList<Uri> list = new ArrayList<>();
        for (int i = 0; i < count && i < cursorCount; i++) {
            cursor.moveToPosition(i);
            long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Images.Media._ID));
            Uri uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    String.valueOf(id));
            list.add(uri);
        }

        cursor.close();

        return list;
    }

    // Since AsyncTask used by the ImageAdapter runs in series, there is no need to synchronize.
    public static void writeBitmapToCache(Context context, Uri uri, Bitmap bitmap, int size) {
        if (uri == null || bitmap == null) {
            return;
        }

        // write cache
        File cacheFile = getCacheFile(context, uri, size);
        BufferedOutputStream stream = null;
        try {
            stream = new BufferedOutputStream(new FileOutputStream(cacheFile));
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
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
    }

    // Since AsyncTask used by the ImageAdapter runs in series, there is no need to synchronize.
    public static Bitmap readBitmapFromCache(Context context, Uri uri, int size) {
        if (uri == null) {
            return null;
        }

        // read cache if exist
        File cacheFile = getCacheFile(context, uri, size);
        if (cacheFile.exists()) {
            BufferedInputStream stream = null;
            try {
                stream = new BufferedInputStream(new FileInputStream(cacheFile));
                return BitmapFactory.decodeStream(stream);
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
        }

        return null;
    }

    @NonNull
    private static File getCacheFile(Context context, @NonNull Uri uri, int size) {
        // cache name
        String path = uri.getPath();
        String cacheName =
                path.replace('/', '_').replace('\\', '_').replace(':', '_').replace(',', '_')
                        .replace(';', '_') + "_" + size + ".png";

        // cache dir
        File dir = getThumbnailsDir(context);

        return new File(dir, cacheName);
    }

    // if use, care synchronization.
    public static void clearCache(Context context) {
        File dir = getThumbnailsDir(context);
        File[] files = dir.listFiles();
        for (File file : files) {
            file.delete();
        }
    }

    public static File getThumbnailsDir(Context context) {
        File dir = new File(context.getExternalCacheDir(), "thumbnails");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        return dir;
    }

    public static Bitmap createBitmapByDecodedStream(ContentResolver resolver, Uri uri,
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

    private static int getMostRegulatedSampleSize(int width, int height, int tgtWidth,
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

    private static int getLeastRegulatedSampleSize(int width, int height, int tgtWidth,
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

}
