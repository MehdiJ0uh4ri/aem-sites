# AEM SDK + Maven Project — Setup From Scratch

Reproduces this working environment starting from a pulled AEM SDK Docker image,
ending with a live edit loop where saving an HTL file updates the running instance
in about a second.

Every command below was run and verified on Linux (Ubuntu 24.04) on 2026-09-09.
Windows equivalents are given inline **only where they differ**; unmarked commands
are identical on both.

Verified end state:

| Component | Version |
|---|---|
| AEM SDK image | `aemdesign/aem:sdk-2026.2.24464` |
| JDK | OpenJDK 21.0.12 (**JDK, not JRE**) |
| Maven | 3.8.7 |
| maven-archetype-plugin | **3.4.1** (3.2.1 fails — see Pitfall 2) |
| aem-project-archetype | 54 |
| aem-sdk-api | `2026.2.24464.20260214T050318Z-260200` (pinned) |
| aemsync | 5.2.0 |

---

## 0. Prerequisites

You need Docker, a **JDK**, Maven, and Node.js.

### Linux

```bash
sudo apt update
sudo apt install -y openjdk-21-jdk-headless maven
```

> **Do not install `openjdk-21-jre-headless`.** The JRE has no `javac` and the
> build fails with a misleading `error: release version 11 not supported`.
> See Pitfall 3.

