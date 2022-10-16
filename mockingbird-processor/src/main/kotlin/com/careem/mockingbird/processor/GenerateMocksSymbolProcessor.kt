/*
 * Copyright Careem, an Uber Technologies Inc. company
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.careem.mockingbird.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo

class GenerateMocksSymbolProcessor(
    val codeGenerator: CodeGenerator,
    val logger: KSPLogger
) : SymbolProcessor {

    private lateinit var mockGenerator: MockGenerator
    private lateinit var spyGenerator: SpyGenerator
    private lateinit var functionsMiner: FunctionsMiner

    override fun process(resolver: Resolver): List<KSAnnotated> {
        functionsMiner = FunctionsMiner()
        mockGenerator = MockGenerator(resolver, logger, functionsMiner)
        spyGenerator = SpyGenerator(resolver, logger, functionsMiner)

        resolver
            .getSymbolsWithAnnotation(MOCK_ANNOTATION)
            .generateCode(mockGenerator)

        resolver
            .getSymbolsWithAnnotation(SPY_ANNOTATION)
            .generateCode(spyGenerator)
        return emptyList()
    }

    private fun Sequence<KSAnnotated>.generateCode(generator: Generator) {
        this
            .map {
                if (it !is KSPropertyDeclaration) error("$it is not a property declaration but is annotated with @${generator.resolveSupertype()}, not supported")
                logger.info("Process: ${it.type}")
                it to it.type
            }
            .distinctBy { (originalProperty, type) ->
                originalProperty to type.resolve().toClassName().canonicalName
            }
            .forEach { (_, type) ->
                logger.info(type.resolve().toClassName().canonicalName)
                generator.createClass(type).apply {
                    try {
                        writeTo(
                            codeGenerator = codeGenerator,
                            // TODO remove this once ksp will be able to see `containingFiles` of class declared in common main ( required for incremental processing )
                            // TODO Dependencies.ALL_FILES  is basically disabling incremental processing
                            dependencies = Dependencies.ALL_FILES
                        )
                    } catch (e: FileAlreadyExistsException) {
                        logger.info("${generator.resolveSupertype()} already generated by parsing another test")
                    }
                }
            }
    }

    companion object {
        const val MOCK_ANNOTATION = "com.careem.mockingbird.test.annotations.Mock"
        const val SPY_ANNOTATION = "com.careem.mockingbird.test.annotations.Spy"
    }
}