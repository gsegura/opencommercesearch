package org.opencommercesearch.api.service

import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date

import org.apache.commons.codec.binary.Base64
import play.api.libs.json.JsValue
import play.api.libs.ws.WS.WSRequestHolder
import play.api.libs.ws._

import scala.concurrent.Future

//TODO gsegura: create interface for this class and extract the api url, user & pass to config files
object OmnitureClient {

  val password = "da581595243f35f5787d6d4aadb7123f".getBytes
  val username = "gsegura:Backcountry"

  def callOmniture(data: JsValue, reportMethod: String): Future[Response] = {

    val holder : WSRequestHolder = WS.url("https://api.omniture.com/admin/1.3/rest/")

    val nonceB = generateNonce()

    val created = generateTimestamp()
    val passwordDigest = getBase64Digest(nonceB, created.getBytes, password)
    val nonce = base64Encode(nonceB)

    val complexHolder : WSRequestHolder =
      holder.withHeaders("X-WSSE" -> ("UsernameToken Username=\"" + username + "\", PasswordDigest=\"" + passwordDigest + "\", Nonce=\"" + nonce + "\", Created=\"" + created + "\""))
            .withQueryString("method" -> reportMethod)

    complexHolder.post(data)
  }

  def generateTimestamp() = {
    val dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    dateFormatter.format(new Date());
  }

  def generateNonce() = {
    val nonce = new Date().getTime().toString;
    nonce.getBytes()
  }

  def base64Encode(bytes: Array[Byte]): String = {
    Base64.encodeBase64String(bytes)
  }

  def getBase64Digest(nonce: Array[Byte], created: Array[Byte], password: Array[Byte]) : String = {
    val messageDigester = MessageDigest.getInstance("SHA-1")
    messageDigester.reset()
    messageDigester.update(nonce)
    messageDigester.update(created)
    messageDigester.update(password)
    base64Encode(messageDigester.digest())
  }
}