Node.js via [nvm](https://github.com/nvm-sh/nvm) (v20 LTS is known good):

```bash
nvm install 20 && nvm use 20
```

### Windows

Use winget from an **Administrator** PowerShell:

```powershell
winget install Microsoft.OpenJDK.21
winget install Apache.Maven
winget install OpenJS.NodeJS.LTS
```

Docker Desktop with the WSL 2 backend is required. Close and reopen the terminal
afterwards so `PATH` updates.

### Verify (both OSes)

```bash
javac -version    # must print a version, NOT "command not found"
mvn -v
node -v
docker --version
```

`javac -version` is the important one. If it fails, stop and fix it now — the
Maven build will fail later with a confusing message.

---

## 1. Start the AEM SDK container

```bash
docker pull aemdesign/aem:sdk-2026.2.24464

docker run -d \
  --name aem-author \
  -p 4502:8080 \
  -p 30303:58242 \
  -v aem-repository:/aem/crx-quickstart/repository \
  -v aem-logs:/aem/crx-quickstart/logs \
  -v aem-backup:/aem/backup \
  aemdesign/aem:sdk-2026.2.24464
```

**Windows (PowerShell)** — replace the trailing `\` line continuations with a
backtick `` ` ``, or put the whole command on one line.

The container listens on **8080 internally**, published to **4502** on the host.
Port 30303 is the JVM debug port (`-agentlib:jdwp`), already enabled in the image.

### Why the named volumes matter

The image declares three `VOLUME`s (`/aem/backup`, `/aem/crx-quickstart/logs`,
`/aem/crx-quickstart/repository`). If you omit `-v`, Docker creates **anonymous**
volumes — and `docker compose down -v` or `docker volume prune` then silently
destroys your entire JCR: every page, asset and deployed package. Naming them
makes the data survive container recreation and makes the intent visible in
`docker volume ls`.

### Wait for startup

First boot takes several minutes. Watch for readiness:

```bash
docker logs -f aem-author
```

Then confirm:

```bash
curl -s -u admin:admin -o /dev/null -w '%{http_code}\n' \
  http://localhost:4502/system/console/bundles.json
```

You want **200**.

> ### Pitfall 1 — `401 Unauthorized` is not an error
>
> A bare `curl http://localhost:4502` returns:
> ```
> HTTP/1.1 401 Unauthorized
> WWW-Authenticate: Basic realm="Sling (Development)"
> ```
> This is correct behaviour. AEM requires basic auth on every request. Always
> pass `-u admin:admin`, or log in through the browser at http://localhost:4502/.

---

## 2. Generate the Maven project

```bash
cd ~
```

**Windows:** `cd $env:USERPROFILE`

```bash
mvn -B org.apache.maven.plugins:maven-archetype-plugin:3.4.1:generate \
  -DarchetypeGroupId=com.adobe.aem \
  -DarchetypeArtifactId=aem-project-archetype \
  -DarchetypeVersion=54 \
  -DappTitle="My Site" -DappId=mysite \
  -DgroupId=com.mysite -DartifactId=mysite \
  -Dversion=1.0.0-SNAPSHOT -Dpackage=com.mysite \
  -DaemVersion=cloud -DsdkVersion=latest -DlanguageCountry=en_us \
  -DfrontendModule=general -DsingleCountry=y -DincludeDispatcherConfig=y \
  -DincludeExamples=n -DincludeErrorHandler=n
```

**Windows (PowerShell)** — use a backtick `` ` `` for line continuation, and wrap
each `-D` argument containing a space or `=` in quotes, because PowerShell
otherwise mangles them:

```powershell
mvn -B org.apache.maven.plugins:maven-archetype-plugin:3.4.1:generate `
  "-DarchetypeGroupId=com.adobe.aem" `
  "-DarchetypeArtifactId=aem-project-archetype" `
  "-DarchetypeVersion=54" `
  "-DappTitle=My Site" "-DappId=mysite" `
  "-DgroupId=com.mysite" "-DartifactId=mysite" `
  "-Dversion=1.0.0-SNAPSHOT" "-Dpackage=com.mysite" `
  "-DaemVersion=cloud" "-DsdkVersion=latest" "-DlanguageCountry=en_us" `
  "-DfrontendModule=general" "-DsingleCountry=y" "-DincludeDispatcherConfig=y" `
  "-DincludeExamples=n" "-DincludeErrorHandler=n"
```

**Windows (cmd.exe)** — line continuation is `^` instead.

### Verify the generation actually completed

```bash
ls ~/mysite
```

You must see exactly `ui.frontend` and `dispatcher`. If you instead see
`ui.frontend.react`, `ui.frontend.angular`, `ui.frontend.general`,
`dispatcher.ams` and `dispatcher.cloud`, the post-generate script did not run —
see Pitfall 2.

> ### Pitfall 2 — `maven-archetype-plugin:3.2.1` cannot run archetype 54
>
> Symptom:
> ```
> Script1.groovy: 2: unable to resolve class groovy.xml.XmlSlurper
> ```
> followed, on the next build attempt, by:
> ```
> Child module .../ui.frontend of .../pom.xml does not exist
> Child module .../dispatcher of .../pom.xml does not exist
> ```
>
> Cause: plugin 3.2.1 embeds Groovy 2.x, where `XmlSlurper` lives in
> `groovy.util`. Archetype 54's `archetype-post-generate.groovy` imports
> `groovy.xml.XmlSlurper` (Groovy 3+), so the script dies on its import line.
>
> That script is what **prunes the variant modules** down to the ones you chose.
> When it dies, the generator leaves every variant on disk and a `pom.xml` that
> references modules which do not exist — hence the second error. The two errors
> are one bug, not two.
>
> **Fix:** use plugin **3.4.1**, as above. Same archetype version, no other change.
>
> If you already have a broken tree, delete it before retrying:
> ```bash
> rm -rf ~/mysite
> ```
> **Windows:** `Remove-Item -Recurse -Force $env:USERPROFILE\mysite`
>
> **Do this from a different directory.** See Pitfall 7.

---

## 3. Pin the SDK API to your container

The archetype resolves `-DsdkVersion=latest` against Adobe's live release feed,
so it will pick whatever is newest — which is almost certainly **not** the
version your container runs. Building against an SDK newer than the instance can
produce a bundle importing OSGi packages the running AEM does not export, which
fails at deploy time or, worse, at first request.

Check what the archetype chose:

```bash
grep -n 'aem.sdk.api' ~/mysite/pom.xml
```

Your container's version is in its image tag — `sdk-2026.2.24464` here. Find the
matching artifact:

```bash
curl -s https://repo1.maven.org/maven2/com/adobe/aem/aem-sdk-api/maven-metadata.xml \
  | grep -oP '(?<=<version>)2026\.2[^<]*'
```

Then set `<aem.sdk.api>` in `~/mysite/pom.xml` (around line 49) to the exact match:

```xml
<aem.sdk.api>2026.2.24464.20260214T050318Z-260200</aem.sdk.api>
```

Edit it in your IDE, or on Linux:

```bash
sed -i 's|<aem.sdk.api>.*</aem.sdk.api>|<aem.sdk.api>2026.2.24464.20260214T050318Z-260200</aem.sdk.api>|' ~/mysite/pom.xml
```

Re-pin this whenever you upgrade the container image.

---

## 4. Build and deploy

```bash
cd ~/mysite
mvn clean install -PautoInstallSinglePackage \
  -Daem.host=localhost -Daem.port=4502 \
  -Dsling.user=admin -Dsling.password=admin
```

**Windows (PowerShell):** backtick for continuation, or one line.

`-Daem.port=4502` is correct even though AEM listens on 8080 inside the
container — Maven talks to the host-published port.

First run takes roughly 3–5 minutes; `ui.frontend` downloads an npm tree.
Expect `BUILD SUCCESS` across all 11 modules.

> ### Pitfall 3 — `error: release version 11 not supported`
>
> This does **not** mean your JDK is too new. JDK 21 supports `--release 11`
> perfectly well. It means there is **no JDK at all** — only a JRE, so Maven has
> no compiler to hand the sources to.
>
> Diagnose:
> ```bash
> javac -version                      # "command not found" → this is your problem
> dpkg -l | grep -E 'jdk|jre'         # Linux: shows only *-jre-headless
> ```
> **Fix:** install the JDK (Step 0). No `pom.xml` change is needed.

### Verify the deploy landed

```bash
# Components installed
curl -s -u admin:admin http://localhost:4502/apps/mysite/components.1.json \
  | python3 -c "import json,sys;d=json.load(sys.stdin);print(' '.join(k for k,v in d.items() if isinstance(v,dict)))"

# OSGi bundle must be Active
curl -s -u admin:admin http://localhost:4502/system/console/bundles.json \
  | python3 -c "import json,sys
d=json.load(sys.stdin)
for b in d['data']:
    if 'mysite' in b['symbolicName']: print(b['symbolicName'], b['version'], '->', b['state'])"

# Site renders
curl -s -u admin:admin -o /dev/null -w '%{http_code}\n' \
  http://localhost:4502/content/mysite/us/en.html
```

Expect ~30 components, `mysite.core 1.0.0.SNAPSHOT -> Active`, and `200`.

**Windows:** these use `python3`; if you have no Python, just open the URLs in a
browser instead — the JSON is readable as-is.

Author the site at:

```
http://localhost:4502/editor.html/content/mysite/us/en.html
```

> ### Pitfall 4 — the home page is `us/en.html`, not `us/en/home.html`
>
> With `-DsingleCountry=y`, `en` **is** the home page and has no child pages.
> `/content/mysite/us/en/home.html` returns 404.

---

## 5. The edit loop

### HTL, dialogs, client-side JS/CSS — instant, no rebuild

```bash
npm i -g aemsync

cd ~/mysite
aemsync -t http://admin:admin@localhost:4502 -w ui.apps/src/main/content/jcr_root
```

**Windows:** the watch path uses backslashes —
`-w ui.apps\src\main\content\jcr_root`

Leave that running in its own terminal and just save files. Each save pushes the
single changed file to the JCR in about a second; reload the page to see it.
`aemsync` has no `--help` — it ignores unknown flags and starts watching anyway.

> ### Pitfall 5 — do not try to push HTL with plain `curl`
>
> Both of these return `409 repository state conflicting with request`:
> ```bash
> curl -u admin:admin -F "helloworld.html=@helloworld.html" \
>   http://localhost:4502/apps/mysite/components/helloworld/
> curl -u admin:admin -F "jcr:data=@helloworld.html" \
>   http://localhost:4502/apps/mysite/components/helloworld/helloworld.html/jcr:content
> ```
> Sling's POST servlet treats the trailing `.html` as a **rendering extension**
> rather than part of the node name. Use `aemsync`, the `repo` tool, or the
> VS Code *AEM Sync* extension — they wrap the file as a FileVault package and
> use the package-manager endpoint instead.

> ### Note — `sed -i` produces spurious delete lines
>
> `sed -i` writes a temp file then renames, so the watcher logs a phantom
> deletion next to the real push:
> ```
> - jcr_root/apps/mysite/components/helloworld/sedNxlqb6
> + jcr_root/apps/mysite/components/helloworld/helloworld.html
> ```
> Harmless — the push still succeeds. Normal editors do not do this.

### Java (OSGi bundle) changes

```bash
cd ~/mysite
mvn clean install -PautoInstallBundle -pl core
```

The bundle refreshes; no AEM restart needed.

### Java debugging

Attach your IDE to `localhost:30303`. The image already runs with
`-agentlib:jdwp=transport=dt_socket,server=y,address=*:58242,suspend=n`.

### Structural changes

New nodes, templates, policies or config need a full package install:

```bash
mvn clean install -PautoInstallSinglePackage \
  -Daem.host=localhost -Daem.port=4502 -Dsling.user=admin -Dsling.password=admin
```

---

## 6. Daily lifecycle — stopping and resuming

Once the container exists you never `docker run` it again. Stop and start the
**same** container:

```bash
# End of a session — give Oak time to close the repository cleanly
docker stop -t 300 aem-author

# Next session
docker start aem-author
```

### Why the long timeout

`docker stop` sends `SIGTERM`, waits, then `SIGKILL`s. The image's entrypoint is
`/bin/tini -- /aem/run-tini.sh`, and tini does forward the signal correctly — but
the image sets no stop timeout of its own:

```bash
docker inspect aem-author --format 'StopTimeout: {{.Config.StopTimeout}}'
# StopTimeout: <nil>
```

so Docker's **10-second default** applies, and Oak needs far longer than that to
close the segment store.

**120 seconds is not enough either.** Measured on this setup, a `docker stop -t 120`
logged the shutdown starting:
```
*INFO * [Apache Sling Terminator] Java VM is shutting down
*INFO * [Apache Sling Terminator] Stopping Apache Sling
```
and was still not done 120 seconds later, so Docker killed it — exit `137`
(`128 + 9`, i.e. `SIGKILL`). A complete shutdown measures about **138 seconds**
here, so use `-t 300`.

### Confirm the stop was clean

The exit code tells you which signal ended the JVM:

```bash
docker inspect aem-author --format 'ExitCode: {{.State.ExitCode}}'
```

| Exit code | Meaning |
| --- | --- |
| `143` | `128 + 15` = `SIGTERM`. **This is the healthy result.** The JVM ran its shutdown hooks and exited on the signal. |
| `137` | `128 + 9` = `SIGKILL`. Docker's timeout expired and it force-killed AEM mid-shutdown. Raise `-t`. |
| `0` | Not what this image produces on `docker stop`. Do not wait for it. |

The exit code is a signal, not proof. The definitive check is the log line that
only a *completed* shutdown writes:

```bash
docker logs aem-author 2>&1 | grep "Apache Sling has been stopped"
```

Measured on this setup, a clean `docker stop -t 300`:

```
12:47:13.719  *INFO * [Apache Sling Terminator] Java VM is shutting down
12:49:30.906  *INFO * [Sling Notifier] Apache Sling has been stopped
```

**About 138 seconds** from `SIGTERM` to a finished shutdown. That is precisely
why `-t 120` is killed at the 120-second mark while `-t 300` completes with room
to spare. In a killed (`137`) run, that final `Apache Sling has been stopped`
line never appears at all — which is the quickest way to tell the two apart.

A `137` is not automatically fatal: after the one above, the instance came back
with all 640 bundles active, `/apps/mysite` and the authored content intact, and
no Oak recovery messages. But it is gambling with the segment store, so do not
make a habit of it.

> **Do not diagnose AEM from responses taken during a stop.** While the shutdown
> is running, the HTTP layer lies: `/crx/packmgr/index.jsp` answered `404` and
> `login.html` never reached `200`, before the port stopped answering at all
> (`curl` writes `000` when the connection is refused). None of that means
> anything is broken. Check `docker inspect --format '{{.State.Running}}'` first.

### Wait for readiness before building

A restart is not instant, and the HTTP status *changes* as it comes up: `404`,
then `503`, then `200`. A single `404` does not mean it is broken.

```bash
until curl -s -o /dev/null -w "%{http_code}" -u admin:admin \
  http://localhost:4502/libs/granite/core/content/login.html | grep -q 200; do sleep 5; done
echo "AEM is up"
```

Deploying before that returns wastes a six-minute build.

### What survives what

| Command | Effect |
| --- | --- |
| `docker stop` / `docker start` | Safe. Bundle, `/apps` and `/content` all intact. |
| `docker rm aem-author` | Container gone. **Named volumes survive** — a `docker run` with the same `-v` flags resumes with content intact. |
| `docker volume rm aem-repository aem-logs aem-backup` | **Destroys the JCR.** |
| `docker volume prune` | **Also destroys it**, once the container is removed and the volumes count as unused. |

### Restoring after a wipe

Source lives in git; the instance does not. A full rebuild and deploy:

```bash
cd ~/mysite
mvn clean install -PautoInstallSinglePackage
```

If `all/target/*.zip` is already current, reinstalling it takes seconds instead
of six minutes:

```bash
curl -u admin:admin -F cmd=upload -F force=true -F install=true \
  -F file=@all/target/mysite.all-1.0.0-SNAPSHOT.zip \
  http://localhost:4502/crx/packmgr/service.jsp
```

The field is **`file`**. Passing `-F package=@...` is not reported as an error —
the endpoint returns its own usage text wrapped in `<status code="200">ok</status>`,
so a check for HTTP 200 reports success while nothing was installed. Grep the
response body for `Package installed` instead.

Content authored only through the UI (or `curl`) lives solely in the JCR and is
**not** in git. If a page must survive a wipe, put it in `ui.content`.

> ### Pitfall 6 — `docker run` a second time can wipe your instance
>
> After a stop, the instinct is to re-run the original `docker run`. It fails:
> ```
> docker: Error response from daemon: Conflict. The container name
> "/aem-author" is already in use
> ```
> The usual reflex — `docker rm aem-author`, then run again — is safe **only if
> the original `docker run` used the named `-v` flags** from section 1. If it did
> not, the repository was sitting in an *anonymous* volume; the replacement
> container gets fresh empty ones and you boot into a vanilla AEM. The symptom is
> unmistakable: `/apps/mysite` and `/content/mysite` both return `404` and the
> bundle count is back to stock.
>
> Check before removing anything:
> ```bash
> docker inspect aem-author --format '{{json .Mounts}}'
> ```
> You want three entries named `aem-repository`, `aem-logs` and `aem-backup`. An
> empty `[]`, or `"Name"` values that are 64-character hex strings, mean anonymous
> volumes — that content will not survive.
>
> The answer is almost always `docker start aem-author`, not `docker run`.

---

## 7. Consoles and where to look

All served by the author container on 4502, all `admin` / `admin`:

| Tool | URL |
| --- | --- |
| CRXDE Lite — browse and edit the JCR | http://localhost:4502/crx/de/index.jsp |
| Package Manager — upload, install, build packages | http://localhost:4502/crx/packmgr/index.jsp |
| OSGi bundles — check your bundle is `Active` | http://localhost:4502/system/console/bundles |
| Sling Models — confirm a model registered | http://localhost:4502/system/console/status-adapters.txt |
| Sites editor | http://localhost:4502/sites.html/content/mysite |

CRXDE Lite is the fastest way to see what actually landed in the repository:
components appear under `/apps/mysite/components`, and authored content under
`/content/mysite/us/en/<page>/jcr:content`. It is a local-SDK convenience and is
disabled on real AEM as a Cloud Service environments, so do not build a workflow
that depends on it.

---

## 8. The dispatcher is not running, and is not part of this setup

The `dispatcher/` module is **configuration only**. `mvn install` packages it as
`dispatcher/target/mysite.dispatcher.cloud-1.0.0-SNAPSHOT.zip`; nothing in the
steps above starts a web server. There is no dispatcher on `localhost` to visit.

Its backend is parameterised rather than hard-coded —
`dispatcher/src/conf.dispatcher.d/renders/default_renders.any`:

```
/0 {
	/hostname "${AEM_HOST}"
	/port "${AEM_PORT}"
	/timeout "10000"
}
```

Those variables are supplied by Adobe's **Dispatcher SDK** (Apache httpd plus the
dispatcher module), a separate download from Adobe Software Distribution that
needs an Adobe ID. It ships `bin/validate.sh` to lint the config and
`bin/docker_run.sh` to run httpd against a backend. `dispatcher/update_sdk.sh`
syncs this repo's config against a downloaded SDK:

