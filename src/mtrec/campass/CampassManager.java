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
import android.util.Log;
import android.widget.TextView;

public class CampassManager {
	
	private final SensorManager mSensorManager;
	private SensorEventListener mSensorEventListener;
	private Sensor magnetSensor, gyroscopeSensor, rotateSensor;
	
	//magnet field intensity
	public float formerMagnetIntensity = 0;
	public float newMagnetIntensity = 0;
	//if started
	private boolean mDirectionStarted = false;
	//reference degree
	public float[] mReferenceDegree = {0, 0, 0};
	//magnet degree
	public float[] magDegree = {0, 0, 0};
	//absolute-north degree
	final float[] quatDegrees = {0, 0, 0};
	//delta absolute-north degree
	final float[] rotateDegrees = {0, 0, 0};
	
	public float[] mDiff = {0, 0, 0};
	public float[][] mDiffArr = new float[3][windowSize];
	private final static int windowSize = 10000;
	private int iter = 0;

	private HandlerThread mCalculationHandlerThread;
	private Handler mCalculationHandler;
	private List<CampassListener> mCampassListeners = new ArrayList<CampassListener>();
	
	
	private boolean firstRotate = true;
	//rotation matrix
	public float[] Rmatrix = new float[9];
	
	//filter factors
	private float low_pass_factor = 0.5f;
	private float high_pass_factor = 0.25f;
	
	private float[] quaterDelta = {0, 0, 0, 0};
	private float[] quaternion = {0, 0, 0, 0};
	private float[] newQuat = {0, 0, 0, 0};
	
