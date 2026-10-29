package dk.bearware.ofaid;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import androidx.core.app.ActivityCompat;
import java.util.List;
import java.util.Locale;

public class OfaLokasi {
    public static String getLokasiDaerah(Context context) {
        if (context == null) return "Tidak diketahui";
        
        // Cek izin PASIF - tidak memicu pop-up
        boolean adaIzin = ActivityCompat.checkSelfPermission(context, "android.permission.ACCESS_FINE_LOCATION") == 0 
                       || ActivityCompat.checkSelfPermission(context, "android.permission.ACCESS_COARSE_LOCATION") == 0;
        
        if (!adaIzin) return "📍 Izinkan lokasi"; // String aman untuk dikirim ke server
        
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Location loc = null;
        
        // Ambil last known location - tidak menunggu fix baru
        try {
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) 
                loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (loc == null && lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) 
                loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        } catch (SecurityException e) {
            return "📍 Izin lokasi ditolak";
        }
        
        if (loc == null) return "📍 Nyalakan lokasi"; // String aman
        
        // Konversi ke teks dengan privasi terjaga (HANYA KAB+PROV)
        try {
            Geocoder gcd = new Geocoder(context, new Locale("id", "ID"));
            List<Address> addresses = gcd.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);
                String kab = addr.getSubAdminArea(); // Kabupaten/Kota
                String prov = addr.getAdminArea();   // Provinsi
                
                if (kab != null && prov != null) 
                    return kab + ". " + prov; // Format parser-friendly: "Kab. Prov"
                if (kab != null) return kab;
                if (prov != null) return prov;
            }
        } catch (Exception e) {
            // Geocoder gagal - kembalikan string aman
        }
        
        return "📍 Tidak dapat dibaca";
    }
}
