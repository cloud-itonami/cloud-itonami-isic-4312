# cloud-itonami-4312

Open Business Blueprint for **ISIC Rev.5 4312**: site preparation (excavation, earth-moving, land clearing, test drilling/boring/core sampling).

This repository designs a forkable OSS business for site-preparation-
project operations coordination: run by a qualified operator so a
community keeps its own operating records instead of renting a closed
SaaS.

## Scope -- this is a COORDINATION-ONLY actor, not equipment control

This is a safety-critical domain: excavation-collapse risk, buried-
utility strike risk, soil/groundwater contamination discovery. **This
actor does NOT hold excavation/earth-moving-equipment-control authority,
and it does NOT hold geotechnical/site-readiness sign-off authority.**
Both are the licensed engineer / site supervisor's exclusive authority,
always. The Site Prep Advisor (LLM) never issues an equipment-control
command and never finalizes a geotechnical/site-readiness decision; the
independent **Site Prep Governor** HARD-blocks any proposal that even
tries (un-overridable by any human approval -- see `site-prep.governor`
ns docstring). This actor coordinates *potential* excavation/earth-
moving-equipment dispatch (a proposed schedule window, a flagged
concern, a supply-order proposal) -- it never directly actuates.

Structurally, EVERY proposal this actor's advisor can produce carries
`:effect :propose`, and the Site Prep Governor HARD-holds any proposal
that doesn't -- this is a permanent invariant distinguishing this actor
from `cloud-itonami-isic-4211` (the robotics-premise reference this
actor follows structurally), whose sibling actuation ops DO commit
real-world effects (mail dispatch, robot placement, structure handover).
`cloud-itonami-isic-4211`'s README robotics-premise framing therefore
does NOT apply verbatim here: this actor is deliberately narrower, the
same posture `cloud-itonami-isic-4311` (Demolition) established for this
fleet.

## Core Contract

```text
site/permit record + independent verification
        |
        v
Advisor -> Site Prep Governor -> proceed (log/schedule/flag/order proposal), hold, or human approval
        |
        v
coordination artifacts (schedule proposal, safety-concern flag,
supply-order proposal) + audit ledger -- NEVER equipment dispatch,
NEVER a geotechnical/site-readiness sign-off
```

No automated advice can propose a schedule the governor refuses, suppress
a safety-concern flag, or slip an equipment-control/site-readiness-
signoff marker past the governor -- and even a clean, governor-approved
proposal still always needs a human sign-off for scheduling and safety
concerns (see `Actuation` below).

## Capability layer

