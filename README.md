# Doki Doki Reading Club

A Java-based client-server application for managing a shared reading community.
The system allows users to manage their accounts and personal libraries, create and join book clubs, track reading progress, buy books, and collaboratively fund books through club fundraisers.

The project is designed around a **multi-client TCP server**, **concurrent request handling**, **JSON-based communication**, and **UDP notifications** for real-time events.

## Features

### Authentication & Accounts

* User registration and login
* Session-based authentication using generated tokens
* Logout and session cleanup
* Password hashing with SHA-256
* Account balance management

### Book Management

* Book market listing
* Book purchasing
* Personal library management
* Reading progress tracking
* Library filtering by reading status:

  * Unread
  * Currently Reading
  * Read

### Book Clubs

* Create reading clubs
* Browse all clubs
* View personal clubs
* Submit membership requests
* Accept or reject join requests
* View club members
* Remove members as the club owner
* Owner-based access control

### Collaborative Fundraising

Club members can collectively fund books that some members do not own.

* Create a fundraiser for a book
* Automatically calculate the required target based on club membership
* Make donations using account balance
* Track fundraiser progress
* Automatically complete fundraisers when the target is reached
* Add the funded book to participating members' libraries
* Handle excess contributions
* Notify club members about fundraiser events

### Notifications

The server provides event-based notifications through UDP.

Notifications are used for events such as:

* Join requests
* Join request approval/rejection
* Member removal
* Fundraiser creation
* Donations
* Fundraiser completion
* Books added through completed fundraisers

## Architecture

The project follows a layered client-server structure:

```text
                         ┌─────────────────────┐
                         │     Java Client     │
                         │                     │
                         │  CommandParser      │
                         │  ClientSession      │
                         │  ServerListener     │
                         └─────────┬───────────┘
                                   │
                              TCP / JSON
                                   │
                                   ▼
                         ┌─────────────────────┐
                         │    ServerMain       │
                         │                     │
                         │  ClientHandler × N  │
                         └─────────┬───────────┘
                                   │
                     ┌─────────────┼─────────────┐
                     │             │             │
                     ▼             ▼             ▼
              ┌───────────┐ ┌───────────┐ ┌──────────────┐
              │ Auth      │ │ Book      │ │ Club         │
              │ Service   │ │ Service   │ │ Service      │
              └───────────┘ └───────────┘ └──────────────┘
                     │             │             │
                     └─────────────┼─────────────┘
                                   ▼
                         ┌─────────────────────┐
                         │ FundraiserService   │
                         └─────────┬───────────┘
                                   │
                                   ▼
                         ┌─────────────────────┐
                         │    StateManager     │
                         │                     │
                         │ Users               │
                         │ Books               │
                         │ Clubs               │
                         │ Join Requests       │
                         │ Fundraisers         │
                         └─────────┬───────────┘
                                   │
                         ┌─────────┴─────────┐
                         ▼                   ▼
                ┌────────────────┐   ┌─────────────────┐
                │ BackupService  │   │ books.json      │
                │ Serialized     │   │ Book dataset    │
                │ application    │   │                 │
                │ state          │   │                 │
                └────────────────┘   └─────────────────┘

                         UDP
                          │
                          ▼
                ┌─────────────────────┐
                │ NotificationManager │
                └─────────────────────┘
```

## Communication Model

### TCP

TCP is used for the main client-server communication.

Requests and responses are exchanged as JSON messages.

A request contains:

```json
{
  "command": "login",
  "token": null,
  "data": {
    "username": "alice",
    "password": "hashed-password"
  }
}
```

The server responds using a structured response:

```json
{
  "success": true,
  "message": "Login successful.",
  "data": {
    "token": "...",
    "userId": "...",
    "balance": 1000
  }
}
```

### UDP

UDP is used for event notifications that do not require a request-response cycle.

For example:

```json
{
  "type": "DONATION_RECEIVED",
  "message": "User alice donated 100 to the fundraiser."
}
```

This allows notifications to be delivered independently from the normal command flow.

## Concurrency

The server supports multiple clients simultaneously.

Each incoming TCP connection is handled by a dedicated thread:

```text
Client 1 ──► ClientHandler ──► Service Layer
Client 2 ──► ClientHandler ──► Service Layer
Client 3 ──► ClientHandler ──► Service Layer
        ...
```

