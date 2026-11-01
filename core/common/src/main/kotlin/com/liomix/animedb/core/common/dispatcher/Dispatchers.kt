package com.liomix.animedb.core.common.dispatcher

import javax.inject.Qualifier

/** 调度器限定符：`@Dispatcher(AnimeDbDispatchers.IO)`。 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Dispatcher(val dispatcher: AnimeDbDispatchers)

enum class AnimeDbDispatchers { Default, IO }
