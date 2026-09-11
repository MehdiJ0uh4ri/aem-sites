# AeroVance Systems — AEM implementation notes

An aerospace & defence corporate site built on AEM as a Cloud Service, layered onto the
existing `mysite` archetype project.

**Rendering model: traditional AEM Sites, Core Components + HTL.** The spec left this open;
the repository had already answered it. This is an archetype project with Sling Models, HTL
templates and a dispatcher module, so the AeroVance work extends that rather than starting an
Edge Delivery project beside it.

**App ID stays `mysite`.** Renaming the app (`/apps/mysite`, `com.mysite.core`,
`/conf/mysite`, `/content/mysite`) would have touched every existing file for no functional
gain. New components carry the AeroVance component groups, so authors see them grouped
correctly regardless of the underlying namespace.

---

## What is here

### Components

All under `/apps/mysite/components`. Each has `.content.xml`, `_cq_dialog`, HTL, a Sling Model
interface plus implementation in `core`, and SCSS (plus JS where it needs behaviour).

| Component | Resource type | Notes |
|---|---|---|
| Aerospace Hero | `aerohero` | Image or muted-loop video backdrop, up to 3 counters, export-control notice. Dark/light and full/compact via Style System. |
| Technical Spec Table | `spectable` | Both unit systems in the markup, metric/imperial switch, accessible column sorting with numeric keys. |
| Programme Teaser | `programteaser` | Status badge, up to 3 headline figures, stretched-link card. |
| Timeline | `timeline` | Ordered milestone list; spine and markers drawn in CSS from the list itself. |
| Resource Library | `resourcelibrary` | Ungated links plus genuinely gated downloads with CRM lead capture. |
| Stock Ticker | `stockticker` | Live quote via OSGi service, delivered through Sling Dynamic Include. |
| Job Listing Grid | `joblisting` | Live ATS search, query-parameter filtering, schema.org JobPosting. |
| Global Locations Map | `locationsmap` | Equirectangular pin projection plus a filterable address list. |

### Services (`com.mysite.core.services`)

| Service | Implementation | Purpose |
|---|---|---|
| `StockQuoteService` | `StockQuoteServiceImpl` | Market-data feed, cached for the licensed delay interval. |
| `JobSearchService` | `WorkdayJobSearchService` | ATS search, bounded LRU cache keyed on the visitor's filters. |
| `CrmLeadService` | `SalesforceCrmLeadService` | Gated-download leads to Salesforce, with a replay log for failures. |

All three share `JsonHttp`, which bounds every outbound call with a connect and socket timeout
and converts failures into an empty `Optional` rather than an exception. No third-party outage
can turn into a 500 on a page a visitor asked for.

### Workflow

`ExportControlGateProcess` + the `aerovance-export-control-publication` model
(`/conf/global/settings/workflow/models`).

Steps: Comms review → Legal review → Export control review → **gate** → activate.

The spec asked for this to be a real step rather than a UI note, and it is: the gate throws
`WorkflowException` unless the page carries both a recorded clearance and a classification,
which stops the workflow and leaves the page unpublished. It cannot grant clearance itself —
it verifies that a named reviewer recorded one on the page's Export Control tab, then writes
the audit record (who, when) onto the page.

The reviewer's fields live in a new **Export Control** tab in page properties
(`/apps/mysite/components/page/_cq_dialog`). The two audit fields there are read-only: a record
of an approval the approver can edit afterwards is not a record.

Assets carry the same determination independently, via the
`aerovance-technical-asset` metadata schema — because an image of a defence programme can be
controlled technical data on its own, and recording it on the asset is what makes the
determination survive reuse on a second and third page.

### The download gate is real

This is the part most often built cosmetically, so to be explicit about how it holds:

1. Gated assets live under `/content/dam/mysite/gated`, which repoinit denies to `everyone` on
   publish. Guessing the URL returns 404.
2. `GatedDownloadServlet` is the only route in. It reads the file through the
   `aerovance-gated-download` service user, which has `jcr:read` on that one folder and nothing
   else.
3. The servlet only serves a path the posted component actually lists as gated, so a crafted
   `asset` parameter cannot turn the service user into an arbitrary file reader.
4. The response is `Cache-Control: private, no-store`, and the dispatcher denies
   `*.download.html*` as well. A gated whitepaper served from cache to the next visitor would
   defeat the gate silently.
5. The document is served **even when the CRM call fails**. The visitor completed their side of
   the exchange; an internal outage is not their problem. Undelivered leads go to a dedicated
   replay log rather than `error.log`, because they are personal data with their own retention
   and access rules.

