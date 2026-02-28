# Plan de Desarrollo — Sistema de Inscripción a Eventos Universitarios
**Versión:** 1.0
**Fecha:** 2026-02-27
**Referencia:** `spec.md` v1.0

---

## Convenciones del plan

- **[T-XX]** Identificador único de tarea
- **Complejidad:** S (< 1h) · M (1–2h) · L (2–4h)
- **Criterio de done (DoD):** condición verificable que indica que la tarea está completada
- **Bloqueada por:** tareas que deben estar completas antes de iniciar

---

## Visión general de fases

```
FASE 0 ──► FASE 1 ──► FASE 2 ──┐
                      FASE 3 ──┼──► FASE 4 (imperativo) ──┐
                                │                           ├──► FASE 6 ──► FASE 7 ──► FASE 8
                                └──► FASE 5 (funcional) ───┘
```

| Fase | Nombre | Capa | Depende de |
|------|--------|------|------------|
| 0 | Configuración del proyecto | Infraestructura (setup) | — |
| 1 | Capa de dominio | Domain | F0 |
| 2 | Adaptadores de persistencia (JPA) | Infrastructure / Persistence | F1 |
| 3 | Adaptador de mensajería (Audit) | Infrastructure / Messaging | F1 |
| 4 | Servicios de aplicación — Imperativo | Application | F2, F3 |
| 5 | Servicios de aplicación — Funcional | Application | F2, F3 |
| 6 | Capa web (DTOs + Controllers + Handler) | Infrastructure / Web | F4, F5 |
| 7 | Configuración y datos iniciales | Config | F2, F3, F4, F5, F6 |
| 8 | Pruebas unitarias e integración | Test | F4, F5, F6 |

---

## FASE 0 — Configuración del Proyecto

> Objetivo: tener el proyecto compilando con Spring Boot 3.x y H2 configurado.

### [T-01] Actualizar build.gradle

**Complejidad:** S
**Descripción:**
Reemplazar el `build.gradle` actual (solo JUnit 5 básico) con la configuración
completa de Spring Boot 3.x, Lombok, H2, JPA, Actuator y Validation.

**Cambios:**
```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.x'
    id 'io.spring.dependency-management' version '1.1.x'
}
java { sourceCompatibility = JavaVersion.VERSION_17 }
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    runtimeOnly 'com.h2database:h2'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

**DoD:** `./gradlew build` compila sin errores.

---

### [T-02] Configurar application.properties

**Complejidad:** S
**Descripción:**
Crear `src/main/resources/application.properties` con configuración de H2
en memoria, JPA, consola H2, Actuator y niveles de log.

**Contenido clave:**
```properties
# H2 in-memory
spring.datasource.url=jdbc:h2:mem:inscripciones;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.h2.console.enabled=true

# JPA
spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false

# Actuator
management.endpoints.web.exposure.include=health,metrics
management.endpoint.health.show-details=always

