#!/usr/bin/env groovy -cp ./lib

@Grab(group='commons-cli', module='commons-cli', version='1.4')

import groovy.json.*
import groovy.cli.commons.CliBuilder
import groovy.transform.Field

@Field String  EMAIL = "Patrick.Hochstenbach@UGent.be"
@Field String  CACHE_FILE = "data/cache.json"
@Field HashMap CACHE = [:]
@Field boolean CLEANUP_REQUIRED = true

def cli = new CliBuilder(
    usage: 'events2graph.groovy [options] data-file' 
)

def opt = cli.parse(args)

if (opt.arguments().size() == 0) {
    cli.usage()
    return
}

def path = opt.arguments()[0]

def fh = new File(path)

if (! fh.isDirectory()) {
    System.err.println("error - ${path} is not a directory")
    System.exit(1)
}

// Adding a cache to make repeated runs of resolution of DOI-s etc faster
loadCache()

Runtime.runtime.addShutdownHook {
  System.err.println("# Shutting down...")
  if( CLEANUP_REQUIRED ) {
    System.err.println("# Cleaning up...")
    writeJson(CACHE_FILE,CACHE)
  }
}
//--------------------

subjectObjectGraph(path)

CLEANUP_REQUIRED = false

def loadCache()  {
    def f = new File(CACHE_FILE)

    if (f.exists()) {
        CACHE = parseJson(CACHE_FILE)
    }
    else {
        CACHE = [:]
    }
}

def subjectObjectGraph(path) {
    File file = new File(path)

    def nodeSet = new HashSet<String>()
    def edgeMap = [:]

    file.eachFileRecurse {
        if (it.getName() =~ /jsonld$/) {
            def json    = parseJson(it.getPath())
            def subject      = json['object']['subject']
            def object       = json['object']['object'] 
            def relationship = json['object']['relationship'] 
            def context      = json['context']

            if (context.equals(subject)) {
                def object_r = resolve(object)

                def subjectAuth      = (new URL(subject)).getAuthority()
                def relationshipAuth = (new URL(relationship)).getPath().replaceAll(".*/","")
                def objectAuth       = (new URL(object_r)).getAuthority()
                
                nodeSet.add(subjectAuth)
                nodeSet.add(objectAuth)

                //def key = "${subjectAuth}-${relationshipAuth}-${objectAuth}"
                def key = "${subjectAuth}-${objectAuth}"

                if (edgeMap[key]) {
                    edgeMap[key]['weight'] += 1
                }
                else {
                    edgeMap[key] = [
                        'subject': subjectAuth ,
                        'object' : objectAuth ,
                        'weight' : 1
                    ]
                }

                System.err.println("<${subject}> <${relationship}> <${object_r}> .")
            }
            else if (context.equals(object)) {
                def subject_r = resolve(subject)

                def subjectAuth      = (new URL(subject_r)).getAuthority()
                def relationshipAuth = (new URL(relationship)).getPath().replaceAll(".*/","")
                def objectAuth       = (new URL(object)).getAuthority()

                //def key = "${subjectAuth}-${relationshipAuth}-${objectAuth}"
                def key = "${subjectAuth}-${objectAuth}"

                nodeSet.add(subjectAuth)
                nodeSet.add(objectAuth)

                if (edgeMap[key]) {
                    edgeMap[key]['weight'] += 1
                }
                else {
                    edgeMap[key] = [
                        'subject': subjectAuth ,
                        'object' : objectAuth ,
                        'weight' : 1
                    ]
                }

                System.err.println("<${subject_r}> <${relationship}> <${object}> .")
            }
            else {
                System.err.println("# ${it} context error");
            }
        }
    }

    println '''<?xml version="1.0" encoding="UTF-8"?>
<gexf xmlns:viz="http:///www.gexf.net/1.1draft/viz" version="1.1" xmlns="http://www.gexf.net/1.1draft">
<graph defaultedgetype="directed" idtype="string" type="static">
'''
    def nodesCount = nodeSet.size()

    println "<nodes count=\"${nodesCount}\">"

    nodeSet.each {
        println "<node id=\"${it}\" label=\"${it}\"/>"        
    }

    println "</nodes>"

    def edgesCount = edgeMap.size()

    println "<edges count=\"${edgesCount}\">"

    edgeMap.each {
        k, v -> {
            def source = v['subject']
            def target = v['object']
            def weight = v['weight']

            println "<edge id=\"${k}\" source=\"${source}\" target=\"${target}\" weight=\"${weight}\"/>"
        }
    }

    println "</edges>"

    print "</graph>\n</gexf>"
}

def parseJson(path) {
    def jsonSlurper = new JsonSlurper()
    File file = new File(path)
    String fileContent = file.text
    return jsonSlurper.parseText(fileContent)
}

def writeJson(path,data) {
    def output = JsonOutput.toJson(data)
    new File(path).withWriter { out -> out.println(output) }
}

def resolve(url) {
    if ( CACHE.containsKey(url))
        return CACHE[url]

    def result 

    // Hack: 10679 is an extreme slow responding server
    if (url.startsWith('http://hdl.handle.net/10679')) {
        result = url.replaceAll(
                    'http://hdl.handle.net/10679',
                    'https://eresearch.ozyegin.edu.tr/handle/10679'
                )  
    }
    else {
        result = resolve_network(url)
    }

    CACHE[url] = result 

    return result
}

def resolve_network(url) {
    System.err.println("# ..resolving ${url}")

    def connection = new URL(url).openConnection()

    connection.setRequestProperty('User-Agent',
        'GroovyBib/1.1 (https://github.com/MellonScholarlyCommunication/scholix-client; ' +
        'mailto:' + EMAIL + ')'  
    )

    connection.setConnectTimeout(10000)

    connection.setRequestMethod('HEAD')
    connection.setInstanceFollowRedirects(false)

    def location = connection.getHeaderField('location')

    def result

    if (! location) {
        result = url
    }
    else if (location.startsWith('http://hdl.handle.net')) {
        result = resolve(location)
    }
    else if (location.startsWith('http')) {
        result = location
    }
    else {
        result = url
    }

    return result
}
