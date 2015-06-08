package com.eharoldreyes.httpwow;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.List;

/**
 * Created by eharoldreyes on 5/18/15.
 * Email: eharoldreyes@gmail.com
 * Linkedin: https://ph.linkedin.com/pub/harold-reyes/97/4a3/614
 */
public class HttpWow {
    private static final String tag = "HttpWow";
    public static final String GET = "GET";
    public static final String POST = "POST";
    public static final String PUT = "PUT";
    public static final String DELETE = "DELETE";
    public static final String HEAD = "HEAD";
    public static final String OPTIONS = "OPTIONS";
    private String method;
    private String url;
    private List<NameValuePair> headers;
    private List<NameValuePair> queries;
    private List<NameFilePair> files;
    private String parameters;
    private OnProgressChangeListener progressListener;
    private Callback callback;
    private AsyncTask<Void, Integer, Response> task;
    private boolean debug = true;
    private int timeOut = 10000;

    public interface Callback {
        void onResult(int status, String result, Exception exception);
    }

    public interface OnProgressChangeListener {
        void onProgressChanged(int progress);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void run() {
        task = new AsyncTask<Void, Integer, Response>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                if(getUrl() != null && getQueries() != null){
                    for (int i = 0; i < queries.size(); i++) {
                        try {
                            if (i == 0) {
                                url += ("?" + queries.get(i).getName() + "=" + URLEncoder.encode(queries.get(i).getValue(), "UTF-8"));
                            } else {
                                url += ("&" + queries.get(i).getName() + "=" + URLEncoder.encode(queries.get(i).getValue(), "UTF-8"));
                            }
                        } catch (UnsupportedEncodingException e) {
                            break;
                        }
                    }
                }
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);
                if(getProgressListener() != null) getProgressListener().onProgressChanged(values[0]);
            }

            @Override
            protected Response doInBackground(Void... params) {
                return HttpUrlConnection(new OnProgressChangeListener() {
                    @Override
                    public void onProgressChanged(int progress) {
                        onProgressUpdate(progress);
                    }
                });
            }

            @Override
            protected void onPostExecute(Response response) {
                super.onPostExecute(response);
                callback.onResult(response.getStatus(), response.getResult(), response.getException());
            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
        else
            task.execute();
    }

    public void cancel() {
        if(task != null) task.cancel(true);
    }

    public void stop() {
        cancel();
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<NameValuePair> getHeaders() {
        return headers;
    }

    public void setHeaders(List<NameValuePair> headers) {
        this.headers = headers;
    }

    public void setFiles(List<NameFilePair> files) {
        this.files = files;
    }

    public List<NameFilePair> getFiles() {
        return files;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public Callback getCallback() {
        return callback;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public int getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(int timeOut) {
        this.timeOut = timeOut;
    }

    public OnProgressChangeListener getProgressListener() {
        return progressListener;
    }

    public void setProgressListener(OnProgressChangeListener progressListener) {
        this.progressListener = progressListener;
    }

    public List<NameValuePair> getQueries() {
        return queries;
    }

    public void setQueries(List<NameValuePair> queries) {
        this.queries = queries;
    }

    private Response HttpUrlConnection(OnProgressChangeListener onProgressChangeListener)  {
        Response webserviceResponse = new Response();
        HttpURLConnection connection = null;
        try {
            if(isDebug()) Log.d(tag, "Url: " + getUrl());
            if(isDebug()) Log.d(tag, "Method: " + getMethod());

            DataOutputStream request;
            URL newURL = new URL(getUrl());
            connection = (HttpURLConnection) newURL.openConnection();
            connection.setRequestMethod(getMethod());
            connection.setConnectTimeout(getTimeOut());
            connection.setRequestProperty("User-Agent", "HttpWow");
            if(getHeaders() != null) {
                for (NameValuePair header : getHeaders()) {
                    if(isDebug()) Log.d(tag, "Header: " + header.getName() + "=" + header.getValue());
                    connection.setRequestProperty(header.getName(), header.getValue());
                }
            }

            if (!getMethod().equals(GET) && !getMethod().equals(DELETE)) {
                if(isDebug()) Log.d(tag, "Parameter: " + getParameters());
                connection.setDoInput(true);
                connection.setDoOutput(true);
                request = new DataOutputStream(connection.getOutputStream());
                if (getParameters() != null)
                    request.writeBytes(getParameters());
                request.flush();
                request.close();
            }

            if((getMethod().equals(POST) || getMethod().equals(PUT)) && getFiles() != null){
                appendFiles(connection, getFiles());
            }

            int status = connection.getResponseCode();
            InputStream is;
            if (status == HttpURLConnection.HTTP_OK)
                is = connection.getInputStream();
            else
                is = connection.getErrorStream();
            String response = readStream(is, onProgressChangeListener);
            webserviceResponse.setResult(response);
            webserviceResponse.setStatus(status);

            if(isDebug()) Log.i(tag, "Status: " + status);
            if(isDebug()) Log.d(tag, "Result: " + connection.getResponseMessage());
        } catch (Exception e) {
            webserviceResponse.setException(e);
        } finally {
            if (connection != null) connection.disconnect();
        }
        return webserviceResponse;
    }

    private String readStream(InputStream in, OnProgressChangeListener onProgressChangeListener) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder sb = new StringBuilder();
        int readBytes = 0;
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
            readBytes += line.getBytes("ISO-8859-2").length + 2; // CRLF bytes!!
            if(isDebug()) Log.d(tag, "Progress: " + readBytes + "%");
            if (onProgressChangeListener != null)
                onProgressChangeListener.onProgressChanged(readBytes);
        }
        reader.close();
        return sb.toString();
    }

    //FROM http://www.codejava.net/java-se/networking/upload-files-by-sending-multipart-request-programmatically
    private void appendFiles(HttpURLConnection connection, List<NameFilePair> files) throws IOException {
        String boundary = "===" + System.currentTimeMillis() + "===";
        connection.setUseCaches(false);
        connection.setDoOutput(true); // indicates POST method
        connection.setDoInput(true);
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        OutputStream outputStream = connection.getOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true);
        String LINE_FEED = "\r\n";
        for(NameFilePair pair: files){
            String fileName = pair.getFile().getName();
            writer.append("--").append(boundary).append(LINE_FEED);
            writer.append("Content-Disposition: form-data; name=\"").append(pair.getName()).append("\"; filename=\"").append(fileName).append("\"").append(LINE_FEED);
            writer.append("Content-Type: ").append(URLConnection.guessContentTypeFromName(fileName)).append(LINE_FEED);
            writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
            writer.append(LINE_FEED);
            writer.flush();
            FileInputStream inputStream = new FileInputStream(pair.getFile());
            byte[] buffer = new byte[4096];
            int bytesRead = -1;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
            inputStream.close();
            writer.append(LINE_FEED);
            writer.flush();
        }
        writer.append(LINE_FEED).flush();
        writer.append("--").append(boundary).append("--").append(LINE_FEED);
        writer.close();
    }