# Logging — solo WARN en general, INFO para auditoría
logging.level.root=WARN
logging.level.com.example=INFO
logging.level.com.example.infrastructure.messaging=INFO
```

**DoD:** La aplicación arranca y `/actuator/health` responde `UP`.

---

### [T-03] Crear clase principal de la aplicación

**Complejidad:** S
**Descripción:**
Crear `InscripcionEventosApplication.java` con `@SpringBootApplication`.

**DoD:** `./gradlew bootRun` inicia el servidor en el puerto 8080.
**Bloqueada por:** T-01, T-02

---

## FASE 1 — Capa de Dominio

> Objetivo: definir el núcleo del sistema sin ninguna dependencia de framework.
> Esta capa debe compilar con solo Java 17 (sin Spring, sin JPA).

### [T-04] Crear modelos de dominio

**Complejidad:** M
**Descripción:**
Crear las clases de dominio puras en `com.example.domain.model`.

**Clases:**
- `Event` — id, name, maxCapacity, availableSpots, eventDate
- `Participant` — id, name, email
- `Enrollment` — id, eventId, participantId, enrolledAt, status, version
- `EnrollmentStatus` (enum) — `ACTIVE`, `CANCELLED`

**Reglas:**
- Sin anotaciones de JPA ni Spring.
- Constructores completos + getters. Lombok `@Value` o records Java 17
  pueden usarse si simplifican sin agregar dependencias de framework.
- Javadoc en cada clase describiendo su responsabilidad de dominio.

**DoD:** Las clases compilan. No hay import de `javax.*`, `jakarta.*`,
`org.springframework.*` en este paquete.

**Bloqueada por:** T-01

---

### [T-05] Crear excepciones de dominio

**Complejidad:** S
**Descripción:**
Crear en `com.example.domain.exception`:
- `EventNotFoundException`
- `EventFullException`
- `DuplicateEnrollmentException`
- `EnrollmentNotFoundException`
- `ParticipantNotFoundException`

Todas extienden `RuntimeException`. Llevan el código de error como campo
(`String errorCode`) para que el handler las mapee sin switch.

```java
public class EventFullException extends RuntimeException {
    private final String errorCode = "EVENT_FULL";
    // ...
}
```

**DoD:** Cinco excepciones creadas, sin dependencias de framework.
**Bloqueada por:** T-01

---

### [T-06] Definir puertos de entrada (in ports)

**Complejidad:** M
**Descripción:**
Crear en `com.example.domain.port.in` una interfaz por caso de uso:

| Interfaz | Método |
|----------|--------|
| `EnrollParticipantPort` | `EnrollmentResponse enroll(EnrollmentRequest request)` |
| `CancelEnrollmentPort` | `void cancel(UUID enrollmentId)` |
| `QueryAvailableSpotsPort` | `SpotsResponse querySpots(UUID eventId)` |
| `ListEnrolledParticipantsPort` | `ParticipantListResponse listParticipants(UUID eventId)` |

**Nota:** Los tipos de retorno en los puertos de entrada pueden referirse a
objetos de respuesta simples del dominio (no DTOs web), o directamente a los
DTOs si se decide mantener los DTOs agnósticos de framework. Definir en esta
tarea qué enfoque se adopta y documentarlo con Javadoc.

**DoD:** Cuatro interfaces creadas con Javadoc explicando el contrato.
**Bloqueada por:** T-04, T-05

---

### [T-07] Definir puertos de salida (out ports)

**Complejidad:** M
**Descripción:**
Crear en `com.example.domain.port.out`:

| Interfaz | Métodos clave |
|----------|--------------|
| `EventRepositoryPort` | `Optional<Event> findById(UUID id)` · `Event save(Event event)` |
| `ParticipantRepositoryPort` | `Optional<Participant> findById(UUID id)` |
| `EnrollmentRepositoryPort` | `Enrollment save(Enrollment e)` · `Optional<Enrollment> findById(UUID id)` · `List<Enrollment> findByEventIdAndStatus(UUID eventId, EnrollmentStatus s)` · `boolean existsByEventIdAndParticipantIdAndStatus(UUID eId, UUID pId, EnrollmentStatus s)` |
| `AuditEventPublisherPort` | `void publish(Object auditEvent)` |

**Regla:** Ninguna interfaz importa clases de JPA, Spring o H2.

**DoD:** Cuatro interfaces compilando sin dependencias de framework.
**Bloqueada por:** T-04

---

## FASE 2 — Adaptadores de Persistencia (JPA)

> Objetivo: implementar los puertos de salida usando Spring Data JPA + H2.
> El dominio no sabe nada de esta fase.

### [T-08] Crear entidades JPA

**Complejidad:** M
**Descripción:**
Crear en `com.example.infrastructure.persistence.entity`:
- `EventEntity` — `@Entity`, `@Table("events")`, campos mapeados, `@Version Long version`
- `ParticipantEntity` — `@Entity`, `@Table("participants")`, constraint `@Column(unique=true)` en email
- `EnrollmentEntity` — `@Entity`, `@Table("enrollments")`, `@Version Long version`,
  constraint única sobre `(event_id, participant_id)` cuando `status = ACTIVE`
  (modelada con `@UniqueConstraint` o validación en servicio)

**Regla:** Entidades JPA **nunca** se exponen fuera del paquete de persistence.
Conversión a/desde dominio mediante métodos estáticos `toDomain()` / `fromDomain()`.

**DoD:** H2 crea las tablas al arrancar (`ddl-auto=create-drop`). Se verifica
en la consola H2 (`/h2-console`).

**Bloqueada por:** T-04, T-02

---

### [T-09] Crear Spring Data JPA repositories

**Complejidad:** S
**Descripción:**
Crear en `com.example.infrastructure.persistence.repository`:
- `SpringEventRepository extends JpaRepository<EventEntity, UUID>`
- `SpringParticipantRepository extends JpaRepository<ParticipantEntity, UUID>`
- `SpringEnrollmentRepository extends JpaRepository<EnrollmentEntity, UUID>`
  - Agregar: `List<EnrollmentEntity> findByEventIdAndStatus(UUID eventId, EnrollmentStatus status)`
  - Agregar: `boolean existsByEventIdAndParticipantIdAndStatus(UUID, UUID, EnrollmentStatus)`

**DoD:** Interfaces creadas. Spring genera las queries automáticamente (verificable con logs JPA en modo debug puntual).
**Bloqueada por:** T-08

---

### [T-10] Crear adaptadores JPA (implementan puertos de salida)

**Complejidad:** M
**Descripción:**
Crear en `com.example.infrastructure.persistence.adapter`:
- `EventJpaAdapter implements EventRepositoryPort`
- `ParticipantJpaAdapter implements ParticipantRepositoryPort`
- `EnrollmentJpaAdapter implements EnrollmentRepositoryPort`

Cada adaptador recibe el Spring Data repo por constructor (DIP), convierte
entre entidad JPA y modelo de dominio usando los mappers de T-08.

```java
@Component
public class EventJpaAdapter implements EventRepositoryPort {
    private final SpringEventRepository jpaRepository;
    // constructor injection
}
```

**DoD:** Los tres adaptadores implementan todos los métodos de sus puertos.
Compilan sin errores. Javadoc en cada clase.
**Bloqueada por:** T-07, T-09

---

## FASE 3 — Adaptador de Mensajería (Audit)

> Objetivo: desacoplar la auditoría del dominio usando Spring Application Events
> de forma síncrona. El dominio solo conoce `AuditEventPublisherPort`.

### [T-11] Crear eventos de auditoría

**Complejidad:** S
**Descripción:**
Crear en `com.example.infrastructure.messaging.event` tres clases record/POJO:
- `EnrollmentCreatedAuditEvent` — eventId, participantId, spotsRemaining, timestamp
- `EnrollmentCancelledAuditEvent` — enrollmentId, eventId, spotsRecovered, timestamp
- `SpotsQueriedAuditEvent` — eventId, availableSpots, timestamp

**DoD:** Tres clases inmutables creadas (records Java 17 preferibles).
**Bloqueada por:** T-04

---

### [T-12] Crear SpringAuditEventPublisher

**Complejidad:** S
**Descripción:**
Crear `SpringAuditEventPublisher implements AuditEventPublisherPort` en
`com.example.infrastructure.messaging`.

Delega en `ApplicationEventPublisher` de Spring (inyectado por constructor).
El método `publish(Object event)` llama `applicationEventPublisher.publishEvent(event)`.

**DoD:** Implementa el puerto. No hay lógica de negocio aquí.
**Bloqueada por:** T-07, T-11

---

### [T-13] Crear AuditEventListener

**Complejidad:** S
**Descripción:**
Crear `AuditEventListener` en `com.example.infrastructure.messaging`.
Usa `@EventListener` (síncrono) para cada tipo de evento de auditoría.

```java
@EventListener
public void onEnrollmentCreated(EnrollmentCreatedAuditEvent event) {
    log.info("[AUDIT] action=ENROLLMENT_CREATED | event={} | participant={} | spots_remaining={}",
        event.eventId(), event.participantId(), event.spotsRemaining());
}
```

Formato de log según `spec.md §9.1`. Solo `INFO` para eventos exitosos,
`WARN` para cancelaciones, `ERROR` no aplica aquí (los errores se loguean
en el GlobalExceptionHandler).

**DoD:** Tres métodos `@EventListener`. Los logs aparecen en consola al
ejecutar manualmente una inscripción.
**Bloqueada por:** T-11

---

## FASE 4 — Servicios de Aplicación: Imperativo

> Objetivo: implementar los 4 casos de uso con estilo imperativo
> (bucles, condicionales explícitos, variables mutables locales).

### [T-14] ImperativeEnrollmentService (CU-01)

**Complejidad:** L
**Descripción:**
Implementa `EnrollParticipantPort`.

**Flujo imperativo:**
```java
// 1. Buscar evento con if (optional.isEmpty()) throw
// 2. Buscar participante con if
// 3. if (repo.existsByEventIdAndParticipantIdAndStatus(...)) throw DuplicateEnrollmentException
// 4. if (event.getAvailableSpots() <= 0) throw EventFullException
// 5. Decrementar availableSpots con setter explícito
// 6. eventRepo.save(event)
// 7. Construir Enrollment con new + setters
// 8. enrollmentRepo.save(enrollment)
// 9. publisher.publish(new EnrollmentCreatedAuditEvent(...))
// 10. Construir y retornar EnrollmentResponse
```

`@Transactional(isolation = Isolation.SERIALIZABLE)` en el método principal.
Captura de `ObjectOptimisticLockingFailureException` en bloque catch → relanza
como respuesta estructurada (o se deja para GlobalExceptionHandler).

**DoD:** Implementa todos los pasos. Javadoc en la clase y el método.
**Bloqueada por:** T-07, T-10, T-12

---

### [T-15] ImperativeCancelEnrollmentService (CU-02)

**Complejidad:** M
**Descripción:**
Implementa `CancelEnrollmentPort`.

**Flujo imperativo:**
```java
// 1. Buscar enrollment por id, throw EnrollmentNotFoundException si no existe
// 2. Cambiar status a CANCELLED con setter
// 3. Buscar evento, incrementar availableSpots con setter
// 4. eventRepo.save + enrollmentRepo.save dentro de @Transactional
// 5. publisher.publish(EnrollmentCancelledAuditEvent)
```

**DoD:** Cancela y restaura cupos. Transaccional.
**Bloqueada por:** T-07, T-10, T-12

---

### [T-16] ImperativeQuerySpotsService (CU-03)

**Complejidad:** S
**Descripción:**
Implementa `QueryAvailableSpotsPort`.

```java
// 1. Buscar evento con if
// 2. Construir SpotsResponse manualmente
// 3. publisher.publish(SpotsQueriedAuditEvent)
```

**DoD:** Retorna SpotsResponse. No es transaccional (lectura pura).
**Bloqueada por:** T-07, T-10, T-12

---

### [T-17] ImperativeListParticipantsService (CU-04)

**Complejidad:** S
**Descripción:**
Implementa `ListEnrolledParticipantsPort`.

```java
// 1. Validar existencia del evento
// 2. List<Enrollment> enrollments = enrollmentRepo.findByEventIdAndStatus(id, ACTIVE)
// 3. Bucle for: por cada enrollment buscar Participant, construir DTO, agregar a lista
// 4. Retornar ParticipantListResponse con lista y total
```

**DoD:** Retorna solo inscritos ACTIVE. Javadoc explicando la diferencia
con la versión funcional.
**Bloqueada por:** T-07, T-10

---

## FASE 5 — Servicios de Aplicación: Funcional

> Objetivo: reimplementar los mismos 4 casos de uso con estilo declarativo
> usando Streams, Optionals y lambdas. Mismos puertos de salida, diferente estilo.

### [T-18] FunctionalEnrollmentService (CU-01)

**Complejidad:** L
**Descripción:**
Implementa `EnrollParticipantPort` con estilo funcional.

**Diferencias respecto a imperativo:**
```java
// Buscar evento: eventRepo.findById(id).orElseThrow(() -> new EventNotFoundException(...))
// Validar cupos: Optional.of(event).filter(e -> e.getAvailableSpots() > 0).orElseThrow(...)
// Construir enrollment: Enrollment.builder()...build()  (Builder pattern)
// Construir respuesta: EnrollmentResponse.builder()...build()
```

`@Transactional(isolation = Isolation.SERIALIZABLE)` igual que imperativo.
La lógica de negocio es idéntica; solo el estilo de procesamiento difiere.

**DoD:** No hay `if` ni `for` explícitos en el flujo principal. Usa
`Optional`, `Stream`, lambdas y Builder.
**Bloqueada por:** T-07, T-10, T-12

---

### [T-19] FunctionalCancelEnrollmentService (CU-02)

**Complejidad:** M
**Descripción:**
Implementa `CancelEnrollmentPort` con estilo funcional.

```java
// enrollmentRepo.findById(id).orElseThrow(...)
// Inmutabilidad: construir nuevo Enrollment con status CANCELLED via builder
```

**DoD:** Sin bucles ni condicionales imperativos en el flujo principal.
**Bloqueada por:** T-07, T-10, T-12

---

### [T-20] FunctionalQuerySpotsService (CU-03)

**Complejidad:** S
**Descripción:**
Implementa `QueryAvailableSpotsPort` con `orElseThrow` y Builder.

**DoD:** Una línea de retorno usando encadenamiento funcional.
**Bloqueada por:** T-07, T-10, T-12

---

### [T-21] FunctionalListParticipantsService (CU-04)

**Complejidad:** S
**Descripción:**
Implementa `ListEnrolledParticipantsPort` con Streams.

```java
// enrollments.stream()
//   .filter(e -> e.getStatus() == ACTIVE)
//   .map(e -> participantRepo.findById(e.getParticipantId()).orElseThrow(...))
//   .map(mapper::toDto)
//   .collect(Collectors.toList())
```

**DoD:** Sin bucles `for`. El conteo usa `.size()` sobre la lista resultante.
**Bloqueada por:** T-07, T-10

---

## FASE 6 — Capa Web

> Objetivo: exponer los casos de uso como API REST con los dos prefijos
> (imperative / functional), manejo centralizado de errores y DTOs con Builder.

### [T-22] Crear DTOs con @Builder

**Complejidad:** M
**Descripción:**
Crear en `com.example.infrastructure.web.dto`:

**Request:**
- `EnrollmentRequest` — `@NotNull UUID eventId`, `@NotNull UUID participantId`

**Response:**
- `EnrollmentResponse` — enrollmentId, eventId, participantId, enrolledAt, status
- `SpotsResponse` — eventId, eventName, maxCapacity, availableSpots
- `ParticipantListResponse` — `List<ParticipantDto> participants`, `int total`
- `ParticipantDto` — participantId, name, email
- `ErrorResponse` — code, message, timestamp, path

Todos los DTOs de respuesta usan `@Builder` de Lombok.
`EnrollmentRequest` usa `@Builder` + `@NotNull` de Jakarta Validation.

**DoD:** Todos los DTOs compilan con Lombok. `ErrorResponse` incluye
un método estático de fábrica `ErrorResponse.of(String code, String message, String path)`.
**Bloqueada por:** T-01

---

### [T-23] Crear GlobalExceptionHandler

**Complejidad:** M
**Descripción:**
Crear `GlobalExceptionHandler` con `@RestControllerAdvice` en
`com.example.infrastructure.web.handler`.

**Mapeo de excepciones:**

| Excepción | HTTP | Log |
|-----------|------|-----|
| `EventNotFoundException` | 404 | `WARN` |
| `EnrollmentNotFoundException` | 404 | `WARN` |
| `ParticipantNotFoundException` | 404 | `WARN` |
| `EventFullException` | 409 | `WARN` |
| `DuplicateEnrollmentException` | 409 | `WARN` |
| `ObjectOptimisticLockingFailureException` | 409 (`OPTIMISTIC_LOCK_CONFLICT`) | `ERROR` |
| `MethodArgumentNotValidException` | 400 | `WARN` |
| `Exception` (catch-all) | 500 | `ERROR` con stack trace |

**Regla:** Es el **único** lugar donde se hace log de errores de negocio.
Los servicios no loguean errores, solo los lanzan.

**DoD:** Todos los handlers retornan `ErrorResponse` con el formato de
`spec.md §10`. Tests de unidad en T-30 lo verificarán.
**Bloqueada por:** T-05, T-22

---

### [T-24] Crear controladores imperativos

**Complejidad:** M
**Descripción:**
Crear en `com.example.infrastructure.web.imperative`:

| Controlador | Endpoint |
|-------------|----------|
| `ImperativeEnrollmentController` | `POST /api/v1/imperative/enrollments` → 201 |
| `ImperativeCancelController` | `DELETE /api/v1/imperative/enrollments/{id}` → 204 |
| `ImperativeSpotsController` | `GET /api/v1/imperative/events/{id}/spots` → 200 |
| `ImperativeParticipantsController` | `GET /api/v1/imperative/events/{id}/participants` → 200 |

Cada controlador recibe su puerto de entrada por constructor (DIP). No
contiene lógica de negocio ni manejo de excepciones (delegado al handler).

**DoD:** Cuatro controladores. Javadoc en clase y métodos.
**Bloqueada por:** T-14, T-15, T-16, T-17, T-22

---

### [T-25] Crear controladores funcionales

**Complejidad:** S
**Descripción:**
Ídem T-24 pero en `com.example.infrastructure.web.functional` usando los
servicios funcionales. La estructura es idéntica; solo cambia el puerto
inyectado.

**DoD:** Cuatro controladores funcionales con prefijo `/api/v1/functional/`.
**Bloqueada por:** T-18, T-19, T-20, T-21, T-22

---

## FASE 7 — Configuración y Datos Iniciales

### [T-26] Crear BeanConfiguration (wiring de puertos)

**Complejidad:** M
**Descripción:**
Crear `BeanConfiguration` en `com.example.config` con `@Configuration`.

Registrar explícitamente los beans que Spring no detecta automáticamente o
que requieren qualifier para distinguir implementaciones (imperativa vs funcional).

```java
@Bean
@Qualifier("imperativeEnroll")
public EnrollParticipantPort imperativeEnrollmentService(
        EventRepositoryPort eventRepo,
        EnrollmentRepositoryPort enrollmentRepo,
        ParticipantRepositoryPort participantRepo,
        AuditEventPublisherPort publisher) {
    return new ImperativeEnrollmentService(eventRepo, enrollmentRepo, participantRepo, publisher);
}

