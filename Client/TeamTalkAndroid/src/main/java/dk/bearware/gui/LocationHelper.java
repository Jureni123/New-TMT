package dk.bearware.gui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.Locale;

public class LocationHelper {
    private final LocationManager locationManager;
    private final Context context;
    private LocationResultCallback currentCallback;

    public interface LocationResultCallback {
        void onResult(String wilayahTeks);
        void onError(String pesanError);
    }

    public LocationHelper(Context context) {
        this.context = context;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public void dapatkanKabupatenProvinsi(LocationResultCallback callback) {
        this.currentCallback = callback;

        // Cek izin dulu (wajib karena kita hapus @SuppressLint tadi)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            callback.onError("Izin lokasi belum diberikan. Silakan izinkan di pengaturan.");
            return;
        }

        try {
            // Coba ambil lokasi terakhir dulu (cepat, tanpa nunggu GPS fix)
            Location lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnown == null) {
                lastKnown = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (lastKnown != null) {
                String wilayah = konversiKeWilayah(lastKnown.getLatitude(), lastKnown.getLongitude());
                if (!TextUtils.isEmpty(wilayah)) {
                    callback.onResult(wilayah);
                } else {
                    callback.onError("Nama wilayah tidak ditemukan.");
                }
            } else {
                // Jika tidak ada lokasi terakhir, request update sekali saja
                locationManager.requestSingleUpdate(
                    LocationManager.GPS_PROVIDER,
                    new LocationListener() {
                        @Override
                        public void onLocationChanged(Location location) {
                            String wilayah = konversiKeWilayah(location.getLatitude(), location.getLongitude());
                            if (!TextUtils.isEmpty(wilayah)) {
                                currentCallback.onResult(wilayah);
                            } else {
                                currentCallback.onError("Nama wilayah tidak ditemukan.");
                            }
                            locationManager.removeUpdates(this); // Stop listening setelah dapat
                        }

                        @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
                        @Override public void onProviderEnabled(String provider) {}
                        @Override public void onProviderDisabled(String provider) {
                            currentCallback.onError("GPS dinonaktifkan.");
                        }
                    },
                    null
                );
                
                // Timeout 10 detik kalau GPS tidak dapat sinyal
                new android.os.Handler().postDelayed(() -> {
                    if (currentCallback != null) {
                        currentCallback.onError("Timeout: Gagal mendapatkan lokasi dalam 10 detik.");
                    }
                }, 10000);
            }
        } catch (Exception e) {
            Log.e("LocationHelper", "Error mengambil lokasi: " + e.getMessage());
            callback.onError("Error: " + e.getMessage());
        }
    }

    private String konversiKeWilayah(double lat, double lng) {
        Geocoder geocoder = new Geocoder(context, new Locale("id"));
        try {
            List<Address> daftarAlamat = geocoder.getFromLocation(lat, lng, 1);
            if (daftarAlamat != null && !daftarAlamat.isEmpty()) {
                Address alamat = daftarAlamat.get(0);
                String kabupaten = alamat.getSubAdminArea();
                String provinsi = alamat.getAdminArea();

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
