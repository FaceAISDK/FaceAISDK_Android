//package ruixun.com.patrol.ui.patrol.live
//
//import android.net.Uri
//import android.os.Bundle
//import android.os.Handler
//import android.os.Looper
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Toast
//import androidx.fragment.app.Fragment
//import androidx.lifecycle.ViewModelProvider
//import com.alexvas.rtsp.codec.VideoDecodeThread
//import com.alexvas.rtsp.widget.RtspStatusListener
//
//
///**
// * 把以前的一帧一帧图像改为 RTSP 标准的视频流
// *
// */
//open class LivePlayFragment : Fragment() {
//
//    private lateinit var binding: FragmentLivePlayerBinding
//    private lateinit var liveViewModel: LiveViewModel
//
//    private val rtspStatusSurfaceListener = object: RtspStatusListener {
//        override fun onRtspStatusConnecting() {
//            binding.apply {
//                pbLoading.visibility = View.VISIBLE
//            }
//        }
//
//        override fun onRtspStatusConnected() {
//            //延后2秒加载
//            binding.apply {
//                pbLoading.visibility = View.VISIBLE
//            }
//        }
//
//        override fun onRtspStatusDisconnecting() {
//
//        }
//
//        override fun onRtspStatusDisconnected() {
//
//        }
//
//        override fun onRtspStatusFailedUnauthorized() {
//
//        }
//
//        override fun onRtspStatusFailed(message: String?) {
//            Log.v(TAG, "onRtspStatusFailed()$message")
//            if (context == null) return
//            binding.apply {
//                var msg = message
//                if (message?.contains("Read timed out") == true) {
//                    msg = "正在拉取视频流，请稍后"
//                }
//                Toast.makeText(context, "$msg", Toast.LENGTH_LONG).show()
//            }
//            binding.svVideo.start(true, false)
//        }
//
//        override fun onRtspFirstFrameRendered() {
//            binding.apply {
//                pbLoading.visibility = View.INVISIBLE
//            }
//        }
//    }
//
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        liveViewModel = ViewModelProvider(this)[LiveViewModel::class.java]
//        binding = FragmentLivePlayerBinding.inflate(inflater, container, false)
//        binding.svVideo.setStatusListener(rtspStatusSurfaceListener)
//
//        return binding.root
//    }
//
//
//    fun stopRTSP() {
//        val started = binding.svVideo.isStarted()
//        liveViewModel.saveParams(requireContext())
//        if (started) {
//            binding.svVideo.stop()
//        }
//    }
//
//
//    override fun onResume() {
//        super.onResume()
//
//        if (!binding.svVideo.isStarted()) {
//            startRTSP()
//        }
//    }
//
//
//
//    private fun startRTSP(){
//        liveViewModel.loadParams(requireContext())
//        val urlString = "rtsp://" + NetUrl.getHost() + ":8554/live/app"
//        val uri = Uri.parse(urlString)
//        if (BuildConfig.DEBUG) {
//            Toasty.success(requireContext(), "地址:$urlString").show()
//            Log.v(TAG, "onResume()")
//        }
//
//        binding.svVideo.init(
//            uri,
//            "",
//            "",
//            "rtsp-client-android"
//        )
//
//        //这个参数是新加的，延时
//        binding.svVideo.experimentalUpdateSpsFrameWithLowLatencyParams = true
//
//        binding.svVideo.videoDecoderType = VideoDecodeThread.DecoderType.HARDWARE
//
//        Handler(Looper.getMainLooper()).postDelayed(
//            {
//                binding.svVideo.start(true, false)
//            }, 200
//        )
//
////        binding.svVideo.start(true, false)
//
//    }
//
//
//    override fun onStop() {
//        super.onStop()
//        stopRTSP()
//    }
//
//
//    companion object {
//        private val TAG: String = LivePlayFragment::class.java.simpleName
//    }
//
//}