```bash
./dispatcher/update_sdk.sh /opt/dispatcher-sdks/dispatcher-sdk-<version> src
```

Two things to expect if you go down that road, neither of which this guide covers:

- The dispatcher is designed to front a **publish** instance. This setup runs an
  author only, so you need a publish container first.
- On Linux, `host.docker.internal` does not resolve by default. Reaching the AEM
  container from the dispatcher container needs
  `--add-host=host.docker.internal:host-gateway`, or both containers on a shared
  Docker network.

---

## Pitfalls that are environmental, not AEM

> ### Pitfall 7 — `getcwd() failed: No such file or directory` (Linux only)
>
> After deleting and regenerating `~/mysite`, an already-open shell that was
> `cd`'d into it still holds the **deleted inode**. Everything then fails,
> including `sudo apt` and Maven:
> ```
> sh: 0: getcwd() failed: No such file or directory
> Error occurred during initialization of VM
> java.lang.Error: Properties init: Could not determine current working directory.
> ```
> Nothing is broken. Re-enter the directory to re-resolve the path:
> ```bash
> cd ~/mysite
> ```
> Avoid it by running `rm -rf ~/mysite` from a *different* directory.
>
> This does not occur on Windows, which refuses to delete a directory that is in
> use by an open shell.

### Windows-specific notes

