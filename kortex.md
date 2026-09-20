# Kordex

Minecraft의 Bukkit/Paper, Fabric, Forge에서 서로 다른 명령어 API를 하나의 Kotlin DSL로 통합하는 멀티 플랫폼 Command Framework를 구현해줘.

프로젝트 이름:

```text
Kordex
```

기본 패키지:

```text
com.choverse.kordex
```

언어:

```text
Kotlin
```

빌드 시스템:

```text
Gradle Kotlin DSL
```

JVM:

```text
Java 21
```

Kordex는 Java 호환성보다 Kotlin DSL의 가독성, 타입 안정성, 간결함을 최우선으로 한다.

특히 다음 두 기능은 Kordex의 핵심 기능이다.

```text
1. then 기반 Command Tree DSL
2. plugin.yml에 commands / permissions를 작성하지 않는 완전 자동 등록
```

---

# 프로젝트 목적

Bukkit/Paper, Fabric, Forge는 명령어 등록 방식이 서로 다르다.

Kordex는 플랫폼별 Command API를 직접 사용하지 않고 하나의 Kotlin DSL만으로 명령어를 정의하고 등록할 수 있도록 한다.

최종적으로:

```kotlin
kordex {
    command("admin") {

        permission("server.admin")

        then("kick") {
            player("target") {
                optionalString("reason") {
                    executes {
                        val target = player("target")
                        val reason =
                            stringOrNull("reason")
                                ?: "Kicked by administrator"

                        target.kick(reason)
                    }
                }
            }
        }

        then("reload") {
            executes {
                reply("Reloaded")
            }
        }
    }
}
```

위 코드만 작성하면 명령어와 Permission이 자동 등록되어야 한다.

결과:

```text
/admin kick <target> [reason]
/admin reload
```

---

# 매우 중요한 기능: plugin.yml 자동 명령 등록

Bukkit/Paper에서 Kordex를 사용하는 개발자는 `plugin.yml`에 명령어를 작성하면 안 된다.

예를 들어 기존 Bukkit 방식:

```yaml
commands:
  admin:
    description: Admin command
    permission: server.admin
    aliases:
      - adm
```

이런 설정을 요구하지 않는다.

Kordex DSL에:

```kotlin
command("admin") {

    description("Admin command")

    aliases(
        "adm",
        "administrator"
    )

    permission("server.admin")

    executes {
        reply("Admin")
    }
}
```

라고 작성하는 것만으로 Kordex가 런타임에 자동 등록해야 한다.

즉 `plugin.yml`에는 기본 플러그인 정보만 있으면 된다.

예:

```yaml
name: ExamplePlugin
version: 1.0.0
main: com.example.ExamplePlugin
api-version: '1.21'
```

다음을 작성하지 않아도 된다.

```yaml
commands:
permissions:
```

Kordex가 자동으로 처리한다.

---

# Paper 명령어 자동 등록

Paper에서는 최신 Paper Command API를 우선 사용한다.

현재 Paper 버전에서 공식적으로 권장되는 API를 확인하고 구현한다.

가능하면:

```text
LifecycleEventManager
LifecycleEvents.COMMANDS
Commands registrar
Brigadier command tree
```

를 이용한다.

Kordex의:

```kotlin
command("hello") {
    executes {
        reply("Hello")
    }
}
```

가 자동으로 Paper command registrar에 등록되어야 한다.

사용자가 다음과 같은 코드를 작성할 필요가 없어야 한다.

```kotlin
getCommand("hello")?.setExecutor(...)
```

또한 다음도 필요 없어야 한다.

```yaml
commands:
  hello:
```

---

# Bukkit 자동 등록

순수 Bukkit/Spigot 호환 Adapter에서도 `plugin.yml`의 `commands:`에 의존하지 않는다.

Bukkit CommandMap API를 사용하여 런타임 등록할 수 있는 구조를 구현한다.

가능하면 공식 public API를 우선한다.

reflection으로 private field에 직접 접근하는 구현은 최후의 수단으로만 사용한다.

지원되는 서버 API에서:

```text
CommandMap
Command
Command#unregister
```

등을 이용해 동적으로 등록 및 해제한다.

Kordex 내부에서:

```kotlin
command("hello") {
}
```

가 호출되면 자동으로 CommandMap에 등록되어야 한다.

---

# Command Metadata 자동 처리

Kordex DSL에 작성된 metadata 역시 자동 적용한다.

예:

```kotlin
command("teleport") {

    description("Teleport another player")

    usage("/teleport <target>")

    aliases(
        "tp",
        "tele"
    )

    permission("server.teleport")
}
```

Kordex가 자동으로:

```text
command name
description
usage
aliases
permission
```

정보를 플랫폼 명령 시스템에 전달해야 한다.

`plugin.yml`과 중복 작성하면 안 된다.

---

