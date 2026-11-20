package br.com.residual.mobile.preview;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends ComponentActivity {
    private static final int PICK_FILE = 10;
    private static final int SAVE_FILE = 11;
    private WebView web;
    private ProgressBar progress;
    private LinearLayout errorPanel;
    private ValueCallback<Uri[]> fileCallback;
    private File pendingDownload;
    private boolean downloading;
    private boolean pageFailed;
    private final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        int green = Color.rgb(24, 81, 38);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(green);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else {
                view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            }
            return Build.VERSION.SDK_INT >= 30 ? WindowInsets.CONSUMED : insets.consumeSystemWindowInsets();
        });
        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(8), 0, dp(8), 0);
        Button back = button("‹", "Voltar");
        back.setOnClickListener(v -> goBack());
        bar.addView(back, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView title = new TextView(this);
        title.setText("Residual");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        bar.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));
        title.setGravity(Gravity.CENTER_VERTICAL);
        Button reload = button("↻", "Atualizar sistema");
        reload.setOnClickListener(v -> reload());
        bar.addView(reload, new LinearLayout.LayoutParams(dp(48), dp(48)));
        root.addView(bar);
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        root.addView(progress, new LinearLayout.LayoutParams(-1, dp(3)));

        FrameLayout content = new FrameLayout(this);
        content.setBackgroundColor(Color.rgb(250, 252, 248));
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        web = new WebView(this);
        web.setId(View.generateViewId());
        content.addView(web, new FrameLayout.LayoutParams(-1, -1));
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSafeBrowsingEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, false);
        WebView.setWebContentsDebuggingEnabled(false);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;
                String target = request.getUrl().toString();
                if (NavigationPolicy.isInternal(target)) return false;
                openExternal(target);
                return true;
            }
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap icon) {
                pageFailed = false;
                errorPanel.setVisibility(View.GONE);
                progress.setVisibility(View.VISIBLE);
            }
            @Override public void onPageFinished(WebView view, String url) {
                progress.setVisibility(View.GONE);
                CookieManager.getInstance().flush();
                if (!pageFailed) errorPanel.setVisibility(View.GONE);
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) showNetworkError();
            }
            @Override public void onReceivedHttpError(WebView view, WebResourceRequest request, android.webkit.WebResourceResponse response) {
                // GitHub Pages serves the valid SPA entry as 404.html on direct routes.
                boolean pagesFallback = response.getStatusCode() == 404
                    && NavigationPolicy.isInternal(request.getUrl().toString());
                if (request.isForMainFrame() && response.getStatusCode() >= 400 && !pagesFallback) showNetworkError();
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int percent) { progress.setProgress(percent); }
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                pick.addCategory(Intent.CATEGORY_OPENABLE);
                pick.setType("*/*");
                String[] types = params.getAcceptTypes();
                if (types.length > 0 && !types[0].isEmpty()) pick.putExtra(Intent.EXTRA_MIME_TYPES, types);
                pick.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, params.getMode() == FileChooserParams.MODE_OPEN_MULTIPLE);
                try { startActivityForResult(pick, PICK_FILE); }
                catch (ActivityNotFoundException error) { fileCallback.onReceiveValue(null); fileCallback = null; toast("Nenhum seletor de arquivos disponível."); }
                return true;
            }
        });
        configureDownloads();
        errorPanel = new LinearLayout(this);
        errorPanel.setOrientation(LinearLayout.VERTICAL);
        errorPanel.setGravity(Gravity.CENTER);
        errorPanel.setPadding(dp(28), dp(28), dp(28), dp(28));
        errorPanel.setBackgroundColor(Color.rgb(250, 252, 248));
        TextView message = new TextView(this);
        message.setText("Não foi possível abrir o Residual.\n\nConfira sua conexão com a internet e tente novamente.");
        message.setTextSize(18);
        message.setGravity(Gravity.CENTER);
        errorPanel.addView(message);
        Button retry = new Button(this);
        retry.setText("Tentar novamente");
        retry.setOnClickListener(v -> reload());
        errorPanel.addView(retry);
        errorPanel.setVisibility(View.GONE);
        content.addView(errorPanel, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        root.requestApplyInsets();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { goBack(); }
        });
        if (state == null || web.restoreState(state) == null) web.loadUrl(NavigationPolicy.HOME);
    }

    private void configureDownloads() {
        boolean bridgeAvailable = WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER);
        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            WebViewCompat.addWebMessageListener(web, "ResidualDownloads",
                Collections.singleton("https://ragcodebr.github.io"), (view, message, origin, mainFrame, reply) -> {
                    if (!mainFrame || !NavigationPolicy.isInternal(view.getUrl())) return;
                    String raw = message.getData();
                    if (raw == null || raw.length() > NavigationPolicy.MAX_BLOB_BYTES * 4L / 3 + 8192) return;
                    try {
                        JSONObject data = new JSONObject(raw);
                        if (data.has("error")) { toast("Não foi possível baixar o arquivo. Limite: 20 MB para arquivos gerados."); return; }
                        if (downloading || pendingDownload != null) { toast("Conclua o download atual primeiro."); return; }
                        byte[] bytes = Base64.decode(data.getString("base64"), Base64.DEFAULT);
                        if (bytes.length > NavigationPolicy.MAX_BLOB_BYTES) { toast("O arquivo excede 20 MB."); return; }
                        String name = data.optString("name", "documento.pdf");
                        String type = data.optString("type", "application/octet-stream");
                        downloading = true;
                        io.execute(() -> {
                            try {
                                File temp = File.createTempFile("residual-", ".download", getCacheDir());
                                try (OutputStream output = new FileOutputStream(temp)) { output.write(bytes); }
                                runOnUiThread(() -> offerSave(temp, name, type));
                            } catch (Exception error) { runOnUiThread(() -> { downloading = false; toast("Não foi possível preparar o arquivo."); }); }
                        });
                    } catch (Exception error) { toast("Arquivo inválido."); }
                });
        }
        web.setDownloadListener((url, userAgent, disposition, mime, length) -> {
            if (url.startsWith("blob:")) {
                if (!bridgeAvailable) { toast("Atualize o Android System WebView para baixar arquivos gerados."); return; }
                if (!NavigationPolicy.isInternal(web.getUrl())) return;
                String script = "(async()=>{try{const r=await fetch(" + JSONObject.quote(url) + ");const b=await r.blob();"
                    + "if(b.size>" + NavigationPolicy.MAX_BLOB_BYTES + ")throw Error('size');"
                    + "const n=Array.from(document.querySelectorAll('a[download]')).find(a=>a.href===" + JSONObject.quote(url) + ")?.download||'documento.pdf';"
                    + "const f=new FileReader();f.onload=()=>ResidualDownloads.postMessage(JSON.stringify({name:n,type:b.type,base64:f.result.split(',')[1]}));f.readAsDataURL(b);"
                    + "}catch(e){ResidualDownloads.postMessage(JSON.stringify({error:true}));}})()";
                web.evaluateJavascript(script, null);
            } else if (url.startsWith("https://")) {
                if (downloading || pendingDownload != null) { toast("Conclua o download atual primeiro."); return; }
                downloading = true;
                String name = URLUtil.guessFileName(url, disposition, mime);
                String cookie = NavigationPolicy.isInternal(url) ? CookieManager.getInstance().getCookie(url) : null;
                toast("Preparando download…");
                io.execute(() -> downloadHttps(url, cookie, userAgent, name, mime));
            } else { toast("Formato de download não suportado nesta versão."); }
        });
    }

    private void downloadHttps(String url, String cookie, String agent, String name, String mime) {
        File temp = null;
        HttpURLConnection connection = null;
        try {
            URL current = new URL(url);
            for (int redirects = 0; redirects < 6; redirects++) {
                connection = (HttpURLConnection) current.openConnection();
                connection.setConnectTimeout(20000); connection.setReadTimeout(30000);
                connection.setInstanceFollowRedirects(false);
                connection.setRequestProperty("User-Agent", agent);
                if (cookie != null && NavigationPolicy.isInternal(current.toString())) connection.setRequestProperty("Cookie", cookie);
                int code = connection.getResponseCode();
                if (code >= 300 && code < 400) {
                    String location = connection.getHeaderField("Location");
                    if (location == null) throw new Exception("Invalid redirect");
                    URL next = new URL(current, location);
                    if (!"https".equals(next.getProtocol())) throw new Exception("HTTPS required");
                    connection.disconnect(); current = next; continue;
                }
                if (code < 200 || code >= 300) throw new Exception("Download failed");
                if (connection.getContentLengthLong() > NavigationPolicy.MAX_DOWNLOAD_BYTES) throw new Exception("File too large");
                temp = File.createTempFile("residual-", ".download", getCacheDir());
                try (InputStream input = connection.getInputStream(); OutputStream output = new FileOutputStream(temp)) {
                    byte[] buffer = new byte[8192]; int count; long total = 0;
                    while ((count = input.read(buffer)) != -1) {
                        total += count;
                        if (total > NavigationPolicy.MAX_DOWNLOAD_BYTES) throw new Exception("File too large");
                        output.write(buffer, 0, count);
                    }
                }
                File ready = temp;
                runOnUiThread(() -> offerSave(ready, name, mime));
                return;
            }
            throw new Exception("Too many redirects");
        } catch (Exception error) {
            if (temp != null) temp.delete();
            runOnUiThread(() -> { downloading = false; toast("Não foi possível baixar. Confira sua conexão e o tamanho do arquivo (até 100 MB)."); });
        } finally { if (connection != null) connection.disconnect(); }
    }

    private void offerSave(File file, String name, String mime) {
        downloading = false;
        if (isFinishing() || isDestroyed()) { file.delete(); return; }
        pendingDownload = file;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mime == null || mime.isEmpty() ? "application/octet-stream" : mime);
        intent.putExtra(Intent.EXTRA_TITLE, name.replaceAll("[/\\\\\\r\\n]", "_"));
        try { startActivityForResult(intent, SAVE_FILE); }
        catch (ActivityNotFoundException error) { file.delete(); pendingDownload = null; toast("Nenhum seletor de arquivos disponível."); }
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == PICK_FILE && fileCallback != null) {
            Uri[] uris = null;
            if (result == RESULT_OK && data != null) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount(); uris = new Uri[count];
                    for (int i = 0; i < count; i++) uris[i] = data.getClipData().getItemAt(i).getUri();
                } else if (data.getData() != null) uris = new Uri[] {data.getData()};
            }
            fileCallback.onReceiveValue(uris); fileCallback = null;
        } else if (request == SAVE_FILE && pendingDownload != null) {
            File file = pendingDownload; pendingDownload = null;
            if (result != RESULT_OK || data == null || data.getData() == null) { file.delete(); return; }
            Uri destination = data.getData();
            io.execute(() -> {
                try (InputStream input = new java.io.FileInputStream(file); OutputStream output = getContentResolver().openOutputStream(destination)) {
                    if (output == null) throw new Exception("No output stream");
                    byte[] bytes = new byte[8192]; int count;
                    while ((count = input.read(bytes)) != -1) output.write(bytes, 0, count);
                    runOnUiThread(() -> toast("Arquivo salvo."));
                } catch (Exception error) { runOnUiThread(() -> toast("Não foi possível salvar o arquivo.")); }
                finally { file.delete(); }
            });
        }
    }

    private void reload() {
        errorPanel.setVisibility(View.GONE);
        if (NavigationPolicy.isInternal(web.getUrl())) web.reload(); else web.loadUrl(NavigationPolicy.HOME);
    }
    private void showNetworkError() { pageFailed = true; progress.setVisibility(View.GONE); errorPanel.setVisibility(View.VISIBLE); }
    private void goBack() { if (web.canGoBack()) web.goBack(); else finish(); }
    @Override protected void onSaveInstanceState(Bundle state) { web.saveState(state); super.onSaveInstanceState(state); }
    @Override protected void onPause() { CookieManager.getInstance().flush(); web.onPause(); super.onPause(); }
    @Override protected void onResume() { super.onResume(); if (web != null) web.onResume(); }
    @Override protected void onDestroy() {
        if (fileCallback != null) fileCallback.onReceiveValue(null);
        if (pendingDownload != null) pendingDownload.delete();
        web.destroy(); io.shutdown(); super.onDestroy();
    }
    private void openExternal(String url) {
        if (!NavigationPolicy.isExternalAllowed(url)) { toast("Este tipo de link não pode ser aberto."); return; }
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (ActivityNotFoundException error) { toast("Nenhum aplicativo disponível para abrir este link."); }
    }
    private Button button(String text, String description) {
        Button button = new Button(this); button.setText(text); button.setTextSize(26);
        button.setTextColor(Color.WHITE); button.setBackgroundColor(Color.TRANSPARENT);
        button.setContentDescription(description); button.setPadding(0, 0, 0, 0); return button;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void toast(String text) { Toast.makeText(this, text, Toast.LENGTH_LONG).show(); }
}
