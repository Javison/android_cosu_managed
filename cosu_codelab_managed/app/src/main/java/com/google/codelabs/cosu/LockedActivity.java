// Copyright 2016 Google Inc.
// 
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
//      http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.codelabs.cosu;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DevicePolicyManager;
import android.app.admin.SystemUpdatePolicy;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class LockedActivity extends Activity implements View.OnClickListener {

    private final String LOG_TAG = this.getClass().getSimpleName();

    private Button mBtnStopLock;
    private TextView mTvProperties;
    private String mPropertiesString;

    private ComponentName mAdminComponentName;
    private DevicePolicyManager mDevicePolicyManager;

    private static final String PREFS_FILE_NAME = "MyPrefsFile";
    private static final String PHOTO_PATH = "Photo Path";


    public static final String LOCK_ACTIVITY_KEY = "lock_activity";
    public static final int FROM_LOCK_ACTIVITY = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_locked);

        Log.d(LOG_TAG, "onCreate");

        initViewById();

        mDevicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        // Set text preferences to View
        setTextPreferencesToView();

        // Set Default COSU policy
        mAdminComponentName = DeviceAdminReceiver.getComponentName(this);
        mDevicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        if(mDevicePolicyManager.isDeviceOwnerApp(getPackageName())){
            setDefaultCosuPolicies(true);
        }
        else {
            Toast.makeText(getApplicationContext(), "NO es un DEVICE OWNER!", Toast.LENGTH_SHORT).show();
        }
    }

    private void initViewById() {
        mBtnStopLock = (Button) findViewById(R.id.btn_stop_lock_button);
        mBtnStopLock.setOnClickListener(this);
    }


    private void setDefaultCosuPolicies(boolean active){

        // Set user restrictions
        setUserRestriction(UserManager.DISALLOW_SAFE_BOOT, active);
        setUserRestriction(UserManager.DISALLOW_FACTORY_RESET, active);
        setUserRestriction(UserManager.DISALLOW_ADD_USER, active);
        setUserRestriction(UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA, active);
        setUserRestriction(UserManager.DISALLOW_ADJUST_VOLUME, active);

        // Disable keyguard and status bar
        mDevicePolicyManager.setKeyguardDisabled(mAdminComponentName, active);
        mDevicePolicyManager.setStatusBarDisabled(mAdminComponentName, active);

        // Enable STAY_ON_WHILE_PLUGGED_IN
        enableStayOnWhilePluggedIn(active);

        // Set system update policy
        if (active){
            mDevicePolicyManager.setSystemUpdatePolicy(mAdminComponentName, SystemUpdatePolicy.createWindowedInstallPolicy(60, 120));
        } else {
            mDevicePolicyManager.setSystemUpdatePolicy(mAdminComponentName, null);
        }

        // set this Activity as a lock task package
        mDevicePolicyManager.setLockTaskPackages(mAdminComponentName, active ? new String[]{getPackageName()} : new String[]{});

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MAIN);
        intentFilter.addCategory(Intent.CATEGORY_HOME);
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);

        if (active) {
            // set Cosu activity as home intent receiver so that it is started on reboot
            mDevicePolicyManager.addPersistentPreferredActivity(mAdminComponentName,
                                                                intentFilter,
                                                                new ComponentName(getPackageName(),
                                                                LockedActivity.class.getName()));
        } else {
            mDevicePolicyManager.clearPackagePersistentPreferredActivities(mAdminComponentName, getPackageName());
        }
    }

    private void setUserRestriction(String restriction, boolean disallow){
        Log.d(LOG_TAG, "setUserRestriction: " + restriction + " allow:" + disallow);
        if (disallow) {
            mDevicePolicyManager.addUserRestriction(mAdminComponentName, restriction);
        } else {
            mDevicePolicyManager.clearUserRestriction(mAdminComponentName, restriction);
        }
    }

    private void enableStayOnWhilePluggedIn(boolean enabled){
        Log.d(LOG_TAG, "enableStayOnWhilePluggedIn: " + enabled);
        if (enabled) {
            mDevicePolicyManager.setGlobalSetting(
                                                    mAdminComponentName,
                                                    Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                                                    Integer.toString(BatteryManager.BATTERY_PLUGGED_AC
                                                            | BatteryManager.BATTERY_PLUGGED_USB
                                                            | BatteryManager.BATTERY_PLUGGED_WIRELESS));
        } else {
            mDevicePolicyManager.setGlobalSetting(
                                                    mAdminComponentName,
                                                    Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                                                    "0"
            );
        }
    }



    @Override
    protected void onStart() {
        super.onStart();
        Log.d(LOG_TAG, "onStart - isLockTaskPermitted");
        // Start lock task mode if its not already active
        if(mDevicePolicyManager.isLockTaskPermitted(this.getPackageName())) {

            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (am.getLockTaskModeState() == ActivityManager.LOCK_TASK_MODE_NONE) {
                Log.d(LOG_TAG, "onStart - startLockTask");
                startLockTask();
            }
        }
    }

    @Override
    protected void onStop(){
        super.onStop();
        Log.d(LOG_TAG, "onStop - Save preferences");
        // Get editor object and make preference changes to save photo filepath
        SharedPreferences settings = getSharedPreferences(PREFS_FILE_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(PHOTO_PATH, mPropertiesString);
        editor.commit();
    }

    private void setTextPreferencesToView(){
        Log.d(LOG_TAG, "setTextPreferencesToView()");
        // Restore preferences
        SharedPreferences settings = getSharedPreferences(PREFS_FILE_NAME, 0);
        String savedPhotoPath = settings.getString(PHOTO_PATH, null);

        //Initialize the image view and display the picture if one exists
        Intent intent = getIntent();
        String passedPhotoPath = intent.getStringExtra(MainActivity.EXTRA_FILEPATH);

        if (passedPhotoPath != null) {
            mPropertiesString = passedPhotoPath;
        } else {
            mPropertiesString = savedPhotoPath;
        }

        Log.d(LOG_TAG, "mPropertiesString: " + mPropertiesString);

        if (mPropertiesString != null) {
            mTvProperties.setText(mPropertiesString);
        }
    }

    @Override
    public void onClick(View view) {
        Log.d(LOG_TAG, "onClick()");

        switch (view.getId()) {
            case R.id.btn_stop_lock_button:
                Log.d(LOG_TAG, "btn_stop_lock_button");

                ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
                if (am.getLockTaskModeState() == ActivityManager.LOCK_TASK_MODE_LOCKED) {
                    stopLockTask();
                }

                setDefaultCosuPolicies(false);

                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.putExtra(LOCK_ACTIVITY_KEY, FROM_LOCK_ACTIVITY);
                startActivity(intent);
                finish();

                break;

        }
    }
}