The marketing opt-in is unticked by default and never required to submit. Downloading a
whitepaper is not consent to be contacted about it.

### Templates

Four editable templates under `/conf/mysite/settings/wcm/templates`:
`aerovance-program`, `aerovance-press-release`, `aerovance-careers`,
`aerovance-investor-relations`.

On the programme template the hero and the specification table are locked into the structure
rather than left to the author. Every programme page must open the same way and carry its
figures in the same place, because the export-control reviewer reads these pages in sequence
and a table that moves between programmes is a table that gets skipped.

### Content Fragment Models

`aircraft-program`, `press-release`, `executive-bio`, `facility`, under
`/conf/mysite/settings/dam/cfm/models`. All GraphQL-exposed for the partner portal and app.

Programme figures are stored **metric only** and converted for display. Storing both systems
invites them to disagree after an edit, and two contradictory published performance figures is
exactly what the export-control review exists to catch.

### Style System

Every visual variant is a Style System entry, not a dialog field. A variant chosen in a dialog
is a content decision an author re-makes on every instance; a style is a design decision the
policy makes once per template. Retiring a variant then costs one policy edit instead of a
content migration.

### Dispatcher

`cache/rules.any` and `filters/filters.any` carry the AeroVance exceptions, each with the
reason it exists: the SDI ticker fragment, careers results, gated downloads, and the gated DAM
tree. The filter allows exactly one POST endpoint (the download selector) rather than opening a
general POST channel into `/content`.

---

## Decisions worth knowing about

**Careers filtering uses query parameters and a plain form submit, not fetch.** Three things
follow, all of which matter on a careers page: it works with JavaScript disabled, a filtered
view has a URL a candidate can send to a colleague, and search engines can crawl it. The cost
is a page load per filter change, which on a page filtered two or three times is the right side
of the bargain.

**The locations map filters client-side instead.** The whole dataset is already in the page, so
filtering there is hiding rows, not fetching anything.

**"ATS unreachable" and "no roles match" are separate states.** Telling a candidate there are no
openings when the system is simply down is actively misleading, and the two ask them to do
different things.

**Gson is declared explicitly in `core/pom.xml`, before `aem-sdk-api`.** The uber jar carries its
own partial, older copy of the same classes; Maven builds the classpath in declaration order, so
listing it second lets the uber jar win and the two versions mix — which fails at run time inside
Gson's internals while the deployed bundle works fine. The ordering is load-bearing.

**Pin positions are formatted with `Locale.ENGLISH`.** CSS only accepts a full stop as the decimal
separator. On a European publish instance the JVM default locale would render `78.8875` as
`78,8875`, the browser would discard the declaration, and every pin would stack in the top-left
corner with nothing in the logs to explain it. There is a test for this.

**Secrets are Cloud Manager references, never literals.** The three OSGi configs use
`$[secret:AEROVANCE_*]`. Set `AEROVANCE_STOCK_API_TOKEN`, `AEROVANCE_ATS_API_TOKEN` and
`AEROVANCE_CRM_ACCESS_TOKEN` per environment. Without them the services log at debug and the
components render their fallbacks, which is the intended behaviour on a local author instance.

---

## Build

```
mvn clean install          # all modules, runs the unit tests
mvn clean install -PautoInstallSinglePackage   # deploy to a local author instance
```

The front end builds through `ui.frontend` (webpack + `aem-clientlib-generator`) into
`/apps/mysite/clientlibs/clientlib-site`.

---

## Not built

Named explicitly so nobody assumes otherwise:

- **MSM live copies and the regional rollout structure.** The blueprint/live-copy configuration
  is a content-tree operation, not source, and depends on which regions launch first.
- **Adobe Target and Analytics/CJA wiring, and the consent management platform.** These are
  Cloud Manager and Launch configuration rather than repository code.
- **Sample assets.** The sample programme page references DAM paths that do not exist in this
  package; the components handle missing assets without breaking, but the page will not look
  finished until real imagery is loaded.
- **Translation workflow connector.** The workflow model has the review steps; the TMS
  integration itself is a separate connector package.
- **IP allowlisting for the supplier portal, and the WAF rules.** Cloud Manager configuration.

Every figure in the sample content is invented. `AV-380X` is not a real aircraft, and the
numbers are internally consistent rather than accurate — deliberately, so plausible-looking
demo data cannot be mistaken for a real specification or be export-controlled.
