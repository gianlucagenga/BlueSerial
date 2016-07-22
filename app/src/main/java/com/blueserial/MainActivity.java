/*
 * Released under MIT License http://opensource.org/licenses/MIT
 * Copyright (c) 2013 Plasty Grove
 * Refer to file LICENSE or URL above for full text 
 */

package com.blueserial;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.blueserial.R;
import com.blueserial.CircularSeekBar;
import com.blueserial.CircularSeekBar.OnCircularSeekBarChangeListener;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ToggleButton;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static final String TAG = "BlueTest5-MainActivity";
	private int mMaxChars = 50000;//Default
	private UUID mDeviceUUID;
	private BluetoothSocket mBTSocket;
	private ReadInput mReadThread = null;

	private boolean mIsUserInitiatedDisconnect = false;
	private static final Map<String,String> levels = new HashMap<String, String>();

	// All controls here
	//private Button mBtnSend;
	private ToggleButton mSwitchONOFF;
	private ToggleButton mSwitchReadONOFF;
	private ToggleButton mSwitchNightONOFF;
	private CircularSeekBar seekbar;

	private boolean mIsBluetoothConnected = false;

	private BluetoothDevice mDevice;

	private ProgressDialog progressDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ActivityHelper.initialize(this);

		Intent intent = getIntent();
		Bundle b = intent.getExtras();
		mDevice = b.getParcelable(Homescreen.DEVICE_EXTRA);
		mDeviceUUID = UUID.fromString(b.getString(Homescreen.DEVICE_UUID));
		mMaxChars = b.getInt(Homescreen.BUFFER_SIZE);

		Log.d(TAG, "Ready");

		mSwitchONOFF = (ToggleButton) findViewById(R.id.switchONOFF);
		mSwitchReadONOFF = (ToggleButton) findViewById(R.id.toggleRead);
		mSwitchNightONOFF = (ToggleButton) findViewById(R.id.toggleNight);
		seekbar = (CircularSeekBar) findViewById(R.id.progressLight);

		mSwitchNightONOFF.setEnabled(false);
		mSwitchReadONOFF.setEnabled(false);
		mSwitchNightONOFF.setChecked(false);
		mSwitchReadONOFF.setChecked(false);
		seekbar.setClickable(false);
		seekbar.setProgress(0);
		levels.put("0", "a");
		levels.put("5", "b");
		levels.put("10", "c");
		levels.put("15", "d");
		levels.put("20", "e");
		levels.put("25", "f");
		levels.put("30", "g");
		levels.put("35", "h");
		levels.put("40", "i");
		levels.put("45", "j");
		levels.put("50", "k");
		levels.put("55", "l");
		levels.put("60", "m");
		levels.put("65", "n");
		levels.put("70", "o");
		levels.put("75", "p");
		levels.put("80", "q");
		levels.put("85", "r");
		levels.put("90", "s");
		levels.put("95", "t");
		levels.put("100", "u");

		mSwitchONOFF.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				try {
					if (mSwitchONOFF.isChecked()) {
						mBTSocket.getOutputStream().write(levels.get("50").getBytes());
						mSwitchNightONOFF.setEnabled(true);
						mSwitchReadONOFF.setEnabled(true);
						seekbar.setProgress(50);
					}
					else {
						mBTSocket.getOutputStream().write(levels.get("0").getBytes());
						mSwitchNightONOFF.setEnabled(false);
						mSwitchReadONOFF.setEnabled(false);
						mSwitchNightONOFF.setChecked(false);
						mSwitchReadONOFF.setChecked(false);
						seekbar.setEnabled(false);
						seekbar.setClickable(false);
						seekbar.setSelected(false);
						seekbar.setProgress(0);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

		mSwitchReadONOFF.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				try {
					if (mSwitchReadONOFF.isChecked()) {
						mBTSocket.getOutputStream().write(levels.get("95").getBytes());
						mSwitchNightONOFF.setChecked(false);
						seekbar.setProgress(95);
					}
					else {
					//	mBTSocket.getOutputStream().write(OFF_READ_CODE.getBytes());
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

		mSwitchNightONOFF.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				try {
					if (mSwitchNightONOFF.isChecked()) {
						mBTSocket.getOutputStream().write(levels.get("5").getBytes());
						mSwitchReadONOFF.setChecked(false);
						seekbar.setProgress(5);
					}
					else {
					//	mBTSocket.getOutputStream().write(OFF_NIGHT_CODE.getBytes());
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

		seekbar.setOnSeekBarChangeListener(new OnCircularSeekBarChangeListener() {
			@Override
			public void onProgressChanged(CircularSeekBar circularSeekBar, int progress, boolean fromUser) {
				try {
					mBTSocket.getOutputStream().write(levels.get((progress/5)*5 + "").getBytes());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void onStopTrackingTouch(CircularSeekBar seekBar) {

			}

			@Override
			public void onStartTrackingTouch(CircularSeekBar seekBar) {

			}
		});
	}

	@Override
	public void onBackPressed() {
		mIsUserInitiatedDisconnect = true;
		new DisConnectBT().execute();
	}

	private class ReadInput implements Runnable {

		private boolean bStop = false;
		private Thread t;

		public ReadInput() {
			t = new Thread(this, "Input Thread");
			t.start();
		}

		public boolean isRunning() {
			return t.isAlive();
		}

		@Override
		public void run() {
			InputStream inputStream;

			try {
				inputStream = mBTSocket.getInputStream();
				while (!bStop) {
					byte[] buffer = new byte[256];
					if (inputStream.available() > 0) {
						inputStream.read(buffer);
						int i = 0;
						/*
						 * This is needed because new String(buffer) is taking the entire buffer i.e. 256 chars on Android 2.3.4 http://stackoverflow.com/a/8843462/1287554
						 */
						for (i = 0; i < buffer.length && buffer[i] != 0; i++) {
						}
						final String strInput = new String(buffer, 0, i);

						/*
						 * If checked then receive text, better design would probably be to stop thread if unchecked and free resources, but this is a quick fix
						 */

					}
					Thread.sleep(500);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		public void stop() {
			bStop = true;
		}

	}

	private class DisConnectBT extends AsyncTask<Void, Void, Void> {

		@Override
		protected void onPreExecute() {
		}

		@Override
		protected Void doInBackground(Void... params) {

			if (mReadThread != null) {
				mReadThread.stop();
				while (mReadThread.isRunning())
					; // Wait until it stops
				mReadThread = null;

			}

			try {
				mBTSocket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			mIsBluetoothConnected = false;
			if (mIsUserInitiatedDisconnect) {
				finish();
			}
		}

	}

	private void msg(String s) {
		Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
	}

	@Override
	protected void onPause() {
		if (mBTSocket != null && mIsBluetoothConnected) {
			new DisConnectBT().execute();
		}
		Log.d(TAG, "Paused");
		super.onPause();
	}

	@Override
	protected void onResume() {
		if (mBTSocket == null || !mIsBluetoothConnected) {
			new ConnectBT().execute();
		}
		Log.d(TAG, "Resumed");
		super.onResume();
	}

	@Override
	protected void onStop() {
		Log.d(TAG, "Stopped");
		super.onStop();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
		super.onSaveInstanceState(outState);
	}

	private class ConnectBT extends AsyncTask<Void, Void, Void> {
		private boolean mConnectSuccessful = true;

		@Override
		protected void onPreExecute() {
			progressDialog = ProgressDialog.show(MainActivity.this, "Hold on", "Connecting");// http://stackoverflow.com/a/11130220/1287554
		}

		@Override
		protected Void doInBackground(Void... devices) {

			try {
				if (mBTSocket == null || !mIsBluetoothConnected) {
					mBTSocket = mDevice.createInsecureRfcommSocketToServiceRecord(mDeviceUUID);
					BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
					mBTSocket.connect();
				}
			} catch (IOException e) {
				// Unable to connect to device
				e.printStackTrace();
				mConnectSuccessful = false;
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);

			if (!mConnectSuccessful) {
				Toast.makeText(getApplicationContext(), "Could not connect to device. Is it a Serial device? Also check if the UUID is correct in the settings", Toast.LENGTH_LONG).show();
				finish();
			} else {
				msg("Connected to device");
				mIsBluetoothConnected = true;
				mReadThread = new ReadInput(); // Kick off input reader
			}

			progressDialog.dismiss();
		}

	}
}


