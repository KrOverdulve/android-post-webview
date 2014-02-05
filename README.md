POST-webview
====================

When for some reason you need to display a webview to the user on which you need to intercept the HTTP calls and perform them yourself (for example to add additional security), you can do so on Android by registering a `WebViewClient` and implementing 

```
android.webkit.WebViewClient#shouldInterceptRequest(android.webkit.WebView webview, java.lang.String url)
```

Unlike on iOS, where similarly implementing your own HTTP requests has the following method definition

```
- (BOOL)webView:(UIWebView *)webView shouldStartLoadWithRequest:(NSURLRequest *)request navigationType:(UIWebViewNavigationType)navigationType
```

from which you can determine the HTTP method by querying the `NSURLRequest` object, Android only passes the `url` making it infeasible to determine whether the intercepted webview request was actually a GET request, a POST request, or any other types of HTTP methods.

This project proposes a very simple sample application in which we demonstrate how we can deduce the HTTP method  to properly intercept web view requests to our own HTTP client.
