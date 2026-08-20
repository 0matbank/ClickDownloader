package com.clickdownloader.app;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public final class CookieStore {

    private static final String FILE_NAME = "cookies.txt";

    private CookieStore() {
    }

    public static File getFile(Context context) {
        return new File(context.getFilesDir(), FILE_NAME);
    }

    public static boolean exists(Context context) {
        File f = getFile(context);
        return f.exists() && f.length() > 0;
    }

    public static boolean importFile(Context context, Uri uri) {
        File target = getFile(context);

        try (InputStream in = context.getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(target, false)) {

            if (in == null) {
                return false;
            }

            byte[] buffer = new byte[8192];
            int read;

            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }

            out.flush();
            return target.length() > 0;

        } catch (Exception e) {
            return false;
        }
    }

    public static void clear(Context context) {
        File f = getFile(context);
        if (f.exists()) {
            //noinspection ResultOfMethodCallIgnored
            f.delete();
        }
    }
}
