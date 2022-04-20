#!/usr/bin/env groovy -cp ./lib

import scholix.ScholixClient
import groovy.json.JsonOutput

if (args.size() == 0) {
    System.err.println("usage: scholix2events.groovy file")
    System.exit(1)
}

new ScholixClient().cache_loop(args[0], {
    x -> announceLinkProcessor(x)
})

def announceLinkProcessor(evt) {
     // need at least one dataset link
     if (!
         (evt['source']['Type'].equals('dataset') || 
          evt['target']['Type'].equals('dataset')
         )) {
             return
     }
 
     def id      = UUID.randomUUID().toString() 
     def published  = evt['LinkPublicationDate']
     def relationshipId   = UUID.randomUUID().toString()
     def relationshipType = evt['RelationshipType']['Name']

     def event = [
        '@context' : 'https://www.w3.org/ns/activitystreams' ,
        'id' : "urn:uuid:${id}" , 
        'type' : 'Announce' ,
        'published' : published ,
        'actor' : [
            'id' : 'https://scholexplorer.openaire.eu/#about' ,
            'name' : 'OPENAIRE Scholexplorer' ,
            'type' : 'OPENAIRE'
        ] ,
        'origin' : [
            'id' : 'https://mellonscholarlycommunication.github.io/about#us' ,
            'name' : 'Mellon Scholix Forward Service' ,
            'type' : 'Application'
        ] ,
        'object' : [
            'id' : relationshipId ,
            'type' : 'Relationship' ,
            'relationship' : "http://www.scholix.org/${relationshipType}" ,
        ]
     ]

     def sources = evt['source']['Identifier']
     def targets = evt['target']['Identifier']

     def targetCandidates = relationCandidates(targets)
     def sourceCandidates = relationCandidates(sources)

     for (source in sourceCandidates) {
        for (target in targetCandidates) {
    
            event['object']['subject'] = source['url']
            event['object']['object']  = target['location']

            // Send an event to the target
            event['context'] = target['location']

            event['target'] = [
                'id'    : "${target['base']}/about#us" ,
                'inbox' : "${target['base']}/inbox" ,
                'type'  : 'Organization'
            ]
            
            sendEvent(event)

            // Send an event to the source

            event['object']['subject'] = source['location']
            event['object']['object']  = target['url']

            event['context'] = source['location']

            event['target'] = [
                'id'    : "${source['base']}/about#us" ,
                'inbox' : "${source['base']}/inbox" ,
                'type'  : 'Organization'
            ] 

            sendEvent(event)
        }
     }
}

def sendEvent(event) {
    def json = JsonOutput.toJson(event)
    // println(JsonOutput.prettyPrint(json))
    println(json)
}

def relationCandidates(data) {
    def candidates = [];

    for (target in data) {
         def idurl    = target['IDURL']
         def idscheme = target['IDScheme']

         if (! idscheme.equals('D-Net Identifier') && 
                idurl && idurl.startsWith('http')) {
            def location = resolve(idurl)
            
            if (location) {
                def base = baseurl(location)

                candidates.push([
                    'url'      : idurl ,
                    'location' : location ,
                    'base'     : base
                ])
            }
         }
    }

    return candidates
}
  
def idType(list, scheme) {
    if (! list) {
        return null
    }

    for (li in list) {
        if (li['IDScheme'] == scheme) {
            return li['IDURL']
        }
    }

    return null
}

def resolve(url) {
    def connection = new URL(url).openConnection()

    connection.setRequestMethod('HEAD')
    connection.setInstanceFollowRedirects(false)

    def location = connection.getHeaderField('location')

    if (! location) {
        return url
    }
    else if (location.startsWith('http://hdl.handle.net')) {
        return resolve(location)
    }
    else if (location.startsWith('http')) {
        return location
    }
    else {
        return url
    }
}

def baseurl(url) {
    def u = new URL(url)
    def base = u.getProtocol() + "://" + u.getHost()
    return base
}