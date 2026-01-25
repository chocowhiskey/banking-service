# Banking Service

Ein Spring Boot Backend-Projekt, das grundlegende Banking-Operationen mit Fokus auf **transaktionale Konsistenz** und **Concurrency-Handling** demonstriert.

Entwickelt als Portfolio-Projekt für Bewerbungen im Finanzsektor.

---

## 🎯 Projektziel

Dieses Projekt zeigt professionelle Java-Backend-Entwicklung mit:

- **Domain-Driven Design**: Geschäftslogik in Domain-Entities
- **Transaktionale Integrität**: ACID-Eigenschaften bei Banking-Operationen
- **Concurrency-Handling**: Optimistic Locking bei parallelen Zugriffen
- **RESTful API**: Saubere HTTP-Schnittstellen
- **Umfassende Tests**: Unit-, Integration- und Concurrency-Tests

---

## 🏗️ Technologie-Stack

- **Java 17**
- **Spring Boot 3.4.1**
- **Spring Data JPA** - Datenbankzugriff
- **Hibernate** - ORM
- **H2 Database** - In-Memory Datenbank für Entwicklung
- **Lombok** - Boilerplate-Code-Reduktion
- **JUnit 5 & AssertJ** - Testing
- **Maven** - Build-Management

---

## 🚀 Schnellstart

### Voraussetzungen

- Java 17 oder höher
- Maven (oder nutze den mitgelieferten Maven Wrapper)

### Projekt starten
```bashRepository klonen
git clone https://github.com/chocowhiskey/banking-service.git
cd banking-serviceAnwendung starten
./mvnw spring-boot:run

Die Anwendung läuft auf: **http://localhost:8080**

### H2-Console (Datenbank-Ansicht)

Während die Anwendung läuft:

**URL:** http://localhost:8080/h2-console

**Login-Daten:**
- JDBC URL: `jdbc:h2:mem:bankingdb`
- Username: `sa`
- Password: _(leer lassen)_

---

## 📡 API-Endpunkte

### Konto erstellen
```bashPOST /api/accounts
Content-Type: application/json{
"iban": "DE89370400440532013000",
"ownerName": "Max Mustermann"
}

**Antwort:**
```json{
"id": 1,
"iban": "DE89370400440532013000",
"ownerName": "Max Mustermann",
"balance": 0,
"createdAt": "2026-01-25T10:30:00"
}

---

### Konto abrufen
```bashGET /api/accounts/{iban}

**Beispiel:**
```bashcurl http://localhost:8080/api/accounts/DE89370400440532013000

---

### Geld einzahlen (Credit)
```bashPOST /api/accounts/{iban}/credit
Content-Type: application/json{
"amount": 1000.00,
"reference": "Gehalt Januar"
}

---

### Geld abheben (Debit)
```bashPOST /api/accounts/{iban}/debit
Content-Type: application/json{
"amount": 50.00,
"reference": "Miete"
}

**Business Rule:** Kontostand darf nicht negativ werden!

---

### Überweisung zwischen Konten
```bashPOST /api/accounts/transfer
Content-Type: application/json{
"fromIban": "DE111",
"toIban": "DE222",
"amount": 300.00,
"reference": "Miete Januar"
}

**Wichtig:** Beide Buchungen erfolgen **atomar** - entweder beide oder keine!

---

### Transaktionen abrufen
```bashGET /api/accounts/{iban}/transactions

**Antwort:** Liste aller Transaktionen, sortiert nach neuesten zuerst.

---

## 🏛️ Architektur & Design-Entscheidungen

### Domain-Driven Design

Die Geschäftslogik liegt **in den Domain-Entities**, nicht in Services:
```java// ❌ Anemic Model (schlecht)
account.setBalance(account.getBalance().subtract(amount));// ✅ Rich Domain Model (gut)
account.debit(amount, reference);

**Vorteil:** 
- Business Rules sind zentral an einem Ort
- Validierung automatisch bei jeder Operation
- Code ist selbstdokumentierend

---

### Transaktionale Konsistenz

Alle Banking-Operationen sind mit `@Transactional` abgesichert:
```java@Transactional
public TransferResponse transfer(String fromIban, String toIban, BigDecimal amount) {
// Beide Accounts werden geladen
// Beide Buchungen durchgeführt
// Beide gespeichert// Bei Fehler: Automatischer Rollback!
}

**Garantiert:**
- Atomarität (alles oder nichts)
- Konsistenz (Regeln werden eingehalten)
- Isolation (keine Race Conditions)

---

### Optimistic Locking

Verhindert **Lost Updates** bei parallelen Zugriffen:
```java@Entity
public class Account {
@Version
private Long version;  // Hibernate prüft Version bei jedem Update
}

