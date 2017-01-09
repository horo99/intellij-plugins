package name.kropp.intellij.makefile

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import name.kropp.intellij.makefile.psi.*


class MakefileAnnotator : Annotator {
  private val lineTokenSet = TokenSet.create(MakefileTypes.LINE)

  override fun annotate(element: PsiElement, holder: AnnotationHolder) {
    if (element is MakefileTarget) {
      holder.createInfoAnnotation(element, null).textAttributes = MakefileSyntaxHighlighter.TARGET
    } else if (element is MakefilePrerequisite) {
      val reference = element.reference!!
      if (reference.resolve() == null) {
        val targets = (element.parent.parent.parent as MakefileTargetLine).targets
        if (targets.targetList.firstOrNull()?.name?.startsWith('.') == false) {
          holder.createErrorAnnotation(element, "Unresolved prerequisite").registerFix(CreateRuleFix(element))
        }
      } else {
        holder.createInfoAnnotation(element, null).textAttributes = MakefileSyntaxHighlighter.PREREQUISITE
      }
    } else if (element is MakefileVariable) {
      holder.createInfoAnnotation(element, null).textAttributes = MakefileSyntaxHighlighter.VARIABLE
    } else if (element is MakefileVariableValue) {
      element.node.getChildren(lineTokenSet).forEach {
        holder.createInfoAnnotation(it, null).textAttributes = MakefileSyntaxHighlighter.VARIABLE_VALUE
      }
    }
  }
}