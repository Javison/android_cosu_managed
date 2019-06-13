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
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements View.OnClickListener {

    private final String LOG_TAG = this.getClass().getSimpleName();

    private Button mBtnTakePic;
    private Button mBtnLockTask;
    private TextView mTvPropeties;

    private String mPropertiesString;

    private PackageManager mPackageManager;
    private ComponentName mAdminComponentName;
    public DevicePolicyManager mDevicePolicyManager;

    private static final int REQUEST_UPDATE_PROPERTIES = 1;

    public static final String EXTRA_FILEPATH = "com.google.codelabs.cosu.EXTRA_FILEPATH";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(LOG_TAG, "onCreate");

        initViewById();

        // Retrieve Device Policy Manager so that we can check whether we can lock to screen later
        mDevicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        // Retrieve DeviceAdminReceiver ComponentName so we can make device management api calls later
        mAdminComponentName = DeviceAdminReceiver.getComponentName(this);

        // Retrieve Package Manager so that we can enable and disable LockedActivity
        mPackageManager = this.getPackageManager();

        // Check to see if started by LockActivity and disable LockActivity if so
        Intent intent = getIntent();
        if(intent.getIntExtra(LockedActivity.LOCK_ACTIVITY_KEY,0) == LockedActivity.FROM_LOCK_ACTIVITY){
            mDevicePolicyManager.clearPackagePersistentPreferredActivities(mAdminComponentName,getPackageName());
            mPackageManager.setComponentEnabledSetting(new ComponentName(getApplicationContext(), LockedActivity.class),
                                                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                                        PackageManager.DONT_KILL_APP);
        }

        mBtnLockTask.setEnabled(true);
    }


    private void initViewById() {

        mTvPropeties = (TextView) findViewById(R.id.tv_properties);
        mBtnLockTask = (Button) findViewById(R.id.btn_start_lock);
        mBtnLockTask.setOnClickListener(this);

        mBtnTakePic = (Button) findViewById(R.id.btn_save_preferences);
        mBtnTakePic.setOnClickListener(this);

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_UPDATE_PROPERTIES && resultCode == RESULT_OK) {
            // setImageToView();
            // Writte something on TextView
            mBtnLockTask.setEnabled(true);
        }
    }


    @Override
    public void onClick(View view) {
        Log.d(LOG_TAG, "onClick()");

        switch (view.getId()) {
            case R.id.btn_start_lock:
                Log.d(LOG_TAG, "btn_start_lock");

                if ( mDevicePolicyManager.isDeviceOwnerApp(getApplicationContext().getPackageName())) {

                    Intent lockIntent = new Intent(getApplicationContext(), LockedActivity.class);
                    lockIntent.putExtra(EXTRA_FILEPATH, mPropertiesString);

                    mPackageManager.setComponentEnabledSetting(new ComponentName(getApplicationContext(), LockedActivity.class),
                                                                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                                                                PackageManager.DONT_KILL_APP);
                    startActivity(lockIntent);
                    finish();

                } else {
                    Toast.makeText(getApplicationContext(), R.string.not_lock_whitelisted,Toast.LENGTH_SHORT).show();
                }

                break;

            case R.id.btn_save_preferences:
                Log.d(LOG_TAG, "btn_save_preferences");

                //Intent intent = new Intent(getApplicationContext(), );
                //intent.putExtra("SAMPLE_INTENT", "Dato a preferencias llegados de Intent");
                //startActivityForResult(intent, REQUEST_UPDATE_PROPERTIES);

                break;
        }

    }
}
