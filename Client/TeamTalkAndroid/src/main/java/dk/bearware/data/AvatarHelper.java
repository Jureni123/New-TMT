package dk.bearware.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class AvatarHelper {
    private static final String AVATAR_DIR = "user_avatars";

    /**
     * Menyimpan avatar ke storage lokal berdasarkan User ID TeamTalk
     */
    public static void saveAvatarToLocal(Context ctx, int userId, Bitmap bitmap) {
        File dir = new File(ctx.getFilesDir(), AVATAR_DIR);
        if (!dir.exists()) dir.mkdirs();

        File file = new File(dir, userId + ".jpg");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Mengambil avatar dari storage lokal
     */
    public static Bitmap getAvatarFromLocal(Context ctx, int userId) {
        File file = new File(ctx.getFilesDir(), AVATAR_DIR + "/" + userId + ".jpg");
        if (file.exists()) {
            return BitmapFactory.decodeFile(file.getAbsolutePath());
        }
        return null;
    }
}
