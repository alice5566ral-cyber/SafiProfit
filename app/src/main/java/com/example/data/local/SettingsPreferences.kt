package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppSettings
import com.example.domain.MoneyFormatter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "safiprofit_settings")

class SettingsPreferences(private val context: Context) {

    private object Keys {
        val DEFAULT_USD_RATE = stringPreferencesKey("default_usd_rate")
        val DEFAULT_EUR_RATE = stringPreferencesKey("default_eur_rate")
        val ALWAYS_SHOW_DECIMALS = booleanPreferencesKey("always_show_decimals")
        val WEEK_STARTS_ON_MONDAY = booleanPreferencesKey("week_starts_on_monday")
    }

    val settingsFlow: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        val usd = prefs[Keys.DEFAULT_USD_RATE]?.let { MoneyFormatter.parseBigDecimal(it) }
            ?.takeIf { it > BigDecimal.ZERO } ?: BigDecimal("10.00")
        val eur = prefs[Keys.DEFAULT_EUR_RATE]?.let { MoneyFormatter.parseBigDecimal(it) }
            ?.takeIf { it > BigDecimal.ZERO } ?: BigDecimal("10.85")
        AppSettings(
            defaultUsdRate = usd,
            defaultEurRate = eur,
            alwaysShowDecimals = prefs[Keys.ALWAYS_SHOW_DECIMALS] ?: false,
            weekStartsOnMonday = prefs[Keys.WEEK_STARTS_ON_MONDAY] ?: true
        )
    }

    suspend fun updateExchangeRates(usdRate: BigDecimal, eurRate: BigDecimal) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.DEFAULT_USD_RATE] = usdRate.toPlainString()
            prefs[Keys.DEFAULT_EUR_RATE] = eurRate.toPlainString()
        }
    }

    suspend fun setAlwaysShowDecimals(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.ALWAYS_SHOW_DECIMALS] = enabled
        }
    }

    suspend fun setWeekStartsOnMonday(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.WEEK_STARTS_ON_MONDAY] = enabled
        }
    }
}
