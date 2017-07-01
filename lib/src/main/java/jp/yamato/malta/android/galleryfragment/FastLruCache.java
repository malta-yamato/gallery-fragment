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

import java.util.Iterator;
import java.util.LinkedHashMap;

public class FastLruCache<K, V> {
    @SuppressWarnings("unused")
    private static final String TAG = "FastLruCache";

    private final LinkedHashMap<K, V> mMap;

    private int mSize;
    private int mMaxSize;

    public FastLruCache(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }
        mMaxSize = maxSize;
        mMap = new LinkedHashMap<>(0, 0.75f, true);

//        Log.d(TAG, "initial max size = " + mMaxSize);
    }

    public void resize(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize <= 0");
        }

        mMaxSize = maxSize;
        trimToSize(maxSize);
    }

    public final V get(K key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }

//        Log.d(TAG, "get[B]: key=" + key + "; " + debugSizeAndKeySet());
//        V value = mMap.get(key);
//        Log.d(TAG, "get[A]: key=" + key + "; " + debugSizeAndKeySet());

        return mMap.get(key);
    }

    public final V put(K key, V value) {
        if (value == null) {
            return null;
        }

        if (key == null) {
            throw new NullPointerException("key == null");
        }

//        Log.d(TAG, "put[B]: key=" + key + "; " + debugSizeAndKeySet());
        mSize += safeSizeOf(key, value);
        V previous = mMap.put(key, value);
        if (previous != null) {
//            Log.d(TAG, "remove previous");
            mSize -= safeSizeOf(key, previous);
            if (mSize < 0) {
                throw new IllegalStateException("negative size");
            }
        }

        trimToSize(mMaxSize);

//        Log.d(TAG, "put[A]: key=" + key + "; " + debugSizeAndKeySet());
        return previous;
    }

    public final V remove(K key) {
        if (key == null) {
            throw new NullPointerException("key == null");
        }

//        Log.d(TAG, "remove[B]: key=" + key + "; " + debugSizeAndKeySet());

        V previous = mMap.remove(key);
        if (previous != null) {
            mSize -= safeSizeOf(key, previous);
            if (mSize < 0) {
                throw new IllegalStateException("negative size");
            }
        }

//        Log.d(TAG, "remove[A]: key=" + key + "; " + debugSizeAndKeySet());
        return previous;
    }

    public final void evictAll() {
//        Log.d(TAG, "evictAll[B]: " + debugSizeAndKeySet());
        trimToSize(-1);
//        Log.d(TAG, "evictAll[A]: " + debugSizeAndKeySet());
    }

    public void trimToSize(int maxSize) {
        while (mSize > maxSize) {
            Iterator<K> it = mMap.keySet().iterator();
            if (it.hasNext()) {
                remove(it.next());
            } else {
                break;
            }
        }

    }

    public final int size() {
        return mSize;
    }

    public final int maxSize() {
        return mMaxSize;
    }

    private int safeSizeOf(K key, V value) {
        int result = sizeOf(key, value);
        if (result < 0) {
            throw new IllegalStateException("Negative size: " + key + "=" + value);
        }
        return result;
    }

    protected int sizeOf(K key, V value) {
        return 1;
    }

    private String debugSizeAndKeySet() {
        StringBuilder builder = new StringBuilder();
        builder.append("size=");
        builder.append(mSize);
        builder.append("; ");
        boolean first = true;
        for (Iterator<K> it = mMap.keySet().iterator(); it.hasNext(); ) {
            if (first) {
                builder.append("[ ");
                first = false;
            } else {
                builder.append(", ");
            }
            builder.append(it.next());
        }
        builder.append(" ]");

        return builder.toString();
    }

}
