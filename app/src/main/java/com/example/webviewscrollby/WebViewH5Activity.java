package com.example.webviewscrollby;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieSyncManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.webviewscrollby.controller.ProgressBarController;
import com.example.webviewscrollby.widget.H5NoNetView;

import java.util.Timer;
import java.util.TimerTask;

public class WebViewH5Activity extends AppCompatActivity {
    private String blackUrl = "about:blank";//浏览器空白页
    private MsWebView webView;
    private HideBarTimeTask hideBarTimeTask;
    private Button btn_gototop;
    private H5NoNetView mH5NoNetView;
    private ProgressBar progressbar;
    private Timer hideProgressBarTimer;
    private long exitTime = 0;

    protected String rootUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web_view_h5_activity);

        rootUrl = getIntent().getStringExtra("site_url");
        if (rootUrl != null) {
            rootUrl = rootUrl.trim();
        }
        initWebView();
        loadWebView();
    }

    private void loadWebView() {
        if (rootUrl.startsWith("https://") || rootUrl.startsWith("http://")) {
            //同步Cookies
            CookieSyncManager.createInstance(this);
            CookieSyncManager.getInstance().sync();
            webView.loadUrl(rootUrl);
        } else {
            webView.loadData(rootUrl, "text/html", "UTF-8");
        }
    }

    private void initWebView() {
        btn_gototop = (Button) findViewById(R.id.btn_gototop);
        webView = (MsWebView) findViewById(R.id.webView);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDefaultTextEncodingName("UTF-8");
        webSettings.setUseWideViewPort(true);

        webView.setWebViewClient(new WebViewClient() );

        //比如这里做一个简单的判断，当页面发生滚动，显示那个 Button
        webView.setOnScrollChangedCallback(new MsWebView.OnScrollChangedCallback() {
            @Override
            public void onScroll(int dx, int dy) {
                if (webView.getScrollY() > 0) {
                    btn_gototop.setVisibility(View.VISIBLE);
                } else {
                    btn_gototop.setVisibility(View.GONE);
                }
            }
        });

        webView.setWebViewClient(new WebViewClient());

        webView.setWebChromeClient(new WebChromeClient());

        btn_gototop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                webView.setScrollY(0);
                btn_gototop.setVisibility(View.GONE);
            }
        });

        progressbar = findViewById(R.id.progressBar);
        mH5NoNetView = (H5NoNetView) findViewById(R.id.nonetview);
        mH5NoNetView.setRefreshListener(new H5NoNetView.HtmlReloadListener() {
            @Override
            public void triggerRefresh() {
                if (netIsAvailable()) {
                    mH5NoNetView.setVisibility(View.GONE);
                    webView.setVisibility(View.VISIBLE);
                }
                loadWebView();
            }
        });
    }

    public boolean netIsAvailable() {
        //检测网络是否可用
        ConnectivityManager cwjManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cwjManager.getActiveNetworkInfo() != null && cwjManager.getActiveNetworkInfo().isAvailable();
    }

    private void runTimer(int delay) {
        stopTimer();
        hideProgressBarTimer = new Timer(true);
        hideBarTimeTask = new HideBarTimeTask();
        hideProgressBarTimer.schedule(hideBarTimeTask, delay);
    }

    private void stopTimer() {
        if (hideBarTimeTask != null) {
            hideBarTimeTask.cancel();
            hideBarTimeTask = null;
        }

        if (hideProgressBarTimer != null) {
            hideProgressBarTimer.cancel();
            hideProgressBarTimer.purge();
            hideProgressBarTimer = null;
        }
    }

    private ProgressBarController progressBarController = new ProgressBarController(new ProgressBarController.ControllerListener() {

        @Override
        public void stop() {
            runTimer(500);
        }

        @Override
        public void setProgress(int progress) {
            progressbar.setProgress(progress);
        }

        @Override
        public void start() {
            if (progressbar.getVisibility() == View.GONE) {
                progressbar.setVisibility(View.VISIBLE);
            }
            stopTimer();
        }

    });

    class HideBarTimeTask extends TimerTask {
        @Override
        public void run() {
            Message msg = new Message();
            msg.what = 10000;
            webviewHandler.sendMessage(msg);
        }
    }

    private Handler webviewHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 10000) {
                progressbar.setVisibility(View.GONE);
                progressbar.setProgress(0);
            }
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (webView != null && webView.canGoBack() && checkBackUrl(blackUrl)) {
                webView.goBack();
            } else {
                webView.stopLoading();
                finish();
            }

            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    private boolean checkBackUrl(String url) {
        WebBackForwardList mWebBackForwardList = webView.copyBackForwardList();
        String backUrl = mWebBackForwardList.getItemAtIndex(mWebBackForwardList.getCurrentIndex() - 1).getUrl();
        //判断是否是空白页
        if (backUrl != null && backUrl.equalsIgnoreCase(url)) {
            return false;
        }
        return true;
    }


    private class WebViewClient extends android.webkit.WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            //开始加载，显示进度
            progressBarController.preloading();
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);

            if (failingUrl.equalsIgnoreCase(rootUrl) == false) {
                return;
            }

            webView.loadUrl(blackUrl);
            //只有加载完毕才应该调用clearHistory()
            webView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    webView.clearHistory();
                    mH5NoNetView.setVisibility(View.VISIBLE);
                    webView.setVisibility(View.GONE);
                }
            }, 500);
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            //忽略SSL证书错误检测,使用SslErrorHandler.proceed()来继续加载
            handler.proceed();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
        }
    }

    private class WebChromeClient extends android.webkit.WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
            //通知程序当前页面加载进度
            progressBarController.setCurrentValue(newProgress);
        }
    }

    private class DownloadListener implements android.webkit.DownloadListener {
        @Override
        public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
            //需要Webview开启下载监听,否则点击下载连接，没有反应
            Uri uri = Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            if ((System.currentTimeMillis() - exitTime) > 2000) {
                Toast.makeText(getApplicationContext(), "再按一次返回主页面",
                    Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            } else {
                finish();
            }

        }
    }
}