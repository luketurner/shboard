(ns shboard.ui.screen
 (:require [shboard.apis.blessed :as blessed]
           [shboard.apis.node.process :refer [exit]]))

(def screen
 "Holds a reference to the primary screen for the app."
 (atom nil))

(defn create-screen!
 "Creates a new screen. This causes Blessed to paint over the terminal window,
  so it should only be called if the application is entering an interactive mode."
 []
 (reset! screen (blessed/screen {:title      "shboard"
                                 :keymap-el  {["S-q" "escape" "C-c"] #(exit 0)}
                                 :options    {:sendFocus true
                                              :smartCSR true}})))
