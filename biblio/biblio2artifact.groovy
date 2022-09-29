#!/usr/bin/env groovy -cp ./lib

@Grab(group='commons-cli', module='commons-cli', version='1.4')
@Grab(group='commons-io', module='commons-io', version='2.8.0')
@Grab(group='org.apache.jena', module='jena-core', version='3.13.1')
@Grab(group='org.apache.jena', module='jena-tdb', version='3.13.1')
@Grab(group='org.slf4j', module='slf4j-api', version='1.7.26')
@Grab(group='org.slf4j', module='slf4j-simple', version='1.7.26')
@Grab(group='com.github.jsonld-java', module='jsonld-java', version='0.9.0')

import scholix.ScholixClient
import citation.Extractor
import groovy.transform.Field
import groovy.cli.commons.CliBuilder

@Field CLIENT = new ScholixClient()
@Field EXTRACTOR = new Extractor()

def cli = new CliBuilder(
    usage: 'biblio2artifact.groovy [options] data-file' 
)

def opt = cli.parse(args)

if (opt.arguments().size() == 0) {
    cli.usage()
    return
}

CLIENT.cache_loop(opt.arguments()[0], {
    x -> {
        extractArtifact(x)
    }
})

def extractArtifact(pub) {
    def id      = pub['_id']
    def created = pub['date_created']
    def file    = pub['file']

    if (file && file.size() > 0 ) {
        def fullText = file.findAll { 
            it['kind'] == 'fullText' &&  
            it['content_type'] == 'application/pdf'
        }

        if (fullText && fullText.size() > 0) {
            def mainFile = fullText[0]['url']
            def access   = fullText[0]['access']
            
            System.err.println("${created} ${id} ${mainFile} ${access}")

            def citation = EXTRACTOR.parse(mainFile)

            println(citation)
        }
    }
}