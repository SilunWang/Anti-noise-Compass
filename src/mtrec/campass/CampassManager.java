package mtrec.campass;

import java.util.ArrayList;
import java.util.List;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;

public class CampassManager {
	
	private final SensorManager mSensorManager;
	private boolean mDirectionStarted;
	private float mReferenceDegree, mDegree, mDiff;
	private int mSampleSize;
	private HandlerThread mCalculationHandlerThread;
	private Handler mCalculationHandler;
	private List<CampassListener> mCampassListeners = new ArrayList<CampassListener>();
	private List<SensorEventListener> mSensorEventListeners = new ArrayList<SensorEventListener>();
	
	public CampassManager(SensorManager sensorManager) {
		mSensorManager = sensorManager;
		initValues();
		mCalculationHandlerThread = new HandlerThread("Campass Calculation Thread");
		mCalculationHandlerThread.start();
		mCalculationHandler = new Handler(mCalculationHandlerThread.getLooper());
		mSensorManager.registerListener(new SensorEventListener() {
			{
				mSensorEventListeners.add(this);
			}
			@Override
			public void onSensorChanged(SensorEvent event) {
				double radian = Math.atan2(event.values[0], event.values[1]);
				final float degree = (float) (radian * 180 / Math.PI);
				mCalculationHandler.post(new Runnable() {
					@Override
					public void run() {
						changeInMagneticField(degree, true);
					}
				});
			}
			
			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
			}
		}, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(new SensorEventListener() {
			{
				mSensorEventListeners.add(this);
			}
			private long startTime = 0;
			@Override
			public void onSensorChanged(SensorEvent event) {
				long currentTime = System.currentTimeMillis();
				if (startTime > 0) {
					final float degree = (float) (event.values[2] * (currentTime - startTime) / 1000 * 180 / Math.PI);
					mCalculationHandler.post(new Runnable() {
						@Override
						public void run() {
							changeInMagneticField(degree, false);
						}
					});
				}
				startTime = currentTime;
			}
			
			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
			}
		}, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_NORMAL);
	}
	
	public void addCampassListener(CampassListener campassListener) {
		mCampassListeners.add(campassListener);
	}
	
	@Override
	protected void finalize() throws Throwable {
		mCalculationHandlerThread.interrupt();
		for (SensorEventListener sensorEventListener : mSensorEventListeners) {
			mSensorManager.unregisterListener(sensorEventListener);
		}
		super.finalize();
	}
	
	private synchronized void changeInMagneticField(float degree, boolean isReal) {
		if (isReal) {
			mDegree = degree;
			if (!mDirectionStarted) {
				mReferenceDegree = degree;
				mDiff = mSampleSize = 0;
				mDirectionStarted = true;
			} else {
				float diff = mDegree - mReferenceDegree;
				while (diff > 180) diff -= 360;
				while (diff < 180) diff += 360;
				mDiff = (mDiff * mSampleSize + diff) / ++mSampleSize;
			}
		} else {
			if (mDirectionStarted) {
				mReferenceDegree += degree;
				float diff = mDegree - mReferenceDegree;
				while (diff > 180) diff -= 360;
				while (diff < 180) diff += 360;
				mDiff = (mDiff * mSampleSize + diff) / ++mSampleSize;
			}
		}
		for (CampassListener campassListener : mCampassListeners) {
			campassListener.onDirectionChanged(mReferenceDegree + mDiff);
		}
	}
	
	private void initValues() {
		mDirectionStarted = false;
		mReferenceDegree = mDegree = mDiff = mSampleSize = 0;
	}

}
