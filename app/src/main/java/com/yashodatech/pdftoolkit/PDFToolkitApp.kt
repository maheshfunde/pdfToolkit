package com.yashodatech.pdftoolkit

import android.app.Application
import android.util.Log
import com.itextpdf.kernel.utils.XmlProcessorCreator
import com.yashodatech.pdftoolkit.pdf.AndroidXmlParserFactory

class PDFToolkitApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            // Register Android-compatible XML parser factory for iText 7 to prevent
            // "This parser does not support specification" crashes on Android.
            XmlProcessorCreator.setXmlParserFactory(AndroidXmlParserFactory())
            Log.i("PDFToolkitApp", "Successfully initialized AndroidXmlParserFactory for iText")
        } catch (e: Throwable) {
            Log.e("PDFToolkitApp", "Failed to configure AndroidXmlParserFactory for iText", e)
        }
    }
}
