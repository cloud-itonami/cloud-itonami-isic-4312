(ns site-prep.phase-test
  "The phase table as executable tests. The invariant this repo cannot
  regress on: `:schedule-site-operation`/`:flag-safety-concern` must
  NEVER be a member of any phase's `:auto` set. `:log-site-record` and
  `:order-supplies` ARE auto-eligible at phase 3 -- see `site-prep.phase`
  ns docstring 'Actuation' section."
  (:require [clojure.test :refer [deftest is testing]]
            [site-prep.phase :as phase]))

(deftest schedule-site-operation-never-auto-at-any-phase
  (testing "structural invariant: no phase auto-commits a site-operation schedule proposal"
    (doseq [[n {:keys [auto]}] phase/phases]
      (is (not (contains? auto :schedule-site-operation))
          (str "phase " n " must not auto-commit :schedule-site-operation")))))

(deftest flag-safety-concern-never-auto-at-any-phase
  (doseq [[n {:keys [auto]}] phase/phases]
    (is (not (contains? auto :flag-safety-concern))
        (str "phase " n " must not auto-commit :flag-safety-concern"))))

(deftest log-site-record-and-order-supplies-are-auto-eligible-at-phase-3
  (is (contains? (:auto (get phase/phases 3)) :log-site-record))
  (is (contains? (:auto (get phase/phases 3)) :order-supplies)))

(deftest write-ops-is-exactly-the-closed-four-op-allowlist
  (is (= #{:log-site-record :schedule-site-operation :flag-safety-concern :order-supplies}
         phase/write-ops)))

(deftest phase-0-is-fully-read-only
  (is (empty? (:writes (get phase/phases 0)))))

(deftest phase-1-only-allows-log-site-record
  (is (= #{:log-site-record} (:writes (get phase/phases 1)))))

(deftest phase-3-auto-set-is-exactly-log-site-record-and-order-supplies
  (is (= #{:log-site-record :order-supplies} (:auto (get phase/phases 3)))))

(deftest gate-hold-always-wins
  (is (= :hold (:disposition (phase/gate 3 {:op :log-site-record} :hold)))))

(deftest gate-escalates-a-clean-non-auto-write
  (is (= :escalate (:disposition (phase/gate 3 {:op :schedule-site-operation} :commit))))
  (is (= :escalate (:disposition (phase/gate 3 {:op :flag-safety-concern} :commit)))))

(deftest gate-auto-commits-log-site-record-when-clean-at-phase-3
  (is (= :commit (:disposition (phase/gate 3 {:op :log-site-record} :commit)))))

(deftest gate-auto-commits-order-supplies-when-clean-at-phase-3
  (is (= :commit (:disposition (phase/gate 3 {:op :order-supplies} :commit)))))

(deftest gate-holds-a-write-disabled-in-this-phase
  (is (= :hold (:disposition (phase/gate 1 {:op :flag-safety-concern} :commit))))
  (is (= :hold (:disposition (phase/gate 0 {:op :log-site-record} :commit)))))
