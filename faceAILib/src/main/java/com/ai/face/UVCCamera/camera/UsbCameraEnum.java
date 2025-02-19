package com.ai.face.UVCCamera.camera;

import android.hardware.usb.UsbConfiguration;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.text.TextUtils;

import androidx.annotation.NonNull;

public enum UsbCameraEnum {
    RGB("RGB Camera", "RGB"),
    IR("IR Camera", "IR"),
    USB("USB Camera", "USB"),
    NONE("NONE", "NONE");;
    private final String name;
    private final String shortName;


    @NonNull
    public static UsbCameraEnum toUsbCameraEnum(UsbDevice device) {
        UsbCameraEnum the = NONE;
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
                for (UsbCameraEnum c : UsbCameraEnum.values()) {
                    if (interfaceName.toUpperCase().contains(c.getShortName().toUpperCase())) {
                        the = c;
                        break OUT;
                    }
                }
            }
        }

        return the;
    }

    UsbCameraEnum(String name, String shortName) {
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
