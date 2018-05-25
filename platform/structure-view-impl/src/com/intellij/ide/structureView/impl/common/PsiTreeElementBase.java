/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.ide.structureView.impl.common;

import com.intellij.ide.structureView.StructureViewExtension;
import com.intellij.ide.structureView.StructureViewFactoryEx;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.structureView.customRegions.CustomRegionStructureUtil;
import com.intellij.ide.util.treeView.AbstractTreeUi;
import com.intellij.ide.util.treeView.NodeDescriptorProvidingKey;
import com.intellij.ide.util.treeView.TreeAnchorizer;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.util.Iconable;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class PsiTreeElementBase <T extends PsiElement> implements StructureViewTreeElement, ItemPresentation, NodeDescriptorProvidingKey {

  private final Object myValue;

  protected PsiTreeElementBase(T psiElement) {
    myValue = psiElement == null ? null : TreeAnchorizer.getService().createAnchor(psiElement);
  }

  @NotNull
  @Override
  public ItemPresentation getPresentation() {
    return this;
  }

  @Override
  @NotNull
  public Object getKey() {
    return String.valueOf(getElement());
  }

  @Nullable
  public final T getElement() {
    //noinspection unchecked
    return myValue == null ? null : (T)TreeAnchorizer.getService().retrieveElement(myValue);
  }

  @Override
  public Icon getIcon(boolean open) {
    final PsiElement element = getElement();
    if (element != null) {
      int flags = Iconable.ICON_FLAG_READ_STATUS;
      if (!(element instanceof PsiFile) || !element.isWritable()) flags |= Iconable.ICON_FLAG_VISIBILITY;
      return element.getIcon(flags);
    }
    else {
      return null;
    }
  }

  @Override
  public T getValue() {
    return getElement();
  }

  @Override
  public String getLocationString() {
    return null;
  }

  public boolean isSearchInLocationString() {
    return false;
  }

  public String toString() {
    final T element = getElement();
    return element != null ? element.toString() : "";
  }

  @NotNull
  @Override
  public final StructureViewTreeElement[] getChildren() {
    return AbstractTreeUi.calculateYieldingToWriteAction(this::doGetChildren);
  }

  @NotNull
  private StructureViewTreeElement[] doGetChildren() {
    final T element = getElement();
    if (element == null) return EMPTY_ARRAY;
    return mergeWithExtensions(element, getChildrenBase());
  }

  @Override
  public void navigate(boolean requestFocus) {
    final T element = getElement();
    if (element != null) {
      ((Navigatable)element).navigate(requestFocus);
    }
  }

  @Override
  public boolean canNavigate() {
    final T element = getElement();
    return element instanceof Navigatable && ((Navigatable)element).canNavigate();
  }

  @Override
  public boolean canNavigateToSource() {
    return canNavigate();
  }

  @NotNull public abstract Collection<StructureViewTreeElement> getChildrenBase();

  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final PsiTreeElementBase that = (PsiTreeElementBase)o;

    T value = getValue();
    return value == null ? that.getValue() == null : value.equals(that.getValue());
  }

  public int hashCode() {
    T value = getValue();
    return value == null ? 0 : value.hashCode();
  }

  public boolean isValid() {
    return getElement() != null;
  }

  /** @return element base children merged with children provided by extensions */
  @NotNull
  public static StructureViewTreeElement[] mergeWithExtensions(@NotNull PsiElement element,
                                                               @NotNull Collection<StructureViewTreeElement> baseChildren) {
    List<StructureViewTreeElement> result = new ArrayList<>(CustomRegionStructureUtil.groupByCustomRegions(element, baseChildren));
    StructureViewFactoryEx structureViewFactory = StructureViewFactoryEx.getInstanceEx(element.getProject());
    Class<? extends PsiElement> aClass = element.getClass();
    for (StructureViewExtension extension : structureViewFactory.getAllExtensions(aClass)) {
      StructureViewTreeElement[] children = extension.getChildren(element);
      if (children != null) {
        ContainerUtil.addAll(result, children);
      }
    }
    return result.toArray(StructureViewTreeElement.EMPTY_ARRAY);
  }
}
