//package ruixun.com.patrol.ui.patrol.live
//
//import android.annotation.SuppressLint
//import android.content.Context
//import android.util.Log
//import androidx.lifecycle.MutableLiveData
//import androidx.lifecycle.ViewModel
//import es.dmoral.toasty.Toasty
//import ruixun.com.patrol.net.NetUrl
//
//private const val RTSP_REQUEST_KEY = "rtsp_request"
//private const val RTSP_USERNAME_KEY = "rtsp_username"
//private const val RTSP_PASSWORD_KEY = "rtsp_password"
//
//
//private const val DEFAULT_RTSP_USERNAME = ""
//private const val DEFAULT_RTSP_PASSWORD = ""
//
//private const val LIVE_PARAMS_FILENAME = "live_params"
//private var DEFAULT_RTSP_REQUEST = "rtsp://" + NetUrl.getHost() + ":8554/ds-onewo" //环境配置正式上线后再改吧
//
//@SuppressLint("LogNotTimber")
//class LiveViewModel : ViewModel() {
//
//    companion object {
//        private val TAG: String = LiveViewModel::class.java.simpleName
//        private const val DEBUG = false
//    }
//
//    val rtspRequest = MutableLiveData<String>().apply {
//        value = DEFAULT_RTSP_REQUEST
//    }
//
//    val rtspUsername = MutableLiveData<String>().apply {
//        value = DEFAULT_RTSP_USERNAME
//    }
//    val rtspPassword = MutableLiveData<String>().apply {
//        value = DEFAULT_RTSP_PASSWORD
//    }
//
//    fun loadParams(context: Context) {
//        if (DEBUG)
//            Log.v(TAG, "loadParams()")
//        val pref = context.getSharedPreferences(LIVE_PARAMS_FILENAME, Context.MODE_PRIVATE)
//        try {
//            rtspRequest.setValue(pref.getString(RTSP_REQUEST_KEY, DEFAULT_RTSP_REQUEST))
//        } catch (e: ClassCastException) {
//            e.printStackTrace()
//        }
//        try {
//            rtspUsername.setValue(pref.getString(RTSP_USERNAME_KEY, DEFAULT_RTSP_USERNAME))
//        } catch (e: ClassCastException) {
//            e.printStackTrace()
//        }
//        try {
//            rtspPassword.setValue(pref.getString(RTSP_PASSWORD_KEY, DEFAULT_RTSP_PASSWORD))
//        } catch (e: ClassCastException) {
//            e.printStackTrace()
//        }
//    }
//
//    fun saveParams(context: Context) {
//        if (DEBUG) Log.v(TAG, "saveParams()")
//        val editor = context.getSharedPreferences(LIVE_PARAMS_FILENAME, Context.MODE_PRIVATE).edit()
//        editor.putString(RTSP_REQUEST_KEY, rtspRequest.value)
//        editor.putString(RTSP_USERNAME_KEY, rtspUsername.value)
//        editor.putString(RTSP_PASSWORD_KEY, rtspPassword.value)
//        editor.apply()
//    }
//
//}