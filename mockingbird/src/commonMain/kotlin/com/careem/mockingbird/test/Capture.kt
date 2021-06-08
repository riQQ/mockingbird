/**
 *
 * Copyright Careem, an Uber Technologies Inc. company
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.careem.mockingbird.test

import co.touchlab.stately.isolate.IsolateState
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic

/**
 * A interface for all capture-able class to implement so the captured value can be stored
 */
interface Captureable {
    fun storeCapturedValue(value: Any?)
}

/**
 * Capture any [Slot] which will be using to compare the property inside
 */
fun <T> capture(slot: Slot<T>): CapturedMatcher<T> {
    return CapturedMatcher(slot)
}

/**
 * Capture any [Slot] which will be using to compare the property inside
 */
fun <T> capture(list: CapturedList<T>): CapturedMatcher<T> {
    return CapturedMatcher(list)
}

/**
 * A slot using to fetch the method invocation and compare the property inside invocation arguments
 * Usage example @see [FunctionsTest]
 */
class Slot<T> : Captureable {
    private val _captured: AtomicRef<T?> = atomic(null)
    var captured: T?
        get() {
            return _captured.value
        }
        private set(value) {
            _captured.value = value
        }

    override fun storeCapturedValue(value: Any?) {
        captured = value as T
    }
}

/**
 * A list that using to fetch the method invocation and compare the property inside
 * invocation arguments
 * Usage example @see [FunctionsTest]
 */
class CapturedList<T> : Captureable {
    private val _captured = IsolateState { mutableListOf<T>() }
    val captured: List<T>
        get() {
            return _captured.access { it.toList() }
        }

    override fun storeCapturedValue(value: Any?) {
        _captured.access { it.add(value as T) }
    }
}

/**
 * A placeholder for where using any() as a testing matcher
 */
class AnyMatcher

/**
 * A placeholder to indicate this argument is captured by [Slot] or [CapturedList]
 * Usage example @see [FunctionsTest]
 */
class CapturedMatcher<T> {
    private val captureable: Captureable

    constructor(slot: Slot<T>) {
        captureable = slot
    }

    constructor(list: CapturedList<T>) {
        captureable = list
    }

    fun setCapturedValue(value: Any?) {
        captureable.storeCapturedValue(value)
    }
}