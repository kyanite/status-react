(ns status-im.ui.screens.wallet.events
  (:require [re-frame.core :as re-frame :refer [dispatch reg-fx]]
            [status-im.utils.handlers :as handlers]
            [status-im.utils.prices :as prices]
            [status-im.utils.scheduler :as scheduler]))

(defn redo-later-fn [event]
  (fn []
    (scheduler/execute-later #(dispatch event) (scheduler/s->ms 10))))

(defn get-balance [{:keys [web3 account-id on-success on-error on-unavailable]}]
  (if web3
    (.getBalance
     (.-eth web3)
     account-id
     (fn [err resp]
       (if-not error
         (on-success resp)
         (on-error err))))
    on-unavailable))

;; FX

(reg-fx
 :get-balance
 (fn [[web3 account-id]]
   (get-balance
    {:web3           web3
     :account-id     account-id
     :on-success     (fn [res] (dispatch [:set :wallet {:balance (str res)}]))
     :on-error       (fn [err] (.log js/console "Unable to get balance: " err))
     :on-unavailable (redo-later-fn [:wallet-init])})))

;; TODO(oskarth): Generalize so it fetches relevant from and to assets
(reg-fx
 :get-prices
 (fn [_]
   (prices/get-prices
    "ETH"
    "USD"
    (fn [resp]  (dispatch [:set :prices resp]))
    (fn [error] (println "Error fetching prices:" error)))))

;; Handlers

;; TODO(oskarth): At some point we want to get list of relevant assets to get prices for
(handlers/register-handler-fx
 :load-prices
 (fn [{{:keys [wallet] :as db} :db} [_ a]]
   {:db          db
    :get-prices  nil}))

(handlers/register-handler-fx
 :wallet-init
 (fn [{{:keys [web3 current-account-id] :as db} :db} [_ a]]
   {:db          db
    :get-balance [web3 current-account-id]
    :dispatch [:load-prices]}))
