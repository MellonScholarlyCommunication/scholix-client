# Scholix Client

Groovy tools to generate LDN Notifications following our [Mellon Spec](https://mellonscholarlycommunication.github.io/spec-notifications/#the-artifact-context)
from Scholix data as provided by [ScholeXplorer](https://scholexplorer.openaire.eu/#/)

# Dependency

- Groovy (use [SDKMAN!](https://sdkman.io))

# Usage

List all providers

```
bin/scholix_client.groovy providers
```

Fetch JSON records for one provider

```
bin/scholix_client.groovy links Lirias > data/lirias.json
```

Convert JSON to events

```
bin/scholix2events.groovy data/lirias.json > data/lirias.events
```

Create inbox folder for the events

```
bin/mkinbox.sh data/lirias.events out
```