package com.example.mainapp;// package name;

import java.net.URL;
import java.net.HttpURLConnection;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Demo App 检测升级
 * https://github.com/AnyLifeZLB/FaceVerificationSDK
 */
public final class UpdateChecker {
    public static final String APP_CHECK = "https://api.pgyer.com/apiv2/app/check";
    public String _api_key = "";

    /**
     * @param _api_key 蒲公英 api key
     */
    public UpdateChecker(String _api_key) {
        this._api_key = _api_key;
    }

    /**
     * 检测App是否有更新
     * @see <a href="https://www.pgyer.com/doc/view/api#appUpdate">https://www.pgyer.com/doc/view/api#appUpdate</a>
     *
     * @param appKey appKey
     * @param buildVersion (选填)使用 App 本身的 Build 版本号，Android 对应字段为 versionname, iOS 对应字段为 version
     * @param buildBuildVersion (选填)使用蒲公英生成的自增 Build 版本号
     * @param channelKey (选填)渠道KEY
     * @param callback 结果回调
     */
    public void check(String appKey, String buildVersion, Integer buildBuildVersion, String channelKey, Callback callback) {
        Map<String, String> data = new HashMap<>();

        data.put("_api_key", _api_key);
        data.put("appKey", appKey);
        data.put("buildVersion", buildVersion == null ? "" : buildVersion);
        data.put("buildBuildVersion", buildBuildVersion == null ? "" : (buildBuildVersion + ""));
        data.put("channelKey", channelKey == null ? "" : channelKey);

        Http.post(APP_CHECK, data, new Http.Callback() {
            @Override
            public Boolean response(String response) {
                Map<String, String> dataMap = parseResponse(response);
                if (dataMap == null) {
                    callback.error("response no data");
                    return false;
                }

                if (dataMap.containsKey("message")) {
                    callback.error(dataMap.get("message"));
                    return false;
                }

                UpdateInfo updateInfo = new UpdateInfo();
                updateInfo.buildBuildVersion = Integer.parseInt(dataMap.get("buildBuildVersion"));
                updateInfo.forceUpdateVersion = dataMap.get("forceUpdateVersion");
                updateInfo.forceUpdateVersionNo = dataMap.get("forceUpdateVersionNo");
                updateInfo.needForceUpdate = dataMap.get("needForceUpdate").equals("true");
                updateInfo.downloadURL = dataMap.get("downloadURL");
                updateInfo.buildHaveNewVersion = dataMap.get("buildHaveNewVersion").equals("true");
                updateInfo.buildVersionNo = dataMap.get("buildVersionNo");
                updateInfo.buildVersion = dataMap.get("buildVersion");
                updateInfo.buildShortcutUrl = dataMap.get("buildShortcutUrl") == null ? "" : dataMap.get("buildShortcutUrl");
                updateInfo.buildUpdateDescription = dataMap.get("buildUpdateDescription");
                callback.result(updateInfo);
                return true;
            }

            @Override
            public void error(String message) {
                callback.error(message);
            }
        });
    }

    private Map<String, String> parseResponse(String response) {
        Map<String, String> responseMap = new HashMap<>();
        String responseRegexp = "^\\{\"code\":(.*),\"message\":\"(.*?)\".*\\}$";
        Pattern responsePattern = Pattern.compile(responseRegexp);
        Matcher responseMatcher = responsePattern.matcher(response);

        if (responseMatcher.find()) {
            responseMap.put("code", responseMatcher.group(1));
            responseMap.put("message", responseMatcher.group(2));
        } else {
            return null;
        }

        String data = "";
        String responseDataRegexp = "^\\{\"code\":.*,\"message\":\".*\",\"data\":(.*)\\}$";
        Pattern responseDataPattern = Pattern.compile(responseDataRegexp);
        Matcher responseDataMatcher = responseDataPattern.matcher(response);

        if (responseDataMatcher.find()) {
            data = responseDataMatcher.group(1);
        } else {
            return responseMap;
        }

        Map<String, String> dataMap = new HashMap<>();
        String dataRegexp = "\"(.*?)\":(\".*?\"|true|false)";
        Pattern dataPattern = Pattern.compile(dataRegexp);
        Matcher dataMatcher = dataPattern.matcher(data);

        while (dataMatcher.find()) {
            String key = dataMatcher.group(1);
            String value = dataMatcher.group(2);
            if (value.equals("true") || value.equals("false")) {
                dataMap.put(key, value);
            } else {
                dataMap.put(key, value.substring(1, value.length() - 1));
            }
        }

        return dataMap;
    }

