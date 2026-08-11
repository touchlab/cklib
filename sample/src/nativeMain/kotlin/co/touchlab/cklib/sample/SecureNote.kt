/*
 * Copyright (c) 2021 Touchlab
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

@file:OptIn(ExperimentalForeignApi::class)

package co.touchlab.cklib.sample

import co.touchlab.cklib.sample.cinterop.CRYPTO_ARGON2_I
import co.touchlab.cklib.sample.cinterop.crypto_aead_lock
import co.touchlab.cklib.sample.cinterop.crypto_aead_unlock
import co.touchlab.cklib.sample.cinterop.crypto_argon2
import co.touchlab.cklib.sample.cinterop.crypto_argon2_config
import co.touchlab.cklib.sample.cinterop.crypto_argon2_extras
import co.touchlab.cklib.sample.cinterop.crypto_argon2_inputs
import co.touchlab.cklib.sample.cinterop.crypto_wipe
import co.touchlab.cklib.sample.cinterop.sample_random_bytes
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.cValue
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.set

/**
 * Passphrase-encrypted notes, built on the vendored Monocypher sources that cklib compiles to
 * bitcode and the cinterop bindings generated from the same headers.
 *
 * The sealed envelope is laid out as:
 *
 * ```
 * | "SNOTE1" (6) | salt (16) | nonce (24) | MAC (16) | ciphertext (n) |
 * ```
 *
 * This is sample code. Read the caveats in the sample README before reusing any of it: the Argon2
 * parameters are tuned to keep the tests fast, not to resist a real attacker.
 */
object SecureNote {

    private const val MAGIC = "SNOTE1"
    private const val MAGIC_SIZE = 6
    private const val SALT_SIZE = 16
    private const val NONCE_SIZE = 24
    private const val MAC_SIZE = 16
    private const val KEY_SIZE = 32

    private const val SALT_OFFSET = MAGIC_SIZE
    private const val NONCE_OFFSET = SALT_OFFSET + SALT_SIZE
    private const val MAC_OFFSET = NONCE_OFFSET + NONCE_SIZE
    private const val HEADER_SIZE = MAC_OFFSET + MAC_SIZE

    // Deliberately weak, so the test suite stays fast. 1024 blocks is 1 MB of work area; real
    // applications should use orders of magnitude more (see the README).
    private const val ARGON2_BLOCKS = 1024u
    private const val ARGON2_PASSES = 3u
    private const val ARGON2_LANES = 1u

    private val magicBytes = MAGIC.encodeToByteArray()

    /** Encrypts [note] under [passphrase] with a fresh random salt and nonce. */
    fun seal(passphrase: String, note: String): ByteArray =
        sealWith(passphrase, note, randomBytes(SALT_SIZE), randomBytes(NONCE_SIZE))

    /**
     * Decrypts an envelope produced by [seal].
     *
     * Returns `null` for a wrong passphrase, tampered contents, or any input that is not a
     * well-formed envelope. Nothing here throws on bad input.
     */
    fun open(passphrase: String, sealed: ByteArray): String? {
        if (sealed.size < HEADER_SIZE) return null
        for (i in magicBytes.indices) {
            if (sealed[i] != magicBytes[i]) return null
        }

        val salt = sealed.copyOfRange(SALT_OFFSET, SALT_OFFSET + SALT_SIZE)
        val nonce = sealed.copyOfRange(NONCE_OFFSET, NONCE_OFFSET + NONCE_SIZE)
        val mac = sealed.copyOfRange(MAC_OFFSET, MAC_OFFSET + MAC_SIZE)
        val cipherText = sealed.copyOfRange(HEADER_SIZE, sealed.size)

        return memScoped {
            val key = allocArray<UByteVar>(KEY_SIZE)
            deriveKey(passphrase, salt, key)

            val plain = allocArray<UByteVar>(bufferSize(cipherText.size))
            val status = crypto_aead_unlock(
                plain,
                allocFilled(mac),
                key,
                allocFilled(nonce),
                null,
                0uL,
                allocFilled(cipherText),
                cipherText.size.convert(),
            )
            crypto_wipe(key, KEY_SIZE.convert())

            // -1 means the MAC did not verify: wrong passphrase or tampered bytes.
            if (status != 0) {
                null
            } else {
                val plainText = plain.toByteArray(cipherText.size)
                crypto_wipe(plain, bufferSize(cipherText.size).convert())
                val note = plainText.decodeToString()
                plainText.fill(0)
                note
            }
        }
    }

