package scala.meta.internal.pc

import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.Optional

import scala.meta.internal.io.FileIO
import scala.meta.internal.io.PathIO
import scala.meta.internal.jdk.CollectionConverters.*
import scala.meta.internal.metals.HtmlBuilder
import scala.meta.internal.mtags.MtagsEnrichments._
import scala.meta.io.AbsolutePath

import com.google.gson.JsonPrimitive
import dotty.tools.dotc.core.tasty.TastyAnsiiPrinter
import dotty.tools.dotc.core.tasty.TastyHTMLPrinter
import dotty.tools.dotc.core.tasty.TastyPrinter
import org.eclipse.{lsp4j => l}

object TastyUtils {
  def getTasty(
      tastyURI: URI,
      isHtmlSupported: Boolean,
      isHttpEnabled: Boolean
  ): String =
    if (isHttpEnabled)
      getStandaloneHtmlTasty(tastyURI)
    else if (isHtmlSupported)
      htmlTasty(tastyURI)
    else
      normalTasty(tastyURI)

  def getStandaloneHtmlTasty(tastyURI: URI): String = {
    htmlTasty(tastyURI, List(standaloneHtmlStyles))
  }

  private def normalTasty(tastyURI: URI): String = {
    val tastyBytes = AbsolutePath.fromAbsoluteUri(tastyURI).readAllBytes
    new TastyPrinter(tastyBytes).showContents()
  }

  private def htmlTasty(
      tastyURI: URI,
      headElems: List[String] = Nil,
      bodyAttributes: String = ""
  ): String = {
    val title = tastyHtmlPageTitle(tastyURI)
    val tastyBytes = AbsolutePath.fromAbsoluteUri(tastyURI).readAllBytes
    val tastyHtml = new TastyHTMLPrinter(tastyBytes).showContents()
    HtmlBuilder()
      .page(title, htmlStyles :: headElems, bodyAttributes) { builder =>
        builder
          .element("pre", "class='container is-dark'")(_.raw(tastyHtml))
      }
      .render
  }

  private def tastyHtmlPageTitle(file: URI) = {
    val filename = PathIO.basename(AbsolutePath.fromAbsoluteUri(file).toString)
    s"TASTy for $filename"
  }

  private val standaloneHtmlStyles =
    """|<style>
       |  body {
       |    background-color: #212529;
       |    color: wheat;
       |    padding: 1em;
       |    margin: 0;
       |    font-size: 14px;
       |  }
       |</style>
       |""".stripMargin

  private val htmlStyles =
    """|<style>
       |  span.name {
       |    color: magenta;
       |  }
       |  span.tree {
       |    color: yellow;
       |  }
       |  span.length {
       |    color: cyan;
       |  }
       |</style>
       |""".stripMargin
}
