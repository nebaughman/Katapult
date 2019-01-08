# Katapult

Kotlin-oriented single-jar API/Web app starter kit using Javalin and Vue-Cli

## Overview

The purpose of this project is to combine a number of useful libraries into a kind of starter kit for other projects. It's not a complete project, but could be helpful as a starting point or for reference.

When built, Katapult provides a single executable `jar` file that can self-serve a REST API and Vue Web app.

### Using Katapult

Katapult is not a framework. Katapult's core functionality is not clearly separated from application code. Consider copying Katapult, then editing to your own needs.

Katapult provides some sample implementations, primarily for demonstration.

> Important: This might not be suitable for production purposes. 
>
> Overall, I wanted to experiment and learn a thing or two. _Share and enjoy._

### Ecosystem

**Server:**

- [Kotlin](https://kotlinlang.org/)
- [Javalin](https://javalin.io)
- [Jetty](https://www.eclipse.org/jetty/)
- [Clikt](https://github.com/ajalt/clikt)
- [BCrypt](https://github.com/patrickfav/bcrypt)
- [Exposed](https://github.com/JetBrains/Exposed)
- [SQLite](https://www.sqlite.org/)
- [JUnit](https://junit.org)
- [SLF4J](https://www.slf4j.org/)
- [Gradle](https://gradle.org/)
- [ShadowJar](https://github.com/johnrengelman/shadow)

Supports HTTPS by reading `fullchain.pem` and `privkey.pem` as provided by [Let's Encrypt](https://letsencrypt.org/) (and maybe other certificate authorities).

**Client:**

- [Vue](https://vuejs.org/)
- [Vue-Cli](https://cli.vuejs.org/)
- [Vue-FontAwesome](https://github.com/FortAwesome/vue-fontawesome)
- [Bootstrap](https://getbootstrap.com)
- [Axios](https://github.com/axios/axios)
- [Yarn](https://yarnpkg.com)

> I develop on [Ubuntu](https://www.ubuntu.com/) with [IntelliJ IDEA](https://www.jetbrains.com/idea/) (Community Edition).

### Architecture

Two subprojects:

- The `srv` project augments Javalin with some classes for REST API development and session management, provides database/DAO integration, and supports HTTPS.

- The `web` project includes a Vue-Cli app configuration, including Bootstrap and FontAwesome.

These are separate projects, but `yarn build` copies the Web app's `dist` to the server's `resources/app`, where it is served as static content.

Instead of bundling the Vue app into the server jar, the Vue app could be served independently from another web server or static hosting service.

> CORS headers are supported by Javalin, but not yet configured, so hosting the API on a different domain than the app may cause cross-origin resource errors.

### Deployment

Server can be bundled with `shadowJar` and deployed as a single executable jar file. All static resources (including Vue app) are bundled within the jar and served via Javalin/Jetty. External resources reside in a `data` directory.

Katapult uses Clikt for command-line argument parsing. For now, kindly refer to the source for command-line options, which are likely to change and expand.

In general, execute a jar something like: 

```
java -jar <the.jar> --options...
```

Otherwise, it's up to you to configure the environment, open ports, keep it running, handle upgrades, etc, etc.

## Javalin Server

The `srv` project integrates a number of components for building a REST API and hosting static pages (such as the Vue app).

### REST API

Javalin is great for REST API development. Katapult includes an Action interface and UserSession object to assist in implementing business logic that complements Javalin's framework.

Katapult uses Kotlin data classes to define API request and response data. These are transformed to/from JSON by Javalin. REST parameters sent by the client are sent as JSON in the body of the request - not part of the URL.

> Javalin supports URL arguments, but Katapult does not (yet?) use this.

### Database

Sample code is configured for SQLite, but can be altered by specifying a different driver and URI.

Data access is via Exposed library, but business logic is isolated from the Exposed api (to some degree).

> This integration needs more experimentation, but the Kotlin Exposed library from JetBrains looks promising!

### Session Management

By default, HTTP sessions are stored in memory, and are lost when the application is restarted. Katapult can be configured to persist sessions to files.

> Other session strategies could/would/should be introduced in the future.

See the `Action` and `UserSession` classes, provided by Katapult. When API logic is implemented by extending `Action`, Katapult will retrieve the `UserSession` and provide it to the `Action.invoke()` method.

To store additional session data, see the `SessionData` class.

### HTTPS

Katapult supports HTTPS. It can read `fullchain.pem` and `privkey.pem` as provided by Let's Encrypt (and maybe other certificate authorities). 

> No messing around with JKS files!

Obtaining these files (eg, from Let's Encrypt) is outside the scope of this project.

## Vue App

The `web` project is a Vue-Cli app with some additional configuration (multi-page setup, Bootstrap and FontAwesome integration, Axios for ajax, etc). The Vue components provided are just an example.

The primary point is that the Vue app can be served as static content from the Javalin server, allowing the whole api/app to be bundled as a single executable jar.

If bundling is not desirable, the Vue app could be hosted separately. (See prior note about CORS headers, which are not yet configured.)

> See the Wishlist regarding future TypeScript configuration.

## Wishlist

- Bring the `web` and `srv` projects together under one build environment (probably by having gradle run the yarn scripts).
- Rewrite `build.gradle` in Kotlin script.
- Cross-compile the Kotlin data classes used by the API for both JS and JVM, so the same implementations can be sent and received on each side.
- Allow for TypeScript `.ts` files, as well as `<script lang="ts">` in `.vue` files.
- Templated server-rendered pages. A mustache template sample was created, but has been commented out. Maybe this could return.
- Maybe modular/plugin architecture allowing addition of features separate from core.
- Integrated automatic Let's Encrypt certificate negotiation/renewal.

Overall, it would be nice to cover more basic use-cases, such as user signup, email verification, file upload, etc, etc.

## License

Heed the [License](LICENSE.txt).
