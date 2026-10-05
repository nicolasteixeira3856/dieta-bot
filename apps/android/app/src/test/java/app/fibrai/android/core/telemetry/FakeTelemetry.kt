package app.fibrai.android.core.telemetry

class FakeTelemetry : Telemetry {
    val events = mutableListOf<Pair<String, Map<String, Any>>>()
    val breadcrumbs = mutableListOf<String>()
    val nonFatals = mutableListOf<Throwable>()
    val keys = mutableMapOf<String, String>()

    override fun event(name: String, params: Map<String, Any>) {
        events += name to params
    }

    override fun breadcrumb(message: String) {
        breadcrumbs += message
    }

    override fun nonFatal(error: Throwable) {
        nonFatals += error
    }

    override fun setKey(key: String, value: String) {
        keys[key] = value
    }

    fun params(name: String): List<Map<String, Any>> = events.filter { it.first == name }.map { it.second }
}
