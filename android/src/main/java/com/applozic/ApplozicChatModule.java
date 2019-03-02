package com.applozic;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.applozic.mobicomkit.Applozic;
import com.applozic.mobicomkit.ApplozicClient;
import com.applozic.mobicomkit.uiwidgets.ApplozicSetting;
import com.applozic.mobicomkit.api.account.register.RegistrationResponse;
import com.applozic.mobicomkit.api.account.register.RegisterUserClientService;
import com.applozic.mobicomkit.api.account.user.MobiComUserPreference;
import com.applozic.mobicomkit.api.account.user.User;
import com.applozic.mobicomkit.api.account.user.UserClientService;
import com.applozic.mobicomkit.api.account.user.UserLoginTask;
import com.applozic.mobicomkit.api.account.user.PushNotificationTask;
import com.applozic.mobicomkit.api.conversation.database.MessageDatabaseService;
import com.applozic.mobicomkit.api.notification.MobiComPushReceiver;
import com.applozic.mobicomkit.api.people.ChannelInfo;
import com.applozic.mobicomkit.channel.service.ChannelService;
import com.applozic.mobicomkit.uiwidgets.async.AlGroupInformationAsyncTask;
import com.applozic.mobicomkit.uiwidgets.async.ApplozicChannelAddMemberTask;
import com.applozic.mobicomkit.uiwidgets.conversation.ConversationUIService;
import com.applozic.mobicomkit.uiwidgets.conversation.activity.ConversationActivity;
import com.applozic.mobicommons.file.FileUtils;
import com.applozic.mobicommons.json.GsonUtils;
import com.applozic.mobicommons.people.channel.Channel;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableArray;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableType;

