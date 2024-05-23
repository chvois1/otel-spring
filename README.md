# Traces distribuées avec OpenTelemetry and Jaeger

## Traces distribuées

Une [architecture EDA](https://learn.microsoft.com/fr-fr/azure/architecture/guide/architecture-styles/event-driven) repose sur un système distribué dont il faut observer le comportement ([observabilité](https://newrelic.com/fr/resources/ebooks/what-is-observability)). En plus des logs et des métriques (Key Performance Indicator ou KPI), la supervision doit proposer un traçage distribué qui permet de suivre la progression d'une requête à l'intérieur du système.

## OpenTelemetry

[OpenTelemetry](https://opentelemetry.io/docs/what-is-opentelemetry/) est une norme récente pour mettre en place la télémétrie (métriques, journaux et traces) dans des applications selon une démarche standardisée.
OpenTelemetry est un standard indépendant des fournisseurs existants de solutions télémétriques. Les données *otel* sont exportées vers les fournisseurs qui interprétent ce nouveau standard. OpenTelemetry propose des SDK pour plusieurs langages et bibliothèques.

## Traçage

Le traçage identifie de manière unique toute requête émise dans un système distribué que ce soit à travers HTTP/S, mais également vers un système de messagerie comme kafka.

Les trois composants du traçage sont le *Span*, le *SpanContext* et la *Trace*.

- Le span est le principal élément constitutif d'une trace distribuée. Il représente une unité de travail individuelle effectuée dans un système distribué.
- Le SpanContext transporte les données au-delà des limites d'un processus.
- La trace est une collection de Spans avec la même racine.

## Cas d'usage

La configuration des services est la suivante:

![Aperçu](images/overview.png "Aperçu")

On distingue 3 types de services:

- Un service utilisateur qui expose une API HTTP.
- Un service de reporting qui expose une API HTTP au service utilisateur et qui utilise un producteur Kafka pour créer des événements.
- Un service de messagerie qui consomme les événements kafka produits par le services de reporting.

L'objectif est de suivre toute demande émise dans le système: depuis le service utilisateur, jusqu'au service de messagerie. Dans cet exemple, l'architecture est simplifiée avec un nombre de services limité. Les avantages de cette architecture apparaissent lorsque le nombre des services impliqués dans un scénario utlisateur augmente.

L'infrastructure est la suivante:

- Un collecteur otel gère les entrées de spring-otel-exporter, les transforme et envoie le résultat à Jaeger.
- Jaeger s'utilise à la fois pour stocker et visualiser les traces.
- zookeeper s'utilise pour gérer et coordonner les brokers Kafka. Il élit le broker leader du cluster.
- kafka est le broker de messages utilisé dans cette architecture.

Le collecteur de traces otel est une chaîne de composants configurables:

récepteur -> traitement -> exporteur

![collecteur otel](images/otel.png "Collecteur otel")

## Déploiement

![network](images/network.png "network overview")

## Configuration

Spring Boot utilise [spring-cloud-starter-sleuth-otel](https://spring-projects-experimental.github.io/spring-cloud-sleuth-otel/docs/current/reference/html/project-features.html).

Le comportement d'une application Java se définit à l'aide de propriétés contenues dans un fichier de configuration YAML. Le comportement de  l'otel-starter est défini dans un fichier de configuration nommé *application.yaml*. La propriété *spring.sleuth.otel.config.trace-id-ratio-based* définit la probabilité d'exportation de traces à: 100 % (Mapping [0.0, 1.0] -> [0, 100] %).

Si le ratio est inférieur à 1.0, alors certaines traces ne seront pas exportées.

Ci-dessous, un extrait du fichier *otel-spring/tracing-user/src/main/resources/application.yaml*.

```yaml
server:
  port: 8080
services:
  report:
    url: http://report-service:8080
  email:
    url: http://email-service:8080

spring:
  application:
    name: user-service
  sleuth:
    otel:
      config:
        trace-id-ratio-based: 1.0
      exporter:
        otlp:
          endpoint: http://sleuth:4317
```

### Définition des traces

Toutes les requêtes sont créées à l'aide de [RestTemplate](https://www.baeldung.com/rest-template). Dans notre exemple, Java Spring ajoute des en-têtes de trace aux requêtes vers le services *user-service* et le service de réception nommé *report-service* sait comment analyser le contenu de ces traces.

Ci-dessous, un extrait du fichier *tracing-user/src/main/java/com/tracing/service/users/clients/ReportClient.java*

```java
public class ReportClient {
    private final RestTemplate restTemplate;
    @Value("${services.report.url}")
    private String reportURL;
    public ReportClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Report postReportForCustomerId(Long id) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        JSONObject reportJsonObject = new JSONObject();
        reportJsonObject.put("id", id);
        reportJsonObject.put("report", "This new generated report.");

        HttpEntity<String> request = new HttpEntity<String>(reportJsonObject.toString(), headers);

        return restTemplate.postForObject(this.reportURL + "/reports", request, Report.class);
    }
}
```

## Démonstration

### Pré-requis

Git et Maven.

### Installation

Cloner le projet git et fabriquer les paquets.

```bash
git clone https://github.com/chvois1/otel-spring.git
cd otel-spring 
mvn compile
mvn package
```

### Services applicatifs

Dans une fenêtre de commande, lancer les services applicatifs et les services de supervisison.

```bash
cd docker-compose 
start.sh
```

### Service utilisateur

Dans une fenêtre de commande, lancer le service utilisateur.

```bash
cd docker-compose 
start.sh
docker compose up user-service
```

### Tests

Dans une fenêtre de commande, lancer les scripts curl qui interrogent les API Web du service utilisateur.

```bash
cd docker-compose 
doit.sh
```

### Résultats

Pour analyser les logs et les traces, il est possible de consulter la sortie standard du service utilisateur. La sortie de ce service utilisateur affichera *traceId* et *spanId*.

```bash
2024-05-12 18:21:45.984  INFO [user-service,f515bcf46b607671e1182d5903a5d261,779f554008223b4c] 1 --- [nio-8080-exec-1] c.tracing.service.users.UserController   : Creating new report for user: 1
```

L'utilisateur fait une demande initiale au service utilisateur et cette demande transite vers le service de reporting. Le service de reporting péserve la trace initiée dans le service utilisateur, la complète puis l'affiche sur sa sortie standard.

```bash
2024-05-12 18:21:46.617  INFO [report-service,f515bcf46b607671e1182d5903a5d261,75dc1c69c94bf0f2] 1 --- [nio-8080-exec-1] c.t.service.reports.ReportController     : Creating new report: 1
```

L'exportateur du collecteur est configuré pour transmettre ses données à Jaeger qui interprète le standard otel. Ensuite, Jaeger représente visuellement le chemin complet d'une requête associé à un scénario utilisateur.

![jaeger screenshot](images/jaeger-timeline.png "jaeger screenshot")

![jaeger screenshot](images/Jager-graph.png "jaeger screenshot")
