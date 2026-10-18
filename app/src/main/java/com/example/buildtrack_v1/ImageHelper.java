package com.example.buildtrack_v1;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import java.io.ByteArrayOutputStream;

public class ImageHelper {

    // photo -> text that can be stored in the database
    public static String bitmapToBase64(Bitmap original) {
        int maxSide = 800;
        int w = original.getWidth();
        int h = original.getHeight();
        float scale = Math.min(1f, (float) maxSide / Math.max(w, h));
        Bitmap small = Bitmap.createScaledBitmap(original, Math.round(w * scale), Math.round(h * scale), true);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        small.compress(Bitmap.CompressFormat.JPEG, 70, stream);
        byte[] bytes = stream.toByteArray();
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    // text from the database -> photo that can be shown in an ImageView
    public static Bitmap base64ToBitmap(String text) {
        byte[] bytes = Base64.decode(text, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}