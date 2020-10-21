package xyz.srclab.common.run

import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor

interface ScheduledRunner : Runner {

    @Throws(RejectedExecutionException::class)
    fun <V> schedule(task: () -> V, delay: Duration): ScheduledRunning<V>

    @Throws(RejectedExecutionException::class)
    fun <V> scheduleAtFixedRate(task: () -> V, initialDelay: Duration, period: Duration): ScheduledRunning<V>

    @Throws(RejectedExecutionException::class)
    fun <V> scheduleWithFixedDelay(task: () -> V, initialDelay: Duration, period: Duration): ScheduledRunning<V>

    companion object {

        @JvmStatic
        fun asyncRunner(): NewThreadScheduledRunner = NewThreadScheduledRunner

        @JvmStatic
        fun singleThreadRunner(): ScheduledExecutorServiceRunner {
            return scheduledExecutorServiceRunner(Executors.newSingleThreadScheduledExecutor())
        }

        @JvmStatic
        fun threadPoolRunner(corePoolSize: Int): ScheduledExecutorServiceRunner {
            return scheduledExecutorServiceRunner(Executors.newScheduledThreadPool(corePoolSize))
        }

        @JvmStatic
        fun scheduledExecutorServiceRunner(
            scheduledExecutorService: ScheduledExecutorService
        ): ScheduledExecutorServiceRunner {
            return ScheduledExecutorServiceRunner(scheduledExecutorService)
        }

        @JvmStatic
        fun scheduledThreadPoolRunner(
            scheduledThreadPoolExecutor: ScheduledThreadPoolExecutor
        ): ScheduledThreadPoolRunner {
            return ScheduledThreadPoolRunner(scheduledThreadPoolExecutor)
        }

        @JvmStatic
        fun scheduledThreadPoolRunnerBuilder(): ScheduledThreadPoolRunner.Builder {
            return ScheduledThreadPoolRunner.Builder()
        }

        @JvmStatic
        fun <V> scheduleNewThread(task: () -> V, delay: Duration): ScheduledRunning<V> {
            return asyncRunner().schedule(task, delay)
        }

        @JvmStatic
        fun <V> scheduleNewThreadAtFixedRate(
            task: () -> V,
            initialDelay: Duration,
            period: Duration
        ): ScheduledRunning<V> {
            return asyncRunner().scheduleAtFixedRate(task, initialDelay, period)
        }

        @JvmStatic
        fun <V> scheduleNewThreadWithFixedDelay(
            task: () -> V,
            initialDelay: Duration,
            period: Duration
        ): ScheduledRunning<V> {
            return asyncRunner().scheduleWithFixedDelay(task, initialDelay, period)
        }
    }
}

fun <V> scheduleNewThread(task: () -> V, delay: Duration): ScheduledRunning<V> {
    return ScheduledRunner.scheduleNewThread(task, delay)
}

fun <V> scheduleNewThreadAtFixedRate(task: () -> V, initialDelay: Duration, period: Duration): ScheduledRunning<V> {
    return ScheduledRunner.scheduleNewThreadAtFixedRate(task, initialDelay, period)
}

fun <V> scheduleNewThreadWithFixedDelay(task: () -> V, initialDelay: Duration, period: Duration): ScheduledRunning<V> {
    return ScheduledRunner.scheduleNewThreadWithFixedDelay(task, initialDelay, period)
}