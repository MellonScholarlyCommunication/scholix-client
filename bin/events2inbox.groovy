#!/usr/bin/env groovy -cp ./lib

import scholix.ScholixClient
import groovy.json.JsonOutput
import java.net.HttpURLConnection

if (args.size() == 0) {
    System.err.println("usage: events2inbox.groovy events-file")
    System.exit(1)
}

new ScholixClient().cache_loop(args[0], {
    x -> sendNotification(x)
})

def sendNotification(evt) {
    def targetInbox = evt['target']['inbox']

    def notification = JsonOutput.prettyPrint(JsonOutput.toJson(evt))

    def url = new URL(targetInbox)
    def connection = (HttpURLConnection) url.openConnection()

    connection.with {
        setDoOutput(true)
        setRequestMethod('POST')
        setRequestProperty('Accept','text/turle')
        setRequestProperty('Content-Type','application/ld+json')
        setRequestProperty('Content-Length',"" + notification.getBytes().length)
        outputStream.withWriter { writer ->
            writer << notification
        }

        println(responseCode + " " + targetInbox)
    }
}