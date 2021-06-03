// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.prettierjs

import com.intellij.ide.actionsOnSave.ActionOnSaveBackedByOwnConfigurable
import com.intellij.ide.actionsOnSave.ActionOnSaveInfo
import com.intellij.ide.actionsOnSave.ActionOnSaveInfoProvider
import com.intellij.ide.actionsOnSave.ActionsOnSaveConfigurable
import com.intellij.javascript.nodejs.util.NodePackageField
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.NlsSafe
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.*
import java.nio.file.FileSystems
import java.util.regex.PatternSyntaxException
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.text.JTextComponent

private const val CONFIGURABLE_ID = "settings.javascript.prettier"

class PrettierConfigurable(private val project: Project) : BoundSearchableConfigurable(
  PrettierBundle.message("configurable.PrettierConfigurable.display.name"), "reference.settings.prettier", CONFIGURABLE_ID) {

  private var runOnSaveCheckBox: JCheckBox? = null

  override fun createPanel(): DialogPanel {
    val prettierConfiguration = PrettierConfiguration.getInstance(project)

    return panel {
      val packageField = NodePackageField(project, PrettierUtil.PACKAGE_NAME, null)
      row(JLabel(PrettierBundle.message("prettier.package.label")).apply { labelFor = packageField }) {
        packageField().withBinding(
          { it.selectedRef },
          { nodePackageField, nodePackageRef -> nodePackageField.selectedRef = nodePackageRef },
          PropertyBinding({ prettierConfiguration.nodePackageRef }, { prettierConfiguration.withLinterPackage(it) })
        )
      }

      row {
        val runForFilesLabel = JLabel(PrettierBundle.message("run.for.files.label"))
        runForFilesLabel()

        cell(isFullWidth = true) {
          component(JBTextField(prettierConfiguration.filesPattern))
            .withBinding({ textField -> textField.text.trim() },
                         JTextComponent::setText,
                         PropertyBinding({ prettierConfiguration.filesPattern }, { prettierConfiguration.filesPattern = it }))
            .withValidationOnInput {
              try {
                FileSystems.getDefault().getPathMatcher("glob:" + it.text)
                null
              }
              catch (e: PatternSyntaxException) {
                @NlsSafe val firstLine = e.localizedMessage?.lines()?.firstOrNull()
                ValidationInfo(firstLine ?: PrettierBundle.message("invalid.pattern"), it)
              }
            }
            .component.apply { runForFilesLabel.labelFor = this }

          comment(PrettierBundle.message("files.pattern.comment"))
        }
      }

      // empty label - to have the check box below the file patterns field
      row("") {
        cell {
          checkBox(PrettierBundle.message("on.code.reformat.label"),
                   { prettierConfiguration.isRunOnReformat },
                   { prettierConfiguration.isRunOnReformat = it })

          val shortcut = ActionManager.getInstance().getKeyboardShortcut(IdeActions.ACTION_EDITOR_REFORMAT)
          shortcut?.let { comment(KeymapUtil.getShortcutText(it)) }
        }
      }

      // empty label - to have the check box below the file patterns field
      row("") {
        cell {
          runOnSaveCheckBox = checkBox(PrettierBundle.message("on.save.label"),
                                       { prettierConfiguration.isRunOnSave },
                                       { prettierConfiguration.isRunOnSave = it })
            .component

          //val shortcut = ActionManager.getInstance().getKeyboardShortcut("SaveAll")
          //shortcut?.let { comment(KeymapUtil.getShortcutText(it)) }

          ActionsOnSaveConfigurable.createGoToActionsOnSavePageLink()().withLargeLeftGap()
        }
      }
    }
  }


  class PrettierOnSaveInfoProvider : ActionOnSaveInfoProvider() {
    override fun getActionOnSaveInfos(project: Project): List<ActionOnSaveInfo> = listOf(PrettierOnSaveActionInfo(project))
  }


  private class PrettierOnSaveActionInfo(val project: Project)
    : ActionOnSaveBackedByOwnConfigurable<PrettierConfigurable>(CONFIGURABLE_ID, PrettierConfigurable::class.java) {

    @Suppress("DialogTitleCapitalization")
    override fun getActionOnSaveName() = PrettierBundle.message("run.on.save.checkbox.on.actions.on.save.page")

    override fun isActionOnSaveEnabledAccordingToStoredState() = PrettierConfiguration.getInstance(project).isRunOnSave

    override fun isActionOnSaveEnabledAccordingToUiState(configurable: PrettierConfigurable) = configurable.runOnSaveCheckBox!!.isSelected

    override fun setActionOnSaveEnabled(configurable: PrettierConfigurable, enabled: Boolean) {
      configurable.runOnSaveCheckBox!!.isSelected = enabled;
    }

    override fun getActionLinks() = listOf(ActionsOnSaveConfigurable.createGoToPageInSettingsLink(CONFIGURABLE_ID))
  }
}
