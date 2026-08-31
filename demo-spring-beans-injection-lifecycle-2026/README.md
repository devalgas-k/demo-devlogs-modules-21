# demo-spring-beans-injection-lifecycle-2026

## Objectif
Ce démo illustre des cas concrets autour des beans Spring Boot :
- création (`@Component`, `@Configuration` + `@Bean`)
- injection (constructeur vs injection pilotée par `ObjectProvider`)
- configuration (`@ConditionalOnProperty`)
- cycle de vie (init coûteuse, `@Lazy`, pénalité de “first hit”)
- désambiguïsation (`@Primary` / `@Qualifier`)
- modularisation (`@Import`)
- scopes (`@Scope("prototype")` + piège prototype injecté dans un singleton)
- extension container (`BeanFactoryPostProcessor` vs `BeanPostProcessor`)
- `FactoryBean` (bean produit vs factory via `&beanName`)

## Prérequis
- Java 21
- Maven

## Build & tests
Depuis la racine du repo :

```bash
mvn -f projets/pom.xml -pl demo-spring-beans-injection-lifecycle-2026 -am test
```

## Exécution (mode non optimisé vs optimisé)

### Mode non optimisé (par défaut)
Le bean “cher” est instancié pendant le démarrage.

```bash
mvn -f projets/pom.xml -pl demo-spring-beans-injection-lifecycle-2026 -am spring-boot:run
```

### Mode optimisé
Le bean “cher” est `@Lazy` et instancié uniquement lors du premier appel.

```bash
mvn -f projets/pom.xml -pl demo-spring-beans-injection-lifecycle-2026 -am spring-boot:run -Dspring-boot.run.arguments="--demo.mode=fast"
```

### Mode optimisé + warmup (payer au démarrage, éviter le first hit)
```bash
mvn -f projets/pom.xml -pl demo-spring-beans-injection-lifecycle-2026 -am spring-boot:run -Dspring-boot.run.arguments="--demo.mode=fast --demo.warmup=true"
```

### Forcer `lazyInit` via BeanFactoryPostProcessor (démo BFPP)
```bash
mvn -f projets/pom.xml -pl demo-spring-beans-injection-lifecycle-2026 -am spring-boot:run -Dspring-boot.run.arguments="--demo.mode=slow --demo.forceLazyViaBfpp=true"
```

## Endpoints de démo
- `GET http://localhost:8080/api/mode`
- `GET http://localhost:8080/api/expensive/run`
- `GET http://localhost:8080/api/beans/init-timing?top=20`
- `GET http://localhost:8080/api/clock/fixed`
- `GET http://localhost:8080/api/imported/new-id`
- `GET http://localhost:8080/api/prototype/good`
- `GET http://localhost:8080/api/prototype/bad`
- `GET http://localhost:8080/api/factory-bean/token`
