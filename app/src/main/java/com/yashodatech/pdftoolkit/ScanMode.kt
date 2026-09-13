package com.yashodatech.pdftoolkit

enum class ScanMode(val label: String, val description: String) {
    ORIGINAL("Original", "No enhancement"),
    SCAN("Scan", "Scanned document"),
    GRAYSCALE("Grayscale", "Black & white document"),
    BLACK_WHITE("Black & White", "Black & white document"),
    LIGHTEN("Lighten", "Lightened document"),
    DARKEN("Darken", "Darkened document")
}