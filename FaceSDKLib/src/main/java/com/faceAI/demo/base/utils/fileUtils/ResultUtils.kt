package com.ai.face.search.facemanger.fileUtils

import android.graphics.Rect
import android.net.Uri
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * # ResultUtils
 *
 * @author javakam
 * @date 2020/12/10  11:06
 */
object ResultUtils {

    data class ResultShowBean(
        var originResult: String = "",
        var compressedResult: String = "",
        var originUri: Uri? = null,
        var compressedUri: Uri? = null,
    )

    fun RecyclerView.asVerticalList() {
        setHasFixedSize(true)
        itemAnimator = null
        layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State,
            ) {
                super.getItemOffsets(outRect, view, parent, state)
                outRect.set(0, 5, 0, 5)
            }
        })
    }








}