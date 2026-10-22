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
     * Menyimpan avatar ke storage lokal. 
     * @param isGif Jika true, simpan sebagai PNG (karena Bitmap tidak bisa simpan animasi GIF).
     */
    public static void saveAvatarToLocal(Context ctx, int userId, Bitmap bitmap, boolean isGif) {
        File dir = new File(ctx.getFilesDir(), AVATAR_DIR);
        if (!dir.exists()) dir.mkdirs();

        // Selalu simpan sebagai PNG agar support transparan & kualitas lebih baik dari JPG
        String fileName = userId + ".png"; 
        File file = new File(dir, fileName);
        
        try (FileOutputStream fos = new FileOutputStream(file)) {
            // Kompresi PNG kualitas 90% (cukup bagus untuk avatar kecil)
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Mengambil avatar dari storage lokal
     */
    public static Bitmap getAvatarFromLocal(Context ctx, int userId) {
        File dir = new File(ctx.getFilesDir(), AVATAR_DIR);
        
        // Cek file PNG dulu (format baru kita)
        File pngFile = new File(dir, userId + ".png");
        if (pngFile.exists()) {
            return BitmapFactory.decodeFile(pngFile.getAbsolutePath());
        }
        
        // Fallback ke JPG lama (biar user lama gak hilang fotonya)
        File jpgFile = new File(dir, userId + ".jpg");
        if (jpgFile.exists()) {
            return BitmapFactory.decodeFile(jpgFile.getAbsolutePath());
        }
        
        return null;
    }
}
