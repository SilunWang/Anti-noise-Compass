package mtrec.campass.activity;

import java.text.DecimalFormat;
import java.util.Arrays;
import mtrec.campass.CampassListener;
import mtrec.campass.CampassManager;
import mtrec.campass.view.CampassView;
import mtrec.rock.campass.R;
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
    
	private CampassView absoluteCampassView;
	
	private TextView magText, mReferenceText, mDiffText;
	private Handler calculationHandler;
	private double magneticTense = 0;
	
	public float orientDegree = 0;
	public float tiltDegree = 0;
	public float rotateDegree = 0;

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mContext = getApplicationContext();
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		//Handler Thread
		HandlerThread handlerThread = new HandlerThread("Calculation Thread");
		handlerThread.start();
		calculationHandler = new Handler(handlerThread.getLooper());
		
		//imageView
		absoluteCampassView = (CampassView) findViewById(R.id.absoluteNorth);

		magText = (TextView) findViewById(R.id.magnitude);
		mReferenceText = (TextView) findViewById(R.id.mReference);
		mDiffText = (TextView) findViewById(R.id.mDiff);
		
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        
		mCampassManager = new CampassManager(sensorManager);
		mCampassManager.addCampassListener(new CampassListener() {
			
			@Override
			public void onDirectionChanged(final float magDegree,  final float oriDegree) {
				
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						absoluteCampassView.setAbsoluteNorth(oriDegree);
						absoluteCampassView.setMagneticNorth(magDegree);
						mReferenceText.setText("ori: "+ String.valueOf(oriDegree));
						mDiffText.setText("mag: " + String.valueOf(magDegree));
					}
				});
			}
		});
		
	
		pressOnceMoreWhenLeave = Toast.makeText(this, "Press once more to leave", Toast.LENGTH_SHORT);
	}
	
	
	//注意activity暂停的时候释放  
    public void onPause(){
    	super.onPause();
    	//unregister all
        mCampassManager.unregisterCampassListener();
    }
    
    public void onResume(){
    	super.onResume();
    	//register all
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
