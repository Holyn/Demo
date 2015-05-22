package com.tencent.x5sdk.demo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.tencent.smtt.sdk.WebView;

/*
 * Copyright (C) 2005-2010 TENCENT Inc.All Rights Reserved.		
 * 
 * FileName：DemoWebView.java
 * 
 * Description：
 * 
 * History：
 * 1.0 sekarao 2014-2-27 Create
 */

public class DemoWebView extends WebView implements View.OnTouchListener
{

	public DemoWebView(Context context)
	{
		super(context);
		setOnTouchListener(this);
		
		//=================设置长按监听方法，两种方法==================
		
		//方法一：对所有版本都适用（暂时建议使用此方法）
		this.setOnLongClickListener(mLongClickListener);
		
		//方法二：只对5.3及以上版本的x5内核适用；5.2及以下版本，这种方法会覆盖内核的长按监听，导致长按后，内核无相应。
		//this.getView().setOnLongClickListener(mLongClickListener);
		
		//=================设置长按监听方法 end=======================
	}
	
	//自定义长按监听
	OnLongClickListener mLongClickListener =  new OnLongClickListener() {
		
		@Override
		public boolean onLongClick(View v) {
			/*
			 * 长按事件处理。。。
			 */
			Log.e("DemoWebView", "DemoWebView-onLongClick()");
			
			return false;	//返回false，则会继续传递长按事件到内核处理。
			//return true;	//返回true，则停止传递。
		}
	};
	
	@Override
	protected boolean drawChild(Canvas canvas, View child, long drawingTime)
	{
		boolean ret = super.drawChild(canvas, child, drawingTime);
		canvas.save();
		Paint paint = new Paint();
		paint.setColor(0x7fff0000);
		paint.setTextSize(24.f);
		paint.setAntiAlias(true);
		if(getX5WebViewExtension() != null)
			canvas.drawText("X5 Core:" + WebView.getQQBrowserVersion(), 10, 50, paint);
		else
			canvas.drawText("Sys Core", 10, 50, paint);
		canvas.restore();
		return ret;
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		Log.e("DemoWebView", "onTouch " + event.getAction());
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			
			break;
		}
		return false;
	}

}
