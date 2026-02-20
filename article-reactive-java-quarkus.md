# Reactive Java n'a pas a etre complexe: Quarkus le rend concret

## Sous-titre
Construire une application Java asynchrone, legere et resiliente avec Quarkus, Mutiny, PostgreSQL reactif et Kafka, sans se perdre dans la complexite.

## Pourquoi cet article

Le reactif en Java a longtemps eu une reputation difficile:

- APIs verbeuses
- chaines d'operations complexes a lire
- erreurs de threading difficiles a diagnostiquer

Mais dans les architectures modernes, il devient difficile d'ignorer les benefices du non-bloquant:

- meilleure concurrence avec moins de threads
- latence plus stable sous charge
- meilleure efficacite en environnement cloud

Le but ici est simple: montrer une approche pratique avec Quarkus et Mutiny, en partant d'un PoC reel et executable.

## Repository GitHub

[reactive-poc](https://github.com/snooker4real/reactive-poc)

## Le probleme du modele bloquant

Dans un modele classique thread-per-request:

1. une requete arrive
2. un thread est reserve
3. ce thread attend les I/O (DB, HTTP, messaging)

Pendant l'attente, ce thread reste immobilise. A faible charge, ce n'est pas visible. Sous pic, cela devient un plafond:

- saturation du pool de threads
- hausse de la memoire
- baisse de la reactivite

Le reactif inverse ce schema: on ne bloque pas, on compose des evenements. Le thread est libere, puis le traitement reprend quand la donnee arrive.

## Pourquoi Quarkus simplifie vraiment

Quarkus apporte trois avantages cles:

1. **Un coeur reactive natif** base sur Vert.x
2. **Mutiny** avec des types simples: `Uni<T>` et `Multi<T>`
3. **Un modele unifie**: imperative et reactive peuvent coexister

Concretement:

- un endpoint qui retourne `Uni<Response>` s'integre naturellement au runtime
- les extensions (REST, DB, Kafka) s'assemblent sans boilerplate massif
- Dev Services demarre les dependances techniques automatiquement en dev/test

## Le PoC: ce que nous avons implemente

Le projet contient trois flux principaux.

### 1) REST reactif + base reactive (FruitResource)

Endpoints:

- `GET /api/fruits`
- `GET /api/fruits/{id}`
- `POST /api/fruits`
- `DELETE /api/fruits/{id}`

Exemple de lecture par id avec `Uni<Response>`:

```java
@GET
@Path("/{id}")
public Uni<Response> getById(@PathParam("id") Long id) {
    return Fruit.<Fruit>findById(id)
            .onItem().ifNotNull().transform(fruit -> Response.ok(fruit).build())
            .onItem().ifNull().continueWith(() -> Response.status(Response.Status.NOT_FOUND).build());
}
```

Le point important: pas de gestion manuelle des threads, pas de callback nesting illisible.

### 2) Messaging reactif Kafka (PriceResource + PricePipeline)

- `POST /api/prices/{price}` publie un prix
- `@Incoming("prices-in")` consomme le message
- `GET /api/prices/last` expose la derniere valeur consommee

Exemple de publication:

```java
return pipeline.send(price)
        .replaceWith(Response.accepted(Map.of("published", price)).build());
```

Exemple de consommation:

```java
@Incoming("prices-in")
public Uni<Void> consume(double price) {
    return Uni.createFrom().voidItem()
            .invoke(() -> lastProcessedPrice.set(price));
}
```

### 3) Flux SSE + backpressure (Mutiny Multi)

Endpoint:

- `GET /api/prices/stream?intervalMs=5&count=50`

Le stream genere des ticks et applique une strategie d'overflow:

```java
return Multi.createFrom().ticks().every(Duration.ofMillis(safeIntervalMs))
        .onOverflow().dropPreviousItems()
        .onItem().transform(sequence -> new PriceTick(...))
        .select().first(safeCount);
```

Ici, `dropPreviousItems()` maintient un flux frais quand le consommateur est plus lent.

## Ce que le PoC demontre en pratique

Ce PoC n'est pas un slideware, il tourne et teste un flux complet:

- API HTTP reactive
- persistence PostgreSQL reactive
- pipeline Kafka reactive
- stream SSE avec gestion de pression

Nous avons aussi ajoute des tests d'integration end-to-end (`@QuarkusTest`) qui valident:

- CRUD fruit via HTTP
- publication prix -> consommation Kafka -> lecture REST
- reponse du flux SSE

## Detail important de stabilite en local

Dans cet environnement Docker, l'image Dev Services `postgres:17` echouait au demarrage.

La configuration a ete epinglee sur `postgres:16`:

```properties
quarkus.datasource.devservices.image-name=postgres:16
```

Ce point est utile si vous rencontrez des erreurs de type container launch timeout au boot.

## Comment lancer le projet

Prerequis:

- Java + Maven wrapper
- Docker actif
- HTTPie (optionnel pour les requetes)

Lancement:

```bash
./mvnw quarkus:dev
```

Requetes HTTPie pretes dans:

`httpie-requests.sh`

Exemple rapide:

```bash
http POST :8080/api/fruits name=Orange
http POST :8080/api/prices/101.25 Content-Type:application/json
http --stream GET :8080/api/prices/stream intervalMs==5 count==50 Accept:text/event-stream
```

## Ce que vous pouvez reprendre dans votre projet

Si vous voulez adopter cette approche en production, commencez par:

1. migrer un endpoint I/O vers `Uni<Response>`
2. remplacer les appels DB bloquants par Hibernate Reactive / client reactif
3. isoler un flux evenementiel avec Kafka + `@Incoming`
4. definir explicitement une strategie backpressure sur les streams critiques

La cle est d'avancer par tranche verticale, pas de tout basculer d'un coup.

## Conclusion

Le reactif en Java peut rester lisible, testable et productif. Avec Quarkus + Mutiny, on obtient:

- un modele asynchrone clair
- une integration native avec REST, data et messaging
- un runtime adapte aux workloads cloud et aux fortes concurrences

La promesse n'est pas "plus de magie".
La promesse est "moins de friction" pour construire des services rapides et resilients.

Si vous hesitiez a passer au reactif en Java, c'est un bon moment pour commencer avec un PoC concret comme celui-ci.