    public static class Response {
        private Exception exception;
        private int status = 0;
        private String result;

        public Response() {
        }

        public Exception getException() {
            return this.exception;
        }

        public void setException(Exception exception) {
            this.exception = exception;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }
    }

    public static class NameFilePair implements Serializable {
        private String name;
        private File file;

        public NameFilePair(String name, File file) {
            this.name = name;
            this.file = file;
        }

        public String getName() {
            return name;
        }

        public File getFile() {
            return file;
        }
    }

    public static class NameValuePair implements Serializable {
        private String name;
        private String value;

        public NameValuePair(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }
    }
}







































//BACKUP
//    private Response HttpUrlConnection(String url, String requestMethod, List<NameValuePair> headers, String parameters, OnProgressChangeListener onProgressChangeListener)  {
//        Response webserviceResponse = new Response();
//        HttpURLConnection connection = null;
//        try {
//            DataOutputStream request;
//            URL newURL = new URL(url);
//            if(isDebug()) Log.d(tag, "Url: " + url);
//            connection = (HttpURLConnection) newURL.openConnection();
//
//            if(isDebug()) Log.d(tag, "Method: " + requestMethod);
//            connection.setRequestMethod(requestMethod);
//            connection.setConnectTimeout(getTimeOut());
//
//            if(headers != null) {
//                for (NameValuePair header : headers) {
//                    if(isDebug()) Log.d(tag, "Header: " + header.getName() + "=" + header.getValue());
//                    connection.setRequestProperty(header.getName(), header.getValue());
//                }
//            }
//
//            if (!requestMethod.equals("GET") && !requestMethod.equals("DELETE")) {
//                if(isDebug()) Log.d(tag, "Parameter: " + parameters);
//                connection.setDoInput(true);
//                connection.setDoOutput(true);
//                request = new DataOutputStream(connection.getOutputStream());
//                if (parameters != null)
//                    request.writeBytes(parameters);
//                request.flush();
//                request.close();
//            }
//
//            int status = connection.getResponseCode();
//            String line;
//            InputStream is;
//
//            if (status == 200)
//                is = connection.getInputStream();
//            else
//                is = connection.getErrorStream();
//
//            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
//            StringBuilder sb = new StringBuilder();
//            int readBytes = 0;
//            while ((line = reader.readLine()) != null) {
//                sb.append(line).append("\n");
//                readBytes += line.getBytes("ISO-8859-2").length + 2; // CRLF bytes!!
//                if(isDebug()) Log.d(tag, "Progress: " + readBytes + "%");
//                if (onProgressChangeListener != null)
//                    onProgressChangeListener.onProgressChanged(readBytes);
//            }
//            reader.close();
//            String response = sb.toString();
//            if(isDebug()) Log.i(tag, "Status: " + status);
//            if(isDebug()) Log.d(tag, "Result: " + connection.getResponseMessage());
//            webserviceResponse.setResult(response);
//            webserviceResponse.setStatus(status);
//
//
//
//        } catch (Exception e) {
//            webserviceResponse.setException(e);
//        } finally {
//            if (connection != null) connection.disconnect();
//        }
//        return webserviceResponse;
//    }


