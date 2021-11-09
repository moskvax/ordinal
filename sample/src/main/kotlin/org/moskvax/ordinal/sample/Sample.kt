package org.moskvax.ordinal.sample

import org.moskvax.ordinal.annotation.Ordinal
import org.moskvax.ordinal.sample.UhOh.Cool
import org.moskvax.ordinal.sample.UhOh.Nice
import org.moskvax.ordinal.sample.UhOh.NotCool.NotNice
import org.moskvax.ordinal.sample.UhOh.NotCool.VeryUncool.Woooo
import org.moskvax.ordinal.sample.UhOh.Weh

@Ordinal
sealed interface UhOh {
    data class Cool(val hi: String) : UhOh
    data class Nice(val hi: String) : UhOh
    sealed class NotCool : UhOh {
        sealed class VeryUncool : NotCool() {
            data class Woooo(val zzz: Int) : VeryUncool()
        }

        class NotNice : NotCool()
    }

    object Weh : UhOh
}

fun main() {
    val cool = Cool("heh")
    val nice = Nice("heh")
    val notNice = NotNice()
    val woooo = Woooo(2)

    println("cool should be 1, and it is: ${cool.ordinal}")
    println("nice should be 2, and it is: ${nice.ordinal}")
    println("woooo should be 3, and it is: ${woooo.ordinal}")
    println("notNice should be 4, and it is: ${notNice.ordinal}")
    println("Weh should be 5, and it is: ${Weh.ordinal}")
}
