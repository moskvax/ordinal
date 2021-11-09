package org.moskvax.ordinal.annotation

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class Ordinal(val recursive: Boolean = true)
