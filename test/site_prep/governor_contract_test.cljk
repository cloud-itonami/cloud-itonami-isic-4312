(ns site-prep.governor-contract-test
  "The governor contract as executable tests -- the site-preparation-
  coordination analog of `demolition.governor-contract-test`. The single
  invariant under test:

    Site Prep Advisor never schedules a site operation, files a
    safety-concern flag or a supply order the Site Prep Governor would
    reject; `:schedule-site-operation`/`:flag-safety-concern` NEVER
    auto-commit at any phase; `:log-site-record` (no direct capital/
    safety risk) MAY auto-commit when clean; `:order-supplies` MAY
    auto-commit when clean AND below the cost threshold; and every
    decision (commit OR hold) leaves exactly one ledger fact. Every
    committed record's `:effect` is `:propose` -- this actor never
    performs a real-world actuation."
  (:require [clojure.test :refer [deftest is testing]]
            [langgraph.graph :as g]
            [site-prep.store :as store]
            [site-prep.operation :as op]
            [site-prep.governor :as governor]
            [site-prep.phase :as phase]))

(defn- fresh []
  (let [db (store/seed-db)]
    [db (op/build db)]))

(def operator {:actor-id "op-1" :actor-role :site-supervisor :phase 3})

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

;; ----------------------------- :log-site-record -----------------------------

(deftest clean-log-site-record-auto-commits
  (let [[db actor] (fresh)
        res (exec-op actor "t1"
                  {:op :log-site-record :subject "site-1" :patch {:id "site-1" :contamination-detected? true}} operator)]
    (is (= :commit (get-in res [:state :disposition])))
    (is (true? (:contamination-detected? (store/site db "site-1"))) "SSoT actually updated")
    (is (= 1 (count (store/ledger db))))
    (is (= "JPN-SRL-000000" (get (first (store/site-record-log-history db)) "record_id")))))

(deftest log-site-record-can-resolve-a-safety-concern
  (let [[db actor] (fresh)]
    (exec-op actor "t1b" {:op :log-site-record :subject "site-6" :patch {:id "site-6" :safety-concern-unresolved? false}} operator)
    (is (false? (:safety-concern-unresolved? (store/site db "site-6"))))))

;; ----------------------------- :schedule-site-operation -----------------------------

(deftest schedule-site-operation-always-escalates-then-human-decides
  (testing "site-1 is fully clean (verified, utility-located, sufficient lead time, no unresolved concern) -- STILL always interrupts for human approval"
    (let [[db actor] (fresh)
          r1 (exec-op actor "t2" {:op :schedule-site-operation :subject "site-1" :window {}} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (let [r2 (approve! actor "t2")]
        (is (= :commit (get-in r2 [:state :disposition])))
        (is (= "JPN-SCH-000000" (get (first (store/schedule-proposal-history db)) "record_id")))
        (is (= 1 (count (store/schedule-proposal-history db))))))))

(deftest fabricated-jurisdiction-is-held
  (testing "site-2 (ATL, no spec-basis in site-prep.facts) -> HOLD, never reaches a human"
    (let [[db actor] (fresh)
          res (exec-op actor "t3" {:op :schedule-site-operation :subject "site-2" :window {}} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:no-legal-basis} (-> (store/ledger db) first :basis)))
      (is (empty? (store/schedule-proposal-history db)) "no schedule proposal recorded"))))

(deftest not-independently-verified-site-is-held
  (testing "site-3 has site-verified? false -> HARD hold, never reaches a human"
    (let [[db actor] (fresh)
          res (exec-op actor "t4" {:op :schedule-site-operation :subject "site-3" :window {}} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:site-not-verified} (-> (store/ledger db) first :basis)))
      (is (empty? (store/schedule-proposal-history db))))))

(deftest utility-locate-incomplete-is-held
  (testing "site-4 has utility-locate-completed? false -> HARD hold"
    (let [[db actor] (fresh)
          res (exec-op actor "t5" {:op :schedule-site-operation :subject "site-4" :window {}} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:utility-locate-incomplete} (-> (store/ledger db) first :basis))))))

