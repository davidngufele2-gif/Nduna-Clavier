package com.ndruna.keyboard;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityWindowInfo;

public class NdrunaAccessibilityService extends AccessibilityService {

    private boolean bubbleVisible = false;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        AccessibilityServiceInfo info =
                new AccessibilityServiceInfo();

        info.eventTypes =
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                        | AccessibilityEvent.TYPE_WINDOWS_CHANGED;

        info.feedbackType =
                AccessibilityServiceInfo.FEEDBACK_GENERIC;

        info.flags =
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;

        setServiceInfo(info);

        checkKeyboard();
    }

    @Override
    public void onAccessibilityEvent(
            AccessibilityEvent event) {

        checkKeyboard();
    }

    private void checkKeyboard() {

        boolean keyboardFound = false;

        try {
            for (AccessibilityWindowInfo window :
                    getWindows()) {

                if (window == null) {
                    continue;
                }

                if (window.getType() ==
                        AccessibilityWindowInfo.TYPE_INPUT_METHOD) {

                    keyboardFound = true;
                    break;
                }
            }
        } catch (Exception ignored) {
        }

        if (keyboardFound && !bubbleVisible) {
            showBubble();
        } else if (!keyboardFound && bubbleVisible) {
            hideBubble();
        }
    }

    private void showBubble() {

        Intent intent =
                new Intent(
                        this,
                        FloatingNdrunaService.class
                );

        try {
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }

            bubbleVisible = true;

        } catch (Exception ignored) {
        }
    }

    private void hideBubble() {

        try {
            stopService(
                    new Intent(
                            this,
                            FloatingNdrunaService.class
                    )
            );
        } catch (Exception ignored) {
        }

        bubbleVisible = false;
    }

    @Override
    public void onInterrupt() {
        hideBubble();
    }

    @Override
    public void onDestroy() {
        hideBubble();
        super.onDestroy();
    }
}
