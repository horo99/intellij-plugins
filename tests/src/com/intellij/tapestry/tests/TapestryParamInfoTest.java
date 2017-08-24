package com.intellij.tapestry.tests;

import com.intellij.codeInsight.hint.api.impls.XmlParameterInfoHandler;
import com.intellij.javaee.ExternalResourceManagerEx;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlTag;
import com.intellij.testFramework.utils.parameterInfo.MockCreateParameterInfoContext;
import com.intellij.testFramework.utils.parameterInfo.MockParameterInfoUIContext;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.impl.schema.XmlAttributeDescriptorImpl;
import com.intellij.xml.util.XmlUtil;

/**
 * @author Alexey.Chmutov
 */
public class TapestryParamInfoTest extends TapestryBaseTestCase {
  public void testTmlTagAttrs() throws Exception {
    addComponentToProject("Count");
    doTest("end mixins start value");
  }

  public void testHtmlTagAttrs() throws Exception {
    final ExternalResourceManagerEx manager = ExternalResourceManagerEx.getInstanceEx();
    final String doctype = manager.getDefaultHtmlDoctype(myFixture.getProject());
    manager.setDefaultHtmlDoctype(XmlUtil.XHTML_URI, myFixture.getProject());
    try {
      addComponentToProject("Count");
      doTest(
        // two lang attrs: one for xml:lang
        "class dir end id lang lang mixins onclick ondblclick onkeydown onkeypress onkeyup onmousedown onmousemove onmouseout onmouseover onmouseup start style title value");
    }
    finally {
      manager.setDefaultHtmlDoctype(doctype, myFixture.getProject());
    }
  }

  private void doTest(String attrs) {
    initByComponent();

    XmlParameterInfoHandler handler = new XmlParameterInfoHandler();
    MockCreateParameterInfoContext createContext = new MockCreateParameterInfoContext(myFixture.getEditor(), myFixture.getFile());
    XmlTag tag = handler.findElementForParameterInfo(createContext);
    assertNotNull(tag);
    handler.showParameterInfo(tag, createContext);
    Object[] items = createContext.getItemsToShow();
    assertTrue(items != null);
    assertTrue(items.length > 0);
    final XmlElementDescriptor descriptor = (XmlElementDescriptor)items[0];
    MockParameterInfoUIContext context = new MockParameterInfoUIContext<PsiElement>(tag);
    handler.updateUI(descriptor, context);

    String joined = StringUtil.join(handler.getParametersForDocumentation(descriptor, createContext), o -> {
      if (o instanceof XmlAttributeDescriptorImpl) {
//          return o.
      }
      return ((XmlAttributeDescriptor)o).getName();
    }, " ");
    assertEquals(attrs, joined);

  }

  @Override
  protected String getBasePath() {
    return "parameterInfo/";
  }
}