@Bean
@Qualifier("functionalEnroll")
public EnrollParticipantPort functionalEnrollmentService(...) { ... }
```

Los controladores usan `@Qualifier` para inyectar la implementación correcta.

**DoD:** La aplicación arranca sin errores de `NoUniqueBeanDefinitionException`.
Todos los puertos tienen exactamente un bean registrado por qualifier.
**Bloqueada por:** T-10, T-12, T-14, T-15, T-16, T-17, T-18, T-19, T-20, T-21

---

### [T-27] Crear DataInitializer

**Complejidad:** S
**Descripción:**
Crear `DataInitializer implements CommandLineRunner` en `com.example.config`.
Inserta en H2 al arrancar:
- 2 eventos con capacidades distintas (50 y 10 cupos)
- 3 participantes con emails institucionales

Permite probar la API inmediatamente sin necesidad de scripts SQL externos.

**DoD:** Al arrancar la app, los UUIDs de eventos y participantes se imprimen
en consola (`log.info`) para usarlos en pruebas manuales con curl/Postman.
**Bloqueada por:** T-09, T-03

---

## FASE 8 — Pruebas

> Objetivo: verificar el comportamiento de cada caso de uso con los escenarios
> Gherkin definidos en `spec.md §12`. Nomenclatura de métodos:
> `should[Resultado]When[Condición]`.

### [T-28] Tests unitarios — servicios imperativos

**Complejidad:** L
**Descripción:**
Crear `ImperativeEnrollmentServiceTest`, `ImperativeCancelServiceTest`,
`ImperativeQuerySpotsServiceTest`, `ImperativeListParticipantsServiceTest`.

Usar Mockito para mockear todos los puertos de salida.
Cubrir los escenarios Gherkin de `spec.md §12`:

| Escenario | Servicio | Verificación |
|-----------|----------|-------------|
| Inscripción exitosa | Enroll | `verify(enrollmentRepo).save(any())` |
| Sin cupos → 409 | Enroll | `assertThrows(EventFullException.class, ...)` |
| Duplicado → 409 | Enroll | `assertThrows(DuplicateEnrollmentException.class, ...)` |
| Cancelación exitosa | Cancel | `verify(eventRepo).save(argThat(e -> e.getAvailableSpots() == inicial + 1))` |
| No encontrada → 404 | Cancel | `assertThrows(EnrollmentNotFoundException.class, ...)` |
| Cupos correctos | QuerySpots | `assertEquals(expectedSpots, response.availableSpots())` |
| Evento no existe | QuerySpots | `assertThrows(EventNotFoundException.class, ...)` |
| Lista activos únicamente | ListParticipants | `assertEquals(3, response.total())` para 3 ACTIVE + 1 CANCELLED |
| Lista vacía | ListParticipants | `assertEquals(0, response.total())` |

Estructura comentada en Given/When/Then dentro de cada test.

**DoD:** Todos los tests pasan. Cobertura de ramas en servicios imperativos ≥ 80%.
**Bloqueada por:** T-14, T-15, T-16, T-17

---

### [T-29] Tests unitarios — servicios funcionales

**Complejidad:** M
**Descripción:**
Crear `FunctionalEnrollmentServiceTest` y equivalentes. Los escenarios son
**idénticos** a T-28 (mismo contrato, diferente implementación).

Objetivo adicional: confirmar que el comportamiento observable es el mismo
en ambas implementaciones para el mismo input.

**DoD:** Todos los escenarios de T-28 reproducidos para la versión funcional.
**Bloqueada por:** T-18, T-19, T-20, T-21

---

### [T-30] Tests unitarios — GlobalExceptionHandler

**Complejidad:** M
**Descripción:**
Crear `GlobalExceptionHandlerTest` usando `MockMvc` standalone.

Verificar que cada excepción de dominio genera el código HTTP y el
`ErrorResponse.code` correcto:

| Input | HTTP esperado | code esperado |
|-------|--------------|---------------|
| `EventNotFoundException` | 404 | `EVENT_NOT_FOUND` |
| `EventFullException` | 409 | `EVENT_FULL` |
| `DuplicateEnrollmentException` | 409 | `DUPLICATE_ENROLLMENT` |
| `ObjectOptimisticLockingFailureException` | 409 | `OPTIMISTIC_LOCK_CONFLICT` |
| `MethodArgumentNotValidException` | 400 | `VALIDATION_ERROR` |

**DoD:** Cinco tests pasan. El cuerpo de respuesta coincide con el schema
de `ErrorResponse`.
**Bloqueada por:** T-23

---

### [T-31] Tests de integración — adaptadores JPA

**Complejidad:** M
**Descripción:**
Crear `EnrollmentJpaAdapterTest` con `@DataJpaTest` (H2 en memoria, sin levantar
todo el contexto Spring).

Escenarios:
- Guardar y recuperar `EnrollmentEntity` con `@Version`
- `existsByEventIdAndParticipantIdAndStatus` retorna `true` para inscripción existente
- `findByEventIdAndStatus` filtra correctamente por ACTIVE vs CANCELLED
- Conflicto de `@Version`: actualizar la misma entidad dos veces con la misma
  versión lanza `ObjectOptimisticLockingFailureException`

**DoD:** Cuatro tests pasan. El test de conflicto de versión valida el
comportamiento de concurrencia sin necesidad de hilos reales.
**Bloqueada por:** T-09, T-10

---

## Diagrama de dependencias completo

```
T-01 (build.gradle)
  └──► T-02 (application.properties)
         └──► T-03 (Main app)          ◄──── T-08