import com.applozic.mobicomkit.feed.AlResponse;
import com.applozic.mobicomkit.uiwidgets.async.ApplozicChannelRemoveMemberTask;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class ApplozicChatModule extends ReactContextBaseJavaModule implements ActivityEventListener {

    final String TAG = "ApplozicChatModule";

    public ApplozicChatModule(ReactApplicationContext reactContext) {
        super(reactContext);
        reactContext.addActivityEventListener(this);
    }

    @Override
    public String getName() {
        return "ApplozicChat";
    }

    public Map<String, String> recursivelyDeconstructReadableMap(ReadableMap readableMap) {
        ReadableMapKeySetIterator iterator = readableMap.keySetIterator();
        Map<String, String> deconstructedMap = new HashMap<>();
        while (iterator.hasNextKey()) {
            String key = iterator.nextKey();
            ReadableType type = readableMap.getType(key);
            switch (type) {
                case Null:
                    deconstructedMap.put(key, null);
                    break;
                case Boolean:
                    deconstructedMap.put(key, String.valueOf(readableMap.getBoolean(key)));
                    break;
                case Number:
                    deconstructedMap.put(key, String.valueOf(readableMap.getDouble(key)));
                    break;
                case String:
                    deconstructedMap.put(key, readableMap.getString(key));
                    break;
                case Map:
                    deconstructedMap.put(key, StringifyReadableMap(readableMap.getMap(key)));
                    break;
                // case Array:
                    // deconstructedMap.put(key, recursivelyDeconstructReadableArray(readableMap.getArray(key)));
                    // break;
                default:
                    throw new IllegalArgumentException("Could not convert object with key: " + key + ".");
            }
  
        }
        return deconstructedMap;
    }

    public String StringifyReadableMap(ReadableMap readableMap) {
        ReadableMapKeySetIterator iterator = readableMap.keySetIterator();
        Map<String, String> deconstructedMap = new HashMap<>();
        while (iterator.hasNextKey()) {
            String key = iterator.nextKey();
            ReadableType type = readableMap.getType(key);
            switch (type) {
                case Null:
                    deconstructedMap.put(key, null);
                    break;
                case Boolean:
                    deconstructedMap.put(key, String.valueOf(readableMap.getBoolean(key)));
                    break;
                case Number:
                    deconstructedMap.put(key, String.valueOf(readableMap.getDouble(key)));
                    break;
                case String:
                    deconstructedMap.put(key, readableMap.getString(key));
                    break;
                case Map:
                    deconstructedMap.put(key, StringifyReadableMap(readableMap.getMap(key)));
                    break;
                // case Array:
                    // deconstructedMap.put(key, recursivelyDeconstructReadableArray(readableMap.getArray(key)));
                    // break;
                default:
                    throw new IllegalArgumentException("Could not convert object with key: " + key + ".");
            }
  
        }
        return String.valueOf(deconstructedMap);
    }

    @ReactMethod
    public void processNotificationIfRequired(final ReadableMap data, final Callback callback) {
        final Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            callback.invoke("Activity doesn't exist", null);
            return;
        }

        Log.i(TAG, "Message data:" + data);
        Map<String, String> mapData = recursivelyDeconstructReadableMap(data);
        if (MobiComPushReceiver.isMobiComPushNotification(mapData)) {
            Log.i(TAG, "Applozic notification processing...");
            MobiComPushReceiver.processMessageAsync(currentActivity, mapData);
            callback.invoke(null, true);
            return;
        }
        Log.i(TAG, "Applozic not my notification");
        callback.invoke(null, false);
    }

    @ReactMethod
    public void updateToken(String token) {
        Activity currentActivity = getCurrentActivity();

        if (currentActivity == null) {
            Log.i("open updateToken  ", "Activity doesn't exist");
            return;
        }

        Log.i(TAG, "Found Registration Id:" + token);
        if (MobiComUserPreference.getInstance(currentActivity).isRegistered()) {
            Log.i(TAG, "user registered:");
            try {
                RegistrationResponse registrationResponse = new RegisterUserClientService(currentActivity).updatePushNotificationId(token);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "user not registered:");

        }
    }

    @ReactMethod
    public void login(final ReadableMap config, final Callback callback) {
        final Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            callback.invoke("Activity doesn't exist", null);
            return;
        }

        UserLoginTask.TaskListener listener = new UserLoginTask.TaskListener() {
            @Override
            public void onSuccess(RegistrationResponse registrationResponse, Context context) {
                //After successful registration with Applozic server the callback will come here
                if (MobiComUserPreference.getInstance(currentActivity).isRegistered()) {
                    String json = GsonUtils.getJsonFromObject(registrationResponse, RegistrationResponse.class);
                    callback.invoke(null, json);

                    PushNotificationTask pushNotificationTask = null;

                    PushNotificationTask.TaskListener listener = new PushNotificationTask.TaskListener() {
                        public void onSuccess(RegistrationResponse registrationResponse) {

                        }

                        @Override
                        public void onFailure(RegistrationResponse registrationResponse, Exception exception) {
                        }
                    };
                    String registrationId = Applozic.getInstance(context).getDeviceRegistrationId();
                    pushNotificationTask = new PushNotificationTask(registrationId, listener, currentActivity);
                    pushNotificationTask.execute((Void) null);
                } else {
                    String json = GsonUtils.getJsonFromObject(registrationResponse, RegistrationResponse.class);
                    callback.invoke(json, null);

                }

            }

            @Override
            public void onFailure(RegistrationResponse registrationResponse, Exception exception) {
                //If any failure in registration the callback  will come here
                callback.invoke(exception != null ? exception.toString() : "error", registrationResponse != null ? GsonUtils.getJsonFromObject(registrationResponse, RegistrationResponse.class) : "Unknown error occurred");

            }
        };

        User user = (User) GsonUtils.getObjectFromJson(GsonUtils.getJsonFromObject(config.toHashMap(), HashMap.class), User.class);
        new UserLoginTask(user, listener, currentActivity).execute((Void) null);
    }

    @ReactMethod
    public void openChat() {
        Activity currentActivity = getCurrentActivity();

        if (currentActivity == null) {
            Log.i("OpenChat Error ", "Activity doesn't exist");
            return;
        }

        Intent intent = new Intent(currentActivity, ConversationActivity.class);
        currentActivity.startActivity(intent);
    }

    @ReactMethod
    public void openChatWithUser(String userId) {
        Activity currentActivity = getCurrentActivity();

        if (currentActivity == null) {
            Log.i("open ChatWithUser  ", "Activity doesn't exist");
            return;
        }

        Intent intent = new Intent(currentActivity, ConversationActivity.class);

        if (userId != null) {

            intent.putExtra(ConversationUIService.USER_ID, userId);
            intent.putExtra(ConversationUIService.TAKE_ORDER, true);

        }
        currentActivity.startActivity(intent);
    }

    @ReactMethod
    public void openChatWithGroup(Integer groupId, final Callback callback) {

        Activity currentActivity = getCurrentActivity();
        Intent intent = new Intent(currentActivity, ConversationActivity.class);

        if (groupId != null) {

            ChannelService channelService = ChannelService.getInstance(currentActivity);
            Channel channel = channelService.getChannel(groupId);

            if (channel == null) {
                callback.invoke("Channel dose not exist", null);
                return;
            }
            intent.putExtra(ConversationUIService.GROUP_ID, channel.getKey());
            intent.putExtra(ConversationUIService.TAKE_ORDER, true);
            currentActivity.startActivity(intent);
            callback.invoke(null, "success");

        } else {
            callback.invoke("unable to launch group chat, check your groupId/ClientGroupId", "success");
        }

    }

    @ReactMethod
    public void openChatWithClientGroupId(String clientGroupId, final Callback callback) {

        Activity currentActivity = getCurrentActivity();
        Intent intent = new Intent(currentActivity, ConversationActivity.class);

        if (TextUtils.isEmpty(clientGroupId)) {

            callback.invoke("unable to launch group chat, check your groupId/ClientGroupId", "success");
        } else {

            ChannelService channelService = ChannelService.getInstance(currentActivity);
            Channel channel = channelService.getChannelByClientGroupId(clientGroupId);

            if (channel == null) {
                callback.invoke("Channel dose not exist", null);
                return;
            }
            intent.putExtra(ConversationUIService.GROUP_ID, channel.getKey());
            intent.putExtra(ConversationUIService.TAKE_ORDER, true);
            currentActivity.startActivity(intent);
            callback.invoke(null, "success");

        }

    }

    @ReactMethod
    public void logoutUser(final Callback callback) {

        Activity currentActivity = getCurrentActivity();

        if (currentActivity == null) {
            callback.invoke("Activity doesn't exist");
            return;
        }

        new UserClientService(currentActivity).logout();
        callback.invoke(null, "success");
    }

    //============================================ Group Method ==============================================

    /***
     *
     * @param config
     * @param callback
     */
    @ReactMethod
    public void createGroup(final ReadableMap config, final Callback callback) {

        final Activity currentActivity = getCurrentActivity();

        if (currentActivity == null) {

            callback.invoke("Activity doesn't exist", null);
            return;

        }

        if (TextUtils.isEmpty(config.getString("groupName"))) {

            callback.invoke("Group name must be passed", null);
            return;
        }

        List<String> channelMembersList = (List<String>) (Object) (config.getArray("groupMemberList").toArrayList());

        final ChannelInfo channelInfo = new ChannelInfo(config.getString("groupName"), channelMembersList);

        if (!TextUtils.isEmpty(config.getString("clientGroupId"))) {
            channelInfo.setClientGroupId(config.getString("clientGroupId"));
        }
        if (config.hasKey("type")) {
            channelInfo.setType(config.getInt("type")); //group type
        } else {
            channelInfo.setType(Channel.GroupType.PUBLIC.getValue().intValue()); //group type
        }
        channelInfo.setImageUrl(config.getString("imageUrl")); //pass group image link URL
        Map<String, String> metadata = (HashMap<String, String>) (Object) (config.getMap("metadata").toHashMap());
        channelInfo.setMetadata(metadata);

        new Thread(new Runnable() {
            @Override
            public void run() {

                AlResponse alResponse = ChannelService.getInstance(currentActivity).createChannel(channelInfo);
                Channel channel = null;
                if (alResponse.isSuccess()) {
                    channel = (Channel) alResponse.getResponse();
                }
                if (channel != null && channel.getKey() != null) {
                    callback.invoke(null, channel.getKey());
                } else {
                    if (alResponse.getResponse() != null) {
                        callback.invoke(GsonUtils.getJsonFromObject(alResponse.getResponse(), List.class), null);
                    } else if (alResponse.getException() != null) {
                        callback.invoke(alResponse.getException().getMessage(), null);
                    }
                }
            }
        }).start();
    }

    /**
     * @param config
     * @param callback
     */
    @ReactMethod
    public void addMemberToGroup(final ReadableMap config, final Callback callback) {

        final Activity currentActivity = getCurrentActivity();

        if (currentActivity == null) {

            callback.invoke("Activity doesn't exist", null);
            return;

        }

        Integer channelKey = null;
        String userId = config.getString("userId");

        if (!TextUtils.isEmpty(config.getString("clientGroupId"))) {
            Channel channel = ChannelService.getInstance(currentActivity).getChannelByClientGroupId(config.getString("clientGroupId"));
            channelKey = channel != null ? channel.getKey() : null;

        } else if (!TextUtils.isEmpty(config.getString("groupId"))) {
            channelKey = Integer.parseInt(config.getString("groupId"));
        }

        if (channelKey == null) {
            callback.invoke("groupId/clientGroupId not passed", null);
            return;
        }

        ApplozicChannelAddMemberTask.ChannelAddMemberListener channelAddMemberListener = new ApplozicChannelAddMemberTask.ChannelAddMemberListener() {
            @Override
            public void onSuccess(String response, Context context) {
                //Response will be "success" if user is added successfully
                Log.i("ApplozicChannelMember", "Add Response:" + response);
                callback.invoke(null, response);
            }

            @Override
            public void onFailure(String response, Exception e, Context context) {
                callback.invoke(response, null);

            }
        };

        ApplozicChannelAddMemberTask applozicChannelAddMemberTask = new ApplozicChannelAddMemberTask(currentActivity, channelKey, userId, channelAddMemberListener);//pass channel key and userId whom you want to add to channel
        applozicChannelAddMemberTask.execute((Void) null);
    }


    /**
     * @param config
     * @param callback
     */
    @ReactMethod
    public void removeUserFromGroup(final ReadableMap config, final Callback callback) {

        final Activity currentActivity = getCurrentActivity();

        if (currentActivity == null) {

            callback.invoke("Activity doesn't exist", null);
            return;

        }

        Integer channelKey = null;
        String userId = config.getString("userId");

        if (!TextUtils.isEmpty(config.getString("clientGroupId"))) {
            Channel channel = ChannelService.getInstance(currentActivity).getChannelByClientGroupId(config.getString("clientGroupId"));
            channelKey = channel != null ? channel.getKey() : null;

        } else if (!TextUtils.isEmpty(config.getString("groupId"))) {
            channelKey = Integer.parseInt(config.getString("groupId"));
        }

        if (channelKey == null) {
            callback.invoke("groupId/clientGroupId not passed", null);
            return;
        }

        ApplozicChannelRemoveMemberTask.ChannelRemoveMemberListener channelRemoveMemberListener = new ApplozicChannelRemoveMemberTask.ChannelRemoveMemberListener() {
            @Override
            public void onSuccess(String response, Context context) {
                callback.invoke(null, response);
                //Response will be "success" if user is removed successfully
                Log.i("ApplozicChannel", "remove member response:" + response);
            }

            @Override
            public void onFailure(String response, Exception e, Context context) {
                callback.invoke(response, null);

            }
        };

        ApplozicChannelRemoveMemberTask applozicChannelRemoveMemberTask = new ApplozicChannelRemoveMemberTask(currentActivity, channelKey, userId, channelRemoveMemberListener);//pass channelKey and userId whom you want to remove from channel
        applozicChannelRemoveMemberTask.execute((Void) null);
    }
    //======================================================================================================

    @ReactMethod
    public void getUnreadCountForUser(String userId, final Callback callback) {

        Activity currentActivity = getCurrentActivity();

        if (currentActivity == null) {
            callback.invoke("Activity doesn't exist", null);
            return;
        }

        int contactUnreadCount = new MessageDatabaseService(getCurrentActivity()).getUnreadMessageCountForContact(userId);
        callback.invoke(null, contactUnreadCount);

    }

    @ReactMethod
    public void getUnreadCountForChannel(ReadableMap config, final Callback callback) {
        Activity currentActivity = getCurrentActivity();

        if (currentActivity == null) {
            callback.invoke("Activity doesn't exist", null);
            return;
        }

        AlGroupInformationAsyncTask.GroupMemberListener listener = new AlGroupInformationAsyncTask.GroupMemberListener() {
            @Override
            public void onSuccess(Channel channel, Context context) {
                if (channel == null) {
                    callback.invoke("Channel dose not exist", null);
                } else {
                    callback.invoke(null, new MessageDatabaseService(context).getUnreadMessageCountForChannel(channel.getKey()));
                }
            }

            @Override
            public void onFailure(Channel channel, Exception e, Context context) {
                callback.invoke("Some error occurred : " + (e != null ? e.getMessage() : ""));
            }
        };

        if (config != null && config.hasKey("clientGroupId")) {
            new AlGroupInformationAsyncTask(currentActivity, config.getString("clientGroupId"), listener).execute();
        } else if (config != null && config.hasKey("groupId")) {
            new AlGroupInformationAsyncTask(currentActivity, config.getInt("groupId"), listener).execute();
        } else {
            callback.invoke("Invalid data sent");
        }
    }

    @ReactMethod
    public void setContactsGroupNameList(ReadableMap config) {
        Activity currentActivity = getCurrentActivity();
        if (currentActivity == null) {
            return;
        }
        List<String> contactGroupIdList = Arrays.asList((String[]) GsonUtils.getObjectFromJson(config.getString("contactGroupNameList"), String[].class));
        Set<String> contactGroupIdsSet = new HashSet<String>(contactGroupIdList);
        MobiComUserPreference.getInstance(currentActivity).setIsContactGroupNameList(true);
        MobiComUserPreference.getInstance(currentActivity).setContactGroupIdList(contactGroupIdsSet);
    }

    @ReactMethod
    public void totalUnreadCount(final Callback callback) {
        Activity currentActivity = getCurrentActivity();

        if (currentActivity == null) {
            callback.invoke("Activity doesn't exist", null);
            return;
        }

        int totalUnreadCount = new MessageDatabaseService(currentActivity).getTotalUnreadCount();
        callback.invoke(null, totalUnreadCount);

    }

    @ReactMethod
    public void isUserLogIn(final Callback successCallback) {
        Activity currentActivity = getCurrentActivity();
        MobiComUserPreference mobiComUserPreference = MobiComUserPreference.getInstance(currentActivity);
        successCallback.invoke(mobiComUserPreference.isLoggedIn());
    }

    @ReactMethod
    public void hideCreateGroupIcon(boolean hide) {
        Activity currentActivity = getCurrentActivity();

        if (hide) {
            ApplozicSetting.getInstance(currentActivity).hideStartNewGroupButton();
        } else {
            ApplozicSetting.getInstance(currentActivity).showStartNewGroupButton();
        }
    }

    @ReactMethod
    public void showOnlyMyContacts(boolean showOnlyMyContacts) {
        Activity currentActivity = getCurrentActivity();

        if (showOnlyMyContacts) {
            ApplozicClient.getInstance(currentActivity).enableShowMyContacts();
        } else {
            ApplozicClient.getInstance(currentActivity).disableShowMyContacts();
        }
    }

    @ReactMethod
    public void hideChatListOnNotification() {
        Activity currentActivity = getCurrentActivity();
        ApplozicClient.getInstance(currentActivity).hideChatListOnNotification();
    }

    @ReactMethod
    public void hideGroupSubtitle() {

    }

    @ReactMethod
    public void setAttachmentType(ReadableMap config) {
        Activity currentActivity = getCurrentActivity();
        Map<FileUtils.GalleryFilterOptions, Boolean> options = new HashMap<>();

        if (config.hasKey("allFiles")) {
            options.put(FileUtils.GalleryFilterOptions.ALL_FILES, config.getBoolean("allFiles"));
        }

        if (config.hasKey("imageVideo")) {
            options.put(FileUtils.GalleryFilterOptions.IMAGE_VIDEO, config.getBoolean("imageVideo"));
        }

        if (config.hasKey("image")) {
            options.put(FileUtils.GalleryFilterOptions.IMAGE_ONLY, config.getBoolean("image"));
        }

        if (config.hasKey("audio")) {
            options.put(FileUtils.GalleryFilterOptions.AUDIO_ONLY, config.getBoolean("audio"));
        }

        if (config.hasKey("video")) {
            options.put(FileUtils.GalleryFilterOptions.VIDEO_ONLY, config.getBoolean("video"));
        }

        ApplozicSetting.getInstance(currentActivity).setGalleryFilterOptions(options);
    }

    @Override
    public void onActivityResult(Activity activity, final int requestCode, final int resultCode, final Intent intent) {
    }

    @Override
    public void onNewIntent(Intent intent) {
    }

}