package com.mickaelg.androidforegroundnotification;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;

/**
 * Created by mickaelg on 03/05/2017.
 */
public class NotificationService extends Service {

    // region Properties

    private static final String TAG = NotificationService.class.getSimpleName();

    private static final int NOTIFICATION_ID = 1337;
    private static final String ACTION_START_SERVICE = "action_start_service";
    private static final String ACTION_STOP_SERVICE = "action_stop_service";
    private static final String ACTION_START_FOREGROUND = "action_start_foreground";
    private static final String ACTION_STOP_FOREGROUND = "action_stop_foreground";

    /**
     * Notification builder that is regularly updated as we receive new data. We keep a reference so we don't have to
     * rebuild the whole notification each time.
     */
    private NotificationCompat.Builder notifBuilder;

    private boolean foreground = false;

    // endregion


    // region Factory methods

    public static Intent getStartIntent(Context context) {
        Intent intent = new Intent(context, NotificationService.class);
        intent.setAction(ACTION_START_SERVICE);
        return intent;
    }

    public static Intent getStopIntent(Context context) {
        Intent intent = new Intent(context, NotificationService.class);
        intent.setAction(ACTION_STOP_SERVICE);
        return intent;
    }

    // endregion


    // region Lifecycle

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            handleAction(intent.getAction());
        }

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // endregion


    // region Notification

    private void handleAction(String action) {
        Log.d(TAG, "Handle action: %s" + action);

        if (action.equals(ACTION_START_SERVICE)) {
            startDeviceControlsNotification();
        } else if (action.equals(ACTION_STOP_SERVICE)) {
            stopDeviceControlsNotification();
        } else if (action.equals(ACTION_START_FOREGROUND)) {
            foreground = true;
            updateNotification();
            showNotification(notifBuilder, foreground);
        } else if (action.equals(ACTION_STOP_FOREGROUND)) {
            foreground = false;
            updateNotification();
            showNotification(notifBuilder, foreground);
        }
    }

    private void startDeviceControlsNotification() {
        Log.d(TAG, "Start device controls notification");

        // Build and show the first notification
        updateNotification();
        showNotification(notifBuilder, foreground);
    }

    private void stopDeviceControlsNotification() {
        Log.d(TAG, "Stop device controls notification");

        // Remove notification
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);

        // Stop the service
        stopForeground(true);
        stopSelf();
    }

    private void updateNotification() {
        if (notifBuilder == null) {
            notifBuilder = getBaseNotificationBuilder();
        }

        notifBuilder.setContentTitle("Never Gonna Give You Up")
                .setContentText("Rick Astley");

        // Remove previous actions
        notifBuilder.mActions.clear();

        // Add one action
        Context context = getApplicationContext();
        if (foreground) {
            Intent intent = new Intent(context, NotificationService.class);
            intent.setAction(ACTION_STOP_FOREGROUND);
            PendingIntent pendingIntent = PendingIntent.getService(context, 1, intent, 0);
            notifBuilder.addAction(android.R.drawable.ic_media_pause, "Pause", pendingIntent);
        } else {
            Intent intent = new Intent(context, NotificationService.class);
            intent.setAction(ACTION_START_FOREGROUND);
            PendingIntent pendingIntent = PendingIntent.getService(context, 1, intent, 0);
            notifBuilder.addAction(android.R.drawable.ic_media_play, "Play", pendingIntent);
        }
    }

    /**
     * Get the base notification builder that is used by every device controls notification.
     *
     * @return Base notification builder
     */
    private NotificationCompat.Builder getBaseNotificationBuilder() {
        Intent notificationIntent = MainActivity.newIntent(getApplicationContext());
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 7, notificationIntent, 0);

        Intent deleteIntent = getStopIntent(getApplicationContext());
        PendingIntent deletePendingIntent = PendingIntent.getService(getApplicationContext(), 1, deleteIntent, 0);

        return new android.support.v7.app.NotificationCompat.Builder(getApplicationContext())
                // Apply the media style template
                .setStyle(new android.support.v7.app.NotificationCompat.MediaStyle()
                        // We show the Like, Play/Pause and Next button in compact view
                        .setShowActionsInCompactView(0)
                        // Show a cancel button for old Android versions
                        .setShowCancelButton(true)
                        // Intent to fire when the cancel button is clicked
                        .setCancelButtonIntent(deletePendingIntent))
                // App icon
                .setSmallIcon(R.mipmap.ic_launcher)
                // Color app icon, app name and actions icons
                .setColor(ResourcesCompat.getColor(getResources(), android.R.color.holo_orange_dark, getTheme()))
                // Show controls on lock screen even when user hides sensitive content
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                // Don't show the notification time
                .setShowWhen(false)
                // Set the Intent to launch when the notification is clicked
                .setContentIntent(pendingIntent)
                // Set the Intent to launch when the notification is deleted
                .setDeleteIntent(deletePendingIntent);
    }

    private void showNotification(NotificationCompat.Builder notifBuilder, boolean foreground) {
        Log.d(TAG, "Show notification. Set foreground: " + foreground);

        if (foreground) {
            startForeground(NOTIFICATION_ID, notifBuilder.build());
        } else {
            stopForeground(false);

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(NOTIFICATION_ID, notifBuilder.build());
        }
    }

    // endregion

}
