package dk.bearware.gui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.util.List;
import java.util.Locale;

public class LocationHelper {
    private final FusedLocationProviderClient fusedLocationClient;
    private final Context context;

    public interface LocationResultCallback {
        void onResult(String wilayahTeks);
        void onError(String pesanError);
    }

    public LocationHelper(Context context) {
        this.context = context;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    @SuppressLint("MissingPermission")
    public void dapatkanKabupatenProvinsi(LocationResultCallback callback) {
        CancellationTokenSource cts = new CancellationTokenSource();

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
            .addOnSuccessListener(location -> {
                if (location != null) {
                    String wilayah = konversiKeWilayah(location.getLatitude(), location.getLongitude());
                    if (!TextUtils.isEmpty(wilayah)) {
                        callback.onResult(wilayah);
                    } else {
                        callback.onError("Nama wilayah tidak ditemukan.");
                    }
                } else {
                    callback.onError("Gagal mengunci sinyal GPS.");
                }
            })
            .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private String konversiKeWilayah(double lat, double lng) {
        Geocoder geocoder = new Geocoder(context, new Locale("id"));
        try {
            List<Address> daftarAlamat = geocoder.getFromLocation(lat, lng, 1);
            if (daftarAlamat != null && !daftarAlamat.isEmpty()) {
                Address alamat = daftarAlamat.get(0);
                
                String kabupaten = alamat.getSubAdminArea(); // Mengambil Kab/Kota
                String provinsi = alamat.getAdminArea();       // Mengambil Provinsi

                if (kabupaten != null && provinsi != null) {
                    return kabupaten + ", " + provinsi;
                } else if (kabupaten != null) {
                    return kabupaten;
                } else if (provinsi != null) {
                    return provinsi;
                }
            }
        } catch (Exception e) {
            Log.e("LocationHelper", "Reverse Geocoding gagal: " + e.getMessage());
        }
        return "";
    }
}
