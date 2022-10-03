package citation

import groovy.json.*
import org.apache.jena.rdf.model.Model
import org.apache.jena.util.FileUtils
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.riot.RDFFormat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import groovy.cli.commons.CliBuilder

class Extractor {
    def script = './script/url2citations.sh'

    def parse(url) {
        def sout = new StringBuilder(), serr = new StringBuilder()
        def proc = "${script} ${url}".execute()
        proc.consumeProcessOutput(sout, serr)
        proc.waitForOrKill(10000) // Timeout

        def data = parseJson(sout.toString())

        return data
    }

    def parseJson(str) {
        if (! str) 
            return null
        def jsonSlurper = new JsonSlurper()
        jsonSlurper.parseText(str)
    }

    def loadModel(str, type, baseUrl="urn:dummy") {
        Model model = ModelFactory.createDefaultModel()
        model.read(new ByteArrayInputStream(str.getBytes()),baseUrl,type)
        return model;
    }

    def asTurtle(jsonld) {
       def dataModel = loadModel(jsonld,'JSONLD')
       def out = new ByteArrayOutputStream()
       RDFDataMgr.write(out, dataModel, RDFFormat.NT);
       return out.toString()
    } 

    def asJSONLD(url,json) {
        def jsonld = [:]
        jsonld['@id'] = url
        jsonld['@type'] = 'schema:CreativeWork' 
        jsonld['@context'] = [
            dct: 'http://purl.org/dc/terms/' ,
            foaf: 'http://xmlns.com/foaf/0.1/' ,
            bibo: 'http://purl.org/ontology/bibo/' ,
            qb: 'http://purl.org/linked-data/cube#' ,
            schema: 'http://schema.org/' ,
            skos: 'http://www.w3.org/2004/02/skos/core#' ,
            citations: [
                '@id': 'schema:citation'
            ] ,
            title: [
                '@id': 'dct:title' 
            ] ,
            author: [
                '@id': 'dct:creator'
            ] ,
            family: [
                '@id': 'foaf:surname'
            ] ,
            given: [
                '@id': 'foaf:firstName'
            ] , 
            date: [
                '@id' : 'dct:date'
            ] ,
            type: [
                '@id' : 'dct:type'
            ] ,
            pages: [
                '@id' : 'bibo:pages'
            ] ,
            'citation-number': [
                '@id' : 'qb:order'
            ] ,
            volume: [
                '@id' : 'bibo:volume'
            ] ,
            issue: [
                '@id' : 'bibo:issue'
            ] ,
            url: [
                '@id' : 'schema:url'
            ] ,
            doi: [
                '@id' : 'bibo:doi'
            ] ,
            note: [
                '@id' : 'skos:note'
            ] ,
            publisher: [
                '@id' : 'dct:publisher'
            ] ,
            'container-title': [
                '@id' : 'dct:source'
            ]
        ]
        jsonld['citations'] = json
        def str = JsonOutput.toJson(jsonld)
        return JsonOutput.prettyPrint(str)
    }
}