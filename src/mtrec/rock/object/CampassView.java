package mtrec.rock.object;

import java.util.Random;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.ImageView;

public class CampassView extends ImageView {
	
	private float absoluteNorth = -90;
	private float magneticNorth = -90;
	private Paint pointPaint = new Paint();
	private Random random = new Random(System.currentTimeMillis());
	private int[] rgbColor = new int[] {random.nextInt(200), random.nextInt(200), random.nextInt(200)};

	public CampassView(Context context) {
		super(context);
	}

	public CampassView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CampassView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	public void setAbsoluteNorth(float degree) {
		absoluteNorth = degree - 90;
		invalidate();
	}
	
	public void setMagneticNorth(float degree) {
		magneticNorth = degree - 90;
		invalidate();
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		float[] estimatedPointValue = new float[]{getWidth() / 2, getHeight() / 2};
		float radius = Math.min(getWidth() / 2, getHeight() / 2), centerRadius = radius / 12f;
		
		pointPaint.setStyle(Style.FILL);
		pointPaint.setColor(Color.argb(30, rgbColor[0], rgbColor[1], rgbColor[2]));
		canvas.drawCircle(estimatedPointValue[0], estimatedPointValue[1], radius, pointPaint);
		
		pointPaint.setColor(Color.rgb(rgbColor[0], rgbColor[1], rgbColor[2]));
		canvas.drawCircle(estimatedPointValue[0], estimatedPointValue[1], centerRadius - 1, pointPaint);
		
		Path arrow = new Path();
		float pointerX = (float) (estimatedPointValue[0] + centerRadius * 1.1 * Math.cos(absoluteNorth / 180 * Math.PI));
		float pointerY = (float) (estimatedPointValue[1] + centerRadius * 1.1 * Math.sin(absoluteNorth / 180 * Math.PI));
		arrow.moveTo(pointerX, pointerY);
		arrow.arcTo(new RectF(estimatedPointValue[0] - radius,
				estimatedPointValue[1] - radius,
				estimatedPointValue[0] + radius,
				estimatedPointValue[1] + radius),
				absoluteNorth - 3, 6);
		arrow.lineTo(pointerX, pointerY);
		canvas.drawPath(arrow, pointPaint);
		
		//rgbColor = new int[] {random.nextInt(200), random.nextInt(200), random.nextInt(200)};
		pointPaint.setColor(Color.argb(90, rgbColor[0], rgbColor[1], rgbColor[2]));
		
		Path arrow2 = new Path();
		float pointerX2 = (float) (estimatedPointValue[0] + centerRadius * 1.1 * Math.cos(magneticNorth / 180 * Math.PI));
		float pointerY2 = (float) (estimatedPointValue[1] + centerRadius * 1.1 * Math.sin(magneticNorth / 180 * Math.PI));
		arrow2.moveTo(pointerX2, pointerY2);
		arrow2.arcTo(new RectF(estimatedPointValue[0] - radius*2/3, 
				estimatedPointValue[1] - radius*2/3, 
				estimatedPointValue[0] + radius*2/3, 
				estimatedPointValue[1] + radius*2/3), 
				magneticNorth - 10, 20);
		arrow2.lineTo(pointerX2, pointerY2);
		canvas.drawPath(arrow2, pointPaint);
		
		pointPaint.setStyle(Style.STROKE);
		pointPaint.setColor(Color.rgb(255, 255, 255));
		canvas.drawCircle(estimatedPointValue[0], estimatedPointValue[1], centerRadius, pointPaint);
	}

}
