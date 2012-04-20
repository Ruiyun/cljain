(ns ^{:doc "place doc string here"
      :author "ruiyun"}
  cljain.header
  (:refer-clojure :exclude [replace reverse])
  (:use cljain.core
        cljain.address
        clojure.string)
  (:import [javax.sip SipFactory SipProvider]
           [javax.sip.header HeaderFactory CallIdHeader]))

(def ^{:doc "place doc string here"
       :added "0.2.0"
       :private true}
  factory (.createHeaderFactory sip-factory))

(defn- unique
  "place doc string here"
  {:added "0.2.0"}
  []
  (str (java.util.UUID/randomUUID)))

(defn make-tag
  "place doc string here"
  {:added "0.2.0"}
  []
  (unique))

(defn make-branch
  "place doc string here"
  {:added "0.2.0"}
  []
  (str "z9hG4bK" (unique)))

(defmacro defheader
  "place doc string here"
  {:added "0.2.0"}
  [name & args]
  (let [cls-name (replace (capitalize name) #"-[a-z]" #(upper-case (subs %1 1)))
        cls-name (str cls-name "Header")
        name (symbol name)
        method (symbol (str "create" cls-name))]
    `(def ~name (partial (memfn ~method ~@args) ~factory))))

(defheader c-seq number method)
(defheader from address tag)
(defheader to address tag)
(defheader via host port transport branch)
(defheader max-forwards number)
(defheader contact address)
(defheader expires seconds)
(defheader call-id id)
(defheader allow method)
(defheader accept content-type sub-type)
(defheader content-type type sub-type)

(defn extension
  "创建标准中未定义的扩展头字段."
  {:added "0.2.0"}
  [name value]
  (.createHeader factory name value))

(defn call-id-by-ctx
  "place doc string here"
  {:added "0.2.0"}
  [ctx-name]
  {:post [(instance? CallIdHeader %)]}
  (let [p (:provider (get-ctx ctx-name))]
    (.getNewCallId p)))

(defn from-by-ctx
  "place doc string here"
  {:added "0.2.0"}
  [ctx-name]
  (let [ctx (get-ctx ctx-name)]
    (from (address (uri (:domain ctx) :user (:user ctx)) (:display-name ctx))
      (make-tag))))