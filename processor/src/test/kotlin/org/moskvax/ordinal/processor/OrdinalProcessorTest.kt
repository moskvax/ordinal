@file:Suppress("unused", "RedundantVisibilityModifier")

package org.moskvax.ordinal.processor

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.intellij.lang.annotations.Language
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals

class OrdinalProcessorTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `single sealed class`() {
        val compilationResult = SourceFile.kotlin(
            "Foo.kt",
            """
            import org.moskvax.ordinal.annotation.Ordinal

            @Ordinal
            sealed class Foo {
                data class Bar1(val bar: String) : Foo()
                data class Bar2(val bar: String) : Foo()
            }
            """.trimIndent()
        ).let { compile(it) }

        assertSourceEquals(
            """
            import kotlin.Int

            public val Foo.Bar1.ordinal: Int
              inline get() = 1

            public val Foo.Bar2.ordinal: Int
              inline get() = 2
            """.trimIndent(),
            compilationResult["FooExt.kt"]
        )
    }

    @Test
    fun `adjacent sealed classes`() {
        val compilationResult = SourceFile.kotlin(
            "Sample.kt",
            """
            import org.moskvax.ordinal.annotation.Ordinal

            @Ordinal
            sealed class Sample {
                data class Cool(val hi: String) : Sample()
                data class Nice(val hi: String) : Sample()
            }

            @Ordinal
            sealed class UhOh {
                data class Cool(val hi: String) : UhOh()
                data class Nice(val hi: String) : UhOh()
            }
            """.trimIndent()
        ).let { compile(it) }

        assertSourceEquals(
            """
            import kotlin.Int

            public val Sample.Cool.ordinal: Int
              inline get() = 1

            public val Sample.Nice.ordinal: Int
              inline get() = 2
            """.trimIndent(),
            compilationResult["SampleExt.kt"]
        )

        assertSourceEquals(
            """
            import kotlin.Int

            public val UhOh.Cool.ordinal: Int
              inline get() = 1

            public val UhOh.Nice.ordinal: Int
              inline get() = 2
            """.trimIndent(),
            compilationResult["UhOhExt.kt"]
        )
    }

    @Test
    fun `nested sealed hierarchy with object`() {
        @Language("kotlin")
        val result = """
            import org.moskvax.ordinal.annotation.Ordinal

            @Ordinal
            sealed interface UhOh {
                data class Cool(val hi: String) : UhOh
                data class Nice(val hi: String) : UhOh
                sealed class NotCool : UhOh {
                    sealed class VeryUncool : NotCool() {
                        data class Woooo(val zzz: Int) : VeryUncool()
                    }

                    data class NotNice(val hmm: String) : NotCool()
                }

                object Weh : UhOh
            }
        """.trimIndent().compile()

        assertSourceEquals(
            """
            import kotlin.Int

            public val UhOh.NotCool.NotNice.ordinal: Int
              inline get() = 4
            """.trimIndent(),
            result["NotCoolExt.kt"]
        )

        assertSourceEquals(
            """
            import kotlin.Int

            public val UhOh.Cool.ordinal: Int
              inline get() = 1

            public val UhOh.Nice.ordinal: Int
              inline get() = 2

            public val UhOh.Weh.ordinal: Int
              inline get() = 5
            """.trimIndent(),
            result["UhOhExt.kt"]
        )

        assertSourceEquals(
            """
            import kotlin.Int

            public val UhOh.NotCool.VeryUncool.Woooo.ordinal: Int
              inline get() = 3
            """.trimIndent(),
            result["VeryUncoolExt.kt"]
        )
    }

    @Test
    fun `nested sealed hierarchy without recursion`() {
        @Language("kotlin")
        val result = """
            import org.moskvax.ordinal.annotation.Ordinal

            @Ordinal(recursive = false)
            sealed interface UhOh { 
                data class Cool(val hi: String) : UhOh
                data class Nice(val hi: String) : UhOh
                sealed class NotCool : UhOh {
                    sealed class VeryUncool : NotCool() {
                        data class Woooo(val zzz: Int) : VeryUncool()
                    }

                    data class NotNice(val hmm: String) : NotCool()
                }

                object Weh : UhOh
            }
        """.trimIndent().compile()

        assertSourceEquals(
            """
            import kotlin.Int

            public val UhOh.Cool.ordinal: Int
              inline get() = 1

            public val UhOh.Nice.ordinal: Int
              inline get() = 2

            public val UhOh.NotCool.ordinal: Int
              inline get() = 3

            public val UhOh.Weh.ordinal: Int
              inline get() = 4
            """.trimIndent(),
            result["UhOhExt.kt"]
        )
    }

    @Test
    fun `nested sealed hierarchy with nested non-overlapping ordinals`() {
        @Language("kotlin")
        val result = """
            import org.moskvax.ordinal.annotation.Ordinal

            @Ordinal(recursive = false)
            sealed interface UhOh {
                data class Cool(val hi: String) : UhOh
                data class Nice(val hi: String) : UhOh
                @Ordinal
                sealed class NotCool : UhOh {
                    sealed class VeryUncool : NotCool() {
                        data class Woooo(val zzz: Int) : VeryUncool()
                    }

                    data class NotNice(val hmm: String) : NotCool()
                }

                object Weh : UhOh
            }
        """.trimIndent().compile()

        assertSourceEquals(
            """
            import kotlin.Int

            public val UhOh.Cool.ordinal: Int
              inline get() = 1

            public val UhOh.Nice.ordinal: Int
              inline get() = 2

            public val UhOh.NotCool.ordinal: Int
              inline get() = 3

            public val UhOh.Weh.ordinal: Int
              inline get() = 4
            """.trimIndent(),
            result["UhOhExt.kt"]
        )

        assertSourceEquals(
            """
            import kotlin.Int

            public val UhOh.NotCool.VeryUncool.Woooo.ordinal: Int
              inline get() = 1
            """.trimIndent(),
            result["VeryUncoolExt.kt"]
        )

        assertSourceEquals(
            """
            import kotlin.Int

            public val UhOh.NotCool.NotNice.ordinal: Int
              inline get() = 2
            """.trimIndent(),
            result["NotCoolExt.kt"]
        )
    }

    @Test
    fun `nested sealed hierarchy with nested overlapping ordinals fails with message`() {
        @Language("kotlin")
        val result = """
            import org.moskvax.ordinal.annotation.Ordinal

            @Ordinal
            sealed interface UhOh {
                data class Cool(val hi: String) : UhOh
                data class Nice(val hi: String) : UhOh
                @Ordinal
                sealed class NotCool : UhOh {
                    sealed class VeryUncool : NotCool() {
                        data class Woooo(val zzz: Int) : VeryUncool()
                    }

                    data class NotNice(val hmm: String) : NotCool()
                }

                object Weh : UhOh
            }
        """.trimIndent().compile()

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertContains(
            result.messages,
            "@Ordinal cannot target sealed type NotCool (line 8) as its count would overlap with the @Ordinal annotated sealed type UhOh (line 4)"
        )
    }

    @Test
    fun `object fails with message`() {
        @Language("kotlin")
        val result = """
            import org.moskvax.ordinal.annotation.Ordinal

            @Ordinal
            object Unreasonable
        """.trimIndent().compile()

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertContains(
            result.messages,
            "@Ordinal must target a sealed type, but Unreasonable (line 4) is object (non-sealed)"
        )
    }

    @Test
    fun `enum fails with message`() {
        @Language("kotlin")
        val result = """
            import org.moskvax.ordinal.annotation.Ordinal

            @Ordinal
            enum class Unreasonable {
                BAD
            }
        """.trimIndent().compile()

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertContains(
            result.messages,
            "@Ordinal must target a sealed type, but Unreasonable (line 4) is enum class (non-sealed)"
        )
    }

    @Test
    fun `private abstract class fails with message`() {
        @Language("kotlin")
        val result = """
            import org.moskvax.ordinal.annotation.Ordinal

            @Ordinal
            private abstract class Unreasonable
        """.trimIndent().compile()

        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertContains(
            result.messages,
            "@Ordinal must target a sealed type, but Unreasonable (line 4) is private abstract class (non-sealed)"
        )
    }

    private fun String.compile() = compile(SourceFile.kotlin("TestFile.kt", this))

    private fun compile(vararg source: SourceFile) = KotlinCompilation().apply {
        sources = source.toList()
        symbolProcessorProviders = listOf(OrdinalProcessorProvider())
        workingDir = temporaryFolder.root
        inheritClassPath = true
        verbose = false
    }.compile()

    private fun assertSourceEquals(@Language("kotlin") expected: String, actual: String) {
        assertEquals(
            expected.trimIndent(),
            // unfortunate hack needed as we cannot enter expected text with tabs rather than spaces
            actual.trimIndent().replace("\t", "    ")
        )
    }

    private operator fun KotlinCompilation.Result.get(fileName: String): String {
        return kspGeneratedSources().find { it.name == fileName }
            ?.readText()
            ?: throw IllegalArgumentException("Could not find file $fileName in ${kspGeneratedSources()}")
    }

    // TODO assert that each test is exhaustive over generated files
    private fun KotlinCompilation.Result.kspGeneratedSources(): List<File> {
        val kspWorkingDir = workingDir.resolve("ksp")
        val kspGeneratedDir = kspWorkingDir.resolve("sources")
        val kotlinGeneratedDir = kspGeneratedDir.resolve("kotlin")
        val javaGeneratedDir = kspGeneratedDir.resolve("java")
        return kotlinGeneratedDir.walk().toList() +
            javaGeneratedDir.walk().toList()
    }

    private val KotlinCompilation.Result.workingDir: File
        get() = checkNotNull(outputDirectory.parentFile)
}
