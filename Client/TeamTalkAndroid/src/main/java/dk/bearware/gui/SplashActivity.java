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
            // SUDAH LOGIN: Langsung ke ServerList. 
            // BIARKAN MainActivity YANG URUS DATABASE NANTI.
            goToServerList();
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
            if (task == null || !task.isSuccessful()) {
                Toast.makeText(this, "Gagal mendapatkan akun Google", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                GoogleSignInAccount account = task.getResult();
                if (account == null) {
                    Toast.makeText(this, "Akun Google kosong", Toast.LENGTH_SHORT).show();
                    return;
                }

                String idToken = account.getIdToken();
                if (idToken == null || idToken.isEmpty()) {
                    goToServerList(); // Fallback langsung lanjut
                    return;
                }

                firebaseAuthWithGoogle(idToken);

            } catch (Exception e) {
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        if (tvStatus != null) tvStatus.setText("Memverifikasi dengan Firebase...");

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // LOGIN BERHASIL: Langsung ke ServerList.
                        // JANGAN SENTUH FIRESTORE DI SINI!
                        goToServerList();
                    } else {
                        Toast.makeText(this, "Login gagal", Toast.LENGTH_LONG).show();
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void goToServerList() {
        Intent intent = new Intent(this, ServerListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
