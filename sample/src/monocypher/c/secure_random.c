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

/*
 * glibc hides getentropy() behind _DEFAULT_SOURCE, and the macro has to be set before the first
 * libc header is pulled in, hence the placement above the includes.
 */
#if defined(__linux__) && !defined(_DEFAULT_SOURCE)
#define _DEFAULT_SOURCE 1
#endif

#include "secure_random.h"

/*
 * Choosing an entropy source is the one genuinely platform-specific thing in this sample:
 *
 *   macOS            getentropy(), declared in <sys/random.h> since 10.12.
 *
 *   iOS / simulator  libSystem exports getentropy() (it is marked __IOS_AVAILABLE(10.0) in the
 *                    macOS SDK), but the iOS SDKs ship no <sys/random.h> at all. arc4random_buf()
 *                    is declared in <stdlib.h> on every Apple SDK, is the same CSPRNG underneath,
 *                    and has no 256 byte request limit.
 *
 *   Linux            getentropy() needs glibc 2.25 or newer, and the sysroot Kotlin/Native cross
 *                    compiles against is glibc 2.19, so read /dev/urandom instead. This branch
 *                    also covers any other Unix without getentropy().
 */
#if defined(__APPLE__) && __has_include(<sys/random.h>)
#include <sys/random.h>
#define SAMPLE_SOURCE_GETENTROPY 1
#elif defined(__APPLE__)
#include <stdlib.h>
#define SAMPLE_SOURCE_ARC4RANDOM 1
#elif defined(__GLIBC__) && (__GLIBC__ > 2 || (__GLIBC__ == 2 && __GLIBC_MINOR__ >= 25))
#include <unistd.h>
#define SAMPLE_SOURCE_GETENTROPY 1
#else
#include <errno.h>
#include <fcntl.h>
#include <unistd.h>
#define SAMPLE_SOURCE_URANDOM 1
#endif

#if defined(SAMPLE_SOURCE_GETENTROPY)

/* getentropy() rejects requests larger than 256 bytes, so loop. */
#define SAMPLE_ENTROPY_CHUNK 256

static int fill_random(uint8_t *buf, size_t size)
{
	size_t offset = 0;
	while (offset < size) {
		size_t chunk = size - offset;
		if (chunk > SAMPLE_ENTROPY_CHUNK) {
			chunk = SAMPLE_ENTROPY_CHUNK;
		}
		if (getentropy(buf + offset, chunk) != 0) {
			return -1;
		}
		offset += chunk;
	}
	return 0;
}

#elif defined(SAMPLE_SOURCE_ARC4RANDOM)

static int fill_random(uint8_t *buf, size_t size)
{
	arc4random_buf(buf, size);
	return 0;
}

#else

static int fill_random(uint8_t *buf, size_t size)
{
	int fd = open("/dev/urandom", O_RDONLY);
	if (fd < 0) {
		return -1;
	}
	size_t offset = 0;
	while (offset < size) {
		ssize_t read_bytes = read(fd, buf + offset, size - offset);
		if (read_bytes <= 0) {
			if (read_bytes < 0 && errno == EINTR) {
				continue;
			}
			close(fd);
			return -1;
		}
		offset += (size_t)read_bytes;
	}
	close(fd);
	return 0;
}

#endif

int sample_random_bytes(uint8_t *buf, size_t size)
{
	if (size == 0) {
		return 0;
	}
	if (buf == NULL) {
		return -1;
	}
	return fill_random(buf, size);
}
