/**
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 José Francisco Irles Durá <jfirles@siptize.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.siptize.ovhapi;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Basic helper for OVH Rest API
 */
public class OvhApi {

    public static final String OVH_API_EU_BASE_URL = "https://eu.api.ovh.com/1.0";
    public static final String OVH_API_CA_BASE_URL = "https://ca.api.ovh.com/1.0";
    private static final String OVH_API_CLIENT_VERSION = "1.0-SNAPSHOT";
    private static final String OVH_API_CLIENT_USER_AGENT = "OVH Api Client/"
            + OVH_API_CLIENT_VERSION;
    private static final String HTTP_HEADER_OVH_APPLICATION = "X-Ovh-Application";
    private static final String HTTP_HEADER_OVH_TIMESTAMP = "X-Ovh-Timestamp";
    private static final String HTTP_HEADER_OVH_SIGNATURE = "X-Ovh-Signature";
    private static final String HTTP_HEADER_OVH_CONSUMER = "X-Ovh-Consumer";
    private static final String HTTP_HEADER_CONTENT_TYPE = "Content-type";
    private static final String HTTP_HEADER_ACCEPT = "Accept";
    private static final String CONTENT_TYPE_JSON = "application/json; charset=utf8";
    private static final String TIME_PATH = "/auth/time";
    private static final String HTTP_HEADER_USER_AGENT = "User-Agent";

    /**
     * HTTP methods for Api
     */
    public enum Method {

        GET, POST, PUT, DELETE
    }

    private final String rootUrl;
    private final String applicationKey;
    private final String applicationSecret;
    private final String consumerKey;
    private final int timeDrift;

    /**
     * Constructor for OvhApi
     *
     * @param rootUrl Base URL for Api query
     * @param applicationKey Application key
     * @param applicationSecret Application secret
     * @param consumerKey Consumer key
     */
    public OvhApi(String rootUrl, String applicationKey,
            String applicationSecret, String consumerKey) {
        this.rootUrl = rootUrl;
        this.applicationKey = applicationKey;
        this.applicationSecret = applicationSecret;
        this.consumerKey = consumerKey;
        String timeUrl = rootUrl + TIME_PATH;
        int tmpTime;
        try {
            HttpURLConnection conn = (HttpURLConnection)new URL(timeUrl).openConnection();
            tmpTime = getServerTimestamp() - Integer.parseInt(readInputStreamAsString(conn.getInputStream()));
            conn.disconnect();
        } catch (IOException ex) {
            tmpTime = 0;
        }
        timeDrift = tmpTime;
    }

    /**
     * Build the request, sign it and execute with the specified params
     *
     * @param method HTTP method to use
     * @param url api path
     * @param body body (in json format) to send, null if it can't send body
     * @return query result in json format
     * @throws IOException if some error happens
     */
    public String call(Method method, String url, String body) throws IOException {
        // si no hay body, cadena vacía
        if (body == null) {
            body = "";
        }
        // componemos la url
        String fullUrl = rootUrl + url;
        // calculamos el timestamp
        int timestamp = getServerTimestamp() - timeDrift;
        // calculamos la firma
        StringBuilder buffer = new StringBuilder(applicationSecret).append("+");
        buffer.append(consumerKey).append("+");
        buffer.append(method.name()).append("+");
        buffer.append(fullUrl).append("+");
        buffer.append(body).append("+");
        buffer.append(timestamp);
        String toSign = buffer.toString();
        String signature = null;
        try {
            signature = "$1$" + sha1(toSign);
        } catch (UnsupportedEncodingException ex) {
            throw new IOException("Error generating sha1 sign", ex);
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException("Error generating sha1 sign", ex);
        }
        // hacemos la conexión
        HttpURLConnection conn = (HttpURLConnection) new URL(fullUrl).openConnection();
        conn.setRequestMethod(method.name());
        conn.setRequestProperty(HTTP_HEADER_OVH_APPLICATION, applicationKey);
        conn.setRequestProperty(HTTP_HEADER_OVH_TIMESTAMP, String.valueOf(timestamp));
        conn.setRequestProperty(HTTP_HEADER_OVH_CONSUMER, consumerKey);
        conn.setRequestProperty(HTTP_HEADER_CONTENT_TYPE, CONTENT_TYPE_JSON);
        conn.setRequestProperty(HTTP_HEADER_ACCEPT, CONTENT_TYPE_JSON);
        conn.setRequestProperty(HTTP_HEADER_OVH_SIGNATURE, signature);
        conn.setRequestProperty(HTTP_HEADER_USER_AGENT, OVH_API_CLIENT_USER_AGENT);
        // enviamos el body si hay
        if (!body.isEmpty()) {
            conn.setDoOutput(true);
            OutputStream os = conn.getOutputStream();
            os.write(body.getBytes("UTF-8"));
        }
        // devolvemos la respuesta
        String response = readInputStreamAsString(conn.getInputStream());
        conn.disconnect();
        return response;
    }

    /**
     * Builds a string from InputStream
     *
     * @param in inputStream to read
     * @return the string builded
     * @throws IOException if some error happens
     */
    private String readInputStreamAsString(InputStream in) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(in);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        int result = bis.read();
        while (result != -1) {
            byte b = (byte) result;
            buf.write(b);
            result = bis.read();
        }
        return buf.toString();
    }

    /**
     * Digest a string with sha1
     *
     * @param str text to digest
     * @return digest of the text
     * @throws NoSuchAlgorithmException if no such sha-1 algorithm
     * @throws UnsupportedEncodingException if not support UTF-8 encoding
     */
    private String sha1(String str) throws NoSuchAlgorithmException,
            UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(str.getBytes("UTF-8"));
        byte[] result = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : result) {
            sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    /**
     * Obtain the server timestamp (seconds) in TimeZone GMT
     *
     * @return
     */
    private int getServerTimestamp() {
        return (int) (Calendar.getInstance(TimeZone.getTimeZone("GMT")).getTimeInMillis() / 1000);
    }

    /**
     * Executes GET to the api
     *
     * @param url
     * @return
     * @throws IOException
     */
    public String get(String url) throws IOException {
        return call(Method.GET, url, null);
    }

    /**
     * Executes PUT to the api
     *
     * @param url
     * @param body
     * @return
     * @throws IOException
     */
    public String put(String url, String body) throws IOException {
        return call(Method.PUT, url, body);
    }

    /**
     * Executes POST to the api
     *
     * @param url
     * @param body
     * @return
     * @throws IOException
     */
    public String post(String url, String body) throws IOException {
        return call(Method.POST, url, body);
    }

    /**
     * Executos DELETE to the api
     *
     * @param url
     * @return
     * @throws IOException
     */
    public String delete(String url) throws IOException {
        return call(Method.DELETE, url, null);
    }

    /**
     * Getter for rootUrl
     *
     * @return root url
     */
    public String getRootUrl() {
        return rootUrl;
    }

    /**
     * Getter for applicationKey
     *
     * @return application key
     */
    public String getApplicationKey() {
        return applicationKey;
    }

    /**
     * Getter for applicationSecret
     *
     * @return application secret
     */
    public String getApplicationSecret() {
        return applicationSecret;
    }

    /**
     * Getter for timeDrift
     *
     * @return time drift
     */
    public int getTimeDrift() {
        return timeDrift;
    }

    /**
     * Getter for consumerKey
     *
     * @return consumerKey
     */
    public String getConsumerKey() {
        return consumerKey;
    }

}
