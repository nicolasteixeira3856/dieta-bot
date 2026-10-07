package app.fibrai.android.core.closure

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.fibrai.android.MainActivity
import app.fibrai.android.R
import app.fibrai.android.core.designsystem.aero.AeroDarkColors
import app.fibrai.android.core.designsystem.aero.AeroLightColors
import app.fibrai.android.domain.Closures
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The closure notification (A60 part B, ADR-044): `Fechamento do dia` or `Fechamento da semana`, the first line of the
 * text as content. App-controlled: small icon, title, text, accent colour; the rest is the system template. Opens the
 * Home, where the card is.
 */
class AndroidClosureNotifier(private val context: Context) : ClosureNotifier {
    override fun notify(period: String, content: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannel(context)
        val week = period == Closures.WEEK
        val open = PendingIntent.getActivity(
            context,
            if (week) REQUEST_WEEK else REQUEST_DAY,
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_CLOSURE, period)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(accent())
            .setContentTitle(if (week) "Fechamento da semana" else "Fechamento do dia")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        @Suppress("MissingPermission") // checked above
        NotificationManagerCompat.from(context).notify(if (week) ID_WEEK else ID_DAY, notification)
    }

    private fun accent(): Int {
        val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        return (if (night) AeroDarkColors else AeroLightColors).accentDefault.toArgb()
    }

    companion object {
        const val CHANNEL = "closures"
        private const val ID_DAY = 2201
        private const val ID_WEEK = 2202
        private const val REQUEST_DAY = -22
        private const val REQUEST_WEEK = -23

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL, "Fechamentos", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "O fechamento do dia às 22h e o da semana no domingo."
                },
            )
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object ClosureModule {
    @Provides
    @Singleton
    fun notifier(@ApplicationContext context: Context): ClosureNotifier = AndroidClosureNotifier(context)
}
