#!/usr/bin/env groovy -cp ./lib

import groovy.cli.commons.CliBuilder
import scholix.ScholixClient
import groovy.json.*

def usage() {
    System.err.println("""
usage: scholix_client.groovy command

command:

   providers - list all providers
   inSource  - list all publishers that provide source objects in Scholix
   inTarget  - list all publishers that provide target objects in Scholix
   links     - list all Scholix links
                    - needs {targetPublisher|sourcePublisher} {name}
                    - optional {harvestedAfter}
""")
    System.exit(1)
}

def cli = new CliBuilder()

def options = cli.parse(args)

if (options.arguments().size() < 1) {
    usage()
}

def function = options.arguments()[0]

if (function == 'providers') {
    doProviders()
}
else if (function == 'inSource') {
    doInSource()
}
else if (function == 'inTarget') {
    doInTarget()
}
else if (function == 'links' && options.arguments().size() > 1 ) {
    def filterName = options.arguments()[1]
    def filterValue = options.arguments()[2]
    def harvestedAfter = null

    if (options.arguments().size() > 3) {
        harvestedAfter = options.arguments()[3]
    }

    doLinks(filterName, filterValue, harvestedAfter)
}
else {
    usage()
}

def doProviders() {
    def providers = new ScholixClient().linkProvider()

    for (prov in providers) {
        println("${prov.name}\t${prov.totalRelationships}")
    }
}

def doInSource() {
    def inSource = new ScholixClient().inSource()

    for (source in inSource) {
        println("${source.name}\t${source.totalRelationships}")
    }
}

def doInTarget() {
    def inTarget = new ScholixClient().inTarget()

    for (target in inTarget) {
        println("${target.name}\t${target.totalRelationships}")
    }
}

def doLinks(filterName, filterValue, harvestedAfter) {
    def links = new ScholixClient().links(filterName, filterValue, harvestedAfter, {
        x -> println(JsonOutput.toJson(x))
    })
}