(deftest notification-lead-time-insufficient-is-held
  (testing "site-5's notification-lead-days-actual (2) is below JPN's 7-day legal minimum -> HARD hold, independent of proposal confidence"
    (let [[db actor] (fresh)
          res (exec-op actor "t6" {:op :schedule-site-operation :subject "site-5" :window {}} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:notification-lead-time-insufficient} (-> (store/ledger db) first :basis))))))

(deftest unresolved-safety-concern-is-held
  (testing "site-6 has safety-concern-unresolved? true on file -> HARD hold, never reaches a human"
    (let [[db actor] (fresh)
          res (exec-op actor "t7" {:op :schedule-site-operation :subject "site-6" :window {}} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:unresolved-safety-concern} (-> (store/ledger db) first :basis)))
      (is (empty? (store/schedule-proposal-history db))))))

(deftest qualitative-jurisdiction-never-fabricates-a-numeric-hold-but-still-always-escalates
  (testing "site-8 (DEU/EU, qualitative) -- notification-lead-time-insufficient never fires there, but schedule still always needs a human"
    (let [[db actor] (fresh)
          r1 (exec-op actor "t8" {:op :schedule-site-operation :subject "site-8" :window {}} operator)]
      (is (= :interrupted (:status r1)) "no numeric bright-line to auto-clear on -- still always a human's call")
      (let [r2 (approve! actor "t8")]
        (is (= :commit (get-in r2 [:state :disposition])))
        (is (= "DEU-SCH-000000" (get (first (store/schedule-proposal-history db)) "record_id")))))))

(deftest usa-cross-jurisdiction-happy-path-always-escalates-then-approved
  (testing "site-7 (USA, quantitative 2-business-day minimum), lead=3 days, sufficient -- still always escalates"
    (let [[db actor] (fresh)
          r1 (exec-op actor "t9" {:op :schedule-site-operation :subject "site-7" :window {}} operator)]
      (is (= :interrupted (:status r1)))
      (let [r2 (approve! actor "t9")]
        (is (= :commit (get-in r2 [:state :disposition])))
        (is (= "USA-SCH-000000" (get (first (store/schedule-proposal-history db)) "record_id")))))))

;; ----------------------------- :flag-safety-concern -----------------------------

(deftest flag-safety-concern-always-escalates-even-when-clean
  (testing "site-1 is fully clean -- :flag-safety-concern STILL always interrupts, unconditionally"
    (let [[db actor] (fresh)
          r1 (exec-op actor "t10" {:op :flag-safety-concern :subject "site-1"
                                   :concern-type :excavation-collapse
                                   :concern-description "trench wall sloughing observed"} operator)]
      (is (= :interrupted (:status r1)))
      (let [r2 (approve! actor "t10")]
        (is (= :commit (get-in r2 [:state :disposition])))
        (is (true? (:safety-concern-unresolved? (store/site db "site-1"))))
        (is (some? (get (first (store/safety-concern-flag-history db)) "document")) "rendered notice document present")))))

