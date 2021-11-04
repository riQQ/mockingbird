# MockingBird

<img src="https://github.com/careem/mockingbird/blob/assets/logo.png" width="40%" height="40%" alt="MockingBird Logo"/>

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.careem.mockingbird/mockingbird/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.careem.mockingbird/mockingbird/)
[![Build Status](https://app.bitrise.io/app/0f4e1b30e3e56dfb/status.svg?token=iHecTZF7GpuyTMqiFj618Q&branch=master)](https://app.bitrise.io/build/c0b2c4e103c222bb)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A Koltin multiplatform library that provides an easier way to mock and write unit tests for a multiplatform project

## Disclaimers

This project may contain experimental code and may not be ready for general use. Support and/or releases may be limited.

## Setup

In your multiplatform project include

Koltin DSL:

```kotlin
implementation("com.careem.mockingbird:mockingbird:$mockingBirdVersion")
```

Groovy DSL:

```groovy
implementation "com.careem.mockingbird:mockingbird:$mockingBirdVersion"
```

## Usage

MockingBird doesn't use any annotation processor or reflection. This means that it is a bit more verbose with respect to
libraries like `Mockito` or `Mockk`

### Mock generation plugin (experimental)

The mock generation plugin generates mock boilerplate code for you, the plugin can be used along with manual mocks, it
is currently experimental and has several limitations.

To use this plugin you have to use mockingbird version `2.0.0-beta06` or above, to see examples
checkout `generate-mocks` branch and explore the `samples` project, You can open `samples` is a standalone project.

NOTE: the plugin doesn't discover which interfaces to mock, it's up to you to configure those.

WARNING: If you do not what to use the plugin you can use the old way of manual mock generation,
check [Mocks](https://github.com/careem/mockingbird#mocks) or [Spies](https://github.com/careem/mockingbird#spies)

#### Plugin Setup

To start using the plugin you need to include it in your project `build.gradle.kts` or `build.gradle`

Be sure you have `mavenCentral()` in your `buildscripts` repositories, your project build gradle might look similar the
one below

Kotlin DSL:

```kotlin
buildscript {
    repositories {
        ...
        mavenCentral()
    }
    dependencies {
        ...
        classpath("com.careem.mockingbird:mockingbird-compiler:$mockingBirdVersion")
    }
}
```

Groovy DSL:

```groovy
buildscript {
    repositories {
        ...
        mavenCentral()
    }
    dependencies {
        ...
        classpath "com.careem.mockingbird:mockingbird-compiler:$mockingBirdVersion"
    }
}
```

To generate mocks for a specific module you have first to apply the plugin in this module's build gradle

Kotlin DSL:

```kotlin
apply(plugin = "com.careem.mockingbird")
```

Groovy DSL:

```groovy
apply plugin: "com.careem.mockingbird"
```

And then specify for what interfaces you what to generate mocks for, see the example below

Kotlin DSL:

```kotlin
configure<com.careem.mockingbird.MockingbirdPluginExtension> {
    generateMocksFor = listOf(
        "com.careem.mockingbird.sample.Mock1",
        "com.careem.mockingbird.sample.MockWithExternalDependencies",
        "com.careem.mockingbird.common.sample.ExternalContract"
    )
}
```

Groovy DSL

```groovy
mockingBird {
    generateMocksFor = [
            'com.careem.mockingbird.sample.Mock1',
            'com.careem.mockingbird.sample.MockWithExternalDependencies',
            'com.careem.mockingbird.common.sample.ExternalContract'
    ]
}
```

#### Plugin Usage

To generate mocks you can simply run `./gradlew generateMocks` or simply run `./gradlew build`, for faster development
loop using `generatedMocks` is recommended

#### Plugin Limitations

* The plugin can only be used in modules containing a `jvm` target
* The plugin can generate mocks only, no support for spies yet
* Only interfaces can be mocked
* Only interfaces that have generic types in their definitions can be mocked
* Only interfaces without `lambdas` can be mocked
* Only interfaces without `suspend` functions can be mocked
* Only interfaces without `inline` functions can be mocked
* Only interfaces without `reified` functions can be mocked

### Mocking

When your mocks are ready you can write your tests and specify the behavior you want when a method on your mock is
called, and verify the method is called your mock.

To do that you will use:

1. `every`
2. `everyAnswer`
3. `verify`

#### every

`every` allows you to specify the value you want to return for a specific invocation

If you want to return 3 when `myDependencyMock.method3(4, 5)` is called, your `every` will look like

```kotlin
testMock.every(
    methodName = MyDependencyMock.Method.method3,
    arguments = mapOf(MyDependencyMock.Arg.value1 to 4, MyDependencyMock.Arg.value2 to 5)
) { 3 }
```

#### everyAnswers

If you wish to perform specific logic when you mock is called you can use `everyAnswer`, here you can specify the
behavior you want for your mock. A typical use case is when you want to invoke a callback that was passed as parameter
to the mocked function.

The code for a callback invocation will look like

```kotlin
myMock.everyAnswers(
    methodName = MyMock.Method.run,
    arguments = mapOf(
        MyMock.Arg.callback to callback,
    )
) {
    val callback = it.arguments[MyMock.Arg.callback] as () -> Unit
    callback.invoke()
}
```

#### Verify

After the invocation of your mock is defined, you need to verify it is invoked to make your unit test valid. For example
if you want to verify `myDependencyMock.method3(4, 5)` is invoked, you should do something like:

```kotlin
testMock.verify(
    exactly = 1,
    methodName = MyDependencyMock.Method.method3,
    arguments = mapOf(MyDependencyMock.Arg.value1 to 4, MyDependencyMock.Arg.value2 to 5),
    timeoutMillis = 5000L
)
```

Note: `exactly` is how many times you want to verify invocation of your mock is invoked, by default it will be 1, so no
need to set it up if you want to verify exactly 1 time invocation.

Note: when `timeoutMillis` is set with a value greater than 0 the test condition will be evaluated multiple times up
to `timeoutMillis`. If the condition is not satisfied within the given timeout the verify will fail.

### Matching

When your mocks are ready you can write your tests and specify the behavior you want when a method on your mock is
called, and verify the method is called your mock. Sometimes besides mocking, we want to verify the equality of argument
that passed to the mock's invocation, sometimes we don't care about the argument value or sometimes we want to strongly
verify that the invocation is **not** invoked no matter what arguments is passed. In all these cases, we need matching
arguments.

To do matching, you will use:

1. `any()`
2. `slot()` and `capture`

#### Any

As it looks like, `any()` matcher will give you ability to ignore the compare of argument when mocking invocation or
verify it. For example, if you want to return 3 when `myDependencyMock.method3` is called no matter what two arguments
is passed in, your `every` will look like:

```kotlin
testMock.every(
    methodName = MyDependencyMock.Method.method3,
    arguments = mapOf(MyDependencyMock.Arg.value1 to any(), MyDependencyMock.Arg.value2 to any())
) { 3 }
```

By doing this, both `myDependencyMock.method3(1,2)` or `myDependencyMock.method3(3,4)` will all returns 3. Similar to
this `every`, you can easily verify `myDependencyMock.method3` is invoked and ignore the argument comparing by:

```kotlin
testMock.verify(
    exactly = 1,
    methodName = MyDependencyMock.Method.method3,
    arguments = mapOf(MyDependencyMock.Arg.value1 to any(), MyDependencyMock.Arg.value2 to any())
)
```

A normal use case on verify with `any()` matcher is verify invocation is invoked `exactly = 0` with
`any()` arguments which means it is never invoked completely.

#### Arguments Capturing

Another use case for matching is: for example you want to verify `myDependencyMock.method4(object1)`
is invoked, but the reference of object1 is not mocked or initiated inside the test case, In this case, an easy way to
verify, or say matching arguments, is create an object using `slot()` or `capturedList()` and then
`capture` this object when verify the invocation, something like:

```kotlin
val objectSlot = slot<Object>()
val objectCapturedList = capturedList<Object>()

testMock.every(
    methodName = MyDependencyMock.Method.method1,
    arguments = mapOf(MyDependencyMock.Arg.str to TEST_STRING)
) {}

// capturing by slot
testMock.verify(
    methodName = MyDependencyMock.Method.method4,
    arguments = mapOf(MyDependencyMock.Arg.object1 to capture(objectSlot))
)
assertEquals(expectedProperty, objectSlot.captured.property)

// capturing by capturedList
testMock.verify(
    methodName = MyDependencyMock.Method.method4,
    arguments = mapOf(MyDependencyMock.Arg.object1 to capture(objectCapturedList))
)
assertEquals(2, objectCapturedList.captured.size)
assertEquals(expectedProperty, capturedList.captured[0])
assertEquals(expectedProperty, capturedList.captured[1])
```

For capturing slot, a common use case for this capturing is when a new instance is created inside testing method and you
want to compare some properties of the captured object initialized correctly. For capturing list, a common use case is
invocation is invoked multiple times and you want to verify the arguments of each separately.

### Test Mode

Changing the test mode will allow you to mock objects for different test scenarios, for example integration tests or
unit tests.

By default mockingbirds handles mocks in a way that they can be shared across multiple threads, sometimes this will
introduce some limitations when you want to test classes that cannot be shared across threads and that for this reason
they might have something like `ensureNeverFrozen` in their constructor.

For those cases you might want to use the `LOCAL_THREAD` test mode where the arguments you pass to the mock do not need
to be frozen because you know that your class is a single threaded class.

Example of `LOCAL_THREAD` mode:

```kotlin
@Test
fun testLocalModeDoNotFreezeClass() = runWithTestMode(TestMode.LOCAL_THREAD) {
        val myDependencyMock = MyDependencyMock()
        myDependencyMock.everyAnswers(
            methodName = MyDependencyMock.Method.method6,
            arguments = mapOf(
                MyDependencyMock.Arg.callback to any()
            )
        ) { it.getArgument<() -> Unit>(MyDependencyMock.Arg.callback).invoke() }

        val instance = LocalThreadAccessibleClass(myDependencyMock)
        instance.execute()

        myDependencyMock.verify(
            methodName = MyDependencyMock.Method.method6,
            arguments = mapOf(Mocks.MyDependencySpy.Arg.callback to any())
        )
        assertEquals(1, instance.counter)
    }
```

### Manual Mocks and Spies generation

In this section it is explained how to generate mocks and spy manually, we suggest to use this approach only when you
face issues with the Mock generator plugin.

#### Mocks

The first step you need to do is create a mock class for the object you want to mock, you need a mock for each
dependency type you want to mock

The library provides 2 functions to help you write your mocks.

1. `mock` this function allows you to mock non-methods with return types other than Unit
1. `mockUnit` this function allows you to mock Unit methods

These helpers enable you to map your mock invocations to MockingBird environment.

Your mock class must implement `Mock`, in addition to extending the actual class or implementing an interface

See below for an example on how to create a mock :

```kotlin
interface MyDependency {
    fun method1(str: String)
    fun method2(str: String, value: Int)
    fun method3(value1: Int, value2: Int): Int
}

class MyDependencyMock : MyDependency, Mock {
    object Method {
        const val method1 = "method1"
        const val method2 = "method2"
        const val method3 = "method3"
        const val method4 = "method4"
    }

    object Arg {
        const val str = "str"
        const val value = "value"
        const val value1 = "value1"
        const val value2 = "value2"
        const val object1 = "object1"
    }

    override fun method1(str: String) = mockUnit(
        methodName = Method.method1,
        arguments = mapOf(
            Arg.str to str
        )
    )

    override fun method2(str: String, value: Int) = mockUnit(
        methodName = Method.method2,
        arguments = mapOf(
            Arg.str to str,
            Arg.value to value
        )
    )

    override fun method3(value1: Int, value2: Int): Int = mock(
        methodName = Method.method3,
        arguments = mapOf(
            Arg.value1 to value1,
            Arg.value2 to value2
        )
    )

    override fun method4(object1: Object): Int = mock(
        methodName = Method.method4,
        arguments = mapOf(
            Arg.object1 to object1
        )
    )
}    
```

#### Spies

When you need a combination of real behavior and mocked behavior you can use `spy` with spy you wrap wrap a real
implementation. Doing so Mocking Bird will record the interactions with the spied object.

To mock a specific invocation you can use the spied object like a normal mock, see sections below for further details.

A Spy sample object is reported here

```kotlin
interface MyDependency {
    fun method1(str: String)
    fun method2(str: String, value: Int)
    fun method3(value1: Int, value2: Int): Int
    fun method4(): Int
}

class MyDependencySpy(private val delegate: MyDependency) : MyDependency, Spy {

    object Method {
        const val method1 = "method1"
        const val method2 = "method2"
        const val method3 = "method3"
        const val method4 = "method4"
    }

    object Arg {
        const val str = "str"
        const val value = "value"
        const val value1 = "value1"
        const val value2 = "value2"
    }

    override fun method1(str: String) = spy(
        methodName = Method.method1,
        arguments = mapOf(
            Arg.str to str
        ),
        delegate = { delegate.method1(str) }
    )

    override fun method2(str: String, value: Int) = spy(
        methodName = Method.method2,
        arguments = mapOf(
            Arg.str to str,
            Arg.value to value
        ),
        delegate = { delegate.method2(str, value) }
    )

    override fun method3(value1: Int, value2: Int): Int = spy(
        methodName = Method.method3,
        arguments = mapOf(
            Arg.value1 to value1,
            Arg.value2 to value2
        ),
        delegate = { delegate.method3(value1, value2) }
    )

    override fun method4(): Int = spy(
        methodName = Method.method4,
        delegate = { delegate.method4() }
    )
}

class MyDependencyImpl : MyDependency {
    private var value: AtomicInt = atomic(0)
    override fun method1(str: String) {

    }

    override fun method2(str: String, value: Int) {

    }

    override fun method3(value1: Int, value2: Int): Int {
        value.value = value1 + value2
        return value.value
    }

    override fun method4(): Int {
        return value.value
    }
}
```

## License

    Copyright Careem, an Uber Technologies Inc. company

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.