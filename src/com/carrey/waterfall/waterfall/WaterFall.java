package com.carrey.waterfall.waterfall;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Random;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.ScrollView;
/**
 * 瀑布流
 * 某些参数做了固定设置，如果想扩展功能，可自行修改
 * @author Ang
 *
 */
public class WaterFall extends ScrollView {
	
	/** 延迟发送message的handler */
	private DelayHandler delayHandler;
	/** 添加单元到瀑布流中的Handler */
	private AddItemHandler addItemHandler;
	
	/** ScrollView直接包裹的LinearLayout */
	private LinearLayout containerLayout;
	/** 存放所有的列Layout */
	private ArrayList<LinearLayout> colLayoutArray;
	
	/** 当前所处的页面（已经加载了几次） */
	private int currentPage;
	
	/** 存储每一列中向上方向的未被回收bitmap的单元的最小行号 */
	private int[] currentTopLineIndex;
	/** 存储每一列中向下方向的未被回收bitmap的单元的最大行号 */
	private int[] currentBomLineIndex;
	/** 存储每一列中已经加载的最下方的单元的行号 */
	private int[] bomLineIndex;
	/** 存储每一列的高度 */
	private int[] colHeight;
	
	/** 所有的图片资源路径 */
	private String[] imageFilePaths;
	
	/** 瀑布流显示的列数 */
	private int colCount;
	/** 瀑布流每一次加载的单元数量 */
	private int pageCount;
	/** 瀑布流容纳量 */
	private int capacity;
	
	private Random random;
	
	/** 列的宽度 */
	private int colWidth;
	
	private boolean isFirstPage;