(deftest flag-safety-concern-triggers-notification-only-after-approval
  (let [[_db actor] (fresh)
        r1 (exec-op actor "t11" {:op :flag-safety-concern :subject "site-1"
                                 :concern-type :buried-utility-strike :concern-description "suspected unmapped gas line"} operator)]
    (is (nil? (:notify-result (:state r1))) "no notify before human approval")
    (let [r2 (approve! actor "t11")
          notify-result (:notify-result (:state r2))]
      (is (= 2 (count notify-result)) "one result entry per site-1 safety-contact")
      (is (every? #(= :sent (get-in % [:mail :status])) notify-result))
      (is (every? #(= :sent (get-in % [:phone :status])) notify-result)))))

;; ----------------------------- :order-supplies -----------------------------

(deftest order-supplies-below-threshold-auto-commits
  (let [[db actor] (fresh)
        res (exec-op actor "t12" {:op :order-supplies :subject "site-1"
                                  :items ["silt-fencing"] :cost-usd 1200 :vendor "Local Earthworks Supply Co."} operator)]
    (is (= :commit (get-in res [:state :disposition])))
    (is (= 1 (count (store/supply-order-proposal-history db))))))

(deftest order-supplies-above-threshold-escalates
  (let [[db actor] (fresh)
        r1 (exec-op actor "t13" {:op :order-supplies :subject "site-1"
                                 :items ["excavator-rental"] :cost-usd 18000 :vendor "Heavy Equip Rentals"} operator)]
    (is (= :interrupted (:status r1)) "above cost threshold -- always a human's call")
    (let [r2 (approve! actor "t13")]
      (is (= :commit (get-in r2 [:state :disposition])))
      (is (= 1 (count (store/supply-order-proposal-history db)))))))

;; ----------------------------- closed op-allowlist -----------------------------

(deftest op-outside-the-closed-allowlist-is-held
  (testing "an op outside {:log-site-record :schedule-site-operation :flag-safety-concern :order-supplies} -> HARD hold, never reaches a human, regardless of what the advisor's default branch returns"
    (let [[db actor] (fresh)
          res (exec-op actor "t14" {:op :direct-equipment-command :subject "site-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:unknown-op} (-> (store/ledger db) first :basis))))))

;; ----------------------------- effect / forbidden-action-class (direct governor/check) -----------------------------
;; The mock advisor never produces these -- they exercise the governor's
;; defense-in-depth against a hypothetically compromised/malfunctioning
;; advisor, so they are tested directly against `governor/check` rather
;; than through the full actor (which can only ever see what the mock
;; advisor actually proposes).

(deftest effect-not-propose-is-a-permanent-hard-violation
  (let [db (store/seed-db)
        request {:op :log-site-record :subject "site-1"}
        proposal {:summary "s" :rationale "r" :cites ["x"] :effect :direct-write
                  :value {:id "site-1"} :stake nil :confidence 0.99}
        verdict (governor/check request {} proposal db)]
    (is (:hard? verdict))
    (is (some #{:effect-not-propose} (map :rule (:violations verdict))))
    (is (not (:ok? verdict)))))

(deftest forbidden-action-class-markers-are-permanent-hard-violations
  (doseq [marker [:equipment-control? :direct-actuation? :finalizes-geotechnical-signoff?]]
    (testing marker
      (let [db (store/seed-db)
            request {:op :schedule-site-operation :subject "site-1"}
            proposal {:summary "s" :rationale "r" :cites ["x"] :effect :propose
                      :value {marker true} :stake :schedule-site-operation :confidence 0.99}
            verdict (governor/check request {} proposal db)]
        (is (:hard? verdict))
        (is (some #{:forbidden-action-class} (map :rule (:violations verdict))))))))

;; ----------------------------- ledger discipline -----------------------------

(deftest every-decision-leaves-one-ledger-fact
  (testing "write-only-through-ledger: N operations -> N ledger facts"
    (let [[db actor] (fresh)]
      (exec-op actor "a" {:op :log-site-record :subject "site-1" :patch {:id "site-1" :contamination-detected? true}} operator)
      (exec-op actor "b" {:op :schedule-site-operation :subject "site-2" :window {}} operator)
      (is (= 2 (count (store/ledger db)))
          "one commit + one hold, both recorded"))))

(deftest approver-rejection-is-held-not-committed
  (let [[db actor] (fresh)
        r1 (exec-op actor "t15" {:op :schedule-site-operation :subject "site-1" :window {}} operator)]
    (is (= :interrupted (:status r1)))
    (let [r2 (g/run* actor {:approval {:status :rejected :by "op-1"}} {:thread-id "t15" :resume? true})]
      (is (= :hold (get-in r2 [:state :disposition])))
      (is (empty? (store/schedule-proposal-history db))))))

;; ----------------------------- phase structural invariants (belt-and-suspenders) -----------------------------

(deftest schedule-and-flag-never-auto-at-any-phase
  (testing "structural invariant: never auto-eligible, even when clean, at any phase"
    (doseq [op [:schedule-site-operation :flag-safety-concern]]
      (is (= :escalate (:disposition (phase/gate 3 {:op op} :commit)))
          (str op " must escalate to a human even when the governor is clean at phase 3")))))