# Permission 자동 등록

Permission 역시 `plugin.yml`의 `permissions:`에 작성하지 않는다.

기존 방식:

```yaml
permissions:
  server.admin:
    description: Server administrator
    default: op
```

이 방식은 Kordex에서 필수가 아니어야 한다.

다음 코드만으로:

```kotlin
command("admin") {
    permission("server.admin")
}
```

Kordex가 Permission을 발견하고 자동 등록한다.

---

# Permission Definition DSL

단순 Permission뿐 아니라 metadata도 선언할 수 있게 한다.

예:

```kotlin
permission("server.admin") {
    description = "Server administrator permission"
    default = PermissionDefault.OP
}
```

또는 command와 함께:

```kotlin
command("admin") {

    permission("server.admin") {
        description = "Allows admin commands"
        default = PermissionDefault.OP
    }

    executes {
        reply("Admin command")
    }
}
```

---

# Kordex PermissionDefault

core 모듈에서 Bukkit 타입을 사용하면 안 되므로 자체 enum을 만든다.

예:

```kotlin
enum class PermissionDefault {
    TRUE,
    FALSE,
    OP,
    NOT_OP
}
```

각 플랫폼 Adapter가 자신의 permission 시스템으로 변환한다.

---

# Permission Children

Permission children도 DSL로 선언할 수 있게 한다.

```kotlin
permission("server.admin") {

    description =
        "All administrator permissions"

    default =
        PermissionDefault.OP

    children {
        permission(
            "server.admin.kick",
            true
        )

        permission(
            "server.admin.ban",
            true
        )

        permission(
            "server.admin.reload",
            true
        )
    }
}
```

Bukkit/Paper에서는 이를 런타임 Permission 객체로 변환하여 등록한다.

---

# Permission Registry

core에 다음과 비슷한 구조를 둔다.

```text
PermissionDefinition
PermissionRegistry
PermissionDefault
PermissionChild
```

Command Tree를 분석하여 사용된 Permission을 자동 수집한다.

예:

```kotlin
command("test") {

    permission("example.test")

    then("admin") {

        permission("example.test.admin")

        executes {
        }
    }
}
```

자동 수집:

```text
example.test
example.test.admin
```

---

# 중복 Permission 처리

이미 동일 Permission이 플랫폼에 등록되어 있을 수 있다.

따라서 무조건 등록하다가 예외를 발생시키면 안 된다.

먼저 기존 Permission을 확인한다.

동일 Permission이 있으면 안전하게 재사용하거나 Kordex 정책에 따라 metadata를 병합한다.

다른 플러그인이 이미 같은 Permission을 등록했다면 함부로 삭제하거나 덮어쓰지 않는다.

Kordex가 직접 등록한 Permission과 외부 Permission을 구분해서 관리한다.

---

# Kordex 종료 시 정리

Kordex가 등록한 명령어와 Permission을 추적한다.

플랫폼이 지원하면:

```kotlin
Kordex.shutdown()
```

또는 플러그인 disable 시 자동으로:

```text
command unregister
permission unregister
internal registry clear
```

를 수행한다.

다른 플러그인이 등록한 Permission이나 Command를 삭제하면 안 된다.

---

# 선언만 하면 자동 등록

Kordex의 핵심 UX는 다음과 같아야 한다.

플러그인 개발자가:

```kotlin
override fun onEnable() {

    kordex(this) {

        command("hello") {

            permission("example.hello")

            executes {
                reply("Hello!")
            }
        }
    }
}
```

라고 작성하면 끝이어야 한다.

다음 작업은 필요 없어야 한다.

```text
plugin.yml command 작성
plugin.yml permission 작성
getCommand()
setExecutor()
setTabCompleter()
CommandMap 수동 접근
Permission 객체 수동 생성
PluginManager.addPermission 수동 호출
Brigadier dispatcher 수동 접근
```

모든 작업을 Kordex가 처리한다.

---

# 자동 초기화 API

Bukkit/Paper에서는 가능하면 다음처럼 사용할 수 있게 한다.

```kotlin
kordex(plugin) {

    command("hello") {
        executes {
            reply("Hello")
        }
    }
}
```

또는 JavaPlugin extension:

```kotlin
kordex {

    command("hello") {
        executes {
            reply("Hello")
        }
    }
}
```

예:

```kotlin
class ExamplePlugin : JavaPlugin() {

    override fun onEnable() {

        kordex {

            command("hello") {

                permission("example.hello")

                executes {
                    reply("Hello!")
                }
            }
        }
    }
}
```

이 코드만으로 실제 `/hello` 명령어가 서버에 등록되어야 한다.

---

# 핵심 then DSL

Kordex에서 다음 Literal Node는 `then`으로 표현한다.

