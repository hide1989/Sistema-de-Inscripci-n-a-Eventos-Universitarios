# Especificación Técnica — Sistema de Inscripción a Eventos Universitarios
**Versión:** 1.0
**Fecha:** 2026-02-27
**Tipo:** Proof of Concept (POC)
**Stack:** Java 17 · Spring Boot 3.x · Gradle · H2 (in-memory)

---

## 1. Contexto y Problema

El sistema gestiona la inscripción de participantes a eventos universitarios.
Durante los primeros 10 minutos de apertura de cupos se producen picos de alta
concurrencia: múltiples usuarios intentan inscribirse al mismo tiempo, lo que
introduce riesgo de **sobrecupo** si no se controla la condición de carrera a
nivel de persistencia.

La POC no usa infraestructura externa. Toda la persistencia corre sobre H2
en memoria y los eventos de auditoría se propagan mediante Spring Application
Events de forma **síncrona**.

---

## 2. Drivers Arquitectónicos → Decisiones → Código

| # | Driver | Decisión arquitectónica | Mecanismo técnico |
|---|--------|------------------------|-------------------|
| 1 | **Escalabilidad** (picos de carga) | Controlar concurrencia en la capa de persistencia en lugar de introducir colas async que complican la consistencia | `@Transactional` + bloqueo optimista (`@Version`) en entidades JPA |
| 2 | **Consistencia de cupos** (no sobrecupo) | Lock optimista + validación de idempotencia antes de persistir | `@Version` en `EnrollmentEntity`; HTTP 409 en conflicto; validación de inscripción duplicada |
| 3 | **Mantenibilidad / extensibilidad** | Arquitectura hexagonal: dominio aislado de frameworks; principios SOLID explícitos | Puertos (interfaces) en dominio; adaptadores en infraestructura; DIP mediante IoC de Spring |
| 4 | **Observabilidad** (auditoría) | Log estructurado con SLF4J enfocado en errores y eventos de negocio; Spring Events para desacoplar auditoría del dominio; Actuator para métricas de sistema | `@EventListener` síncronos; `AuditEventPublisherPort`; `/actuator/health` y `/actuator/metrics` |

---

## 3. Arquitectura — Hexagonal (Puertos y Adaptadores)

```
┌─────────────────────────────────────────────────────────────────┐
│                        DRIVING SIDE                              │
│  REST Controllers (imperative / functional)                      │
│      ↓ llama a                                                   │
│  ┌───────────────────────────────────────────────────────────┐   │
│  │                   APPLICATION LAYER                        │   │
│  │  ImperativeEnrollmentService  │ FunctionalEnrollmentService│   │
│  │  (implementa puertos de entrada / casos de uso)           │   │
│  │  ┌─────────────────────────────────────────────────────┐  │   │
│  │  │                  DOMAIN LAYER                        │  │   │
│  │  │  Entidades · Value Objects · Excepciones de dominio  │  │   │
│  │  │  Puertos de entrada (in) · Puertos de salida (out)   │  │   │
│  │  └─────────────────────────────────────────────────────┘  │   │
│  │       ↓ usa puertos out (abstracciones)                   │   │
│  └───────────────────────────────────────────────────────────┘   │
│      ↓ implementado por                                          │
│  JPA Adapters · SpringEvents Audit Adapter                       │
│                        DRIVEN SIDE                               │
└─────────────────────────────────────────────────────────────────┘
```

### 3.1 Estructura de paquetes

