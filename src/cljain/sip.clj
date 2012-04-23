(ns ^{:doc "place doc string here"
      :author "ruiyun"}
  cljain.sip
  (:use cljain.core
        cljain.util
        [clojure.string :only [upper-case]])
  (:require [cljain.header :as header]
            [cljain.address :as addr]
            [cljain.message :as msg])
  (:import [java.util Properties]
           [javax.sip SipStack SipProvider ClientTransaction ServerTransaction Dialog ListeningPoint]
           [javax.sip.address URI Address]
           [javax.sip.message Request]
           [javax.sip.header FromHeader]))

(defn start!
  "place doc string here"
  {:added "0.2.0"}
  [ctx-name host & options]
  {:pre [(even? (count options))]}
  (let [opts (apply hash-map options)
        ctx (create-ctx ctx-name host opts)]
    (.start (:stack ctx))
    (store-ctx! ctx-name ctx)))

(defn stop!
  "place doc string here"
  {:added "0.2.0"}
  [ctx-name]
  (let [ctx (pick-ctx! ctx-name)]
    (and ctx (.stop (:stack ctx)))))

(defn running?
  "place doc string here"
  {:added "0.2.0"}
  [ctx-name]
  (not (nil? (get-ctx ctx-name))))

(defn reset-options!
  "特别注意，当部分选项设置失败时，已经成功设置的选项不会回退."
  {:added "0.2.0"}
  [ctx-name key val & kvs]
  {:pre [(contains? #{:user :domain :display-name :request-handler} key)]}
  (alter-ctx-content! ctx-name key #(first %2) val)
  (when kvs (recur ctx-name (first kvs) (second kvs) (nnext kvs))))

(defn listening-point
  "place doc string here"
  {:added "0.2.0"}
  [ctx-name]
  (get-ctx-content ctx-name :listening-point))

(defn client-trans
  "place doc string here"
  {:added "0.2.0"}
  [ctx-name request]
  (let [provider (get-ctx-content ctx-name :provider)]
    (.getNewClientTransaction provider request)))

(defn server-trans
  "place doc string here"
  {:added "0.2.0"}
  [ctx-name request]
  (let [provider (get-ctx-content ctx-name :provider)]
    (.getNewServerTransaction provider request)))
;
;(defn send-request
;  "place doc string here"
;  {:added "0.2.0"}
;  [trans & evt-handlers]
;  {:pre [(even? (count evt-handlers))]}
;  (let [trans-id (.getBranchId trans)
;        ctx-name (.getStackName (.getSIPStack trans))
;        handlers (apply hash-map evt-handlers)]
;    (.sendRequest trans)
;    (alter-ctx-content! ctx-name :transactions
;      #(assoc-in %1 (first %2) (second %2)) [trans-id :evt-handler] handlers)))
;
;(defn send-response
;  "place doc string here"
;  {:added "0.2.0"}
;  [trans rsp]
;  (.sendResponse trans rsp))

(def ^{:doc "Before call any function expect 'provider' in the sip namespace,
            please binding *sip-provider* with the current provider object first."
       :added "0.2.0"
       :private false
       :dynamic true}
  *sip-provider*)

(def ^{:doc "place doc string here"
       :added "0.2.0"
       :private true}
  account-map (atom {}))

(defn account
  "place doc string here"
  {:added "0.2.0"}
  []
  (get @account-map (.. *sip-provider* (getSipStack) (getStackName))))

(defn provider
  "place doc string here"
  {:added "0.2.0"}
  [name host & options]
  {:pre [(even? (count options))
         (check-optional options :port > 0)
         (check-optional options :transport :by upper-case in? ["TCP" "UDP"])
         (check-optional options :user string?)
         (check-optional options :domain string?)
         (check-optional options :display-name string?)
         (check-optional options :request-handler fn?)
         (check-optional options :out-proxy legal-proxy-address?)]}
  (let [{:keys [port transport user domain display-name request-handler out-proxy]} (apply hash-map options)
        properties      (doto (Properties.)
                          (.setProperty "javax.sip.STACK_NAME" name)
                          (#(when out-proxy (.setProperty % "javax.sip.OUTBOUND_PROXY" out-proxy))))
        stack           (.createSipStack sip-factory properties)
        port            (or port 5060)
        transport       (or transport "UDP")
        listening-point (.createListeningPoint stack host port transport)
        provider        (doto (.createSipProvider stack listening-point)
                          (.addSipListener (create-listener name)))]
    (swap! account-map assoc name {:user user, :domain domain, :display-name display-name})
    provider))

(defn send-request
  "place doc string here"
  {:added "0.2.0"}
  [message & options]
  {:pre [(even? (count options))
         (check-optional options :pack #(and (map? %)
                                          (contains? % :content-type)
                                          (contains? % :content-sub-type)
                                          (contains? % :content-length)
                                          (contains? % :content)))
         (check-required options :to #(instance? Address %))
         (check-optional options :from #(instance? Address %))
         (check-optional options :use-endpoint :by upper-case in? ["UDP" "TCP"])
         (check-optional options :in #(or (in? % [:new-transaction :new-dialog]) (instance? Dialog %)))
         (check-optional options :on-response fn?)
         (check-optional options :on-timeout fn?)
         (check-optional options :on-transaction-terminated fn?)]}
  (let [{:keys [pack to from use-endpoint in on-response on-timeout on-transaction-terminated]} (apply hash-map options)
        {:keys [user domain display-name]} (account)
        method          (str message)
        from-header     (if (nil? from)
                          (header/from (addr/address (addr/sip-uri domain :user user) display-name) (header/gen-tag))
                          (header/from from (header/gen-tag)))
        to-header       (if (= method Request/REGISTER) ; for REGISTER, To header's uri should equal the From header's.
                          (header/to (.getAddress from-header) nil) ; out of dialog, do not need tag
                          (header/to to nil)) ; TODO i don't know if need to pick the tag from exist dialog, try later.
        contact-header  (let [endpoint  (if (nil? use-endpoint)
                                          (.getListeningPoint *sip-provider*)
                                          (.getListeningPoint *sip-provider* use-endpoint))]
                          (header/contact (addr/sip-uri (.getIPAddress endpoint) :port (.getPort endpoint)
                                            :transport (.getTransport endpoint) :user user)))
        call-id-header  (.getCallId *sip-provider*)
        request         (doto (msg/request method (.getURI to) from-header call-id-header to-header contact-header))]
    (.sendRequest *sip-provider* )))

;; 重构思路
;; (binding [*sip-provider*]
;;   (send ccdp (xml...) :to (uri "192.168.1.2") :by "MESSAGE" :at (uri ("192.168.1.3") :on-response rsp-handler
;       :on-timeout timeout-handler))
;; ccdp is a function, return {:content-type "TEXT" :content-sub-type "UDP" :content-length 123 :content byte[]}