- **Line endings.** Git may convert LF to CRLF on checkout, which breaks shell
  scripts inside `ui.frontend` and `ui.tests`. The generated `.gitattributes`
  handles this; do not override `core.autocrlf` for this repo.
- **Path length.** The `ui.frontend` npm tree nests deeply and can exceed the
  260-character limit. Keep the project near the drive root (`C:\dev\mysite`
  rather than a deep `Documents` path), or enable long paths:
  ```powershell
  git config --system core.longpaths true
  ```
- **Docker Desktop memory.** The JVM is configured for `-Xmx1524m`. Give Docker
  Desktop at least 4 GB in Settings → Resources, or AEM will thrash.

---

## Known-noisy output you can ignore

- **`19 vulnerabilities (2 critical)`** from the `ui.tests` npm install. That is
  the archetype's stock Cypress tooling. It never ships to AEM and only runs in
  local test containers.
- **`[WARNING] CP Don't override file ...`** during archetype generation. Normal
  for a successful run. Only worry if it is followed by `BUILD FAILURE`.

---

## Teardown

```bash
docker stop -t 300 aem-author && docker rm aem-author
```

Volumes survive this **provided the container was created with the named `-v`
flags** from section 1, so the next `docker run` with those same flags resumes
with your content intact. If it was not, see Pitfall 6 — the content is in an
anonymous volume and will be orphaned. To destroy the repository as well:

```bash
docker volume rm aem-repository aem-logs aem-backup
```
