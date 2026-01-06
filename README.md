# Fix Trading Simulator

- [Overview](#overview)
- [System Architecture](#system-architecture)
- [Project Structure](#project-structure)
- [Features](#features)
- [Running the project](#running-the-project)
- [Images](#images)
- [Help Queries](#help-queries)


# Overview

A trading simulator between a Broker and a Stock Exchange using the [Financial Information eXchange (FIX) Protocol](https://www.fixtrading.org/). It's a study project using [QuickFIX/J](https://www.quickfixj.org/), [Quarkus](https://quarkus.io/), [Angular](https://angular.io/), Docker, Docker Compose and [PostgreSQL](https://www.postgresql.org/).

If you want to participate on this project, just open an issue and we can talk about!

Both Broker and Exchange systems were built with Quarkus on the back-end and Angular on the front-end.

The back-ends communicate each other with QuickFIX/J and each has a schema into the PostgreSQL.

Each Angular front-end communicates with the Quarkus back-end using REST and Websockets.

# System Architecture

![image](./documentation/design/fix-trading-simulator-design.png)

# Project Structure

[Broker back-end](./broker-back-end/README.md)

[Broker front-end](./broker-front-end/README.md)

[Exchange back-end](./exchange-back-end/README.md)

[Exchange front-end](./exchange-front-end/README.md)

[Documentation](./documentation/README.md)

# Features

## Orders

You can submit, negotiate, cancel and list your orders.

It's possible to set the Exchange to automatically negotiate the orders.

Every change in the orders are broadcasted using websockets and are imeaditelly updated on the front-end.

## Session

Make logon and logout.

View the session status and storage.

View the session configuration.

List the messages sent from the session.

## Logs

List the FIX events.

List the messages received and sent.


# Running the project

After start, access project at:
- Broker Front end
  - http://localhost:8085/
- Broker Back end swagger:
  - http://localhost:8080/q/swagger-ui/
- Exchange Front end
  - http://localhost:8095/
- Exchange Back end swagger:
  - http://localhost:8090/q/swagger-ui/
- PostgreSQL:
  - jdbc:postgresql://localhost:5432/postgres
  - user: postgres
  - password: postgres

The containers should be running like this:
```
docker ps
CONTAINER ID        IMAGE                               COMMAND                  CREATED             STATUS              PORTS                    NAMES
8191a0fcde2f        felipewind/exchange-front-end:1.0   "/docker-entrypoint.…"   16 minutes ago      Up 16 minutes       0.0.0.0:90->80/tcp       exchange-front-end
1178d4e1c02f        felipewind/broker-front-end:1.0     "/docker-entrypoint.…"   16 minutes ago      Up 16 minutes       0.0.0.0:80->80/tcp       broker-front-end
2370c47d0a2d        felipewind/broker-back-end:1.0      "/deployments/run-ja…"   16 minutes ago      Up 16 minutes       0.0.0.0:8080->8080/tcp   broker-back-end
8106b9a48217        felipewind/exchange-back-end:1.0    "/deployments/run-ja…"   16 minutes ago      Up 16 minutes       0.0.0.0:8090->8090/tcp   exchange-back-end
6b53a07b72ac        postgres                            "docker-entrypoint.s…"   16 minutes ago      Up 16 minutes       0.0.0.0:5432->5432/tcp   postgresql-qfj
```
### Standard Mode (JVM)

This mode is suitable for development, debugging, and general use. It compiles and runs Java applications inside a
standard OpenJDK container. Crucially, by leveraging Multi-stage Docker builds, this allows the images to be compiled
entirely from source without requiring local installations of Java, Maven, Node.js, or Angular CLI on your machine.

**Files used:** 
`./run-project.sh` and `docker-compose.yml`.

**Startup Command:**
```
$ chmod +x ./run-project.sh 
$ ./run-project.sh
```

### Native Image Mode (Instant Startup)

This mode compiles the Quarkus backends into fast, standalone native executables using GraalVM (Mandrel). Native
executables offer microsecond startup times and a significantly lower memory footprint, making them ideal for production
or minimal environments. The process leverages Multi-stage Docker builds, allowing the images to be compiled entirely
from source without requiring local installations of Java, Maven, Node.js, or Angular CLI.

**Files used:**
`./run-project-native.sh` and `docker-compose-native.yml`.

**Startup Command:**
```
$ chmod +x ./run-project-native.sh 
$ ./run-project-native.sh
```
**Prerequisites:** Ensure your Docker environment is allocated sufficient resources (8GB+ RAM recommended for the compilation stage).

### Native Distroless Mode (Minimal & Secure)
Extends the Native Image mode by packaging the executable into a distroless container. These images contain only the application, removing shells (sh, bash) and OS tools to minimize the attack surface and image size. Uses Multi-stage Docker builds to compile entirely from source.

**Files used:**
`./run-project-native-distroless.sh` and `docker-compose-native-distroless.yml`.

**Startup Command:**
```
$chmod +x ./run-project-native-distroless.sh
$ ./run-project-native-distroless.sh
```

### Using the Docker Hub Images

Inside the root folder of the project, execute:
```
$chmod +x ./run-from-docker-hub.sh
$ ./run-from-docker-hub.sh
```

Docker Hub images:
- [exchange-back-end](https://hub.docker.com/repository/docker/felipewind/exchange-back-end)
- [exchange-front-end](https://hub.docker.com/repository/docker/felipewind/exchange-front-end)
- [broker-back-end](https://hub.docker.com/repository/docker/felipewind/broker-back-end)
- [broker-front-end](https://hub.docker.com/repository/docker/felipewind/broker-front-end)

## Development Mode (Manual Setup)

> **Note:** This section is intended for **developers** who want to modify the source code and use features like **Live Coding / Hot Reload**.
>
> Unlike the Docker method, this mode **REQUIRES** you to install Java 11+, Maven, Node.js, and Angular CLI on your local machine.

> **Warning! QuickFIX/J Dependency Setup**
> 
> This project relies on the latest version of **QuickFIX/J**, which is not yet available in the public Maven Central
> repository and must be assembled directly from the GitHub source code. Before building the project, ensure that 
> Docker is running and execute the initialization script below. This will compile the required dependency within a 
> container and install it into your local Maven repository.
> ```
> $chmod +x quickfixj-m2-local.sh
> $./quickfixj-m2-local.sh
> ```

The default version of the development back-end projects is using H2 data base (in memory).

It's possible to change the `application.properties` and set them to run with PostgreSQL, in this case you should start a PostgreSQL container:
```
docker run -d --name postgres-qfj -p 5432:5432 -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=postgres postgres
```


### Navigate to the `exchange-back-end` folder and type:
```
$ ../mvnw compile quarkus:dev -Ddebug=5006
```

Access http://localhost:8090/q/swagger-ui/

### Navigate to the `broker-back-end` folder and type:
```
$ ../mvnw compile quarkus:dev
```

Access http://localhost:8080/q/swagger-ui/


### Navigate to the `exchange-front-end` folder and type:
```
$ npm install
$ ng serve
```

Access http://localhost:4300


### Navigate to the `broker-front-end` folder and type:
```
$ npm install
$ ng serve
```

Access http://localhost:4200


# Images

## Session Control

![image](./documentation/images/broker-session-control.png)

## Session Properties

![image](./documentation/images/broker-session-properties.png)

## Session Messages

![image](./documentation/images/broker-session-messages.png)

## Orders list

### Broker

![image](./documentation/images/broker-orders-list.png)

### Exchange

![image](./documentation/images/exchange-orders-list.png)

## Logs - FIX Events

![image](./documentation/images/broker-logs-events.png)

## Logs - Messages incoming

![image](./documentation/images/broker-logs-incoming-messages.png)

## Broker - Swagger

![image](./documentation/images/broker-swagger.png)

## Exchange - Swagger

![image](./documentation/images/exchange-swagger.png)



# Help Queries


```sql
select * from broker.sessions;
select * from broker.messages;
select * from broker.event_log  order by id desc;
select * from broker.messages_log_incoming order by id desc;
select * from broker.messages_log_outgoing order by id desc;

select * from exchange.sessions;
select * from exchange.messages;
select * from exchange.event_log  order by id desc;
select * from exchange.messages_log_incoming order by id desc;
select * from exchange.messages_log_outgoing order by id desc;
```

