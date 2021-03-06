package jetbrains.xodus.browser.web

import mu.KLogging


fun main(args: Array<String>) {
    val port = Integer.getInteger("server.port", 18080)
    val host = System.getProperty("server.host", "localhost")
    val context = System.getProperty("server.context", "")
    Application.start()

    HttpServer(host, port, context).setup()

    OS.launchBrowser(host, port, context)
}

private object OS : KLogging() {

    fun launchBrowser(host: String, port: Int, context: String) {
        val url = "http://$host:$port/$context"
        logger.info { "try to open browser for '$url'" }
        try {
            val osName = "os.name".system()
            if (osName.startsWith("Mac OS")) {
                logger.info("mac os detected")
                val fileMgr = Class.forName("com.apple.eio.FileManager")
                val openURL = fileMgr.getDeclaredMethod("openURL", String::class.java)
                openURL.invoke(null, url)
            } else if (osName.startsWith("Windows")) {
                logger.info("windows detected")
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler $url")
            } else {
                // Unix or Linux
                logger.info("linux detected")
                val browsers = arrayOf("google-chrome", "firefox", "opera", "konqueror", "epiphany", "mozilla", "netscape")
                val selectedBrowser: String? = browsers.firstOrNull { Runtime.getRuntime().exec(arrayOf("which", it)).waitFor() == 0 }
                if (selectedBrowser == null) {
                    throw Exception("Couldn't find web browser")
                } else {
                    logger.info { "open url using browser $selectedBrowser" }
                    Runtime.getRuntime().exec(arrayOf(selectedBrowser, url))
                }
            }
        } catch (e: Exception) {
            println("Unable to open browser: " + e.message)
        }
    }

}