T-01 ──► T-04 (domain models)
           ├──► T-05 (exceptions)
           ├──► T-06 (in ports)
           ├──► T-07 (out ports)
           │      ├──► T-10 (JPA adapters)  ◄── T-09 ◄── T-08
           │      ├──► T-12 (AuditPublisher) ◄── T-11 ◄── T-04
           │      └──► T-13 (AuditListener)  ◄── T-11
           └──► T-11 (audit events)

T-10 + T-12 ──► T-14..T-17 (imperative services)
T-10 + T-12 ──► T-18..T-21 (functional services)

T-22 (DTOs)
T-05 + T-22 ──► T-23 (GlobalExceptionHandler)

T-14..T-17 + T-22 ──► T-24 (imperative controllers)
T-18..T-21 + T-22 ──► T-25 (functional controllers)

T-10 + T-12..T-21 ──► T-26 (BeanConfiguration)
T-09 + T-03 ──► T-27 (DataInitializer)

T-14..T-17 ──► T-28 (unit tests imperative)
T-18..T-21 ──► T-29 (unit tests functional)
T-23        ──► T-30 (exception handler tests)
T-09 + T-10 ──► T-31 (JPA integration tests)
```

---

## Resumen de tareas por fase

| Fase | Tareas | Complejidad total |
|------|--------|-------------------|
| F0 — Setup | T-01, T-02, T-03 | S+S+S |
| F1 — Dominio | T-04, T-05, T-06, T-07 | M+S+M+M |
| F2 — Persistencia JPA | T-08, T-09, T-10 | M+S+M |
| F3 — Mensajería Audit | T-11, T-12, T-13 | S+S+S |
| F4 — App Imperativo | T-14, T-15, T-16, T-17 | L+M+S+S |
| F5 — App Funcional | T-18, T-19, T-20, T-21 | L+M+S+S |
| F6 — Web | T-22, T-23, T-24, T-25 | M+M+M+S |
| F7 — Config | T-26, T-27 | M+S |
| F8 — Tests | T-28, T-29, T-30, T-31 | L+M+M+M |
| **Total** | **31 tareas** | |

---

## Criterios de aceptación del proyecto completo

- [ ] `./gradlew build` pasa sin errores ni warnings de compilación
- [ ] `./gradlew test` — todos los tests en verde
- [ ] `POST /api/v1/imperative/enrollments` y `POST /api/v1/functional/enrollments` producen el mismo resultado para el mismo input
- [ ] Dos peticiones concurrentes sobre el último cupo: exactamente una termina en 201 y la otra en 409 `OPTIMISTIC_LOCK_CONFLICT`
- [ ] `GET /actuator/health` responde `{"status":"UP"}`
- [ ] Los logs de auditoría aparecen en consola con el formato `[AUDIT] action=...`
- [ ] El dominio (`com.example.domain`) no importa ninguna clase de `org.springframework` ni `jakarta.persistence`

---

*Este plan cubre todos los drivers arquitectónicos definidos en `spec.md §2`.
La implementación puede comenzar desde F0 y avanzar fase por fase siguiendo
las dependencias indicadas.*
