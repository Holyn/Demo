package com.tencent.x5sdk.demo.specialcase;


import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import com.tencent.smtt.export.external.libwebp;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.NinePatch;
import android.graphics.Rect;
import android.graphics.BitmapFactory.Options;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;

/***
 * 
 * robins copy this file from ui project to core 2014-02
 * for sdk will read scrollbar picture and listbox will read backgroud picture.
 * 2014.2  getDrawableFromDefaultSkin()/createListboxDrawable()有使用，其他未使用
 * @author robins
 * 
 */
public class NinePatchUtils
{

	private final static int	NO_COLOR	= 0x00000001;

	private NinePatchUtils()
	{
	}

	/***
	 * 读取assets下图片,此处修改了NinePatchDrawable不能正常读取问题，
	 * e.g
	 * Drawable bg = NinePatchTool.decodeDrawableFromAsset(this, path);
	 * 
	 * assetPath:如果存在子目录，则需要写子目录名称，如下："aaa/log.png"
	 * 
	 * @param context
	 * @param assetPath
	 * @return
	 * @throws Exception
	 */
	public static Drawable decodeDrawableFromAsset(Context context, String assetPath) throws Exception
	{
		Bitmap bitmap = readFromAsset(context, assetPath);
		if (bitmap.getNinePatchChunk() == null)
		{
			BitmapDrawable bitmapDrawable = new BitmapDrawable(bitmap);
			return bitmapDrawable;
		}
		else
		{
			Rect paddingRect = new Rect();
			readPaddingFromChunk(bitmap.getNinePatchChunk(), paddingRect);
			return new NinePatchDrawable(context.getResources(), bitmap, bitmap.getNinePatchChunk(), paddingRect, null);
		}
	}

	/***
	 * 通过流读取图片
	 * 
	 * @param in
	 * @return
	 * @throws Exception
	 */
	public static Bitmap decodeFromStream(InputStream in) throws Exception
	{
		Bitmap srcBm = BitmapFactory.decodeStream(in);
		srcBm.setDensity(DisplayMetrics.DENSITY_HIGH);
		byte[] chunk = readChunk(srcBm);
		boolean isNinePatchChunk = NinePatch.isNinePatchChunk(chunk);
		if (isNinePatchChunk)
		{
			Bitmap tgtBm = Bitmap.createBitmap(srcBm, 1, 1, srcBm.getWidth() - 2, srcBm.getHeight() - 2);
			srcBm.recycle();
			Field f = tgtBm.getClass().getDeclaredField("mNinePatchChunk");
			f.setAccessible(true);
			f.set(tgtBm, chunk);
			return tgtBm;
		}
		else
		{
			return srcBm;
		}
	}

	/***
	 * 通过制定目录读取文件
	 * 
	 * @param path
	 * @return
	 * @throws Exception
	 */
	public static Bitmap decodeFromFile(String path) throws Exception
	{
		Bitmap srcBm = BitmapFactory.decodeFile(path);

		if (srcBm == null)
			return null;

		srcBm.setDensity(DisplayMetrics.DENSITY_HIGH);
		byte[] chunk = readChunk(srcBm);
		boolean isNinePatchChunk = NinePatch.isNinePatchChunk(chunk);
		if (isNinePatchChunk)
		{
			try
			{
				Bitmap tgtBm = Bitmap.createBitmap(srcBm, 1, 1, srcBm.getWidth() - 2, srcBm.getHeight() - 2);
				srcBm.recycle();
				Field f = tgtBm.getClass().getDeclaredField("mNinePatchChunk");
				f.setAccessible(true);
				f.set(tgtBm, chunk);
				return tgtBm;
			}
			catch (OutOfMemoryError e)
			{
				return null;
			}
		}
		else
		{
			return srcBm;
		}
	}

	

