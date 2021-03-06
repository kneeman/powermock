/*
 * Copyright 2011 the original author or authors.
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
package org.powermock.core.classloader;

import javassist.ByteArrayClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import junit.framework.Assert;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.powermock.core.classloader.annotations.UseClassPathAdjuster;
import org.powermock.core.transformers.MockTransformer;
import org.powermock.core.transformers.impl.ClassMockTransformer;
import org.powermock.reflect.Whitebox;

import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.powermock.core.classloader.MockClassLoader.MODIFY_ALL_CLASSES;

public class MockClassLoaderTest {
    @Test
    public void autoboxingWorks() throws Exception {
        String name = this.getClass().getPackage().getName() + ".HardToTransform";
        final MockClassLoader mockClassLoader = new MockClassLoader(new String[]{name});
        List<MockTransformer> list = new LinkedList<MockTransformer>();
        list.add(new ClassMockTransformer());
        mockClassLoader.setMockTransformerChain(list);
        Class<?> c = mockClassLoader.loadClass(name);

        Object object = c.newInstance();
        Whitebox.invokeMethod(object, "run");

        assertThat(5).isEqualTo(Whitebox.invokeMethod(object, "testInt"));
        assertThat(5L).isEqualTo(Whitebox.invokeMethod(object, "testLong"));
        assertThat(5f).isEqualTo(Whitebox.invokeMethod(object, "testFloat"));
        assertThat(5.0).isEqualTo(Whitebox.invokeMethod(object, "testDouble"));
        assertThat(new Short("5")).isEqualTo(Whitebox.invokeMethod(object, "testShort"));
        assertThat(new Byte("5")).isEqualTo(Whitebox.invokeMethod(object, "testByte"));
        assertThat(true).isEqualTo(Whitebox.invokeMethod(object, "testBoolean"));
        assertThat('5').isEqualTo(Whitebox.invokeMethod(object, "testChar"));
        assertThat("5").isEqualTo(Whitebox.invokeMethod(object, "testString"));
    }

    @Test
    public void callFindClassWorks() throws Exception {
        MyClassloader myClassloader = new MyClassloader(new String[]{"org.mytest.myclass"});
        assertEquals(String.class, myClassloader.findClassPublic("java.lang.String"));
    }

    @Test
    public void prepareForTestHasPrecedenceOverPowerMockIgnoreAnnotatedPackages() throws Exception {
        MockClassLoader mockClassLoader = new MockClassLoader(new String[]{"org.mytest.myclass"});
        Whitebox.setInternalState(mockClassLoader, new String[]{"*mytest*"}, DeferSupportingClassLoader.class);
        assertTrue(Whitebox.<Boolean>invokeMethod(mockClassLoader, "shouldModify", "org.mytest.myclass"));
    }

    @Test
    public void powerMockIgnoreAnnotatedPackagesAreIgnored() throws Exception {
        MockClassLoader mockClassLoader = new MockClassLoader(new String[]{"org.ikk.Jux"});
        Whitebox.setInternalState(mockClassLoader, new String[]{"*mytest*"}, DeferSupportingClassLoader.class);
        assertFalse(Whitebox.<Boolean>invokeMethod(mockClassLoader, "shouldModify", "org.mytest.myclass"));
    }

    @Test
    public void powerMockIgnoreAnnotatedPackagesHavePrecedenceOverPrepareEverythingForTest() throws Exception {
        MockClassLoader mockClassLoader = new MockClassLoader(new String[]{MODIFY_ALL_CLASSES});
        Whitebox.setInternalState(mockClassLoader, new String[]{"*mytest*"}, DeferSupportingClassLoader.class);
        assertFalse(Whitebox.<Boolean>invokeMethod(mockClassLoader, "shouldModify", "org.mytest.myclass"));
    }

    @Test
    public void prepareForTestPackagesArePrepared() throws Exception {
        MockClassLoader mockClassLoader = new MockClassLoader(new String[]{"*mytest*"});
        assertTrue(Whitebox.<Boolean>invokeMethod(mockClassLoader, "shouldModify", "org.mytest.myclass"));
    }

    @Test
    public void shouldAddIgnorePackagesToDefer() throws Exception {
        MockClassLoader mockClassLoader = new MockClassLoader(new String[0]);
        mockClassLoader.addIgnorePackage("test*");
        String[] deferPackages = Whitebox.<String[]>getInternalState(mockClassLoader, "deferPackages");
        assertTrue(deferPackages.length > 1);
        assertEquals("test*", deferPackages[deferPackages.length - 1]);
    }

    @Test
    public void canFindResource() throws Exception {
        final MockClassLoader mockClassLoader = new MockClassLoader(new String[0]);
        List<MockTransformer> list = new LinkedList<MockTransformer>();
        list.add(new ClassMockTransformer());
        mockClassLoader.setMockTransformerChain(list);

        // Force a ClassLoader that can find 'foo/bar/baz/test.txt' into
        // mockClassLoader.deferTo.
        ResourcePrefixClassLoader resourcePrefixClassLoader = new ResourcePrefixClassLoader(
                                                                                                   getClass().getClassLoader(), "org/powermock/core/classloader/");
        mockClassLoader.deferTo = resourcePrefixClassLoader;

        // MockClassLoader will only be able to find 'foo/bar/baz/test.txt' if it
        // properly defers the resource lookup to its deferTo ClassLoader.
        URL resource = mockClassLoader.getResource("foo/bar/baz/test.txt");
        Assert.assertNotNull(resource);
        Assert.assertTrue(resource.getPath().endsWith("test.txt"));
    }

    @Test
    public void canFindResources() throws Exception {
        final MockClassLoader mockClassLoader = new MockClassLoader(new String[0]);
        List<MockTransformer> list = new LinkedList<MockTransformer>();
        list.add(new ClassMockTransformer());
        mockClassLoader.setMockTransformerChain(list);

        // Force a ClassLoader that can find 'foo/bar/baz/test.txt' into
        // mockClassLoader.deferTo.
        ResourcePrefixClassLoader resourcePrefixClassLoader = new ResourcePrefixClassLoader(
                                                                                                   getClass().getClassLoader(), "org/powermock/core/classloader/");
        mockClassLoader.deferTo = resourcePrefixClassLoader;

        // MockClassLoader will only be able to find 'foo/bar/baz/test.txt' if it
        // properly defers the resources lookup to its deferTo ClassLoader.
        Enumeration<URL> resources = mockClassLoader.getResources("foo/bar/baz/test.txt");
        Assert.assertNotNull(resources);
        Assert.assertTrue(resources.hasMoreElements());
        URL resource = resources.nextElement();
        Assert.assertTrue(resource.getPath().endsWith("test.txt"));
        Assert.assertFalse(resources.hasMoreElements());
    }

    @Test
    public void resourcesNotDoubled() throws Exception {
        final MockClassLoader mockClassLoader = new MockClassLoader(new String[0]);
        List<MockTransformer> list = new LinkedList<MockTransformer>();
        list.add(new ClassMockTransformer());
        mockClassLoader.setMockTransformerChain(list);

        // MockClassLoader will only be able to find 'foo/bar/baz/test.txt' if it
        // properly defers the resources lookup to its deferTo ClassLoader.
        Enumeration<URL> resources = mockClassLoader.getResources("org/powermock/core/classloader/foo/bar/baz/test.txt");
        Assert.assertNotNull(resources);
        Assert.assertTrue(resources.hasMoreElements());
        URL resource = resources.nextElement();
        Assert.assertTrue(resource.getPath().endsWith("test.txt"));
        Assert.assertFalse(resources.hasMoreElements());
    }

    @Test
    public void canFindDynamicClassFromAdjustedClasspath() throws Exception {
        // Construct MockClassLoader with @UseClassPathAdjuster annotation.
        // It activates our MyClassPathAdjuster class which appends our dynamic
        // class to the MockClassLoader's classpool.
        UseClassPathAdjuster useClassPathAdjuster = new UseClassPathAdjuster() {
            public Class<? extends Annotation> annotationType() {
                return UseClassPathAdjuster.class;
            }

            public Class<? extends ClassPathAdjuster> value() {
                return MyClassPathAdjuster.class;
            }
        };
        final MockClassLoader mockClassLoader = new MockClassLoader(new String[0], useClassPathAdjuster);
        List<MockTransformer> list = new LinkedList<MockTransformer>();
        list.add(new ClassMockTransformer());
        mockClassLoader.setMockTransformerChain(list);

        // setup custom classloader providing our dynamic class, for MockClassLoader to defer to
        mockClassLoader.deferTo = new ClassLoader(getClass().getClassLoader()) {
            @Override
            public Class<?> loadClass(String name)
                    throws ClassNotFoundException {
                if (name.equals(DynamicClassHolder.clazz.getName())) {
                    return DynamicClassHolder.clazz;
                }
                return super.loadClass(name);
            }
        };

        // verify that MockClassLoader can successfully load the class
        Class<?> dynamicTestClass = Class.forName(DynamicClassHolder.clazz.getName(), false, mockClassLoader);
        Assert.assertNotNull(dynamicTestClass);
        // .. and that MockClassLoader really loaded the class itself rather
        // than just providing the class from the deferred classloader
        assertNotSame(DynamicClassHolder.clazz, dynamicTestClass);
    }

    @Test(expected = ClassNotFoundException.class)
    public void cannotFindDynamicClassInDeferredClassLoader() throws Exception {

        MockClassLoader mockClassLoader = new MockClassLoader(new String[0]);
        List<MockTransformer> list = new LinkedList<MockTransformer>();
        list.add(new ClassMockTransformer());
        mockClassLoader.setMockTransformerChain(list);

        // setup custom classloader providing our dynamic class, for MockClassLoader to defer to
        mockClassLoader.deferTo = new ClassLoader(getClass().getClassLoader()) {

            @Override
            public Class<?> loadClass(String name)
                    throws ClassNotFoundException {
                return super.loadClass(name);
            }
        };

        //Try to locate and load a class that is not in MockClassLoader.
        Class<?> dynamicTestClass = Class.forName(DynamicClassHolder.clazz.getName(), false, mockClassLoader);

    }

    @Test
    public void canLoadDefinedClass() throws Exception {
        final String className = "my.ABCTestClass";
        final MockClassLoader mockClassLoader = new MockClassLoader(new String[]{className});


        Whitebox.invokeMethod(mockClassLoader, "defineClass", className, DynamicClassHolder.classBytes,
                              0, DynamicClassHolder.classBytes.length, this.getClass().getProtectionDomain());
        Class.forName(className, false, mockClassLoader);

        mockClassLoader.loadClass(className);

    }

    // helper class for canFindDynamicClassFromAdjustedClasspath()
    static class MyClassPathAdjuster implements ClassPathAdjuster {
        public void adjustClassPath(ClassPool classPool) {
            classPool.appendClassPath(new ByteArrayClassPath(DynamicClassHolder.clazz.getName(), DynamicClassHolder.classBytes));
        }
    }

    // helper class for canFindDynamicClassFromAdjustedClasspath()
    static class DynamicClassHolder {
        final static byte[] classBytes;
        final static Class<?> clazz;

        static {
            try {
                // construct a new class dynamically
                ClassPool cp = ClassPool.getDefault();
                final CtClass ctClass = cp.makeClass("my.ABCTestClass");
                classBytes = ctClass.toBytecode();
                clazz = ctClass.toClass();
            } catch (Exception e) {
                throw new RuntimeException("Problem constructing custom class", e);
            }
        }
    }

    static class MyClassloader extends MockClassLoader {

        public MyClassloader(String[] classesToMock) {
            super(classesToMock);
        }

        @Override
        protected Class findClass(String name) throws ClassNotFoundException {
            if (name.startsWith("java.lang")) {
                return this.getClass().getClassLoader().loadClass(name);
            }
            return super.findClass(name);
        }

        public Class<?> findClassPublic(String s) throws ClassNotFoundException {
            return findClass(s);
        }
    }

}
