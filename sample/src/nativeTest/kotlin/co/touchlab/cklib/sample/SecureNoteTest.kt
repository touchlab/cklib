/*
 * Copyright (c) 2026 Touchlab
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

package co.touchlab.cklib.sample

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SecureNoteTest {

    @Test
    fun roundTrips() {
        val sealed = SecureNote.seal(PASSPHRASE, "meet me at the docks at midnight")
        assertEquals("meet me at the docks at midnight", SecureNote.open(PASSPHRASE, sealed))
    }

    @Test
    fun sealIsRandomized() {
        // Fresh salt and nonce per call, so the same note never seals to the same bytes.
        val first = SecureNote.seal(PASSPHRASE, "same note")
        val second = SecureNote.seal(PASSPHRASE, "same note")
        assertNotEquals(first.toHex(), second.toHex())
        assertEquals("same note", SecureNote.open(PASSPHRASE, second))
    }

    @Test
    fun wrongPassphraseReturnsNull() {
        val sealed = SecureNote.seal(PASSPHRASE, "meet me at the docks at midnight")
        assertNull(SecureNote.open("hunter3", sealed))
    }

    @Test
    fun flippedCipherTextByteReturnsNull() {
        val sealed = SecureNote.seal(PASSPHRASE, "meet me at the docks at midnight")
        val tampered = sealed.copyOf()
        tampered[HEADER_SIZE] = (tampered[HEADER_SIZE].toInt() xor 0x01).toByte()
        assertNull(SecureNote.open(PASSPHRASE, tampered))
    }

    @Test
    fun flippedMacByteReturnsNull() {
        val sealed = SecureNote.seal(PASSPHRASE, "meet me at the docks at midnight")
        val tampered = sealed.copyOf()
        tampered[MAC_OFFSET] = (tampered[MAC_OFFSET].toInt() xor 0x80).toByte()
        assertNull(SecureNote.open(PASSPHRASE, tampered))
    }

    @Test
    fun emptyNoteRoundTrips() {
        val sealed = SecureNote.seal(PASSPHRASE, "")
        assertEquals(HEADER_SIZE, sealed.size)
        assertEquals("", SecureNote.open(PASSPHRASE, sealed))
    }

    @Test
    fun multiBlockNoteRoundTrips() {
        // Several kilobytes, well past ChaCha20's 64 byte block, to catch off-by-one buffer sizing.
        val note = buildString {
            repeat(200) { append("line $it: the quick brown fox jumps over the lazy dog\n") }
        }
        assertTrue(note.length > 4096)
        val sealed = SecureNote.seal(PASSPHRASE, note)
        assertEquals(note, SecureNote.open(PASSPHRASE, sealed))
    }

    @Test
    fun nonAsciiNoteRoundTrips() {
        val note = "ここは秘密です — naïve café, 🔐 100% sûr"
        val sealed = SecureNote.seal(PASSPHRASE, note)
        assertEquals(note, SecureNote.open(PASSPHRASE, sealed))
    }

    @Test
    fun malformedInputReturnsNull() {
        assertNull(SecureNote.open(PASSPHRASE, ByteArray(0)))
        assertNull(SecureNote.open(PASSPHRASE, ByteArray(HEADER_SIZE - 1)))
        assertNull(SecureNote.open(PASSPHRASE, ByteArray(HEADER_SIZE + 8) { 0x41 }))
        assertNull(SecureNote.open(PASSPHRASE, ByteArray(1024) { it.toByte() }))

        // Right magic, right length, garbage everywhere else.
        val garbage = ByteArray(HEADER_SIZE + 16) { 0x7f }
        "SNOTE1".encodeToByteArray().copyInto(garbage, 0)
        assertNull(SecureNote.open(PASSPHRASE, garbage))

        // A valid envelope truncated mid-ciphertext.
        val sealed = SecureNote.seal(PASSPHRASE, "meet me at the docks at midnight")
        assertNull(SecureNote.open(PASSPHRASE, sealed.copyOf(sealed.size - 5)))
        assertNull(SecureNote.open(PASSPHRASE, sealed.copyOf(HEADER_SIZE / 2)))
    }

    @Test
    fun knownAnswer() {
        // The point of this test: the salt, nonce, passphrase and note are all fixed, so the
        // envelope is fully deterministic. Every target that compiles the same vendored
        // Monocypher sources must produce these exact bytes. If this passes on macosArm64 and
        // linuxX64 alike, the C really is being compiled and linked identically everywhere.
        val sealed = SecureNote.sealWith(
            passphrase = "correct horse battery staple",
            note = "attack at dawn",
            salt = ByteArray(16) { it.toByte() },
            nonce = ByteArray(24) { (0xa0 + it).toByte() },
        )
        assertEquals(GOLDEN, sealed.toHex())
        assertEquals(
            "attack at dawn",
            SecureNote.open("correct horse battery staple", sealed),
        )
    }

    private companion object {
        const val PASSPHRASE = "hunter2"

        const val MAGIC_SIZE = 6
        const val SALT_SIZE = 16
        const val NONCE_SIZE = 24
        const val MAC_SIZE = 16
        const val MAC_OFFSET = MAGIC_SIZE + SALT_SIZE + NONCE_SIZE
        const val HEADER_SIZE = MAC_OFFSET + MAC_SIZE

        // "SNOTE1" | salt 00..0f | nonce a0..b7 | MAC | ciphertext of "attack at dawn"
        const val GOLDEN = "534e4f544531" +
            "000102030405060708090a0b0c0d0e0f" +
            "a0a1a2a3a4a5a6a7a8a9aaabacadaeafb0b1b2b3b4b5b6b7" +
            "aba1e52c9024785fa4bdf90f729b34da" +
            "7d9757b3e43a89cdb2b21953a174"
    }
}

private fun ByteArray.toHex(): String {
    val hex = "0123456789abcdef"
    return buildString(size * 2) {
        for (byte in this@toHex) {
            val value = byte.toInt() and 0xff
            append(hex[value shr 4])
            append(hex[value and 0x0f])
        }
    }
}
