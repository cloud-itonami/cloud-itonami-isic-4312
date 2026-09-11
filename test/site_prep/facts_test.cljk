(ns site-prep.facts-test
  (:require [clojure.test :refer [deftest is]]
            [site-prep.facts :as facts]))

(deftest jpn-has-a-spec-basis
  (is (some? (facts/spec-basis "JPN")))
  (is (string? (:utility-locate-provenance (facts/spec-basis "JPN"))))
  (is (= :quantitative (:threshold-model (facts/spec-basis "JPN"))))
  (is (= 7 (:notification-lead-days (facts/spec-basis "JPN")))))

(deftest usa-has-a-spec-basis-with-a-different-numeric-lead-time
  (is (= :quantitative (:threshold-model (facts/spec-basis "USA"))))
  (is (= 2 (:notification-lead-days (facts/spec-basis "USA")))))

(deftest deu-is-honestly-qualitative-not-fabricated
  (is (= :qualitative (:threshold-model (facts/spec-basis "DEU"))))
  (is (nil? (:notification-lead-days (facts/spec-basis "DEU")))))

(deftest unknown-jurisdiction-has-no-fabricated-spec-basis
  (is (nil? (facts/spec-basis "ATL"))))

(deftest coverage-never-reports-a-missing-jurisdiction-as-covered
  (let [report (facts/coverage ["JPN" "ATL" "USA"])]
    (is (= 2 (:covered report)))
    (is (= ["ATL"] (:missing-jurisdictions report)))
    (is (= ["JPN" "USA"] (:covered-jurisdictions report)))))

;; ----------------------------- notification-lead-insufficient? -----------------------------

(deftest jpn-lead-time-is-a-real-numeric-recheck
  (is (true? (facts/notification-lead-insufficient? "JPN" {:notification-lead-days-actual 2})))
  (is (false? (facts/notification-lead-insufficient? "JPN" {:notification-lead-days-actual 7})))
  (is (false? (facts/notification-lead-insufficient? "JPN" {:notification-lead-days-actual 10}))))

(deftest usa-lead-time-uses-its-own-different-numeric-minimum
  (is (true? (facts/notification-lead-insufficient? "USA" {:notification-lead-days-actual 1})))
  (is (false? (facts/notification-lead-insufficient? "USA" {:notification-lead-days-actual 2})))
  (is (false? (facts/notification-lead-insufficient? "USA" {:notification-lead-days-actual 3}))))

(deftest deu-never-gets-a-fabricated-true-false
  (is (= :qualitative (facts/notification-lead-insufficient? "DEU" {:notification-lead-days-actual 100})))
  (is (= :qualitative (facts/notification-lead-insufficient? "DEU" {:notification-lead-days-actual 0}))))

(deftest unknown-jurisdiction-returns-nil-not-a-guess
  (is (nil? (facts/notification-lead-insufficient? "ATL" {:notification-lead-days-actual 100}))))

(deftest non-numeric-actual-never-fires-a-quantitative-hold
  (is (false? (facts/notification-lead-insufficient? "JPN" {:notification-lead-days-actual nil}))))

;; ----------------------------- catalog citation honesty -----------------------------

(deftest jpn-cites-real-utility-locate-and-notification-law
  (let [sb (facts/spec-basis "JPN")]
    (is (re-find #"労働安全衛生規則" (:utility-locate-basis sb)))
    (is (re-find #"laws\.e-gov\.go\.jp" (:utility-locate-provenance sb)))
    (is (re-find #"騒音規制法" (:excavation-notification-basis sb)))
    (is (re-find #"laws\.e-gov\.go\.jp" (:excavation-notification-provenance sb)))))

(deftest usa-cites-real-osha-and-811-onecall-basis
  (let [sb (facts/spec-basis "USA")]
    (is (re-find #"1926\.651" (:utility-locate-basis sb)))
    (is (re-find #"osha\.gov" (:utility-locate-provenance sb)))
    (is (re-find #"811" (:excavation-notification-basis sb)))
    (is (re-find #"811beforeyoudig\.com" (:excavation-notification-provenance sb)))
    (is (re-find #"State-law convention" (:notification-note sb)) "honestly labeled a state-law convention, not a single federal statute")))

(deftest deu-cites-real-german-din-standard-and-duty-of-care
  (let [sb (facts/spec-basis "DEU")]
    (is (re-find #"DIN 4124" (:utility-locate-basis sb)))
    (is (re-find #"dinmedia\.de" (:utility-locate-provenance sb)))
    (is (re-find #"Verkehrssicherungspflicht|Leitungsauskunft" (:utility-locate-basis sb)))))

(deftest uncovered-jurisdiction-has-no-fabricated-catalog-entry
  (is (nil? (facts/spec-basis "ATL"))))
