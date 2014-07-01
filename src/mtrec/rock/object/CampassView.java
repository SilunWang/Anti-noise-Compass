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
	
	private float orientation = -90;
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
	
	public void setOrientation(float degree) {
		orientation = degree - 90;
		invalidate();
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		float[] estimatedPointValue = new float[]{getWidth() / 2, getHeight() / 2};
		float radius = Math.min(getWidth() / 2, getHeight() / 2), centerRadius = radius / 4f;
		
		pointPaint.setStyle(Style.FILL);
		pointPaint.setColor(Color.argb(30, rgbColor[0], rgbColor[1], rgbColor[2]));
		canvas.drawCircle(estimatedPointValue[0], estimatedPointValue[1], radius, pointPaint);
		
		pointPaint.setColor(Color.rgb(rgbColor[0], rgbColor[1], rgbColor[2]));
		canvas.drawCircle(estimatedPointValue[0], estimatedPointValue[1], centerRadius - 1, pointPaint);
		
		Path arrow = new Path();
		float pointerX = (float) (estimatedPointValue[0] + centerRadius * 2 * Math.cos(orientation / 180 * Math.PI));
		float pointerY = (float) (estimatedPointValue[1] + centerRadius * 2 * Math.sin(orientation / 180 * Math.PI));
		arrow.moveTo(pointerX, pointerY);
		arrow.arcTo(new RectF(estimatedPointValue[0] - centerRadius * 1.25f, estimatedPointValue[1] - centerRadius * 1.25f, estimatedPointValue[0] + centerRadius * 1.25f, estimatedPointValue[1] + centerRadius * 1.25f), orientation - 30, 60);
		arrow.lineTo(pointerX, pointerY);
		canvas.drawPath(arrow, pointPaint);
		
		pointPaint.setStyle(Style.STROKE);
		pointPaint.setColor(Color.rgb(255, 255, 255));
		canvas.drawCircle(estimatedPointValue[0], estimatedPointValue[1], centerRadius, pointPaint);
	}

}
