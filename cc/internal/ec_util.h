// Copyright 2021 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
///////////////////////////////////////////////////////////////////////////////
#ifndef TINK_INTERNAL_EC_UTIL_H_
#define TINK_INTERNAL_EC_UTIL_H_

#include <string>

#include "openssl/ec.h"
#include "tink/internal/ssl_unique_ptr.h"
#include "tink/subtle/common_enums.h"
#include "tink/util/secret_data.h"
#include "tink/util/statusor.h"

namespace crypto {
namespace tink {
namespace internal {

constexpr int64_t X25519KeyPubKeySize() { return 32; }
constexpr int64_t X25519KeyPrivKeySize() { return 32; }

struct EcKey {
  subtle::EllipticCurveType curve;
  // Affine coordinates in bigendian representation.
  std::string pub_x;
  std::string pub_y;
  // Big integer in bigendian representation.
  crypto::tink::util::SecretData priv;
};

struct X25519Key {
  uint8_t public_value[X25519KeyPubKeySize()];
  uint8_t private_key[X25519KeyPrivKeySize()];
};

struct Ed25519Key {
  std::string public_key;
  std::string private_key;
};

// EcKey.

// Returns a new EC key for the specified curve.
crypto::tink::util::StatusOr<EcKey> NewEcKey(
    crypto::tink::subtle::EllipticCurveType curve_type);

// Returns a new EC key for the specified curve derived from a secret seed.
crypto::tink::util::StatusOr<EcKey> NewEcKey(
    crypto::tink::subtle::EllipticCurveType curve_type,
    const crypto::tink::util::SecretData &secret_seed);

// X25519 Key Utils.

// Returns a new X25519Key key. It returns a kInternal error status if the
// OpenSSL/BoringSSL APIs fail.
crypto::tink::util::StatusOr<std::unique_ptr<X25519Key>> NewX25519Key();

// Returns a X25519Key matching the specified EcKey.
crypto::tink::util::StatusOr<std::unique_ptr<X25519Key>> X25519KeyFromEcKey(
    const EcKey &ec_key);

// Returns an EcKey matching the specified X25519Key.
EcKey EcKeyFromX25519Key(const X25519Key *x25519_key);

// EC_POINT Encode/Decode.

// Given x, y as curve_size_in_bytes big-endian byte array, encoding is as
// follows:
// - The uncompressed point is encoded as 0x04 || x || y.
// - The compressed point is encoded as:
//   - 0x03 || x if the least significant bit of y is 1;
//   - 0x02 || x otherwise.

// Returns OpenSSL/BoringSSL's EC_POINT constructed from curve type
// `curve_type`, point `format` and encoded public key's point `encoded_point`.
crypto::tink::util::StatusOr<SslUniquePtr<EC_POINT>> EcPointDecode(
    crypto::tink::subtle::EllipticCurveType curve_type,
    crypto::tink::subtle::EcPointFormat format,
    absl::string_view encoded_point);

// Returns the encoded public key based on curve type `curve_type`, point
// `format` and OpenSSL/BoringSSL's EC_POINT public key `point`.
crypto::tink::util::StatusOr<std::string> EcPointEncode(
    crypto::tink::subtle::EllipticCurveType curve_type,
    crypto::tink::subtle::EcPointFormat format, const EC_POINT *point);

// Returns the encoding size of a point on the specified elliptic curve
// `curve_type` when the given point `format` is used.
util::StatusOr<int32_t> EcPointEncodingSizeInBytes(
    crypto::tink::subtle::EllipticCurveType curve_type,
    crypto::tink::subtle::EcPointFormat format);

// Returns the size (in bytes) of an element of the field over which
// the curve `curve_type` is defined.
util::StatusOr<int32_t> EcFieldSizeInBytes(
    crypto::tink::subtle::EllipticCurveType curve_type);

// EC_GROUP Utils.

// Returns OpenSSL/BoringSSL's EC_GROUP constructed from the given `curve_type`.
crypto::tink::util::StatusOr<SslUniquePtr<EC_GROUP>> EcGroupFromCurveType(
    crypto::tink::subtle::EllipticCurveType curve_type);

// Returns the curve type associated with the given `group`.
crypto::tink::util::StatusOr<crypto::tink::subtle::EllipticCurveType>
CurveTypeFromEcGroup(const EC_GROUP *group);

// Returns OpenSSL/BoringSSL's EC_POINT constructed from the curve type,
// big-endian representation of public key's x-coordinate and y-coordinate.
crypto::tink::util::StatusOr<SslUniquePtr<EC_POINT>> GetEcPoint(
    crypto::tink::subtle::EllipticCurveType curve, absl::string_view pubx,
    absl::string_view puby);

}  // namespace internal
}  // namespace tink
}  // namespace crypto

#endif  // TINK_INTERNAL_EC_UTIL_H_
