package mtrec.rock.campass;

import java.util.Arrays;

import mtrec.campass.CampassListener;
import mtrec.campass.CampassManager;
import mtrec.rock.object.CampassView;
import android.app.Activity;
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
import android.widget.Toast;

public class MainActivity extends Activity {
	
	private SensorManager sensorManager;
	private CampassView orientationCampassView, magneticFieldCampassView, refinedOrientationCampassView, refinedMagneticFieldCampassView;
	private float orientationReferenceDegree, orientationDegree, orientationDiff;
	private int orientationSampleSize = -1;
	private boolean gravityFilled = false, magneticFilled = false;
	private final Matrix3f gravityVector = new Matrix3f(), magneticFieldReferenceVector = new Matrix3f(), magneticFieldVector = new Matrix3f();
	private float magneticFieldReferenceDegree, magneticFieldDegree, magneticFieldDiffInDegree;
	private int magneticFieldSampleSize = -1;
	private Handler calculationHandler;
	
	private CampassManager mCampassManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		HandlerThread handlerThread = new HandlerThread("Calculation Thread");
		handlerThread.start();
		calculationHandler = new Handler(handlerThread.getLooper());
		
		orientationCampassView = (CampassView) findViewById(R.id.orientation);
		magneticFieldCampassView = (CampassView) findViewById(R.id.magnetic_field);
		refinedOrientationCampassView = (CampassView) findViewById(R.id.refined_orientation);
		refinedMagneticFieldCampassView = (CampassView) findViewById(R.id.refined_magnetic_field);
		
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		
		mCampassManager = new CampassManager(sensorManager);
		mCampassManager.addCampassListener(new CampassListener() {
			
			@Override
			public void onDirectionChanged(final float degree) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						orientationCampassView.setOrientation(degree);
					}
				});
			}
		});
		
		pressOnceMoreWhenLeave = Toast.makeText(this, "Press once more to leave", Toast.LENGTH_SHORT);
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
