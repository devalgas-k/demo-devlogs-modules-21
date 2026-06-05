# demo-spring-beans-2026

Objectif : illustrer un “cas non optimisé” vs “cas optimisé” autour des beans Spring, en particulier la différence entre :

- `@Configuration` (par défaut, `proxyBeanMethods = true`) : classe “enhanced” via CGLIB pour garantir la sémantique singleton quand on appelle des méthodes `@Bean` entre elles.
- `@Configuration(proxyBeanMethods = false)` : pas d’enhancement, démarrage potentiellement plus léger, mais interdit de compter sur les appels `@Bean()` “entre méthodes”.

## Pré-requis

- Java 21
- Maven

## Build & tests

```bash
mvn -f ../pom.xml -pl demo-spring-beans-2026 test
```

## Run (3 modes)

### Mode A — “slow” (proxyBeanMethods=true)

```bash
mvn -f ../pom.xml -pl demo-spring-beans-2026 spring-boot:run -Dspring-boot.run.profiles=slow
```

### Mode B — “fast” (proxyBeanMethods=false)

```bash
mvn -f ../pom.xml -pl demo-spring-beans-2026 spring-boot:run -Dspring-boot.run.profiles=fast
```

### Mode C — “broken” (anti-pattern : appel inter-@Bean en lite mode)

```bash
mvn -f ../pom.xml -pl demo-spring-beans-2026 spring-boot:run -Dspring-boot.run.profiles=broken
```

## Endpoint de vérification

Une fois démarré :

```bash
curl -s http://localhost:8080/api/beans | jq .
```

Sans `jq` :

```bash
curl -s http://localhost:8080/api/beans
```

Tu dois observer :

- en `slow`, la classe de config contient généralement `$$SpringCGLIB$$`
- en `fast`, la classe de config n’est pas enhanced (pas de `$$SpringCGLIB$$`)
- en `broken`, la classe de config n’est pas enhanced, et `repositorySameInstance=false`

## Détecter un bean “lazy” (démarrage retardé)

Le bean `expensiveBean` est déclaré `@Lazy`, donc il ne doit pas être instancié au démarrage.

Vérifier l’état sans le créer :

```bash
curl -s "http://localhost:8080/api/beans/expensive" | jq .
```

Déclencher l’instanciation :

```bash
curl -s "http://localhost:8080/api/beans/expensive?create=true" | jq .
```
