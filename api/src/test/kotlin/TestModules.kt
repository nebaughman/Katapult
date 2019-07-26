import net.nyhm.katapult.Katapult
import net.nyhm.katapult.KatapultException
import net.nyhm.katapult.KatapultModule
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TestMod(a: SubA, b: SubB): KatapultModule

open class BaseMod: KatapultModule

class SubA(mod: InstMod): BaseMod()
class SubB(mod: InstMod, conf: SubBConf): BaseMod()

data class SubBConf(
    val value: Int = 1
)

object InstMod: KatapultModule

class TestModules {
  @Test
  fun testDependencies() {
    assertThrows<KatapultException> {
      Katapult(
          listOf(
              SubBConf()
          ),
          listOf(
              TestMod::class, SubA::class, SubB::class, InstMod::class, SubB::class // extra SubB
          )
      ).start()
    }

    assertThrows<KatapultException> {
      Katapult(
          listOf(),
          listOf(
              TestMod::class, SubA::class, InstMod::class // missing SubB
          )
      ).start()
    }

    assertThrows<KatapultException> {
      Katapult(
          listOf(), // missing SubBConf
          listOf(
              TestMod::class, SubA::class, InstMod::class, SubB::class
          )
      ).start()
    }

    assertThrows<KatapultException> {
      Katapult(
          listOf(
              SubBConf(3), SubBConf(4) // extra SubBConf
          ),
          listOf(
              TestMod::class, SubA::class, SubB::class, InstMod::class
          )
      ).start()
    }

    Katapult(
        listOf(
            SubBConf()
        ),
        listOf(
            TestMod::class, SubA::class, SubB::class, InstMod::class
        )
    ).start()

    // TODO: Better tests...
    //   - Fine-grained exception checking
    //   - Each dependency instance actually created
    //   - Each dependency created exactly once
    //   - ...
  }
}