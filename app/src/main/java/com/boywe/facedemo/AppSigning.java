package com.boywe.facedemo;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Log;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * 获取签名工具类，签名SHA1等
 *
 */
public class AppSigning {
    public final static String MD5 = "MD5";
    public final static String SHA1 = "SHA1";
    public final static String SHA256 = "SHA256";
    private static final HashMap<String, ArrayList<String>> mSignMap = new HashMap<>();

    /**
     * 返回一个签名的对应类型的字符串
     *
     * @param context
     * @param type
     * @return 因为一个安装包可以被多个签名文件签名，所以返回一个签名信息的list
     */
    public static ArrayList<String> getSignInfo(Context context, String type) {
        if (context == null || type == null) {
            return null;
        }
        String packageName = context.getPackageName();
        if (packageName == null) {
            return null;
        }
        if (mSignMap.get(type) != null) {
            return mSignMap.get(type);
        }
        ArrayList<String> mList = new ArrayList<>();
        try {
            Signature[] signs = getSignatures(context, packageName);
            for (Signature sig : signs) {
                String tmp = "error!";
                if (MD5.equals(type)) {
                    tmp = getSignatureByteString(sig, MD5);
                } else if (SHA1.equals(type)) {
                    tmp = getSignatureByteString(sig, SHA1);
                } else if (SHA256.equals(type)) {
                    tmp = getSignatureByteString(sig, SHA256);
                }
                mList.add(tmp);
            }
        } catch (Exception e) {
            Log.e("Sign",e.toString());
        }
        mSignMap.put(type, mList);
        return mList;
    }

    /**
     * 获取签名sha1值
     *
     * @param context
     * @return
     */
    public static String getSha1(Context context) {
        String res = "";
        ArrayList<String> mlist = getSignInfo(context, SHA1);
        if (mlist != null && !mlist.isEmpty()) {
            res = mlist.get(0);
        }
        return res;
    }

    /**
     * 获取签名MD5值
     *
     * @param context
     * @return
     */
    public static String getMD5(Context context) {
        String res = "";
        ArrayList<String> mlist = getSignInfo(context, MD5);
        if (mlist != null && !mlist.isEmpty()) {
            res = mlist.get(0);
        }
        return res;
    }

    /**
     * 获取签名SHA256值
     *
     * @param context
     * @return
     */
    public static String getSHA256(Context context) {
        String res = "";
        ArrayList<String> mlist = getSignInfo(context, SHA256);
        if (mlist != null && !mlist.isEmpty()) {
            res = mlist.get(0);
        }
        return res;
    }

    /**
     * 返回对应包的签名信息
     *
     * @param context
     * @param packageName
     * @return
     */
    private static Signature[] getSignatures(Context context, String packageName) {
        PackageInfo packageInfo = null;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            return packageInfo.signatures;
        } catch (Exception e) {
            Log.e("Sign",e.toString());
        }
        return null;
    }

    /**
     * 获取相应的类型的字符串（把签名的byte[]信息转换成16进制）
     *
     * @param sig
     * @param type
     * @return
     */
    private static String getSignatureString(Signature sig, String type) {
        byte[] hexBytes = sig.toByteArray();
        String fingerprint = "error!";
        try {
            MessageDigest digest = MessageDigest.getInstance(type);
            byte[] digestBytes = digest.digest(hexBytes);
            StringBuilder sb = new StringBuilder();
            for (byte digestByte : digestBytes) {
                sb.append((Integer.toHexString((digestByte & 0xFF) | 0x100)).substring(1, 3));
            }
            fingerprint = sb.toString();
        } catch (Exception e) {
            Log.e("Sign",e.toString());
        }

        return fingerprint;
    }

    /**
     * 获取相应的类型的字符串（把签名的byte[]信息转换成 95:F4:D4:FG 这样的字符串形式）
     *
     * @param sig
     * @param type
     * @return
     */
    private static String getSignatureByteString(Signature sig, String type) {
        byte[] hexBytes = sig.toByteArray();
        String fingerprint = "error!";
        try {
            MessageDigest digest = MessageDigest.getInstance(type);
            byte[] digestBytes = digest.digest(hexBytes);
            StringBuilder sb = new StringBuilder();
            for (byte digestByte : digestBytes) {
                sb.append(((Integer.toHexString((digestByte & 0xFF) | 0x100)).substring(1, 3)).toUpperCase());
                sb.append(":");
            }
            fingerprint = sb.substring(0, sb.length() - 1);
        } catch (Exception e) {
            Log.e("Sign",e.toString());
        }

        return fingerprint;
    }
}
