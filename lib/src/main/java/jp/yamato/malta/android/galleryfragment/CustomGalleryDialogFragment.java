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
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import java.util.ArrayList;

@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
public class CustomGalleryDialogFragment extends GalleryDialogFragment
        implements ImageAdapter.LoadTask.BitmapLoader {

    public static CustomGalleryDialogFragment newInstance(int resource) {
        CustomGalleryDialogFragment instance = new CustomGalleryDialogFragment();
        GalleryFragmentDelegate.setArguments(instance, resource);
        return instance;
    }

    public static CustomGalleryDialogFragment newInstance(int resource, int spanCount) {
        CustomGalleryDialogFragment instance = new CustomGalleryDialogFragment();
        GalleryFragmentDelegate.setArguments(instance, resource, spanCount);
        return instance;
    }

    public static CustomGalleryDialogFragment newInstance(int resource, int spanCount,
            ArrayList<Uri> data) {
        CustomGalleryDialogFragment instance = new CustomGalleryDialogFragment();
        GalleryFragmentDelegate.setArguments(instance, resource, spanCount, data);
        return instance;
    }

    @Override
    public Bitmap loadBitmap(ContentResolver resolver, Uri uri) {
        //noinspection ConstantConditions
        long id = Long.valueOf(uri.getLastPathSegment());
        //noinspection ConstantConditions
        return MediaStore.Images.Thumbnails.getThumbnail(getContext().getContentResolver(), id,
                MediaStore.Images.Thumbnails.MICRO_KIND, null);
    }
}
