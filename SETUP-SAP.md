# SAP HANA Express in Docker — Setup From Scratch

Reproduces this working environment starting from a pulled `saplabs/hanaexpress`
image, ending with a running multi-tenant HANA 2.0 instance whose data survives
container recreation.

Every command below was run and verified on Linux (Ubuntu, kernel 7.0.0-31-generic)
on 2026-09-09. Where something was *not* verified on the running instance, it says
so explicitly.

Verified end state:

| Component | Version |
|---|---|
| Image | `saplabs/hanaexpress:latest` (4.49 GB) |
| HANA | `2.00.088.00.1760424921`, branch `fa/hana2sp08` |
| SID / instance | `HXE` / `90` |
| Container user | `hxeadm`, **uid 12000, gid 79 (sapsys)** |
| Host mount | `/data/s4hana` → `/hana/mounts` |
| Container hostname | `hxehost` (**pinned — see Pitfall 5**) |
| System DB | `localhost:39013` |
| Tenant `HXE` | `localhost:39041` |

---

## 0. Read this before you start

**This image is a database server. It is not an application platform.**

There is no Fiori launchpad, no SAP GUI, no Web IDE, no cockpit, and no
"components" to edit. It ships an XS Classic engine that answers HTTP on 8090,
but with **zero applications deployed** — see Section 8, where this is measured
rather than asserted. If you came here expecting a browser-based SAP UI, that
lives in the separate, much larger `saplabs/hanaexpress-xsa` image.

### Resource reality

HANA reserves memory aggressively. Measured on this host during a successful
first start:

```
before:  15Gi total, 7.8Gi used, 337Mi free, 3.5Gi swap used
after:   15Gi total,  10Gi used, 279Mi free, 3.4Gi swap used
```

`hdbnameserver` alone reached ~1.5 GB RSS, and five other processes
(`hdbcompileserver`, `hdbpreprocessor`, `hdbdiserver`, `hdbwebdispatcher`,
`hdbindexserver`) run alongside it. On a 15 GB host, stop everything else first:

```bash
docker stop postgres          # or whatever else is running
docker stop -t 300 aem-author
```

Running this next to the AEM SDK container is not realistic on 15 GB.

### Prerequisites

Docker, root access (for the sysctls in Section 1), and ~10 GB free disk for the
image plus the initial database.

```bash
docker pull saplabs/hanaexpress:latest
```

> ### Pitfall 1 — SAP's own documented `docker run` no longer works
>
> The command in much of SAP's documentation references the image as:
> ```
> store/saplabs/hanaexpress:<tag>
> ```
> which now fails:
> ```
> docker: Error response from daemon: Head "https://registry-1.docker.io/v2/store/
> saplabs/hanaexpress/manifests/latest": unknown: store has been removed
> ```
> The `store/` namespace was the old Docker Store and has been retired. Use
> `saplabs/hanaexpress:latest`. Note this failed *even though the image was
> already pulled locally* — `store/saplabs/...` is a different reference, so
> Docker went to the registry for it.
>
> The same documented command also contains literal `<directory_name>` and
> `<container_name>` placeholders. Pasting it into bash gives:
> ```
> bash: directory_name: Aucun fichier ou dossier de ce nom
> ```
> because `<` is a redirect. Substitute every placeholder before running.

---

## 1. Host kernel parameters

HANA wants larger shared-memory limits than the default. SAP's documented command
sets these with `--sysctl` on `docker run`. **Three of the four cannot be set that
way on this kernel.**

Set them on the host instead:

```bash
sudo tee -a /etc/sysctl.conf <<'EOF'
kernel.shmmax=1073741824
kernel.shmall=8388608
kernel.shmmni=32768
EOF

sudo sysctl -p
sysctl kernel.shmmax kernel.shmall kernel.shmmni
```

Expected:

```
kernel.shmmax = 1073741824
kernel.shmall = 8388608
kernel.shmmni = 32768
```

> ### Pitfall 2 — `kernel.shmmni` is not namespaced, and 524288 is rejected
>
> Passing `--sysctl kernel.shmmni=524288` to `docker run` fails at container init:
> ```
> OCI runtime create failed: runc create failed: unable to start container process:
> error during container init: failed to write sysctl kernel.shmmni = "524288":
> write fsmount:fscontext:proc/./sys/kernel/shmmni: invalid argument
> ```
> Two separate things are wrong:
>
> 1. **`kernel.shmmni` is not a namespaced sysctl** on this kernel, so Docker
>    cannot set it per-container at all. (`kernel.shmmax` and `kernel.shmall`
>    *are* accepted by `--sysctl`, but setting them on the host makes the
>    container flags redundant.)
> 2. **The value 524288 is itself rejected**, even on the host as root:
>    ```
>    sysctl: change la clé « kernel.shmmni »: Argument invalide
>    ```
>    The default was `4096`; `32768` is accepted. Higher values were not tested.
>
> So: set `shmmax`/`shmall`/`shmmni` on the **host**, and pass **only**
> `--sysctl net.ipv4.ip_local_port_range` to `docker run` — that one is
> namespaced and works.
>
> HANA started successfully with `shmmni=32768`. Whether SAP's 524288 matters
> for larger workloads is untested here.

