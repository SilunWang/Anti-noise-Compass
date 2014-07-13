package mtrec.compass.core;

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

public class CompassManager {
	
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
	public float[] magnetDegree = {0, 0, 0};
	//absolute-north degree
	final float[] quatDegrees = {0, 0, 0};
	private float[] qqDegrees = {0, 0, 0};
	//delta absolute-north degree
	public final float[] rotateDegrees = {0, 0, 0};
	//delta-angle
	float[] deltaAngular = {0, 0, 0};
	float[] angularSpeed = {0, 0, 0};
	
	public float[] mDiff = {0, 0, 0};
	public float[][] mDiffArr = new float[3][windowSize];
	private final static int windowSize = 4000;
	private int iter = 0;
	private long period = 0;

	private HandlerThread mCalculationHandlerThread;
	private Handler mCalculationHandler;
	private List<CompassListener> mCampassListeners = new ArrayList<CompassListener>();
	
	
	private boolean firstRotate = true;
	//rotation matrix
	public float[] Rmatrix = new float[9];
	
	//filter factors
	private float low_pass_factor = 0.48f;
	private float high_pass_factor = 0.25f;
	
	private float[] deltaQuater = {0, 0, 0, 0};
	private float[] oldQuaternion = {0, 0, 0, 0};
	private float[] newQuaternion = {0, 0, 0, 0};
	
	public CompassManager(SensorManager sensorManager) {
		
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
					magnetDegree[0] = (float)Math.toDegrees(Math.atan2(event.values[0], event.values[1]));
					//x-z plane
					magnetDegree[1] = (float)Math.toDegrees(Math.atan2(event.values[0], event.values[2]));
					//y-z plane
					magnetDegree[2] = (float)Math.toDegrees(Math.atan2(event.values[1], event.values[2]));
					
					mCalculationHandler.post(new Runnable() {
						@Override
						public void run() {
							calibrateMagnetDegree(magnetDegree, false);
						}
					});
				}
				
				// Gyroscope
				else if(event.sensor.getType() == Sensor.TYPE_GYROSCOPE){
					
					long currentTime = System.currentTimeMillis();
					
					// keep compass pointing to magnet north in the first 0.5s
					if (period > 500)
					{
						// get the middle values of angular
						//roll
						deltaAngular[0] = (angularSpeed[0] + event.values[0]) * (currentTime - startTime) / 2000;
						angularSpeed[0] = event.values[0];
						//pitch
						deltaAngular[1] = (angularSpeed[1] + event.values[1]) * (currentTime - startTime) / 2000;
						angularSpeed[1] = event.values[1];
						//yaw or azimuth
						deltaAngular[2] = (angularSpeed[2] + event.values[2]) * (currentTime - startTime) / 2000;
						angularSpeed[2] = event.values[2];
					  
						
						float sin1 = (float) Math.sin(deltaAngular[0]);
						float cos1 = (float) Math.cos(deltaAngular[0]);
						float sin2 = (float) Math.sin(deltaAngular[1]);
						float cos2 = (float) Math.cos(deltaAngular[1]);
						float sin3 = (float) Math.sin(deltaAngular[2]);
						float cos3 = (float) Math.cos(deltaAngular[2]);
						
						//delta-Quaternion
						deltaQuater[0] = cos1*cos2*cos3 - sin1*sin2*sin3;
						deltaQuater[1] = cos1*cos2*sin3 + sin1*sin2*cos3;
						deltaQuater[2] = sin1*cos2*cos3 + cos1*sin2*sin3;
						deltaQuater[3] = cos1*sin2*cos3 - sin1*cos2*sin3;
						
						//new Quaternion values
						newQuaternion[0] = (oldQuaternion[0]*deltaQuater[0] - oldQuaternion[1]*deltaQuater[1] - oldQuaternion[2]*deltaQuater[2] - oldQuaternion[3]*deltaQuater[3]);
						newQuaternion[1] = (oldQuaternion[0]*deltaQuater[1] + oldQuaternion[1]*deltaQuater[0] + oldQuaternion[2]*deltaQuater[3] - oldQuaternion[3]*deltaQuater[2]);
						newQuaternion[2] = (oldQuaternion[0]*deltaQuater[2] - oldQuaternion[1]*deltaQuater[3] + oldQuaternion[2]*deltaQuater[0] + oldQuaternion[3]*deltaQuater[1]);
						newQuaternion[3] = (oldQuaternion[0]*deltaQuater[3] + oldQuaternion[1]*deltaQuater[2] - oldQuaternion[2]*deltaQuater[1] + oldQuaternion[3]*deltaQuater[0]);
						
						//low pass filter
						for (int i = 0; i < oldQuaternion.length; i++) {
							oldQuaternion[i] = low_pass_factor*oldQuaternion[i] + (1-low_pass_factor)*newQuaternion[i];
						}
						
						mSensorManager.getRotationMatrixFromVector(Rmatrix, oldQuaternion);
						
						//calculate degree changes
						//x-y plane direction rotate degree
						rotateDegrees[0] = (float)Math.toDegrees(Math.atan2(Rmatrix[3], Rmatrix[4])) - quatDegrees[0];
						quatDegrees[0] += rotateDegrees[0];
						
						//x-z plane direction rotate degree
						rotateDegrees[1] = (float)Math.toDegrees(Math.atan2(Rmatrix[3], Rmatrix[5])) - quatDegrees[1];
						quatDegrees[1] += rotateDegrees[1];
						
						//y-z plane direction rotate degree
						rotateDegrees[2] = (float)Math.toDegrees(Math.atan2(Rmatrix[4], Rmatrix[5])) - quatDegrees[2];
						quatDegrees[2] += rotateDegrees[2];
						
						mCalculationHandler.post(new Runnable() {
							@Override
							public void run() {
								calibrateMagnetDegree(rotateDegrees, true);
							}
						});
					
					}
					
					period += currentTime - startTime;
					startTime = currentTime;
				}
				