    public class UpdateInfo {
        /** 蒲公英生成的用于区分历史版本的build号 */
        public Integer buildBuildVersion = 0;
        /** 强制更新版本号（未设置强置更新默认为空） */
        public String forceUpdateVersion = "";
        /** 强制更新的版本编号 */
        public String forceUpdateVersionNo = "";
        /** 是否强制更新 */
        public Boolean needForceUpdate = false;
        /** 应用安装地址 */
        public String downloadURL = "";
        /** 是否有新版本 */
        public Boolean buildHaveNewVersion = false;
        /** 上传包的版本编号，默认为1 (即编译的版本号，一般来说，编译一次会变动一次这个版本号, 在 Android 上叫 Version Code。对于 iOS 来说，是字符串类型；对于 Android 来说是一个整数。例如：1001，28等。) */
        public String buildVersionNo = "";
        /** 版本号, 默认为1.0 (是应用向用户宣传时候用到的标识，例如：1.1、8.2.1等。) */
        public String buildVersion = "";
        /** 应用短链接 */
        public String buildShortcutUrl = "";
        /** 应用更新说明 */
        public String buildUpdateDescription = "";
    }

    public interface Callback {
        public void result(UpdateInfo updateInfo);
        public void error(String message);
    }

    private static class Http {
        private static final Integer TIMEOUT = 3000;
        private static final String BOUNDARY = UUID.randomUUID().toString();

        public static void get(String url, Map<String, String> query, Callback callback) {
            request(url, "GET", query, null, callback);
        }

        public static void post(String url, Map<String, String> data, Callback callback) {
            request(url, "POST", null, data, callback);
        }

        public static void request(String url, String method, Map<String, String> query, Map<String, String> data, Callback callback) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String queryString = "";
                        if (method.equals("GET") && query != null) {
                            queryString = makeQueryString(query);
                        }

                        HttpURLConnection connection = (HttpURLConnection) new URL(url + "?" + queryString).openConnection();
                        connection.setRequestMethod(method);
                        connection.setUseCaches(false);
                        connection.setConnectTimeout(TIMEOUT);
                        connection.setReadTimeout(TIMEOUT);

                        if (method.equals("POST") && data != null) {
                            connection.setDoInput(true);
                            connection.setDoOutput(true);
                            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + BOUNDARY);
                            connection.setRequestProperty("Accept", "application/json");

                            OutputStream outputStream = connection.getOutputStream();
                            outputStream.write(makeFormData(data).getBytes());
                            outputStream.close();
                        }

                        connection.connect();
                        int responseCode = connection.getResponseCode();
                        if (responseCode == 200) {
                            String response = readResponse(connection);
                            response = unicodeDecode(response);
                            callback.response(response);
                        } else {
                            callback.error("Status Code " + responseCode);
                        }
                        connection.disconnect();
                    } catch (Exception e) {
                        callback.error(e.getMessage());
                    }
                }
            }).start();
        }

        private static String makeQueryString(Map<String, String> query) {
            List<String> list = new ArrayList(0);
            for (String key: query.keySet()) {
                list.add(key + "=" + query.get(key));
            }
            return String.join("&", list);
        }

        private static String makeFormData(Map<String, String> data) {
            StringBuilder formData = new StringBuilder();;
            for (String key: data.keySet()) {
                formData.append(String.format("--%s\r\n", BOUNDARY));
                formData.append(String.format("Content-Disposition: form-data; name=\"%s\"\r\n", key));
                formData.append("\r\n");
                formData.append(String.format("%s\r\n", data.get(key)));
            }
            formData.append(String.format("--%s--\r\n", BOUNDARY));
            return formData.toString();
        }

        private static String readResponse(HttpURLConnection connection) {
            try {
                InputStream inputStream = connection.getInputStream();
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[256];
                int readLength = 0;

                while ((readLength = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, readLength);
                }

                String response = new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
                inputStream.close();
                outputStream.close();
                return response;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "";
        }

        private static String unicodeDecode(String string) {
            Pattern pattern = Pattern.compile("(\\\\u(\\p{XDigit}{4}))");
            Matcher matcher = pattern.matcher(string);
            char ch;
            while (matcher.find()) {
                ch = (char) Integer.parseInt(matcher.group(2), 16);
                string = string.replace(matcher.group(1), ch + "");
            }
            return string;
        }

        public interface Callback {
            public Boolean response(String response);
            public void error(String message);
        }
    }
}