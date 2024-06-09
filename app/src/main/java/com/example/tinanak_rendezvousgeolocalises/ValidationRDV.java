package com.example.tinanak_rendezvousgeolocalises;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ValidationRDV extends AppCompatActivity {

    TextView displaySMS;
    String phoneNumber; // Ajouter une variable pour le numéro de téléphone

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_validation_rdv);

        displaySMS = findViewById(R.id.display_sms);

        // Récupérer les détails du SMS de l'intention
        Intent intent = getIntent();
        String sender = intent.getStringExtra("sender");
        String message = intent.getStringExtra("message");

        // Afficher les détails du SMS dans le TextView
        displaySMS.setText("Expéditeur : " + sender + "\nMessage : " + message);

    }

    // Méthode appelée lorsque le bouton Accepter est cliqué
    public void acceptAction(View view) {
        sendResponse("accepted");
        MainActivity.sendSMS(this, phoneNumber, "Rendez-vous accepté"); // Utiliser la méthode statique sendSMS
    }

    // Méthode appelée lorsque le bouton Refuser est cliqué
    public void refuseAction(View view) {
        sendResponse("refused");
        MainActivity.sendSMS(this, phoneNumber, "Rendez-vous refusé"); // Utiliser la méthode statique sendSMS
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
}