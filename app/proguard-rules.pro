# Room entities and DAOs are kept by the Room artifacts' consumer rules.
# Broadcast receivers are referenced from the manifest and kept automatically.
# TensorFlow Lite in Play services is called through its Java API; keep it whole.
-keep class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**
