package com.cptodix.rimalux.desktop.platform

import java.io.PrintWriter
import java.io.StringWriter
import java.util.logging.FileHandler
import java.util.logging.Level
import java.util.logging.Logger
import java.util.logging.SimpleFormatter

/**
 * Small application logger writing to the platform state directory.
 */
object AppLog {
    private val logger: Logger = Logger.getLogger("RimaluxDesktop")
    private var initialized = false

    fun initialize(paths: PlatformPaths) {
        if (initialized) return
        initialized = true
        runCatching {
            paths.ensureDirectories()
            logger.useParentHandlers = false
            logger.level = Level.INFO
            val handler = FileHandler(paths.logFile.toString(), 1_000_000, 3, true)
            handler.formatter = SimpleFormatter()
            handler.level = Level.INFO
            logger.addHandler(handler)
            logger.info("Rimalux Desktop log initialized at ${paths.logFile}")
        }
    }

    fun info(message: String) {
        logger.info(message)
    }

    fun warning(message: String, error: Throwable? = null) {
        if (error == null) {
            logger.warning(message)
        } else {
            logger.log(Level.WARNING, message, error)
        }
    }

    fun error(message: String, error: Throwable? = null) {
        if (error == null) {
            logger.severe(message)
        } else {
            logger.log(Level.SEVERE, message, error)
        }
    }

    fun stackTrace(error: Throwable): String {
        val writer = StringWriter()
        error.printStackTrace(PrintWriter(writer))
        return writer.toString()
    }
}
