#!/usr/bin/env groovy -cp ./lib

import scholix.ScholixClient
import groovy.json.JsonOutput
import java.net.HttpURLConnection
import groovy.transform.Field

@Field CLIENT = new ScholixClient()

if (args.size() == 0) {
    System.err.println("usage: events2inbox.groovy events-file")
    System.exit(1)
}

new ScholixClient().cache_loop(args[0], {
    x -> {
        def start = CLIENT.time()
        
        sendNotification(x)

        def end = CLIENT.time()

        CLIENT.duration("sendNotification", start, end)
    }
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

        CLIENT.verbose("post(${targetInbox}) : code = ${responseCode}")
    }
}