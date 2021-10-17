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
public interface Captureable {
    public fun storeCapturedValue(value: Any?)
}

/**
 * Capture any [Slot] which will be using to compare the property inside
 */
public fun <T> capture(slot: Slot<T>): CapturedMatcher<T> {
    return CapturedMatcher(slot)
}

/**
 * Capture any [Slot] which will be using to compare the property inside
 */
public fun <T> capture(list: CapturedList<T>): CapturedMatcher<T> {
    return CapturedMatcher(list)
}

/**
 * A slot using to fetch the method invocation and compare the property inside invocation arguments
 * Usage example @see [FunctionsTest]
 */

public class Slot<T> : Captureable {

    private val genericSlot : GenericSlot<T> = initializeSlot()

    public val captured: T?
        get() {
            return genericSlot.value
        }

    override fun storeCapturedValue(value: Any?) {
        genericSlot.storeCapturedValue(value)
    }
}

private fun <T> initializeSlot(): GenericSlot<T> {
    return when (MockingBird.mode) {
        TestMode.MULTI_THREAD -> ThreadSafeSlot()
        TestMode.LOCAL_THREAD -> LocalThreadSlot()
    }
}

public interface GenericSlot<T> : Captureable {
    public val value: T?
}

private class ThreadSafeSlot<T> : GenericSlot<T> {

    private val _captured: AtomicRef<T?> = atomic(null)

    @Suppress("UNCHECKED_CAST")
    override fun storeCapturedValue(value: Any?) {
        _captured.value = value as T
    }

    override val value: T?
        get() = _captured.value
}


private class LocalThreadSlot<T> : GenericSlot<T> {

    private var _captured: T? = null

    public override var value: T?
        get() {
            return _captured
        }
        private set(value) {
            _captured = value
        }

    @Suppress("UNCHECKED_CAST")
    override fun storeCapturedValue(value: Any?) {
        this.value = value as T
    }
}

/**
 * A list that using to fetch the method invocation and compare the property inside
 * invocation arguments
 * Usage example @see [FunctionsTest]
 */
public class CapturedList<T> : Captureable {

    private val genericCaptureList = initializeGenericCaptureList<T>()
    public val captured: List<T>
        get() {
            return genericCaptureList.value
        }

    @Suppress("UNCHECKED_CAST")
    override fun storeCapturedValue(value: Any?) {
        genericCaptureList.storeCapturedValue(value)
    }
}

private interface GenericCapturedList<T> : Captureable {
    val value: List<T>
}

private class MultiThreadCapturedList<T> : GenericCapturedList<T> {

    private val _captured = IsolateState { mutableListOf<T>() }

    override val value: List<T>
        get() {
            return _captured.access { it.toList() }
        }

    @Suppress("UNCHECKED_CAST")
    override fun storeCapturedValue(value: Any?) {
        _captured.access { it.add(value as T) }
    }
}

private fun <T> initializeGenericCaptureList(): GenericCapturedList<T> {
    return when (MockingBird.mode) {
        TestMode.MULTI_THREAD -> MultiThreadCapturedList()
        TestMode.LOCAL_THREAD -> LocalThreadCapturedList()
    }
}

private class LocalThreadCapturedList<T> : GenericCapturedList<T> {

    private val _captured = mutableListOf<T>()

    override val value: List<T>
        get() {
            return _captured.toList()
        }

    @Suppress("UNCHECKED_CAST")
    override fun storeCapturedValue(value: Any?) {
        this._captured.add(value as T)
    }
}

/**
 * A placeholder for where using any() as a testing matcher
 */
public class AnyMatcher

/**
 * A placeholder to indicate this argument is captured by [Slot] or [CapturedList]
 * Usage example @see [FunctionsTest]
 */
public class CapturedMatcher<T> {
    private val captureable: Captureable

    public constructor(slot: Slot<T>) {
        captureable = slot
    }

    public constructor(list: CapturedList<T>) {
        captureable = list
    }

    internal fun setCapturedValue(value: Any?) {
        captureable.storeCapturedValue(value)
    }
}