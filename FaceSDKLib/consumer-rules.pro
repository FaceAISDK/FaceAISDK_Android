# 只限制 FaceSDKLib 自己的类/成员名，不使用 -dontobfuscate，
# 避免关闭接入方整个 App 的混淆。未使用的内部代码仍可被最终 App 的 R8 删除。
-keepnames class com.faceAI.demo.**
-keepclassmembernames class com.faceAI.demo.** {
    *;
}
