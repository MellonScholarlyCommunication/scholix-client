package scholix

import groovy.json.*

class ScholixClient {
    def BASE_URL = 'http://api.scholexplorer.openaire.eu/v2'

    def linkProvider() {
        def jsonSlurper = new JsonSlurper()

        def connection = new URL("${BASE_URL}/LinkProvider").openConnection()
        
        connection.setRequestProperty('Accept','application/json')

        def response = connection.inputStream.text

        def json = jsonSlurper.parseText(response)

        return json
    }

    def inSource() {
        def jsonSlurper = new JsonSlurper()

        def connection = new URL("${BASE_URL}/LinkPublisher/inSource").openConnection()
        
        connection.setRequestProperty('Accept','application/json')

        def response = connection.inputStream.text

        def json = jsonSlurper.parseText(response)

        return json
    }

    def inTarget() {
        def jsonSlurper = new JsonSlurper()

        def connection = new URL("${BASE_URL}/LinkPublisher/inTarget").openConnection()
        
        connection.setRequestProperty('Accept','application/json')

        def response = connection.inputStream.text

        def json = jsonSlurper.parseText(response)

        return json
    }

    def links(linkProvider, closure) {
        def jsonSlurper = new JsonSlurper()

        def linkProviderEsc = java.net.URLEncoder.encode(linkProvider, "UTF-8")
        def totalPages = 0
        def currentPage = 0

        do {
            def connection = new URL("${BASE_URL}/Links?linkProvider=${linkProviderEsc}&page=${currentPage}").openConnection()
        
            connection.setRequestProperty('Accept','application/json')

            def response = connection.inputStream.text

            def json = jsonSlurper.parseText(response)

            for (res in json.result) {
                closure.call(res)
            }

            totalPages = json.totalPages
            currentPage += 1
        } while (currentPage <= totalPages)
    }
}