    /**
     * [seal] with the salt and nonce supplied by the caller, so a test can pin them and assert
     * exact ciphertext bytes. Reusing a nonce with the same key destroys the security of the
     * scheme, which is why this is not public.
     */
    internal fun sealWith(
        passphrase: String,
        note: String,
        salt: ByteArray,
        nonce: ByteArray,
    ): ByteArray {
        require(salt.size == SALT_SIZE) { "salt must be $SALT_SIZE bytes, was ${salt.size}" }
        require(nonce.size == NONCE_SIZE) { "nonce must be $NONCE_SIZE bytes, was ${nonce.size}" }

        val plainText = note.encodeToByteArray()
        val sealed = ByteArray(HEADER_SIZE + plainText.size)
        magicBytes.copyInto(sealed, 0)
        salt.copyInto(sealed, SALT_OFFSET)
        nonce.copyInto(sealed, NONCE_OFFSET)

        memScoped {
            val key = allocArray<UByteVar>(KEY_SIZE)
            deriveKey(passphrase, salt, key)

            val plain = allocFilled(plainText)
            val cipher = allocArray<UByteVar>(bufferSize(plainText.size))
            val mac = allocArray<UByteVar>(MAC_SIZE)

            crypto_aead_lock(
                cipher,
                mac,
                key,
                allocFilled(nonce),
                null,
                0uL,
                plain,
                plainText.size.convert(),
            )

            crypto_wipe(key, KEY_SIZE.convert())
            crypto_wipe(plain, bufferSize(plainText.size).convert())

            for (i in 0 until MAC_SIZE) {
                sealed[MAC_OFFSET + i] = mac[i].toByte()
            }
            for (i in plainText.indices) {
                sealed[HEADER_SIZE + i] = cipher[i].toByte()
            }
        }
        plainText.fill(0)
        return sealed
    }

    /** Stretches [passphrase] into a [KEY_SIZE] byte key with Argon2i, writing it to [keyOut]. */
    private fun MemScope.deriveKey(
        passphrase: String,
        salt: ByteArray,
        keyOut: CPointer<UByteVar>,
    ) {
        val passBytes = passphrase.encodeToByteArray()
        val passPtr = allocFilled(passBytes)
        val saltPtr = allocFilled(salt)

        // Argon2 needs nb_blocks * 1024 bytes of scratch. memScoped hands out native heap memory
        // rather than stack memory, so a megabyte here is fine, and it is freed with the scope.
        val workArea = allocArray<UByteVar>(ARGON2_BLOCKS.toInt() * 1024)

        // Monocypher 4.x takes these three structs by value, hence cValue { } rather than plain
        // arguments. Monocypher 3.x had a flat crypto_argon2i(...) instead.
        crypto_argon2(
            keyOut,
            KEY_SIZE.convert(),
            workArea,
            cValue<crypto_argon2_config> {
                algorithm = CRYPTO_ARGON2_I.convert()
                nb_blocks = ARGON2_BLOCKS
                nb_passes = ARGON2_PASSES
                nb_lanes = ARGON2_LANES
            },
            cValue<crypto_argon2_inputs> {
                this.pass = passPtr
                this.salt = saltPtr
                this.pass_size = passBytes.size.convert()
                this.salt_size = salt.size.convert()
            },
            // The same thing the crypto_argon2_no_extras global holds: no key, no associated data.
            cValue<crypto_argon2_extras> {
                this.key = null
                this.ad = null
                this.key_size = 0u
                this.ad_size = 0u
            },
        )

        crypto_wipe(passPtr, passBytes.size.convert())
        passBytes.fill(0)
    }

    private fun randomBytes(size: Int): ByteArray = memScoped {
        val buffer = allocArray<UByteVar>(size)
        val status = sample_random_bytes(buffer, size.convert())
        check(status == 0) { "sample_random_bytes() failed with status $status" }
        buffer.toByteArray(size)
    }
}

/** allocArray(0) is not useful, and an empty note is a legitimate input. */
private fun bufferSize(size: Int): Int = if (size == 0) 1 else size

private fun MemScope.allocFilled(bytes: ByteArray): CPointer<UByteVar> {
    val pointer = allocArray<UByteVar>(bufferSize(bytes.size))
    for (i in bytes.indices) {
        pointer[i] = bytes[i].toUByte()
    }
    return pointer
}

/** Copies C memory into a Kotlin-owned array, so nothing outlives the enclosing [memScoped]. */
private fun CPointer<UByteVar>.toByteArray(size: Int): ByteArray =
    ByteArray(size) { this[it].toByte() }
