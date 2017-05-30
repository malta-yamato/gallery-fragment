package jp.yamato.malta.android.galleryfragment;

import android.net.Uri;

import java.util.ArrayList;

/**
 * Created by Malta on 2017/05/29.
 * GalleryFragmentUtils
 */

public class GalleryFragmentUtils {

    public static ArrayList<String> toStringArrayList(ArrayList<Uri> uriList) {
        ArrayList<String> stringList = new ArrayList<>();
        if (uriList != null) {
            for (int i = 0; i < uriList.size(); i++) {
                stringList.add(uriList.get(i).toString());
            }
        }
        return stringList;
    }

    public static ArrayList<Uri> toUriArrayList(ArrayList<String> stringList) {
        ArrayList<Uri> uriList = new ArrayList<>();
        if (stringList != null) {
            for (int i = 0; i < stringList.size(); i++) {
                uriList.add(Uri.parse(stringList.get(i)));
            }
        }
        return uriList;
    }

}
