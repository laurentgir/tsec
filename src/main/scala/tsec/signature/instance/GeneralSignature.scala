package tsec.signature.instance

import java.security.spec._
import java.security.{KeyFactory, KeyPairGenerator}

import cats.syntax.either._
import tsec.core.ErrorConstruct._
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.spec.ECNamedCurveSpec
import tsec.signature.core.SigAlgoTag

abstract class GeneralSignature[A](signature: String) {
  implicit val sig = new SigAlgoTag[A] {
    override lazy val algorithm: String = signature
  }
}

abstract class RSASignature[A](signature: String) extends KFTag[A] with SigAlgoTag[A] {

  override lazy val algorithm: String = signature

  val keyFactoryAlgo: String = "RSA"

  implicit val sig: SigAlgoTag[A] = this

  implicit val kt: KFTag[A] = this

  def generateKeyPair: Either[SignatureKeyError, SigKeyPair[A]] =
    Either.catchNonFatal(generateKeyPairUnsafe).mapError[SignatureKeyError]

  def generateKeyPairUnsafe: SigKeyPair[A] =
    SigKeyPair.fromKeyPair[A](KeyPairGenerator.getInstance(kt.keyFactoryAlgo).generateKeyPair())

  def buildPrivateKey(keyBytes: Array[Byte]): Either[SignatureKeyError, SigPrivateKey[A]] =
    Either.catchNonFatal(buildPrivateKeyUnsafe(keyBytes)).mapError[SignatureKeyError]

  def buildPrivateKeyUnsafe(keyBytes: Array[Byte]): SigPrivateKey[A] =
    SigPrivateKey.fromKey[A](
      KeyFactory.getInstance(kt.keyFactoryAlgo).generatePrivate(new PKCS8EncodedKeySpec(keyBytes))
    )

  def buildPublicKey(keyBytes: Array[Byte]): Either[SignatureKeyError, SigPublicKey[A]] =
    Either.catchNonFatal(buildPublicKeyUnsafe(keyBytes)).mapError[SignatureKeyError]

  def buildPublicKeyUnsafe(keyBytes: Array[Byte]): SigPublicKey[A] =
    SigPublicKey.fromKey[A](KeyFactory.getInstance(kt.keyFactoryAlgo).generatePublic(new X509EncodedKeySpec(keyBytes)))
}

abstract class ECDSASignature[A](signature: String, dCurve: String, outLen: Int)
    extends SigAlgoTag[A]
    with ECCurve[A]
    with ECKFTag[A] {

  override lazy val algorithm: String = signature

  protected val defaultCurve: String = dCurve

  val keyFactoryAlgo: String = "ECDSA"
  val outputLen: Int         = outLen

  implicit val sig: SigAlgoTag[A] = this

  implicit val curve: ECCurve[A] = this

  implicit val kt: ECKFTag[A] = this

  def generateKeyPair: Either[SignatureKeyError, SigKeyPair[A]] =
    Either.catchNonFatal(generateKeyPairUnsafe).mapError[SignatureKeyError]

  def generateKeyPairUnsafe: SigKeyPair[A] = {
    val instance = KeyPairGenerator.getInstance(keyFactoryAlgo, ECDSASignature.Provider)
    instance.initialize(new ECGenParameterSpec(defaultCurve))
    SigKeyPair.fromKeyPair[A](instance.generateKeyPair())
  }

  def buildPrivateKey(keyBytes: Array[Byte]): Either[SignatureKeyError, SigPrivateKey[A]] =
    Either.catchNonFatal(buildPrivateKeyUnsafe(keyBytes)).mapError[SignatureKeyError]

  def buildPrivateKeyUnsafe(keyBytes: Array[Byte]): SigPrivateKey[A] =
    SigPrivateKey.fromKey[A](
      KeyFactory
        .getInstance(kt.keyFactoryAlgo, ECDSASignature.Provider)
        .generatePrivate(new PKCS8EncodedKeySpec(keyBytes))
    )

  def buildPrivateKey(S: BigInt): Either[SignatureKeyError, BigInt] =
    Either.catchNonFatal(S).mapError[SignatureKeyError]

  def buildPrivateKeyUnsafe(S: BigInt): SigPrivateKey[A] = {
    val spec = new ECPrivateKeySpec(S.underlying(), curveSpec)
    SigPrivateKey.fromKey[A](
      KeyFactory
        .getInstance(kt.keyFactoryAlgo, ECDSASignature.Provider)
        .generatePrivate(spec)
    )
  }

  private lazy val curveSpec = {
    val paramSpec = ECNamedCurveTable.getParameterSpec(defaultCurve)
    new ECNamedCurveSpec(defaultCurve, paramSpec.getCurve, paramSpec.getG, paramSpec.getN, paramSpec.getH)
  }

  def buildPublicKey(x: BigInt, y: BigInt): Either[SignatureKeyError, SigPublicKey[A]] =
    Either.catchNonFatal(buildPublicKeyUnsafe(x, y)).mapError[SignatureKeyError]

  def buildPublicKeyUnsafe(x: BigInt, y: BigInt): SigPublicKey[A] = {
    val spec = new ECPublicKeySpec(new ECPoint(x.underlying(), y.underlying()), curveSpec)
    SigPublicKey.fromKey[A](KeyFactory.getInstance(keyFactoryAlgo, ECDSASignature.Provider).generatePublic(spec))
  }

  def buildPublicKey(keyBytes: Array[Byte]): Either[SignatureKeyError, SigPublicKey[A]] =
    Either.catchNonFatal(buildPublicKeyUnsafe(keyBytes)).mapError[SignatureKeyError]

  def buildPublicKeyUnsafe(keyBytes: Array[Byte]): SigPublicKey[A] =
    SigPublicKey.fromKey[A](
      KeyFactory
        .getInstance(kt.keyFactoryAlgo, ECDSASignature.Provider)
        .generatePublic(new X509EncodedKeySpec(keyBytes))
    )

}

object ECDSASignature {
  val Provider = "BC"
}
