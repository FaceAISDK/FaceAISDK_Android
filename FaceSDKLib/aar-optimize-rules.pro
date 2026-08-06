# FaceSDKLib Release AAR：允许 R8 裁剪和优化，但禁止任何类名、方法名和字段名混淆。
-dontobfuscate

# 保留源文件、行号、泛型和注解信息，方便三方调试和定位问题。
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod

# Java 11 字符串拼接会在 Android desugar 阶段转换，Library R8 检查时无需对该 JDK 运行时类报警。
-dontwarn java.lang.invoke.StringConcatFactory

# AAR 构建时 R8 不知道未来接入方会调用哪些 API，
# 因此保留 FaceSDKLib 的公开/受保护 API，内部私有实现仍可被裁剪和优化。
-keep,allowoptimization public class com.faceAI.demo.** {
    public protected *;
}
-keep,allowoptimization public interface com.faceAI.demo.** {
    public protected *;
}
-keep,allowoptimization public enum com.faceAI.demo.** {
    public protected *;
}

# 保留 JNI native 方法名，避免 Java/Kotlin 与 so 之间的符号匹配失效。
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}
