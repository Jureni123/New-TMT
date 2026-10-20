package dk.bearware.gui; // Sesuaikan dengan package MainActivity-mu

import android.widget.ImageView;
import androidx.annotation.DrawableRes;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class UserProfileManager {
    private static final String TAG = "UserProfileMgr";
    private final FirebaseFirestore db;

    public UserProfileManager() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Sinkronisasi data user saat login. 
     * Dipanggil dari onCmdMyselfLoggedIn di MainActivity.
     */
    public void syncUserProfile(FirebaseUser user, int teamTalkUserId) {
        if (user == null) return;

        Map<String, Object> data = new HashMap<>();
        data.put("displayName", user.getDisplayName() != null ? user.getDisplayName() : "Unknown");
        data.put("email", user.getEmail());
        data.put("foto_profil_url", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : "");
        data.put("teamtalk_id", teamTalkUserId);
        data.put("lastLogin", System.currentTimeMillis());

        // Cari dokumen berdasarkan email (kunci unik & stabil)
        db.collection("pengguna")
          .whereEqualTo("email", user.getEmail())
          .limit(1)
          .get()
          .addOnSuccessListener(snapshot -> {
              if (!snapshot.isEmpty()) {
                  // Update dokumen lama
                  String docId = snapshot.getDocuments().get(0).getId();
                  db.collection("pengguna").document(docId).update(data)
                    .addOnSuccessListener(a -> android.util.Log.d(TAG, "✅ Profil terupdate: " + user.getDisplayName()))
                    .addOnFailureListener(e -> android.util.Log.e(TAG, "❌ Gagal update: " + e.getMessage()));
              } else {
                  // Buat dokumen baru jika belum ada
                  db.collection("pengguna").add(data)
                    .addOnSuccessListener(a -> android.util.Log.d(TAG, "✅ Dokumen baru dibuat: " + user.getDisplayName()))
                    .addOnFailureListener(e -> android.util.Log.e(TAG, "❌ Gagal buat: " + e.getMessage()));
              }
          });
    }

    /**
     * Load avatar user lain berdasarkan DisplayName.
     * Dipanggil dari ChannelListAdapter.getView()
     */
    public void loadAvatar(String displayName, ImageView targetView, @DrawableRes int placeholderRes) {
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
                 // QueryDocumentSnapshot doc = snapshot.getDocuments().get(0);
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
              // Fallback ke icon default
              targetView.setImageResource(placeholderRes);
          })
          .addOnFailureListener(e -> {
              android.util.Log.e(TAG, "Gagal load avatar " + displayName + ": " + e.getMessage());
              targetView.setImageResource(placeholderRes);
          });
    }
}
