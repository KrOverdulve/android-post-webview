package org.solidsoftware.postwebview;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.squareup.mimecraft.FormEncoding;
import com.squareup.okhttp.OkHttpClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

public class InterceptingWebViewClient extends WebViewClient {
    public static final String TAG = "InterceptingWebViewClient";

    private Context mContext = null;
    private WebView mWebView = null;
    private PostInterceptJavascriptInterface mJSSubmitIntercept = null;
    private OkHttpClient client = new OkHttpClient();


    public InterceptingWebViewClient(Context context, WebView webView) {
        mContext = context;
        mWebView = webView;
        mJSSubmitIntercept = new PostInterceptJavascriptInterface(this);
        mWebView.addJavascriptInterface(mJSSubmitIntercept, "interception");

    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        mNextAjaxRequestContents = null;
        mNextFormRequestContents = null;

        view.loadUrl(url);
        return true;
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(final WebView view, final String url) {
        try {
            // Our implementation just parses the response and visualizes it. It does not properly handle
            // redirects or HTTP errors at the moment. It only serves as a demo for intercepting POST requests
            // as a starting point for supporting multiple types of HTTP requests in a full fletched browser

            // Construct request
            HttpURLConnection conn = client.open(new URL(url));
            conn.setRequestMethod(isPOST() ? "POST" : "GET");

            // Write body
            if (isPOST()) {
                OutputStream os = conn.getOutputStream();
                if (mNextAjaxRequestContents != null) {
                    writeBody(os);
                } else {
                    writeForm(os);
                }
                os.close();
            }

            // Read input
            String charset = conn.getContentEncoding() != null ? conn.getContentEncoding() : Charset.defaultCharset().displayName();
            String mime = conn.getContentType();
            byte[] pageContents = IOUtils.readFully(conn.getInputStream());

            // Perform JS injection
            if (mime.equals("text/html")) {
                pageContents = PostInterceptJavascriptInterface
                        .enableIntercept(mContext, pageContents)
                        .getBytes(charset);
            }

            // Convert the contents and return
            InputStream isContents = new ByteArrayInputStream(pageContents);

            return new WebResourceResponse(mime, charset,
                    isContents);
        } catch (FileNotFoundException e) {
            Log.w(TAG, "Error 404: " + e.getMessage());
            e.printStackTrace();

            return null;        // Let Android try handling things itself
        } catch (Exception e) {
            e.printStackTrace();

            return null;        // Let Android try handling things itself
        }
    }

    private boolean isPOST() {
        return (mNextFormRequestContents != null || mNextAjaxRequestContents != null);
    }

    private void writeBody(OutputStream out) {
        try {
            out.write(mNextAjaxRequestContents.body.getBytes("UTF-8"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void writeForm(OutputStream out) {
        try {
            JSONArray jsonPars = new JSONArray(mNextFormRequestContents.json);

            // We assume to be dealing with a very simple form here, so no file uploads or anything
            // are possible for reasons of clarity
            FormEncoding.Builder m = new FormEncoding.Builder();
            for (int i = 0; i < jsonPars.length(); i++) {
                JSONObject jsonPar = jsonPars.getJSONObject(i);

                m.add(jsonPar.getString("name"), jsonPar.getString("value"));
                // jsonPar.getString("type");
                // TODO TYPE?
            }
            m.build().writeBodyTo(out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getType(Uri uri) {
        String contentResolverUri = mContext.getContentResolver().getType(uri);
        if (contentResolverUri == null) {
            contentResolverUri = "*/*";
        }
        return contentResolverUri;
    }

    private PostInterceptJavascriptInterface.FormRequestContents mNextFormRequestContents = null;

    public void nextMessageIsFormRequest(PostInterceptJavascriptInterface.FormRequestContents formRequestContents) {
        mNextFormRequestContents = formRequestContents;
    }

    private PostInterceptJavascriptInterface.AjaxRequestContents mNextAjaxRequestContents = null;

    public void nextMessageIsAjaxRequest(PostInterceptJavascriptInterface.AjaxRequestContents ajaxRequestContents) {
        mNextAjaxRequestContents = ajaxRequestContents;
    }
}
