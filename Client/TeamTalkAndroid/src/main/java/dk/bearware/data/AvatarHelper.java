package dk.bearware.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class AvatarHelper {
    private static final String AVATAR_DIR = "user_avatars";
    private static final String TAG = "AvatarHelper";

    /**
     * Menyimpan avatar ke storage lokal secara permanen.
     * Menggunakan PNG agar kualitas tetap tajam saat disimpan di HP.
     */
    public static void saveAvatarToLocal(Context ctx, int userId, Bitmap bitmap, boolean isGif) {
        try {
            // 1. Pastikan folder penyimpanan ada
            File dir = new File(ctx.getFilesDir(), AVATAR_DIR);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (!created) {
                    Log.e(TAG, "Gagal membuat folder: " + dir.getAbsolutePath());
                    return;
                }
            }

            // 2. Tentukan nama file (selalu PNG untuk konsistensi)
            String fileName = userId + ".png"; 
            File file = new File(dir, fileName);
            
            // 3. Hapus file lama jika ada (mencegah data korup/tumpang tindih)
            if (file.exists()) {
                file.delete();
            }

            // 4. Tulis file baru dengan aman
            FileOutputStream fos = new FileOutputStream(file);
            try {
                // Kompresi PNG kualitas 90% (sangat cukup untuk ukuran 150px)
                boolean success = bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);
                
                if (success) {
                    // WAJIB: Flush agar data benar-benar masuk ke storage fisik
                    fos.flush(); 
                    Log.d(TAG, "✅ Avatar berhasil disimpan permanen untuk UserID: " + userId);
                } else {
                    Log.e(TAG, "❌ Gagal kompresi bitmap untuk UserID: " + userId);
                }
            } finally {
                // WAJIB: Tutup stream agar resource dilepas
                fos.close(); 
            }
            
        } catch (IOException e) {
            Log.e(TAG, "❌ Error I/O saat simpan avatar: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            Log.e(TAG, "❌ Error tak terduga saat simpan avatar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Mengambil avatar dari storage lokal dengan sistem Fallback
     */
    public static Bitmap getAvatarFromLocal(Context ctx, int userId) {
        try {
            File dir = new File(ctx.getFilesDir(), AVATAR_DIR);
            if (!dir.exists()) return null;
            
            // 1. Cek file PNG dulu (format baru kita)
            File pngFile = new File(dir, userId + ".png");
            if (pngFile.exists()) {
                Bitmap bmp = BitmapFactory.decodeFile(pngFile.getAbsolutePath());
                if (bmp != null) return bmp;
            }
            
            // 2. Fallback ke JPG lama (biar data user sebelumnya tidak hilang)
            File jpgFile = new File(dir, userId + ".jpg");
            if (jpgFile.exists()) {
                Bitmap bmp = BitmapFactory.decodeFile(jpgFile.getAbsolutePath());
                if (bmp != null) return bmp;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error saat load avatar lokal: " + e.getMessage());
        }
        
        return null;
    }
}
