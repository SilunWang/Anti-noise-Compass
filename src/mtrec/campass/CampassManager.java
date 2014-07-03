package mtrec.campass;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.widget.TextView;

public class CampassManager {
	
	private final SensorManager mSensorManager;
	private SensorEventListener magnetListener, gyroscopeListener;
	
	//if started
	private boolean mDirectionStarted;
	//
	public float mReferenceDegree;
	//magnet degree
	public float magDegree;
	public float mDiff;
	private int mSampleSize;

	private HandlerThread mCalculationHandlerThread;
	private Handler mCalculationHandler;
	private List<CampassListener> mCampassListeners = new ArrayList<CampassListener>();
	private List<SensorEventListener> mSensorEventListeners = new ArrayList<SensorEventListener>();
	
	//x, y, z degrees
	public float orientDegree = 0;
	public float tiltDegree = 0;
	public float rotateDegree = 0;
	
	public CampassManager(SensorManager sensorManager) {
		
		mSensorManager = sensorManager;
		initValues();
		
		//Handler Thread
		mCalculationHandlerThread = new HandlerThread("Campass Calculation Thread");
		mCalculationHandlerThread.start();
		mCalculationHandler = new Handler(mCalculationHandlerThread.getLooper());
		
		//magnetic field
		magnetListener = new SensorEventListener() {
			{
				mSensorEventListeners.add(this);
			}
			
			@Override
			public void onSensorChanged(SensorEvent event) {
				
				float radian = (float) Math.atan2(event.values[0], event.values[1]);
				final float degree = (float) Math.toDegrees(radian);
				
				mCalculationHandler.post(new Runnable() {
					@Override
					public void run() {
						calibrateMagnetDegree(degree, true);
					}
				});
	            
			}
			
			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
				
			}
			
		};
		
		//gyroscope change
		gyroscopeListener = new SensorEventListener() {
			{
				mSensorEventListeners.add(this);
			}
			private long startTime = 0;
			
			@Override
			public void onSensorChanged(SensorEvent event) {
				
				long currentTime = System.currentTimeMillis();
				
				if (startTime > 0)
				{
					// z-axis rotation
					final float degree = (float) Math.toDegrees(event.values[2] * (currentTime - startTime) / 1000);
					
					if(tiltDegree > 60 || tiltDegree < -60)
						resetNorth();
					if(rotateDegree > 60 || rotateDegree < -60)
						resetNorth();
					
					mCalculationHandler.post(new Runnable() {
						@Override
						public void run() {
							calibrateMagnetDegree(degree, false);
						}
					});
				}
				startTime = currentTime;
			}
			
			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
				// TODO Auto-generated method stub
				
			}
		};
		
	}
	
	//reset when extreme rotation
	public void resetNorth() {
		mDiff = 0;
		mSampleSize = 0;
		mReferenceDegree = magDegree;
		mDirectionStarted = false;
	}
	
	//register all
	public void registerCampassListener() {
		mSensorManager.registerListener(magnetListener, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_GAME);
		mSensorManager.registerListener(gyroscopeListener, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_GAME);
	}
	
	//unregister all
	public void unregisterCampassListener() {
		for (SensorEventListener sensorEventListener : mSensorEventListeners) {
			mSensorManager.unregisterListener(sensorEventListener);
		}
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
	
	//core algorithm: calibrate north direction
	private synchronized float calibrateMagnetDegree(float degree, boolean isReal) {
		
		if (isReal)	//magnetic input
		{
			magDegree = degree;
			if (!mDirectionStarted)
			{
				//initial state
				mReferenceDegree = degree;
				mDiff = mSampleSize = 0;
				mDirectionStarted = true;
			} 
			else 
			{
				float diff = magDegree - mReferenceDegree;
				normalize(diff);
				//take average
				mDiff = (mDiff * mSampleSize + diff) / ++mSampleSize;
				normalize(mDiff);
			}
		} 
		else //gyroscope input
		{
			if (mDirectionStarted) 
			{
				mReferenceDegree += degree;
				float diff = magDegree - mReferenceDegree;
				normalize(diff);
				//take average
				mDiff = (mDiff * mSampleSize + diff) / ++mSampleSize;
				normalize(mDiff);
			}
		}
		
		for (CampassListener campassListener : mCampassListeners) {
			campassListener.onDirectionChanged(mReferenceDegree + mDiff);
		}
		
		return mReferenceDegree + mDiff;
	}
	
	public float normalize(float degree) {
		while (degree > 180)
			degree -= 360;
		while (degree < -180)
			degree += 360;
		return degree;
	}
	
	//init degrees
	private void initValues() {
		mDirectionStarted = false;
		mReferenceDegree = magDegree = mDiff = mSampleSize = 0;
	}

}
