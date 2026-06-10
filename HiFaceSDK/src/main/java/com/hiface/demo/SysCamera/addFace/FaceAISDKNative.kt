package uts.sdk.modules.uniFaceAISDK

import androidx.lifecycle.LifecycleOwner
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import android.util.Log  
// 第三方库引用
import com.ai.face.core.engine.FaceAISDKEngine
import com.ai.face.faceSearch.search.Image2FaceFeature
import com.ai.face.faceSearch.search.FaceSearchEngine;
import com.ai.face.faceSearch.search.FaceSearchFeatureManger;
import com.ai.face.faceSearch.searchByFeature.FeatureSearchResult;

import com.ai.face.faceSearch.search.SearchProcessBuilder
import com.ai.face.faceSearch.search.SearchProcessCallBack
import com.ai.face.faceSearch.search.SearchProcessTipsCode
import com.ai.face.faceSearch.utils.FaceSearchResult


import com.faceAI.demo.FaceSDKConfig
import com.faceAI.demo.SysCamera.search.ImageToast
import com.faceAI.demo.base.utils.BitmapUtils
import com.faceAI.demo.base.utils.VoicePlayer
import com.tencent.mmkv.MMKV
import com.faceAI.demo.base.utils.TTSPlayer

// UTS/UniApp 引用
import io.dcloud.uts.UTSAndroid
import io.dcloud.uts.UTSJSONObject
import io.dcloud.uts.clearInterval
import io.dcloud.uts.console
import io.dcloud.uts.setInterval
import org.json.JSONObject

 
/**
 *  kotlin 方法集  
 *
 */
object FaceAISDKNative {
	
	
	/**
	 * 「1:N人脸识别」从照片中提取人脸特征
	 * 
	 */
	fun faceSearchByImageNative(context:Context,base64FaceSearch: String,searchThreshold:Float,callback: (UTSJSONObject) -> Unit){
		
		//Bitmap为null提示msg: base64ToBitmap failedF
		val bitmap = BitmapUtils.base64ToBitmap(base64FaceSearch)
		if (bitmap == null) {
		    val errorResult = UTSJSONObject()
		    errorResult["code"] = 0 // 0 代表失败
		    errorResult["msg"] = "base64ToBitmap failed" // 明确提示转码失败
		    callback(errorResult)
		    return // 结束执行
		}
		
		
        val builder = SearchProcessBuilder.Builder(context)
            .setLifecycleOwner(context as LifecycleOwner)
            .setSearchType(SearchProcessBuilder.SearchType.SINGLE_IMAGE)
            .setThreshold(searchThreshold)
            .setProcessCallBack(object : SearchProcessCallBack() {
                override fun onFaceMatched(
                    results: MutableList<FaceSearchResult>?,
                    bitmap: Bitmap?,
                    liveness: Float
                ) {				   
				   val result = UTSJSONObject()
				   result["code"] = 1  // 1 代表成功
				   result["msg"] = "onFaceMatched success"  
				   
				   val dataArray = ArrayList<Any>()
				   if (results != null) {
				          for (item in results) {
				              val itemObj = UTSJSONObject()
							  
							  itemObj["faceName"] = item.getFaceName()
							  itemObj["faceScore"] = item.getFaceScore()
				              
				              val rect = item.getRect() 
				              if (rect != null) {
				                  val rectObj = UTSJSONObject()
				                  rectObj["left"] = rect.left
				                  rectObj["top"] = rect.top
				                  rectObj["right"] = rect.right
				                  rectObj["bottom"] = rect.bottom
				                  itemObj["rect"] = rectObj
				              } else {
				                  itemObj["rect"] = null
				              }

				              dataArray.add(itemObj)
				          }
				    }
				   	               
				   result["result"] = dataArray				   
				   callback(result)
                }

                override fun onProcessTips(i: Int) {
                    if (i == SearchProcessTipsCode.NO_LIVE_FACE) {
                        val errorResult = UTSJSONObject()
                        errorResult["code"] = -1 // 0 代表失败
                        errorResult["msg"] = "No face detected" // 图片中没有人脸
                        callback(errorResult)
                    }else if(i == SearchProcessTipsCode.LOCAL_FACE_DATABASE_EMPTY) {
                        val errorResult = UTSJSONObject()
                        errorResult["code"] = -2 // 0 代表失败
                        errorResult["msg"] = "Face db no exist data" //人脸库没有数据
                        callback(errorResult)
                    }else if(i == SearchProcessTipsCode.SEARCH_PREPARED) {
		                FaceSearchEngine.getInstance().runSearchWithBitmap(bitmap)
                    }
                }
            }).create()

        FaceSearchEngine.getInstance().initSearchParams(builder)	
		   

	 }
	
	
	
	
	/**
	 * Toast 信息
	 * 
	 */
	fun TTSPlayer(message: String){
	    TTSPlayer.getInstance().playTTS(message); //语音播报faceID
	}
	
	
	/**
	 * Toast 信息
	 * 
	 */
	fun toastMessage(context:Context,base64Image: String,message: String){
        ImageToast().showBase64(context, base64Image, message)
	}
	
	
	/**
	 * 「1:N人脸识别」从照片中提取人脸特征
	 * 
	 */
	fun getFaceFeatureByImageNative(context:Context,faceID: String,base64FaceImage: String,callback: (UTSJSONObject) -> Unit){
		
	   //Bitmap为null提示msg: base64ToBitmap failed
	   val bitmap = BitmapUtils.base64ToBitmap(base64FaceImage)
	   if (bitmap == null) {
	       val errorResult = UTSJSONObject()
	       errorResult["code"] = 0 // 0 代表失败
	       errorResult["msg"] = "base64ToBitmap failed" // 明确提示转码失败
	       errorResult["faceID"] = faceID
		   errorResult["faceFeature"] = " "
	       callback(errorResult)
	       return // 结束执行
	   }
	       
		
	   Image2FaceFeature.getInstance(context).getFaceFeatureByBitmap(bitmap,faceID,object : Image2FaceFeature.Callback{
	       override fun onFailed(msg: String) {
			   var result: UTSJSONObject = object : UTSJSONObject() {
			   			var code = 0
			   			var msg = msg
			            var faceID = faceID
						var faceFeature =" "
			    }
			   	callback(result)
	       }
	   
	       override fun onSuccess(
	           bitmap: Bitmap,
	           faceID: String,
	           faceFeature: String
	       ) {
	           // 修复 1 & 2: 使用 context 而不是 this，使用属性 maxSimilarity 和 faceID
	           val featureSearchResult: FeatureSearchResult =
	               FaceSearchEngine.getInstance().getFeatureSearcher(context).search(faceFeature)
	               
	           if (featureSearchResult.maxSimilarity > 0.8) {
	               Log.e("录入人脸", "可能已经存在相似的人脸，请确认 " + featureSearchResult.faceID)
	           }
	       
	           // 修复 3: 使用 context 而不是 this
	           FaceSearchFeatureManger.getInstance(context)
	               .insertFaceFeature(faceID, faceFeature, System.currentTimeMillis(), "tag", "group")
	       
	           // 修复 4: 使用 context 而不是 this
	           FaceAISDKEngine.getInstance(context)
	               .saveCroppedFaceImage(bitmap, FaceSDKConfig.CACHE_SEARCH_FACE_DIR, faceID)      
	          
	           var result: UTSJSONObject = object : UTSJSONObject() {
	               var code = 1
	               var msg = "Add FaceFeatureByImage success"
	               var faceID = faceID
	               var faceFeature = faceFeature
	           }
	           callback(result)
	       }
		   
	   })
	}
	
}