```
com.example
├── domain
│   ├── model                        # Entidades y value objects (sin dependencias de framework)
│   │   ├── Event.java
│   │   ├── Participant.java
│   │   └── Enrollment.java
│   ├── port
│   │   ├── in                       # Puertos de entrada (casos de uso — interfaces)
│   │   │   ├── EnrollParticipantPort.java
│   │   │   ├── CancelEnrollmentPort.java
│   │   │   ├── QueryAvailableSpotsPort.java
│   │   │   └── ListEnrolledParticipantsPort.java
│   │   └── out                      # Puertos de salida (abstracciones de infraestructura)
│   │       ├── EventRepositoryPort.java
│   │       ├── EnrollmentRepositoryPort.java
│   │       ├── ParticipantRepositoryPort.java
│   │       └── AuditEventPublisherPort.java
│   └── exception                    # Excepciones de dominio tipadas
│       ├── EventNotFoundException.java
│       ├── EventFullException.java
│       ├── DuplicateEnrollmentException.java
│       └── EnrollmentNotFoundException.java
├── application
│   ├── imperative                   # Implementaciones imperativas de los puertos de entrada
│   │   ├── ImperativeEnrollmentService.java
│   │   ├── ImperativeCancelEnrollmentService.java
│   │   ├── ImperativeQuerySpotsService.java
│   │   └── ImperativeListParticipantsService.java
│   └── functional                   # Implementaciones funcionales (Streams / lambdas)
│       ├── FunctionalEnrollmentService.java
│       ├── FunctionalCancelEnrollmentService.java
│       ├── FunctionalQuerySpotsService.java
│       └── FunctionalListParticipantsService.java
├── infrastructure
│   ├── persistence
│   │   ├── entity                   # Entidades JPA (aisladas del dominio)
│   │   │   ├── EventEntity.java
│   │   │   ├── ParticipantEntity.java
│   │   │   └── EnrollmentEntity.java
│   │   ├── repository               # Spring Data JPA interfaces
│   │   │   ├── SpringEventRepository.java
│   │   │   ├── SpringParticipantRepository.java
│   │   │   └── SpringEnrollmentRepository.java
│   │   └── adapter                  # Adaptadores que implementan los puertos de salida
│   │       ├── EventJpaAdapter.java
│   │       ├── ParticipantJpaAdapter.java
│   │       └── EnrollmentJpaAdapter.java
│   ├── messaging
│   │   ├── SpringAuditEventPublisher.java   # Implementa AuditEventPublisherPort
│   │   ├── AuditEventListener.java          # @EventListener — escribe log de auditoría
│   │   └── event                            # Eventos de dominio para auditoría
│   │       ├── EnrollmentCreatedAuditEvent.java
│   │       ├── EnrollmentCancelledAuditEvent.java
│   │       └── SpotsQueriedAuditEvent.java
│   └── web
│       ├── imperative
│       │   ├── ImperativeEnrollmentController.java
│       │   ├── ImperativeCancelController.java
│       │   ├── ImperativeSpotsController.java
│       │   └── ImperativeParticipantsController.java
│       ├── functional
│       │   ├── FunctionalEnrollmentController.java
│       │   ├── FunctionalCancelController.java
│       │   ├── FunctionalSpotsController.java
│       │   └── FunctionalParticipantsController.java
│       ├── dto
│       │   ├── request
│       │   │   ├── EnrollmentRequest.java   # @Builder (Lombok)
│       │   │   └── ParticipantRequest.java
│       │   └── response
│       │       ├── EnrollmentResponse.java  # @Builder (Lombok)
│       │       ├── SpotsResponse.java
│       │       └── ParticipantListResponse.java
│       └── handler
│           └── GlobalExceptionHandler.java  # @RestControllerAdvice
└── config
    ├── BeanConfiguration.java               # Wiring de puertos ↔ adaptadores
    └── DataInitializer.java                 # Carga de datos iniciales (H2)
```

---

## 4. Modelo de Dominio

### 4.1 Event (Evento)

| Campo | Tipo | Restricciones |
|-------|------|---------------|
| `id` | `UUID` | PK, generado |
| `name` | `String` | NOT NULL, max 150 |
| `maxCapacity` | `int` | > 0 |
| `availableSpots` | `int` | >= 0, <= maxCapacity |
| `eventDate` | `LocalDateTime` | NOT NULL |

### 4.2 Participant (Participante)

| Campo | Tipo | Restricciones |
|-------|------|---------------|
| `id` | `UUID` | PK, generado |
| `name` | `String` | NOT NULL, max 100 |
| `email` | `String` | NOT NULL, único, formato email |

### 4.3 Enrollment (Inscripción)

| Campo | Tipo | Restricciones |
|-------|------|---------------|
| `id` | `UUID` | PK, generado |
| `eventId` | `UUID` | FK → Event |
| `participantId` | `UUID` | FK → Participant |
| `enrolledAt` | `LocalDateTime` | NOT NULL, generado |
| `status` | `EnrollmentStatus` | ACTIVE / CANCELLED |
| `version` | `Long` | Control de lock optimista (`@Version`) |

> **Restricción de unicidad:** `(eventId, participantId)` con `status = ACTIVE`
> garantiza idempotencia y previene inscripciones duplicadas.

---

## 5. Casos de Uso

### CU-01 · Registrar Inscripción (Enroll)

**Actor:** Participante
**Precondición:** El evento existe y tiene cupos disponibles. El participante no está inscrito actualmente.
**Flujo principal:**
1. Validar existencia del evento.
2. Validar que el participante no esté ya inscrito (idempotencia).
3. Verificar `availableSpots > 0` dentro de transacción.
4. Decrementar `availableSpots` y persistir `Enrollment` con `status = ACTIVE`.
5. Publicar `EnrollmentCreatedAuditEvent` para auditoría.
6. Retornar `EnrollmentResponse`.

