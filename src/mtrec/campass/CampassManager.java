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
	//reference degree
	public float[] mReferenceDegree = {0, 0, 0};
	//magnet degree
	public float[] magDegree = {0, 0, 0};
	public float[] mDiff = {0, 0, 0};
	public float[][] mDiffArr = new float[3][windowSize];
	private static int windowSize = 50000;
	private int iter = 0;

	private HandlerThread mCalculationHandlerThread;
	private Handler mCalculationHandler;
	private List<CampassListener> mCampassListeners = new ArrayList<CampassListener>();
	
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
	                				magneticFieldValues[0] * magneticFieldValues[0] 
	                				+ magneticFieldValues[1] * magneticFieldValues[1]
	                				+ magneticFieldValues[2] * magneticFieldValues[2]);
	                
	                formerMagnetIntensity = newMagnetIntensity;
	                newMagnetIntensity = (float) magneticIntensity;
	                
	                //format change
	                /*DecimalFormat fnum = new  DecimalFormat("#########.#");
		            String str = fnum.format(magneticTense);
					magText.setText("Magnet Tensity: " + str);*/
	                
					magDegree[0] = (float)Math.toDegrees(Math.atan2(event.values[0], event.values[1]));
					magDegree[1] = (float)Math.toDegrees(Math.atan2(event.values[0], event.values[2]));
					magDegree[2] = (float)Math.toDegrees(Math.atan2(event.values[1], event.values[2]));
					
					mCalculationHandler.post(new Runnable() {
						@Override
						public void run() {
							calibrateMagnetDegree(magDegree, true);
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
						float zRotateDegree = (float) Math.toDegrees(event.values[2] * (currentTime - startTime) / 1000);
						float yRotateDegree = (float) Math.toDegrees(event.values[1] * (currentTime - startTime) / 1000);
						float xRotateDegree = (float) Math.toDegrees(event.values[0] * (currentTime - startTime) / 1000);
						final float[] rotateDegrees = {zRotateDegree, yRotateDegree, xRotateDegree};
						
						mCalculationHandler.post(new Runnable() {
							@Override
							public void run() {
								calibrateMagnetDegree(rotateDegrees, false);
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
	/*public void resetNorth() {
		for (int i = 0; i < mDiff.length; i++) {
			mDiff
		}
		mDiff = 0;
		for (int i = 0; i < windowSize; i++) {
			mDiffArr[i] = 0;
		}
		mReferenceDegree = magDegree;
		mDirectionStarted = false;
	}*/
	
	//register all
	public void registerCampassListener() {
		mSensorManager.registerListener(mSensorEventListener, accelSensor, SensorManager.SENSOR_DELAY_FASTEST);
		mSensorManager.registerListener(mSensorEventListener, magnetSensor, SensorManager.SENSOR_DELAY_FASTEST);
		mSensorManager.registerListener(mSensorEventListener, gyroscopeSensor, SensorManager.SENSOR_DELAY_FASTEST);
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
	private synchronized void calibrateMagnetDegree(float[] degrees, boolean isReal) {
		
		if (isReal)	//magnetic input
		{
			for (int i = 0; i < degrees.length; i++) {
				magDegree[i] = degrees[i];
			}
			if (!mDirectionStarted)
			{
				//initial state
				for (int i = 0; i < degrees.length; i++) {
					mReferenceDegree[i] = degrees[i];
				}
				mDirectionStarted = true;
			} 
			else 
			{
				for (int i = 0; i < magDegree.length; i++) {
					
					float diff = magDegree[i] - mReferenceDegree[i];
					diff = normalize(diff);
					if(iter >= windowSize)
						iter = 0;
					mDiff[i] -= mDiffArr[i][iter] / windowSize;
					mDiff[i] += diff / windowSize;
					mDiffArr[i][iter++] = diff;
				}

			}
		}
		else //gyroscope input
		{
			if (mDirectionStarted)
			{
				for (int i = 0; i < magDegree.length; i++) {
					
					mReferenceDegree[i] += degrees[i];
					float diff = magDegree[i] - mReferenceDegree[i];
					diff = normalize(diff);
					//take average
					if(iter >= windowSize)
						iter = 0;
					
					mDiff[i] -= mDiffArr[i][iter] / windowSize;
					mDiff[i] += diff / windowSize;
					mDiffArr[i][iter++] = diff;
				}
			}
		}
		
		for (CampassListener campassListener : mCampassListeners) {
			campassListener.onDirectionChanged(magDegree[0], mReferenceDegree[0] + mDiff[0]);
		}
		
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
		for (int i = 0; i < magDegree.length; i++) {
			mReferenceDegree[i] = magDegree[i] = mDiff[i] = 0;
		}
		
	}

}