---

## 2. The data directory and its ownership

This is the single most common failure. Create the directory, then hand it to the
container's uid:

```bash
sudo mkdir -p /data/s4hana
sudo chown -R 12000:79 /data/s4hana
sudo chmod 755 /data/s4hana
```

> ### Pitfall 3 — `Insufficient permissions on /hana/mounts`
>
> Symptom, seconds after start:
> ```
> Checking allowed mountpoints ...
> ERROR: Insufficient permissions on /hana/mounts. Required permissions: rwx
> ```
>
> The container runs as `hxeadm`. Verified from inside the image:
> ```bash
> docker run --rm --entrypoint /bin/sh saplabs/hanaexpress:latest -c 'id hxeadm'
> # uid=12000(hxeadm) gid=79(sapsys) groups=79(sapsys),80(hxeshm)
> ```
>
> Chowning the directory to *your* user (uid 1000) is not enough — that is what
> produced the error above.
>
> **`chmod 777` will not fix it either.** The check in `/run_hana:582` is:
> ```
> allowed_mount_points[${mounts}]="-rwxUG"
> ```
> The `U` and `G` require the directory to be **owned by that user and group**,
> not merely writable by anyone. Only the `chown 12000:79` satisfies it.
>
> Consequence: after the chown you can no longer write there as yourself.
> `nano /data/s4hana/foo` will fail with permission denied — use `sudoedit`.

---

## 3. The password file

```bash
sudoedit /data/s4hana/statham.json
```

Two formats are accepted. Both are listed by the image's own help text
(`/run_hana:390-391`):

```json
{"master_password":"<pwd>"}
```

or, to set the system DB and the tenant separately:

```json
{
"system_user_password" : "<pwd>",
"default_tenant_system_user_password" : "<pwd>"
}
```

Then:

```bash
sudo chown 12000:79 /data/s4hana/statham.json
sudo chmod 600 /data/s4hana/statham.json
```

> ### Pitfall 4 — the password policy is enforced, and the error comes late
>
> `/run_hana:1143`:
> ```
> Passwords do not match password policy. A password must have at least 8
> characters with one uppercase letter, one lowercase letter and one number.
> ```
> Every value in the JSON must independently satisfy all four conditions:
> **≥8 characters, ≥1 uppercase, ≥1 lowercase, ≥1 digit.**
>
> This check runs *after* the mount check and *after* the hostname check, so a
> weak password does not surface until you have already fixed everything else.
> Get it right the first time.

> ### The file is deleted after it is read
>
> From the image README (`/hana/print/README:101`):
> > If it is a local file, this file will be deleted, after it has been read. If
> > it is not deletable, the passwords are not changed and the container start
> > fails.
>
> This is by design, not a bug. Two consequences:
> - **Record the passwords somewhere before you start.** They will not be on disk
>   afterwards, and they are the only way into the database.
> - Deletion requires write permission on the *directory*, which the
>   `chown 12000:79` in Section 2 already provides.
> - On any subsequent start, **omit `--passwords-url` entirely** — the file is
>   gone and the passwords are already set.

---

## 4. Start the container

```bash
sudo docker run -d \
  --name sap-hana \
  --hostname hxehost \
  -p 39013:39013 -p 39017:39017 -p 39041-39045:39041-39045 \
  -p 1128-1129:1128-1129 -p 59013-59014:59013-59014 \
  -v /data/s4hana:/hana/mounts \
  --ulimit nofile=1048576:1048576 \
  --sysctl net.ipv4.ip_local_port_range='40000 60999' \
  saplabs/hanaexpress:latest \
  --passwords-url file:///hana/mounts/statham.json \
  --agree-to-sap-license

sudo docker logs -f sap-hana
```

Two flags in there are the difference between this working and not.

