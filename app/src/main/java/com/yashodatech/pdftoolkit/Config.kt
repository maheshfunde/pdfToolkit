package com.yashodatech.pdftoolkit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object Config {
    const val PACKAGE_NAME = "com.yashodatech.pdftoolkit"
    const val APP_NAME = "PDF Toolkit – PDF Editor, Merge, Compress, Split"
    const val APP_VERSION = "1.5"

    // Email
    const val DEVELOPER_EMAIL = "fundemahesh@gmail.com"

    // Hosted Pages (Replace with your actual URLs)
    const val PRIVACY_POLICY_URL = "https://benevolent-beijinho-65df2d.netlify.app/privacy-policy"
    const val DISCLAIMER_URL = "https://benevolent-beijinho-65df2d.netlify.app/disclaimer"

    // Play Store Links (Will auto-work after publishing)
    const val PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=$PACKAGE_NAME"
    const val PLAY_STORE_MARKET = "market://details?id=$PACKAGE_NAME"

    const val DEVELOPER_PAGE_MARKET = "market://search?q=pub:Mahesh Funde"
    const val DEVELOPER_PAGE_URL = "https://play.google.com/store/apps/developer?id=Mahesh Funde"

    // Share Text
    const val SHARE_TEXT = """
📄 Check out PDF Toolkit!

Merge, Split, Compress PDFs and Convert Images to PDF — all in one app!

Download now:
https://play.google.com/store/apps/details?id=$PACKAGE_NAME
    """
    var SHOW_ADS by mutableStateOf(false)
}