package com.example.tinanak_rendezvousgeolocalises;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ListSmsRDV extends Activity {

    ListView listViewSMS;
    public static List<String> smsResponses = new ArrayList<>(); // Faire la liste statique

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_sms);

        // Récupérer les réponses SMS stockées dans les préférences partagées
        SharedPreferences sharedPreferences = getSharedPreferences("SMS_PREFERENCES", Context.MODE_PRIVATE);
        smsResponses = new ArrayList<>(sharedPreferences.getStringSet("smsResponses", new HashSet<>()));

        // Récupérer la liste des SMS
        listViewSMS = findViewById(R.id.listViewSMS);

        // Adapter pour la liste des SMS
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, smsResponses);
        listViewSMS.setAdapter(adapter);

        // Ajouter un écouteur d'événements pour les éléments de la liste
        listViewSMS.setOnItemClickListener((parent, view, position, id) -> {

            String selectedSMS = (String) parent.getItemAtPosition(position);

            // Extraire le numéro de l'expéditeur et le contenu du message
            String[] smsParts = selectedSMS.split("\n");
            String sender = smsParts[0];
            String message = smsParts[1];

            // Renvoyer les détails du SMS à l'activité principale
            Intent resultIntent = new Intent(this, ValidationRDV.class);
            resultIntent.putExtra("sender", sender);
            resultIntent.putExtra("message", message);
            startActivity(resultIntent);
        });
    }

    public static void addSMSResponse(Context context, String message) {
        smsResponses.add(message);
        SharedPreferences sharedPreferences = context.getSharedPreferences("SMS_PREFERENCES", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet("smsResponses", new HashSet<>(smsResponses));
        editor.apply();
    }
}