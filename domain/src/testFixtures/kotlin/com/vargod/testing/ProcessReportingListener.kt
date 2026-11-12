package com.vargod.testing

import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.support.descriptor.ClassSource
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Учёт тестовых процессов и потоков (ТиОПО ЛР1, требование 12).
 *
 * Регистрируется через ServiceLoader (META-INF/services) и поэтому активен в любой
 * тестовой JVM, у которой на classpath есть testFixtures модуля domain. По окончании
 * тест-плана пишет файл `<pid>.pid` в каталог из системного свойства `vargod.test.pidDir`
 * (его задаёт Gradle). Число файлов = число JVM, в которых реально выполнялись тесты;
 * внутри файла — сколько классов/тестов прошло в этой JVM и сколько разных потоков
 * их выполняло (1 по умолчанию, N при -Pthreads=N).
 */
class ProcessReportingListener : TestExecutionListener {

    private val classes = AtomicInteger()
    private val tests = AtomicInteger()
    private val threads = ConcurrentHashMap.newKeySet<String>()

    override fun executionStarted(testIdentifier: TestIdentifier) {
        if (testIdentifier.isTest) threads += Thread.currentThread().name
    }

    override fun executionFinished(testIdentifier: TestIdentifier, testExecutionResult: TestExecutionResult) {
        if (testIdentifier.isTest) {
            tests.incrementAndGet()
        } else if (testIdentifier.source.map { it is ClassSource }.orElse(false)) {
            classes.incrementAndGet()
        }
    }

    override fun testPlanExecutionFinished(testPlan: TestPlan) {
        val dir = System.getProperty("vargod.test.pidDir") ?: return
        val pid = ProcessHandle.current().pid()
        File(dir).apply { mkdirs() }
            .resolve("$pid.pid")
            .appendText("test classes=${classes.get()}, tests=${tests.get()}, threads=${threads.size}\n")
    }
}
