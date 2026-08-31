package com.ndruna.keyboard;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final String CHANNEL_ID = "ndruna_activation";
    private static final int NOTIFICATION_ID = 1001;
    private static final int REQUEST_NOTIFICATION = 2001;

    private static final String PREFS = "ndruna_prefs";
    private static final String NOTIFICATION_SHOWN = "notification_shown";

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        createInterface();
        createNotificationChannel();

        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATION
            );

        } else {
            checkAndShowNotification();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Si l'utilisateur revient des paramètres
        // après avoir activé Ndruna, on supprime immédiatement
        // la notification.
        if (isNdrunaEnabled()) {
            cancelActivationNotification();
        }
    }

    private void createInterface() {

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(40, 40, 40, 40);

        TextView title = new TextView(this);
        title.setText("Clavier Ndruna");
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);

        TextView info = new TextView(this);
        info.setText(
                "Activez Clavier Ndruna dans la liste des claviers Android."
        );
        info.setTextSize(18);
        info.setGravity(Gravity.CENTER);
        info.setPadding(0, 30, 0, 30);

        Button activate = new Button(this);
        activate.setText("⚙ Activer Clavier Ndruna");

        activate.setOnClickListener(v -> openKeyboardSettings());

        layout.addView(title);
        layout.addView(info);
        layout.addView(activate);

        Button overlay = new Button(this);
        overlay.setText("Autoriser la bulle Ndruna");

        overlay.setOnClickListener(v -> {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                try {
                    Intent intent =
                            new Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    android.net.Uri.parse(
                                            "package:" + getPackageName()
                                    )
                            );

                    startActivity(intent);

                } catch (Exception e) {

                    Intent intent =
                            new Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION
                            );

                    startActivity(intent);
                }
            }
        });

        Button accessibility = new Button(this);
        accessibility.setText("♿ Autoriser la détection du clavier");

        accessibility.setOnClickListener(v -> {

            try {
                Intent intent =
                        new Intent(
                                Settings.ACTION_ACCESSIBILITY_SETTINGS
                        );

                startActivity(intent);

            } catch (Exception ignored) {
            }
        });

        layout.addView(overlay);
        layout.addView(accessibility);

        setContentView(layout);
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "Activation Clavier Ndruna",
                            NotificationManager.IMPORTANCE_DEFAULT
                    );

            channel.setDescription(
                    "Notification d'activation de Clavier Ndruna"
            );

            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void checkAndShowNotification() {

        // Si Ndruna est déjà activé,
        // aucune notification.
        if (isNdrunaEnabled()) {
            cancelActivationNotification();
            return;
        }

        // Notification déjà affichée pendant cette installation.
        if (prefs.getBoolean(NOTIFICATION_SHOWN, false)) {
            return;
        }

        showActivationNotification();

        prefs.edit()
                .putBoolean(NOTIFICATION_SHOWN, true)
                .apply();
    }

    private void showActivationNotification() {

        Intent settingsIntent =
                new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this,
                        100,
                        settingsIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT |
                        (Build.VERSION.SDK_INT >= 23
                                ? PendingIntent.FLAG_IMMUTABLE
                                : 0)
                );

        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Clavier Ndruna")
                .setContentText(
                        "Voulez-vous activer le clavier Ndruna ?"
                )
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .setPriority(Notification.PRIORITY_DEFAULT)
                .addAction(
                        android.R.drawable.ic_menu_manage,
                        "ACTIVER",
                        pendingIntent
                );

        NotificationManager manager =
                (NotificationManager)
                        getSystemService(NOTIFICATION_SERVICE);

        if (manager != null) {
            manager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    private void cancelActivationNotification() {

        NotificationManager manager =
                (NotificationManager)
                        getSystemService(NOTIFICATION_SERVICE);

        if (manager != null) {
            manager.cancel(NOTIFICATION_ID);
        }
    }

    private boolean isNdrunaEnabled() {

        String enabledMethods =
                Settings.Secure.getString(
                        getContentResolver(),
                        Settings.Secure.ENABLED_INPUT_METHODS
                );

        if (enabledMethods == null) {
            return false;
        }

        String fullServiceName =
                getPackageName()
                        + "/"
                        + NdrunaKeyboardService.class.getName();

        String shortServiceName =
                getPackageName()
                        + "/.NdrunaKeyboardService";

        return enabledMethods.contains(fullServiceName)
                || enabledMethods.contains(shortServiceName);
    }

    private void openKeyboardSettings() {

        Intent intent =
                new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS);

        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == REQUEST_NOTIFICATION) {

            if (Build.VERSION.SDK_INT < 33 ||
                    (grantResults.length > 0 &&
                     grantResults[0] == PackageManager.PERMISSION_GRANTED)) {

                checkAndShowNotification();
            }
        }
    }
}