**Flujo alternativo — sin cupos:**
Lanzar `EventFullException` → HTTP 409.

**Flujo alternativo — inscripción duplicada:**
Lanzar `DuplicateEnrollmentException` → HTTP 409.

**Flujo alternativo — conflicto de concurrencia:**
`OptimisticLockException` capturada en fallback → HTTP 409 con código `OPTIMISTIC_LOCK_CONFLICT`.

---

### CU-02 · Cancelar Inscripción

**Actor:** Participante
**Precondición:** La inscripción existe y está `ACTIVE`.
**Flujo principal:**
1. Buscar inscripción por `enrollmentId`.
2. Cambiar `status` a `CANCELLED`.
3. Incrementar `availableSpots` del evento dentro de transacción.
4. Publicar `EnrollmentCancelledAuditEvent`.
5. Retornar 204 No Content.

**Flujo alternativo — no encontrada:**
Lanzar `EnrollmentNotFoundException` → HTTP 404.

---

### CU-03 · Consultar Cupos Disponibles

**Actor:** Cualquier usuario
**Flujo principal:**
1. Buscar evento por `eventId`.
2. Retornar `SpotsResponse` con `availableSpots` y `maxCapacity`.
3. Publicar `SpotsQueriedAuditEvent`.

**Flujo alternativo — evento no encontrado:**
Lanzar `EventNotFoundException` → HTTP 404.

---

### CU-04 · Listar Inscritos

**Actor:** Administrador
**Flujo principal:**
1. Buscar evento por `eventId`.
2. Recuperar todas las inscripciones con `status = ACTIVE`.
3. Retornar `ParticipantListResponse` con lista de participantes y total.

**Variante funcional:** Usa `Stream` con `filter`, `map` y `collect` para proyectar la respuesta desde la lista de inscripciones.

---

## 6. API REST

**Base URL:** `/api/v1`
**Content-Type:** `application/json`
**Prefijos de modo:**
- `/imperative` → implementación imperativa
- `/functional` → implementación declarativa con Streams

### Tabla de endpoints

| Método | Path | Caso de uso | Respuesta OK | Error posible |
|--------|------|-------------|-------------|---------------|
| `POST` | `/api/v1/imperative/enrollments` | CU-01 | 201 `EnrollmentResponse` | 404, 409 |
| `POST` | `/api/v1/functional/enrollments` | CU-01 | 201 `EnrollmentResponse` | 404, 409 |
| `DELETE` | `/api/v1/imperative/enrollments/{id}` | CU-02 | 204 | 404 |
| `DELETE` | `/api/v1/functional/enrollments/{id}` | CU-02 | 204 | 404 |
| `GET` | `/api/v1/imperative/events/{id}/spots` | CU-03 | 200 `SpotsResponse` | 404 |
| `GET` | `/api/v1/functional/events/{id}/spots` | CU-03 | 200 `SpotsResponse` | 404 |
| `GET` | `/api/v1/imperative/events/{id}/participants` | CU-04 | 200 `ParticipantListResponse` | 404 |
| `GET` | `/api/v1/functional/events/{id}/participants` | CU-04 | 200 `ParticipantListResponse` | 404 |

### DTOs principales

**EnrollmentRequest** (entrada)
```json
{
  "eventId": "uuid",
  "participantId": "uuid"
}
```

**EnrollmentResponse** (salida)
```json
{
  "enrollmentId": "uuid",
  "eventId": "uuid",
  "participantId": "uuid",
  "enrolledAt": "2026-02-27T10:00:00",
  "status": "ACTIVE"
}
```

**SpotsResponse**
```json
{
  "eventId": "uuid",
  "eventName": "string",
  "maxCapacity": 100,
  "availableSpots": 42
}
```

---

## 7. Principios SOLID Aplicados

| Principio | Aplicación concreta |
|-----------|-------------------|
| **SRP** | Cada servicio de aplicación implementa exactamente un puerto de entrada. `AuditEventListener` solo escribe logs. `GlobalExceptionHandler` solo mapea errores a HTTP. |
| **OCP** | Agregar un nuevo modo de procesamiento (ej. reactive) implica crear un nuevo servicio que implemente el puerto de entrada existente, sin modificar el dominio ni los controladores. |
| **LSP** | `ImperativeEnrollmentService` y `FunctionalEnrollmentService` son intercambiables como implementaciones de `EnrollParticipantPort`. |
| **ISP** | Un puerto por caso de uso: `EnrollParticipantPort`, `CancelEnrollmentPort`, etc. Los controladores dependen solo del puerto que necesitan. |
| **DIP** | Los servicios de aplicación dependen de `EventRepositoryPort` (abstracción), nunca de `EventJpaAdapter` (concreción). Spring inyecta la implementación concreta. |

