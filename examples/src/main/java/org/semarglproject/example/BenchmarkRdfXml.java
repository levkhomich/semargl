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

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.UriRef;
import org.apache.clerezza.rdf.core.access.TcManager;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.clerezza.rdf.core.serializedform.SupportedFormat;
import org.semarglproject.clerezza.core.sink.ClerezzaSink;
import org.semarglproject.jena.core.sink.JenaSink;
import org.semarglproject.source.StreamProcessor;
import org.semarglproject.rdf.core.ParseException;
import org.semarglproject.rdf.RdfXmlParser;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class BenchmarkRdfXml {

    private static final String BENCHMARK_DIR = "src/main/resources";
    private static final String HTTP_EXAMPLE_COM = "http://example.com";

    private BenchmarkRdfXml() {
    }

    private static List<File> listFiles(String path) {
        ArrayList<File> result = new ArrayList<File>();
        File dir = new File(path);
        if (dir.exists()) {
            result.addAll(Arrays.asList(dir.listFiles()));
        }
        return result;
    }

    private static MGraph createClerezzaModel() {
        TcManager manager = TcManager.getInstance();
        UriRef graphUri = new UriRef(HTTP_EXAMPLE_COM);
        if (manager.listMGraphs().contains(graphUri)) {
            manager.deleteTripleCollection(graphUri);
        }
        return manager.createMGraph(graphUri);
    }

    private static long benchmarkSemarglJena(String path) throws SAXException, ParseException {
        System.out.println("Semargl-Jena benchmark");
        Model model = ModelFactory.createDefaultModel();

        StreamProcessor streamProcessor = new StreamProcessor(RdfXmlParser.connect(JenaSink.connect(model)));

        List<File> files = listFiles(path);
        long time = System.nanoTime();
        for (File file : files) {
            streamProcessor.process(file, HTTP_EXAMPLE_COM);
        }
        System.out.println("Model size = " + model.size());
        return System.nanoTime() - time;
    }

    private static long benchmarkSemarglClerezza(String path) throws SAXException, ParseException {
        System.out.println("Semargl-Clerezza benchmark");
        MGraph model = createClerezzaModel();

        StreamProcessor streamProcessor = new StreamProcessor(RdfXmlParser.connect(ClerezzaSink.connect(model)));

        List<File> files = listFiles(path);
        long time = System.nanoTime();
        for (File file : files) {
            streamProcessor.process(file, HTTP_EXAMPLE_COM);
        }
        System.out.println("Model size = " + model.size());
        return System.nanoTime() - time;
    }

    private static long benchmarkJena(String path) throws FileNotFoundException {
        System.out.println("Jena benchmark");
        Model model = ModelFactory.createDefaultModel();

        List<File> files = listFiles(path);
        long time = System.nanoTime();
        for (File file : files) {
            model.read(new FileReader(file), HTTP_EXAMPLE_COM);
        }
        System.out.println("Model size = " + model.size());
        return System.nanoTime() - time;
    }

    private static long benchmarkClerezza(String path) throws FileNotFoundException {
        System.out.println("Clerezza benchmark");
        MGraph model = createClerezzaModel();
        Parser parser = Parser.getInstance();

        List<File> files = listFiles(path);
        long time = System.nanoTime();
        for (File file : files) {
            parser.parse(model, new FileInputStream(file), SupportedFormat.RDF_XML);
        }
        System.out.println("Model size = " + model.size());
        return System.nanoTime() - time;
    }

    private static void printResults(long time) {
        System.out.println("Processing time: " + time / 1000000 + " ms");
        System.out.println();
    }

    public static void main(String[] args) throws Exception {
        printResults(benchmarkSemarglJena(BENCHMARK_DIR));
        printResults(benchmarkJena(BENCHMARK_DIR));
        printResults(benchmarkSemarglClerezza(BENCHMARK_DIR));
        printResults(benchmarkClerezza(BENCHMARK_DIR));
    }

}
