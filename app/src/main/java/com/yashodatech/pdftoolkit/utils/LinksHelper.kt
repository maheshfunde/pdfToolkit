package com.yashodatech.pdftoolkit.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import com.yashodatech.pdftoolkit.Config

object LinkHelper {

    // ================================
    // 📜 Privacy Policy & Disclaimer
    // ================================

    fun openPrivacyPolicy(context: Context) {
        openUrl(context, Config.PRIVACY_POLICY_URL)
    }

    fun openDisclaimer(context: Context) {
        openUrl(context, Config.DISCLAIMER_URL)
    }

    // ================================
    // ⭐ Rate App
    // ================================

    fun rateApp(context: Context) {
        try {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(Config.PLAY_STORE_MARKET)
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            openUrl(context, Config.PLAY_STORE_URL)
        }
    }

    // ================================
    // 📤 Share App
    // ================================

    fun shareApp(context: Context) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, Config.APP_NAME)
            putExtra(Intent.EXTRA_TEXT, Config.SHARE_TEXT.trimIndent())
        }
        context.startActivity(
            Intent.createChooser(intent, "Share via")
        )
    }

    // ================================
    // 📧 Contact Us
    // ================================

    fun contactUs(context: Context) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(Config.DEVELOPER_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, "${Config.APP_NAME} - Feedback")
            putExtra(
                Intent.EXTRA_TEXT, """
                App: ${Config.APP_NAME}
                Version: ${Config.APP_VERSION}
                Device: ${Build.MODEL}
                Android: ${Build.VERSION.RELEASE}
                
                Hi, I would like to...
            """.trimIndent()
            )
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
        }
    }

    // ================================
    // 📱 More Apps
    // ================================

    fun moreApps(context: Context) {
        try {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(Config.DEVELOPER_PAGE_MARKET)
            )
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            openUrl(context, Config.DEVELOPER_PAGE_URL)
        }
    }

    // ================================
    // 🔧 Helper
    // ================================

    fun openUrl(context: Context, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open link", Toast.LENGTH_SHORT).show()
        }
    }
}