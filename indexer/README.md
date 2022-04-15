Search-Indexer is the service that pulls events from Kafka and stores them in the specific ES indices.

# Elasticsearch Migrations guide

1. Migrations are stored in [src/main/resources/es/migration](src/main/resources/es/migration)
2. Each file's name starts with `V1`. This designates the version of ES implementation.
3. The order of migration script execution goes after `V1`. E.g. file `V1.25__...` will be executed after `V1.24__...`
4. After the version, goes the script name, followed by the required extension `.http`
5. The scripts content is essentially a curl string for calling ES Rest Api. [See examples](src/main/resources/es/migration/V1.1__activity_template.http)