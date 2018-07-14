# Graph-migrate

A [DSE Graph](https://www.datastax.com/products/datastax-enterprise-graph) schema evolution tool, written in Java.

The migration tool allows you to version schema and data creation scripts for graph.

Database migrations allows you to:

1. Create a graph schema from scratch
2. Apply a specific set of schema versions to a database
3. Evolve a database schema, moving it from an older version to a newer one

## How does it work

Schema definitions and data creation scripts are stored in set of numerically versioned files. Once a version is applied to a database, the file is treated as immutable. The database stores all of the versions applied to it in a ```databaseMigration``` vertex. 

Every time the migration tool runs, it will retrieve the versions already applied and only apply the versions needed to evolve the database to the desired version.

A checksum of the content of every version is stored in the database. Should a previously applied file be changed, the migration will abort because the immutability constraint has been violated. This can only be resolved by rolling back the change to the file and applying the changes in a new version.

### Example

In our first schema version we define a author vertex with a single property ```name```.

```
v001_author.gremlin
-------------------
schema.propertyKey('name').Text().create();
schema.vertexLabel('author').properties('name').create();
```

Next we get some new requriements and need to store the authors ```nationality```. Rather than going back and modifying ```v001_author.gremlin``` we create a new version as below.

When we run the migration tool again, it only needs to apply ```v002_author.gremlin``` to evolve the database to the latest version.  

```
v002_author.gremlin
-------------------
schema.propertyKey('nationality').Text().create();
schema.vertexLabel('author').properties('name').add();
```

## Schema Agreement

A check for schema agreement is performed after every schema modifying statement is executed.
 
The migration will wait until all hosts that are currently up, agree on the schema definition. If schema agreement cannot be acheived within 15 seconds the migration will be halted.

## Configuration

Graph schemas are configured using a YAML files.

```
schema: killrvideo
migrationPath: ./
profiles:
  local:
    options:
      graph.replication_config: "{'class' : 'SimpleStrategy', 'replication_factor' : 1 }"
      graph.allow_scan: true
  prod:
    options:
      graph.replication_config: "{'class' : 'SimpleStrategy', 'replication_factor' : 3 }"
      graph.allow_scan: false
```

```schema``` the name of the graph schema to create. The schema is only created if it doesn't already exist.

```migrationPath``` specifies the directory containing '.groovy' or '.gremlin' files. The path must be relative to the location of the yaml configuration file. Files will be executed in lexical order.

```profiles``` allows you to specify environment specific options for the graph schema.

## Files

File names must follow the convention ```v{version_number:03d}_{description}.{extension:(groovy|gremlin)}``` e.g. v001_author.gremlin

A simple parser is used to process graph statements. The parser requires all statements to be terminated with a semicolon. It can handle multiline indented statements, comment lines and inline comments. For simplicity multiline comment blocks are not supported.

## Usage

Common parameters:
```
 -H STRING[] : Comma-separated list of contact points
 -P VAL      : Connection password
 -c FILE     : Path to configuration file (default: graph-migrate.yml)
 -m VAL      : Name of the configuration profile to use
 -p N        : Connection port (default: 9042)
 -s          : Connect using SSL (default: false)
 -u VAL      : Connection username
 -v N        : Apply all versions up to and including (default: all)
```

### Running with Maven

```
mvn exec:java -Dexec.args="-H 127.0.0.1 -c graph-migrate.yaml -m profile -u username -P password -s" -Djavax.net.ssl.trustStore=truststore.jks
```

### Running from a Jar

```
mvn clean package

java -jar target/graph-migrate-1.0-SNAPSHOT.jar -H 127.0.0.1 -c graph-migrate.yaml -m profile -u admin -P password -s -Djavax.net.ssl.trustStore=truststore.jks
```

## Testing Locally

You can run a migration locally by using [ccm](https://github.com/riptano/ccm) to create a local graph cluster.

### Create a local graph node

* Follow the ccm installation [instructions](https://github.com/riptano/ccm#installation)

* Create an [account](https://academy.datastax.com/user/register?destination=quick-downloads) with DataStax

* Create a DSE cluster

```
ccm create localtest --dse -v 5.1.9 --dse-username=<username> --dse-password=<password> -n 1
ccm node1 setworkload graph
ccm start
```

### Run a test migration

```
java -jar ./target/graph-migrate-1.0-SNAPSHOT.jar -H 127.0.0.1 -c ./src/test/resources/omahoco/migrate/config/graph-migrate.yaml
```

### View versions applied to the database

```
ccm node1 dse gremlin-console

:remote config alias g killrvideo.g

g.V().hasLabel('databaseMigration').has('migrationLabel', 'databaseMigration')
```

