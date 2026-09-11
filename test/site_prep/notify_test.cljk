(ns site-prep.notify-test
  (:require [clojure.test :refer [deftest is]]
            [site-prep.notify :as notify]))

(deftest mock-notifier-records-mail-and-phone-sends
  (let [n (notify/mock-notifier)]
    (notify/-send-mail! n {:to "a@example.com" :subject "s" :body "b"})
    (notify/-send-phone-call! n {:to "+1555" :message "m"})
    (is (= [{:status :sent :channel :mail :to "a@example.com" :subject "s" :body "b"}
            {:status :sent :channel :phone :to "+1555" :message "m"}]
           (notify/sent-log n)))))

(deftest dispatch-safety-concern-notice-fans-out-to-every-contact-both-channels
  (let [n (notify/mock-notifier)
        contacts [{:name "Tanaka" :email "tanaka@example.com" :phone "+819000000001"}
                  {:name "Suzuki" :email "suzuki@example.com" :phone "+819000000002"}]
        result (notify/dispatch-safety-concern-notice! n contacts {:subject-line "CONCERN" :body "check now" :message "check now"})]
    (is (= 2 (count result)))
    (is (every? #(= :sent (get-in % [:mail :status])) result))
    (is (every? #(= :sent (get-in % [:phone :status])) result))
    (is (= 4 (count (notify/sent-log n))) "2 contacts x 2 channels")))

(deftest dispatch-safety-concern-notice-isolates-one-contacts-failure-from-the-rest
  (let [failing-once (atom false)
        n (reify notify/Notifier
            (-send-mail! [_ {:keys [to]}]
              (if (and (not @failing-once) (= to "bad@example.com"))
                (do (reset! failing-once true) (throw (ex-info "boom" {})))
                {:status :sent :channel :mail :to to}))
            (-send-phone-call! [_ {:keys [to]}] {:status :sent :channel :phone :to to}))
        contacts [{:name "Bad" :email "bad@example.com" :phone "+1"}
                  {:name "Good" :email "good@example.com" :phone "+2"}]
        result (notify/dispatch-safety-concern-notice! n contacts {:subject-line "s" :body "b" :message "m"})]
    (is (= :failed (get-in (first result) [:mail :status])))
    (is (= :sent (get-in (second result) [:mail :status])) "one contact's failure must not suppress the next contact's send")))

;; ----------------------------- real transports (stubbed http-fn, no real network) -----------------------------

(deftest resend-mail-notifier-posts-to-resend-with-stubbed-http-fn
  (let [captured (atom nil)
        stub-http-fn (fn [req] (reset! captured req) {:status 200 :body "{\"id\":\"abc123\"}"})
        n (notify/resend-mail-notifier "ops@example.com" {:http-fn stub-http-fn :token "test-token"})
        result (notify/-send-mail! n {:to "engineer@example.com" :subject "CONCERN" :body "check now"})]
    (is (= "https://api.resend.com/emails" (:url @captured)))
    (is (= "Bearer test-token" (get (:headers @captured) "Authorization")))
    (is (re-find #"engineer@example\.com" (:body @captured)))
    (is (= :sent (:status result)))
    (is (re-find #"abc123" (:response result)) "raw Resend response body is surfaced, not parsed (no JSON decoder dependency)")))

(deftest resend-mail-notifier-throws-on-non-2xx
  (let [stub-http-fn (fn [_] {:status 500 :body "{\"message\":\"fail\"}"})
        n (notify/resend-mail-notifier "ops@example.com" {:http-fn stub-http-fn :token "t"})]
    (is (thrown? Exception (notify/-send-mail! n {:to "e@example.com" :subject "s" :body "b"})))))

(deftest twilio-voice-notifier-places-call-with-stubbed-http-fn
  (let [captured (atom nil)
        stub-http-fn (fn [req] (reset! captured req) {:status 201 :body "{\"sid\":\"CA123\"}"})
        n (notify/twilio-voice-notifier "+15550001111" {:http-fn stub-http-fn :account-sid "AC123" :auth-token "tok"})
        result (notify/-send-phone-call! n {:to "+819000000001" :message "安全性懸念です"})]
    (is (re-find #"Accounts/AC123/Calls\.json" (:url @captured)))
    (is (re-find #"To=%2B819000000001" (:body @captured)))
    (is (re-find #"Say" (:body @captured)))
    (is (= :sent (:status result)))))

(deftest twilio-voice-notifier-throws-on-non-2xx
  (let [stub-http-fn (fn [_] {:status 400 :body "{\"message\":\"bad request\"}"})
        n (notify/twilio-voice-notifier "+1" {:http-fn stub-http-fn :account-sid "AC" :auth-token "t"})]
    (is (thrown? Exception (notify/-send-phone-call! n {:to "+1" :message "m"})))))

(deftest dual-notifier-composes-a-mail-only-and-a-phone-only-notifier
  (let [mail-log (atom [])
        phone-log (atom [])
        mail-only (reify notify/Notifier
                    (-send-mail! [_ m] (swap! mail-log conj m) {:status :sent :channel :mail :to (:to m)})
                    (-send-phone-call! [_ _] (throw (ex-info "mail-only" {}))))
        phone-only (reify notify/Notifier
                    (-send-mail! [_ _] (throw (ex-info "phone-only" {})))
                    (-send-phone-call! [_ m] (swap! phone-log conj m) {:status :sent :channel :phone :to (:to m)}))
        dual (notify/dual-notifier mail-only phone-only)]
    (notify/-send-mail! dual {:to "a@example.com" :subject "s" :body "b"})
    (notify/-send-phone-call! dual {:to "+1" :message "m"})
    (is (= 1 (count @mail-log)))
    (is (= 1 (count @phone-log)))))
