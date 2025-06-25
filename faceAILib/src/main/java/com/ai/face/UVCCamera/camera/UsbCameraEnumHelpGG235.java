package com.ai.face.UVCCamera.camera;

import android.hardware.usb.UsbConfiguration;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.text.TextUtils;
import androidx.annotation.NonNull;

/**
 * 摄像头种类，每家摄像头的命名不太一样，需要你根据用的摄像头自行配置
 *
 */

@Deprecated
public enum UsbCameraEnumHelpGG235 {
    RGB("RGB Camera", "RGB"),
    IR("IR Camera", "IR"),
    USB("USB Camera", "USB"),
    NONE("NONE", "NONE");;

    private final String name;
    private final String shortName;


    /**
     * 检测你的UVC 协议USB 摄像头是否符合枚举种类，符合返回对应的枚举种类
     * 大部份的摄像头都是上述样式命名，不是这种规范需要你调试修改
     *
     * @param device
     * @return
     */
    @NonNull
    @Deprecated
    public static UsbCameraEnumHelpGG235 toUsbCameraEnum(UsbDevice device) {
        UsbCameraEnumHelpGG235 the = NONE;
        if (device == null) {
            return the;
        }

        OUT:
        for (int i = 0; i < device.getConfigurationCount(); i++) {
            UsbConfiguration configuration = device.getConfiguration(i);
            int interfaceCount = configuration.getInterfaceCount();
            for (int n = 0; n < interfaceCount; n++) {
                UsbInterface usbInterface = configuration.getInterface(n);
                String interfaceName = usbInterface.getName();
                if (TextUtils.isEmpty(interfaceName)) {
                    continue;
                }
                for (UsbCameraEnumHelpGG235 c : UsbCameraEnumHelpGG235.values()) {
                    if (interfaceName.toUpperCase().contains(c.getShortName().toUpperCase())) {
                        the = c;
                        break OUT;
                    }
                }
            }
        }

        return the;
    }

    UsbCameraEnumHelpGG235(String name, String shortName) {
        this.name = name;
        this.shortName = shortName;
    }

    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }
}
