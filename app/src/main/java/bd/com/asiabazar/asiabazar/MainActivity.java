package bd.com.asiabazar.asiabazar;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends Activity {

    private WebView asiaBazaarView;
    private SwipeRefreshLayout asiaBazaarRefresher;
    private ProgressDialog asiaBazaarLoader;
    private LinearLayout llWebViewContainer;
    private boolean doubleBackPressed = false;
    private ValueCallback<Uri[]> abCallback;
    private String abCameraPhotoPath;
    private static final int AB_INPUT_FILE = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        llWebViewContainer = (LinearLayout) findViewById(R.id.ll_web_view_container);
        asiaBazaarView = (WebView) findViewById(R.id.web_view_asia_bazaar);
        asiaBazaarView.setBackgroundColor(Color.parseColor("#03A9F4"));
        asiaBazaarLoader = new ProgressDialog(MainActivity.this);
        asiaBazaarRefresher = (SwipeRefreshLayout) findViewById(R.id.refresh_layout_asia_bazaar);
        asiaBazaarRefresher.setColorSchemeColors(Color.parseColor("#03A9F4"));
        asiaBazaarRefresher.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                asiaBazaarRefresher.setRefreshing(true);
                getAsiaBazaarViewer();
            }
        });
        registerReceiver(ncStatusReceiver, new IntentFilter("asia_bazaar_nc"));

        asiaBazaarLoader.setMessage("Loading...");
        asiaBazaarLoader.setCancelable(false);
        asiaBazaarLoader.show();
        getAsiaBazaarViewer();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ncStatusReceiver != null) {
            unregisterReceiver(ncStatusReceiver);
        }
    }

    private void getAsiaBazaarViewer() {
        if (isNetworkAvailable()) {
            loadAsiaBazaar();
        } else {
            if (asiaBazaarLoader.isShowing()) {
                asiaBazaarLoader.dismiss();
            }
            if (asiaBazaarRefresher.isRefreshing()) {
                asiaBazaarRefresher.setRefreshing(false);
            }
            asiaBazaarAlert();
        }
    }

    private void loadAsiaBazaar() {
        WebSettings abSettings = asiaBazaarView.getSettings();
        abSettings.setJavaScriptEnabled(true);
        abSettings.setAppCacheEnabled(true);
        abSettings.setDatabaseEnabled(true);
        abSettings.setDomStorageEnabled(true);
        abSettings.setAllowFileAccess(true);
        abSettings.setJavaScriptCanOpenWindowsAutomatically(true);

        abSettings.setUseWideViewPort(true);
        abSettings.setSavePassword(true);
        abSettings.setSaveFormData(true);
        abSettings.setEnableSmoothTransition(true);
        abSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        abSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        abSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        asiaBazaarView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        asiaBazaarView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (asiaBazaarLoader.isShowing()) {
                    asiaBazaarLoader.dismiss();
                }
                if (asiaBazaarRefresher.isRefreshing()) {
                    asiaBazaarRefresher.setRefreshing(false);
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                asiaBazaarView.stopLoading();
                asiaBazaarView.canGoBack();
                asiaBazaarView.loadUrl("about:blank");
                super.onReceivedError(view, request, error);
            }
        });
        asiaBazaarView.loadUrl(getString(R.string.asia_bazaar_url));
        asiaBazaarView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (abCallback != null) {
                    abCallback.onReceiveValue(null);
                }
                abCallback = filePathCallback;
                Intent pIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (pIntent.resolveActivity(MainActivity.this.getPackageManager()) != null) {
                    File photoFile = null;
                    try {
                        photoFile = createImgFile();
                        pIntent.putExtra("PhotoPath", abCameraPhotoPath);
                    } catch (IOException ex) {
                        Log.e("bb", "Unable to create Image File", ex);
                    }
                    if (photoFile != null) {
                        abCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                        pIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                                Uri.fromFile(photoFile));
                    } else {
                        pIntent = null;
                    }
                }
                Intent conSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                conSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                conSelectionIntent.setType("image/*");
                Intent[] intentArray;
                if (pIntent != null) {
                    intentArray = new Intent[]{pIntent};
                } else {
                    intentArray = new Intent[0];
                }
                Intent cIntent = new Intent(Intent.ACTION_CHOOSER);
                cIntent.putExtra(Intent.EXTRA_INTENT, conSelectionIntent);
                cIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
                cIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                startActivityForResult(cIntent, AB_INPUT_FILE);
                return true;
            }
        });
    }

    private File createImgFile() throws IOException {
        File stgDir = this.getCacheDir();
        String timeStm = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imgFName = "JPEG_" + timeStm + "_";
        File imFile = File.createTempFile(imgFName, ".jpg", stgDir);
        return imFile;
    }

    private void asiaBazaarAlert() {
        Dialog dialog = new Dialog(MainActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.asia_bazaar_alert);
        ImageView iv = (ImageView) dialog.findViewById(R.id.iv_wifi_off);
        iv.setImageDrawable(getResources().getDrawable(R.drawable.ic_wifi_off));
        dialog.findViewById(R.id.tv_refresh).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                overridePendingTransition(0, 0);
                startActivity(intent);
            }
        });
        dialog.findViewById(R.id.tv_internet_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(Settings.ACTION_SETTINGS), 0);
            }
        });
        dialog.show();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    @Override
    public void onBackPressed() {
        if (doubleBackPressed) {
            asiaBazaarView.canGoBack();
            finish();
        } else {
            doubleBackPressed = true;
            snackBarAlert();
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    doubleBackPressed = false;
                }
            }, 3000);
        }
    }

    private void snackBarAlert() {
        Snackbar snackbar = Snackbar.make(llWebViewContainer, R.string.back_press, Snackbar.LENGTH_LONG);
        View sbView = snackbar.getView();
        sbView.setBackgroundColor(getResources().getColor(R.color.asiaBazaarBGColor));
        TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(Color.WHITE);
        snackbar.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != AB_INPUT_FILE || abCallback == null) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        Uri[] results = null;
        if (resultCode == Activity.RESULT_OK) {
            if (data == null) {
                if (abCameraPhotoPath != null) {
                    results = new Uri[]{Uri.parse(abCameraPhotoPath)};
                }
            } else {
                String dataString = data.getDataString();
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                }
            }
        }
        abCallback.onReceiveValue(results);
        abCallback = null;
    }

    private BroadcastReceiver ncStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getBooleanExtra("status", false)) {
                if (isNetworkAvailable()) {
                    finish();
                    Intent intents = new Intent(MainActivity.this, MainActivity.class);
                    intents.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    overridePendingTransition(0, 0);
                    startActivity(intents);
                }
            }
        }
    };
}