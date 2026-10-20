package dk.bearware.gui;

import android.widget.ImageView;
import androidx.annotation.DrawableRes;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class UserProfileManager {
    private static final String TAG = "UserProfileMgr";
    private final FirebaseFirestore db;

    public UserProfileManager() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Sinkronisasi data user saat login/masuk ke server
     * Pakai UID sebagai dokumen ID — konsisten dengan SplashActivity
     */
    public void syncUserProfile(FirebaseUser user, int teamTalkUserId) {
        if (user == null) return;

        String uid = user.getUid(); // 🆔 PAKAI UID = ID PERMANEN
        String displayName = user.getDisplayName() != null ? user.getDisplayName() : "Unknown";
        String email = user.getEmail() != null ? user.getEmail() : "";
        String photoUrl = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "";

        Map<String, Object> data = new HashMap<>();
        data.put("uid", uid);
        data.put("displayName", displayName);
        data.put("email", email);
        data.put("foto_profil_url", photoUrl);
        data.put("teamtalk_id", teamTalkUserId);
        data.put("lastLogin", System.currentTimeMillis());

        // ✅ Langsung pakai UID sebagai ID dokumen — tidak perlu cari
        db.collection("pengguna")
          .document(uid)
          .set(data)
          .addOnSuccessListener(aVoid -> 
              android.util.Log.d(TAG, "✅ Profil tersimpan: " + displayName + " | UID: " + uid))
          .addOnFailureListener(e -> 
              android.util.Log.e(TAG, "❌ Gagal simpan profil: " + e.getMessage()));
    }

    /**
     * Load avatar berdasarkan UID — PALING AMAN & AKURAT
     * Tambahkan parameter uid saat panggil dari adapter
     */
    public void loadAvatar(String uid, ImageView targetView, @DrawableRes int placeholderRes) {
        if (uid == null || uid.isEmpty()) {
            targetView.setImageResource(placeholderRes);
            return;
        }

        db.collection("pengguna")
          .document(uid) // ✅ Langsung akses pakai UID — cepat & pasti benar
          .get()
          .addOnSuccessListener(doc -> {
              if (doc.exists()) {
                  String url = doc.getString("foto_profil_url");
                  if (url != null && !url.isEmpty()) {
                      Glide.with(targetView.getContext())
                           .load(url)
                           .circleCrop()
                           .placeholder(placeholderRes)
                           .error(placeholderRes)
                           .into(targetView);
                      return;
                  }
              }
              targetView.setImageResource(placeholderRes);
          })
          .addOnFailureListener(e -> {
              android.util.Log.e(TAG, "❌ Gagal load avatar: " + e.getMessage());
              targetView.setImageResource(placeholderRes);
          });
    }
    
    // 🔄 Versi cadangan: cari pakai displayName (jika UID tidak tersedia di adapter)
    // HANYA pakai ini jika benar-benar tidak punya UID — bisa tertukar!
    public void loadAvatarByDisplayName(String displayName, ImageView targetView, @DrawableRes int placeholderRes) {
        if (displayName == null || displayName.isEmpty()) {
            targetView.setImageResource(placeholderRes);
            return;
        }

        db.collection("pengguna")
          .whereEqualTo("displayName", displayName)
          .limit(1)
          .get()
          .addOnSuccessListener(snapshot -> {
              if (!snapshot.isEmpty()) {
                  DocumentSnapshot doc = snapshot.getDocuments().get(0);
                  String url = doc.getString("foto_profil_url");
                  if (url != null && !url.isEmpty()) {
                      Glide.with(targetView.getContext())
                           .load(url)
                           .circleCrop()
                           .placeholder(placeholderRes)
                           .error(placeholderRes)
                           .into(targetView);
                      return;
                  }
              }
              targetView.setImageResource(placeholderRes);
          })
          .addOnFailureListener(e -> {
              android.util.Log.e(TAG, "❌ Gagal load avatar: " + e.getMessage());
              targetView.setImageResource(placeholderRes);
          });
    }
}
