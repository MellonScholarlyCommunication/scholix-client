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

def cli = new CliBuilder(
    usage: 'artifact2citations.groovy [options] data-file' 
)

cli.with {
    t(longOpt: 'type' , 'output type (default turtle)' , args: 1, required: false)
}

def opt = cli.parse(args)

if (opt.arguments().size() == 0) {
    cli.usage()
    return
}

def input = opt.arguments()[0]
def type  = opt.t

if (input.equals("-")) {
    input = "/dev/stdin"
}

if (!type) {
    type = "turtle"
}

main_loop(input, type)

def main_loop(file, type) {
    new File(file).withReader('UTF-8') {
        reader -> {
            def line
            while( (line = reader.readLine()) != null) {
                def start = CLIENT.time()
                extractArtifact(line, type)
                def end = CLIENT.time()
                CLIENT.duration("extractArtifact", start, end)
            }
        }
    }
}

def extractArtifact(url, type) {
    def ext = new Extractor()

    CLIENT.verbose("${url} - parsing")

    def data = ext.parse(url)

    if (! data) {
        CLIENT.verbose("${url} - failed to find citations")
        return -1
    }

    def jsonld = ext.asJSONLD(url,data) 

    if (! jsonld) {
        CLIENT.verbose("${url} - failed to create JSON-LD")
        return -2
    }

    if (type == 'jsonld') {
        println(jsonld) 
        return 0
    }
    else if (type == 'turtle') {
        def turtle = ext.asTurtle(jsonld)

        if (! turtle) {
            CLIENT.verbose("${url} - failed to create Turtle")
            return -3
        }

        println(turtle)

        return 0
    }
    else {
        CLIENT.verbose("${url} - unknown type ${type}")
        return -4
    }
}