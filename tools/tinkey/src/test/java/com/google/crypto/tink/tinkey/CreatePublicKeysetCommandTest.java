// Copyright 2017 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
////////////////////////////////////////////////////////////////////////////////

package com.google.crypto.tink.tinkey;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertTrue;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.InsecureSecretKeyAccess;
import com.google.crypto.tink.Key;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.KmsClients;
import com.google.crypto.tink.PrivateKey;
import com.google.crypto.tink.TinkJsonProtoKeysetFormat;
import com.google.crypto.tink.TinkProtoKeysetFormat;
import com.google.crypto.tink.hybrid.HybridConfig;
import com.google.crypto.tink.signature.Ed25519Parameters;
import com.google.crypto.tink.signature.SignatureConfig;
import com.google.crypto.tink.testing.TestUtil;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@code CreatePublicKeysetCommand}.
 */
@RunWith(JUnit4.class)
public class CreatePublicKeysetCommandTest {
  private enum KeyType {
    HYBRID,
    SIGNATURE,
  };

  private static final String OUTPUT_FORMAT = "json";
  private static final String INPUT_FORMAT = "json";

  @BeforeClass
  public static void setUp() throws Exception {
    HybridConfig.register();
    SignatureConfig.register();
  }

  @Test
  public void testCreatePublicKeyset_ed25519Json_works() throws Exception {
    Path path = Files.createTempDirectory(/* prefix= */ "");
    Path privateKeyFile = Paths.get(path.toString(), "privateKeyFile");
    Path publicKeyFile = Paths.get(path.toString(), "publicKeyFile");

    KeysetHandle privateKeyset =
        KeysetHandle.generateNew(Ed25519Parameters.create(Ed25519Parameters.Variant.TINK));
    String serializedKeyset =
        TinkJsonProtoKeysetFormat.serializeKeyset(privateKeyset, InsecureSecretKeyAccess.get());
    Files.write(privateKeyFile, serializedKeyset.getBytes(UTF_8));

    Tinkey.main(
        new String[] {
          "create-public-keyset",
          "--in",
          privateKeyFile.toString(),
          "--out",
          publicKeyFile.toString()
        });

    KeysetHandle publicKeyset =
        TinkJsonProtoKeysetFormat.parseKeysetWithoutSecret(
            new String(Files.readAllBytes(publicKeyFile), UTF_8));
    assertThat(publicKeyset.size()).isEqualTo(1);
    Key expectedPublicKey = ((PrivateKey) privateKeyset.getPrimary().getKey()).getPublicKey();
    assertTrue(publicKeyset.getPrimary().getKey().equalsKey(expectedPublicKey));
  }

  @Test
  public void testCreatePublicKeyset_ed25519Binary_works() throws Exception {
    Path path = Files.createTempDirectory(/* prefix= */ "");
    Path privateKeyFile = Paths.get(path.toString(), "privateKeyFile");
    Path publicKeyFile = Paths.get(path.toString(), "publicKeyFile");

    KeysetHandle privateKeyset =
        KeysetHandle.generateNew(Ed25519Parameters.create(Ed25519Parameters.Variant.TINK));
    byte[] serializedKeyset =
        TinkProtoKeysetFormat.serializeKeyset(privateKeyset, InsecureSecretKeyAccess.get());
    Files.write(privateKeyFile, serializedKeyset);

    Tinkey.main(
        new String[] {
          "create-public-keyset",
          "--in",
          privateKeyFile.toString(),
          "--in-format",
          "binary",
          "--out",
          publicKeyFile.toString(),
          "--out-format",
          "binary"
        });

    KeysetHandle publicKeyset =
        TinkProtoKeysetFormat.parseKeysetWithoutSecret(Files.readAllBytes(publicKeyFile));
    assertThat(publicKeyset.size()).isEqualTo(1);
    Key expectedPublicKey = ((PrivateKey) privateKeyset.getPrimary().getKey()).getPublicKey();
    assertTrue(publicKeyset.getPrimary().getKey().equalsKey(expectedPublicKey));
  }

