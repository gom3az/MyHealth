# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Huawei Wear Engine SDK - narrow the keep rules
# Only keep specific classes that are actually used at runtime

# Keep only the WearEngine auth manager that we directly reference
-keep class com.huawei.wearengine.auth.AuthManager { *; }

# HMS core utilities that may be needed at runtime (keep minimal set)
-keep class com.huawei.hms.support.api.entity.auth.Scope { *; }