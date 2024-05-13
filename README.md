# Traces distribuées avec OpenTelemetry and Jaeger

La configuration des services est la suivante:
![Aperçu](images/overview.png "Aperçu")

On distingue 3 types de services:

- un service utilisateur qui expose une API HTTP
- un service de reporting qui  expose une API HTTP au service utilisateur  et qui utilise un producter Kafka pour créer des événements
- un service de messagerie qui consomme des événements kafka.

L'infrastructure est la suivante:

- Jaeger
- collecteur otel
- zookeeper
- kafka

Jaeger est utilisé pour visualiser les traces. Otel-collector gère les entrées de spring-otel-exporter, les transforme et envoie le résultat à Jaeger. Zookeeper et Kafka sont les composants usuels d'infrastructure.
