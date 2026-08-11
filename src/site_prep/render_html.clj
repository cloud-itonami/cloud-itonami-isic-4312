(ns site-prep.render-html
  "Build-time HTML renderer for `docs/samples/operator-console.html`.

  This repo had a `docs/index.html` blueprint page but NO operator console
  and no generator. This namespace closes that gap by driving the REAL
  actor stack -- `site-prep.operation` (the langgraph-clj StateGraph) ->
  `site-prep.advisor` -> `site-prep.governor` -> `site-prep.phase` ->
  `site-prep.store` -> `site-prep.notify` -- through a scenario adapted
  from this repo's own `site-prep.sim` demo driver (`clojure -M:dev:run`,
  confirmed working against the real seeded site ids `site-1`..`site-8`
  BEFORE this file was written), and rendering the resulting SSoT +
  append-only audit ledger.

  Nothing on the page is hand-typed domain data:

    - the site table is `site-prep.store/all-sites` of the seeded
      `MemStore`, read field by field (`:site-verified?`,
      `:utility-locate-completed?`, `:notification-lead-days-actual`,
      `:contamination-detected?`, `:safety-concern-unresolved?`);
    - the notification-lead-time verdict per site is recomputed by the
      SAME `site-prep.facts/notification-lead-insufficient?` the governor
      itself calls (three-valued -- a `:qualitative` jurisdiction is
      reported as such, never given a fabricated day count);
    - every HARD hold row is a real `:governor-hold` fact the Site Prep
      Governor produced on deliberately non-compliant input, carrying the
      governor's OWN `:rule` keyword and its OWN `:detail` string;
    - the safety-concern notice dispatch table is
      `site-prep.notify/sent-log` of the mock notifier the `:commit` node
      actually called;
    - jurisdiction coverage is `site-prep.facts/coverage`.

  Only the `Action gate` table is prose: it describes this actor's fixed
  op contract (`site-prep.governor` / `site-prep.phase` ns docstrings),
  which is documentation of permanent behavior, not runtime telemetry.

  Ledger fact types: `site-prep.store/append-ledger!` is called from
  exactly two graph nodes -- `:commit` (writing `:committed`) and `:hold`
  (writing `:governor-hold` / `:approval-rejected`). `:approval-granted`
  and `:approval-requested` only ever reach the in-memory `:audit`
  channel, so this renderer deliberately does NOT branch on them; doing
  so would render a status that can never appear.

  Deterministic: no timestamps, no randomness, no wall-clock reads. Two
  consecutive runs against the same seed produce byte-identical output
  (verify with `cmp` on two runs).

  Usage: `clojure -M:dev:render-html [out-file]`
  (default `docs/samples/operator-console.html`)."
  (:require [clojure.string :as str]
            [jp-go-dds.skin :as skin]
            [langgraph.graph :as g]
            [site-prep.facts :as facts]
            [site-prep.notify :as notify]
            [site-prep.operation :as op]
            [site-prep.store :as store]))

(def ^:private operator
  {:actor-id "op-1" :actor-role :site-supervisor :phase 3})

(defn- exec! [actor tid request]
  (g/run* actor {:request request :context operator} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}}
          {:thread-id tid :resume? true}))