---

## 8. Control de Concurrencia

### Estrategia: Bloqueo Optimista + Idempotencia

```
Thread A ─────────────────────────┐
  lee availableSpots=1             │ @Transactional
  valida duplicado → ok            │ (SERIALIZABLE)
  decrementa → 0                   │
  persiste version=2 ──────────────┘ ✓ COMMIT

Thread B ─────────────────────────┐
  lee availableSpots=1             │ @Transactional
  valida duplicado → ok            │
  decrementa → 0                   │
  persiste version=2 ──────────────┘ ✗ OptimisticLockException
                                       → HTTP 409 OPTIMISTIC_LOCK_CONFLICT
```

**Mecanismos:**
- `@Version Long version` en `EnrollmentEntity` y `EventEntity`
- `@Transactional(isolation = Isolation.SERIALIZABLE)` en el caso de uso de inscripción
- Captura de `ObjectOptimisticLockingFailureException` en `GlobalExceptionHandler`

---

## 9. Observabilidad

### 9.1 Estrategia de Logging

Nivel de log por capa:

| Capa | Nivel | Qué se loguea |
|------|-------|---------------|
| Dominio | — | No loguea (lógica pura) |
| Aplicación | `WARN` / `ERROR` | Errores de negocio, conflictos de concurrencia |
| Adaptadores JPA | `ERROR` | Fallos de persistencia |
| AuditEventListener | `INFO` | Evento de negocio completado (inscripción, cancelación) |
| GlobalExceptionHandler | `ERROR` | Stack trace de errores no esperados |

> **Regla:** No se loguea el flujo feliz en detalle para evitar spam. Solo
> eventos de negocio relevantes y errores.

**Formato de log de auditoría:**
```
[AUDIT] action=ENROLLMENT_CREATED | event={eventId} | participant={participantId} | spots_remaining={n}
[AUDIT] action=ENROLLMENT_CANCELLED | enrollment={enrollmentId} | spots_recovered={n}
[ERROR] action=ENROLLMENT_FAILED | reason=EVENT_FULL | event={eventId} | participant={participantId}
```

### 9.2 Spring Boot Actuator

Endpoints habilitados sin infraestructura externa:
- `GET /actuator/health` → estado de la aplicación y H2
- `GET /actuator/metrics` → métricas de JVM, pool de hilos y endpoints HTTP

---

## 10. Manejo de Errores y Fallbacks

### Catálogo de errores de dominio

| Código | Excepción | HTTP Status | Descripción |
|--------|-----------|-------------|-------------|
| `EVENT_NOT_FOUND` | `EventNotFoundException` | 404 | El evento no existe |
| `PARTICIPANT_NOT_FOUND` | `ParticipantNotFoundException` | 404 | El participante no existe |
| `ENROLLMENT_NOT_FOUND` | `EnrollmentNotFoundException` | 404 | La inscripción no existe |
| `EVENT_FULL` | `EventFullException` | 409 | No hay cupos disponibles |
| `DUPLICATE_ENROLLMENT` | `DuplicateEnrollmentException` | 409 | El participante ya está inscrito |
| `OPTIMISTIC_LOCK_CONFLICT` | `ObjectOptimisticLockingFailureException` | 409 | Conflicto de concurrencia |
| `VALIDATION_ERROR` | `MethodArgumentNotValidException` | 400 | Datos de entrada inválidos |

### Estructura de respuesta de error

```json
{
  "code": "EVENT_FULL",
  "message": "El evento no tiene cupos disponibles",
  "timestamp": "2026-02-27T10:00:00",
  "path": "/api/v1/imperative/enrollments"
}
```

### Fallback por caso de uso

Cada servicio de aplicación captura y re-lanza como excepción de dominio tipada.
`GlobalExceptionHandler` es el único lugar donde las excepciones se mapean a HTTP.
Esto centraliza el manejo de errores (SRP) y evita que la lógica de mapeo HTTP
contamine la capa de aplicación.

---

## 11. Imperativo vs. Funcional — Diferencias de Implementación

| Aspecto | Imperativo | Funcional |
|---------|-----------|-----------|
| **Filtrado** | `for` + `if` explícito | `stream().filter()` |
| **Transformación** | Bucle con `add()` a lista | `stream().map().collect()` |
| **Búsqueda** | `if (result == null) throw` | `Optional.orElseThrow()` |
| **Conteo** | Variable contador + loop | `stream().count()` |
| **Proyección de lista** | Loop + construcción manual | `stream().map(mapper::toDto).toList()` |

