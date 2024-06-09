package com.example.tinanak_rendezvousgeolocalises;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class ValidationRDV extends AppCompatActivity implements OnMapReadyCallback {

    private MapView mapView;
    private LatLng rendezvousLocation;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_validation_rdv);

        // Récupérer les données de l'intention
        Intent intent = getIntent();
        double latitude = intent.getDoubleExtra("latitude", 0.0);
        double longitude = intent.getDoubleExtra("longitude", 0.0);
        rendezvousLocation = new LatLng(latitude, longitude);
        String rendezvousDate = intent.getStringExtra("date");

        // Initialiser les vues
        TextView displayDate = findViewById(R.id.display_date);
        mapView = findViewById(R.id.map_view);

        // Mettre à jour les vues avec les données
        displayDate.setText("Date et heure: " + rendezvousDate);

        // Initialiser la carte
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        googleMap.addMarker(new MarkerOptions().position(rendezvousLocation).title("Lieu du rendez-vous"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(rendezvousLocation, 15));
    }

    // Méthode appelée lorsque le bouton Accepter est cliqué
    public void acceptAction(View view) {
        sendResponse("accepted");
    }

    // Méthode appelée lorsque le bouton Refuser est cliqué
    public void refuseAction(View view) {
        sendResponse("refused");
    }

    // Envoyer la réponse à la personne ayant initié le rendez-vous
    private void sendResponse(String response) {
        // Logic to send the response back to the inviter (e.g., via SMS, email, or a server call)
        // Example:
        String message = response.equals("accepted") ? "Rendez-vous accepté" : "Rendez-vous refusé";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

        // Terminer l'activité
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