	public WaterFall(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public WaterFall(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public WaterFall(Context context) {
		super(context);
		init();
	}
	
	/** 基本初始化工作 */
	private void init() {
		delayHandler = new DelayHandler(this);
		addItemHandler = new AddItemHandler(this);
		colCount = 4;//默认情况下是4列
		pageCount = 30;//默认每次加载30个瀑布流单元
		capacity = 10000;//默认容纳10000张图
		random = new Random();
		colWidth = getResources().getDisplayMetrics().widthPixels / colCount;
		
		colHeight = new int[colCount];
		currentTopLineIndex = new int[colCount];
		currentBomLineIndex = new int[colCount];
		bomLineIndex = new int[colCount];
		colLayoutArray = new ArrayList<LinearLayout>();
	}
	
	/**
	 * 在外部调用 第一次装载页面 必须调用
	 */
	public void setup() {
		containerLayout = new LinearLayout(getContext());
		containerLayout.setBackgroundColor(Color.WHITE);
		LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
		addView(containerLayout, layoutParams);
		
		for (int i = 0; i < colCount; i++) {
			LinearLayout colLayout = new LinearLayout(getContext());
			LinearLayout.LayoutParams colLayoutParams = new LinearLayout.LayoutParams(
					colWidth, LinearLayout.LayoutParams.WRAP_CONTENT);
			colLayout.setPadding(2, 2, 2, 2);
			colLayout.setOrientation(LinearLayout.VERTICAL);
			
			containerLayout.addView(colLayout, colLayoutParams);
			colLayoutArray.add(colLayout);
		}
		
		try {
			imageFilePaths = getContext().getAssets().list("images");
		} catch (IOException e) {
			e.printStackTrace();
		}
		//添加第一页
		addNextPageContent(true);
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			break;
		case MotionEvent.ACTION_UP:
			//手指离开屏幕的时候向DelayHandler延时发送一个信息，然后DelayHandler
			//届时来判断当前的滑动位置，进行不同的处理。
			delayHandler.sendMessageDelayed(delayHandler.obtainMessage(), 200);
			break;
		}
		return super.onTouchEvent(ev);
	}
	
	@Override
	protected void onScrollChanged(int l, int t, int oldl, int oldt) {
		//在滚动过程中，回收滚动了很远的bitmap,防止OOM
		/*---回收算法说明：
		 * 回收的整体思路是：
		 * 我们只保持当前手机显示的这一屏以及上方两屏和下方两屏 一共5屏内容的Bitmap,
		 * 超出这个范围的单元Bitmap都被回收。
		 * 这其中又包括了一种情况就是之前回收过的单元的重新加载。
		 * 详细的讲解：
		 * 向下滚动的时候：回收超过上方两屏的单元Bitmap,重载进入下方两屏以内Bitmap
		 * 向上滚动的时候：回收超过下方两屏的单元bitmao,重载进入上方两屏以内bitmap
		 * ---*/
		int viewHeight = getHeight();
		if (t > oldt) {//向下滚动
			if (t > 2 * viewHeight) {
				for (int i = 0; i < colCount; i++) {
					LinearLayout colLayout = colLayoutArray.get(i);
					//回收上方超过两屏bitmap
					FlowingView topItem = (FlowingView) colLayout.getChildAt(currentTopLineIndex[i]);
					if (topItem.getFootHeight() < t - 2 * viewHeight) {
						topItem.recycle();
						currentTopLineIndex[i] ++;
					}
					//重载下方进入(+1)两屏以内bitmap
					FlowingView bomItem = (FlowingView) colLayout.getChildAt(Math.min(currentBomLineIndex[i] + 1, bomLineIndex[i]));
					if (bomItem.getFootHeight() <= t + 3 * viewHeight) {
						bomItem.reload();
						currentBomLineIndex[i] = Math.min(currentBomLineIndex[i] + 1, bomLineIndex[i]);
					}
				}
			}
		} else {//向上滚动
			for (int i = 0; i < colCount; i++) {
				LinearLayout colLayout = colLayoutArray.get(i);
				//回收下方超过两屏bitmap
				FlowingView bomItem = (FlowingView) colLayout.getChildAt(currentBomLineIndex[i]);
				if (bomItem.getFootHeight() > t + 3 * viewHeight) {
					bomItem.recycle();
					currentBomLineIndex[i] --;
				}
				//重载上方进入(-1)两屏以内bitmap
				FlowingView topItem = (FlowingView) colLayout.getChildAt(Math.max(currentTopLineIndex[i] - 1, 0));
				if (topItem.getFootHeight() >= t - 2 * viewHeight) {
					topItem.reload();
					currentTopLineIndex[i] = Math.max(currentTopLineIndex[i] - 1, 0);
				}
			}
		}
		super.onScrollChanged(l, t, oldl, oldt);
	}
	
	/**
	 * 这里之所以要用一个Handler，是为了使用他的延迟发送message的函数
	 * 延迟的效果在于，如果用户快速滑动，手指很早离开屏幕，然后滑动到了底部的时候，
	 * 因为信息稍后发送，在手指离开屏幕到滑动到底部的这个时间差内，依然能够加载图片
	 * @author carrey
	 *
	 */
	private static class DelayHandler extends Handler {
		private WeakReference<WaterFall> waterFallWR;
		private WaterFall waterFall;
		public DelayHandler(WaterFall waterFall) {
			waterFallWR = new WeakReference<WaterFall>(waterFall);
			this.waterFall = waterFallWR.get();
		}
		
		@Override
		public void handleMessage(Message msg) {
			//判断当前滑动到的位置，进行不同的处理
			if (waterFall.getScrollY() + waterFall.getHeight() >= 
					waterFall.getMaxColHeight() - 20) {
				//滑动到底部，添加下一页内容
				waterFall.addNextPageContent(false);
			} else if (waterFall.getScrollY() == 0) {
				//滑动到了顶部
			} else {
				//滑动在中间位置
			}
			super.handleMessage(msg);
		}
	}
	
	/**
	 * 添加单元到瀑布流中的Handler
	 * @author carrey
	 *
	 */
	private static class AddItemHandler extends Handler {
		private WeakReference<WaterFall> waterFallWR;
		private WaterFall waterFall;
		public AddItemHandler(WaterFall waterFall) {
			waterFallWR = new WeakReference<WaterFall>(waterFall);
			this.waterFall = waterFallWR.get();
		}
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case 0x00:
				FlowingView flowingView = (FlowingView)msg.obj;
				waterFall.addItem(flowingView);
				break;
			}
			super.handleMessage(msg);
		}
	}
	/**
	 * 添加单元到瀑布流中
	 * @param flowingView
	 */
	private void addItem(FlowingView flowingView) {
		int minHeightCol = getMinHeightColIndex();
		colLayoutArray.get(minHeightCol).addView(flowingView);
		colHeight[minHeightCol] += flowingView.getViewHeight();
		flowingView.setFootHeight(colHeight[minHeightCol]);
		
		if (!isFirstPage) {
			bomLineIndex[minHeightCol] ++;
			currentBomLineIndex[minHeightCol] ++;
		}
	}
	
	/**
	 * 添加下一个页面的内容
	 */
	private void addNextPageContent(boolean isFirstPage) {
		this.isFirstPage = isFirstPage;
		
		//添加下一个页面的pageCount个单元内容
		for (int i = pageCount * currentPage; 
				i < pageCount * (currentPage + 1) && i < capacity; i++) {
			new Thread(new PrepareFlowingViewRunnable(i)).run();
		}
		currentPage ++;
	}
	
	/**
	 * 异步加载要添加的FlowingView
	 * @author carrey
	 *
	 */
	private class PrepareFlowingViewRunnable implements Runnable {
		private int id;
		public PrepareFlowingViewRunnable (int id) {
			this.id = id;
		}
		
		@Override
		public void run() {
			FlowingView flowingView = new FlowingView(getContext(), id, colWidth);
			String imageFilePath = "images/" + imageFilePaths[random.nextInt(imageFilePaths.length)];
			flowingView.setImageFilePath(imageFilePath);
			flowingView.loadImage();
			addItemHandler.sendMessage(addItemHandler.obtainMessage(0x00, flowingView));
		}
		
	}
	
	/**
	 * 获得所有列中的最大高度
	 * @return
	 */
	private int getMaxColHeight() {
		int maxHeight = colHeight[0];
		for (int i = 1; i < colHeight.length; i++) {
			if (colHeight[i] > maxHeight)
				maxHeight = colHeight[i];
		}
		return maxHeight;
	}
	
	/**
	 * 获得目前高度最小的列的索引
	 * @return
	 */
	private int getMinHeightColIndex() {
		int index = 0;
		for (int i = 1; i < colHeight.length; i++) {
			if (colHeight[i] < colHeight[index])
				index = i;
		}
		return index;
	}
}
