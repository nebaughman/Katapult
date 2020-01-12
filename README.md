# Katapult

[Kotlin](https://kotlinlang.org/) oriented single-jar API+Web app starter kit using [Javalin](https://javalin.io) and [Vue-Cli](https://cli.vuejs.org/)

## Overview

The purpose of this project is to combine a number of useful libraries into a starting stack for other projects.

When built, Katapult provides a single executable `jar` file that can self-serve an API (REST or session-based) and Vue Web app (single-page or multi-page).

This makes for easy deployment. :bulb: Also consider building `localhost` Web apps.

> Important: This might not be suitable for production purposes. My primary motivation is to experiment and learn a thing or two.

### Ecosystem

**Server:**

- [Kotlin](https://kotlinlang.org/) (& [Java](https://java.com)): Core language
- [Javalin](https://javalin.io) (& [Jetty](https://www.eclipse.org/jetty/)): API server
- [Guice](https://github.com/google/guice): Dependency injection
- [Clikt](https://github.com/ajalt/clikt): Command-line processing
- [BCrypt](https://github.com/patrickfav/bcrypt): Secure salted password hashing
- [Exposed](https://github.com/JetBrains/Exposed): ORM/DAO DB-object mapping
- [SQLite](https://www.sqlite.org/): Database
- [JUnit](https://junit.org): Testing
- [SLF4J](https://www.slf4j.org/): Logging
- [Gradle](https://gradle.org/): Build system
- [ShadowJar](https://github.com/johnrengelman/shadow): Single-JAR bundler

Supports HTTPS by reading `fullchain.pem` and `privkey.pem` as provided by [Let's Encrypt](https://letsencrypt.org/) - no messing with `jks` files!

**Client:**

- [Vue](https://vuejs.org/): Front-end rendering
- [Vue-Cli](https://cli.vuejs.org/): Vue project tools
- [Vue Router](https://router.vuejs.org/): Front-end routing system
- [Vue-FontAwesome](https://github.com/FortAwesome/vue-fontawesome): FontAwesome fonts in Vue!
- [Bootstrap](https://getbootstrap.com): UI framework
- [Axios](https://github.com/axios/axios): AJAX calls
- [Yarn](https://yarnpkg.com): Dependency & Build system

> I'd like to add TypeScript to the ecosystem (for TS classes, as well as TS-based Vue components), but I don't have the configuration quite right yet.

### Project Structure

Two subprojects:

- The `api` project augments Javalin with a modular framework and dependency injection system. Example modules are included, which help with authentication and session management, database/DAO integration (via Exposed), HTTPS support, Mustache templates, CORS headers, and static Vue app hosting.

- The `app` project includes an example Vue-Cli app, including Vue Router, Bootstrap, and FontAwesome, configured as an SPA (but supporting multiple entry points). `yarn build` copies `dist` to the API server's `resources/app`, where it is bundled and served as static content.

### Deployment

Create the single executable jar via `shadowJar` (see `buildjar.sh`). All static resources (including Vue app) are bundled within the jar and served via Javalin/Jetty.

A [Java Runtime Environment](https://java.com) is required to run Katapult. In general, execute a jar like: 

```
java -jar <the.jar> --options...
```

`example.Main` runs a sample api/app. Kindly refer to the source for command-line options.

### Extension

The included api/app are for demonstration purposes. As Katapult is still very much an experimental work-in-progress, there is not a clearly defined way to utilize Katapult for other projects.

Consider [forking](https://help.github.com/articles/fork-a-repo/) Katapult and developing your app/api in a different directory structure. [Sync your fork](https://help.github.com/articles/syncing-a-fork/) for Katapult updates (and beware of breaking changes).

## Javalin Server

The `api` project integrates a number of components for building an HTTP(S) API (RESTful or otherwise) and hosting static pages (such as the Vue app).

- **Modules:** Katapult includes a simple module system. Modules extend `KatapultModule` and are given the opportunity to augment the Javalin server upon startup, such as adding API route handlers. Modules (and endpoints) take advantage of dependency injection via Guice.

- **Endpoints:** Katapult augments Javalin with dependency-injected endpoint handlers and a `@Body` annotation for conveniently extracting the request body as a typed object.

- **Database/DAO:** Experimenting with [Exposed](https://github.com/JetBrains/Exposed) for data access.
 
    - Sample SQLite module (should be easy to add others)
    
    - Sample `UserDao`, which further simplifies and isolates business logic from Exposed library (except for Exposed `transaction`, which is kind of hard to avoid)

- **HTTPS:** Katapult reads `fullchain.pem` and `privkey.pem` as provided by Let's Encrypt (and maybe other certificate authorities). _No messing around with `jks` files!_
  
  > Explaining how to obtain SSL certificate files (eg, from Let's Encrypt) is outside the scope of this project.
  
- **Templating:** Javalin supports template engines for server-rendered HTML. An example [Mustache](https://mustache.github.io/) Katapult module is provided.

- **External Data:** The example server stores external data in a data directory. This includes SQLite database, session files, SSL certificate files, etc. Refer to the command-line options for the example server.

### Now with Guice

Dependencies are resolved and injected using Guice dependency injection framework. Guice is used for Module startup and endpoint handling.

#### Modules

`KatapultModule` classes must be either `object` instances (Kotlin singletons) or constructable by Guice (with an `@Inject` constructor):

```
class MyModule @Inject constructor(private val myData: MyData): KatapultModule {
 ...
}
```

In the above example, `MyData` must be available to the Guice Injector. Refer to `example.Main` for an example of setting up an Injector for use by Katapult.

#### Endpoints

Katapult endpoint handlers are dependency-injected `KFunction` references. Katapult augments the Javalin `Context` with a `process(endpoint: KFunction<*>)` function. Setting up a route can look like this:

```
get("/api/path/my-resource") { it.process(::getMyResource) }
```

`::getMyResource` is a `KFunction` reference that will have its parameters injected. The following can be injected:

- Javalin's request `Context` instance
- At most one parameter annotated with `@Body` (explained below)
- Any other instance available to the configured Guice Injector

Katapult adds a convenience `@Body` annotation, which transforms the request body into an object (via Javalin's `Context.getBodyAsClass(..)`) and injects it into the handler function. An endpoint handler function might look like:

```
fun login(ctx: Context, userDao: UserDao, @Body data: LoginRequest) { ... }

data class LoginRequest(
  val username: String,
  val password: String
)
```

In the above example, `Context`, `UserDao`, and `LoginRequest` will be injected. Notice that `LoginRequest` is a data class that is extracted from the request body. The Guice Injector configured for the Katapult server must be able to obtain a `UserDao` instance.

> Configuring the Guice Injector is a bit out-of-scope for these instructions, but have a look at `example.Main` and read the Guice instructions.

## Vue App

The `app` project is a sample Vue-Cli SPA with some additional configuration (Bootstrap, FontAwesome, Axios, etc). The Vue app itself is just an example starting point.

The primary point is that the Vue app can be served as static content from the Javalin server, allowing the whole app/api to be bundled as a single executable jar.

If bundling is not desirable, the Vue app could be hosted separately.

> Hosting the API on a different domain from the app may require CORS headers to be configured correctly. A CORS module is provided, but has not been well tested.

## Hybrid Multi-Page Mode

> **DEPRECATED**
> 
> This was tricky to configure, and I found very little information on this type of setup, so here is a rather verbose description of this experiment.
>
> This multi-mode configuration worked, but was a bit awkward. As of Katapult v0.4.0, the API & app are configured for a more traditional SPA. Note that `/login` is still a separate entrypoint, but is isolated from the main SPA, so there is no complication between server-side and client-side routing.

In the example setup, the Javalin server and Vue app are configured for multi-page mode _with_ [Vue Router](https://router.vuejs.org/) support. 

Vue Router is typically used in a single-page app (SPA) to handle all navigation links client-side. In multi-page mode, some links may need to be traditional page reloads (server-handled), while others are client-side Vue-routed.

Javalin has support for multiple SPA root paths, but the configuration is particular. See `AppModule`, which uses `JavalinConfig.addSinglePageRoot(..)`. Each root of the multi-page app is configured as an SPA root. These are matched in the order configured (not by most-specific path).

For example, the sample setup has three entrypoint _pages_ configured in `vue.config.js`: main (`/`), `/login`, and `/admin`. Login is isolated (no links), so can be ignored. Since main and admin have a common header, with links between each other, they need to be configured as single-page roots. Notice that `/admin` must be added first, so it is matched before main (`/`).

What is more, Vue Router has been configured in [HTML5 History Mode](https://router.vuejs.org/guide/essentials/history-mode.html), to avoid `/#/`-routed URLs. This takes extra care to handle properly in multi-page mode.
 
To support history mode, intra-page links must be rendered with the `<router-link>` component. Links to other pages should be hard links (`<a href="..">`), which are handled server-side, and cause a traditional page reload.

To accomplish this, see `NavLink.vue`. It determines if there is an active Vue Router instance (`this.$router`) and whether it handles the given path. If so, `<router-link>` is used, otherwise just `<a href="..">`.

This way, when in the main (`/`) page scope, links to anything under `/admin` are hard links. When in the `/admin` page scope, intra-admin links (`/admin/users`) are handled by Vue Router (no page reload), while links to other pages are traditional page reloads.

This multi-page/SPA hybrid setup has some caveats, but is working, at least for this simple example app!

## Wishlist

- Bring the `app` and `api` projects together under one build environment (maybe by having gradle run the yarn scripts)
- Rewrite `build.gradle` in Kotlin script
- Cross-compile the Kotlin data classes used by the API for both JS and JVM, so the same implementations can be sent and received on each side
- Allow for TypeScript `.ts` files, as well as `<script lang="ts">` in `.vue` files (preliminary `tsconfig.json` added, but not yet well-tested)
- Integrated automatic Let's Encrypt certificate registration/renewal
- Meaningful unit tests

Overall, it would be desirable to develop modules for more common use-cases, such as file upload, user signup, email verification, etc, etc.

> Advice is most welcome!

## Development

- This project follows a development process inspired by [nvie's GIT branching model](https://nvie.com/posts/a-successful-git-branching-model/)

- The [git](https://git-scm.com/) source repository is hosted on [GitHub](https://github.com/nebaughman/Katapult)

- I develop in [Ubuntu](https://www.ubuntu.com/) with [IntelliJ IDEA](https://www.jetbrains.com/idea/) (Community Edition)

## License

MIT &copy; Nathaniel Baughman &bull; [License](LICENSE.txt)
