package com.cmarcillovargas.turismovalencia.data

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Handler
import android.os.Looper
import androidx.annotation.ArrayRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.cmarcillovargas.turismovalencia.R
import com.cmarcillovargas.turismovalencia.data.model.LanguageCode
import java.util.concurrent.ConcurrentHashMap

/**
 * Quien cambia el idioma de la app en el sistema. La elección se guarda en Room (`SettingsRepository`), pero quien hace que los
 * recursos (`values-ca`, `values-en`, `values-fr`) cambien de verdad es Android: ver [AppCompatLanguageApplier].
 */
fun interface LanguageApplier {
    fun apply(language: LanguageCode)
}

/** No toca el sistema: para las pruebas, que no deben cambiar el idioma del móvil en el que corren. */
object NoLanguageApplier : LanguageApplier {
    override fun apply(language: LanguageCode) = Unit
}

/**
 * Idioma por app de Android (`AppCompatDelegate.setApplicationLocales`): en Android 13 o más lo guarda y lo aplica el sistema
 * (y el usuario también puede cambiarlo en Ajustes → Aplicaciones → Idioma de la app); en versiones anteriores lo hace AppCompat
 * (`MainActivity` es una `AppCompatActivity` y el manifiesto declara su servicio de almacenamiento). En los dos casos la actividad se
 * recrea con los recursos del idioma nuevo, y `MainActivity` vuelve a cargar con él los datos y las pantallas.
 */
class AppCompatLanguageApplier : LanguageApplier {
    private val main = Handler(Looper.getMainLooper())

    override fun apply(language: LanguageCode) {
        // `setApplicationLocales` recrea actividades: tiene que hacerse en el hilo principal.
        main.post { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag)) }
    }
}

/** El idioma con el que Android ha resuelto los recursos de [context]: lo dice su cadena `locale_code`. */
fun Context.appLanguage(): LanguageCode = LanguageCode.fromCode(getString(R.string.locale_code)) ?: LanguageCode.ES

/**
 * Textos de la app en el idioma elegido para quien no tiene una pantalla detrás (las reglas de ValBot, las sugerencias del
 * buscador, la notificación de la mascletà). Las pantallas usan `stringResource`; esto crea los `Resources` de un contexto con el
 * idioma de [language], que cambia con el de la app y no depende del que tenga el proceso en ese momento (un `BroadcastReceiver`
 * que despierta una alarma no pasa por ninguna actividad).
 */
class AppStrings(private val context: Context, initial: LanguageCode = LanguageCode.ES) {
    @Volatile
    var language: LanguageCode = initial

    private val byLanguage = ConcurrentHashMap<LanguageCode, Resources>()

    private fun resources(): Resources = byLanguage.getOrPut(language) {
        val configuration = Configuration(context.resources.configuration).apply { setLocale(language.locale) }
        context.createConfigurationContext(configuration).resources
    }

    fun get(@StringRes id: Int, vararg args: Any): String = resources().getString(id, *args)

    fun plural(@PluralsRes id: Int, quantity: Int, vararg args: Any): String = resources().getQuantityString(id, quantity, *args)

    fun array(@ArrayRes id: Int): List<String> = resources().getStringArray(id).toList()
}

/**
 * El último idioma de la app, guardado aparte de Room para quien necesita los textos sin abrir la base de datos: la alarma de un aviso
 * (`ReminderReceiver`) despierta el proceso sin pasar por ninguna actividad. En Android 13+ el sistema ya da el idioma de la app a
 * todo el proceso, pero en versiones anteriores solo lo hace AppCompat con las actividades.
 */
object AppLanguagePrefs {
    private const val File = "language"
    private const val Key = "code"

    fun read(context: Context): LanguageCode =
        LanguageCode.fromCode(context.getSharedPreferences(File, Context.MODE_PRIVATE).getString(Key, null)) ?: LanguageCode.ES

    fun write(context: Context, language: LanguageCode) {
        context.getSharedPreferences(File, Context.MODE_PRIVATE).edit().putString(Key, language.code).apply()
    }
}
