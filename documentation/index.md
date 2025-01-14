# Titanium Module for Google Cloud Messaging Push Notifications for Android #

A Titanium module for registering a device with Google Cloud Messaging and handling push notifications sent to the device.

1. Install the module as usual in Titanium Studio by downloading the [zip file](https://github.com/morinel/gcmpush/releases/download/1.1/nl.vanvianen.android.gcm-android-1.1.zip) or use ```gittio install nl.vanvianen.android.gcm```
1. Refer to the example for possibilities
1. Send a server push notification with your preferred server-side technology to the registrationId returned while registering your device.
1. The callback you specified will then be called

This module does not require any tiapp.xml properties, all configuration is done in Javascript.

## Example server-side code ##

```java
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.MulticastResult;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;

...

public void sendPush() {
    Sender sender = new Sender(APIKEY);
    Message msg = new Message.Builder()
        .addData("title", title)
        .addData("message", message)
        .addData("sound", "mysound.mp3")
        .addData("vibrate", "true")
        .addData("insistent", "true")
        .addData("priority", "2")
        .addData("localOnly", "false")
        .addData("group", "mygroup")
        .build();
    try {
        List<String> list = new ArrayList<>(REGISTRATION_IDS);
        MulticastResult result = sender.send(msg, list, 1);
        log.info("Total = " + result.getTotal() + ", success = " + result.getSuccess() + ", failure = " + result.getFailure());
    } catch (IOException ex) {
        log.error("Cannot send Android push notification: " + ex.getMessage(), ex);
    }
}
```

## Notification settings ##

See the [example](https://github.com/morinel/gcmpush/blob/master/example/app.js) for an overview of how to specify the below settings.

The following settings can be specified:

1. **smallIcon**: the tiny icon shown at the top of the screen, see this [stackoverflow question](http://stackoverflow.com/questions/28387602/notification-bar-icon-turns-white-in-android-5-lollipop) for details. The file should be placed in the ```platform/android/res/drawable``` directory.
1. **largeIcon**: the large icon shown in the notification bar. If not specified your appicon will be used. The file should be placed in the ```platform/android/res/drawable``` directory.
1. **sound**: the sound file to play while receiving the notification or 'default' for the default sound. The sound file should be placed in the ```platform/android/res/raw``` directory.
1. **vibrate** (true / false): whether vibration should be on, default false.
1. **insistent** (true / false): whether the notification should be [insistent](http://developer.android.com/reference/android/app/Notification.html#FLAG_INSISTENT), default false.
1. **group**: name of group to group similar notifications together, default null.
1. **localOnly** (true / false): whether this notification should be bridged to other devices (false) or is only relevant to this device (true), default true.
1. **priority**: specifies the priority of the notification, should be between [PRIORITY_MIN](http://developer.android.com/reference/android/support/v4/app/NotificationCompat.html#PRIORITY_MIN) and [PRIORITY_MAX](http://developer.android.com/reference/android/support/v4/app/NotificationCompat.html#PRIORITY_MAX), default 0.

The settings sound, vibrate, insistent, group, localOnly and priority can also be set as data in the push message being received (see the server-side example above).

If the app is not active when the notification is received, use gcm.getLastData() to retrieve the contents of the notification and act accordingly to start or resume the app in a suitable way. If you're done, call gcm.clearLastData(), otherwise the same logic will happen when resuming the app again, see the [example](https://github.com/morinel/gcmpush/blob/master/example/app.js).
