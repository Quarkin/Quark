package com.android.launcher3.iconpack

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.content.res.XmlResourceParser
import android.graphics.drawable.Drawable
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream

data class IconPackInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable? = null
)

class IconPackManager(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    // Cache of parsed appfilter per icon pack package: componentKey ("package/class") -> drawableName
    private val appfilterCache = mutableMapOf<String, Map<String, String>>()
    // Cache of unique drawable names per icon pack package for the Icon Picker
    private val drawablesCache = mutableMapOf<String, List<String>>()

    companion object {
        private val ICON_PACK_INTENTS = listOf(
            "org.adw.launcher.THEMES",
            "com.novalauncher.THEME",
            "com.gau.go.launcherex.theme",
            "com.fede.launcher.THEME_ICONPACK",
            "com.teslacoilsw.launcher.THEME",
            "com.anddoes.launcher.THEME"
        )
    }

    fun getInstalledIconPacks(): List<IconPackInfo> {
        val result = mutableMapOf<String, IconPackInfo>()

        for (action in ICON_PACK_INTENTS) {
            val intent = Intent(action)
            val list: List<ResolveInfo> = packageManager.queryIntentActivities(intent, 0)
            for (info in list) {
                val pkg = info.activityInfo.packageName
                if (pkg != context.packageName && !result.containsKey(pkg)) {
                    val label = info.loadLabel(packageManager).toString()
                    val icon = info.loadIcon(packageManager)
                    result[pkg] = IconPackInfo(pkg, label, icon)
                }
            }
        }
        return result.values.sortedBy { it.label }
    }

    suspend fun getAppFilter(iconPackPackage: String): Map<String, String> = withContext(Dispatchers.IO) {
        if (appfilterCache.containsKey(iconPackPackage)) {
            return@withContext appfilterCache[iconPackPackage] ?: emptyMap()
        }

        val map = mutableMapOf<String, String>()
        val drawableList = mutableSetOf<String>()

        try {
            val packContext = context.createPackageContext(iconPackPackage, Context.CONTEXT_IGNORE_SECURITY)
            val res = packContext.resources

            var parser: XmlPullParser? = null

            // 1. Try res/xml/appfilter.xml
            val xmlId = res.getIdentifier("appfilter", "xml", iconPackPackage)
            if (xmlId != 0) {
                try {
                    parser = res.getXml(xmlId)
                } catch (ignored: Exception) {
                }
            }

            // 2. Try assets/appfilter.xml
            if (parser == null) {
                try {
                    val stream: InputStream = packContext.assets.open("appfilter.xml")
                    val factory = XmlPullParserFactory.newInstance()
                    factory.isNamespaceAware = true
                    parser = factory.newPullParser().apply {
                        setInput(stream, "utf-8")
                    }
                } catch (ignored: Exception) {
                }
            }

            // Parse appfilter items: <item component="ComponentInfo{package/class}" drawable="name" />
            if (parser != null) {
                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                        val component = parser.getAttributeValue(null, "component")
                        val drawable = parser.getAttributeValue(null, "drawable")
                        if (!component.isNullOrBlank() && !drawable.isNullOrBlank()) {
                            drawableList.add(drawable)
                            // Clean ComponentInfo{com.example/com.example.MainActivity} -> com.example/com.example.MainActivity
                            val cleaned = component
                                .replace("ComponentInfo{", "")
                                .replace("}", "")
                                .trim()
                            map[cleaned] = drawable
                        }
                    }
                    eventType = parser.next()
                }
            }
        } catch (e: Exception) {
            Log.e("IconPackManager", "Failed to parse appfilter for $iconPackPackage", e)
        }

        appfilterCache[iconPackPackage] = map
        drawablesCache[iconPackPackage] = drawableList.toList().sorted()
        map
    }

    suspend fun getAvailableDrawables(iconPackPackage: String): List<String> = withContext(Dispatchers.IO) {
        if (drawablesCache.containsKey(iconPackPackage)) {
            return@withContext drawablesCache[iconPackPackage] ?: emptyList()
        }
        getAppFilter(iconPackPackage)
        drawablesCache[iconPackPackage] ?: emptyList()
    }

    fun loadDrawable(iconPackPackage: String, drawableName: String): Drawable? {
        return try {
            val packContext = context.createPackageContext(iconPackPackage, Context.CONTEXT_IGNORE_SECURITY)
            val resId = packContext.resources.getIdentifier(drawableName, "drawable", iconPackPackage)
            if (resId != 0) {
                packContext.resources.getDrawable(resId, null)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