```kotlin
command("party") {

    then("invite") {

        player("target") {

            executes {
                val target =
                    player("target")
            }
        }
    }

    then("accept") {

        player("player") {

            executes {
                val player =
                    player("player")
            }
        }
    }
}
```

결과:

```text
/party invite <target>
/party accept <player>
```

`then`은 단순 alias가 아니라 Kordex Command Tree의 공식적인 child-node 연결 방식이다.

---

# then

Literal:

```kotlin
command("admin") {

    then("reload") {

        executes {
            reply("Reloaded")
        }
    }
}
```

Nested:

```kotlin
command("admin") {

    then("player") {

        then("ban") {

            player("target") {

                executes {
                }
            }
        }
    }
}
```

결과:

```text
/admin player ban <target>
```

---

# then(argument)

Argument Definition도 `then`으로 연결할 수 있게 한다.

```kotlin
command("give") {

    then(
        playerArgument("target")
    ) {

        then(
            integerArgument(
                "amount",
                min = 1
            )
        ) {

            executes {

                val target =
                    player("target")

                val amount =
                    int("amount")
            }
        }
    }
}
```

---

# Argument DSL

다음을 기본 지원한다.

```kotlin
string("name") {
}

greedyString("message") {
}

integer("amount") {
}

integer(
    "amount",
    min = 1,
    max = 100
) {
}

long("value") {
}

float("value") {
}

double("value") {
}

boolean("enabled") {
}

player("target") {
}

enum<MyEnum>("mode") {
}
```

Factory API:

```text
stringArgument
greedyStringArgument
integerArgument
longArgument
floatArgument
doubleArgument
booleanArgument
playerArgument
enumArgument
```

---

# Context Argument API

```kotlin
val name =
    string("name")

val amount =
    int("amount")

val target =
    player("target")
```

지원:

```text
string
stringOrNull

int
intOrNull

long
longOrNull

float
floatOrNull

double
doubleOrNull

boolean
booleanOrNull

player
playerOrNull

argument<T>
argumentOrNull<T>
```

Kotlin `inline` + `reified`를 적극 활용한다.

---

# Optional Argument

```kotlin
optionalString("reason") {

    executes {

        val reason =
            stringOrNull("reason")
    }
}
```

지원:

```text
optionalString
optionalInteger
optionalLong
optionalFloat
optionalDouble
optionalBoolean
optionalPlayer
```

Brigadier에서는 optional node 자체가 없으므로 command tree branching/executor 배치를 이용하여 올바르게 구현한다.

---

# Suggestions

```kotlin
string("world") {

    suggests {

        listOf(
            "world",
            "world_nether",
            "world_the_end"
        )
    }
}
```

Context 접근도 지원한다.

---

# Alias

```kotlin
command("teleport") {

    aliases(
        "tp",
        "tele"
    )
}
```

Alias도 자동으로 플랫폼 명령 시스템에 등록한다.

`plugin.yml`에 alias를 작성할 필요가 없다.

---

# Description / Usage

```kotlin
command("teleport") {

    description(
        "Teleport to another player"
    )

    usage(
        "/teleport <player>"
    )
}
```

metadata 역시 자동 등록한다.

---

# Sender Abstraction

```kotlin
interface KordexSender {

    val name: String

    val isPlayer: Boolean

    fun sendMessage(
        message: String
    )

    fun hasPermission(
        permission: String
    ): Boolean
}
```

---

# Player Abstraction

```kotlin
interface KordexPlayer :
    KordexSender {

    val uuid: UUID

    fun kick(
        message: String
    )
}
```

---

# Native Access

필요한 경우:

```kotlin
sender.native<T>()
```

예:

```kotlin
val bukkitSender =
    sender.native<CommandSender>()
```

일반적인 명령어에서는 사용할 필요가 없어야 한다.

---

# Error Handling

```kotlin
fail(
    "Player not found"
)
```

또는:

```kotlin
throw CommandException(
    "Player not found"
)
```

---

# Custom Argument

```kotlin
object WorldArgument :
    Argument<KordexWorld> {

    override fun parse(
        input: String,
        context: CommandContext
    ): KordexWorld {

        // 실제 구현
    }
}
```

사용:

```kotlin
argument(
    "world",
    WorldArgument
) {

    executes {

        val world =
            argument<KordexWorld>(
                "world"
            )
    }
}
```

---

# Custom DSL Extension

```kotlin
fun NodeBuilder.world(
    name: String,
    block:
        ArgumentBuilder<KordexWorld>.() -> Unit
) {

    argument(
        name,
        WorldArgument,
        block
    )
}
```

---

# DSL Marker

```kotlin
@DslMarker
annotation class KordexDsl
```

모든 DSL Builder에 적용한다.

---

# Project Structure

