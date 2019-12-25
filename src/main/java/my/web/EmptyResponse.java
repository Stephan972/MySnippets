package my.web;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import my.web.utils.WebUtils;

public final class EmptyResponse implements Response {
    private static final byte[] EMPTY_BYTE_ARRAY = new byte[] {};
    private static final BufferedInputStream EMPTY_BUFFEREDINPUTSTREAM = new BufferedInputStream(new ByteArrayInputStream(EMPTY_BYTE_ARRAY));

    private Method method;
    private URL url;
    private int statusCode;
    private String statusMessage;

    public EmptyResponse(Method method, String url, String statusMessage) {
        this(method, url, WebClient.FAILED_REQUEST, statusMessage);
    }

    public EmptyResponse(Method method, String url0, int statusCode, String statusMessage) {
        try {           
            this.method = method;
            this.url = URI.create(WebUtils.format(url0)).toURL();
            this.statusCode = statusCode;
            this.statusMessage = statusMessage;
        } catch (IllegalArgumentException | MalformedURLException e) {
            throw new IllegalArgumentException( //
                    "\nMethod: " + method + //
                            "\nURL: " + url0 + //
                            "\nStatus code: " + statusCode + //
                            "\nStatus message: " + statusMessage,
                    e);
        }
    }

    @Override
    public URL url() {
        return url;
    }

    @Override
    public Response url(URL url) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Method method() {
        return method;
    }

    @Override
    public Response method(Method method) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String header(String name) {
        return "";
    }

    @Override
    public Response header(String name, String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasHeader(String name) {
        return false;
    }

    @Override
    public boolean hasHeaderWithValue(String name, String value) {
        return false;
    }

    @Override
    public Response removeHeader(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, String> headers() {
        return Collections.emptyMap();
    }

    @Override
    public String cookie(String name) {
        return "";
    }

    @Override
    public Response cookie(String name, String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasCookie(String name) {
        return false;
    }

    @Override
    public Response removeCookie(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, String> cookies() {
        return Collections.emptyMap();
    }

    @Override
    public int statusCode() {
        return statusCode;
    }

    @Override
    public String statusMessage() {
        return statusMessage;
    }

    @Override
    public String charset() {
        return "UTF-8";
    }

    @Override
    public String contentType() {
        return "text/html";
    }

    @Override
    public Document parse() throws IOException {
        return Jsoup.parse("");
    }

    @Override
    public String body() {
        return "";
    }

    @Override
    public byte[] bodyAsBytes() {
        return EMPTY_BYTE_ARRAY;
    }

    @Override
    public Response charset(String charset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Response addHeader(String arg0, String arg1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> headers(String name) {
        return Collections.emptyList();
    }

    @Override
    public Map<String, List<String>> multiHeaders() {
        return Collections.emptyMap();
    }

    @Override
    public BufferedInputStream bodyStream() {
        return EMPTY_BUFFEREDINPUTSTREAM;
    }

    @Override
    public Response bufferUp() {
        return this;
    }
}