package com.example.buildtrack_v1;

import android.util.LruCache;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

public class ImageHelper {

    /** Decoded base64 is expensive to fetch again, so keep the last few in memory. */
    private static final LruCache<String, String> PHOTO_CACHE = new LruCache<>(12);

    /**
     * Shows a stored photo as a cropped thumbnail. Stored shapes: legacy Storage URLs (https://),
     * raw base64, "fs:<docId>" (sitePhotos collection) and "doc:<docId>" (documentFiles collection).
     */
    public static void showPhoto(ImageView view, String stored) {
        show(view, stored, false);
    }

    /** Same, but uncropped and untransformed, for the zoomable full-screen viewer. */
    public static void showFull(ImageView view, String stored) {
        show(view, stored, true);
    }

    private static void show(ImageView view, String stored, boolean full) {
        if (stored == null || stored.isEmpty()) {
            Glide.with(view).clear(view);
            return;
        }
        String collection = null;
        if (stored.startsWith("fs:")) collection = "sitePhotos";
        else if (stored.startsWith("doc:")) collection = "documentFiles";

        if (collection == null) {
            load(view, stored, full);
            return;
        }

        String id = stored.substring(stored.indexOf(':') + 1);
        String key = collection + "/" + id;
        String cached = PHOTO_CACHE.get(key);
        if (cached != null) {
            load(view, cached, full);
            return;
        }

        FirebaseFirestore.getInstance().collection(collection).document(id).get()
                .addOnSuccessListener(doc -> {
                    String data = doc.getString("data");
                    if (data == null) {
                        Glide.with(view).clear(view);
                        return;
                    }
                    PHOTO_CACHE.put(key, data);
                    load(view, data, full);
                })
                .addOnFailureListener(e -> Glide.with(view).clear(view));
    }

    private static void load(ImageView view, String stored, boolean full) {
        String model = stored.startsWith("http") ? stored : "data:image/jpeg;base64," + stored;
        if (full) Glide.with(view).load(model).dontTransform().into(view);
        else Glide.with(view).load(model).centerCrop().into(view);
    }
}
