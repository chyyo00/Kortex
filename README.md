# Kordex

Bukkit/Spigot, Paper, Fabric, Forge의 서로 다른 명령어 API를 **하나의 Kotlin DSL**로 통합하는 멀티 플랫폼
Command Framework 입니다. **Minecraft 1.21.11 ~ 26.2** 를 지원합니다. 명령어 트리를 한 번만 선언하면 Kordex 가

- 명령어와 alias 를 서버에 **자동 등록**하고,
- 트리에서 사용된 **Permission 을 자동 발견·등록**하며,
- 보내는 사람마다 **자동완성(Tab Completion)을 권한/조건에 맞게 필터링**합니다.

`plugin.yml` 의 `commands:` / `permissions:` 는 필요 없습니다 (Fabric/Forge 에는 애초에 그런 개념이 없습니다).

```kotlin
class ExamplePlugin : JavaPlugin() {
    override fun onEnable() {
        kordex {
            command("admin") {
                permission("example.admin") { default = PermissionDefault.OP }

                then("kick") {
                    player("target") {
                        optionalString("reason") {
                            executes {
                                val target = player("target")
                                target.kick(stringOrNull("reason") ?: "Kicked by administrator")
                            }
                        }
                    }
                }
                then("reload") { executes { reply("Reloaded") } }
            }
        }
    }
}
```

```yaml
# plugin.yml — 이것이 전부입니다
name: ExamplePlugin
version: 1.0.0
main: com.example.ExamplePlugin
api-version: '1.21.11'
```

DSL 안에서 쓰는 모든 것(`then`, 인자 빌더, `permission`, `requires`, `executes` …)은 멤버 함수라서 **별도 import 가
필요 없습니다.** import 는 진입점 `kordex` 와 `PermissionDefault` 정도입니다.

## 지원 버전과 산출물

Minecraft 1.21.11 ~ 26.2 에는 5개 릴리스가 있습니다: **1.21.11, 26.1, 26.1.1, 26.1.2, 26.2**.

| 플랫폼 | 산출물 (`com.choverse:…`) | 지원 Minecraft | 비고 |
|---|---|---|---|
| Bukkit/Spigot | `kordex-bukkit` | 1.21.11 ~ 26.2 (jar 1개) | Java 21 바이트코드 |
| Paper | `kordex-paper` | 1.21.11 ~ 26.2 (jar 1개) | Java 21 바이트코드 |
| Fabric | `kordex-fabric-1.21.11` | 1.21.11 | Java 21, 난독화 시대(Loom remap) |
| Fabric | `kordex-fabric` | 26.1 ~ 26.2 | Java 25, 비난독화 |
| Forge | `kordex-forge-1.21.11` | 1.21.11 | Java 21 |
| Forge | `kordex-forge` | 26.1 ~ 26.2 | Java 25 |
| — | `kordex-core` | (Minecraft 무관) | Java 21 |

**Fabric/Forge 는 계열별로 jar 가 둘입니다.** Minecraft 1.21.11 은 난독화된 채 배포되고 26.1 부터는 난독화되지 않은 채
배포되어, 실행 시점의 클래스 이름이 서로 다릅니다 (Fabric 은 Loom 플러그인 자체도 `fabric-loom-remap` 과 `fabric-loom` 으로
다릅니다). 그래서 jar 하나로 두 계열을 모두 지원하는 것은 불가능합니다. 두 빌드는 **같은 어댑터 소스**를 공유합니다
(`shared/fabric`, `shared/forge`). Bukkit/Paper 는 API 가 바이너리 호환이라 jar 하나로 전 범위를 지원합니다.

`plugin.yml` 의 `api-version` 은 **가장 낮은 지원 버전(`'1.21.11'`)** 으로 두세요. 서버보다 높은 값이면 로드를 거부합니다.

| 플랫폼 | 진입점 |
|---|---|
| Paper | `import com.choverse.kordex.paper.kordex` — `kordex(plugin) { }` 또는 `JavaPlugin` 안에서 `kordex { }` |
| Bukkit/Spigot | `import com.choverse.kordex.bukkit.kordex` — 동일 |
| Fabric | `import com.choverse.kordex.fabric.kordex` — `ModInitializer.onInitialize()` 안에서 `kordex { }` |
| Forge | `import com.choverse.kordex.forge.kordex` — 모드 생성자 안에서 `kordex { }` |

Fabric/Forge 의 `kordex(namespace = "modid") { }` 는 충돌 시 `modid:command` 로 등록하기 위한 접두사입니다 (아래 "충돌 처리").

