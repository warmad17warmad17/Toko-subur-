package com.example.ui.components

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * An input field designed for POS barcode/QR scanners that suppresses the
 * on-screen software keyboard (IME) when touched or active, while retaining
 * full hardware keyboard and scanner input capabilities.
 * Automatically clears scanned text once a product is successfully added.
 */
@Composable
fun NoKeyboardScannerInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: (String) -> Unit,
    clearTrigger: Long = 0L,
    modifier: Modifier = Modifier,
    placeholder: String = "Scan / Ketik Kode QR Produk...",
    onClear: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    var editTextRef by remember { mutableStateOf<EditText?>(null) }

    LaunchedEffect(clearTrigger) {
        if (clearTrigger > 0L) {
            editTextRef?.setText("")
            editTextRef?.requestFocus()
        }
    }

    val borderColor = if (isFocused) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }
    val borderWidth = if (isFocused) 2.dp else 1.dp
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val hintColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f).toArgb()

    Surface(
        modifier = modifier
            .height(56.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                editTextRef?.requestFocus()
                val imm = editTextRef?.context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(editTextRef?.windowToken, 0)
            }
            .testTag("qr_scanner_input_field"),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(borderWidth, borderColor),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.QrCodeScanner,
                contentDescription = "Scan QR",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    factory = { context ->
                        EditText(context).apply {
                            // Disable on-screen virtual keyboard at the Android OS TextView level
                            showSoftInputOnFocus = false
                            background = null
                            setPadding(0, 0, 0, 0)
                            hint = placeholder
                            setHintTextColor(hintColor)
                            setTextColor(textColor)
                            textSize = 15f
                            isSingleLine = true
                            maxLines = 1
                            imeOptions = EditorInfo.IME_ACTION_DONE
                            isFocusable = true
                            isFocusableInTouchMode = true

                            // Intercept touch to guarantee soft keyboard never opens upon touching
                            setOnTouchListener { view, _ ->
                                view.requestFocus()
                                val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                imm?.hideSoftInputFromWindow(view.windowToken, 0)
                                false
                            }

                            setOnFocusChangeListener { view, hasFocus ->
                                isFocused = hasFocus
                                if (hasFocus) {
                                    val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                    imm?.hideSoftInputFromWindow(view.windowToken, 0)
                                }
                            }

                            addTextChangedListener(object : TextWatcher {
                                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                    val newText = s?.toString() ?: ""
                                    if (newText != value) {
                                        onValueChange(newText)
                                    }
                                }
                                override fun afterTextChanged(s: Editable?) {}
                            })

                            setOnEditorActionListener { v, actionId, event ->
                                if (actionId == EditorInfo.IME_ACTION_DONE ||
                                    (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP)
                                ) {
                                    val code = (v as? EditText)?.text?.toString() ?: value
                                    onDone(code)
                                    true
                                } else {
                                    false
                                }
                            }

                            setOnKeyListener { v, keyCode, event ->
                                if (event.action == KeyEvent.ACTION_UP &&
                                    (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)
                                ) {
                                    val code = (v as? EditText)?.text?.toString() ?: value
                                    onDone(code)
                                    true
                                } else {
                                    false
                                }
                            }

                            editTextRef = this
                            post {
                                requestFocus()
                                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                imm?.hideSoftInputFromWindow(windowToken, 0)
                            }
                        }
                    },
                    update = { editText ->
                        editText.showSoftInputOnFocus = false
                        editText.setTextColor(textColor)
                        editText.setHintTextColor(hintColor)
                        if (value.isEmpty() && editText.text.isNotEmpty()) {
                            editText.setText("")
                        } else if (editText.text.toString() != value) {
                            editText.setText(value)
                            editText.setSelection(value.length)
                        }
                    }
                )
            }

            if (value.isNotBlank() || (editTextRef?.text?.isNotEmpty() == true)) {
                IconButton(
                    onClick = {
                        editTextRef?.setText("")
                        onClear()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Hapus",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