  @Test
  public void testCreatePublicKeyset_ed25519_encrypted_json_works() throws Exception {
    Path path = Files.createTempDirectory(/* prefix= */ "");
    Path privateKeyFile = Paths.get(path.toString(), "privateKeyFile");
    Path publicKeyFile = Paths.get(path.toString(), "publicKeyFile");

    KeysetHandle privateKeyset =
        KeysetHandle.generateNew(Ed25519Parameters.create(Ed25519Parameters.Variant.TINK));

    Aead masterKeyAead =
        KmsClients.getAutoLoaded(TestUtil.GCP_KMS_TEST_KEY_URI)
            .withCredentials(TestUtil.SERVICE_ACCOUNT_FILE)
            .getAead(TestUtil.GCP_KMS_TEST_KEY_URI);
    String serializedKeyset =
        TinkJsonProtoKeysetFormat.serializeEncryptedKeyset(
            privateKeyset, masterKeyAead, new byte[] {});

    Files.write(privateKeyFile, serializedKeyset.getBytes(UTF_8));

    Tinkey.main(
        new String[] {
          "create-public-keyset",
          "--in",
          privateKeyFile.toString(),
          "--out",
          publicKeyFile.toString(),
          "--master-key-uri",
          TestUtil.GCP_KMS_TEST_KEY_URI,
          "--credential",
          TestUtil.SERVICE_ACCOUNT_FILE
        });

    KeysetHandle publicKeyset =
        TinkJsonProtoKeysetFormat.parseKeysetWithoutSecret(
            new String(Files.readAllBytes(publicKeyFile), UTF_8));
    assertThat(publicKeyset.size()).isEqualTo(1);
    Key expectedPublicKey = ((PrivateKey) privateKeyset.getPrimary().getKey()).getPublicKey();
    assertTrue(publicKeyset.getPrimary().getKey().equalsKey(expectedPublicKey));
  }

  @Test
  public void testCreatePublicKeyset_ed25519_encrypted_jsonBinaryMixed_works() throws Exception {
    Path path = Files.createTempDirectory(/* prefix= */ "");
    Path privateKeyFile = Paths.get(path.toString(), "privateKeyFile");
    Path publicKeyFile = Paths.get(path.toString(), "publicKeyFile");

    KeysetHandle privateKeyset =
        KeysetHandle.generateNew(Ed25519Parameters.create(Ed25519Parameters.Variant.TINK));

    Aead masterKeyAead =
        KmsClients.getAutoLoaded(TestUtil.GCP_KMS_TEST_KEY_URI)
            .withCredentials(TestUtil.SERVICE_ACCOUNT_FILE)
            .getAead(TestUtil.GCP_KMS_TEST_KEY_URI);
    byte[] serializedKeyset =
        TinkProtoKeysetFormat.serializeEncryptedKeyset(privateKeyset, masterKeyAead, new byte[] {});

    Files.write(privateKeyFile, serializedKeyset);

    Tinkey.main(
        new String[] {
          "create-public-keyset",
          "--in",
          privateKeyFile.toString(),
          "--in-format",
          "binary",
          "--out",
          publicKeyFile.toString(),
          "--master-key-uri",
          TestUtil.GCP_KMS_TEST_KEY_URI,
          "--credential",
          TestUtil.SERVICE_ACCOUNT_FILE
        });

    KeysetHandle publicKeyset =
        TinkJsonProtoKeysetFormat.parseKeysetWithoutSecret(
            new String(Files.readAllBytes(publicKeyFile), UTF_8));
    assertThat(publicKeyset.size()).isEqualTo(1);
    Key expectedPublicKey = ((PrivateKey) privateKeyset.getPrimary().getKey()).getPublicKey();
    assertTrue(publicKeyset.getPrimary().getKey().equalsKey(expectedPublicKey));
  }
}