Shared application state is stored using `ConcurrentHashMap`, while critical operations on users, clubs, and fundraisers use synchronization to protect state changes.

Examples include:

* Concurrent account balance updates
* Concurrent book purchases
* Concurrent donations
* Fundraiser completion
* Club membership modifications

## Persistence & Recovery

Application state is periodically persisted using Java serialization.

`BackupService`:

* Creates periodic backups
* Uses a temporary file before replacing the main backup
* Performs an atomic file move when possible
* Restores the application state when the server starts
* Performs a final backup during shutdown

The book catalog is loaded separately from `books.json`.

## Project Structure

```text
src/
├── client/
│   ├── ClientMain.java
│   ├── ClientSession.java
│   ├── CommandParser.java
│   └── ServerListener.java
│
├── server/
│   ├── ServerMain.java
│   ├── ClientHandler.java
│   │
│   ├── notification/
│   │   └── NotificationManager.java
│   │
│   ├── service/
│   │   ├── AccountService.java
│   │   ├── AuthService.java
│   │   ├── BookService.java
│   │   ├── ClubService.java
│   │   └── FundraiserService.java
│   │
│   └── storage/
│       ├── BackupService.java
│       └── StateManager.java
│
└── shared/
    ├── model/
    │   ├── Book.java
    │   ├── BookClub.java
    │   ├── Donation.java
    │   ├── Fundraiser.java
    │   ├── JoinRequest.java
    │   ├── LibraryBook.java
    │   └── User.java
    │
    ├── network/
    │   ├── Request.java
    │   ├── Response.java
    │   └── Notification.java
    │
    ├── model/enums/
    │   ├── BookStatus.java
    │   ├── FundraiserStatus.java
    │   └── MessageType.java
    │
    └── util/
        ├── HashUtil.java
        └── JsonUtil.java
```

## Main Technologies

* **Java**
* **TCP sockets**
* **UDP / DatagramSocket**
* **Multithreading**
* **ConcurrentHashMap**
* **Synchronization**
* **JSON communication**
* **Gson**
* **Java Serialization**
* **Object-oriented design**

## Running the Project

The project currently contains both the server and a command-line client.

### 1. Configure Gson

The project uses Google's Gson library for JSON serialization and deserialization.

Make sure Gson is available on the project's classpath.

### 2. Start the Server

Run:

```text
server.ServerMain
```

The server listens on:

```text
127.0.0.1:8080
```

### 3. Start a Client

Run:

```text
client.ClientMain
```

Multiple client instances can be started simultaneously to test concurrent connections.

## Example Commands

### Authentication

```text
register <username> <password>
login <username> <password>
logout
```

### Account

```text
account_charge <amount>
balance_show
```

### Books

```text
books_market_list
book_buy <bookId>
progress_submit <bookId> <page>
library_read_not_list
library_reading_list
library_read_list
```

### Clubs

```text
club_create <clubName>
clubs_list
clubs_list_total
join <clubId>
club_view <clubId>
```

### Fundraising

Inside club mode:

```text
members_club_list
fundraiser_create <bookId>
progress_fundraiser_view
donate <amount>
member_remove <userId>
```

## Design Highlights

### Separation of Responsibilities

Application logic is separated into dedicated services:

```text
AuthService
AccountService
BookService
ClubService
FundraiserService
```

This keeps request routing, business logic, persistence, and networking concerns relatively independent.

### Shared Domain Model

The `shared` package contains models and network objects used by both the client and server.

This avoids duplicating protocol and domain definitions across the two sides.

### Thread-Safe Shared State

The central `StateManager` provides a shared in-memory state for concurrent client handlers.

Thread-safe collections combined with synchronized critical sections are used to protect important operations.

### Event-Driven Notifications

Business events can trigger UDP notifications without blocking the normal request/response protocol.

For example:

```text
Donation
   │
   ▼
Fundraiser updated
   │
   ├──► Fundraiser completion
   │
   └──► UDP notifications
```

## Project Goals

This project was designed to explore practical concepts in:

* Client-server architecture
* Network programming
* Concurrent programming
* State management
* Authentication
* Business logic design
* Inter-component communication
* Persistence and recovery
* Event notifications

## License

This project is currently intended as a personal/educational project.
