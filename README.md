# Katapult

[Kotlin](https://kotlinlang.org/) oriented single-jar API/Web app starter kit using [Javalin](https://javalin.io) and [Vue-Cli](https://cli.vuejs.org/)

## Overview

The purpose of this project is to combine a number of useful libraries into a starting framework for other projects.

When built, Katapult provides a single executable `jar` file that can self-serve a REST API and Vue Web app.

This makes for easy deployment. :bulb: Also consider building `localhost` Web apps.

> Important: This might not be suitable for production purposes. The primary motivation is to experiment and learn a thing or two.

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

- The `srv` project augments Javalin with some classes for REST API development and session management, provides database/DAO integration via Exposed framework, and supports HTTPS.

- The `web` project includes a Vue-Cli app configuration, including Bootstrap and FontAwesome. `yarn build` copies `dist` to the server's `resources/app`, where it is served as static content.

### Deployment

Create the single executable jar via `shadowJar`. All static resources (including Vue app) are bundled within the jar and served via Javalin/Jetty. External resources reside in a `data` directory.

In general, execute a jar like: 

```
java -jar <the.jar> --options...
```

The included `main` function runs the sample api/app. Kindly refer to the source for command-line options.

## Javalin Server

The `srv` project integrates a number of components for building a REST API and hosting static pages (such as the Vue app).

- **Modules:** Katapult includes a simple module system. Modules extend `KatapultModule` and are given the opportunity to augment the Javalin server upon startup.

- **REST API:** Katapult augments Javalin's API handling with a convenient `Action` helper, which leverages Kotlin data classes for request/response handlers.

  > REST parameters sent by the client are sent as JSON in the body of the request - not part of the URL. Javalin supports URL arguments, but Katapult does not (yet?) use this.
 
- **Sessions:** (Experimental) `UserSession` and `SessionData` data class to aid in session management. Sessions can be stored to files.

  > Everything is experimental, sessions especially!

- **Database/DAO:** Experimenting with [Exposed](https://github.com/JetBrains/Exposed) for data access.
 
    - Sample SQLite module (should be easy to add others).
    
    - Sample `UserDao`, which further simplifies and isolates business logic from Exposed library.

- **HTTPS:** Katapult reads `fullchain.pem` and `privkey.pem` as provided by Let's Encrypt (and maybe other certificate authorities).

  > **No messing around with JKS files!** 
  
  Obtaining SSL certificate files (eg, from Let's Encrypt) is outside the scope of this project.
  
- **Templating:** Javalin supports template engines for server-rendered HTML. An example [Mustache](https://mustache.github.io/) Katapult module is provided.

- **External Data:** Server runtime data is stored in a separate data directory. This includes SQLite database, session files, SSL certificate files, etc.

## Vue App

The `web` project is a Vue-Cli app with some additional configuration (multi-page setup, Bootstrap and FontAwesome integration, Axios for ajax, etc). The Vue app itself is just an example starting point.

The primary point is that the Vue app can be served as static content from the Javalin server, allowing the whole app/api to be bundled as a single executable jar.

If bundling is not desirable, the Vue app could be hosted separately.

> CORS headers are supported by Javalin, but not yet configured, so hosting the API on a different domain than the app may cause cross-origin resource errors.

## Wishlist

- Bring the `web` and `srv` projects together under one build environment (maybe by having gradle run the yarn scripts)
- Rewrite `build.gradle` in Kotlin script
- Cross-compile the Kotlin data classes used by the API for both JS and JVM, so the same implementations can be sent and received on each side
- Allow for TypeScript `.ts` files, as well as `<script lang="ts">` in `.vue` files
- Integrated automatic Let's Encrypt certificate registration/renewal

Overall, it would be desirable to develop modules for more common use-cases, such as file upload, user signup, email verification, etc, etc.

## License

MIT &copy; Nathaniel Baughman &bull; [License](LICENSE.txt)
