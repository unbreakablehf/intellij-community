// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.codeInspection.deprecation;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInspection.DeprecationUtil;
import com.intellij.codeInspection.InspectionProfile;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.apiUsage.ApiUsageUastVisitor;
import com.intellij.codeInspection.ui.MultipleCheckboxOptionsPanel;
import com.intellij.java.analysis.JavaAnalysisBundle;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.uast.UastVisitorAdapter;
import org.intellij.lang.annotations.Pattern;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class MarkedForRemovalInspection extends DeprecationInspectionBase {

  public boolean IGNORE_PROJECT_CLASSES = false;

  @Override
  @NotNull
  public PsiElementVisitor buildVisitor(@NotNull final ProblemsHolder holder, boolean isOnTheFly) {
    PsiFile file = holder.getFile();
    DeprecatedApiUsageProcessor processor =
      new DeprecatedApiUsageProcessor(holder, false, false, false, false, IGNORE_IN_SAME_OUTERMOST_CLASS, true, IGNORE_PROJECT_CLASSES,
                                      getCurrentSeverity(file));
    return new UastVisitorAdapter(new ApiUsageUastVisitor(processor), true);
  }

  @Override
  @NotNull
  public String getGroupDisplayName() {
    return "";
  }

  @Override
  @NotNull
  public String getShortName() {
    return DeprecationUtil.FOR_REMOVAL_SHORT_NAME;
  }

  @Pattern(VALID_ID_PATTERN)
  @Override
  @NotNull
  @NonNls
  public String getID() {
    return DeprecationUtil.FOR_REMOVAL_ID;
  }

  @Override
  public JComponent createOptionsPanel() {
    final MultipleCheckboxOptionsPanel panel = new MultipleCheckboxOptionsPanel(this);
    addSameOutermostClassCheckBox(panel);
    panel.addCheckbox(JavaAnalysisBundle.message("ignore.in.the.same.project"), "IGNORE_PROJECT_CLASSES");
    return panel;
  }

  private static HighlightSeverity getCurrentSeverity(@NotNull PsiFile file) {
    HighlightDisplayKey highlightDisplayKey = HighlightDisplayKey.find(DeprecationUtil.FOR_REMOVAL_SHORT_NAME);
    InspectionProfile profile = InspectionProjectProfileManager.getInstance(file.getProject()).getCurrentProfile();
    HighlightDisplayLevel displayLevel = profile.getErrorLevel(highlightDisplayKey, file);
    return displayLevel.getSeverity();
  }
}
