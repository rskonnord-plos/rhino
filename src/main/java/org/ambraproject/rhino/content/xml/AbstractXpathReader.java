/*
 * Copyright (c) 2017 Public Library of Science
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package org.ambraproject.rhino.content.xml;

import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.ambraproject.rhino.util.NodeListAdapter;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * A container for a node of XML data that reads it with XPath queries.
 * <p/>
 * Instances of this class are not thread-safe because they hold instances of {@link XPath} and {@link Transformer} to
 * use.
 */
public abstract class AbstractXpathReader {

  private static final ThreadLocal<XPathFactory> XPATH_FACTORY = ThreadLocal.withInitial(XPathFactory::newInstance);
  private static final ThreadLocal<TransformerFactory> TRANSFORMER_FACTORY = ThreadLocal.withInitial(TransformerFactory::newInstance);

  protected Node xml;
  protected final XPath xPath;
  private final Transformer transformer;

  protected AbstractXpathReader(Node xml) {
    this();
    this.xml = Preconditions.checkNotNull(xml);
  }

  protected AbstractXpathReader() {
    // XPath isn't thread-safe, so we need one per instance of this class
    this.xPath = XPATH_FACTORY.get().newXPath();

    try {
      this.transformer = TRANSFORMER_FACTORY.get().newTransformer();
    } catch (TransformerConfigurationException e) {
      throw new RuntimeException(e);
    }

    // The output will be stored in a character-encoded context (JSON or the database),
    // so declaring the encoding as part of the string doesn't make sense.
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
  }

  /**
   * Define default text formatting for this object.
   */
  protected String sanitize(String text) {
    return text;
  }

  /**
   * Define default text formatting for this object. By default, just calls {@link org.w3c.dom.Node#getTextContent()}.
   * Override for something different. Returns null on null input.
   *
   * @param node an XML node
   * @return the node's text content with optional formatting
   */
  protected final String getTextFromNode(Node node) {
    return (node == null) ? null : sanitize(node.getTextContent());
  }

  protected final String getXmlFromNode(Node node) {
    if (node == null) return null;
    StringWriter writer = new StringWriter();
    try {
      transformer.transform(new DOMSource(node), new StreamResult(writer));
    } catch (TransformerException e) {
      throw new RuntimeException(e);
    }
    return sanitize(writer.toString());
  }

  protected String readString(String query) {
    return readString(query, xml);
  }

  protected String readString(String query, Node node) {
    Node stringNode = readNode(query, node);
    if (stringNode == null) {
      return null;
    }
    String text = getTextFromNode(stringNode);
    return (StringUtils.isBlank(text) ? null : text);
  }

  protected Node readNode(String query) {
    return readNode(query, xml);
  }

  protected Node readNode(String query, Node node) {
    try {
      return (Node) xPath.evaluate(query, node, XPathConstants.NODE);
    } catch (XPathExpressionException e) {
      throw new InvalidXPathException(query, e);
    }
  }

  protected List<Node> readNodeList(String query) {
    return readNodeList(query, xml);
  }

  protected List<Node> readNodeList(String query, Node node) {
    NodeList nodeList;
    try {
      nodeList = (NodeList) xPath.evaluate(query, node, XPathConstants.NODESET);
    } catch (XPathExpressionException e) {
      throw new InvalidXPathException(query, e);
    }
    return NodeListAdapter.wrap(nodeList);
  }

  protected List<String> readTextList(String query) {
    List<Node> nodeList = readNodeList(query);
    List<String> textList = Lists.newArrayListWithCapacity(nodeList.size());
    for (Node node : nodeList) {
      textList.add(getTextFromNode(node));
    }
    return textList;
  }


  /**
   * Produce a description of a node suitable for debugging.
   */
  protected static String logNode(Node node) {
    List<String> parentChain = getParentChain(node);
    String xml = recoverXml(node);
    xml = CharMatcher.WHITESPACE.trimAndCollapseFrom(xml, ' '); // Condense whitespace to log onto one line
    return String.format("{Location: %s. Node: %s}", Joiner.on('/').join(parentChain), xml);
  }

  private static List<String> getParentChain(Node node) {
    Deque<String> stack = new ArrayDeque<>();
    while (node != null) {
      stack.push(node.getNodeName());
      node = node.getParentNode();
    }
    return ImmutableList.copyOf(stack);
  }

  private static String recoverXml(Node node) {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    try {
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      transformer.transform(new DOMSource(node), new StreamResult(outputStream));
    } catch (TransformerException e) {
      throw new RuntimeException(e);
    }
    return new String(outputStream.toByteArray()); // TODO: Encoding?
  }

}
