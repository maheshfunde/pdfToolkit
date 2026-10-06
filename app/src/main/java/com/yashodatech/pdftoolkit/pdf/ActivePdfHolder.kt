package com.yashodatech.pdftoolkit.pdf

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ActivePdfHolder {
    var activeUri by mutableStateOf<Uri?>(null)

    fun set(uri: Uri?) {
        activeUri = uri
    }

    fun consume(): Uri? {
        val uri = activeUri
        activeUri = null
        return uri
    }

    fun clear() {
        activeUri = null
    }
}