	/***
	 * 读取Assets下图片包括.9.png
	 * 
	 * @param context
	 * @param ninePatchPngPath
	 * @return
	 * @throws Exception
	 */
	public static Bitmap readFromAsset(Context context, String ninePatchPngPath) throws Exception
	{
		InputStream is = context.getAssets().open(ninePatchPngPath);
		Bitmap bm = decodeFromStream(is);
		is.close();
		return bm;
	}

	public static void readPaddingFromChunk(byte[] chunk, Rect paddingRect)
	{
		paddingRect.left = getInt(chunk, 12);
		paddingRect.right = getInt(chunk, 16);
		paddingRect.top = getInt(chunk, 20);
		paddingRect.bottom = getInt(chunk, 24);
	}

	public static byte[] readChunk(Bitmap resourceBmp) throws IOException
	{
		final int BM_W = resourceBmp.getWidth();
		final int BM_H = resourceBmp.getHeight();

		int xPointCount = 0;
		int yPointCount = 0;
		int xBlockCount = 0;
		int yBlockCount = 0;

		ByteArrayOutputStream bao = new ByteArrayOutputStream();
		for (int i = 0; i < 32; i++)
		{
			bao.write(0);
		}

		{
			int[] pixelsTop = new int[BM_W - 2];
			resourceBmp.getPixels(pixelsTop, 0, BM_W, 1, 0, BM_W - 2, 1);
			boolean topFirstPixelIsBlack = pixelsTop[0] == Color.BLACK;
			boolean topLastPixelIsBlack = pixelsTop[pixelsTop.length - 1] == Color.BLACK;
			int tmpLastColor = Color.TRANSPARENT;
			for (int i = 0, len = pixelsTop.length; i < len; i++)
			{
				if (tmpLastColor != pixelsTop[i])
				{
					xPointCount++;
					writeInt(bao, i);
					tmpLastColor = pixelsTop[i];
				}
			}
			if (topLastPixelIsBlack)
			{
				xPointCount++;
				writeInt(bao, pixelsTop.length);
			}
			xBlockCount = xPointCount + 1;
			if (topFirstPixelIsBlack)
			{
				xBlockCount--;
			}
			if (topLastPixelIsBlack)
			{
				xBlockCount--;
			}
		}

		{
			int[] pixelsLeft = new int[BM_H - 2];
			resourceBmp.getPixels(pixelsLeft, 0, 1, 0, 1, 1, BM_H - 2);
			boolean firstPixelIsBlack = pixelsLeft[0] == Color.BLACK;
			boolean lastPixelIsBlack = pixelsLeft[pixelsLeft.length - 1] == Color.BLACK;
			int tmpLastColor = Color.TRANSPARENT;
			for (int i = 0, len = pixelsLeft.length; i < len; i++)
			{
				if (tmpLastColor != pixelsLeft[i])
				{
					yPointCount++;
					writeInt(bao, i);
					tmpLastColor = pixelsLeft[i];
				}
			}
			if (lastPixelIsBlack)
			{
				yPointCount++;
				writeInt(bao, pixelsLeft.length);
			}
			yBlockCount = yPointCount + 1;
			if (firstPixelIsBlack)
			{
				yBlockCount--;
			}
			if (lastPixelIsBlack)
			{
				yBlockCount--;
			}
		}

		{
			for (int i = 0; i < xBlockCount * yBlockCount; i++)
			{
				writeInt(bao, NO_COLOR);
			}
		}

		byte[] data = bao.toByteArray();
		data[0] = 1;
		data[1] = (byte) xPointCount;
		data[2] = (byte) yPointCount;
		data[3] = (byte) (xBlockCount * yBlockCount);
		dealPaddingInfo(resourceBmp, data);
		return data;
	}

