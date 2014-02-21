package com.example.epublib;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.epub.EpubReader;
import android.app.Activity;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.webkit.JavascriptInterface;
import android.widget.ImageView;
import android.widget.TextView;


/**

 * Log the info of 'assets/books/testbook.epub'.

 *

 * @author paul.siegmann

 *

 */

public class MainActivity extends Activity {

	private static final int SWIPE_MIN_DISTANCE = 120;
	private static final int SWIPE_THRESHOLD_VELOCITY = 200;
	private int CHANGE_PAGE_DISTANCE;
	private int SCREEN_WIDTH;
	private GestureDetector mDetector;

	private MyWebView	mWebview;
	private Book 		book;
	private int 		mCurrentPage;
	private int 		mCurrentChapter;
	private int 		mNbPage;
	private String 		mData;
	private View 		mFrameContent;
	private int 		mPageWidth;
	private View	 	mProgress;
	private TextView 	mInfo;
	private boolean 	mGoBack;
	private MyWebView 	mWebviewBack;
	private int 		mPageRealWidth;
	private Handler 	mHandler;
	private String 		mDataBackData;
	private View 		mPageBefore;
	private View 		mPageCurrent;
	private View 		mPageAfter;
	private View 		mWebviewContainer;
	protected float 	mPosDown;
	protected float 	mPosView;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		Display display = getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		CHANGE_PAGE_DISTANCE = Math.min(size.x / 3, 200);
		SCREEN_WIDTH = size.x;

		mDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
			private static final String DEBUG_TAG = "Gestures"; 

