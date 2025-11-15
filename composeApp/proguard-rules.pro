-dontwarn javax.swing.*
-dontwarn com.intellij.icons.*
-dontwarn org.slf4j.impl.*
-dontwarn reactor.blockhound.**

# ICU4J uses reflection, so we need to keep its classes if we want to access its data
-keep class com.ibm.icu.** { *; }
-dontwarn com.ibm.icu.dev.tool.docs.**