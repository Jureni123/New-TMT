/*---DiBuat oleh Jureni123 2026----*/
package dk.bearware.gui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class SplashActivity extends AppCompatActivity {
    private static final int RC_SIGN_IN = 9001;
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private ProgressBar progressBar;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();

        // Cek apakah sudah login sebelumnya
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // SUDAH LOGIN: Sinkronkan profil dulu, baru ke ServerList
            syncUserProfileIfNeeded(currentUser);
            return;
        }

        // BELUM LOGIN: Tampilkan layar login
        setContentView(R.layout.activity_splash);
        progressBar = findViewById(R.id.progress_bar);
        tvStatus = findViewById(R.id.tv_status);

        if (tvStatus != null) tvStatus.setText("Silakan login untuk melanjutkan");

        // Konfigurasi Google Sign-In
        try {
            String webClientId = getString(R.string.default_web_client_id);
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(webClientId)
                    .requestEmail()
                    .build();
            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        } catch (Exception e) {
            // Fallback jika string resource tidak ditemukan
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build();
            mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        }

        SignInButton signInButton = findViewById(R.id.sign_in_button);
        if (signInButton != null) {
            signInButton.setSize(SignInButton.SIZE_WIDE);
            signInButton.setOnClickListener(v -> signIn());
        }
    }

    private void signIn() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (tvStatus != null) tvStatus.setText("Membuka Google Sign-In...");
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            if (progressBar != null) progressBar.setVisibility(View.GONE);

            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            
            // Validasi hasil sign-in dari Google
            if (task == null || !task.isSuccessful()) {
                Exception exception = task != null ? task.getException() : null;
                String errorMsg = "Gagal mendapatkan akun Google";
                if (exception != null) {
                    errorMsg += ": " + exception.getMessage();
                }
                Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                
                // Reset state agar user bisa mencoba lagi
                if (mGoogleSignInClient != null) mGoogleSignInClient.signOut();
                return;
            }

            try {
                GoogleSignInAccount account = task.getResult();
                if (account == null) {
                    Toast.makeText(this, "Data akun Google kosong", Toast.LENGTH_SHORT).show();
                    if (mGoogleSignInClient != null) mGoogleSignInClient.signOut();
                    return;
                }

                String idToken = account.getIdToken();
                
                // PERBAIKAN UTAMA: Jangan pernah fallback diam-diam jika ID Token null
                if (idToken == null || idToken.isEmpty()) {
                    if (tvStatus != null) tvStatus.setText("Konfigurasi OAuth Error");
                    Toast.makeText(this, "Web Client ID salah atau SHA-1 belum terdaftar", Toast.LENGTH_LONG).show();
                    
                    // Paksa logout & reset UI agar tidak stuck di layar login
                    if (mGoogleSignInClient != null) mGoogleSignInClient.signOut();
                    return; 
                }

                // Lanjut autentikasi ke Firebase hanya jika ID Token valid
                firebaseAuthWithGoogle(idToken);

            } catch (Exception e) {
                Toast.makeText(this, "Error parsing akun: " + e.getMessage(), Toast.LENGTH_LONG).show();
                if (mGoogleSignInClient != null) mGoogleSignInClient.signOut();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        if (tvStatus != null) tvStatus.setText("Memverifikasi dengan Firebase...");

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // ✅ Sinkronkan profil ke Firestore setelah login berhasil
                            syncUserProfileIfNeeded(user);
                        } else {
                            goToServerList();
                        }
                    } else {
                        Toast.makeText(this, "Login Firebase gagal: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        if (mGoogleSignInClient != null) mGoogleSignInClient.signOut();
                    }
                });
    }

    /**
     * Sinkronkan profil user ke Firestore
     */
    private void syncUserProfileIfNeeded(FirebaseUser user) {
        if (user == null) {
            goToServerList();
            return;
        }

        if (tvStatus != null) tvStatus.setText("Menyimpan profil...");
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        UserProfileManager profileMgr = new UserProfileManager();
        // Generate random ID untuk teamTalkUserId (sesuai preferensimu)
        int teamTalkUserId = (int) (Math.random() * 900000) + 100000;
        
        profileMgr.syncUserProfile(user, teamTalkUserId);
        
        // Langsung ke ServerList (Firestore akan update di background)
        goToServerList();
    }

    private void goToServerList() {
        Intent intent = new Intent(this, ServerListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
