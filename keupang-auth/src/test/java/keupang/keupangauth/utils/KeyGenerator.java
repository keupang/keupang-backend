package keupang.keupangauth.utils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;

public class KeyGenerator {
    public static void main(String[] args) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(256);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        ECPrivateKey privateKey = (ECPrivateKey) keyPair.getPrivate();
        ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();

        String privateKeyBase64 = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        String publicKeyBase64 = Base64.getEncoder().encodeToString(publicKey.getEncoded());

        // PemUtils expects the Env Var to be Base64 encoded version of the PEM/Key
        // String.
        // So we need to encode the Base64 key string AGAIN.
        String envVarPrivateKey = Base64.getEncoder().encodeToString(privateKeyBase64.getBytes());
        String envVarPublicKey = Base64.getEncoder().encodeToString(publicKeyBase64.getBytes());

        System.out.println("--- JWT PRIVATE KEY (jwt_private_key) ---");
        System.out.println(envVarPrivateKey);
        System.out.println("\n--- JWT PUBLIC KEY (jwt_public_key) ---");
        System.out.println(envVarPublicKey);
    }
}
