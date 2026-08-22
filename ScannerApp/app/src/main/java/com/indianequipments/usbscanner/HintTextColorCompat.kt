package com.indianequipments.usbscanner

import android.widget.EditText

/**
 * Compatibility property for the scanner UI.
 * Keeps the existing `hintTextColor = ...` UI code source-compatible
 * while delegating to Android's supported setter API.
 */
var EditText.hintTextColor: Int
    get() = currentHintTextColor
    set(value) { setHintTextColor(value) }