> ### Pitfall 5 — `--hostname` is mandatory, even though nothing says so
>
> Symptom on the *second* `docker run`:
> ```
> ERROR: Unknown host '9caceb770df3'. The container must be started with one of
> these host names:
> ERROR:  e7d72d459429
> ```
>
> HANA writes its hostname into the persistent mount at
> `/hana/mounts/system/hosts/<hostname>` on first init. Docker, absent
> `--hostname`, sets the container hostname to the **random container ID** — so
> every `docker rm` + `docker run` presents a new, unrecognised host and HANA
> refuses to start. The logic is at `/run_hana:117-127`.
>
> **Always pass `--hostname hxehost`** (any stable name works). Do it on the very
> first run. If you have already initialized under a random ID, you can either
> adopt it (`--hostname e7d72d459429`), pass `--add-host` (`/run_hana:279`) to
> register an additional host, or — if the instance holds nothing worth keeping —
> wipe and start over:
> ```bash
> docker rm sap-hana
> sudo find /data/s4hana -mindepth 1 ! -name statham.json -delete
> sudo chown -R 12000:79 /data/s4hana
> ```
>
> This is the same class of trap as the AEM guide's Pitfall 6, but worse: there
> the identity lived in the volume binding, here it is written *into the data*.

> ### Pitfall 6 — `--passwords-url` is a URL, not a path
>
> Passing a bare path:
> ```
> --passwords-url /hana/mounts/statham.json
> ```
> gets you, after several minutes of otherwise successful startup:
> ```
> Downloading password file from /hana/mounts/statham.json (parameters: --max-time 30 --insecure) ...
> curl: (3) URL using bad/illegal format or missing URL
> ```
>
> The value is handed straight to `curl` (`/run_hana:1090`). It needs a scheme.
> Use **three slashes** — `file://` plus the absolute path `/hana/mounts/...`:
> ```
> --passwords-url file:///hana/mounts/statham.json
> ```
> HTTP/S endpoints also work.

### What a successful start looks like

```
New host: 'hxehost'
Setting HANAs host name to 'hxehost' ...
Checking allowed mountpoints ...
Checking system requirements ...
...
Start type: initial
Entering pre start phase ...
Downloading password file from file:///hana/mounts/statham.json ...
...
>>>>>>>>>>>>>>>>   HDB info after HANA start
USER          PID     PPID  %CPU        VSZ        RSS COMMAND
hxeadm          1        0   0.0       7320       4440 /bin/bash /run_hana ...
hxeadm       1143        1   0.1     323492      47876 hdbdaemon
hxeadm       1149     1143  59.3    2466148    1566564  \_ hdbnameserver
hxeadm       1919     1143   2.1     285848     155804  \_ hdbcompileserver
hxeadm       1920     1143   2.5     400964     174952  \_ hdbpreprocessor
hxeadm       1960     1143   9.2     723916     331160  \_ hdbdiserver
hxeadm       1961     1143  27.2     963876     390200  \_ hdbwebdispatcher
Entering post start phase ...
Creating tenant database ...
```

`Creating tenant database` is the last long step — several more minutes, mostly
silent. **`Start type: initial` means a genuine first-time database creation.**
Budget 10–15 minutes end to end on this hardware.

---

## 5. Verify

The image ships a healthcheck, so Docker itself will tell you:

```bash
docker ps --filter name=sap-hana --format '{{.Status}}'
# Up 11 minutes (healthy)
```

It runs `/check_hana_health -e ready-status` every 10s with a **30-minute start
period**, so `(health: starting)` during initial creation is expected, not a
warning sign.

Confirm the ports that are genuinely listening rather than trusting the published
list:

```bash
docker exec sap-hana bash -lc 'ss -ltnp'
```

Measured on a healthy instance:

| Port | Bound | Process | What it is |
|---|---|---|---|
| **39013** | `0.0.0.0` | `hdbnameserver` | **SYSTEMDB SQL** |
| **39041** | `0.0.0.0` | `hdbindexserver` | **Tenant `HXE` SQL** |
| 39017 | `0.0.0.0` | `hdbnameserver` | published by SAP's docs as the tenant port |
| 8090 | `0.0.0.0` | `hdbwebdispatcher` | XS Classic, HTTP — see Section 8 |
| 4390 | `0.0.0.0` | `hdbwebdispatcher` | HTTPS — **did not answer** (`curl` → 000) |
| 39000–39014, 39040, 39042, 65000 | `127.0.0.1` | various | internal, not reachable from the host |

> **The tenant is on 39041, not 39017.** Much documentation (and the published
> port list) implies 39017. On this instance `hdbindexserver` — the tenant's
> actual SQL server — binds **39041**. `hdbnameserver` holds 39017. Publishing
> the `39041-39045` range covers it either way, but connect to **39041**.

Also note the majority of ports bind `127.0.0.1` *inside* the container, so
publishing them with `-p` achieves nothing. Only 39013, 39017, 39041, 8090 and
4390 bind `0.0.0.0`.

