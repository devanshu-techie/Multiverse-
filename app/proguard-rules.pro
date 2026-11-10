# Keep kotlinx.serialization generated serializers
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class com.devanshu.miniverse.** {
    *** Companion;
}
-keepclasseswithmembers class com.devanshu.miniverse.** {
    kotlinx.serialization.KSerializer serializer(...);
}
