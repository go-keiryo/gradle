/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.collections

import org.gradle.api.internal.provider.AbstractProvider
import org.gradle.api.internal.provider.ProviderInternal
import spock.lang.Specification

class IterationOrderRetainingSetElementSourceTest extends Specification {
    IterationOrderRetainingSetElementSource<CharSequence> source = new IterationOrderRetainingSetElementSource<>()

    def "can add a realized element"() {
        when:
        source.add("foo")

        then:
        source.size() == 1
        source.contains("foo")
    }

    def "can add a provider"() {
        when:
        source.addPending(provider("foo"))

        then:
        source.size() == 1
        source.contains("foo")
    }

    def "iterates elements in the order they were added"() {
        when:
        source.addPending(provider("foo"))
        source.add("bar")
        source.add("baz")
        source.addPending(provider("fizz"))

        then:
        source.iteratorNoFlush().collect() == ["bar", "baz"]

        and:
        source.iterator().collect() == ["foo", "bar", "baz", "fizz"]
    }

    def "once realized, provided values appear like realized values"() {
        when:
        source.addPending(provider("foo"))
        source.add("bar")
        source.add("baz")
        source.addPending(provider("fizz"))

        then:
        source.iteratorNoFlush().collect() == ["bar", "baz"]

        when:
        source.realizePending()

        then:
        source.iteratorNoFlush().collect() == ["foo", "bar", "baz", "fizz"]
    }

    def "can add only providers"() {
        when:
        source.addPending(provider("foo"))
        source.addPending(provider("bar"))
        source.addPending(provider("baz"))
        source.addPending(provider("fizz"))

        then:
        source.iteratorNoFlush().collect() == []

        and:
        source.iterator().collect() == ["foo", "bar", "baz", "fizz"]
    }

    def "can add only realized providers"() {
        when:
        source.add("foo")
        source.add("bar")
        source.add("baz")
        source.add("fizz")

        then:
        source.iteratorNoFlush().collect() == ["foo", "bar", "baz", "fizz"]

        and:
        source.iterator().collect() == ["foo", "bar", "baz", "fizz"]
    }

    def "can add the same element multiple times"() {
        when:
        3.times { source.add("foo") }
        3.times { source.addPending(provider("bar"))}

        then:
        source.iteratorNoFlush().collect() == ["foo"]

        and:
        source.iterator().collect() == ["foo", "bar"]
    }

    def "can remove a realized element"() {
        given:
        source.add("foo")
        source.addPending(provider("bar"))
        source.add("baz")

        expect:
        source.remove("foo")

        and:
        source.size() == 2
        source.iterator().collect() == ["bar", "baz"]

        and:
        !source.remove("foo")
    }

    def "can remove a provider"() {
        given:
        def bar = provider("bar")
        source.add("foo")
        source.addPending(bar)
        source.add("baz")

        expect:
        source.removePending(bar)

        and:
        source.size() == 2
        source.iterator().collect() == ["foo", "baz"]
    }

    def "can realize a filtered set of providers and order is retained"() {
        when:
        source.addPending(provider("foo"))
        source.addPending(provider(new StringBuffer("bar")))
        source.addPending(provider(new StringBuffer("baz")))
        source.addPending(provider("fizz"))

        then:
        source.iteratorNoFlush().collect() == []

        when:
        source.realizePending(StringBuffer.class)

        then:
        source.iteratorNoFlush().collect { it.toString() } == ["bar", "baz"]

        and:
        source.iterator().collect { it.toString() } == ["foo", "bar", "baz", "fizz"]
    }

    def "an element added as both a provider and a realized value is not duplicated"() {
        when:
        source.add("foo")
        source.addPending(provider("foo"))

        then:
        source.iterator().collect() == ["foo"]
    }

    ProviderInternal<? extends String> provider(String value) {
        return new TypedProvider<String>(String, value)
    }

    ProviderInternal<? extends StringBuffer> provider(StringBuffer value) {
        return new TypedProvider<StringBuffer>(StringBuffer, value)
    }

    private static class TypedProvider<T> extends AbstractProvider<T> {
        final Class<T> type
        final T value

        TypedProvider(Class<T> type, T value) {
            this.type = type
            this.value = value
        }

        @Override
        Class<T> getType() {
            return type
        }

        @Override
        T getOrNull() {
            return value
        }
    }
}
