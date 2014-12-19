package com.carrey.waterfall.waterfall;

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;
import android.widget.Toast;
/**
 * �ٲ����������ĵ�Ԫ
 * @author Ang
 *
 */
public class FlowingView extends View implements View.OnClickListener, View.OnLongClickListener {
	
	/** ��Ԫ�ı��,�������ٲ�������Ψһ��,����������ʶ��� */
	private int index;
	
	/** ��Ԫ��Ҫ��ʾ��ͼƬBitmap */
	private Bitmap imageBmp;
	/** ͼ���ļ���·�� */
	private String imageFilePath;
	/** ��Ԫ�Ŀ��,Ҳ��ͼ��Ŀ�� */
	private int width;
	/** ��Ԫ�ĸ߶�,Ҳ��ͼ��ĸ߶� */
	private int height;
	
	/** ���� */
	private Paint paint;
	/** ͼ��������� */
	private Rect rect;
	
	/** �����Ԫ�ĵײ����������еĶ���֮��ľ��� */
	private int footHeight;
	
	public FlowingView(Context context, int index, int width) {
		super(context);
		this.index = index;
		this.width = width;
		init();
	}
	
	/**
	 * ������ʼ������
	 */
	private void init() {
		setOnClickListener(this);
		setOnLongClickListener(this);
		paint = new Paint();
		paint.setAntiAlias(true);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(width, height);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		//����ͼ��
		canvas.drawColor(Color.WHITE);
		if (imageBmp != null && rect != null) {
			canvas.drawBitmap(imageBmp, null, rect, paint);
		}
		super.onDraw(canvas);
	}
	
	/**
	 * ��WaterFall�����첽����ͼƬ����
	 */
	public void loadImage() {
		InputStream inStream = null;
		try {
			inStream = getContext().getAssets().open(imageFilePath);
			imageBmp = BitmapFactory.decodeStream(inStream);
			inStream.close();
			inStream = null;
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (imageBmp != null) {
			int bmpWidth = imageBmp.getWidth();
			int bmpHeight = imageBmp.getHeight();
			height = (int) (bmpHeight * width / bmpWidth);
			rect = new Rect(0, 0, width, height);
		}
	}
	
	/**
	 * ���¼��ػ����˵�Bitmap
	 */
	public void reload() {
		if (imageBmp == null) {
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					InputStream inStream = null;
					try {
						inStream = getContext().getAssets().open(imageFilePath);
						imageBmp = BitmapFactory.decodeStream(inStream);
						inStream.close();
						inStream = null;
						postInvalidate();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}).start();
		}
	}
	
	/**
	 * ��ֹOOM���л���
	 */
	public void recycle() {
		if (imageBmp == null || imageBmp.isRecycled()) 
			return;
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				imageBmp.recycle();
				imageBmp = null;
				postInvalidate();
			}
		}).start();
	}
	
	@Override
	public boolean onLongClick(View v) {
		Toast.makeText(getContext(), "long click : " + index, Toast.LENGTH_SHORT).show();
		return true;
	}

	@Override
	public void onClick(View v) {
		Toast.makeText(getContext(), "click : " + index, Toast.LENGTH_SHORT).show();
	}

	/**
	 * ��ȡ��Ԫ�ĸ߶�
	 * @return
	 */
	public int getViewHeight() {
		return height;
	}
	/**
	 * ����ͼƬ·��
	 * @param imageFilePath
	 */
	public void setImageFilePath(String imageFilePath) {
		this.imageFilePath = imageFilePath;
	}

	public Bitmap getImageBmp() {
		return imageBmp;
	}

	public void setImageBmp(Bitmap imageBmp) {
		this.imageBmp = imageBmp;
	}

	public int getFootHeight() {
		return footHeight;
	}

	public void setFootHeight(int footHeight) {
		this.footHeight = footHeight;
	}
}
