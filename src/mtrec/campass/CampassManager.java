package mtrec.campass;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import android.R.integer;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.widget.TextView;

public class CampassManager {
	
	private final SensorManager mSensorManager;
	private SensorEventListener mSensorEventListener;
	private Sensor magnetSensor, accelSensor, gyroscopeSensor;
	
	public float formerMagnetIntensity = 0;
	public float newMagnetIntensity = 0;
	//if started
	private boolean mDirectionStarted;
	//
	public float mReferenceDegree;
	//magnet degree
	public float magDegree;
	public float mDiff;
	public float[] mDiffArr = new float[windowSize];
	private static int windowSize = 50000;
	private int iter = 0;

	private HandlerThread mCalculationHandlerThread;
	private Handler mCalculationHandler;
	private List<CampassListener> mCampassListeners = new ArrayList<CampassListener>();
	private List<SensorEventListener> mSensorEventListeners = new ArrayList<SensorEventListener>();
	
	//magnet field tensity
	private double magneticIntensity = 0;
	//matrix
	private float[] accelerometerValues = new float[3];
	private float[] magneticFieldValues = new float[3];
	private float[] values = new float[3];
	private float[] Rmatrix = new float[9];
	//x, y, z degrees
	public float orientDegree = 0;
	public float tiltDegree = 0;
	public float rotateDegree = 0;
	
	public CampassManager(SensorManager sensorManager) {
		
		mSensorManager = sensorManager;
		magnetSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		accelSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		gyroscopeSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		initValues();
		
		//Handler Thread
		mCalculationHandlerThread = new HandlerThread("Campass Calculation Thread");
		mCalculationHandlerThread.start();
		mCalculationHandler = new Handler(mCalculationHandlerThread.getLooper());
		
		mSensorEventListener = new SensorEventListener() {
			
			private long startTime = 0;
			@Override
			public void onSensorChanged(SensorEvent event) {
				
				// TODO Auto-generated method stub
				// Magnet change
				if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
					
					magneticFieldValues = event.values;
	                //calculate intensity
	                magneticIntensity = Math.sqrt(
	                				magneticFieldValues[0]*magneticFieldValues[0] 
	                				+ magneticFieldValues[1]*magneticFieldValues[1] 
	                				+ magneticFieldValues[2]*magneticFieldValues[2]);
	                
	                formerMagnetIntensity = newMagnetIntensity;
	                newMagnetIntensity = (float) magneticIntensity;
	                //format change
	                /*DecimalFormat fnum = new  DecimalFormat("#########.#");
		            String str = fnum.format(magneticTense);
					magText.setText("Magnet Tensity: " + str);*/
	                float radian = (float) Math.atan2(event.values[0], event.values[1]);
					final float degree = (float) Math.toDegrees(radian);
					
					mCalculationHandler.post(new Runnable() {
						@Override
						public void run() {
							calibrateMagnetDegree(degree, true);
						}
					});
				}
				// acceler change
				else if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
					
					accelerometerValues = event.values;
				}
				// gyroscope change
				else if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
					
					long currentTime = System.currentTimeMillis();
					
					if (startTime > 0)
					{
						// z-axis rotation
						final float degree = (float) Math.toDegrees(event.values[2] * (currentTime - startTime) / 1000);
						
						mCalculationHandler.post(new Runnable() {
							@Override
							public void run() {
								calibrateMagnetDegree(degree, false);
							}
						});
						
					}
					startTime = currentTime;
				}
				
				//get R-matrix
	            SensorManager.getRotationMatrix(Rmatrix, null, accelerometerValues, magneticFieldValues);    
	            //get orientation
	            SensorManager.getOrientation(Rmatrix, values);
	            
	            //×ª»»Îª½Ç¶È
	            orientDegree = (float)Math.toDegrees(values[0]);
	            tiltDegree = (float)Math.toDegrees(values[1]);
	            rotateDegree = (float)Math.toDegrees(values[2]);
	            
	            double radian = Math.atan2(event.values[0], event.values[1]);
				float degree = (float) Math.toDegrees(radian);
				
				//mReferenceText.setText("mRefer: " + String.valueOf(mCampassManager.mReferenceDegree));
				//mDiffText.setText("mDiff: " + String.valueOf(mCampassManager.newMagnetTensity - mCampassManager.formerMagnetTensity));
				
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
		for (int i = 0; i < windowSize; i++) {
			mDiffArr[i] = 0;
		}
		mReferenceDegree = magDegree;
		mDirectionStarted = false;
	}
	
	//register all
	public void registerCampassListener() {
		mSensorManager.registerListener(mSensorEventListener, accelSensor, SensorManager.SENSOR_DELAY_GAME);
		mSensorManager.registerListener(mSensorEventListener, magnetSensor, SensorManager.SENSOR_DELAY_GAME);
		mSensorManager.registerListener(mSensorEventListener, gyroscopeSensor, SensorManager.SENSOR_DELAY_GAME);
	}
	
	//unregister all
	public void unregisterCampassListener() {
		mSensorManager.unregisterListener(mSensorEventListener);
	}
	
	public void addCampassListener(CampassListener campassListener) {
		mCampassListeners.add(campassListener);
	}
	
	@Override
	protected void finalize() throws Throwable {
		
		mCalculationHandlerThread.interrupt();
		mSensorManager.unregisterListener(mSensorEventListener);
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
				mDiff = 0;
				mDirectionStarted = true;
			} 
			else 
			{
				float diff = magDegree - mReferenceDegree;
				diff = normalize(diff);
				if(iter >= windowSize)
					iter = 0;
				
				mDiff -= mDiffArr[iter] / windowSize;
				mDiff += diff / windowSize;
				mDiffArr[iter++] = diff;
			}
		}
		else //gyroscope input
		{
			if (mDirectionStarted)
			{
				mReferenceDegree += degree;
				float diff = magDegree - mReferenceDegree;
				diff = normalize(diff);
				//take average
				if(iter >= windowSize)
					iter = 0;
				
				mDiff -= mDiffArr[iter] / windowSize;
				mDiff += diff / windowSize;
				mDiffArr[iter++] = diff;
			}
		}
		
		for (CampassListener campassListener : mCampassListeners) {
			campassListener.onDirectionChanged(magDegree, mReferenceDegree + mDiff);
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
		mReferenceDegree = magDegree = mDiff = 0;
	}

}
