package com.AI.FaceVerification;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.AI.FaceVerify.utils.AiUtil;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        Bitmap bitmap = AiUtil.readFromAssets(appContext, "test.png");
        int[] floatValues = normalizeImage(bitmap);
        for (int i = 0; i <floatValues.length ; i++) {
            Log.i("infor", ""+floatValues[i] + "-" + i);
        }
    }

    private int[] normalizeImage(Bitmap bitmap) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int[] floatValues = new int[w * h * 3];
        int[] intValues = new int[w * h];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        for (int i = 0; i < intValues.length; i++) {
            final int val = intValues[i];
            floatValues[i * 3] = ((val >> 16) & 0xFF);
            floatValues[i * 3 + 1] = ((val >> 8) & 0xFF);
            floatValues[i * 3 + 2] = (val & 0xFF);
        }
        return floatValues;
    }
}
