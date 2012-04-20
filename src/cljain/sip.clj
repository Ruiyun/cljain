(ns ^{:doc "place doc string here"
      :author "ruiyun"}
  cljain.sip
  (:use cljain.core
        cljain.util)
  (:import [javax.sip SipStack SipProvider ClientTransaction
            ServerTransaction]))

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

(defn send-request
  "place doc string here"
  {:added "0.2.0"}
  [trans & evt-handlers]
  {:pre [(even? (count evt-handlers))]}
  (let [trans-id (.getBranchId trans)
        ctx-name (.getStackName (.getSIPStack trans))
        handlers (apply hash-map evt-handlers)]
    (.sendRequest trans)
    (alter-ctx-content! ctx-name :transactions
      #(assoc-in %1 (first %2) (second %2)) [trans-id :evt-handler] handlers)))

(defn send-response
  "place doc string here"
  {:added "0.2.0"}
  [trans rsp]
  (.sendResponse trans rsp))
