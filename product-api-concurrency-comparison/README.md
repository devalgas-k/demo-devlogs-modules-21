# Product API concurrency comparison

Démo Spring Boot de l'article « Une fiche produit, 5 appels API : CompletableFuture ou Structured Concurrency ? ».

Les deux endpoints agrègent les mêmes cinq services simulés et renvoient le même contrat : produit, prix, stock, avis et livraison. La politique est volontairement stricte : les cinq résultats sont requis, le premier échec annule les autres appels et une deadline globale de 500 ms s'applique à toute l'agrégation.

## Versions

- JDK 25.0.2 LTS ;
- Structured Concurrency : cinquième preview de JDK 25 ;
- Spring Boot 4.1.2 ;
- Maven 3.9.9.

Structured Concurrency est une API preview. La compilation et l'exécution nécessitent donc `--enable-preview`, avec la même version majeure du JDK.

## Pourquoi Maven

Le dépôt utilise déjà un agrégateur Maven. Le module reste autonome grâce à son propre parent Spring Boot 4.1.2, nécessaire pour la prise en charge officielle de Java 25, tout en étant déclaré dans `projets/pom.xml`.

## Compiler et tester

Depuis la racine du dépôt :

```bash
mvn -f projets/product-api-concurrency-comparison/pom.xml clean verify
```

Ou via l'agrégateur :

```bash
mvn -f projets/pom.xml -pl product-api-concurrency-comparison clean verify
```

Les tests couvrent :

- les cinq appels réussis et l'égalité des réponses ;
- un service lent et la deadline globale ;
- un service indisponible ;
- deux pannes concurrentes ;
- l'interruption du thread propriétaire ;
- l'interruption des tâches devenues inutiles ;
- les statuts et contrats Problem Details des deux endpoints.

## Lancer

```bash
java --enable-preview -jar projets/product-api-concurrency-comparison/target/product-api-concurrency-comparison-1.0.0-SNAPSHOT.jar
```

## Endpoints

```bash
curl -s 'http://localhost:8080/api/products/keyboard-42/completable-future?scenario=success'
curl -s 'http://localhost:8080/api/products/keyboard-42/structured-concurrency?scenario=success'
```

Scénarios disponibles :

| Scénario | Comportement attendu |
|---|---|
| `success` | cinq résultats, HTTP 200 |
| `slow-service` | Delivery dépasse la deadline, HTTP 504 |
| `one-failure` | Pricing échoue, siblings interrompus, HTTP 502 |
| `multiple-failures` | Pricing et Reviews échouent presque ensemble, HTTP 502 |

Exemple timeout :

```bash
curl -i 'http://localhost:8080/api/products/keyboard-42/structured-concurrency?scenario=slow-service'
```

## Ce que la comparaison rend visible

La variante `CompletableFuture` conserve le `Thread` virtuel de chaque appel. C'est nécessaire car `CompletableFuture.cancel(true)` ne garantit pas l'interruption du calcul sous-jacent. Un future de coordination explicite assure aussi le fail-fast : `CompletableFuture.allOf()` seul attendrait toutes les tâches.

La variante Structured Concurrency regroupe les cinq tâches dans un scope lexical. Le `Joiner` annule les siblings au premier échec, le timeout appartient à la configuration du scope, et `close()` attend la fin effective des enfants. La variante CompletableFuture reproduit cette garantie avec `Thread.interrupt()` puis `Thread.join()` explicites.

## Limites

- Les APIs distantes sont simulées avec des délais déterministes et interruptibles.
- Ces tests vérifient la sémantique et le nettoyage, pas les performances.
- Aucun benchmark de débit, CPU ou mémoire n'est fourni.
- Une opération I/O réelle qui ignore l'interruption peut retarder la fermeture d'un scope.
- L'API Structured Concurrency évolue encore entre versions du JDK ; ce code cible précisément JDK 25.