## DSL 요약

| 기능 | DSL |
|---|---|
| 명령어 / 메타데이터 | `command("name") { description(".."); usage(".."); aliases("a", "b") }` |
| 서브 명령 | `then("literal") { }` (중첩·다중 가능), `then(playerArgument("t")) { }` |
| 인자 | `string`, `greedyString`, `integer(min, max)`, `long`, `float`, `double`, `boolean`, `player`, `enum<E>`, 커스텀 `argument("w", MyArgument)` |
| 선택 인자 | `optionalString`, `optionalInteger`, `optionalLong`, `optionalFloat`, `optionalDouble`, `optionalBoolean`, `optionalPlayer` |
| 실행 | `executes { reply(".."); player("t"); intOrNull("n"); fail("..") }` — 반환 타입 지정 접근자 `argument<T>()` / `argumentOrNull<T>()` |
| 권한 | `permission("x")`, `permission("x") { description = ..; default = PermissionDefault.OP; children { permission("x.y", true) } }` |
| 조건 / 가시성 | `requires { }`, `visibleIf { }`, `hidden()`, `audience { permission(..); withoutPermission(..); predicate { } }`, `adminOnly(permission = ..)`, `userOnly(adminPermission = ..)` |
| 자동완성 | `suggests { listOf(..) }` 또는 `suggests { suggestion("x") { requires { .. } } }` |
| 정책 | `kordex { visibility { hideEmptyParents = true } }` (기본 `true`) |
| 런타임 | `Kordex.register(command)`, `Kordex.unregister(name)`, `Kordex.shutdown()` |

핵심 규칙: **`permission`/`requires` 는 실행 제한 + 자동완성 제한을 동시에 합니다.** 자동완성에서 숨기는 것은 UX 이고,
실행 시점에 Permission/Requirement 는 (자동완성을 거치지 않은 직접 입력에 대해서도) 항상 다시 검사됩니다.
`visibleIf` 로 표시 조건만 따로 지정해도 실행 조건(`requires`)은 별도로 강제됩니다.

## 모듈 구성

| 모듈 | 역할 |
|---|---|
| `kordex-core` | 명령 트리, DSL 빌더, 인자/권한/요구조건 시스템, 플랫폼 무관 `CommandDispatcher`. **Minecraft/Bukkit/Fabric/Forge/Brigadier import 없음.** 서버 없이 테스트 가능. |
| `kordex-bukkit` | `CommandMap` 에 런타임 등록. Bukkit 에는 Brigadier 가 없으므로 core 의 `CommandDispatcher` 로 실행·자동완성. |
| `kordex-paper` | 명령 트리를 Brigadier 로 변환해 `LifecycleEvents.COMMANDS` 로 등록 (Paper 공식 권장 방식). |
| `kordex-fabric`, `kordex-fabric-1.21.11` | 동일한 Brigadier 변환, Fabric API `CommandRegistrationCallback` 로 등록. |
| `kordex-forge`, `kordex-forge-1.21.11` | 동일한 Brigadier 변환, Forge `RegisterCommandsEvent` 로 등록. |
| `examples/*` | 플랫폼·계열별 실제 플러그인/모드 예제 (`plugin.yml`/`fabric.mod.json`/`mods.toml` 에 명령·권한 선언 없음). |
| `shared/brigadier`, `shared/fabric`, `shared/forge` | 여러 모듈이 소스셋으로 공유하는 소스 (Gradle 모듈 아님). |

Brigadier 변환 로직(`shared/brigadier/com/choverse/kordex/brigadier/BrigadierTreeBuilder.kt`)은 세 플랫폼·다섯 빌드에서 동일해야 하므로 복사하지 않고
한 파일로 두었고, 각 모듈이 자기 소스셋에 포함해 컴파일합니다. core 가 Brigadier 를 import 하지 않는다는 규칙을 지키면서도,
Paper 에서 테스트한 코드가 Fabric/Forge 가 실행하는 코드와 정확히 같습니다.

## 빌드

```bash
./gradlew clean build       # Linux/macOS
gradlew.bat clean build     # Windows
```

- 첫 빌드는 오래 걸립니다: Fabric/Forge 는 계열마다 Minecraft 아티팩트를 준비하고(수 분), JDK 도 내려받습니다. 이후는 캐시됩니다.
- 가벼운 모듈만 빠르게 확인하려면 `./gradlew :kordex-core:test` (`org.gradle.configureondemand` 덕분에 Fabric/Forge 툴체인은 설정되지 않습니다).

