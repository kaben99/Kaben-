package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.JurisdictionOption
import com.example.model.UsageMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.clarityPrefsStore: DataStore<Preferences> by preferencesDataStore(
    name = "clarity_lens_prefs"
)

data class UserGatewayPreferences(
    val jurisdiction: JurisdictionOption = JurisdictionOption.US_DMV_CORRIDOR,
    val usageMode: UsageMode = UsageMode.PERSONAL,
    val hasAgreed: Boolean = false
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val JURISDICTION_ID = stringPreferencesKey("jurisdiction_id")
        val USAGE_MODE_ID = stringPreferencesKey("usage_mode_id")
        val HAS_AGREED = booleanPreferencesKey("has_agreed")
    }

    val preferencesFlow: Flow<UserGatewayPreferences> = context.clarityPrefsStore.data.map { prefs ->
        UserGatewayPreferences(
            jurisdiction = JurisdictionOption.fromId(prefs[Keys.JURISDICTION_ID]),
            usageMode = UsageMode.fromId(prefs[Keys.USAGE_MODE_ID]),
            hasAgreed = prefs[Keys.HAS_AGREED] ?: false
        )
    }

    suspend fun saveGatewayAgreement(
        jurisdiction: JurisdictionOption,
        usageMode: UsageMode,
        agreed: Boolean = true
    ) {
        context.clarityPrefsStore.edit { prefs ->
            prefs[Keys.JURISDICTION_ID] = jurisdiction.id
            prefs[Keys.USAGE_MODE_ID] = usageMode.id
            prefs[Keys.HAS_AGREED] = agreed
        }
    }

    suspend fun updateJurisdictionAndMode(
        jurisdiction: JurisdictionOption,
        usageMode: UsageMode
    ) {
        context.clarityPrefsStore.edit { prefs ->
            prefs[Keys.JURISDICTION_ID] = jurisdiction.id
            prefs[Keys.USAGE_MODE_ID] = usageMode.id
        }
    }
}
