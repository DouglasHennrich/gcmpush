package nl.vanvianen.android.gcm;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import com.google.android.gcm.GCMBaseIntentService;
import com.google.gson.Gson;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.util.TiRHelper;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class GCMIntentService extends GCMBaseIntentService {

    private static final String LCAT = "GCMIntentService";

    private static final String UNREGISTER_EVENT = "unregister";

    public GCMIntentService() {
        super("");
    }

    @Override
    public void onRegistered(Context context, String registrationId) {
        Log.d(LCAT, "Registered: " + registrationId);

        GCMModule.getInstance().sendSuccess(registrationId);
    }

    @Override
    public void onUnregistered(Context context, String registrationId) {
        Log.d(LCAT, "Unregistered");

        GCMModule.getInstance().fireEvent(UNREGISTER_EVENT, new HashMap<String, Object>());
    }

    private int getResource(String type, String name) {
        int icon = 0;
        if (name != null) {
            /* Remove extension from icon */
            int index = name.lastIndexOf(".");
            if (index > 0) {
                name = name.substring(0, index);
            }
            try {
                icon = TiRHelper.getApplicationResource(type + "." + name);
            } catch (TiRHelper.ResourceNotFoundException ex) {
                Log.e(LCAT, type + "." + name + " not found; make sure it's in platform/android/res/" + type);
            }
        }

        return icon;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onMessage(Context context, Intent intent) {
        Log.d(LCAT, "Push notification received");

        HashMap<String, Object> data = new HashMap<String, Object>();
        for (String key : intent.getExtras().keySet()) {
            Log.d(LCAT, "Message key: " + key + " value: " + intent.getExtras().getString(key));

            String eventKey = key.startsWith("data.") ? key.substring(5) : key;
            data.put(eventKey, intent.getExtras().getString(key));
        }

        /* Store data to be retrieved when resuming app as a JSON object, serialized as a String, otherwise
         * Ti.App.Properties.getString("com.activate.gcm.last_data") doesn't work. */
        JSONObject json = new JSONObject(data);
        TiApplication.getInstance().getAppProperties().setString(GCMModule.LAST_DATA, json.toString());

        /* Get settings from notification object */
        int smallIcon = 0;
        int largeIcon = 0;
        String sound = null;
        boolean vibrate = false;
        boolean insistent = false;
        String group = null;
        boolean localOnly = true;
        int priority = 0;

        Map<String, Object> notificationSettings = new Gson().fromJson(TiApplication.getInstance().getAppProperties().getString(GCMModule.NOTIFICATION_SETTINGS, null), Map.class);
        if (notificationSettings != null) {
            if (notificationSettings.get("smallIcon") instanceof String) {
                smallIcon = getResource("drawable", (String) notificationSettings.get("smallIcon"));
            } else {
                Log.e(LCAT, "Invalid setting smallIcon, should be String");
            }

            if (notificationSettings.get("largeIcon") instanceof String) {
                largeIcon = getResource("drawable", (String) notificationSettings.get("largeIcon"));
            } else {
                Log.e(LCAT, "Invalid setting largeIcon, should be String");
            }

            if (notificationSettings.get("sound") instanceof String) {
                if (notificationSettings.get("sound") != null) {
                    sound = (String) notificationSettings.get("sound");
                } else {
                    Log.e(LCAT, "Invalid setting sound, should be string");
                }
            }

            if (notificationSettings.get("vibrate") != null) {
                if (notificationSettings.get("vibrate") instanceof Boolean) {
                    vibrate = (Boolean) notificationSettings.get("vibrate");
                } else {
                    Log.e(LCAT, "Invalid setting vibrate, should be boolean");
                }
            }

            if (notificationSettings.get("insistent") != null) {
                if (notificationSettings.get("insistent") instanceof Boolean) {
                    insistent = (Boolean) notificationSettings.get("insistent");
                } else {
                    Log.e(LCAT, "Invalid setting insistent, should be boolean");
                }
            }

            if (notificationSettings.get("group") != null) {
                if (notificationSettings.get("group") instanceof String) {
                    group = (String) notificationSettings.get("group");
                } else {
                    Log.e(LCAT, "Invalid setting group, should be string");
                }
            }

            if (notificationSettings.get("localOnly") != null) {
                if (notificationSettings.get("localOnly") instanceof Boolean) {
                    localOnly = (Boolean) notificationSettings.get("localOnly");
                } else {
                    Log.e(LCAT, "Invalid setting localOnly, should be boolean");
                }
            }

            if (notificationSettings.get("priority") != null) {
                if (notificationSettings.get("priority") instanceof Integer) {
                    priority = (Integer) notificationSettings.get("priority");
                } else {
                    Log.e(LCAT, "Invalid setting priority, should be int, between PRIORITY_MIN (" + NotificationCompat.PRIORITY_MIN + ") and PRIORITY_MAX (" + NotificationCompat.PRIORITY_MAX + ")");
                }
            }

        } else {
            Log.d(LCAT, "No notification settings found");
        }

        /* If icon not found, default to appicon */
        if (smallIcon == 0) {
            smallIcon = getResource("drawable", "appicon");
        }

        /* If large icon not found, default to icon */
        if (largeIcon == 0) {
            largeIcon = smallIcon;
        }

        /* Create intent to (re)start the app's root activity */
        String pkg = TiApplication.getInstance().getApplicationContext().getPackageName();
        Intent launcherIntent = TiApplication.getInstance().getApplicationContext().getPackageManager().getLaunchIntentForPackage(pkg);
        launcherIntent.setFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, launcherIntent, PendingIntent.FLAG_ONE_SHOT);

        String title = (String) data.get("title");
        String message = (String) data.get("message");
        String ticker = (String) data.get("ticker");

        if (message == null) {
            Log.d(LCAT, "Message received but no message so will make this silent");
        } else {
            Log.d(LCAT, "Creating notification...");

            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), largeIcon);
            if (bitmap == null) {
                Log.d(LCAT, "No large icon found");
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setTicker(ticker)
                    .setContentIntent(contentIntent)
                    .setSmallIcon(smallIcon)
                    .setLargeIcon(bitmap);

            /* Name of group to group similar notifications together, can also be set in the push notification payload */
            if (data.get("group") != null) {
                group = (String) data.get("group");
            }
            if (group != null) {
                builder = builder.setGroup(group);
            }
            Log.i(LCAT, "Group: " + group);

            /* Whether notification should be for this device only or bridged to other devices, can also be set in the push notification payload */
            if (data.get("localOnly") != null) {
                localOnly = Boolean.getBoolean((String) data.get("localOnly"));
            }
            builder = builder.setLocalOnly(localOnly);
            Log.i(LCAT, "LocalOnly: " + localOnly);

            /* Specify notification priority, can also be set in the push notification payload */
            if (data.get("priority") != null) {
                priority = Integer.parseInt((String) data.get("priority"));
            }
            if (priority >= NotificationCompat.PRIORITY_MIN && priority <= NotificationCompat.PRIORITY_MAX) {
                builder.setPriority(priority);
                Log.i(LCAT, "Priority: " + priority);
            } else {
                Log.e(LCAT, "Ignored invalid priority " + priority);
            }

            Notification notification = builder.build();

            /* Sound, can also be set in the push notification payload */
            if (data.get("sound") != null) {
                Log.d(LCAT, "Sound specified in notification");
                sound = (String) data.get("sound");
            }

            if ("default".equals(sound)) {
                Log.i(LCAT, "Sound: default sound");
                notification.defaults |= Notification.DEFAULT_SOUND;
            } else if (sound != null) {
                Log.i(LCAT, "Sound " + sound);
                notification.sound = Uri.parse("android.resource://" + pkg + "/" + getResource("raw", sound));
            }

            /* Vibrate, can also be set in the push notification payload */
            if (data.get("vibrate") != null) {
                vibrate = Boolean.getBoolean((String) data.get("vibrate"));
            }
            if (vibrate) {
                notification.defaults |= Notification.DEFAULT_VIBRATE;
            }
            Log.i(LCAT, "Vibrate: " + vibrate);

            /* Insistent, can also be set in the push notification payload */
            if ("true".equals(data.get("insistent"))) {
                insistent = true;
            }
            if (insistent) {
                notification.flags |= Notification.FLAG_INSISTENT;
            }
            Log.i(LCAT, "Insistent: " + insistent);

            notification.defaults |= Notification.DEFAULT_LIGHTS;
            notification.flags |= Notification.FLAG_AUTO_CANCEL;

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(1, notification);
        }

        if (GCMModule.getInstance() != null) {
            GCMModule.getInstance().sendMessage(data);
        }
    }

    @Override
    public void onError(Context context, String errorId) {
        Log.e(LCAT, "Error: " + errorId);

        if (GCMModule.getInstance() != null) {
            GCMModule.getInstance().sendError(errorId);
        }
    }

    @Override
    public boolean onRecoverableError(Context context, String errorId) {
        Log.e(LCAT, "RecoverableError: " + errorId);

        if (GCMModule.getInstance() != null) {
            GCMModule.getInstance().sendError(errorId);
        }

        return true;
    }
}
