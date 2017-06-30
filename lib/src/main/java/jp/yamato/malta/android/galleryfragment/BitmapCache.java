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
import android.graphics.Bitmap;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
public class BitmapCache<K> {
    @SuppressWarnings("unused")
    private static final String TAG = "BitmapCache";

    public static final int MIN_CACHE_SIZE_KB = 10 * 1024; // 10MB

    private FastLruCache<K, Bitmap> mCache;

    public BitmapCache() {
        this(20.0f);
    }

    public BitmapCache(float percentOfMaxMemory) {
        if (percentOfMaxMemory < 10.0f || percentOfMaxMemory > 90.0f) {
            throw new IllegalArgumentException("percentOfMaxMemory must be from 10.0 to 90.0");
        }
        float ratio = percentOfMaxMemory / 100;
//        Log.d(TAG, "BitmapCache: ratio = " + ratio);

        int maxMemoryKB = (int) (Runtime.getRuntime().maxMemory() / 1024); // KB
        int cacheSizeKB = (int) (maxMemoryKB * ratio);
//        Log.d(TAG, "BitmapCache: maxMemoryKB, cacheSizeKB = " + maxMemoryKB + ", " + cacheSizeKB);

        if (cacheSizeKB < MIN_CACHE_SIZE_KB) {
            cacheSizeKB = MIN_CACHE_SIZE_KB;
        }

//        cacheSizeKB = 1000;

        mCache = new FastLruCache<K, Bitmap>(cacheSizeKB) {
            @Override
            protected int sizeOf(K key, Bitmap bitmap) {
                return getAllocationKB(bitmap); // KB
            }
        };

//        Log.d(TAG, "BitmapCache: cacheSizeKB = " + cacheSizeKB);

    }

    public Bitmap get(K key) {
        return mCache.get(key);
    }

    public void put(K key, Bitmap bitmap) {
        mCache.put(key, bitmap);
//        Log.d(TAG, "putBitmap: key, KB, = " + key + ", " + getAllocationKB(bitmap));
//        Log.d(TAG, "putBitmap(Cont.): size, Max= " + mCache.size() + ", " + mCache.maxSize());

//        Bitmap old = mCache.put(key, bitmap);
//        if (old != null) {
//            if (!old.isRecycled()) {
//                old.recycle();
//            }
//            old = null;
//        }
    }

    public Bitmap remove(K key) {
        return mCache.remove(key);
    }

    public void evictAll() {
//        Log.d(TAG, "evictAll");
        mCache.evictAll();
    }

    public int size() {
        return mCache.size();
    }

    private static int getAllocationKB(Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return bitmap.getAllocationByteCount() / 1024; // KB
        }
        return bitmap.getRowBytes() * bitmap.getHeight() / 1024; // KB
    }

}
