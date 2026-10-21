# Offset, cursor et keyset : démonstration reproductible

Java 21, Maven 3.6.3+, Spring Boot 3.4.0 hérité du [parent](../pom.xml). Le programme compare des contrats de pagination ; **cursor est un protocole de continuation, keyset une stratégie SQL**. Un cursor peut également encapsuler un offset.

## Build, tests et exécution

Depuis la racine du dépôt avec `JAVA_HOME` configuré sur JDK 21 :

```bash
mvn -f projets/pom.xml -pl demo-pagination-offset-cursor-keyset clean verify
java -jar projets/demo-pagination-offset-cursor-keyset/target/demo-pagination-offset-cursor-keyset-0.0.1-SNAPSHOT.jar
```

Depuis ce module :

```bash
mvn clean verify
java -jar target/demo-pagination-offset-cursor-keyset-0.0.1-SNAPSHOT.jar
```

Sortie utile attendue (puis fermeture du contexte) :

```text
first=[30, 29, 28, 27, 26]
second=[25, 24, 23, 22, 21]
cursorPresent=true
```

L'exécution Java utilise H2 PostgreSQL-mode en mémoire. Elle ne nécessite pas Docker et ne mesure **pas** les performances PostgreSQL. Elle insère 30 lignes du tenant 1 avec timestamp identique. Les huit tests isolés couvrent parcours complet avec égalités, séparation des tenants, insertion avant frontière, suppression de l'ancre, page size/offset, expiration/mismatch/tampering HMAC, précision microseconde et omission après modification de clé de tri.

## Sources et contrat

- [FeedRepository.java](src/main/java/com/example/pagination/FeedRepository.java) : JDBC, offset et tuple seek `(created_at,id)<(?,?)`, tri DESC sur les deux clés.
- [PaginationService.java](src/main/java/com/example/pagination/PaginationService.java) : limite 1..100, lookahead size+1, curseur construit depuis la dernière ligne **rendue**, pas la ligne supplémentaire.
- [CursorCodec.java](src/main/java/com/example/pagination/CursorCodec.java) : Base64url + HMAC SHA256, version v1, tenant, taille, expiration 15 min et frontière. Version v1 définit l'ordre fixe ; aucune option de tri libre.
- [DemoApplication.java](src/main/java/com/example/pagination/DemoApplication.java) : runner sans endpoint HTTP.
- [PaginationTest.java](src/test/java/com/example/pagination/PaginationTest.java) : preuves de correction H2.
- [schema.sql](src/main/resources/schema.sql) : clés NOT NULL, précision 6, index `(tenant_id,created_at DESC,id DESC)`.

Le timestamp est un TIMESTAMP sans zone, traité comme une convention UTC par le domaine. Ne pas mélanger des heures locales sans normalisation. L'identifiant est unique au sein d'un tenant. Le keyset exige l'ordre total, la présence de toutes les clés dans le token et des clés stables pour éviter les sauts liés aux déplacements.

Le token est opaque par contrat, **pas chiffré**. Le client doit le retransmettre sans interpréter ses champs. Le secret intégré au runner est public et réservé à la démonstration. En API réelle : secret privé stable partagé entre instances, rotation/version de clé, autorisation du tenant indépendamment du cursor, adaptation du token à chaque filtre/ordre supporté et erreurs HTTP sans détails internes. Ici aucun filtre additionnel n'existe : ne pas ajouter un filtre sans étendre le contrat. L'expiration est renouvelée à chaque continuation, ce n'est pas une durée absolue de session.

Aucun snapshot multi-page n'est promis. Sous des lectures distinctes, une insertion/suppression avant la frontière décale offset ; keyset conserve une frontière de valeurs même si la ligne-ancre disparaît. Une ligne dont la clé de tri est modifiée peut tout de même être omise ou revue. Le test de mutation le démontre. La signature empêche la falsification sous un secret privé, elle ne donne ni autorisation ni stabilité transactionnelle.

## Reproduire les plans PostgreSQL

Docker doit être démarré. Le benchmark utilise une base jetable dédiée et ne publie aucun port. Le script réinitialise uniquement tenant 42 : ne pas l'exécuter dans une base contenant des données utiles. Depuis le dossier de ce module :

```bash
docker run --name pagination-bench --rm -d -e POSTGRES_PASSWORD=local-demo-only -e POSTGRES_DB=pagination postgres:17.9
docker exec pagination-bench pg_isready -U postgres -d pagination
```

Attendre que pg_isready annonce « accepting connections », puis :

```bash
docker exec -i pagination-bench psql -U postgres -d pagination < sql/benchmark.sql > measurements/postgresql-local.txt
docker stop pagination-bench
```

Le script initialise 1 M lignes, crée l'index, fait VACUUM ANALYZE, puis capture 3 paires de plans `EXPLAIN (ANALYZE,BUFFERS,SETTINGS)` et vérifie l'égalité des pages. Il met intentionnellement en évidence offset 900000 contre une frontière déjà connue. Cette frontière représente la continuation d'une page précédente ; elle ne rend pas gratuit le saut direct à une page arbitraire. L'accès exact au rang 900000 nécessiterait de trouver cette frontière autrement.

Les [résultats mesurés](measurements/README.md) et la [trace brute](measurements/postgresql-17.9.txt) sont inclus. Résultat structurel constaté : Index Scan émet 900050 lignes côté offset, 50 côté keyset. Les durées de ce poste et les états de cache ne sont pas une promesse de production. Les plans portent sur LIMIT 50, contre LIMIT 51 en Java pour vérifier la présence d'une suite.

La voie SQL PostgreSQL est exécutée réellement ; le runner JDBC et ses tests restent vérifiés sur H2. Ce n'est pas une suite d'intégration Java/PostgreSQL. Le pilote PostgreSQL est inclus pour permettre cette extension, mais changer de moteur doit être accompagné de tests d'intégration adaptés.

## Limites et choix

Offset garde son intérêt pour petits jeux, administration et accès direct aux numéros de pages. Keyset convient aux parcours séquentiels profonds avec index adapté. Aucun COUNT exact n'est exécuté ; le nombre total et la navigation arrière constituent des contrats supplémentaires. Les jointures, tris mixtes ASC/DESC, valeurs NULL, recherche textuelle et filtres additionnels demandent un autre prédicat/index et des tests spécifiques.

Diagnostic : `release version 21 not supported` indique un mauvais JDK Maven (`mvn -version`). Un moteur Docker inaccessible n'empêche pas les tests H2 mais empêche de reproduire les mesures PG. Le warning de connexion ne doit pas être interprété comme un résultat de performance.

Validation : OpenJDK 21.0.10, Maven 3.9.9, huit tests réussis, JAR exécuté, protocole PostgreSQL 17.9 exécuté et résultats sauvegardés.
