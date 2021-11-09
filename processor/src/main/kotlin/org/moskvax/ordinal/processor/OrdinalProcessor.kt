package org.moskvax.ordinal.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.FileLocation
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.ksp.KotlinPoetKspPreview
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import org.moskvax.ordinal.annotation.Ordinal
import java.io.IOException

@OptIn(KspExperimental::class, KotlinPoetKspPreview::class)
class OrdinalProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getSymbolsWithAnnotation(Ordinal::class.qualifiedName!!)
            .forEach { symbol ->
                with(symbol.getAnnotationsByType(Ordinal::class).first()) {
                    symbol.accept(Visitor(recursive), Unit)
                }
            }

        return emptyList()
    }

    private inner class Visitor(
        private val recursive: Boolean
    ) : KSVisitorVoid() {
        private var ordinalCounter: Int = 1
        private lateinit var sealedContainingFile: KSFile

        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            fun logError(message: String) = logger.error(message, classDeclaration)

            if (!classDeclaration.isSealed) {
                with(classDeclaration) {
                    val modifiers = modifiers.map { it.name.lowercase() }
                        .plus(classKind.type.substringAfter('_'))
                        .joinToString(" ")
                    logError("@Ordinal must target a sealed type, but $declarationInfo is $modifiers (non-sealed)")
                }
            }

            if (!::sealedContainingFile.isInitialized) {
                sealedContainingFile = with(classDeclaration) {
                    containingFile ?: superTypes.firstNotNullOf { it.containingFile }
                }
            }

            val builder = FileSpec.builder(
                classDeclaration.packageName.asString(),
                "${classDeclaration.simpleName.asString()}Ext"
            ).apply {
                classDeclaration.getSealedSubclasses().forEach { sealedSubclass ->
                    if (sealedSubclass.isSealed) {
                        if (recursive) visitClassDeclaration(sealedSubclass, Unit)
                    } else {
                        val ordinalGetter = FunSpec.getterBuilder()
                            .addModifiers(KModifier.INLINE)
                            .addStatement("return %L", ordinalCounter++)
                            .build()

                        val ordinalProperty = PropertySpec.builder("ordinal", Int::class)
                            .receiver(sealedSubclass.asStarProjectedType().toTypeName())
                            .getter(ordinalGetter)
                            .build()

                        addProperty(ordinalProperty)
                    }
                }
            }

            try {
                builder.build().writeTo(codeGenerator, false, listOf(sealedContainingFile))
            } catch (e: IOException) {
                classDeclaration.declarationInfo?.let { targetDeclarationInfo ->
                    classDeclaration.getAllSuperTypes()
                        .map { it.declaration }
                        .first { it.getAnnotationsByType(Ordinal::class).any() }
                        .let { supertype ->
                            logError(
                                "@Ordinal cannot target sealed type $targetDeclarationInfo " +
                                    "as its count would overlap with the @Ordinal " +
                                    "annotated sealed type ${supertype.declarationInfo}"
                            )
                        }
                }
            }
        }
    }
}

private val KSClassDeclaration.isSealed inline get() = Modifier.SEALED in modifiers

private val KSDeclaration.declarationInfo: DeclarationInfo?
    get() =
        (location as? FileLocation)?.lineNumber?.let {
            DeclarationInfo(simpleName.asString(), it)
        }

private data class DeclarationInfo(
    val name: String,
    val lineNumber: Int
) {
    override fun toString() = "$name (line $lineNumber)"
}
