package mtrec.rock.campass;

import java.text.DecimalFormat;
import java.util.Arrays;
import mtrec.campass.CampassListener;
import mtrec.campass.CampassManager;
import mtrec.rock.object.CampassView;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.renderscript.Matrix3f;
import android.util.Log;
import android.view.Menu;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	private static Context mContext;
	
	private SensorManager sensorManager;
	private CampassManager mCampassManager;
	private Sensor accSensor = null;
    private Sensor magSensor = null;
    private SensorEventListener mSensorListener = null;
    
	private CampassView absoluteCampassView;
	
	private TextView magText;
	
	private float orientationReferenceDegree, orientationDegree, orientationDiff;
	private int orientationSampleSize = -1;
	private boolean gravityFilled = false, magneticFilled = false;
	private final Matrix3f gravityVector = new Matrix3f(), magneticFieldReferenceVector = new Matrix3f(), magneticFieldVector = new Matrix3f();
	private float magneticFieldReferenceDegree, magneticFieldDegree, magneticFieldDiffInDegree;
	private int magneticFieldSampleSize = -1;
	private Handler calculationHandler;
	private double magneticTense = 0;
	
	private float[] accelerometerValues = new float[3];
	private float[] magneticFieldValues = new float[3];
	private float[] values = new float[3];
	private float[] Rmatrix = new float[9];
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mContext = getApplicationContext();
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		HandlerThread handlerThread = new HandlerThread("Calculation Thread");
		handlerThread.start();
		calculationHandler = new Handler(handlerThread.getLooper());
		
		absoluteCampassView = (CampassView) findViewById(R.id.orientation);
		//oriText = (TextView) findViewById(R.id.oriDegree);
		//absoText = (TextView) findViewById(R.id.absoDegree);
		//magText = (TextView) findViewById(R.id.magDegree);
		magText = (TextView) findViewById(R.id.magnitude);
		
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        
		mCampassManager = new CampassManager(sensorManager);
		mCampassManager.addCampassListener(new CampassListener() {
			
			@Override
			public void onDirectionChanged(final float degree) {
				
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						absoluteCampassView.setAbsoluteNorth(degree);
					}
				});
			}
		});
		
		
		
		mSensorListener = new SensorEventListener() {
			
			@Override
			public void onSensorChanged(SensorEvent event) {
				// TODO Auto-generated method stub
				if(event.sensor.getType()==Sensor.TYPE_ACCELEROMETER){    
	                accelerometerValues=event.values;
	            }    
	            if(event.sensor.getType()==Sensor.TYPE_MAGNETIC_FIELD){    
	                magneticFieldValues=event.values;
	                magneticTense = Math.sqrt(magneticFieldValues[0]*magneticFieldValues[0] + magneticFieldValues[1]*magneticFieldValues[1] + magneticFieldValues[2]*magneticFieldValues[2]);
	                DecimalFormat fnum = new  DecimalFormat("#########.#");
		            String str = fnum.format(magneticTense);
					magText.setText(str);
	            }
	            //调用getRotaionMatrix获得变换矩阵R[]
	            SensorManager.getRotationMatrix(Rmatrix, null, accelerometerValues, magneticFieldValues);    
	            SensorManager.getOrientation(Rmatrix, values);
	            
	            //得到的values值为弧度
	            //转换为角度
	            values[0] = (float)Math.toDegrees(values[0]);
	            double radian = Math.atan2(event.values[0], event.values[1]);
				float degree = (float) (radian * 180 / Math.PI);
				absoluteCampassView.setMagneticNorth(degree);
			}
			
			@Override
			public void onAccuracyChanged(Sensor sensor, int accuracy) {
				// TODO Auto-generated method stub
				
			}
		};
	
		pressOnceMoreWhenLeave = Toast.makeText(this, "Press once more to leave", Toast.LENGTH_SHORT);
	}
	
	
	//注意activity暂停的时候释放  
    public void onPause(){
    	super.onPause();
        sensorManager.unregisterListener(mSensorListener);
        mCampassManager.unregisterCampassListener();
    }
    
    public void onResume(){
    	super.onResume();
    	sensorManager.registerListener(mSensorListener, accSensor, SensorManager.SENSOR_DELAY_GAME);
		sensorManager.registerListener(mSensorListener, magSensor, SensorManager.SENSOR_DELAY_GAME);
		mCampassManager.registerCampassListener();
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	private long backPressedTime = 0;
	private Toast pressOnceMoreWhenLeave;
	
	@Override
	public void onBackPressed() {
		long currentTime = System.currentTimeMillis();
		if (currentTime - backPressedTime > 1000) {
			pressOnceMoreWhenLeave.show();
		} else {
			finish();
		}
		backPressedTime = currentTime;
	}

}