	public CampassManager(SensorManager sensorManager) {
		
		mSensorManager = sensorManager;
		magnetSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		gyroscopeSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		rotateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
		//initial degrees and matrix
		initValues();
		
		//Handler Thread
		mCalculationHandlerThread = new HandlerThread("Campass Calculation Thread");
		mCalculationHandlerThread.start();
		mCalculationHandler = new Handler(mCalculationHandlerThread.getLooper());
		
		
		mSensorEventListener = new SensorEventListener() {
			
			private long startTime = 0;
			
			@Override
			public void onSensorChanged(SensorEvent event) {
				
				// Magnet
				if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
	                //x-y plane
					magDegree[0] = (float)Math.toDegrees(Math.atan2(event.values[0], event.values[1]));
					//x-z plane
					magDegree[1] = (float)Math.toDegrees(Math.atan2(event.values[0], event.values[2]));
					//y-z plane
					magDegree[2] = (float)Math.toDegrees(Math.atan2(event.values[1], event.values[2]));
					
					mCalculationHandler.post(new Runnable() {
						@Override
						public void run() {
							calibrateMagnetDegree(magDegree, false);
						}
					});
				}
				
				// Gyroscope
				else if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
					
					long currentTime = System.currentTimeMillis();
					
					if (startTime > 0)
					{
						//delta-angle
						float[] angulars = {0, 0, 0};
						//roll
						angulars[0] = event.values[0] * (currentTime - startTime) / 1000;
						//pitch
						angulars[1] = event.values[1] * (currentTime - startTime) / 1000;
						//yaw / azimuth
						angulars[2] = event.values[2] * (currentTime - startTime) / 1000;
					  
						
						float sin1 = (float) Math.sin(angulars[0]);
						float cos1 = (float) Math.cos(angulars[0]);
						float sin2 = (float) Math.sin(angulars[1]);
						float cos2 = (float) Math.cos(angulars[1]);
						float sin3 = (float) Math.sin(angulars[2]);
						float cos3 = (float) Math.cos(angulars[2]);
						
						//delta-Quaternion
						quaterDelta[0] = cos1*cos2*cos3 - sin1*sin2*sin3;
						quaterDelta[1] = cos1*cos2*sin3 + sin1*sin2*cos3;
						quaterDelta[2] = sin1*cos2*cos3 + cos1*sin2*sin3;
						quaterDelta[3] = cos1*sin2*cos3 - sin1*cos2*sin3;
						
						//new Quaternion values
						newQuat[0] = (quaternion[0]*quaterDelta[0] - quaternion[1]*quaterDelta[1] - quaternion[2]*quaterDelta[2] - quaternion[3]*quaterDelta[3]);
						newQuat[1] = (quaternion[0]*quaterDelta[1] + quaternion[1]*quaterDelta[0] + quaternion[2]*quaterDelta[3] - quaternion[3]*quaterDelta[2]);
						newQuat[2] = (quaternion[0]*quaterDelta[2] - quaternion[1]*quaterDelta[3] + quaternion[2]*quaterDelta[0] + quaternion[3]*quaterDelta[1]);
						newQuat[3] = (quaternion[0]*quaterDelta[3] + quaternion[1]*quaterDelta[2] - quaternion[2]*quaterDelta[1] + quaternion[3]*quaterDelta[0]);
						
						//low pass filter
						for (int i = 0; i < quaternion.length; i++) {
							quaternion[i] = low_pass_factor*quaternion[i] + (1-low_pass_factor)*newQuat[i];
						}
						
						mSensorManager.getRotationMatrixFromVector(Rmatrix, quaternion);
						
						//calculate degree changes
						rotateDegrees[0] = (float)Math.toDegrees(Math.atan2(Rmatrix[3], Rmatrix[4])) - quatDegrees[0];
						quatDegrees[0] += rotateDegrees[0];
						rotateDegrees[1] = (float)Math.toDegrees(Math.atan2(Rmatrix[3], Rmatrix[5])) - quatDegrees[1];
						quatDegrees[1] += rotateDegrees[1];
						rotateDegrees[2] = (float)Math.toDegrees(Math.atan2(Rmatrix[4], Rmatrix[5])) - quatDegrees[2];
						quatDegrees[2] += rotateDegrees[2];
						
						mCalculationHandler.post(new Runnable() {
							@Override
							public void run() {
								calibrateMagnetDegree(rotateDegrees, true);
							}
						});
					
					}
					
					startTime = currentTime;
				}
				
				// rotation vector
				else if(event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR){
					
					if(firstRotate || startTime % 15000 < 20){
						
						initValues();
						quaternion[0] = event.values[0];
						quaternion[1] = event.values[1];
						quaternion[2] = event.values[2];
						
						if (event.values.length == 4) {
							quaternion[3] = event.values[3];
						}
						else {
							quaternion[3] = (float) Math.sqrt(1 - quaternion[0]*quaternion[0] 
									- quaternion[1]*quaternion[1] - quaternion[2]*quaternion[2]);
						}
						
						firstRotate = false;
					}
				}
				
			}
			
			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
				// TODO Auto-generated method stub
				
			}
		};
		
		
	}
	
	//reset when extreme rotation
	public void resetNorth() {
		for (int i = 0; i < mDiff.length; i++) {
			mDiff[i] = 0;
			mReferenceDegree[i] = magDegree[i];
		}
		for (int i = 0; i < windowSize; i++) {
			mDiffArr[0][i] = 0;
			mDiffArr[1][i] = 0;
			mDiffArr[2][i] = 0;
		}
		iter = 0;
		mDirectionStarted = false;
	}
	
	//register all listeners
	public void registerCampassListener() {
		mSensorManager.registerListener(mSensorEventListener, rotateSensor, SensorManager.SENSOR_DELAY_FASTEST);
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
	
	//core algorithm: calibrate direction
	private synchronized void calibrateMagnetDegree(float[] degrees, boolean isReal) {
		
		if (!isReal)	//magnet degrees
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
					//high pass filter
					float diff = (magDegree[i] - mReferenceDegree[i])*high_pass_factor;
					diff = normalize(diff);
					if(iter >= windowSize)
						iter = 0;
					mDiff[i] -= mDiffArr[i][iter] / windowSize;
					mDiff[i] += diff / windowSize;
					mDiffArr[i][iter++] = diff;
				}
			}
		}
		else //gyroscope calibrate
		{
			if (mDirectionStarted)
			{
				for (int i = 0; i < magDegree.length; i++) {
					
					mReferenceDegree[i] += degrees[i];
					float diff = (magDegree[i] - mReferenceDegree[i])*high_pass_factor;
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
	
	//normalize degree to [-180, 180)
	public float normalize(float degree) {
		while (degree >= 180)
			degree -= 360;
		while (degree < -180)
			degree += 360;
		return degree;
	}
	
	//initiate degrees
	private void initValues() {
		
		firstRotate = true;
		mDirectionStarted = false;
		iter = 0;
		
		for (int i = 0; i < magDegree.length; i++) {
			mReferenceDegree[i] = quatDegrees[i] = magDegree[i];
			mDiff[i] = rotateDegrees[i] = 0;
			for (int j = 0; j < windowSize; j++) {
				mDiffArr[i][j] = 0;
			}
		}
		
		for (int i = 0; i < quaternion.length; i++){
			quaternion[i] = quaterDelta[i] = newQuat[i] = 0;
		}
		
	}

}
