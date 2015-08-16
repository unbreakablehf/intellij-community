/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.jetbrains.reactivemodel.mapping

import java.lang.annotation.*
import kotlin.reflect.KClass

/**
 * Define custom bean mapper. Mapper should have default constructor
 */
Documented
Retention(RetentionPolicy.RUNTIME)
Target(value = ElementType.TYPE)
public annotation class BeanMapper(public val from: KClass<out Any>, public val to: KClass<out Any>)