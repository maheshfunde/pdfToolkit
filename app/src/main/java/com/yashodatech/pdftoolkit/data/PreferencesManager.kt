package com.yashodatech.pdftoolkit.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

data class RecentDoc(
    val name: String,
    val uri: String,
    val tool: String,    // "Organize" / "Watermark" / etc.
    val timestamp: Long  // System.currentTimeMillis()
)

// How the apps visual theme is chosen. "SYSTEM" follows the OS setting.
enum class ThemeMode(val key: String, val label: String) {
    SYSTEM("SYSTEM", "System"),
    LIGHT("LIGHT", "Light"),
    DARK("DARK", "Dark")
}

private val Context.dataStore by preferencesDataStore("app_preferences")

class PreferencesManager(private val context: Context) {

    companion object {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val PDF_QUALITY = stringPreferencesKey("pdf_quality")
        val PDF_PAGE_SIZE = stringPreferencesKey("pdf_page_size")
        val APP_OPEN_COUNT = intPreferencesKey("app_open_count")
        val HAS_RATED = booleanPreferencesKey("has_rated")
        val LAST_USED_TOOL = stringPreferencesKey("last_used_tool")
        val RECENT_DOCS = stringPreferencesKey("recent_docs") // JSON array of RecentDoc
        val THEME_MODE = stringPreferencesKey("theme_mode") // ThemeMode.key
        val HOME_CATEGORY = intPreferencesKey("home_category") // browsed segment index
    }

    // ── Onboarding ──────────────────────────────────
    val onboardingCompleted: Flow<Boolean> = context.dataStore.data
        .map { it[ONBOARDING_COMPLETED] ?: false }

    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { it[ONBOARDING_COMPLETED] = true }
    }

    // ── App Open Count ──────────────────────────────
    val appOpenCount: Flow<Int> = context.dataStore.data
        .map { it[APP_OPEN_COUNT] ?: 0 }

    suspend fun incrementAppOpenCount() {
        context.dataStore.edit {
            val current = it[APP_OPEN_COUNT] ?: 0
            it[APP_OPEN_COUNT] = current + 1
        }
    }

    // ── Has Rated ───────────────────────────────────
    val hasRated: Flow<Boolean> = context.dataStore.data
        .map { it[HAS_RATED] ?: false }

    suspend fun setHasRated() {
        context.dataStore.edit { it[HAS_RATED] = true }
    }

    // ── Last Used Tool ──────────────────────────────
    val lastUsedTool: Flow<String> = context.dataStore.data
        .map { it[LAST_USED_TOOL] ?: "" }

    suspend fun setLastUsedTool(route: String) {
        context.dataStore.edit { it[LAST_USED_TOOL] = route }
    }

    // ── Theme Mode ───────────────────────────────────
    val themeMode: Flow<String> = context.dataStore.data
        .map { it[THEME_MODE] ?: ThemeMode.SYSTEM.key }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[THEME_MODE] = mode }
    }

    // ── Home Category (last-browsed segment) ──────────────
    // Null until the user first browses; persists the category so relaunching
    // the app returns them to where they were, not always "Create".
    val homeCategory: Flow<Int?> = context.dataStore.data
        .map { it[HOME_CATEGORY] }

    suspend fun setHomeCategory(index: Int) {
        context.dataStore.edit { it[HOME_CATEGORY] = index }
    }

    // ── PDF Quality ─────────────────────────────────
    val pdfQuality: Flow<String> = context.dataStore.data
        .map { it[PDF_QUALITY] ?: "HIGH" }

    suspend fun setPdfQuality(quality: String) {
        context.dataStore.edit { it[PDF_QUALITY] = quality }
    }

    // ── PDF Page Size ───────────────────────────────
    val pdfPageSize: Flow<String> = context.dataStore.data
        .map { it[PDF_PAGE_SIZE] ?: "FIT_IMAGE" }

    suspend fun setPdfPageSize(size: String) {
        context.dataStore.edit { it[PDF_PAGE_SIZE] = size }
    }

    // ── Recent Documents ────────────────────────────
    private val MAX_RECENT = 20

    val recentDocs: Flow<List<RecentDoc>> = context.dataStore.data
        .map { prefs ->
            val json = prefs[RECENT_DOCS] ?: return@map emptyList()
            try {
                val arr = JSONArray(json)
                (0 until arr.length()).mapNotNull { i ->
                    val obj = arr.getJSONObject(i)
                    RecentDoc(
                        name = obj.getString("name"),
                        uri = obj.getString("uri"),
                        tool = obj.getString("tool"),
                        timestamp = obj.getLong("ts")
                    )
                }
            } catch (_: Exception) { emptyList() }
        }

    suspend fun addRecentDoc(doc: RecentDoc) {
        context.dataStore.edit { prefs ->
            val existing = try {
                val arr = JSONArray(prefs[RECENT_DOCS] ?: "[]")
                (0 until arr.length()).mapNotNull { i ->
                    val obj = arr.getJSONObject(i)
                    RecentDoc(obj.getString("name"), obj.getString("uri"), obj.getString("tool"), obj.getLong("ts"))
                }
            } catch (_: Exception) { emptyList() }

            // Remove duplicate by URI, prepend new, cap at MAX_RECENT
            val updated = listOf(doc) + existing.filter { it.uri != doc.uri }
            prefs[RECENT_DOCS] = serializeRecentDocs(updated.take(MAX_RECENT))
        }
    }

    // Rewrites the shelf wholesale — used to prune entries whose file has been
    // deleted from the device.
    suspend fun setRecentDocs(docs: List<RecentDoc>) {
        context.dataStore.edit { prefs ->
            prefs[RECENT_DOCS] = serializeRecentDocs(docs.take(MAX_RECENT))
        }
    }

    private fun serializeRecentDocs(docs: List<RecentDoc>): String {
        val arr = JSONArray()
        docs.forEach { d ->
            arr.put(JSONObject().apply {
                put("name", d.name)
                put("uri", d.uri)
                put("tool", d.tool)
                put("ts", d.timestamp)
            })
        }
        return arr.toString()
    }
}