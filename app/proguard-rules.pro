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

# ===== Gson 配置 =====
# 保留 Gson 序列化/反序列化所需的注解
-keepattributes Signature
-keepattributes *Annotation*

# 保留 Source 类的所有成员，防止 R8 混淆字段名
-keepclassmembers class com.lengyuefenghua.newsreader.data.Source {
    *;
}

# 更通用的 Gson 规则（保留所有带 @SerializedName 注解的类）
-keep class com.google.gson.annotations.SerializedName
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# 保留所有实现了 Serializable 的数据类
-keep class com.lengyuefenghua.newsreader.data.** { *; }

# ===== v0.0.3 新增类保护 =====
# 保留 SettingsManager 和相关设置类
-keep class com.lengyuefenghua.newsreader.util.SettingsManager { *; }

# 保留刷新进度数据类
-keep class com.lengyuefenghua.newsreader.data.RefreshProgress { *; }
-keep class com.lengyuefenghua.newsreader.data.RefreshSummary { *; }

# ===== v0.0.5 数据备份类保护 =====
# 保留所有数据备份相关类
-keep class com.lengyuefenghua.newsreader.data.BackupData { *; }
-keep class com.lengyuefenghua.newsreader.data.BackupSettings { *; }
-keep class com.lengyuefenghua.newsreader.data.Article { *; }
-keep class com.lengyuefenghua.newsreader.data.Source { *; }

# 保留所有 ViewModel 中的数据类
-keep class com.lengyuefenghua.newsreader.viewmodel.ImportResult { *; }