			@Override
			public boolean onDown(MotionEvent event) { 
				Log.d(DEBUG_TAG,"onDown: " + event.toString()); 
				return true;
			}

			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {	            
				if (e1.getX() - e2.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					goNextPage();
					return true;
				} else if (e2.getX() - e1.getX() > SWIPE_MIN_DISTANCE && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY) {
					goPreviousPage();
					return true;
				}
				return false;

			}
		});

		mProgress = findViewById(R.id.progress);
		mWebviewContainer = findViewById(R.id.webview_container);

		mInfo = (TextView)findViewById(R.id.info);

		mWebviewBack = (MyWebView)findViewById(R.id.webView2);
		initWebView(mWebviewBack);

		mWebview = (MyWebView)findViewById(R.id.webView1);
		initWebView(mWebview);

		mPageBefore = findViewById(R.id.page1);
		mPageBefore.setTag(mWebview);
		mPageCurrent = findViewById(R.id.page2);
		mPageCurrent.setTag(mWebviewBack);

		mHandler = new Handler();

		mFrameContent = findViewById(R.id.frame_content);
		//	    mFrameContent.setOnTouchListener(new OnTouchListener() {		
		//			@Override
		//			public boolean onTouch(View v, MotionEvent event) {
		//				mDetector.onTouchEvent(event);
		//				return false;
		//			}
		//		});

		findViewById(R.id.button1).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				goPreviousPage();
			}
		});
		findViewById(R.id.button2).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				goNextPage();
			}
		});

		AssetManager assetManager = getAssets();

		try {

			// find InputStream for book

			//copyBookToDevice("book.epub");

			File file = new File(Environment.getExternalStorageDirectory(), "book.epub");
			//	    	InputStream epubInputStream = new FileInputStream(file);
			//assetManager.open("book.epub");


			// Load Book from inputStream
			book = (new EpubReader()).readEpubLazy(file.getAbsolutePath(), "utf-8");

			Collection<Resource> res = book.getResources().getAll();
			for (Resource r : res) {
				if (".css".equals(r.getMediaType().getDefaultExtension())) {
					String path = Environment.getExternalStorageDirectory() + "/book/" + r.getHref();
					Log.e("reader", "reader: " + r.getMediaType().getDefaultExtension());
					Log.e("reader", "reader: " + r.getHref());
					//		    	  OutputStream out = new FileOutputStream(new File(path));
					writetofile(path, r.getInputStream());
				}
			}

			Log.e("reader", "reader: " + res.size());

			// Log the book's authors
			Log.i("epublib", "author(s): " + book.getMetadata().getAuthors());

			// Log the book's title
			Log.i("epublib", "title: " + book.getTitle());

			// Log the book's coverimage property
			Bitmap coverImage = BitmapFactory.decodeStream(book.getCoverImage().getInputStream());

			Log.i("epublib", "Coverimage is " + coverImage.getWidth() + " by " + coverImage.getHeight() + " pixels");

			// Log the tale of contents
			logTableOfContents(book.getTableOfContents().getTocReferences(), 0);

			((ImageView)findViewById(R.id.cover)).setImageBitmap(coverImage);

		} catch (IOException e) {
			Log.e("epublib", e.getMessage());
		}

		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				loadChapter(6);
			}
		}, 500);
	}

	//	@Override 
	//    public boolean onTouchEvent(MotionEvent event){ 
	//        this.mDetector.onTouchEvent(event);
	//        return super.onTouchEvent(event);
	//    }

	public void writetofile(String path, InputStream input) throws IOException {
		try {
			final File file = new File(path);
			final OutputStream output = new FileOutputStream(file);
			try {
				try {
					final byte[] buffer = new byte[1024];
					int read;

					while ((read = input.read(buffer)) != -1)
						output.write(buffer, 0, read);

					output.flush();
				} finally {
					output.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} finally {
			input.close();
		}
	}

	private void initWebView(final MyWebView webview) {
		webview.getSettings().setJavaScriptEnabled(true);

		// disable scroll on touch	    
		webview.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_MOVE:
					mPageCurrent.setTranslationX(event.getRawX() - mPosDown);
					mPosView = event.getRawX() - mPosDown;
					break;
				case MotionEvent.ACTION_DOWN:
					mPosDown = event.getRawX();
					break;
				case MotionEvent.ACTION_UP:
//					mPosView = mPosDown - event.getRawX();
					if (mPosView > CHANGE_PAGE_DISTANCE) {
						Log.e("gg", "go next");

						mPageCurrent.setTranslationX(0);
						TranslateAnimation anim = new TranslateAnimation(
								Animation.ABSOLUTE,
								mPosView,
								Animation.ABSOLUTE,
								SCREEN_WIDTH,
								Animation.ABSOLUTE,
								0,
								Animation.ABSOLUTE,
								0);
//						TranslateAnimation anim = new TranslateAnimation(mPosView, 0, 0, 0);
						anim.setRepeatMode(0);
						anim.setDuration(200);
						anim.setFillAfter(true);
						mPageCurrent.startAnimation(anim);	
					} else {
						Log.e("gg", "cancel");

						mPageCurrent.setTranslationX(0);
						TranslateAnimation anim = new TranslateAnimation(
								Animation.ABSOLUTE,
								mPosView,
								Animation.ABSOLUTE,
								0,
								Animation.ABSOLUTE,
								0,
								Animation.ABSOLUTE,
								0);
//						TranslateAnimation anim = new TranslateAnimation(mPosView, 0, 0, 0);
						anim.setRepeatMode(0);
						anim.setDuration(200);
						anim.setFillAfter(true);
						mPageCurrent.startAnimation(anim);	
					}
					
					break;
				default:
					mDetector.onTouchEvent(event);
				}
				return true;
			}

		});

		webview.addJavascriptInterface(new Object() { 	
			@JavascriptInterface
			public void getWidth(final int width) {
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						Log.e("width", "width: " + width);

						mNbPage = width / mPageWidth;
						loadPage(mGoBack ? mNbPage - 1 : 0);

						mProgress.setVisibility(View.GONE);
					}
				});
			}
		}, "JavaCallback");		
	}


	public void goPreviousPage() {
		if (mNbPage == -1) {
			return;
		}

		mGoBack = true;

		if (mCurrentPage == 0) {
			loadChapter(mCurrentChapter - 1);	
		} else {
			loadPage(mCurrentPage - 1);
		}

		anim();
	}

	public void goNextPage() {
		if (mNbPage == -1) {
			return;
		}

		mGoBack = false;

		if (mCurrentPage == mNbPage - 1) {
			loadChapter(mCurrentChapter + 1);	
		} else {
			loadPage(mCurrentPage + 1);
		}

		anim();
	}

	private void anim() {

		TranslateAnimation anim = new TranslateAnimation(
				Animation.ABSOLUTE,
				0,
				Animation.ABSOLUTE,
				(mGoBack ? mPageRealWidth : -mPageRealWidth),
				Animation.ABSOLUTE,
				0,
				Animation.ABSOLUTE,
				0);
		anim.setRepeatMode(0);
		anim.setDuration(300);
		anim.setFillAfter(false);
		anim.setAnimationListener(new AnimationListener() {	
			@Override
			public void onAnimationStart(Animation animation) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationRepeat(Animation animation) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				View back = mPageBefore;
				mPageBefore = mPageCurrent;
				mPageCurrent = back;
				mPageCurrent.bringToFront();

				MyWebView webview = (MyWebView)mPageBefore.getTag();
				webview.loadUrl("javascript:window.scrollTo(" + (mPageWidth * mCurrentPage) + ", 0)");
			}
		});
		//	  mPage1.setTranslationX(-mPage1.getTranslationX());
		//	  mPage2.bringToFront();
		mPageCurrent.startAnimation(anim);	
	}

	private void loadPage(int page) {
		Log.i("Reader", "loadPage: " + page + " / " + (mNbPage - 1));
		mInfo.setText(page + " / " + (mNbPage - 1));

		mCurrentPage = page;
		mWebview.loadUrl("javascript:window.scrollTo(" + (mPageWidth * page) + ", 0)");	
		mWebviewBack.loadUrl("javascript:window.scrollTo(" + (mPageWidth * page) + ", 0)");	
	}

	private void loadChapter(int chapter) {
		Log.i("Reader", "loadChapter: " + chapter);

		try {
			mProgress.setVisibility(View.VISIBLE);
			mNbPage = -1;
			mCurrentChapter = chapter;
			InputStream in = book.getContents().get(chapter).getInputStream();
			byte[] contents = new byte[1024];
			int bytesRead = 0;
			mData = new String(); 
			while( (bytesRead = in.read(contents)) != -1) {
				mData += new String(contents, 0, bytesRead);
			}
			Resources resources = getResources();
			DisplayMetrics metrics = resources.getDisplayMetrics();
			int height = (int) (mFrameContent.getHeight() / (metrics.densityDpi / 160f));
			int width = (int) ((mFrameContent.getWidth() - 2) / (metrics.densityDpi / 160f));
//			int margin = (int) (2 / (metrics.densityDpi / 160f));
			mPageRealWidth = mFrameContent.getWidth();
			mPageWidth = width;
			addStyle("html { padding: 0; } body { margin: 0; padding: 0; -webkit-column-gap: 0; -webkit-column-width: " + width + "px; height: " + height + "px}");

			//book.getResources().getByHref(href)

			mWebview.loadDataWithBaseURL("file://"+Environment.getExternalStorageDirectory()+"/book/", mData, "text/html; charset=UTF-8", "utf-8", null);
			//		  mWebview.loadData(mData, "text/html; charset=UTF-8", null);

			addJavascript("window.addEventListener (\"load\", function() { window.JavaCallback.getWidth(document.body.scrollWidth); }, false);");
			mWebviewBack.loadDataWithBaseURL("file://"+Environment.getExternalStorageDirectory()+"/book/", mData, "text/html; charset=UTF-8", "utf-8", null);
			//		  mWebviewBack.loadData(mData, "text/html; charset=UTF-8", null);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void copyBookToDevice(String fileName) {     
		System.out.println("Copy Book to donwload folder in phone");
		try
		{
			InputStream localInputStream = new FileInputStream(Environment.getExternalStorageDirectory() + "/" + fileName);//getAssets().open("books/"+fileName);
			String path = Environment.getExternalStorageDirectory() + "/books/"+fileName;
			FileOutputStream localFileOutputStream = new FileOutputStream(path);

			byte[] arrayOfByte = new byte[1024];
			int offset;
			while ((offset = localInputStream.read(arrayOfByte))>0)
			{
				localFileOutputStream.write(arrayOfByte, 0, offset);                  
			}
			localFileOutputStream.close();
			localInputStream.close();
			Log.d("Reader", fileName+" copied to phone");   

		}
		catch (IOException localIOException)
		{
			localIOException.printStackTrace();
			Log.d("Reader", "failed to copy");
			return;
		}
	}

	private void addJavascript(String string) {
		addIntoHeader("<script type=\"text/javascript\">\n" + string + "\n</script>");
	}

	public void addStyle(String css)
	{
		addIntoHeader("<style type=\"text/css\">\n" + css + "\n</style>");
	}

	public void addIntoHeader(String code)
	{
		addBeforeTag("/head", code);
	}

	public void addBeforeTag(String tag, String code)
	{
		mData = mData.replaceFirst("(?i)(<"+tag+"(.*?)>)", "\n" + code + "\n$1");
	}


	/**

	 * Recursively Log the Table of Contents

	 *

	 * @param tocReferences

	 * @param depth

	 */

	private void logTableOfContents(List<TOCReference> tocReferences, int depth) {

		if (tocReferences == null) {

			return;

		}

		for (TOCReference tocReference : tocReferences) {

			StringBuilder tocString = new StringBuilder();

			for (int i = 0; i < depth; i++) {

				tocString.append("\t");

			}

			tocString.append(tocReference.getTitle());

			Log.i("epublib", tocString.toString());


			logTableOfContents(tocReference.getChildren(), depth + 1);

		}

	}

}
