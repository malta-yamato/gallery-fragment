package jp.yamato.malta.android.galleryfragment;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import java.util.ArrayList;

/**
 * Created by malta on 2017/06/04.
 * CustomGalleryDialogFragment
 */
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
    public Bitmap loadBitmap(Uri uri) {
        long id = Long.valueOf(uri.getLastPathSegment());
        return MediaStore.Images.Thumbnails.getThumbnail(getContext().getContentResolver(), id,
                MediaStore.Images.Thumbnails.MICRO_KIND, null);
    }
}