```text
Kordex/
├─ build.gradle.kts
├─ settings.gradle.kts
├─ gradle.properties
│
├─ kordex-core/
├─ kordex-bukkit/
├─ kordex-paper/
├─ kordex-fabric/
├─ kordex-forge/
│
└─ examples/
   ├─ paper-example/
   ├─ bukkit-example/
   ├─ fabric-example/
   └─ forge-example/
```

Paper 전용 최신 API 사용을 위해 `kordex-paper`를 `kordex-bukkit`과 분리하는 것을 권장한다.

---

# Core Architecture

```text
Kordex

CommandRegistry
CommandDefinition

CommandNode
LiteralNode
ArgumentNode
ExecutionNode

CommandBuilder
NodeBuilder
ArgumentBuilder<T>

CommandContext

KordexSender
KordexPlayer

CommandPlatform

Argument<T>
ArgumentDefinition<T>

PermissionDefinition
PermissionRegistry
PermissionDefault

Requirement
SuggestionProvider

CommandResult
CommandException
```

core에는 특정 Minecraft 플랫폼 클래스가 포함되면 안 된다.

---

# Paper Adapter

구현:

```text
PaperCommandPlatform
PaperCommandRegistrar
PaperPermissionRegistrar

PaperSender
PaperPlayer
```

Paper에서는 최신 공식 Command API를 우선한다.

Kordex Command Tree를 Brigadier Tree로 변환한다.

Command 등록 시 `plugin.yml`의 `commands:`를 사용하지 않는다.

Permission은 Bukkit/Paper runtime Permission Manager를 통해 자동 등록한다.

---

# Bukkit Adapter

구현:

```text
BukkitCommandPlatform
BukkitCommandRegistrar
BukkitPermissionRegistrar

BukkitSender
BukkitPlayer
```

`plugin.yml` command 등록에 의존하면 안 된다.

지원되는 Bukkit CommandMap API를 이용하여 동적으로 등록한다.

Command 충돌 시 namespace/fallback prefix를 적절히 처리한다.

---

# Fabric Adapter

구현:

```text
FabricCommandPlatform
FabricSender
FabricPlayer
```

Fabric Command Registration API와 Brigadier를 이용한다.

Fabric에는 `plugin.yml` 개념이 없으므로 DSL 정의가 곧 실제 등록 정보가 된다.

---

# Forge Adapter

구현:

```text
ForgeCommandPlatform
ForgeSender
ForgePlayer
```

Forge Command Registration Event와 Brigadier를 이용한다.

Forge에서도 별도 설정 파일에 명령어를 중복 선언하도록 요구하지 않는다.

---

# Permission Platform SPI

플랫폼별 Permission 시스템이 다르므로 core에 SPI를 만든다.

예:

```kotlin
interface PermissionPlatform {

    fun register(
        permission:
            PermissionDefinition
    )

    fun unregister(
        permission: String
    )

    fun isRegistered(
        permission: String
    ): Boolean
}
```

Paper/Bukkit은 실제 Permission Registry와 연결한다.

Fabric/Forge에서는 플랫폼 기본 권한 수준 또는 별도 permission integration module로 연결할 수 있도록 확장 가능한 구조를 만든다.

---

# Command Platform

```kotlin
interface CommandPlatform {

    fun register(
        command:
            CommandDefinition
    )

    fun unregister(
        name: String
    )
}
```

---

# 자동 Registration Lifecycle

등록 과정은 대략 다음과 같아야 한다.

```text
Kordex DSL
    ↓
CommandDefinition 생성
    ↓
Command Tree 검증
    ↓
Permission 수집
    ↓
Permission 자동 등록
    ↓
Platform Command Tree 변환
    ↓
Command 자동 등록
    ↓
Alias 자동 등록
    ↓
Suggestion 등록
```

개발자가 중간 과정을 직접 호출하면 안 된다.

---

# Command 충돌 처리

동일한 command가 이미 서버에 존재할 수 있다.

예:

```text
/help
/version
```

Kordex는 충돌을 감지해야 한다.

플랫폼에서 namespace가 가능하면:

```text
pluginname:command
```

형태의 fallback namespace를 지원한다.

외부 플러그인의 명령어를 임의로 제거하거나 덮어쓰면 안 된다.

---

# Register / Unregister

런타임 등록도 가능하게 한다.

```kotlin
val command =
    command("temporary") {

        executes {
            reply("Temporary")
        }
    }

Kordex.register(command)
```

그리고:

```kotlin
Kordex.unregister(
    "temporary"
)
```

를 지원한다.

가능하면 tab completion / Brigadier command tree도 즉시 갱신한다.

---

# 최종적인 Bukkit/Paper 사용 예

