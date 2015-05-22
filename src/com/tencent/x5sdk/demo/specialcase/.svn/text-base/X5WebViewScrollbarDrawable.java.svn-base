package com.tencent.x5sdk.demo.specialcase;


import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * 为了屏蔽UI和内核在滚动条使用方法上的不一致，在此包装UI的滚动条以供内核使用
 * @author launliu
 * robinsli copy it from ui to corejava,to support sdk
 *
 */
public class X5WebViewScrollbarDrawable extends Drawable 
{
	private final boolean mVertical;
	private final Drawable mDrawable;
	private final int mMargin;
	private final int mSize;
	private final int mAlpha;
	
	private final Rect mTempRect = new Rect();
	
	public X5WebViewScrollbarDrawable(boolean mVertical,
			Drawable mDrawable, int margin, int size, int alpha) 
	{
		this.mVertical = mVertical;
		this.mDrawable = mDrawable;
		this.mMargin = margin;
		this.mSize = size;
		this.mAlpha = alpha;
		
		setAlpha(255);
	}
	
	@Override
	public int getIntrinsicHeight()
	{
		if (this.mVertical) 
		{
			return this.mDrawable.getIntrinsicHeight();
		}
		else
		{
			return this.mSize + this.mMargin;
		}
	}
	
	@Override
	public int getIntrinsicWidth() 
	{
		if (this.mVertical)
		{
			return this.mSize + this.mMargin;
		}
		else
		{
			return this.mDrawable.getIntrinsicWidth();
		}
	}
	

	/*
	 * 滚动条MttCtrlVerScroll和内核webview显示的滚动条图片都是共享的同一张图片
	 * Drawable.setBounds调用X5WebViewScrollbarDrawable.onBoundsChanged来设置此次滚动条显示的位置.
	 * 但是bug[49197521]，webview的滚动条在设置完显示位置后draw之前，中间显示了下拉列表框。
	 * 共享图片的位置被设置成下拉列表框的位置而没清理。
	 * 导致页面滚动条用下拉列表框的位置进行显示.
	 * 
	 * 把位置设置放到draw的时候
	 * */
	@Override
	public void draw(Canvas canvas)
	{
		Rect	oldRectBounds = this.mDrawable.getBounds();//不要污染别人的img，用完设置回去.
		final Rect drawableBounds = this.mTempRect;
		drawableBounds.set(getBounds());
		
		// Reduce actual drawable's size from its margin
		if (this.mVertical) {
			drawableBounds.right -= this.mMargin;
		}
		else {
			drawableBounds.bottom -= this.mMargin;
		}
		
		this.mDrawable.setBounds(drawableBounds);
		this.mDrawable.draw(canvas);
		
		this.mDrawable.setBounds(oldRectBounds);
	}

	@Override
	public int getOpacity() 
	{
		return this.mDrawable.getOpacity();
	}

	@Override
	public void setAlpha(int alpha)
	{
		this.mDrawable.setAlpha(alpha * this.mAlpha / 255);
	}

	@Override
	public void setColorFilter(ColorFilter cf) 
	{
		this.mDrawable.setColorFilter(cf);
	}

}

