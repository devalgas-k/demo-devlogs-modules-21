# Démo : `String` concat dans une boucle : l'anti-pattern qui vide ta heap

Reproduction minimale de l'article :
**"`String` concat dans une boucle : l'anti-pattern qui vide ta heap"** (article #20).

Le projet expose deux endpoints HTTP équivalents fonctionnellement, mais
dont les profils d'allocation sont radicalement différents.

- `GET /api/concat?rows=20000` — implémentation naïve (`String +=` dans une boucle).
- `GET /api/builder?rows=20000` — implémentation optimisée (`StringBuilder`).

Une simulation Gatling enchaîne les deux endpoints en parallèle pour
mesurer l'écart de latence et de débit.

## Stack

- Java **21**
- Spring Boot **3.4.0**
- Gatling **3.10.3** (plugin Maven)

## Build

```bash
mvn -q -DskipTests package
```

## Lancer l'application

```bash
mvn spring-boot:run
```

L'application écoute sur `http://localhost:8080`.

## Reproduire le bench (Gatling)

Dans un terminal : l'application tourne (cf. commande ci-dessus).

Dans un second terminal :

```bash
mvn gatling:test -Dgatling.simulationClass=com.example.demo.simulations.ConcatVsBuilderSimulation
```

Le rapport HTML est généré dans `target/gatling/<nom-simulation>-<timestamp>/index.html`.

> Pour pousser la charge, augmente `constantConcurrentUsers(50)` à 200 ou 500.
> Le mode "naïf" s'effondrera bien plus vite que le mode "builder".

## Ce qu'il faut observer

- **p50 / p95 / p99** : la divergence apparaît dès p50 et explose à p99.
- **Débit max** : `/api/builder` sature à un multiple du débit de `/api/concat`.
- **GC** : activer `-Xlog:gc*` pour voir le volume d'allocations/minor GC.

> Anecdote : sur 50 utilisateurs concurrents, `/api/concat` dépasse
> fréquemment les 10 s par requête, là où `/api/builder` reste sous 200 ms.

## Limites de la démo

- Pas de pool partagé de `StringBuilder` : la démo crée un builder par requête
  (équivalent à ce que ferait `StringBuilder` dans 99 % des cas réels).
- Pas de cache de `String.format` : à chaque appel, `CsvRow.toCsvCell()`
  alloue. C'est volontaire (sinon le micro-bench perdrait son sens), mais
  à éviter en prod.
- Pas de `StringConcatFactory` (`Java 9+`) : on reste sur le mode "old school"
  pour rendre l'écart visible.