```kotlin
class ExamplePlugin :
    JavaPlugin() {

    override fun onEnable() {

        kordex {

            command("party") {

                description(
                    "Party management"
                )

                permission(
                    "example.party"
                ) {
                    default =
                        PermissionDefault.TRUE
                }

                then("invite") {

                    player("target") {

                        executes {

                            val target =
                                player("target")

                            reply(
                                "${target.name} invited"
                            )
                        }
                    }
                }

                then("admin") {

                    permission(
                        "example.party.admin"
                    ) {
                        default =
                            PermissionDefault.OP
                    }

                    then("reload") {

                        executes {
                            reply("Reloaded")
                        }
                    }
                }
            }
        }
    }
}
```

이 플러그인의 `plugin.yml`:

```yaml
name: ExamplePlugin
version: 1.0.0
main: com.example.ExamplePlugin
api-version: '1.21'
```

끝이다.

아래 내용이 없어야 한다.

```yaml
commands:
permissions:
```

---

# Artifact

```text
com.choverse:kordex-core
com.choverse:kordex-bukkit
com.choverse:kordex-paper
com.choverse:kordex-fabric
com.choverse:kordex-forge
```

---

# Unit Test

Minecraft 서버 없이 core를 테스트할 수 있어야 한다.

Mock:

```text
MockCommandPlatform
MockPermissionPlatform
MockSender
MockPlayer
```

다음 항목을 테스트한다.

```text
command

then

nested then

then(argument)

argument parsing

optional arguments

permission

permission metadata

permission children

permission collection

automatic permission registration

automatic command registration

aliases

metadata

suggestions

custom arguments

custom DSL extensions

command conflict

command unregister

permission unregister
```

---

# 자동 등록 테스트

특히 다음 DSL:

```kotlin
kordex {

    command("admin") {

        description(
            "Admin command"
        )

        aliases(
            "adm"
        )

        permission(
            "example.admin"
        ) {
            default =
                PermissionDefault.OP
        }

        then("reload") {

            executes {
            }
        }
    }
}
```

를 등록한 뒤 MockPlatform에:

```text
Command:
admin

Alias:
adm

Permission:
example.admin

Subcommand:
reload
```

가 자동으로 들어갔는지 검증한다.

---

# 실제 Paper Integration Test

가능하면 Paper 테스트 서버 또는 MockBukkit/적절한 테스트 환경에서 다음을 검증한다.

`plugin.yml`:

```yaml
name: KordexTest
version: 1.0.0
main: com.example.KordexTest
api-version: '1.21'
```

여기에는:

```yaml
commands:
permissions:
```

가 없어야 한다.

그 상태에서 서버를 실행한 뒤:

```text
/test
/admin reload
```

명령어가 정상 동작해야 한다.

Permission도 서버 runtime registry에 존재해야 한다.

---

# 구현 규칙

TODO를 남기지 않는다.

placeholder를 작성하지 않는다.

의사 코드만 작성하지 않는다.

다음을 남기지 않는다.

```kotlin
TODO()
```

```kotlin
throw NotImplementedError()
```

빈 임시 구현도 남기지 않는다.

전체 프로젝트가 실제로 컴파일되어야 한다.

최신 플랫폼 API가 중요하므로 구현 전에 현재 사용하는 Paper, Bukkit, Fabric, Forge API의 공식 문서를 확인한다.

Deprecated API는 특별한 이유가 없는 한 사용하지 않는다.

Paper에서 최신 공식 Command API가 존재하면 reflection 기반 CommandMap 구현보다 공식 API를 우선한다.

Bukkit 호환 Adapter에서는 지원 가능한 public API를 최대한 활용한다.

core에는:

```text
Bukkit
Paper
Fabric
Forge
Minecraft
```

관련 클래스 import가 하나도 없어야 한다.

---

# 절대로 하면 안 되는 구현

Kordex 사용자가 다음 코드를 작성하도록 만들면 안 된다.

```kotlin
getCommand("test")!!
    .setExecutor(...)
```

다음도 안 된다.

```kotlin
getCommand("test")!!
    .tabCompleter = ...
```

다음 설정도 요구하면 안 된다.

```yaml
commands:
  test:
```

다음 설정도 요구하면 안 된다.

```yaml
permissions:
  example.test:
```

Kordex DSL만으로 전부 처리해야 한다.

---

# 작업 순서

1. 프로젝트 구조 확인

2. Gradle Multi-Module 구성

3. kordex-core 구현

4. Command Tree 구현

5. `then` DSL 구현

6. Argument System 구현

7. Permission Definition/Registry 구현

8. Command metadata 구현

9. Command 자동 등록 architecture 구현

10. Permission 자동 등록 architecture 구현

11. Mock Platform 구현

12. Unit Test 작성

13. Paper Adapter 구현

14. Bukkit Adapter 구현

15. Fabric Adapter 구현

16. Forge Adapter 구현

17. Example 프로젝트 구현

18. 전체 빌드

19. 컴파일 오류 수정

20. 테스트 실행

21. README 작성

---

# 완료 조건

Windows:

```text
gradlew.bat clean build
```

Linux/macOS:

