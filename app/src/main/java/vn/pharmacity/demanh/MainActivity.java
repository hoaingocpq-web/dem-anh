package vn.pharmacity.demanh;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Vỏ WebView: mở web app Đếm Ảnh (Apps Script), hỗ trợ chụp ảnh và chọn ảnh có sẵn. */
public class MainActivity extends Activity {
    private static final int REQ_FILE = 1001;

    private WebView web;
    private ValueCallback<Uri[]> fileCallback;
    private Uri cameraUri;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setMediaPlaybackRequiresUserGesture(true);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                String host = r.getUrl().getHost() == null ? "" : r.getUrl().getHost();
                boolean inApp = host.endsWith("google.com") || host.endsWith("googleusercontent.com");
                if (inApp) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, r.getUrl())); } catch (Exception ignored) { }
                return true;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = cb;
                try {
                    Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
                    pick.addCategory(Intent.CATEGORY_OPENABLE);
                    pick.setType("image/*");

                    List<Intent> extra = new ArrayList<>();
                    Intent cam = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (cam.resolveActivity(getPackageManager()) != null) {
                        File dir = new File(getCacheDir(), "photos");
                        //noinspection ResultOfMethodCallIgnored
                        dir.mkdirs();
                        File f = new File(dir, "p" + System.currentTimeMillis() + ".jpg");
                        cameraUri = FileProvider.getUriForFile(MainActivity.this, getPackageName() + ".fileprovider", f);
                        cam.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
                        cam.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        extra.add(cam);
                    }

                    Intent chooser;
                    if (p.isCaptureEnabled() && !extra.isEmpty()) {
                        chooser = cam;                                   // trang yêu cầu chụp ảnh
                    } else {
                        chooser = Intent.createChooser(pick, "Chọn ảnh");
                        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, extra.toArray(new Intent[0]));
                    }
                    startActivityForResult(chooser, REQ_FILE);
                } catch (ActivityNotFoundException e) {
                    fileCallback = null;
                    Toast.makeText(MainActivity.this, "Không mở được máy ảnh hoặc thư viện ảnh", Toast.LENGTH_LONG).show();
                    return false;
                }
                return true;
            }
        });

        if (b == null) web.loadUrl(getString(R.string.app_url));
        else web.restoreState(b);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQ_FILE || fileCallback == null) return;
        Uri[] out = null;
        if (res == RESULT_OK) {
            if (data != null && data.getData() != null) out = new Uri[]{data.getData()};
            else if (cameraUri != null) out = new Uri[]{cameraUri};
        }
        fileCallback.onReceiveValue(out);
        fileCallback = null;
        cameraUri = null;
    }

    @Override
    protected void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);
        web.saveState(b);
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (web != null) { ((android.view.ViewGroup) web.getParent()).removeView(web); web.destroy(); }
        super.onDestroy();
    }
}
