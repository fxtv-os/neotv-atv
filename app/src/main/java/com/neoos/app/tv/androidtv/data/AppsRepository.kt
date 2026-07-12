package com.neoos.neotv.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.neoos.neotv.model.AppChannel
import com.neoos.neotv.model.AppTile
import org.json.JSONObject

/**
 * Reads the curated (bundled) list of streaming apps and app-internal
 * "channels" from assets/apps_channels.json, and cross-checks against
 * PackageManager to see what's actually installed on this Android TV box.
 */
object AppsRepository {

    private fun readJson(context: Context): JSONObject {
        val text = context.assets.open("apps_channels.json").bufferedReader().use { it.readText() }
        return JSONObject(text)
    }

    private fun isInstalled(context: Context, pkg: String): Boolean = try {
        context.packageManager.getPackageInfo(pkg, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    fun getApps(context: Context): List<AppTile> {
        val json = readJson(context)
        val arr = json.optJSONArray("apps") ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val pkg = o.getString("package")
            AppTile(
                label = o.getString("label"),
                packageName = pkg,
                isInstalled = isInstalled(context, pkg)
            )
        }
    }

    fun getAppChannels(context: Context): List<AppChannel> {
        val json = readJson(context)
        val arr = json.optJSONArray("appChannels") ?: return emptyList()
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val pkg = o.getString("package")
            AppChannel(
                label = o.getString("label"),
                packageName = pkg,
                deepLinkUri = if (o.isNull("deepLink")) null else o.optString("deepLink"),
                logoUrl = if (o.isNull("logo")) null else o.optString("logo"),
                isInstalled = isInstalled(context, pkg)
            )
        }
    }

    /** Launches an app, using its deep link if given, else just its launcher intent. */
    fun launch(context: Context, packageName: String, deepLinkUri: String? = null) {
        val intent = if (!deepLinkUri.isNullOrBlank()) {
            Intent(Intent.ACTION_VIEW, Uri.parse(deepLinkUri)).apply {
                setPackage(packageName)
            }
        } else {
            context.packageManager.getLaunchIntentForPackage(packageName)
        }
        if (intent != null) {
            context.startActivity(intent)
        } else {
            // Not installed: send to Play Store listing instead
            val marketIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=$packageName")
            )
            context.startActivity(marketIntent)
        }
    }
}
