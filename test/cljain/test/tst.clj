;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns ^{:doc "The core Clojure language."
       :author "Rich Hickey"}
  cljain.test.sts)

(def ^{:private true :dynamic true}
  assert-valid-fdecl (fn [fdecl]))

(def
 ^{:private true}
 sigs
 (fn [fdecl]
   (assert-valid-fdecl fdecl)
   (let [asig 
         (fn [fdecl]
           (let [arglist (first fdecl)
                 ;elide implicit macro args
                 arglist (if (clojure.lang.Util/equals '&form (first arglist)) 
                           (clojure.lang.RT/subvec arglist 2 (clojure.lang.RT/count arglist))
                           arglist)
                 body (next fdecl)]
             (if (map? (first body))
               (if (next body)
                 (with-meta arglist (conj (if (meta arglist) (meta arglist) {}) (first body)))
                 arglist)
               arglist)))]
     (if (seq? (first fdecl))
       (loop [ret [] fdecls fdecl]
         (if fdecls
           (recur (conj ret (asig (first fdecls))) (next fdecls))
           (seq ret)))
       (list (asig fdecl))))))

(def 

 ^{:doc "Same as (def name (fn [params* ] exprs*)) or (def
    name (fn ([params* ] exprs*)+)) with any doc-string or attrs added
    to the var metadata. prepost-map defines a map with optional keys
    :pre and :post that contain collections of pre or post conditions."
   :arglists '([name doc-string? attr-map? [params*] prepost-map? body]
                [name doc-string? attr-map? ([params*] prepost-map? body)+ attr-map?])
   :added "1.0"}
 my-defn (fn my-defn [&form &env name & fdecl]
        (let [_ (prn "fdecl 1" fdecl)
              m (if (string? (first fdecl))
                  {:doc (first fdecl)}
                  {})
              _ (prn "m 1" m)
              fdecl (if (string? (first fdecl))
                      (next fdecl)
                      fdecl)
              _ (prn "fdecl 2" fdecl)
              m (if (map? (first fdecl))
                  (conj m (first fdecl))
                  m)
              _ (prn "m 2" m)
              fdecl (if (map? (first fdecl))
                      (next fdecl)
                      fdecl)
              _ (prn "fdecl 3" fdecl)
              fdecl (if (vector? (first fdecl))
                      (list fdecl)
                      fdecl)
              _ (prn "fdecl 4" fdecl)
              m (if (map? (last fdecl))
                  (conj m (last fdecl))
                  m)
              _ (prn "m 3" m)
              fdecl (if (map? (last fdecl))
                      (butlast fdecl)
                      fdecl)
              _ (prn "fdecl 5" fdecl)
              m (conj {:arglists (list 'quote (sigs fdecl))} m)
              _ (prn "m 4" m)
              m (let [inline (:inline m)
                      ifn (first inline)
                      iname (second inline)]
                  ;; same as: (if (and (= 'fn ifn) (not (symbol? iname))) ...)
                  (if (if (clojure.lang.Util/equiv 'fn ifn)
                        (if (instance? clojure.lang.Symbol iname) false true))
                    ;; inserts the same fn name to the inline fn if it does not have one
                    (assoc m :inline (cons ifn (cons (clojure.lang.Symbol/intern (.concat (.getName ^clojure.lang.Symbol name) "__inliner"))
                                                     (next inline))))
                    m))
              _ (prn "m 5" m)
              m (conj (if (meta name) (meta name) {}) m)
              _ (prn "m 6" m)]
          (list 'def (with-meta name m)
                ;;todo - restore propagation of fn name
                ;;must figure out how to convey primitive hints to self calls first
                (cons `fn fdecl) ))))

(. (var my-defn) (setMacro))
