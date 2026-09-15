package no.nav.syfo

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.reactor.mono
import java.io.File
import java.util.jar.JarFile

object CoroutineRuntimeVerifier {
    private val expectedModules =
        setOf(
            "kotlinx-coroutines-core-jvm",
            "kotlinx-coroutines-reactive",
            "kotlinx-coroutines-reactor",
        )
    private val coroutineJarPattern =
        Regex("^(kotlinx-coroutines-(?:core-jvm|reactive|reactor))-(.+)\\.jar$")

    @JvmStatic
    fun main(args: Array<String>) {
        mono<Unit> { awaitCancellation() }.subscribe().dispose()

        val runtimeVersions =
            versionsFrom(
                System.getProperty("java.class.path")
                    .split(File.pathSeparator)
                    .asSequence()
                    .map { File(it).name },
            )
        val packagedVersions =
            JarFile(args.single()).use { appJar ->
                versionsFrom(appJar.entries().asSequence().map { it.name.substringAfterLast('/') })
            }

        check(packagedVersions == runtimeVersions) {
            "Coroutine modules in app.jar $packagedVersions do not match runtimeClasspath $runtimeVersions"
        }
    }

    private fun versionsFrom(fileNames: Sequence<String>): Map<String, String> {
        val modules =
            fileNames
                .mapNotNull { fileName ->
                    coroutineJarPattern.matchEntire(fileName)?.destructured?.let { (module, version) ->
                        module to version
                    }
                }.toList()

        check(modules.size == expectedModules.size && modules.map { it.first }.toSet() == expectedModules) {
            "Expected one jar for each coroutine runtime module $expectedModules, found $modules"
        }
        check(modules.map { it.second }.toSet().size == 1) {
            "Mixed coroutine versions: $modules"
        }
        return modules.toMap()
    }
}