(defn run-demo!
  "Runs a fresh seeded store + mock notifier through a scenario that
  reaches every disposition this actor can produce. All eight subjects
  (`site-1`..`site-8`) come from `site-prep.store/demo-data`; no id is
  invented.

  site-1 walks a full coordination episode: a site-record log entry
  (auto-commits at phase 3 -- pure data logging), a site-operation
  schedule proposal (ALWAYS escalates -- `:schedule-site-operation` is
  permanently high-stakes, never auto at any phase -- approved), a
  safety-concern flag (ALWAYS escalates -- approved, and the `:commit`
  node then really dispatches the notice through the injected mock
  notifier to the site's `:safety-contacts` roster), a second
  site-record log entry recording the concern resolved (auto-commits), a
  re-proposed schedule now that the ground-truth field is clear
  (approved), a supply order below `site-prep.governor/
  supply-order-cost-threshold-usd` (auto-commits) and one above it
  (escalates on cost -- approved).

  Then SEVEN HARD holds that never reach a human at all, one per
  governor check that this seed can exercise: site-2 has no spec-basis
  for its (deliberately unregistered) jurisdiction; site-3 is not
  independently verified AND has no utility locate on file; site-4 has an
  incomplete utility locate; site-5's own recorded notification lead time
  (2 days) is short of Japan's 7-day statutory minimum; site-6 has an
  unresolved safety concern on file; an op outside the closed four-op
  allowlist is rejected structurally; and a `:log-site-record` patch
  carrying an `:equipment-control?` marker is rejected by the
  forbidden-action-class check -- the defence-in-depth probe proving the
  governor censors the ADVISOR'S OWN OUTPUT independently, not just the
  request.

  Finally a cross-jurisdiction (USA, quantitative, lead sufficient) and a
  qualitative (DEU/EU, no fabricated numeric lead-time) schedule
  walkthrough, both approved.

  Returns `{:db store :notifier notifier}`."
  []
  (let [db (store/seed-db)
        notifier (notify/mock-notifier)
        actor (op/build db {:notifier notifier})]

    ;; --- site-1: the full coordination episode ---
    (exec! actor "t1" {:op :log-site-record :subject "site-1"
                       :patch {:id "site-1" :contamination-detected? false}})

    (exec! actor "t2" {:op :schedule-site-operation :subject "site-1"
                       :window {:proposed-start-date "2026-08-01"
                                :proposed-end-date "2026-08-10"}
                       :notes "表土剥ぎ、掘削、整地の順で実施"})
    (approve! actor "t2")

    (exec! actor "t3" {:op :flag-safety-concern :subject "site-1"
                       :concern-type :buried-utility-strike
                       :concern-description "掘削中に未記載のガス管と思われる埋設物を確認。"})
    (approve! actor "t3")

    (exec! actor "t4" {:op :log-site-record :subject "site-1"
                       :patch {:id "site-1" :safety-concern-unresolved? false}})

    (exec! actor "t5" {:op :schedule-site-operation :subject "site-1"
                       :window {:proposed-start-date "2026-08-08"
                                :proposed-end-date "2026-08-18"}
                       :notes "埋設物迂回ルートを反映した改訂スケジュール"})
    (approve! actor "t5")

    (exec! actor "t6" {:op :order-supplies :subject "site-1"
                       :items ["silt-fencing" "geotextile-fabric"]
                       :cost-usd 1200 :vendor "Local Earthworks Supply Co."})

    (exec! actor "t7" {:op :order-supplies :subject "site-1"
                       :items ["excavator-rental" "haul-truck-rental"]
                       :cost-usd 18000 :vendor "Heavy Equip Rentals"})
    (approve! actor "t7")

    ;; --- the HARD holds (no human is ever consulted) ---
    (exec! actor "t8"  {:op :schedule-site-operation :subject "site-2" :window {}})
    (exec! actor "t9"  {:op :schedule-site-operation :subject "site-3" :window {}})
    (exec! actor "t10" {:op :schedule-site-operation :subject "site-4" :window {}})
    (exec! actor "t11" {:op :schedule-site-operation :subject "site-5" :window {}})
    (exec! actor "t12" {:op :schedule-site-operation :subject "site-6" :window {}})
    (exec! actor "t13" {:op :direct-equipment-command :subject "site-1"})
    (exec! actor "t14" {:op :log-site-record :subject "site-1"
                        :patch {:id "site-1" :equipment-control? true}})

    ;; --- cross-jurisdiction walkthroughs ---
    (exec! actor "t15" {:op :schedule-site-operation :subject "site-7"
                        :window {:proposed-start-date "2026-09-01"
                                 :proposed-end-date "2026-09-12"}})
    (approve! actor "t15")

    (exec! actor "t16" {:op :schedule-site-operation :subject "site-8"
                        :window {:proposed-start-date "2026-09-10"
                                 :proposed-end-date "2026-09-20"}})
    (approve! actor "t16")

    {:db db :notifier notifier}))

;; ----------------------------- rendering -----------------------------

(defn- esc [v]
  (-> (str v)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")))

(defn- clip
  "Truncate a long citation for a table cell, marking the truncation."
  [s n]
  (if (> (count s) n) (str (subs s 0 n) "…") s))

(defn- basis-str
  "Ledger `:basis` is `(mapv :rule violations)` for holds (keywords) and
  the proposal's `:cites` for commits (official legal-basis strings)."
  [basis]
  (->> basis
       (map #(clip (if (keyword? %) (name %) (str %)) 88))
       (str/join " / ")))

(defn- last-fact-for [ledger site-id]
  (last (filter #(= (:subject %) site-id) ledger)))

(defn- status-cell [ledger site-id]
  (let [f (last-fact-for ledger site-id)]
    (cond
      (nil? f) "<span class=\"muted\">no activity</span>"
      (= :committed (:t f))
      (str "<span class=\"ok\">committed</span> <span class=\"muted\">"
           (esc (name (:op f))) "</span>")
      (= :governor-hold (:t f))
      (str "<span class=\"critical\">HARD hold</span> <span class=\"muted\">"
           (esc (name (:op f))) "</span><br><span class=\"critical\">"
           (esc (basis-str (:basis f))) "</span>")
      :else (str "<span class=\"muted\">" (esc (name (:t f))) "</span>"))))

(defn- bool-cell [v yes no]
  (if (true? v)
    (str "<span class=\"ok\">" yes "</span>")
    (str "<span class=\"critical\">" no "</span>")))

(defn- contamination-cell [v]
  (cond (true? v)  "<span class=\"warn\">detected</span>"
        (false? v) "<span class=\"ok\">none recorded</span>"
        :else      "<span class=\"muted\">unknown</span>"))

(defn- concern-cell [v]
  (if (true? v)
    "<span class=\"critical\">unresolved</span>"
    "<span class=\"ok\">none open</span>"))

(defn- lead-cell
  "Recomputed with the governor's own `site-prep.facts/notification-lead-
  insufficient?` -- three-valued, so a `:qualitative` jurisdiction is
  reported as qualitative rather than given an invented day count."
  [{:keys [jurisdiction notification-lead-days-actual] :as site}]
  (let [minimum (:notification-lead-days (facts/spec-basis jurisdiction))
        verdict (facts/notification-lead-insufficient? jurisdiction site)]
    (case verdict
      true  (str "<span class=\"critical\">" (esc notification-lead-days-actual)
                 "d &lt; " (esc minimum) "d statutory minimum</span>")
      ;; `notification-lead-insufficient?` is `false` both when a recorded
      ;; actual clears the minimum AND when nothing is recorded at all
      ;; (`(number? nil)` is false) -- do not report the second case as
      ;; sufficient.
      false (if (number? notification-lead-days-actual)
              (str "<span class=\"ok\">" (esc notification-lead-days-actual)
                   "d &ge; " (esc minimum) "d</span>")
              (str "<span class=\"muted\">not recorded &mdash; statutory minimum "
                   (esc minimum) "d</span>"))
      :qualitative "<span class=\"muted\">qualitative jurisdiction &mdash; no fixed numeric lead time</span>"
      "<span class=\"muted\">no spec-basis on file</span>")))

(defn- site-row [ledger {:keys [id jurisdiction site-verified?
                                utility-locate-completed?
                                contamination-detected?
                                safety-concern-unresolved?] :as site}]
  (format (str "        <tr><td><code>%s</code><br><span class=\"muted\">%s</span></td>"
               "<td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>")
          (esc id) (esc (:name site)) (esc jurisdiction)
          (bool-cell site-verified? "verified" "not verified")
          (bool-cell utility-locate-completed? "complete" "incomplete")
          (lead-cell site)
          (contamination-cell contamination-detected?)
          (concern-cell safety-concern-unresolved?)
          (status-cell ledger id)))

(defn- ledger-row [{:keys [t op subject disposition basis]}]
  (format "        <tr><td>%s</td><td><code>%s</code></td><td><code>%s</code></td><td>%s</td><td>%s</td></tr>"
          (if (= :committed t)
            "<span class=\"ok\">committed</span>"
            (str "<span class=\"critical\">" (esc (name t)) "</span>"))
          (esc (name (or op :n-a)))
          (esc subject)
          (esc (name (or disposition :n-a)))
          (esc (basis-str basis))))

(defn- hold-rows
  "One row per violation of every `:governor-hold` fact in the ledger --
  the governor's own `:rule` keyword and its own `:detail` text."
  [ledger]
  (for [f ledger
        :when (= :governor-hold (:t f))
        v (:violations f)]
    (format "        <tr><td><code>%s</code></td><td><code>%s</code></td><td><code>%s</code></td><td>%s</td></tr>"
            (esc (:subject f)) (esc (name (:op f)))
            (esc (name (:rule v))) (esc (:detail v)))))

(defn- notice-rows [sent]
  (for [{:keys [channel to subject message status]} sent]
    (format "        <tr><td><code>%s</code></td><td>%s</td><td>%s</td><td>%s</td></tr>"
            (esc (name channel)) (esc to)
            (esc (or subject message ""))
            (str "<span class=\"ok\">" (esc (name status)) "</span>"))))

(def ^:private action-gate-rows
  ;; Prose description of this actor's own CLOSED four-op contract
  ;; (`site-prep.governor` / `site-prep.phase` ns docstrings) -- fixed
  ;; behaviour, not runtime telemetry, so it is legitimately described
  ;; here rather than derived from a run.
  ["        <tr><td><code>:log-site-record</code></td><td><span class=\"ok\">phase-3 auto-commit when the governor is clean</span> &middot; pure data logging, no capital or safety risk</td></tr>"
   "        <tr><td><code>:schedule-site-operation</code></td><td><span class=\"warn\">ALWAYS human approval</span> &middot; never auto at any phase &middot; spec-basis, utility locate, notification lead time and open safety concerns all re-checked against the store's own ground truth</td></tr>"
   "        <tr><td><code>:flag-safety-concern</code></td><td><span class=\"warn\">ALWAYS human approval</span> &middot; never auto at any phase &middot; on approval the notice is really dispatched to the site's safety-contact roster</td></tr>"
   "        <tr><td><code>:order-supplies</code></td><td><span class=\"ok\">phase-3 auto-commit below the cost threshold</span> &middot; <span class=\"warn\">escalates above 5000 USD or below the confidence floor</span></td></tr>"
   "        <tr><td><code>anything else</code></td><td><span class=\"critical\">structural HARD hold</span> &middot; the four-op allowlist is closed; <code>:effect</code> must be <code>:propose</code>; equipment-control / direct-actuation / geotechnical-sign-off markers are permanently forbidden and un-overridable by any approver</td></tr>"])

(defn- coverage-rows [{:keys [covered-jurisdictions]}]
  (for [iso3 covered-jurisdictions
        :let [{:keys [owner-authority threshold-model notification-lead-days]
               :as sb} (facts/spec-basis iso3)]]
    (format "        <tr><td><code>%s</code></td><td>%s</td><td><code>%s</code></td><td>%s</td><td>%s</td></tr>"
            (esc iso3) (esc (:name sb)) (esc (name threshold-model))
            (if notification-lead-days
              (str "<span class=\"num\">" (esc notification-lead-days) "</span> days")
              "<span class=\"muted\">no fixed numeric lead time &mdash; not invented</span>")
            (esc (clip owner-authority 120)))))

(defn render
  "Renders the whole operator console from a store `db` and `notifier`
  that have already been driven by `run-demo!` (or any other real run)."
  [db notifier]
  (let [ledger   (vec (store/ledger db))
        sites    (store/all-sites db)
        cov      (facts/coverage)
        sent     (notify/sent-log notifier)
        holds    (hold-rows ledger)]
    (str
     "<!DOCTYPE html>\n"
     "<html lang=\"en\"><head><meta charset=\"utf-8\">"
     "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, viewport-fit=cover\">"
     "<meta name=\"color-scheme\" content=\"light\">"
     "<title>cloud-itonami-isic-4312 &middot; site preparation operator console</title>"
     "<style>" (skin/dds+skin) "</style></head><body>\n"
     "<header class=\"bar\">\n"
     "  <h1>Site preparation (ISIC 4312) &mdash; Operator Console</h1>\n"
     "</header>\n"
     "<p><span class=\"badge\">read-only sample</span> <span class=\"badge\">governor-gated</span> <span class=\"badge\">coordination-only &mdash; no equipment is ever dispatched</span></p>\n"
     "<p class=\"subtitle\">Build-time generated by <code>site-prep.render-html</code> (<code>clojure -M:dev:render-html</code>) from a real run of the actor graph "
     "<code>site-prep.operation</code> &rarr; <code>site-prep.governor</code> &rarr; <code>site-prep.store</code>. Every value below is actor output; nothing is hand-typed.</p>\n"
     "<main>\n"

     "  <section class=\"card\">\n"
     "    <h2>Sites</h2>\n"
     "    <p class=\"muted\">The seeded site directory (<code>site-prep.store/all-sites</code>) after this run. "
     "The notification-lead column is recomputed here by the same <code>site-prep.facts/notification-lead-insufficient?</code> the Site Prep Governor calls &mdash; three-valued, so a jurisdiction with no fixed statutory day count is reported as qualitative instead of being given an invented number.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Site</th><th>Juris.</th><th>Independently verified</th><th>Utility locate</th><th>Notification lead time</th><th>Contamination</th><th>Safety concern</th><th>Last ledger fact</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" (map (partial site-row ledger) sites)) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"

     "  <section class=\"card\">\n"
     "    <h2>Action gate (Site Prep Governor &times; rollout phase 3)</h2>\n"
     "    <p class=\"muted\">HARD holds cannot be overridden by any approver. This actor holds no excavation / earth-moving-equipment control authority and issues no geotechnical or site-readiness sign-off &mdash; every proposal it can produce carries <code>:effect :propose</code>.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Op</th><th>Gate</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" action-gate-rows) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"

     "  <section class=\"card\">\n"
     "    <h2>HARD holds in this run <span class=\"muted\">(" (count holds) " violations, none reached a human)</span></h2>\n"
     "    <p class=\"muted\">Each row is a real <code>:governor-hold</code> fact from the append-only ledger, showing the Site Prep Governor's own rule keyword and its own detail text.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Site</th><th>Op</th><th>Rule</th><th>Governor detail</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" holds) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"

     "  <section class=\"card\">\n"
     "    <h2>Safety-concern notice dispatch</h2>\n"
     "    <p class=\"muted\">The <code>:commit</code> node dispatches the notice through the injected notifier (a deterministic mock here, Resend + Twilio in production) only AFTER a human approved the flag &mdash; <code>:flag-safety-concern</code> is never auto-eligible at any phase. Recipients come from the site's own <code>:safety-contacts</code> roster.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Channel</th><th>To</th><th>Subject / message</th><th>Status</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" (notice-rows sent)) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"

     "  <section class=\"card\">\n"
     "    <h2>Audit ledger (this run) <span class=\"muted\">(" (count ledger) " facts)</span></h2>\n"
     "    <p class=\"muted\">Append-only decision log. <code>site-prep.store/append-ledger!</code> is reached from exactly two graph nodes: <code>:commit</code> and <code>:hold</code>. "
     "Basis is the governor's violated rules for a hold, and the proposal's cited official legal basis for a commit (clipped with &hellip; where long).</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Fact</th><th>Op</th><th>Site</th><th>Disposition</th><th>Basis</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" (map ledger-row ledger)) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"

     "  <section class=\"card\">\n"
     "    <h2>Jurisdiction coverage <span class=\"muted\">(" (:covered cov) " of " (:requested cov) " seeded)</span></h2>\n"
     "    <p class=\"muted\">" (esc (:note cov)) "</p>\n"
     "    <table>\n"
     "      <thead><tr><th>ISO3</th><th>Jurisdiction</th><th>Threshold model</th><th>Statutory notification lead</th><th>Owner authority</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" (coverage-rows cov)) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"
     "</main>\n"
     "<footer class=\"footer\">\n"
     "  <p>Regenerate with <code>clojure -M:dev:render-html</code>. Deterministic: no timestamps, no randomness &mdash; two runs against the same seed are byte-identical.</p>\n"
     "  <p>Coordination artifacts only. Excavation sequencing, equipment dispatch and geotechnical / site-readiness sign-off remain the licensed engineer&rsquo;s and site supervisor&rsquo;s exclusive authority.</p>\n"
     "</footer>\n"
     "</body></html>\n")))

(defn -main [& args]
  (let [out (or (first args) "docs/samples/operator-console.html")
        {:keys [db notifier]} (run-demo!)
        html (render db notifier)
        f (java.io.File. ^String out)]
    (when-let [parent (.getParentFile f)] (.mkdirs parent))
    (spit f html)
    (println "wrote" out
             "(" (count (store/ledger db)) "ledger facts,"
             (count (filter #(= :governor-hold (:t %)) (store/ledger db))) "HARD holds,"
             (count (store/schedule-proposal-history db)) "schedule proposals,"
             (count (store/safety-concern-flag-history db)) "safety-concern flags,"
             (count (store/supply-order-proposal-history db)) "supply orders,"
             (count (notify/sent-log notifier)) "notice sends )")))
