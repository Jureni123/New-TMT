package dk.bearware.gui;

import android.content.Context;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Button;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import dk.bearware.TeamTalkBase;
import dk.bearware.backend.TeamTalkService;
import dk.bearware.ClientStatistics;

public class OfaidUiBridge {

    private final Context context;
    private BridgeDataProvider dataProvider;
    private String lokasiSayaCache = null;

    public interface BridgeDataProvider {
        TeamTalkBase getClient();
        TeamTalkService getService();
        String getCurrentChannelName();
        int getCurrentChannelId();
    }

    public OfaidUiBridge(Context context, BridgeDataProvider provider) {
        this.context = context;
        this.dataProvider = provider;
    }

    public void setLokasiCache(String lokasi) {
        this.lokasiSayaCache = lokasi;
    }

    public void setupWebView(WebView webView) {
        android.webkit.WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        webView.addJavascriptInterface(new JsBridge(), "AndroidBridge");
    }

    private class JsBridge {
        @JavascriptInterface
        public String getBridgeData() {
            try {
                String chanName = "Lobby";
                int chanId = 0;
                if (dataProvider != null && dataProvider.getCurrentChannelName() != null) {
                    chanName = dataProvider.getCurrentChannelName();
                    chanId = dataProvider.getCurrentChannelId();
                }

                String myLoc = (lokasiSayaCache != null) ? lokasiSayaCache : "Memuat Lokasi...";
                int ping = 0;
                long tx = 0;
                long rx = 0;

                if (dataProvider != null && dataProvider.getClient() != null) {
                    ClientStatistics stats = new ClientStatistics();
                    if (dataProvider.getClient().getClientStatistics(stats)) {
                        ping = (int) stats.nUdpPingTimeMs;
                        tx = stats.nUdpBytesSent / 1024;
                        rx = stats.nUdpBytesRecv / 1024;
                    }
                }

                String avatarUrl = "";
                try {
                    FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (fbUser != null && fbUser.getPhotoUrl() != null) {
                        avatarUrl = fbUser.getPhotoUrl().toString();
                    }
                } catch (Exception e) {
                    // abaikan
                }

                return "{" +
                    "\"channelName\":\"" + escapeJson(chanName) + "\"," +
                    "\"channelId\":\"CH " + chanId + "\"," +
                    "\"myLocation\":\"" + escapeJson(myLoc) + "\"," +
                    "\"myAvatarUrl\":\"" + escapeJson(avatarUrl) + "\"," +
                    "\"ping\":" + ping + "," +
                    "\"txKbs\":" + tx + "," +
                    "\"rxKbs\":" + rx + "," +
                    "\"isConnected\":true" +
                "}";
            } catch (Exception e) {
                return "{\"channelName\":\"Error\",\"myLocation\":\"Gagal load\",\"myAvatarUrl\":\"\"}";
            }
        }

        @JavascriptInterface
        public boolean isVoiceTransmitting() {
            if (dataProvider != null && dataProvider.getService() != null) {
                return dataProvider.getService().isVoiceTransmitting();
            }
            return false;
        }

        @JavascriptInterface
        public void triggerPtt() {
            // Tidak bisa langsung akses UI, MainActivity yang tangani
            // Bisa pakai interface terpisah jika dibutuhkan
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
