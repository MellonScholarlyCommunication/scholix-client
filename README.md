# Scholix Client

Groovy tools to generate LDN Notifications following our [Mellon Spec](https://mellonscholarlycommunication.github.io/spec-notifications/#the-artifact-context)
from Scholix data as provided by [ScholeXplorer](https://scholexplorer.openaire.eu/#/)

# Dependency

- Groovy (use [SDKMAN!](https://sdkman.io))
- Anystyle (use `gem install anystyle-cli`)
- Jena riot (JSON-LD processing is required `brew install jena`)

# Usage

List all sources / targets

```
bin/scholix_client.groovy inSource
bin/scholix_client.groovy inTarget
```

Fetch JSON records for one source/target

```
bin/scholix_client.groovy links targetPublisher "Ghent University" > data/ghent.json
bin/scholix_client.groovy links sourcePublisher "Ghent University" >> data/ghent.json
```

Convert JSON to events

```
bin/scholix2events.groovy data/ghent.json > data/ghent.events
```

Create inbox folder for the events

```
bin/mkinbox.sh data/ghent.events out
```

Create a Gephi graph for a repository

```
bin/events2graph.groovy  out/biblio/biblio.ugent.be > data/biblio.gexf
bin/events2graph.groovy  out/antwerpen/repository.uantwerpen.be > data/antwerpen.gexf
bin/events2graph.groovy  out/liege/orbi.uliege.be/ > data/liege.gexf
bin/events2graph.groovy  out/belgium/ > data/belgium.gexf
```