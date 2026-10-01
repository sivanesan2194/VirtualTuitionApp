# ProGuard rules for Virtual Tuition App
-keep class com.virtualtuition.app.models.** { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.razorpay.** { *; }
-dontwarn com.razorpay.**
