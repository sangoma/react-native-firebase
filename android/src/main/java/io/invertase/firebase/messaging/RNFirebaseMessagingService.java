package io.invertase.firebase.messaging;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.facebook.react.HeadlessJsTaskService;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import io.invertase.firebase.Utils;

public class RNFirebaseMessagingService extends FirebaseMessagingService {
  public static final String MESSAGE_EVENT = "messaging-message";
  public static final String REMOTE_NOTIFICATION_EVENT = "notifications-remote-notification";
  private static final String TAG = "RNFMessagingService";

  @Override
  public void onMessageReceived(RemoteMessage message) {
    Log.d(TAG, "onMessageReceived event received");
    Log.d(TAG, "onMessageReceived event message" + message);

    if (message.getNotification() != null) {
      // It's a notification, pass to the Notifications module
      Intent notificationEvent = new Intent(REMOTE_NOTIFICATION_EVENT);
      notificationEvent.putExtra("notification", message);

      // Broadcast it to the (foreground) RN Application
      LocalBroadcastManager
        .getInstance(this)
        .sendBroadcast(notificationEvent);
    } else {
      // It's a data message
      // If the app is in the foreground we send it to the Messaging module
      if (Utils.isAppInForeground(this.getApplicationContext())) {
        Intent messagingEvent = new Intent(MESSAGE_EVENT);
        messagingEvent.putExtra("message", message);
        // Broadcast it so it is only available to the RN Application
        LocalBroadcastManager
          .getInstance(this)
          .sendBroadcast(messagingEvent);
      } else {
        try {
          // If the app is in the background we send it to the Headless JS Service
            launchApplication(this.getApplicationContext() , "INCALL");

            if (Utils.isAppInForeground(this.getApplicationContext())) {
                Intent messagingEvent = new Intent(MESSAGE_EVENT);
                messagingEvent.putExtra("message", message);
                // Broadcast it so it is only available to the RN Application
                LocalBroadcastManager.getInstance(this).sendBroadcast(messagingEvent);
            } else {
                Intent headlessIntent = new Intent(this.getApplicationContext(), RNFirebaseBackgroundMessagingService.class);
                headlessIntent.putExtra("message", message);
                this.getApplicationContext().startService(headlessIntent);
                HeadlessJsTaskService.acquireWakeLockNow(this.getApplicationContext());
            }
        } catch (IllegalStateException ex) {
          Log.e(TAG, "Background messages will only work if the message priority is set to 'high'", ex);
        }
      }
    }
  }

    private void launchApplication(android.content.Context context, String codeID) {
    String packageName = context.getApplicationContext().getPackageName();
    Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    //launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    launchIntent.putExtra("codeID", codeID);
    context.startActivity(launchIntent);
    Log.i(TAG, "Start activity with extra: Launching: (" + packageName + ")  : " + codeID );
  }
}