/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.codehaus.groovy.classgen.asm.sc

import groovy.transform.stc.ArraysAndCollectionsSTCTest

/**
 * Unit tests for static type checking : miscellaneous tests.
 */
class ArraysAndCollectionsStaticCompileTest extends ArraysAndCollectionsSTCTest implements StaticCompilationTestSupport {

    void testListStarWithMethodReturningVoid() {
        assertScript '''
            class A { void m() {} }
            List<A> elems = [new A(), new A(), new A()]
            List result = elems*.m()
            assert result == [null,null,null]
        '''
    }

    void testListStarWithMethodWithNullInList() {
        assertScript '''
            List<String> elems = ['a',(String)null,'C']
            List<String> result = elems*.toUpperCase()
            assert result == ['A',null,'C']
        '''
    }

    void testShouldNotThrowVerifyError() {
        assertScript '''
            def al = new ArrayList<Double>()
            al.add(2.0d)
            assert al.get(0) + 1 == 3.0d
        '''
    }

    // GROOVY-5654
    void testShouldNotThrowForbiddenAccessWithMapProperty() {
        assertScript '''
            Map<String, Integer> m = ['abcd': 1234]
            assert m['abcd'] == 1234
            assert m.abcd == 1234
        '''
    }

    // GROOVY-5988
    void testMapArraySetPropertyAssignment() {
        assertScript '''import static java.lang.reflect.Modifier.isPrivate
            Map<String, Object> props(Object o) {
                Map<String, Object> props = [:]
                for (property in o.metaClass.properties) {
                    if (!isPrivate(property.modifiers)) {
                        props[property.name] = 'TEST'
                        //props.put(property, 'TEST')
                    }
                }
                props
            }
            def map = props('SOME RANDOM STRING')
            assert map['class'] == 'TEST'
            assert map['bytes'] == 'TEST'
        '''
    }

    // GROOVY-7656
    void testSpreadSafeMethodCallsOnListLiteralShouldNotCreateListTwice() {
        assertScript '''
            class Foo {
                static void test() {
                    def list = [1, 2]
                    def lengths = [list << 3]*.size()
                    assert lengths == [3]
                    assert list == [1, 2, 3]
                }
            }
            Foo.test()
        '''
        assert astTrees['Foo'][1].count('ScriptBytecodeAdapter.createList') == 4
    }

    // GROOVY-7442
    void testSpreadDotOperatorWithinAssert() {
        assertScript '''
            def myMethod(String a, String b) {
                assert [a, b]*.size() == [5, 5]
            }

            myMethod('hello', 'world')
        '''
    }

    // GROOVY-7688
    void testSpreadSafeMethodCallReceiversWithSideEffectsShouldNotBeVisitedTwice() {
        assertScript '''
            class Foo {
                static void test() {
                    def list = ['a', 'b']
                    def lengths = list.toList()*.length()
                    assert lengths == [1, 1]
                }
            }
            Foo.test()
        '''
        assert astTrees['Foo'][1].count('DefaultGroovyMethods.toList') == 1
    }

    // GROOVY-10029
    void testCollectionToArrayAssignmentSC() {
        assertScript '''
            class C {
                static List<String> m() {
                    return ['foo']
                }
                static main(args) {
                    String[] strings = m()
                    assert strings.length == 1
                    assert strings[0] == 'foo'
                }
            }
        '''
        String out = astTrees['C'][1]
        out = out.substring(out.indexOf('main([Ljava/lang/String;)'))
        assert out.contains('INVOKEINTERFACE java/util/List.toArray')
        assert !out.contains('INVOKEDYNAMIC cast(Ljava/util/List;)') : 'dynamic cast should have been replaced by direct method call'
    }

    void testCollectionToObjectAssignmentSC() {
        assertScript '''
            def collectionOfI = [1,2,3]

            def obj
            obj = new String[0]
            obj = new Number[1]
            obj = collectionOfI

            assert obj instanceof List
        '''
        String out = astTrees.values()[0][1]
        assert !out.contains('INVOKEINTERFACE java/util/List.toArray')
    }
}