### 버전

`version.properties` 가 현재 버전(`MAJOR.MINOR.PATCH`)이고, **`build` 또는 `centralBundle` 을 실행할 때마다 PATCH 가 1 올라갑니다.**
올리는 시점이 프로젝트를 설정하기 전이라서, 한 번의 실행에 들어간 모든 모듈·예제·Central 번들이 같은 새 버전을 씁니다.

- IDE 동기화, `help`, `test`, `--dry-run` 처럼 빌드가 아닌 실행은 버전을 바꾸지 않습니다.
- 빌드가 실패해도 이미 올라간 번호는 되돌리지 않습니다. 번호가 비는 것은 문제되지 않습니다.
- MAJOR/MINOR 를 올리려면 `version.properties` 를 직접 고치세요. 다음 빌드가 그 값에서 PATCH 를 올립니다.
- 같은 버전으로 다시 빌드하려면 `-Pkordex.bump=false`, 특정 버전을 그대로 쓰려면 `-Pversion=1.2.3` 을 붙입니다 (둘 다 파일을 쓰지 않습니다).
- 첫 빌드는 `1.0.1` 이 됩니다. 첫 공개 릴리스를 정확히 `1.0.0` 으로 내고 싶다면 `-Pkordex.bump=false` 로 빌드하세요.
- 테스트와 Central 번들을 같은 버전으로 묶으려면 한 번에 실행합니다: `./gradlew clean build centralBundle` (버전은 한 번만 올라갑니다).
- 파일은 빌드가 직접 고칩니다. 여러 컴퓨터/CI 에서 빌드한다면 번호가 겹치지 않도록 이 파일을 커밋해서 공유하세요.

### JDK

Kordex 코드는 **Java 21** 바이트코드를 기본으로 합니다 (`kordex-core`, `kordex-bukkit`, `kordex-paper`, 그리고 1.21.11
계열 Fabric/Forge). Java 21 바이트코드는 1.21.11 서버(Java 21)와 26.x 서버(Java 25) 모두에서 실행됩니다. Minecraft 26.x
전용 모듈(`kordex-fabric`, `kordex-forge` 와 그 예제)만 **Java 25 툴체인**으로 컴파일합니다.

Fabric Loom 은 *Gradle 데몬 자체*가 Java 25 로 실행 중이어야 합니다. 이를 위해 프로젝트가
`gradle/gradle-daemon-jvm.properties` (`toolchainVersion=25`) 를 포함하며, 기본 `JAVA_HOME` 이 JDK 21 인 환경에서도
Gradle 이 JDK 25 를 자동으로 내려받아 데몬을 띄웁니다. 별도 설정은 필요 없습니다.

### Minecraft 버전별 관련 사항

- 26.1 부터 Minecraft 는 **난독화되지 않은 상태로 배포**되어 매핑(mappings)이 필요 없습니다. Fabric Loom 은 `mappings()` 의존성
  없이 `net.fabricmc.fabric-loom` 플러그인으로, ForgeGradle 7 은 `minecraft.mavenizer` + `minecraft.dependency(...)` 로
  설정합니다 (공식 MDK 와 동일한 방식). 1.21.11 은 각각 `net.fabricmc.fabric-loom-remap` + `officialMojangMappings()`,
  `minecraft { mappings("official", "1.21.11") }` 가 필요합니다.
- 두 계열 모두 Mojang 공식 이름으로 작성하면 어댑터 소스가 동일합니다 (1.21.11 부터 새 권한 API `permissions()` 가 이미 존재).
- ForgeGradle 7 의 Mavenizer 는 접근 변경 단계에서 JDK 8 을 자동으로 내려받습니다.
- Windows 에서 IDE(예: VS Code Kotlin 언어 서버)가 `build/libs/*.jar` 를 열어두고 있으면 `clean` 이 "파일이 사용 중" 으로
  실패할 수 있습니다. 이때는 IDE 의 언어 서버를 재시작하거나 IDE 를 닫고 다시 실행하세요.

### 다른 Minecraft 버전에 대해 다시 검증하기

`kordex-fabric`/`kordex-forge` 는 계열 중 가장 오래된 버전(26.1)에 대해 컴파일됩니다. 계열 안의 다른 버전에 대해 어댑터가
그대로 컴파일되는지는 다음으로 확인합니다:

```bash
./gradlew :kordex-fabric:compileKotlin -Pkordex.minecraft=26.2     # 26.1 | 26.1.1 | 26.1.2 | 26.2
./gradlew :kordex-forge:compileKotlin  -Pkordex.minecraft=26.1.2
```

전체 버전을 한 번에 돌리는 스크립트도 있습니다 (버전마다 Minecraft 를 처음 받을 때는 수 분씩 걸립니다):

```bash
bash scripts/verify-versions.sh                         # Linux/macOS
powershell -File scripts\verify-versions.ps1            # Windows
```

`kordex-bukkit`/`kordex-paper` 는 `./gradlew check` 가 각 API 버전에 대해 같은 테스트를 다시 실행합니다.

## Maven Central 배포

`com.choverse:kordex-*` 를 Maven Central 에 올리기 위한 설정이 들어 있습니다. **이 저장소의 빌드는 어디에도 업로드하지 않습니다.**
Central 에 올린 릴리스는 삭제도 덮어쓰기도 불가능하므로, 업로드는 아래 절차의 마지막 단계에서 사람이 직접 합니다.

발행 대상은 `kordex-core`, `kordex-bukkit`, `kordex-paper`, `kordex-fabric`, `kordex-fabric-1.21.11`, `kordex-forge`,
`kordex-forge-1.21.11` 7개이고 `examples/*` 는 발행하지 않습니다. artifact 마다 `.jar`, `-sources.jar`, `-javadoc.jar`, `.pom`,
Gradle 모듈 메타데이터(`.module`)가 올라갑니다.

- POM 의 의존성은 `kordex-core` 와 Kotlin stdlib 뿐입니다. Paper/Spigot/Fabric/Forge/Minecraft 는 `compileOnly` 라서 사용하는
  쪽으로 전이되지 않습니다.
- `-javadoc.jar` 에는 안내 파일만 들어 있습니다. Kordex 는 Kotlin 라이브러리라 Javadoc 이 만들어지지 않고, Central 은 이런
  자리표시 jar 를 허용합니다. KDoc 은 `-sources.jar` 에 그대로 있어서 IDE 에서 보입니다.
- Bukkit/Paper 는 한 artifact 가 1.21.11 ~ 26.2 전체를 지원하고, Fabric/Forge 는 계열별 artifact 가 따로 있습니다 (위 표).

### 1. 처음 한 번만 하는 준비

1. <https://central.sonatype.com> 계정을 만들고 **네임스페이스 `com.choverse` 를 인증**합니다. Portal 에서 네임스페이스를 추가하면
   검증 키를 알려 주는데, 이를 소유하신 `choverse.com` 도메인의 DNS TXT 레코드로 등록한 뒤 Portal 에서 인증을 요청합니다
   (DNS 전파에 시간이 걸릴 수 있습니다).
2. GPG 키를 만들고 **공개키를 키 서버에 올립니다** (`keyserver.ubuntu.com` 또는 `keys.openpgp.org`). Central 이 서명을 검증할 때 씁니다.
3. Portal 의 *Account → Generate User Token* 으로 토큰(사용자명/비밀번호 쌍)을 만듭니다.
4. `gradle.properties` 맨 아래 안내대로 `kordex.pom.*` (라이선스, 프로젝트 URL, 개발자 이름·이메일, SCM URL)를 채웁니다.
   개발자 이메일은 모든 POM 에 그대로 공개됩니다.

서명 키와 토큰은 저장소 파일에 넣지 마세요. 환경 변수나 `~/.gradle/gradle.properties` 로만 전달합니다.

### 2. 업로드용 번들 만들기

```bash
# 로컬 GPG 키링을 쓰는 경우 (gpg 가 PATH 에 있어야 합니다. 키가 여러 개면 -Psigning.gnupg.keyName=<키 ID> 를 더합니다)
./gradlew clean build centralBundle -Pkordex.signing.gpg=true

# 개인키를 환경 변수로 주는 경우 (CI 등) - 아스키 아머 개인키 전체
ORG_GRADLE_PROJECT_signingInMemoryKey="$(cat private-key.asc)" \
ORG_GRADLE_PROJECT_signingInMemoryKeyPassword="..." \
./gradlew clean build centralBundle
```

```powershell
# Windows PowerShell
$env:ORG_GRADLE_PROJECT_signingInMemoryKey = Get-Content private-key.asc -Raw
$env:ORG_GRADLE_PROJECT_signingInMemoryKeyPassword = "..."
.\gradlew.bat clean build centralBundle
```

