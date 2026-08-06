-dontwarn kotlinx.parcelize.Parcelize
#
#-dontwarn com.tencent.bugly.**
#-keep public class com.tencent.bugly.**{*;}

# Fix Missing class errors for com.vanniktech.maven.publish and Android Gradle Plugin APIs
-dontwarn com.vanniktech.maven.publish.**
-dontwarn com.android.build.api.**

# Fix Missing class errors for java.awt (used by Hutool)
-dontwarn java.awt.**
-dontwarn cn.hutool.**
