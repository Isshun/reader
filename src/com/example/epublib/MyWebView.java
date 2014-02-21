package com.example.epublib;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.webkit.WebView;

public class MyWebView extends WebView {


	public MyWebView(Context context) {
		super(context);
		init();
	}

	private void init() {
		
	}

	public MyWebView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public MyWebView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public int computeHorizontalScrollRange () {
		return super.computeHorizontalScrollRange();
	}
	
}
