package net.nyhm.katapult

import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.valueParameters

/**
 * An Injector holds a collection of dependency factories,
 * which may provide dependency instances to a [Resolver].
 */
class Injector {

  private val factories = mutableMapOf<KClass<*>,() -> Any>()

  fun <T:Any> inject(type: KClass<T>, factory: () -> T) = apply {
    factories[type] = factory
  }

  fun <T:Any> inject(type: KClass<T>, instance: T) = inject(type, factory = { instance })

  fun <T:Any> inject(instance: T) =
    inject(type = instance.javaClass.kotlin, instance = instance)

  fun <T:Any> provide(type: KClass<T>): Any {
    return attempt(type) ?: throw ResolverException("Unable to provide $type")
  }

  // TODO: support providing subtypes; eg,
  //  attempt(List::class) would match a LinkedList instance
  //
  fun <T:Any> attempt(type: KClass<T>): Any? = factories[type]?.invoke()
}

class Resolver(private val injector: Injector) {

  /**
   * Resolve (find constructor dependencies and instantiate) the given type.
   * The given type may refer to an object instance (singleton) or a class with exactly one constructor.
   * This Resolver's [Injector] will be used to find dependencies.
   *
   * Optionally provide a list ([Iterable]) of [given] dependencies.
   * These take precedence over [Injector] dependencies.
   * These are matched strictly by class (not subclass), for consistency with Injector.
   * (Notice: This rule might change to allow subclass matching in the future.)
   *
   * If the given dependencies contains multiple matches for a param,
   * the first one found will be used.
   *
   * @param type Class to be resolved
   * @param given External potential dependency instances
   */
  fun <T:Any> resolve(type: KClass<T>, given: Iterable<Any> = emptyList()): T =
      attempt(type, given, true) ?: throw ResolverException("Unable to resolve $type")

  /**
   * Same as [resolve] but may (optionally) return null rather than throw an exception if resolution fails.
   *
   * When [failMode] is false, this method will attempt to resolve the class, returning null if
   * dependencies cannot be met. When [failMode] is true, failure to provide a dependency will
   * raise an exception.
   *
   * Note: Even if [failMoe] is false, this may still throw an exception if rules are violated
   * (eg, class is not suitable for dependency resolution).
   */
  fun <T:Any> attempt(type: KClass<T>, given: Iterable<Any> = emptyList(), failMode: Boolean = false): T? {
    type.objectInstance?.let { return it } // singleton, no dependencies
    if (type.constructors.size != 1) {
      throw ResolverException("Classes must have exactly one constructor $type")
    }
    val const = type.constructors.first() // only one
    if (const.valueParameters.isEmpty()) return const.call() // no dependencies
    val deps = const.valueParameters.map {
      val param = it.type.classifier as KClass<*>
      // TODO: find { it::class.isSubclassOf(param) }
      val found = given.find { it::class == param } ?: injector.attempt(param)
      when {
        found == null && failMode -> throw ResolverException("Unable to resolve $type, missing param $param")
        found == null -> return null // stop iterating and return null (non-fail mode)
        else -> found
      }
    }
    return const.call(*deps.toTypedArray())
  }

  /**
   * Resolve a (potentially mutually-dependent) group of classes.
   *
   * Classes in the group may depend on themselves (as well as any
   * dependencies provided by this Resolver's [Injector]).
   *
   * All of the classes will be fully resolved or an exception will be thrown.
   *
   * The order of the returned types is not strictly defined, but wil be in the
   * order in which they were resolved (dependency order).
   * So, if TypeA depended on TypeB, then TypeB will be before TypeA.
   */
  fun resolveGroup(types: Set<KClass<*>>): List<Any> {
    val unresolved = types.toMutableList()
    val resolved = mutableListOf<Any>() // items that have been resolved
    var halt = false
    while (!halt && unresolved.isNotEmpty()) {
      halt = true // if nothing resolved this round, must stop
      val iter = unresolved.listIterator()
      while (iter.hasNext()) {
        val type = iter.next()
        attempt(type, resolved)?.let { // may depend on themselves
          resolved.add(it)
          iter.remove() // resolved
          halt = false // something resolved, keep resolving
        }
      }
    }
    if (unresolved.isNotEmpty()) throw ResolverException("Unable to resolve all types $unresolved")
    return resolved // these are in dependency order
  }

  // TODO: Params are matched by subclass in inject list, err on side of consistency with other
  //  operations in this class, which match by strict class type?
  fun <R> call(instance: Any, callable: KCallable<R>, inject: List<Any>): R {
    val deps = mutableListOf<Any>()
    //callable.instanceParameter?.let { deps.add(it) }
    deps.add(instance)
    callable.valueParameters.forEach {
      val p = it.type.classifier as KClass<*>
      inject.find { p.isSubclassOf(it::class) }?.let { deps.add(it) } ?: deps.add(resolve(p))
    }
    return callable.call(*deps.toTypedArray())
  }
}

class ResolverException(message: String, cause: Throwable? = null): Exception(message, cause)