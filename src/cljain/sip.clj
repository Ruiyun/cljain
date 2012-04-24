(ns ^{:doc "place doc string here"
      :author "ruiyun"}
  cljain.sip
  (:use cljain.util
        [clojure.string :only [upper-case]])
  (:require [cljain.core :as core]
            [cljain.header :as header]
            [cljain.address :as addr]
            [cljain.message :as msg]))

(def ^{:doc "Before call any function expect 'provider' in the sip namespace,
            please binding *sip-provider* with the current provider object first."
       :added "0.2.0"
       :dynamic true}
  *sip-provider*)

(def ^{:doc "place doc string here"
       :added "0.2.0"
       :dynamic true}
  account-map (atom {}))

(defn account
  "Get current bound account information."
  {:added "0.2.0"}
  []
  (get @account-map (core/stack-name *sip-provider*)))

(defn listening-point
  "Get the current bound listening ip, port and transport information."
  {:added "0.2.0"}
  ([] (core/listening-point *sip-provider*))
  ([transport] (core/listening-point *sip-provider* transport)))

(defn provider!
  "Create a new sip provider with a meaningful name and a local IP address.
  Becareful, the name must be unique to make a distinction between other provider."
  {:added "0.2.0"}
  [name ip & options]
  {:pre [(even? (count options))
         (check-optional options :port > 0)
         (check-optional options :transport :by upper-case in? ["TCP" "UDP"])
         (check-optional options :on-request fn?)
         (check-optional options :on-io-exception fn?)
         (check-optional options :outbound-proxy core/legal-proxy-address?)]}
  (let [{:keys [port transport on-request on-io-exception outbound-proxy]} (apply hash-map options)
        port      (or port 5060)
        transport (or transport "UDP")
        provider  (if (nil? outbound-proxy)
                    (core/sip-provider! name ip port transport)
                    (core/sip-provider! name ip port transport :outbound-proxy outbound-proxy))]
    (core/add-listener! provider on-request on-io-exception)
    provider))

(defn start!
  "Start to run with the current bound provider."
  {:added "0.2.0"}
  [& account-info]
  {:pre [(even? (count account-info))
         (check-optional account-info :user string?)
         (check-optional account-info :domain string?)
         (check-optional account-info :display-name string?)]}
  (let [{:keys [user domain display-name]} (apply hash-map account-info)
        stack-name (core/stack-name *sip-provider*)]
    (core/start! *sip-provider*)
    (swap! account-map assoc stack-name {:user user, :domain domain, :display-name display-name})))

(defn stop!
  "Stop to run with the current bound provider."
  {:added "0.2.0"}
  []
  (let [stack-name (core/stack-name *sip-provider*)]
    (swap! account-map dissoc stack-name)
    (core/stop! *sip-provider*)))

(defn running?
  "Check whether the current bound provider is running."
  {:added "0.2.0"}
  []
  (contains? account-map (core/stack-name *sip-provider*)))

(defn send-request!
  "place doc string here"
  {:added "0.2.0"}
  [message & options]
  {:pre [(even? (count options))
         (check-optional options :pack #(and (map? %)
                                          (contains? % :content-type)
                                          (contains? % :content-sub-type)
                                          (contains? % :content-length)
                                          (contains? % :content)))
         (check-required options :to addr/address?)
         (check-optional options :from addr/address?)
         (check-optional options :use-endpoint :by upper-case in? ["UDP" "TCP"])
         (check-optional options :in #(or (in? % [:new-transaction :new-dialog]) (core/dialog? %)))
         (check-optional options :more-headers vector?)
         (check-optional options :on-response fn?)
         (check-optional options :on-timeout fn?)
         (check-optional options :on-transaction-terminated fn?)]}
  (let [{:keys [pack to from use-endpoint in more-headers
                on-response on-timeout on-transaction-terminated]} (apply hash-map options)
        {:keys [user domain display-name]} (account)
        {:keys [ip port transport]} (if (nil? use-endpoint)
                                      (listening-point)
                                      (listening-point use-endpoint))
        domain          (or domain ip)
        method          (upper-case (name message))
        request-uri     (addr/uri-from-address to)
        from-header     (if (nil? from)
                          (header/from (addr/address (addr/sip-uri domain :user user) display-name) (header/gen-tag))
                          (header/from from (header/gen-tag)))
        to-header       (if (= method "REGISTER") ; for REGISTER, To header's uri should equal the From header's.
                          (header/to (header/get-address from-header) nil) ; out of dialog, do not need tag
                          (header/to to nil)) ; TODO i don't know if need to pick the tag from exist dialog, try later.
        contact-header  (header/contact (addr/address (addr/sip-uri ip :port port :transport transport :user user)))
        call-id-header  (core/gen-call-id-header *sip-provider*)
        via-header      (header/via ip port transport (header/gen-branch))
        request         (msg/request method request-uri from-header call-id-header to-header via-header contact-header)]
    (doseq [header more-headers]
      (msg/add-header! request header))
    (core/send-request! *sip-provider* request)
    request))

;; 重构思路
;; (binding [*sip-provider*]
;;   (send ccdp (xml...) :to (uri "192.168.1.2") :by "MESSAGE" :at (uri ("192.168.1.3") :on-response rsp-handler
;       :on-timeout timeout-handler))
;; ccdp is a function, return {:content-type "TEXT" :content-sub-type "UDP" :content-length 123 :content byte[]}