**Szenario:**
- Thread 1 und Thread 2 lesen Account (Balance: 1000)
- Thread 1 bucht 300 ab → Balance: 700, Version: 1
- Thread 2 versucht zu buchen → `OptimisticLockException`
- Thread 2 wiederholt mit aktuellen Daten

**Ergebnis:** Keine verlorenen Updates! 🔒

---

### Retry-Mechanismus

Bei `OptimisticLockException` wird automatisch wiederholt:
```javaint maxRetries = 3;
while (attempt < maxRetries) {
try {
return performTransfer(...);
} catch (OptimisticLockException e) {
attempt++;
Thread.sleep(10 * attempt); // Exponentielles Backoff
}
}

**Vorteil:** Hohe Erfolgsrate auch bei hoher Concurrent Load.

---

## 🧪 Tests ausführen
```bashAlle Tests
./mvnw testNur Concurrency-Tests
./mvnw test -Dtest=ConcurrencyTestMit Coverage-Report
./mvnw test jacoco:report

### Test-Kategorien

**Unit Tests** - Domain-Logik
- `AccountTest`: Validierung von debit/credit
- Business Rules werden getestet

**Integration Tests** - Service-Layer
- `AccountServiceTest`: Transaktionale Operationen
- `TransferServiceTest`: Account-Überweisungen
- Rollback-Verhalten

**Concurrency Tests** - Parallele Zugriffe
- Simultane Transfers auf dasselbe Konto
- Verhindert Überziehung bei Race Conditions
- **Highlight-Feature für Banking-Bewerbungen!**

---

## 💡 Was ich dabei gelernt habe

### Technisch

- Wie `@Transactional` mit verschiedenen Isolation Levels funktioniert
- Unterschied zwischen Pessimistic und Optimistic Locking
- Warum `BigDecimal` für Geldbeträge essentiell ist
- Wie man Concurrency-Probleme testet und verhindert

### Architektur

- Domain-Driven Design in der Praxis
- Separation of Concerns (Controller → Service → Repository → Domain)
- Immutability bei Transaktionen (Audit Trail)
- DTO-Pattern für saubere API-Grenzen

### Best Practices

- Constructor Injection statt Field Injection
- Records für DTOs (Java 14+)
- Proper Exception Handling mit RFC 7807 Problem Details
- Test-Driven Development bei kritischer Logik

---

## 📦 Projekt-Struktursrc/main/java/com/simohoff/banking_service/
├── domain/              # Domain-Entities mit Business Logic
│   ├── Account.java
│   ├── Transaction.java
│   └── TransactionType.java
├── repository/          # JPA Repositories
│   ├── AccountRepository.java
│   └── TransactionRepository.java
├── service/             # Business Services mit @Transactional
│   ├── AccountService.java
│   └── TransferService.java
├── controller/          # REST API Controllers
│   └── AccountController.java
├── dto/                 # Data Transfer Objects
│   ├── AccountResponse.java
│   ├── TransactionRequest.java
│   └── TransferRequest.java
└── exception/           # Custom Exceptions & Exception Handler
├── AccountNotFoundException.java
└── GlobalExceptionHandler.java

---

## 🔒 Business Rules

Das System garantiert folgende Banking-Rules:

1. **Kontostand darf nicht negativ werden**
   - Validierung in `Account.debit()`
   
2. **Transaktionen sind immutable**
   - Einmal erstellt, nie geändert (Audit Trail)
   
3. **Buchungen sind atomar**
   - Transfer = Debit + Credit in EINER Transaktion
   
4. **Keine verlorenen Updates**
   - Optimistic Locking mit `@Version`
   
5. **Beträge sind immer positiv**
   - Validierung vor jeder Operation

---

## 🚧 Mögliche Erweiterungen

Dieses Projekt ist bewusst auf die Kern-Funktionalität fokussiert. Mögliche Erweiterungen:

- [ ] PostgreSQL als Production-Datenbank mit Docker Compose
- [ ] Spring Security mit JWT-Authentication
- [ ] REST API Dokumentation mit Swagger/OpenAPI
- [ ] Überweisung-Limits und KYC-Prüfungen
- [ ] Event-Sourcing für vollständige Audit-Historie
- [ ] GitHub Actions CI/CD Pipeline
- [ ] Metrics mit Spring Actuator + Prometheus

---

## 📄 Lizenz

Dieses Projekt ist zu Lern- und Portfolio-Zwecken erstellt.

---

## 👤 Autor

**Simone Hoffmann**  
[GitHub](https://github.com/simohoff) | [LinkedIn](https://linkedin.com/in/simohoff)

---

## 🙏 Acknowledgments

Entwickelt als Portfolio-Projekt mit Fokus auf:
- Clean Code & SOLID Principles
- Banking-spezifische Anforderungen
- Production-Ready Patterns