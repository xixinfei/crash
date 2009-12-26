/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.crsh.jcr;

import org.crsh.fs.FileSystem;
import org.crsh.util.Safe;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a>
 * @version $Revision$
 */
public class Importer implements FileSystem {

  /** . */
  private final ContentHandler handler;

  /** . */
  private final LinkedList<EndElement> stack;

  /** . */
  private final List<String> prefixes;

  /** . */
  private final DefaultHandler attributesHandler = new DefaultHandler() {

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
      if (stack.isEmpty()) {
        System.out.println("Adding prefix " + prefix + " = " + uri);
        handler.startPrefixMapping(prefix, uri);
        prefixes.add(prefix);
      }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      System.out.println("Creating element " + qName);
      handler.startElement(uri, localName, qName, attributes);
      stack.addLast(new EndElement(uri, localName, qName));
    }
  };

  public Importer(ContentHandler handler) {
    this.handler = handler;
    this.stack = new LinkedList<EndElement>();
    this.prefixes = new ArrayList<String>();
  }

  public void startDirectory(String directoryName) throws IOException {
  }

  public void file(String fileName, int length, InputStream data) throws IOException {
    try {
      SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setNamespaceAware(true);
      factory.setValidating(true);
      SAXParser parser = factory.newSAXParser();
      parser.parse(data, attributesHandler);
    }
    catch (Exception e) {
      Safe.rethrow(IOException.class, e);
    }
  }

  public void endDirectory(String directoryName) throws IOException {
    try {
      EndElement end = stack.removeLast();
      handler.endElement(end.uri, end.localName, end.qName);
      if (stack.isEmpty()) {
        for (String prefix : prefixes) {
          System.out.println("Removing prefix " + prefix);
          handler.endPrefixMapping(prefix);
        }
        prefixes.clear();
      }
    }
    catch (Exception e) {
      Safe.rethrow(IOException.class, e);
    }
  }

  private static class EndElement {

    /** . */
    private final String uri;

    /** . */
    private final String localName;

    /** . */
    private final String qName;

    private EndElement(String uri, String localName, String qName) {
      this.uri = uri;
      this.localName = localName;
      this.qName = qName;
    }
  }
}

