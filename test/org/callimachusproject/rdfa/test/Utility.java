/*
 * Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.callimachusproject.rdfa.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;

import javanet.staxutils.XMLEventPipe;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stax.StAXResult;
import javax.xml.transform.stream.StreamResult;

import org.callimachusproject.engine.RDFEventReader;
import org.callimachusproject.engine.RDFaReader;
import org.callimachusproject.engine.events.RDFEvent;
import org.callimachusproject.engine.events.Triple;
import org.callimachusproject.engine.model.Literal;
import org.callimachusproject.engine.model.Node;
import org.callimachusproject.engine.model.PlainLiteral;
import org.callimachusproject.engine.model.Term;
import org.callimachusproject.xml.DocumentFactory;
import org.openrdf.model.Graph;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.GraphImpl;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * @author Steve Battle
 */
public class Utility {
	
	static XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
	static {
		xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		xmlInputFactory.setProperty(XMLInputFactory.IS_VALIDATING, false);
		xmlInputFactory.setProperty("http://java.sun.com/xml/stream/properties/ignore-external-dtd", true);
	}
	
	// DOM support
	static DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
	static final String DTD_FEATURE = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
	static {
		documentBuilderFactory.setNamespaceAware(true);
		try {
			documentBuilderFactory.setFeature(DTD_FEATURE, false);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
	}
	
	// XSL support
	static TransformerFactory transformerFactory = TransformerFactory.newInstance();
	static final String INDENT_AMOUNT = "{http://xml.apache.org/xslt}indent-amount";


	public static RDFaReader parseRDFa(File rdfa, String base) throws Exception {
		XMLEventReader xml = xmlInputFactory.createXMLEventReader(new FileReader(rdfa));   
		RDFaReader rdf = new RDFaReader(base, xml, base);
		return rdf;
	}
	
	public static Statement createStatement(Triple t, ValueFactory f) throws Exception {
		Resource s = null;
		Node node = t.getSubject();
		if (node.isIRI()) {
			s = f.createURI(node.stringValue());
		}
		else {
			s = f.createBNode(node.stringValue());
		}
		URI p = f.createURI(t.getPredicate().stringValue());
		Term term = t.getObject();
		Value o = null;
		if (term.isPlainLiteral()) {
			PlainLiteral lit = term.asPlainLiteral();
			String lang = lit.getLang();
			if (lang == null) {
				o = f.createLiteral(lit.stringValue());
			} else {
				o = f.createLiteral(lit.stringValue(), lang);
			}
		}
		else if (term.isIRI()) {
			o = f.createURI(term.stringValue());
		}
		else if (term.isNode()) {
			o = f.createBNode(term.stringValue());
		}
		else if (term.isLiteral()) {
			Literal typed = term.asLiteral();
			URI uri = f.createURI(typed.getDatatype().stringValue());
			o = f.createLiteral(typed.stringValue(), uri);
		}
		else throw new Exception("unimplemented term: "+term);
		return f.createStatement(s, p, o);
	}
	
	public static void loadRepository(RepositoryConnection con, RDFEventReader rdf) throws Exception {
	    while (rdf.hasNext()) {
	    	RDFEvent e = rdf.next() ;
			if (e.isTriple())
				con.add(createStatement(e.asTriple(),con.getValueFactory()));
	    }
	    rdf.close();
	}
	
	public String load(File file) throws IOException {
		final FileReader reader = new FileReader(file);
		char[] block = new char[4096];
		final StringBuffer buffer = new StringBuffer();
		try {
			int len;
			while ((len = reader.read(block)) >0) {
				buffer.append(block, 0, len);
			}
		} finally {
			reader.close();
		}
		return buffer.toString();
	}
	
	public static Document readDocument(File file) throws Exception {
		if (!file.exists()) return null;
		DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
		return builder.parse(file);
	}
	
	public static void write(XMLEventReader xml, OutputStream out) throws Exception {
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(INDENT_AMOUNT, "2");
		transformer.transform (newSource(xml), new StreamResult(out));
		out.write('\n');
	}
	
	public static Document asDocument(XMLEventReader xml) throws Exception {
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(INDENT_AMOUNT, "2");
		Document doc = documentBuilderFactory.newDocumentBuilder().newDocument();
		transformer.transform (newSource(xml), new DOMResult(doc));
		return doc;
	}
	
	public static void write(Document doc, OutputStream out) throws Exception {
		Transformer transformer = transformerFactory.newTransformer();
		transformer.transform (new DOMSource(doc), new StreamResult(out));
		out.write('\n');
	}

	public static XMLEventReader asXMLEventReader(org.w3c.dom.Node node) throws Exception {
		XMLEventPipe pipe = new XMLEventPipe(10000);
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(INDENT_AMOUNT, "1");
		transformer.transform (new DOMSource(node), new StAXResult(pipe.getWriteEnd()));
		return pipe.getReadEnd();
	}

	public static Graph exportGraph(RepositoryConnection con) throws Exception {
		final Graph graph = new GraphImpl();
		con.export(new RDFHandlerBase() {
			public void handleStatement(Statement statement) throws RDFHandlerException {
				graph.add(statement);
			}			
		});
		return graph;
	}

	public static void write(Graph graph, OutputStream out) throws Exception {
		for (Iterator<Statement> i = graph.iterator(); i.hasNext(); ) {
			System.out.println(i.next());
		}
		out.write('\n');
	}
		
	public static Document transform(XMLEventReader xml, Transformer transformer) throws Exception {
		Document doc = documentBuilderFactory.newDocumentBuilder().newDocument();
		transformer.transform(newSource(xml), new DOMResult(doc));
		return doc;
	}

	private static Source newSource(XMLEventReader xml) throws IOException,
			SAXException, ParserConfigurationException, TransformerException {
		return createSource(toByteArrayInputStream(xml), null);
	}

	private static DOMSource createSource(InputStream in, String systemId)
			throws IOException, SAXException, ParserConfigurationException {
		try {
			DocumentFactory factory = DocumentFactory.newInstance();
			if (systemId == null)
				return createSource(factory.parse(in), null);
			return createSource(factory.parse(in, systemId), systemId);
		} finally {
			in.close();
		}
	}

	private static DOMSource createSource(Document node, String systemId) {
		if (systemId == null) {
			String documentURI = ((Document) node).getDocumentURI();
			if (documentURI != null)
				return createSource(node, documentURI);
		}
		if (systemId == null)
			return new DOMSource(node);
		return new DOMSource(node, systemId);
	}

	private static ByteArrayInputStream toByteArrayInputStream(XMLEventReader reader)
			throws TransformerException {
		if (reader == null)
			return null;
		ByteArrayOutputStream output = new ByteArrayOutputStream(8192);
		try {
			XMLEventWriter writer = XMLOutputFactory.newInstance().createXMLEventWriter(output);
			try {
				writer.add(reader);
			} finally {
				reader.close();
				writer.close();
				output.close();
			}
		} catch (XMLStreamException e) {
			throw new TransformerException(e);
		} catch (IOException e) {
			throw new TransformerException(e);
		}
		ByteArrayInputStream input = new ByteArrayInputStream(
				output.toByteArray());
		return input;
	}
	
}