```text
./gradlew clean build
```

성공해야 한다.

다음 기능이 실제 구현되어 있어야 한다.

```text
command

then

nested then

multiple then

then(argument)

executes

string

greedyString

integer

long

float

double

boolean

player

enum

optional arguments

aliases

description

usage

permissions

permission metadata

permission defaults

permission children

automatic permission discovery

automatic permission registration

automatic command registration

automatic alias registration

custom requirements

suggestions

custom suggestions

custom arguments

custom DSL extensions

exceptions

sender abstraction

player abstraction

native platform access

command unregister

permission unregister
```

가장 중요한 완료 조건:

```text
plugin.yml에 commands:가 없어도
Kordex 명령어가 정상 등록되어야 한다.

plugin.yml에 permissions:가 없어도
Kordex Permission이 정상 등록되어야 한다.
```

즉 다음 정도의 `plugin.yml`만으로:

```yaml
name: ExamplePlugin
version: 1.0.0
main: com.example.ExamplePlugin
api-version: '1.21'
```

이 Kotlin 코드가:

```kotlin
kordex {

    command("admin") {

        permission(
            "example.admin"
        ) {
            default =
                PermissionDefault.OP
        }

        then("kick") {

            player("target") {

                executes {

                    val target =
                        player("target")

                    target.kick(
                        "Kicked"
                    )
                }
            }
        }
    }
}
```

별도의 YAML 선언 없이 실제로:

```text
/admin kick <target>
```

명령어와:

```text
example.admin
```

Permission을 자동 등록해야 한다.

이 기능은 선택 기능이 아니라 **Kordex의 기본 동작**으로 구현해줘.




# Kordex 추가 요구사항: Permission-Aware Suggestions / Command Visibility

Kordex에는 **사용자의 권한 및 조건에 따라 명령어 Tree와 자동완성 결과를 동적으로 필터링하는 기능**이 반드시 있어야 한다.

단순히 명령 실행 단계에서 Permission을 검사하는 것으로 끝내면 안 된다.

사용자에게 권한이 없는 명령어는 `/` 명령 자동완성 및 TAB Completion에서도 보이지 않아야 한다.

---

# 목표

예를 들어 다음 명령어가 있다고 가정한다.

```text
/test admin
/test user
```

`admin`은 관리자 전용이고 `user`는 일반 사용자 전용이다.

관리자가:

```text
/test <TAB>
```

을 입력하면:

```text
admin
```

만 보여야 한다.

일반 사용자가:

```text
/test <TAB>
```

을 입력하면:

```text
user
```

만 보여야 한다.

즉 사용자마다 보이는 Command Tree 자체가 달라져야 한다.

---

# 기본 DSL

다음처럼 사용할 수 있게 한다.

```kotlin
command("test") {

    then("admin") {

        permission("test.admin")

        executes {
            reply("관리자 명령어")
        }
    }

    then("user") {

        requires {
            !sender.hasPermission("test.admin")
        }

        executes {
            reply("일반 사용자 명령어")
        }
    }
}
```

이 경우:

```text
test.admin 권한 있음
→ /test admin 만 자동완성에 표시

test.admin 권한 없음
→ /test user 만 자동완성에 표시
```

---

# Permission은 실행 제한 + 자동완성 제한

다음 코드를:

```kotlin
then("admin") {

    permission("test.admin")

    executes {
    }
}
```

작성하면 `permission()`은 기본적으로 두 가지 역할을 모두 수행해야 한다.

```text
1. 명령 실행 권한 검사
2. Command Suggestion / Tree Visibility 검사
```

즉 권한이 없는 사용자가:

```text
/test <TAB>
```

을 입력했을 때 `admin`이 표시되면 안 된다.

또한 직접:

```text
/test admin
```

을 입력해도 실행되면 안 된다.

---

# requires 역시 자동완성에 반영

다음처럼 Custom Requirement를 사용해도:

```kotlin
then("secret") {

    requires {
        sender.name == "Example"
    }

    executes {
    }
}
```

조건을 만족하지 않는 사용자에게는 `secret`이 자동완성에 노출되면 안 된다.

즉 `requires {}`는 단순 Execution Predicate가 아니라 Command Node Requirement로 취급한다.

Brigadier의 `.requires {}`와 자연스럽게 대응할 수 있도록 설계한다.

---

# adminOnly / userOnly DSL

자주 사용하는 경우를 위해 편의 DSL도 제공한다.

예:

```kotlin
command("test") {

    then("admin") {

        adminOnly(
            permission = "test.admin"
        )

        executes {
            reply("Admin")
        }
    }

    then("user") {

        userOnly(
            adminPermission = "test.admin"
        )

        executes {
            reply("User")
        }
    }
}
```

동작:

```text
관리자
/test <TAB>
→ admin

일반 사용자
/test <TAB>
→ user
```

