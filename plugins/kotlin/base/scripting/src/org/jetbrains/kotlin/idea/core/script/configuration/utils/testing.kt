// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.idea.core.script.configuration.utils

import com.intellij.openapi.application.Application

var testScriptConfigurationNotification: Boolean = false

internal val Application.isUnitTestModeWithoutScriptLoadingNotification: Boolean
    get() = isUnitTestMode && !testScriptConfigurationNotification