/**
 * Copyright 2012-2013 the Semargl contributors. See AUTHORS for more details.
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
package org.semarglproject.example;

import org.ccil.cowan.tagsoup.jaxp.SAXParserImpl;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.semarglproject.rdf.TurtleSerializer;
import org.semarglproject.sink.CharOutputSink;
import org.semarglproject.source.StreamProcessor;
import org.semarglproject.rdf.core.ParseException;
import org.semarglproject.rdf.rdfa.RdfaParser;
import org.semarglproject.vocab.RDFa;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

/**
 * Creates standard RFFa processor endpoint.
 *
 * Request expmples:
 * GET http://localhost:8080/?uri=http://schema.org/Person
 * GET http://localhost:8080/?uri=http://schema.org/Person&rdfaversion=1.0
 * GET http://localhost:8080/?uri=http://schema.org/Person&rdfagraph=output
 *
 */
public final class RdfaProcessorEndpoint extends AbstractHandler {

    private final StreamProcessor streamProcessor;
    private final CharOutputSink charOutputSink;

    public RdfaProcessorEndpoint() throws SAXException {
        charOutputSink = new CharOutputSink("UTF-8");
        streamProcessor = new StreamProcessor(RdfaParser.connect(TurtleSerializer.connect(charOutputSink)));
        streamProcessor.setProperty(RdfaParser.ENABLE_VOCAB_EXPANSION, true);

        // use error-prone HTML parser to produce valid XML documents
        XMLReader reader = SAXParserImpl.newInstance(null).getXMLReader();
        streamProcessor.setProperty(StreamProcessor.XML_READER_PROPERTY, reader);
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);
        server.setHandler(new RdfaProcessorEndpoint());
        server.start();
        server.join();
    }

    @Override
    public void handle(String s, Request baseRequest, HttpServletRequest request,
                       HttpServletResponse response) throws IOException, ServletException {
        String uri = request.getParameter("uri");
        if (uri == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            baseRequest.setHandled(false);
            return;
        }

        // parameter name specified by RDFa Core 1.1 section 7.6.1
        String rdfaGraph = request.getParameter("rdfagraph");
        boolean sinkOutputGraph = false;
        boolean sinkProcessorGraph = false;
        if (rdfaGraph != null) {
            String[] keys = rdfaGraph.trim().toLowerCase().split(",");
            for (String key : keys) {
                if (key.equals("output")) {
                    sinkOutputGraph = true;
                } else if (key.equals("processor")) {
                    sinkProcessorGraph = true;
                }
            }
        } else {
            sinkOutputGraph = true;
            sinkProcessorGraph = true;
        }
        streamProcessor.setProperty(RdfaParser.ENABLE_OUTPUT_GRAPH, sinkOutputGraph);
        streamProcessor.setProperty(RdfaParser.ENABLE_PROCESSOR_GRAPH, sinkProcessorGraph);

        String rdfaversion = request.getParameter("rdfaversion");
        if ("1.0".equals(rdfaversion)) {
            streamProcessor.setProperty(RdfaParser.RDFA_VERSION_PROPERTY, RDFa.VERSION_10);
        } else if ("1.1".equals(rdfaversion)) {
            streamProcessor.setProperty(RdfaParser.RDFA_VERSION_PROPERTY, RDFa.VERSION_11);
        }

        System.out.println(uri);
        URL url = new URL(uri);
        Reader reader = new InputStreamReader(url.openStream());

        response.setContentType("text/turtle; charset=UTF-8");
        charOutputSink.connect(response.getWriter());
        try {
            streamProcessor.process(reader, uri);
        } catch (ParseException e) {
            // ignore
        }
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
    }
}
