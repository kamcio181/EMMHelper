package com.kaszubski.kamil.emmhelper;

import android.app.enterprise.license.EnterpriseLicenseManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.kaszubski.kamil.emmhelper.utils.Constants;

public class LicenseReceiver extends BroadcastReceiver {
	private static final String TAG = LicenseReceiver.class.getSimpleName();
	public LicenseReceiver(){

	}

	@Override
	public void onReceive(Context context, Intent intent) {

		if (intent != null) {
			String action = intent.getAction();
			if (action == null) {
				Log.w(TAG, context.getString(R.string.null_action));
			}
			//If ELM activation result Intent is obtained
			else if (action
					.equals(EnterpriseLicenseManager.ACTION_LICENSE_STATUS)) {
				int errorCode = intent.getIntExtra(
						EnterpriseLicenseManager.EXTRA_LICENSE_ERROR_CODE,
						Constants.DEFAULT_ERROR);
				Log.i(TAG, "" + errorCode);
				//If ELM is successfully activated
				if(errorCode == EnterpriseLicenseManager.ERROR_NONE){
					context.sendBroadcast(new Intent(Constants.SUCCESS_ACTION));
				}
				//If ELM activation failed
				else {
					Intent errorIntent = new Intent(Constants.FAILURE_ACTION);
					errorIntent.putExtra(Constants.ERROR_CODE, errorCode);
					context.sendBroadcast(errorIntent);
				}
			}
		}
	}
}
