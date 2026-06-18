# ===== Extreme Coffee - regole R8/ProGuard =====
# Attributi necessari alla deserializzazione Firestore (reflection)
-keepattributes Signature, *Annotation*, RuntimeVisibleAnnotations, AnnotationDefault, InnerClasses, EnclosingMethod

# Modelli serializzati da Firestore (toObject<> usa reflection): NON offuscare
-keep class com.extremecoffee.app.model.** { *; }
-keepclassmembers class com.extremecoffee.app.model.** {
    <init>();
    <fields>;
    public <methods>;
}

# Annotazioni PropertyName di Firestore
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName *;
}

# Google Maps / Places (sicurezza extra; di norma già coperti dai loro consumer rules)
-keep class com.google.android.gms.maps.** { *; }
-keep class com.google.android.libraries.places.** { *; }

# Riduce i warning su provider crittografici opzionali
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