> Ambas implementaciones comparten **exactamente los mismos puertos de salida**
> (repositorios). La diferencia es únicamente en cómo procesan colecciones y
> aplican lógica de selección.

---

## 12. Estrategia de Pruebas

### Cobertura mínima requerida

| Capa | Tipo | Herramientas |
|------|------|-------------|
| Servicios de aplicación (imperative + functional) | Unitaria | JUnit 5 + Mockito |
| GlobalExceptionHandler | Unitaria | JUnit 5 + MockMvc |
| Adaptadores JPA | Integración (H2) | `@DataJpaTest` |

### Escenarios en lenguaje Gherkin (JUnit 5)

#### Feature: Registrar inscripción

```gherkin
Scenario: Inscripción exitosa con cupos disponibles
  Given un evento con 10 cupos disponibles
  And un participante no inscrito
  When se solicita la inscripción
  Then la inscripción es creada con estado ACTIVE
  And los cupos disponibles disminuyen en 1

Scenario: Inscripción rechazada por cupo lleno
  Given un evento con 0 cupos disponibles
  When un participante intenta inscribirse
  Then se lanza EventFullException
  And la respuesta HTTP es 409 con código EVENT_FULL

Scenario: Inscripción duplicada rechazada
  Given un participante ya inscrito en el evento
  When el mismo participante intenta inscribirse de nuevo
  Then se lanza DuplicateEnrollmentException
  And la respuesta HTTP es 409 con código DUPLICATE_ENROLLMENT

Scenario: Conflicto de concurrencia resuelto
  Given un evento con 1 cupo disponible
  And dos hilos intentan inscribirse simultáneamente
  When ambas transacciones se ejecutan
  Then exactamente 1 inscripción es creada
  And el segundo hilo recibe 409 con código OPTIMISTIC_LOCK_CONFLICT
```

#### Feature: Cancelar inscripción

```gherkin
Scenario: Cancelación exitosa
  Given una inscripción activa
  When se solicita la cancelación
  Then el estado cambia a CANCELLED
  And los cupos disponibles aumentan en 1

Scenario: Cancelación de inscripción inexistente
  Given un enrollmentId que no existe
  When se solicita la cancelación
  Then se lanza EnrollmentNotFoundException
  And la respuesta HTTP es 404
```

#### Feature: Consultar cupos

```gherkin
Scenario: Consulta exitosa de cupos
  Given un evento con 10 cupos máximos y 3 inscritos
  When se consultan los cupos disponibles
  Then la respuesta contiene availableSpots = 7

Scenario: Consulta de evento inexistente
  Given un eventId que no existe
  When se consultan los cupos
  Then la respuesta HTTP es 404 con código EVENT_NOT_FOUND
```

#### Feature: Listar inscritos

```gherkin
Scenario: Listado de inscritos activos
  Given un evento con 3 inscripciones ACTIVE y 1 CANCELLED
  When se listan los inscritos
  Then la respuesta contiene exactamente 3 participantes

Scenario: Listado vacío cuando no hay inscritos
  Given un evento sin inscripciones activas
  When se listan los inscritos
  Then la respuesta contiene una lista vacía y total = 0
```

---

## 13. Dependencias del Proyecto (build.gradle)

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.x'
    id 'io.spring.dependency-management' version '1.1.x'
}

java { sourceCompatibility = JavaVersion.VERSION_17 }

dependencies {
    // Spring Boot
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-validation'

    // Base de datos en memoria
    runtimeOnly 'com.h2database:h2'

    // Reducción de boilerplate (Builder, constructores)
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    // Testing
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    // Incluye: JUnit 5, Mockito, AssertJ, MockMvc, @DataJpaTest
}
```

---

## 14. Decisiones Excluidas y Justificación

| Decisión excluida | Justificación |
|-------------------|--------------|
| Spring Events `@Async` + ThreadPoolTaskExecutor | Introduce complejidad sin resolver el driver de consistencia. Async hace que el resultado de inscripción sea eventual, no inmediato, lo que contradice CU-01. |
| Redis / RabbitMQ / Kafka | Infraestructura externa. La POC debe correr sin dependencias externas. |
| Pessimistic locking (`PESSIMISTIC_WRITE`) | Bloquea filas durante la transacción completa. Para picos de alta concurrencia, el lock optimista escala mejor (no hay bloqueo en lectura). |
| Spring Security | Fuera del alcance de la POC. Los endpoints no requieren autenticación. |

---

*Próximo artefacto: `spec_plan.md` — Plan de desarrollo con orden de implementación, dependencias entre tareas y criterios de done.*