`build` 를 함께 실행하면 테스트를 통과한 코드만 번들에 들어가고, 버전은 한 번만 올라갑니다 (위 "버전").
결과는 `build/central/kordex-<버전>-central-bundle.zip` 입니다. 7개 모듈을 모두 설정하므로 Fabric/Forge 툴체인이 필요하고,
처음에는 수 분 걸립니다.

`centralBundle` 은 zip 을 만들기 전에 Central 의 요구사항을 직접 검사합니다: SNAPSHOT 버전 여부, 모든 파일의 `.asc` 서명과
md5/sha1 체크섬, sources/javadoc jar, POM 필수 항목(name, description, url, license, developer 이름·이메일, scm). 빠진 것이
있으면 무엇이 빠졌는지 나열하고 멈춥니다. 서명 없이 `./gradlew build` 나 `publishAllPublicationsToCentralBundleRepository` 를
돌리는 것은 자유롭지만, 그렇게 만든 결과물은 Central 이 거절합니다.

### 3. 업로드 (되돌릴 수 없는 단계)

- **웹 (처음이라면 권장)**: <https://central.sonatype.com/publishing/deployments> 에서 *Publish Component* 로 zip 을 올립니다.
  검증이 끝나면 내용을 확인하고 **Publish** 를 누릅니다.
- **API**:

```bash
curl --request POST \
  --header "Authorization: Bearer $(printf '<토큰 사용자명>:<토큰 비밀번호>' | base64)" \
  --form bundle=@build/central/kordex-<버전>-central-bundle.zip \
  "https://central.sonatype.com/api/v1/publisher/upload?publishingType=USER_MANAGED"
```

`USER_MANAGED` 는 검증 후 Portal 에서 직접 Publish 를 눌러야 공개됩니다. `AUTOMATIC` 은 검증을 통과하면 바로 공개하므로 첫
릴리스에는 권장하지 않습니다. 공개한 버전은 수정할 수 없으니 고칠 게 있으면 다시 빌드해서 발행하세요 (빌드마다 버전이 자동으로
올라갑니다. `-SNAPSHOT` 은 Central 이 받지 않습니다).

### 사용하는 쪽

```kotlin
repositories { mavenCentral() }

dependencies {
    implementation("com.choverse:kordex-paper:<버전>")   // kordex-bukkit / kordex-fabric / kordex-forge ...
}
```

Fabric/Forge 는 Minecraft 계열에 맞는 artifact 를 고릅니다 (`kordex-fabric` 은 26.1 ~ 26.2, `kordex-fabric-1.21.11` 은 1.21.11).
`examples/*/build.gradle.kts` 가 각 플랫폼에서 Kordex 를 모드/플러그인 jar 에 포함하는 방법을 보여 줍니다 (프로젝트 의존성
대신 위 좌표를 쓰면 됩니다).

## 충돌 처리

Kordex 는 다른 플러그인/모드의 명령어를 제거하거나 덮어쓰지 않습니다.

| 플랫폼 | 동작 |
|---|---|
| Bukkit | `CommandMap.register(prefix, command)` 로 `prefix:name` 도 함께 등록. 이미 점유된 이름/alias 는 건드리지 않고 로그로 알립니다. `unregister` 는 자기 자신의 항목만 `knownCommands` 에서 제거합니다. |
| Paper | Paper 의 registrar 가 `pluginname:command` fallback 을 자체 처리합니다. |
| Fabric / Forge | Brigadier 는 같은 이름의 루트를 **병합(실행 로직 덮어쓰기)** 하므로, 이미 존재하는 이름은 건드리지 않고 `namespace:name` 으로만 등록합니다. 모든 명령은 `namespace:name` 으로도 항상 호출 가능합니다. |

## 검증 현황