Resolves via [`kotoba-lang/industry`](https://github.com/kotoba-lang/industry)
(ISIC `4312`). Required capabilities:

- `:identity`
- `:forms`
- `:audit-ledger`
- `:notifications`

## Implemented slice (`src/site_prep`)

`blueprint.edn` names the governor `:site-prep-governor` and is now
`:implemented`. This repo implements it end-to-end -- **Site Prep
Advisor ⊣ Site Prep Governor** -- following the SAME `.cljc` actor
pattern (langgraph-clj StateGraph, mock-by-default advisor, dual
MemStore/Datomic backend, 0→3 phase rollout) every prior
`cloud-itonami-isic-*` actor in this fleet uses, structured after
[`cloud-itonami-isic-4311`](https://github.com/cloud-itonami/cloud-itonami-isic-4311)
(the coordination-only demolition-domain reference, itself narrowed from
[`cloud-itonami-isic-4211`](https://github.com/cloud-itonami/cloud-itonami-isic-4211)),
narrowed to coordination-only authority as described above.

### Closed op-allowlist (4 ops, all `:effect :propose`)

| Op | Ask | Implementation |
|---|---|---|
| `:log-site-record` | excavation-progress / soil-test / utility-locate data logging | Normalizes and commits a patch onto the site's ground-truth fields (`:site-verified?`, `:utility-locate-completed?`, `:notification-lead-days-actual`, concern resolution, etc.) and appends an immutable site-record-log entry. No direct capital/safety risk -- MAY auto-commit at phase 3. |
| `:schedule-site-operation` | excavation / earth-moving / clearing scheduling proposal | Drafts a proposed schedule WINDOW (never a geotechnical/site-readiness sign-off). ALWAYS escalates to a human at every phase -- coordinates potential excavation/earth-moving-equipment dispatch. |
| `:flag-safety-concern` | surface an excavation-collapse / buried-utility-strike / contamination concern | Drafts a safety-concern flag; ALWAYS escalates to a human, unconditionally. Once approved, `site-prep.notify` sends the notice (mail + phone) to the site's licensed-engineer/site-supervisor/geotechnical-authority contact roster. |
| `:order-supplies` | equipment/materials procurement proposal | Drafts a supply-order proposal. Escalates above a cost threshold or below the confidence floor; may auto-commit at phase 3 otherwise. |

**Legal basis is data, not code** -- `src/site_prep/facts.cljc`'s
`catalog` is the per-jurisdiction EDN source-of-truth the governor checks
every `:schedule-site-operation` proposal against (JPN/USA/DEU seeded;
DEU stands in for the EU, the same convention `construction.facts`/
`demolition.facts` use for EASA/state-level German law):

| Jurisdiction | Utility-locate legal basis | Excavation-notification legal basis |
|---|---|---|
| 🇯🇵 Japan | 労働安全衛生規則（昭和47年労働省令第32号）第355条 -- [e-Gov](https://laws.e-gov.go.jp/law/347M50002000032) | 騒音規制法 第14条（特定建設作業〈バックホウ80kW以上・ブルドーザー40kW以上等〉開始の7日前までの市町村長への届出）-- [e-Gov](https://laws.e-gov.go.jp/law/343AC0000000098) |
| 🇺🇸 USA | OSHA 29 CFR 1926.651(b) -- [osha.gov](https://www.osha.gov/laws-regs/regulations/standardnumber/1926/1926.651) | State one-call ("Call Before You Dig") statutes coordinated via the national 811 number; most states require ≥2 business days' notice (e.g. Texas, New York) -- [811beforeyoudig.com](https://811beforeyoudig.com/) |
| 🇪🇺 EU (DEU proxy) | DIN 4124:2012-01 (excavation/trench slope & shoring design) + general duty-of-care utility-operator query (Verkehrssicherungspflicht) -- [dinmedia.de](https://www.dinmedia.de/en/standard/din-4124/147362129) | Landesbauordnung Baubeginnanzeige (qualitative -- state-level, no fixed EU-wide day count) |

Japan (7 calendar days) and the USA (2 business days) have real numeric
notification lead-time triggers; the EU deliberately does NOT --
`site-prep.facts/notification-lead-insufficient?` reports `:qualitative`
there rather than fabricating a number, and `:schedule-site-operation`
always routes to a human regardless of jurisdiction anyway (see
`Actuation` below). Note the USA citation is honestly labeled a
WIDELY-ADOPTED STATE-LAW CONVENTION coordinated through the national 811
one-call number, NOT a single federal statute with one fixed day count
(unlike `demolition.facts`'s federal NESHAP 10-working-day rule) -- see
`site-prep.facts` ns docstring for the full honesty discipline.

**Governor -- eight HARD checks, ALL un-overridable by human approval:**
unknown op (outside the closed 4-op allowlist), `:effect` not `:propose`,
forbidden action class (equipment-control / direct-actuation /
geotechnical-signoff-finalization markers), site not independently
verified/registered, legal-basis missing, utility-locate survey
incomplete, notification lead time insufficient (quantitative
jurisdictions only), unresolved safety concern on file. See
`site-prep.governor` ns docstring for the full enumeration, rationale and
real-law citations behind each.

## Actuation

This actor performs **no real-world actuation** -- every committed
record carries `:effect :propose` (see `site-prep.governor` ns
docstring). `:schedule-site-operation` and `:flag-safety-concern` NEVER
auto-commit at any phase -- both always need a human sign-off, even when
the governor is completely clean (`site-prep.phase` ns docstring
'Actuation' section, `site-prep.governor`'s `high-stakes` set).
`:log-site-record` (pure data logging) and `:order-supplies` BELOW the
cost threshold (`site-prep.governor/supply-order-cost-threshold-usd`)
MAY auto-commit at phase 3 when the governor is clean.

```bash
clojure -M:dev:run    # demo: full coordination episode + every HARD hold
clojure -M:dev:test   # test suite
clojure -M:lint       # clj-kondo, errors fail
```

## License

AGPL-3.0-or-later.
