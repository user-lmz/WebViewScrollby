package com.example.webviewscrollby;

import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;

public class MsWebView extends WebView {
    private OnScrollChangedCallback mOnScrollChangedCallback;

    public MsWebView(Context context) {
        super(context);
    }

    public MsWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MsWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);
        if (mOnScrollChangedCallback != null) {
            mOnScrollChangedCallback.onScroll(l - oldl, t - oldt);
        }
    }

    public OnScrollChangedCallback getOnScrollChangedCallback() {
        return mOnScrollChangedCallback;
    }

    public void setOnScrollChangedCallback(
        final OnScrollChangedCallback onScrollChangedCallback) {
        mOnScrollChangedCallback = onScrollChangedCallback;
    }

    public static interface OnScrollChangedCallback {
        public void onScroll(int dx, int dy);
    }
}