`adminOnly()`와 `userOnly()`는 내부적으로 일반적인 Requirement 시스템을 사용하도록 구현한다.

별도의 하드코딩된 Command 처리 로직을 만들지 않는다.

---

# 더 일반적인 Visibility DSL

명령 실행 조건과 표시 조건을 세밀하게 분리하고 싶은 경우도 지원한다.

예:

```kotlin
then("debug") {

    visibleIf {
        sender.hasPermission(
            "server.debug.view"
        )
    }

    requires {
        sender.hasPermission(
            "server.debug.use"
        )
    }

    executes {
        reply("Debug")
    }
}
```

이 경우:

```text
server.debug.view
```

권한이 있어야 자동완성에서 보이고,

```text
server.debug.use
```

권한이 있어야 실제 실행할 수 있다.

---

# 기본 정책

단, 별도로 `visibleIf {}`를 지정하지 않았다면:

```kotlin
requires {
    ...
}
```

조건을 Visibility에도 자동으로 사용한다.

즉 기본 동작:

```text
requires = 실행 가능 여부 + 표시 가능 여부
```

이다.

필요한 경우에만:

```kotlin
visibleIf {
}
```

를 사용하여 분리한다.

---

# 숨김 전용 DSL

실행은 가능하지만 자동완성에서는 숨기는 기능도 지원하면 좋다.

예:

```kotlin
then("internal") {

    hidden()

    executes {
        reply("Internal")
    }
}
```

이 경우:

```text
/test <TAB>
```

에서는 `internal`이 나오지 않는다.

하지만 사용자가 정확한 명령어를 알고 있다면:

```text
/test internal
```

은 실행할 수 있다.

Permission이 있다면 실행되는 구조다.

---

# Suggestion Visibility

Argument suggestion에도 동일한 원칙을 적용한다.

예:

```kotlin
string("action") {

    suggests {

        buildList {

            add("info")

            if (
                sender.hasPermission(
                    "server.admin"
                )
            ) {
                add("reload")
                add("shutdown")
            }
        }
    }
}
```

관리자:

```text
info
reload
shutdown
```

일반 사용자:

```text
info
```

---

# Suggestion DSL 개선

다음 API도 제공한다.

```kotlin
suggests {
    suggestion("info")

    suggestion("reload") {
        requires {
            sender.hasPermission(
                "server.admin"
            )
        }
    }

    suggestion("shutdown") {
        requires {
            sender.hasPermission(
                "server.admin"
            )
        }
    }
}
```

이 구조를 통해 개별 suggestion에도 Requirement를 적용할 수 있다.

---

# Nested Permission Filtering

중첩된 명령에서도 정확하게 동작해야 한다.

예:

```kotlin
command("server") {

    then("info") {

        executes {
        }
    }

    then("admin") {

        permission(
            "server.admin"
        )

        then("reload") {

            executes {
            }
        }

        then("shutdown") {

            permission(
                "server.shutdown"
            )

            executes {
            }
        }
    }
}
```

일반 사용자:

```text
/server <TAB>

info
```

`server.admin` 사용자:

```text
/server <TAB>

info
admin
```

그리고:

```text
/server admin <TAB>

reload
```

`server.shutdown`까지 가진 관리자:

```text
/server admin <TAB>

reload
shutdown
```

처럼 각각의 Node Requirement를 기준으로 동적으로 Command Tree가 필터링되어야 한다.

---

# 부모 Node 자동 숨김

특히 중요한 기능이다.

어떤 Literal Node 아래의 모든 자식이 현재 사용자에게 보이지 않는다면 부모 Node도 자동으로 숨길 수 있어야 한다.

예:

```kotlin
command("test") {

    then("admin") {

        then("kick") {
            permission("test.admin.kick")
        }

        then("ban") {
            permission("test.admin.ban")
        }
    }
}
```

일반 사용자가:

```text
test.admin.kick
test.admin.ban
```

둘 다 가지고 있지 않으면:

```text
/test <TAB>
```

에서 `admin` 자체를 보여주지 않는다.

즉 접근 가능한 하위 명령이 하나도 없는 빈 Node가 자동완성에 노출되지 않도록 한다.

이 기능은 다음과 같은 정책으로 설정 가능하게 해도 된다.

```kotlin
kordex {
    visibility {
        hideEmptyParents = true
    }
}
```

기본값은:

```text
true
```

로 한다.

---

# Command Tree Security

자동완성에서 숨기는 것만으로 보안을 구현하면 안 된다.

다음 두 계층을 반드시 모두 검사한다.

```text
Suggestion / Visibility
Execution
```

즉 악의적인 사용자가 자동완성을 사용하지 않고 직접:

```text
/test admin
```

을 입력하더라도 Requirement를 다시 검사해야 한다.

Visibility는 UX 기능이고 Permission/Requirement는 실제 보안 계층이다.