	private static void dealPaddingInfo(Bitmap bm, byte[] data)
	{
		{ // padding left & padding right
			int[] bottomPixels = new int[bm.getWidth() - 2];
			bm.getPixels(bottomPixels, 0, bottomPixels.length, 1, bm.getHeight() - 1, bottomPixels.length, 1);
			for (int i = 0; i < bottomPixels.length; i++)
			{
				if (Color.BLACK == bottomPixels[i])
				{ // padding left
					writeInt(data, 12, i);
					break;
				}
			}
			for (int i = bottomPixels.length - 1; i >= 0; i--)
			{
				if (Color.BLACK == bottomPixels[i])
				{ // padding right
					writeInt(data, 16, bottomPixels.length - i - 2);
					break;
				}
			}
		}
		{ // padding top & padding bottom
			int[] rightPixels = new int[bm.getHeight() - 2];
			bm.getPixels(rightPixels, 0, 1, bm.getWidth() - 1, 0, 1, rightPixels.length);
			for (int i = 0; i < rightPixels.length; i++)
			{
				if (Color.BLACK == rightPixels[i])
				{ // padding top
					writeInt(data, 20, i);
					break;
				}
			}
			for (int i = rightPixels.length - 1; i >= 0; i--)
			{
				if (Color.BLACK == rightPixels[i])
				{ // padding bottom
					writeInt(data, 24, rightPixels.length - i - 2);
					break;
				}
			}
		}
	}

	private static void writeInt(OutputStream out, int v) throws IOException
	{
		out.write((v >> 0) & 0xFF);
		out.write((v >> 8) & 0xFF);
		out.write((v >> 16) & 0xFF);
		out.write((v >> 24) & 0xFF);
	}

	private static void writeInt(byte[] b, int offset, int v)
	{
		b[offset + 0] = (byte) (v >> 0);
		b[offset + 1] = (byte) (v >> 8);
		b[offset + 2] = (byte) (v >> 16);
		b[offset + 3] = (byte) (v >> 24);
	}

	private static int getInt(byte[] bs, int from)
	{
		int b1 = bs[from + 0];
		int b2 = bs[from + 1];
		int b3 = bs[from + 2];
		int b4 = bs[from + 3];
		int i = b1 | (b2 << 8) | (b3 << 16) | b4 << 24;
		return i;
	}

