/**
 * Copyright (c) 2022 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */
package com.qualcomm.qti.dcf.client

import android.content.Context
import android.util.JsonReader
import android.util.Log
import android.widget.Toast
import java.io.File

import com.qualcomm.qti.dcf.nearbyclient.R

// credential related infos
private const val INDEX_SECRET_ID = 0
private const val INDEX_AUTHENTICITY_KEY = 1
private const val INDEX_PUBLIC_KEY = 2
private const val INDEX_ENCRYPTED_METADATA_BYTES = 3
private const val INDEX_METADATA_ENCRYPTION_KEY_TAG = 4
private const val INDEX_METADATA_ENCRYPTION_KEY = 5
private const val INDEX_SALT = 6

private const val TAG_SECRET_ID = "secret_id"
private const val TAG_AUTHENTICITY_KEY = "authenticity_key"
private const val TAG_PUBLIC_KEY = "public_key"
private const val TAG_ENCRYPTED_METADATA_BYTES = "encrypted_metadata_bytes"
private const val TAG_METADATA_ENCRYPTION_KEY_TAG = "metadata_encryption_key_tag"
private const val TAG_METADATA_ENCRYPTION_KEY = "metadata_encryption_key"
private const val TAG_SALT = "salt"

private const val CREDENTIAL_FILE_NAME = "credentials.json"
private const val DELIMITERS: String = ", "

private val credentialElementsMap = mutableMapOf<Int, ByteArray>()

var credentialsValid: Boolean = false

fun readCredentialInfo(context: Context): Boolean {

    val credentialsFile = File(context.getExternalFilesDir(null)?.absolutePath
            + File.separator + CREDENTIAL_FILE_NAME)
    Log.d(TAG, "readCredentialInfo: credentials file path: ${credentialsFile.absolutePath}")

    if (!credentialsFile.exists()) {
        Toast.makeText(context, context.getString(R.string.toast_error_credential_file_not_found),
            Toast.LENGTH_LONG).show()
        credentialsValid = false
        Log.i(TAG, "load credentials failed.")
        return false
    }

    JsonReader(credentialsFile.reader()).use {
        it.beginObject()
        while (it.hasNext()) {
            when(it.nextName()) {
                TAG_SECRET_ID -> credentialElementsMap[INDEX_SECRET_ID] =
                    parseStringToByteArray(it.nextString())
                TAG_AUTHENTICITY_KEY -> credentialElementsMap[INDEX_AUTHENTICITY_KEY] =
                    parseStringToByteArray(it.nextString())
                TAG_PUBLIC_KEY -> credentialElementsMap[INDEX_PUBLIC_KEY] =
                    parseStringToByteArray(it.nextString())
                TAG_ENCRYPTED_METADATA_BYTES ->
                    credentialElementsMap[INDEX_ENCRYPTED_METADATA_BYTES] =
                        parseStringToByteArray(it.nextString())
                TAG_METADATA_ENCRYPTION_KEY_TAG ->
                    credentialElementsMap[INDEX_METADATA_ENCRYPTION_KEY_TAG] =
                        parseStringToByteArray(it.nextString())
                TAG_METADATA_ENCRYPTION_KEY ->
                    credentialElementsMap[INDEX_METADATA_ENCRYPTION_KEY] =
                        parseStringToByteArray(it.nextString())
                TAG_SALT -> credentialElementsMap[INDEX_SALT] =
                    parseStringToByteArray(it.nextString())
            }
        }
        it.endObject()
    }

    credentialsValid = credentialElementsMap.size == INDEX_SALT + 1
    Log.i(TAG, "load credentials ${if (credentialsValid) "success" else "failed"}.")
    if (!credentialsValid) {
        Toast.makeText(context, context.getString(R.string.toast_error_credential_file_error),
            Toast.LENGTH_LONG).show()
    }
    return credentialsValid
}

fun getSecretId() = credentialElementsMap[INDEX_SECRET_ID]

fun getAuthenticityKey() = credentialElementsMap[INDEX_AUTHENTICITY_KEY]

fun getPublicKey() = credentialElementsMap[INDEX_PUBLIC_KEY]

fun getEncryptedMetadataBytes() = credentialElementsMap[INDEX_ENCRYPTED_METADATA_BYTES]

fun getMetadataEncryptionKeyTag() = credentialElementsMap[INDEX_METADATA_ENCRYPTION_KEY_TAG]

fun getMetadataEncryptionKey() = credentialElementsMap[INDEX_METADATA_ENCRYPTION_KEY]

fun getSalt() = credentialElementsMap[INDEX_SALT]

private fun parseStringToByteArray(str: String): ByteArray {
    return str.split(DELIMITERS).map { element -> element.toInt().toByte()}.toByteArray()
}