---

# Brigadier 연동

Fabric / Forge / 최신 Paper처럼 Brigadier 기반 플랫폼에서는 가능한 경우 Kordex Requirement를 Brigadier Node의:

```text
requires
```

predicate로 변환한다.

이렇게 하여 Brigadier 자체가 사용자별 Command Tree와 Suggestions를 필터링하도록 한다.

Kordex가 별도의 문자열 기반 자동완성 시스템을 중복 구현하지 않도록 한다.

단 플랫폼에서 필요한 기능이 부족하면 Kordex Adapter에서 동일한 동작을 구현한다.

---

# Bukkit/Paper

Bukkit/Paper에서도 사용자의 Permission에 따라 Tab Completion을 필터링한다.

예:

```kotlin
command("test") {

    then("admin") {

        permission(
            "test.admin"
        )

        executes {
        }
    }

    then("user") {

        requires {
            !sender.hasPermission(
                "test.admin"
            )
        }

        executes {
        }
    }
}
```

관리자:

```text
/test <TAB>
admin
```

일반 사용자:

```text
/test <TAB>
user
```

가 실제 서버에서도 보장되어야 한다.

---

# Fabric / Forge

Fabric과 Forge에서는 Brigadier `requires` predicate를 적극 활용한다.

예를 들어 Kordex의:

```kotlin
permission(
    "server.admin"
)
```

을 해당 플랫폼의 Requirement predicate로 변환한다.

사용자가 볼 수 없는 Node는 클라이언트로 전송되는 Command Tree에서도 가능한 한 제외되어야 한다.

---

# Core Architecture 추가

다음 abstraction을 추가한다.

```text
CommandRequirement
CommandVisibility
VisibilityPredicate

Suggestion
SuggestionRequirement

CommandAudience
```

예:

```kotlin
fun interface CommandRequirement {

    fun test(
        context: RequirementContext
    ): Boolean
}
```

Visibility:

```kotlin
fun interface CommandVisibility {

    fun isVisible(
        context: RequirementContext
    ): Boolean
}
```

---

# Audience DSL

향후 확장 가능하도록 Audience 개념도 설계한다.

예:

```kotlin
then("admin") {

    audience {
        permission("test.admin")
    }
}
```

또는:

```kotlin
then("user") {

    audience {
        withoutPermission(
            "test.admin"
        )
    }
}
```

다음과 같은 조합도 가능하게 한다.

```kotlin
audience {

    permission(
        "server.staff"
    )

    predicate {
        sender.isPlayer
    }
}
```

---

# 최종 대표 예제

다음 DSL이 실제로 동작해야 한다.

```kotlin
kordex {

    command("test") {

        then("admin") {

            permission(
                "test.admin"
            )

            then("reload") {

                executes {
                    reply(
                        "Server reloaded"
                    )
                }
            }

            then("kick") {

                permission(
                    "test.admin.kick"
                )

                player("target") {

                    executes {

                        val target =
                            player("target")

                        target.kick(
                            "Kicked"
                        )
                    }
                }
            }
        }

        then("user") {

            requires {
                !sender.hasPermission(
                    "test.admin"
                )
            }

            then("profile") {

                executes {
                    reply(
                        "Your profile"
                    )
                }
            }
        }
    }
}
```

일반 사용자:

```text
/test <TAB>
user
```

그리고:

```text
/test user <TAB>
profile
```

관리자:

```text
/test <TAB>
admin
```

그리고 일반 관리자:

```text
/test admin <TAB>
reload
```

`test.admin.kick` 권한까지 가진 관리자:

```text
/test admin <TAB>
reload
kick
```

가 보여야 한다.

---

# 완료 조건 추가

다음 기능을 반드시 테스트한다.

```text
Permission-based command visibility

Requirement-based command visibility

User-specific tab completion

Admin-only commands

User-only commands

Nested permission filtering

Argument suggestion filtering

Hidden commands

visibleIf

requires

Permission execution check

Empty parent hiding
```

Mock 테스트도 작성한다.

예를 들어:

```text
User:
permissions = []

/test suggestions
→ ["user"]
```

```text
Admin:
permissions = ["test.admin"]

/test suggestions
→ ["admin"]
```

```text
Super Admin:
permissions = [
    "test.admin",
    "test.admin.kick"
]

/test admin suggestions
→ ["reload", "kick"]
```

이 결과를 자동 테스트한다.

가장 중요한 원칙:

```text
사용자가 실행할 수 없는 명령은
기본적으로 자동완성에서도 보여주지 않는다.
```

그리고:

```text
자동완성에서 숨겼더라도
실제 실행 시 Permission / Requirement를
반드시 다시 검사한다.
```

Kordex에서는 이 기능을 별도 애드온이 아니라 **기본 Command Tree 기능**으로 구현해줘.