---

## 6. Connecting

`hdbsql` ships inside the container, so nothing is needed on the host. The binary
is version-pathed, so glob it:

```bash
# system database
sudo docker exec -it -u hxeadm sap-hana bash -lc \
  '/hana/shared/HXE/exe/linuxx86_64/HDB_*/hdbsql -n localhost:39013 -u SYSTEM -p "<system password>"'

# tenant HXE
sudo docker exec -it -u hxeadm sap-hana bash -lc \
  '/hana/shared/HXE/exe/linuxx86_64/HDB_*/hdbsql -n localhost:39041 -d HXE -u SYSTEM -p "<tenant password>"'
```

The two passwords differ if you used the two-key JSON form: `system_user_password`
for 39013, `default_tenant_system_user_password` for 39041.

> **Do not use `/usr/sap/HXE/HDB90/exe/hdbsql`.** That path does not exist —
> `exe` is a symlink to `../exe/linuxx86_64/hdb`, which is also absent. The only
> `hdbsql` in the image is under `/hana/shared/HXE/exe/linuxx86_64/HDB_<version>_<hash>/`.
>
> **Unverified:** bare `hdbsql` *may* be on `hxeadm`'s `PATH` in the running
> container via `hdbenv.sh`. Testing this in a throwaway container failed —
> ```
> hdbenv.sh: Did not find directory /usr/sap/HXE/HDB90/localhost:
> Wrong host 'localhost' -- you must source ~/hdbenv.sh [<virt hostname>]
> ```
> — because that container had no `--hostname hxehost`. It was not retested
> against the real instance. The absolute path above always works.

From the host, any HANA client works against `localhost:39041`:
JDBC (`jdbc:sap://localhost:39041/?databaseName=HXE`), DBeaver's SAP HANA driver,
or the `@sap/hana-client` npm package. **Not verified here** — no host client was
installed.

---

## 7. Daily lifecycle

Once the container exists, never `docker run` it again — Pitfall 5 explains why.

```bash
# End of session — HANA needs a long time to close cleanly
sudo docker stop -t 600 sap-hana

# Next session
sudo docker start sap-hana
```

> ### Pitfall 7 — a plain `docker stop` SIGKILLs the database
>
> Measured after `docker stop sap-hana`:
> ```
> docker inspect sap-hana --format 'ExitCode: {{.State.ExitCode}}'
> # ExitCode: 137
> ```
> `137` is `128 + 9` — **SIGKILL**. Docker sent SIGTERM, waited its default 10
> seconds, then killed the process group mid-shutdown.
>
> The image sets no stop timeout of its own:
> ```
> docker inspect saplabs/hanaexpress:latest --format '{{.Config.StopTimeout}}'
> # <nil>
> ```
> so the 10-second default applies to a database that needs far longer to flush
> and close its persistence layer.
>
> **Always `docker stop -t 600`.** This is the same failure the AEM guide
> documents at `-t 300`, and HANA is slower than Oak.
>
> A `137` is not automatically corruption — this instance was killed that way and
> is expected to recover — but it is gambling with the data volume. Verify the
> exit code after every stop:
> ```bash
> docker inspect sap-hana --format 'ExitCode: {{.State.ExitCode}}'   # want 0
> ```

### What survives what

| Command | Effect |
|---|---|
| `docker stop -t 600` / `docker start` | Safe. |
| `docker stop` (no `-t`) | **SIGKILL at 10s.** Data usually survives; do not rely on it. |
| `docker rm sap-hana` | Container gone. `/data/s4hana` is a **bind mount**, so the database survives. Recreate with the *same* `--hostname`. |
| `rm -rf /data/s4hana/*` | **Destroys the database.** |

A bind mount to `/data/s4hana` is deliberately safer than a named volume here:
`docker volume prune` cannot touch it.

---

## 8. The UI question

**There is no SAP UI in this image.** This was measured, not assumed.

One HTTP listener exists — `hdbwebdispatcher` on 8090 — and it answers:

```bash
docker exec sap-hana bash -lc 'curl -s http://localhost:8090/ | head -c 200'
# <html><head>...<title>SAP XSEngine</title>...
```

That is an orange-on-black placeholder page. Probing the standard XS Classic
application paths on a healthy instance:

| Path | Status |
|---|---|
| `/` | 200 — "SAP XSEngine" placeholder |
| `/sap/hana/xs/admin/` | 404 |
| `/sap/hana/ide/` | 404 |
| `/sap/hana/xs/ide/` | 404 |
| `/sap/hana/democontent/` | 404 |
| `/sap/hana/xs/formLogin/login.html` | 404 |
| `/sap/hana/xs/wdisp/admin/` | 301 |

