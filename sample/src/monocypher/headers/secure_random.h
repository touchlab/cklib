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

#ifndef SAMPLE_SECURE_RANDOM_H
#define SAMPLE_SECURE_RANDOM_H

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Fill `buf` with `size` cryptographically secure random bytes.
 *
 * Monocypher deliberately ships no RNG, so the sample provides one here in the
 * C layer that cklib already compiles, rather than as a Kotlin expect/actual.
 *
 * Returns 0 on success and a non-zero value on failure. On failure the contents
 * of `buf` are unspecified and must not be used.
 */
int sample_random_bytes(uint8_t *buf, size_t size);

#ifdef __cplusplus
}
#endif

#endif /* SAMPLE_SECURE_RANDOM_H */