| 대상 | 방법 | 상태 |
|---|---|---|
| `kordex-core` | `MockCommandPlatform` 등 (서버 불필요) — 40개 테스트: 트리, 인자, 옵션 인자, 권한(메타데이터/children/수집/병합), 자동 등록, alias, 충돌, unregister, 자동완성, 커스텀 인자/DSL, 가시성 전체, `audience { }` | 통과 |
| `kordex-bukkit` | 1.21.11 API 로 컴파일한 **같은 바이트코드**를, 실제 Spigot `SimpleCommandMap` + JDK 프록시 `CommandSender` 로, **Spigot API 6개 버전**(1.21.11-R0.1/R0.2, 26.1, 26.1.1, 26.1.2, 26.2)에 대해 각각 14개 테스트 | 통과 |
| `kordex-paper` (= 공유 Brigadier 변환기) | 1.21.11 API 로 컴파일한 같은 바이트코드를, **실제 Brigadier `CommandDispatcher`** + Paper 인터페이스 프록시로, **Paper API 1.21.11 / 26.1.2 / 26.2** 에 대해 각각 14개 테스트 | 통과 |
| `kordex-fabric*`, `kordex-forge*` | 실제 Minecraft / Fabric API / Forge 대상 **컴파일** — 1.21.11 (전용 모듈)과 26.1, 26.1.1, 26.1.2, 26.2 (같은 모듈을 `-Pkordex.minecraft` 로 바꿔가며) 각각 | 컴파일 통과, **런타임 테스트 없음** |
| 예제 jar 10개 | 산출물 내부 검사: `plugin.yml`/`fabric.mod.json`/`mods.toml` 에 `commands:`/`permissions:` 없음 | 확인 |
| Fabric 1.21.11 리맵 | `kordex-fabric-1.21.11` jar 는 난독화 이름(`net/minecraft/class_N`)으로 리맵되고, 26.x 용 jar 는 Mojang 이름 그대로 | 확인 |
| Maven Central 번들 | `centralBundle`: 7개 모듈의 서명(임시 테스트 키로 `gpg --verify`)·체크섬·POM 검증, 발행물만으로 Paper/Bukkit 소비자 프로젝트 컴파일, POM 의존성에 Minecraft/서버 API 없음 | 확인 (**실제 Central 업로드는 하지 않음**) |
| 자동 버전업 | `clean build centralBundle` 한 번에 버전이 1회만 상승하고 7개 모듈·예제 6개·번들이 같은 버전을 씀. `help`/`test`/`--dry-run`/`-Pkordex.bump=false`/`-Pversion` 은 파일을 바꾸지 않음 | 확인 |
| **실서버/실클라이언트 실행** | — | **수행하지 못함** (아래 참고) |

`./gradlew build` 는 각 계열의 가장 오래된 버전(26.1)만 컴파일하므로, 계열 안의 나머지 버전 검증은 위 "다른 Minecraft 버전에 대해
다시 검증하기" 의 명령이나 `scripts/verify-versions.*` 로 다시 할 수 있습니다.

Paper 는 26.1 에 대한 API 를 배포하지 않았고 26.1.1 은 alpha 빌드만 있어서, 26.1.x 계열은 최종 릴리스인 26.1.2 로 대표해 검증합니다.

Paper/Bukkit/Fabric/Forge **서버를 실제로 띄워** `plugin.yml` 없이 명령이 등록·동작하는지 확인하는 통합 테스트는 이 저장소에
포함되어 있지 않습니다. Fabric/Forge 는 `CommandSourceStack` 이 구체 클래스라 서버 없이 단위 테스트할 수 없어, Paper 에서 검증한
동일 소스(`shared/brigadier`)를 쓰는 것으로 갈음했습니다. 범위 밖인 26.3 은 검증하지 않았습니다.

## 알려진 제한

- **`hidden()` (Brigadier 플랫폼)**: Brigadier 의 `requires` 는 파싱과 자동완성을 동시에 막으므로 "직접 입력하면 실행되지만
  자동완성에서는 숨김" 을 표현할 수 없습니다. Paper/Fabric/Forge 에서는 `hidden()` 을 무시하고 일반 노드로 취급합니다
  (실행 가능하며 자동완성에 노출될 수 있음). `kordex-bukkit` 의 서브 명령 자동완성은 `hidden()` 을 그대로 지킵니다.
- **Fabric/Forge 권한**: 바닐라에는 문자열 권한 노드가 없고 고정된 명령 권한 등급(moderator/gamemaster/admin/owner)만
  있습니다. `hasPermission(..)` 은 권한 문자열과 무관하게 `gamemaster` 등급(과거 OP 레벨 2)으로 매핑되고,
  `FabricPermissionPlatform`/`ForgePermissionPlatform` 은 의도적으로 no-op 입니다. 세분화가 필요하면 실제 권한 모드를
  감싼 `PermissionPlatform` 구현을 연결하세요 (SPI 가 그 용도입니다).
- **런타임 동적 등록**: Bukkit 은 `CommandMap` 에 즉시 반영됩니다. Paper/Fabric/Forge 는 서버가 명령 트리를 다시 만들 때
  (예: `/reload`) 반영됩니다.