The engine runs; **no applications are deployed to it.** There is no Web IDE, no
cockpit, no admin UI, no sample content.

Port 8090 is also **not in the `-p` list** in Section 4, so it is not reachable
from the host at all as configured. If you want the placeholder page anyway, add
`-p 8090:8090 -p 4390:4390` when recreating the container (keeping
`--hostname hxehost`, and dropping `--passwords-url`). 4390 did not answer even
from inside, so HTTPS appears unconfigured.

### So how do you get a UI?

Three options, in ascending order of cost:

**1. A SQL client.** Not a browser, but it is the real interface to what you
built. `hdbsql` in the container (Section 6), or DBeaver on the host against
`localhost:39041`. For a database, this is usually the honest answer.

**2. Write a UI5 app on the host.** SAPUI5 is where "SAP components" actually
live — a component is a literal `Component.js` + `manifest.json`. Developed with
Node on the host, not inside any container:

```bash
npm i -g @ui5/cli
ui5 init && ui5 use OpenUI5 && ui5 add sap.m sap.ui.core themelib_sap_horizon
ui5 serve -o index.html --livereload
```

That serves `localhost:8080` with reload-on-save — the direct analogue of the AEM
`aemsync` loop. A backend (CAP via `@sap/cds-dk`, or a small Node service using
`@sap/hana-client`) bridges it to HANA on 39041.

**Unverified:** none of the UI5/CAP tooling in this section was installed or run
on this host. The commands are from documentation, not from a working setup here.

**3. `saplabs/hanaexpress-xsa`.** A different, substantially larger image that
includes the XS Advanced runtime and its browser tooling, deployed to via
`xs push`. Not attempted — the RAM figures in Section 0 rule it out on a 15 GB
host that peaked at 279 MB free running the *server-only* image.

---

## 9. Teardown

```bash
sudo docker stop -t 600 sap-hana && docker rm sap-hana
```

The database survives in `/data/s4hana`; recreating the container with the same
`--hostname hxehost`, the same `-v`, and **no** `--passwords-url` resumes it.

To destroy the database as well:

```bash
sudo rm -rf /data/s4hana
```

---

## Known-noisy output you can ignore

- **`WARNING: Unsupported Linux kernel version 7.0. Supported are 3.12 and 4.4+.`**
  Printed on every start. Non-fatal — the script continues straight past it. The
  image predates this kernel.

- **The seccomp syscall check "fails" and startup continues anyway:**
  ```
  Checking syscalls ...
          WARNING: Operation not permitted: move_pages
          WARNING: Operation not permitted: mbind
  Check failed: syscalls
  ```
  Both are NUMA memory-placement syscalls blocked by Docker's default seccomp
  profile. On a 4-core single-socket box there is no NUMA topology to optimize
  for, so the practical cost is nil.

  **The remedy the message suggests does not work.** It says to run
  `docker run --rm <hana_image> --print seccomp.json > seccomp.json`, but:
  ```bash
  docker run --rm --entrypoint /bin/sh saplabs/hanaexpress:latest -c 'ls /hana/print/'
  # README
  # hdb_version
  ```
  There is no `seccomp.json` to print in this image. `--security-opt
  seccomp=unconfined` silences the warning at the cost of dropping syscall
  filtering entirely; leaving it alone is reasonable for local development.

- **`use_sapstartsrv: false`** on the first line of every start. Informational.

---

## Appendix — how the image was inspected

Every claim above about `/run_hana` and image internals came from reading the
image directly. These are safe to re-run:

```bash
# the user the container runs as
docker run --rm --entrypoint /bin/sh saplabs/hanaexpress:latest -c 'id hxeadm'

# mountpoint permission requirements
docker run --rm --entrypoint /bin/sh saplabs/hanaexpress:latest \
  -c 'grep -n -A12 "Checking allowed mountpoints" /run_hana'

# hostname check logic
docker run --rm --entrypoint /bin/sh saplabs/hanaexpress:latest \
  -c 'grep -n -B10 -A20 "must be started with one of these host names" /run_hana'

# accepted password JSON formats and the password policy message
docker run --rm --entrypoint /bin/sh saplabs/hanaexpress:latest \
  -c 'sed -n "385,395p;1143p" /run_hana'

# full README, including the file:/// and file-deletion semantics
docker run --rm --entrypoint /bin/sh saplabs/hanaexpress:latest -c 'cat /hana/print/README'

# available --print options
docker run --rm --entrypoint /bin/sh saplabs/hanaexpress:latest -c 'ls /hana/print/'
```
