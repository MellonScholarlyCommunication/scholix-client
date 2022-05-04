#!/usr/bin/env groovy -cp ./lib

import scholix.ScholixClient
import groovy.json.JsonOutput
import groovy.transform.Field

@Field String MOCK_BASE_POD = 'https://bellow2.ugent.be/test/scholix/'
@Field String EMAIL = "Patrick.Hochstenbach@UGent.be"

if (args.size() == 0) {
    System.err.println("usage: scholix2events.groovy json-ld-file")
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
            'name' : 'OpenAIRE ScholeXplorer' ,
            'inbox' : 'https://scholexplorer.openaire.eu/inbox/' ,
            'type' : 'Organization'
        ] ,
        'origin' : [
            'id' : 'https://mellonscholarlycommunication.github.io/about#us' ,
            'name' : 'UGent/Mellon JSON-LD/AS2 Notification Generator for Scholix' ,
            'type' : 'Application'
        ] ,
        'object' : [
            'id' : "urn:uuid:${relationshipId}" ,
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
            event['object']['object']  = target['resolved']

            // Send an event to the target
            event['context'] = target['resolved']

            event['target'] = [
                'id'    : "${target['base']}/about#us" ,
                'inbox' : "${target['base']}/inbox/" ,
                'type'  : 'Organization'
            ]
            
            sendEvent(event)

            // Send an event to the source

            event['object']['subject'] = source['resolved']
            event['object']['object']  = target['url']

            event['context'] = source['resolved']

            event['target'] = [
                'id'    : "${source['base']}/about#us" ,
                'inbox' : "${source['base']}/inbox/" ,
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
         def id       = target['ID']
         def idurl    = target['IDURL']
         def idscheme = target['IDScheme']

         if (idscheme.equals('D-Net Identifier')) {
             // TODO How to resolve
         }
         else if (idscheme.equals('handle') && id) {
            def location = resolve("http://hdl.handle.net/${id}")

            if (location) {
                def base = baseurl(location)

                candidates.push([
                    'url'      : "http://hdl.handle.net/${id}" ,
                    'resolved' : location ,
                    'base'     : base
                ])
            }
         }
         else if (idscheme.equals('pmc') && id) {
            def location = "http://europepmc.org/articles/${id}"
            def base = baseurl(location)
            candidates.push([
                'url'      : location,
                'resolved' : location,
                'base'     : base
            ]) 
         }
         else if (idscheme.equals('pmid') && id) {
            def location = "https://pubmed.ncbi.nlm.nih.gov/${id}"
            def base = baseurl(location)
            candidates.push([
                'url'      : location,
                'resolved' : location,
                'base'     : base
            ])
         }
         else if (idurl && idurl.startsWith('http')) {
            def location = resolve(idurl)
            
            if (location) {
                def base = baseurl(location)

                candidates.push([
                    'url'      : idurl ,
                    'resolved' : location ,
                    'base'     : base
                ])
            }
         }
         else {
             System.err.println(
                    'Skipped : idscheme(' + idscheme + ') ' + 
                    'id(' + id + ') ' + 
                    'idurl(' + idurl + ') '); 
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

    connection.setRequestProperty('User-Agent',
        'GroovyBib/1.1 (https://github.com/MellonScholarlyCommunication/scholix-client; ' +
        'mailto:' + EMAIL + ')'  
    )

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
    
    if (MOCK_BASE_POD) {
        return MOCK_BASE_POD + u.getHost()
    }
    else {
        return "https://" + u.getHost()
    }
}