	/** print chunk info from bitmap */
	public static void printChunkInfo(Bitmap bm)
	{
		byte[] chunk = bm.getNinePatchChunk();
		if (null == chunk)
		{
			System.out.println("can't find chunk info from this bitmap(" + bm + ")");
			return;
		}
		int xLen = chunk[1];
		int yLen = chunk[2];
		int cLen = chunk[3];

		StringBuilder sb = new StringBuilder();
		int peddingLeft = getInt(chunk, 12);
		int paddingRight = getInt(chunk, 16);
		int paddingTop = getInt(chunk, 20);
		int paddingBottom = getInt(chunk, 24);
		sb.append("peddingLeft=" + peddingLeft);
		sb.append("\r\n");
		sb.append("paddingRight=" + paddingRight);
		sb.append("\r\n");
		sb.append("paddingTop=" + paddingTop);
		sb.append("\r\n");
		sb.append("paddingBottom=" + paddingBottom);
		sb.append("\r\n");

		sb.append("x info=");
		for (int i = 0; i < xLen; i++)
		{
			int vv = getInt(chunk, 32 + i * 4);
			sb.append("," + vv);
		}
		sb.append("\r\n");
		sb.append("y info=");
		for (int i = 0; i < yLen; i++)
		{
			int vv = getInt(chunk, xLen * 4 + 32 + i * 4);
			sb.append("," + vv);
		}
		sb.append("\r\n");
		sb.append("color info=");
		for (int i = 0; i < cLen; i++)
		{
			int vv = getInt(chunk, xLen * 4 + yLen * 4 + 32 + i * 4);
			sb.append("," + vv);
		}
		System.err.println("" + sb);
	}
	//////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	//读取下拉列表框的背景图片，
	//context第三方的context
	//patch x5_打头，文件名，不用带后缀.
//	public static Drawable createListboxDrawable(Context context, String path) 
//	{
//		try{
//		NinePatchDrawable d;
//		Options opt= new Options();
//		Bitmap bm = SmttResource.decodeResource(SmttResource.loadIdentifierResource(path, SmttResource.TYPE_DRAWABLE), opt);
//		if (bm == null)
//		{
//			return null;
//		}
//		bm.setDensity(DisplayMetrics.DENSITY_HIGH);
//		Rect paddingRect = new Rect();
//		NinePatchUtils.readPaddingFromChunk(bm.getNinePatchChunk(), paddingRect);
//		d = new NinePatchDrawable(context.getResources(), bm, bm.getNinePatchChunk(), paddingRect, null);
//		d.getPaint().setAntiAlias(true);
//		d.setFilterBitmap(true);
//		return d;
//		}
//		catch(Exception e)
//		{
//			e.printStackTrace();
//			return null;
//		}
//	}
	/*
	 * Only Use for SDK 页面滚动条
	 * 读取asset目录下 skin/bmp/lsjd的滚动条图片
	 * 传入参数完整文件名
	 * 
	**从ui工程获得滚动条图片，图片在asset目录下
	* SDK从浏览器的asset目录下获取滚动条图片
	* robins
	* */
	public static  Drawable getDrawableFromDefaultSkin(Context ctx, String fileName)
	{
		String finalFileName =  fileName;
		if (!TextUtils.isEmpty(finalFileName))
		{
			// 构建完整路径
			StringBuilder builder = new StringBuilder("skin/bmp/");
			builder.append("lsjd");
			builder.append("/");
			builder.append(finalFileName);
			String filePath = builder.toString();

			InputStream input = null;
			try
			{
				input = ctx.getAssets().open(filePath);
			}
			catch (IOException e)
			{
				e.printStackTrace();
				Log.e("FileUtil", e.getMessage());
				return null;
			}
			if (input == null)
			{
				return null;
			}

			Drawable d = Drawable.createFromStream(input, null);
			return d;
		}
		return null;
	}
	
