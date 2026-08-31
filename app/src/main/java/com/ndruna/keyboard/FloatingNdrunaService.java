package com.ndruna.keyboard;

import android.app.Service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;

public class FloatingNdrunaService extends Service {

    private WindowManager windowManager;
    private TextView bubble;
    private ObjectAnimator blinkAnimator;

    private static final String CHANNEL_ID = "ndruna_bubble";

    private void startBubbleForeground() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "Bulle Ndruna",
                            NotificationManager.IMPORTANCE_LOW
                    );

            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        Intent launchIntent =
                new Intent(this, MainActivity.class);

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this,
                        300,
                        launchIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | (Build.VERSION.SDK_INT >= 23
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
                .setContentTitle("Bulle Ndruna")
                .setContentText("Ndruna est disponible")
                .setContentIntent(pendingIntent)
                .setOngoing(true);

        Notification notification = builder.build();

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                    2002,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            );
        } else {
            startForeground(2002, notification);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        startBubbleForeground();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }

        windowManager =
                (WindowManager) getSystemService(WINDOW_SERVICE);

        createBubble();
    }

    private void createBubble() {

        bubble = new TextView(this);

        bubble.setText("N");
        bubble.setTextSize(18);
        bubble.setTextColor(Color.WHITE);
        bubble.setGravity(Gravity.CENTER);

        GradientDrawable background =
                new GradientDrawable();

        background.setShape(GradientDrawable.OVAL);
        background.setColor(Color.rgb(20, 130, 80));
        background.setStroke(2, Color.WHITE);

        bubble.setBackground(background);
        bubble.setElevation(10);

        bubble.setOnClickListener(v -> {
            try {
                InputMethodManager imm =
                        (InputMethodManager) getSystemService(
                                INPUT_METHOD_SERVICE
                        );

                if (imm != null) {
                    imm.showInputMethodPicker();
                }

                android.widget.Toast.makeText(
                        FloatingNdrunaService.this,
                        "Sélecteur de clavier",
                        android.widget.Toast.LENGTH_SHORT
                ).show();

            } catch (Exception e) {
                Intent intent = new Intent(
                        Settings.ACTION_INPUT_METHOD_SETTINGS
                );
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });

        int overlayType;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            overlayType =
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            overlayType =
                    WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams params =
                new WindowManager.LayoutParams(
                        58,
                        58,
                        overlayType,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                        PixelFormat.TRANSLUCENT
                );

        params.gravity =
                Gravity.RIGHT | Gravity.BOTTOM;

        params.x = 12;
        params.y = 180;

        try {
            windowManager.addView(bubble, params);
        } catch (Exception e) {
            stopSelf();
            return;
        }

        // Clignotement doux.
        blinkAnimator =
                ObjectAnimator.ofFloat(
                        bubble,
                        "alpha",
                        1.0f,
                        0.35f,
                        1.0f
                );

        blinkAnimator.setDuration(900);
        blinkAnimator.setRepeatCount(
                ValueAnimator.INFINITE
        );

        blinkAnimator.start();
    }

    @Override
    public void onDestroy() {

        if (blinkAnimator != null) {
            blinkAnimator.cancel();
        }

        if (bubble != null &&
                windowManager != null) {

            try {
                windowManager.removeView(bubble);
            } catch (Exception ignored) {
            }
        }

        bubble = null;

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
