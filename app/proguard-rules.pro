# Room - keep entity and DAO classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-keepclassmembers @androidx.room.Entity class * { *; }

# Room - keep query result data classes (used for @Query return types)
-keep class com.clubdarts.data.db.dao.ScoreFrequency { *; }
-keep class com.clubdarts.data.db.dao.PlayerStatsAggregate { *; }

# Hilt - keep generated components
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keepnames @dagger.hilt.android.lifecycle.HiltViewModel class *

# Kotlin - coroutines and reflection
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable
-keep class kotlin.coroutines.Continuation
-dontwarn kotlinx.coroutines.**

# Kotlin serialization (data classes used across ViewModels)
-keepclassmembers class com.clubdarts.** {
    <fields>;
}

# Jetpack Compose - keep lambdas and function types used in slot APIs
-keepclassmembers class androidx.compose.** { *; }

# SQLite / SupportSQLite
-keep class androidx.sqlite.db.** { *; }

# Keep app model classes (Room entities & UI state)
-keep class com.clubdarts.data.model.** { *; }
-keep class com.clubdarts.ui.**.** { *; }