	//获取web图片
	public static  Drawable getWepbFromDefaultSkin(Context context, String fileName)
	{
		String finalFileName =  fileName;
		if (!TextUtils.isEmpty(finalFileName))
		{
			// 构建完整路径
			StringBuilder builder = new StringBuilder("skin/bmp/");
			builder.append("lsjd");
			builder.append("/");
			builder.append(finalFileName);
			String filePath = builder.toString();

			InputStream input = null;
			try
			{
				input = context.getAssets().open(filePath);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				Log.e("FileUtil", e.getMessage());
				return null;
			}
			if (input == null)
			{
				return null;
			}
			
			
			InputStream in = null;
			int len = 0;
			byte[] data = null;
			try
			{
				len = input.available();
				data = new byte[len];
				in = new BufferedInputStream(input);
				in.read(data, 0, len);
			}
			catch (Exception e)
			{
				e.printStackTrace();
				return null;
			}
			finally
			{
				if (in != null)
				{
					try
					{
						in.close();
					}
					catch (IOException e)
					{
						e.printStackTrace();
						return null;
					}
				}
			}

			Bitmap b = webpToBitmap(data, len, Bitmap.Config.ARGB_8888,getPictureScale(context),context);
			return  getBmpDrawable(context, b);
		}
		return null;
	}
	public static boolean isWebP(byte[] data)
	{
		if (data.length <= 20)
			return false;

		/*
		 * inline magic matching instead of memcmp()
		 */
		// Find "RIFF" (0 ~ 3):
		if (data[0] != 'R' || data[1] != 'I' || data[2] != 'F' || data[3] != 'F')
		{
			return false;
		}

		// Find "WEBP" (8 ~ 11):
		if (data[8] != 'W' || data[9] != 'E' || data[10] != 'B' || data[11] != 'P')
		{
			return false;
		}

		/*
		 * Find "VP8"
		 * older is "VP8 "
		 * newer is "VP8X" or "VP8L"
		 */
		if (data[12] != 'V' || data[13] != 'P' || data[14] != '8')
		{
			return false;
		}
		return true;
	}
	/**
	 * 
	 * webp格式的数据文件转换成Bitmap
	 * 
	 * @param buffer
	 *            从webp文件里读取的全部文件内容数据
	 * @param len
	 * @param config
	 *            目前只支持ARGB8888
	 * @param scale
	 * @return
	 */
	public static Bitmap webpToBitmap(byte[] buffer, int len, Bitmap.Config config, float scale,Context context)
	{
		if (!isWebP(buffer))
		{
			return null;
		}
		// long now = System.currentTimeMillis();
		int[] width = new int[] { 0 };
		int[] height = new int[] { 0 };
		Bitmap b = null;
		try
		{
			int[] int_pixels = libwebp.getInstance(context).decodeBase(buffer, width, height);
			b = Bitmap.createBitmap(int_pixels, width[0], height[0], Bitmap.Config.ARGB_8888);
		}
		catch (OutOfMemoryError e)
		{
			e.printStackTrace();
		}
		catch (Exception e1)
		{
			e1.printStackTrace();
		}
		// 先暂时在java层做缩放
		Bitmap newB = b;
		// now = System.currentTimeMillis();
		if (b != null && Math.abs(scale - 1.0f) > 0.001f)
		{
			// 根据当前密度做一下缩放
			int w = (int) (b.getWidth() * scale);
			int h = (int) (b.getHeight() * scale);
			// 有些图片长宽可能为1，缩放后变成0了
			if (w <= 0)
			{
				w = 1;
			}
			if (h <= 0)
			{
				h = 1;
			}
			if (w > 0 && h > 0)
			{
				try
				{
					newB = Bitmap.createScaledBitmap(b, w, h, true);//三星手机返回的
				}
				catch (OutOfMemoryError e)
				{
					e.printStackTrace();
				}
				catch (Exception e1)
				{
					
				}
				if (newB != b)
				{
					b.recycle();
				}
			}
			// Logger.d("scaleWebP", "scale cost time:" +
			// (System.currentTimeMillis() - now));
		}

		return newB;

	}
	public static BitmapDrawable getBmpDrawable(Context ctx, Bitmap bitmap)
	{
		BitmapDrawable bd = null;
		Constructor			mBmpConstructor			= null;//三方调用，尽量不用static，避免资源泄漏.
		try
		{
			Class[] arrayOfClass = new Class[2];
			arrayOfClass[0] = Resources.class;
			arrayOfClass[1] = Bitmap.class;
			mBmpConstructor = BitmapDrawable.class.getConstructor(arrayOfClass);
			Object[] arrayOfObject = new Object[2];
			//arrayOfObject[0] = SmttResource.getResources();//用QQ浏览器Resource还不行
			arrayOfObject[0] = ctx.getResources();//需要用第三方调用的resource
			arrayOfObject[1] = bitmap;
			bd = (BitmapDrawable) mBmpConstructor.newInstance(arrayOfObject);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			bd = new BitmapDrawable(bitmap);//三星手机，由于createScaleBitmap返回的宽高为-1，-1。用这个构造转化drawable会得到原图.
		}
		return bd;
	}
	//工具函数,获取屏幕density
	private static float mDensity= 0f;
	public static float getPictureScale(Context context)
	{
		float EPSINON = 0.000001f;//浮点精度
		if(Math.abs(mDensity) > EPSINON)// float判0
			return mDensity;
		android.view.WindowManager manager = (android.view.WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		if(manager == null)
			return 1f;
		DisplayMetrics dm = new DisplayMetrics();
		manager.getDefaultDisplay().getMetrics(dm);
		mDensity= dm.density/2;// 和UI的缩放比对齐
		if( Math.abs(mDensity) < EPSINON) 
			return 1f;
		return mDensity;
	}
}

