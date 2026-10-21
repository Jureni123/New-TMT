package dk.bearware.gui;

import android.content.Context;
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
     * ✅ Sinkronisasi data user saat login.
     * PERBAIKAN: Gunakan UID sebagai Document ID agar unik & stabil.
     */
    public void syncUserProfile(FirebaseUser user, int teamTalkUserId) {
        if (user == null) return;

        String uid = user.getUid();
        Map<String, Object> data = new HashMap<>();
        data.put("displayName", user.getDisplayName() != null ? user.getDisplayName() : "Unknown");
        data.put("email", user.getEmail());
        data.put("foto_profil_url", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
        data.put("teamtalk_id", teamTalkUserId);
        data.put("lastLogin", System.currentTimeMillis());

        // ✅ GUNAKAN UID SEBAGAI DOCUMENT ID (BUKAN ADD RANDOM)
        db.collection("pengguna").document(uid)
          .set(data) // set() otomatis create jika belum ada, update jika sudah ada
          .addOnSuccessListener(a -> android.util.Log.d(TAG, "✅ Profil tersimpan/update untuk UID: " + uid))
          .addOnFailureListener(e -> android.util.Log.e(TAG, "❌ Gagal simpan profil: " + e.getMessage()));
    }

    /**
     * ✅ Load avatar user lain berdasarkan UID (BUKAN DisplayName).
     * PERBAIKAN: Cari berdasarkan UID agar tidak tertukar kalau nama sama.
     */
    public void loadAvatarByUid(String uid, ImageView targetView, @DrawableRes int placeholderRes) {
        if (uid == null || uid.isEmpty()) {
            targetView.setImageResource(placeholderRes);
            return;
        }

        db.collection("pengguna").document(uid)
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
              android.util.Log.e(TAG, "Gagal load avatar UID " + uid + ": " + e.getMessage());
              targetView.setImageResource(placeholderRes);
          });
    }

    /**
     * Fallback: Load avatar berdasarkan DisplayName (hanya jika UID tidak tersedia).
     * ️ RISIKO: Bisa tertukar kalau ada nama sama. Gunakan hanya untuk kompatibilitas lama.
     */
    public void loadAvatarByName(String displayName, ImageView targetView, @DrawableRes int placeholderRes) {
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
              android.util.Log.e(TAG, "Gagal load avatar nama " + displayName + ": " + e.getMessage());
              targetView.setImageResource(placeholderRes);
          });
    }
}