				// rotation vector
				else if(event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR){
					
					// make compass point to magnet north every 10 seconds
					if(firstRotate || startTime % 10000 < 5){
						
						initValues();
						oldQuaternion[0] = event.values[0];
						oldQuaternion[1] = event.values[1];
						oldQuaternion[2] = event.values[2];
						
						if (event.values.length == 4) {
							oldQuaternion[3] = event.values[3];
						}
						else {
							oldQuaternion[3] = (float) Math.sqrt(1 - oldQuaternion[0]*oldQuaternion[0] 
									- oldQuaternion[1]*oldQuaternion[1] - oldQuaternion[2]*oldQuaternion[2]);
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
			mReferenceDegree[i] = magnetDegree[i];
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
	
	public void addCampassListener(CompassListener campassListener) {
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
				magnetDegree[i] = degrees[i];
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
				for (int i = 0; i < magnetDegree.length; i++) {
					//high pass filter
					float diff = (magnetDegree[i] - mReferenceDegree[i]);
					diff = normalize(diff)*high_pass_factor;
					
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
				for (int i = 0; i < magnetDegree.length; i++) {
					
					mReferenceDegree[i] += degrees[i];
					mReferenceDegree[i] = normalize(mReferenceDegree[i]);
					float diff = (magnetDegree[i] - mReferenceDegree[i]);
					diff = normalize(diff)*high_pass_factor;
					//take average
					if(iter >= windowSize)
						iter = 0;
					mDiff[i] -= mDiffArr[i][iter] / windowSize;
					mDiff[i] += diff / windowSize;
					mDiffArr[i][iter++] = diff;
				}
			}
		}
		
		for (CompassListener campassListener : mCampassListeners) {
			campassListener.onDirectionChanged(magnetDegree[0], mReferenceDegree[0] + mDiff[0]);
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
	public void initValues() {
		
		firstRotate = true;
		mDirectionStarted = false;
		iter = 0;
		
		for (int i = 0; i < magnetDegree.length; i++) {
			mReferenceDegree[i] = quatDegrees[i] = magnetDegree[i];
			mDiff[i] = rotateDegrees[i] = 0;
			for (int j = 0; j < windowSize; j++) {
				mDiffArr[i][j] = 0;
			}
		}
		
		for (int i = 0; i < oldQuaternion.length; i++){
			oldQuaternion[i] = deltaQuater[i] = newQuaternion[i] = 0;
		}
		
	}

}
