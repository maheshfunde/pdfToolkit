package com.yashodatech.pdftoolkit.pdf

import android.util.Log
import com.itextpdf.kernel.utils.IXmlParserFactory
import org.xml.sax.EntityResolver
import org.xml.sax.InputSource
import org.xml.sax.XMLReader
import java.io.StringReader
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.SAXParserFactory

/**
 * Android-compatible XML parser factory for iText 7.
 *
 * iText's default [com.itextpdf.kernel.utils.DefaultSafeXmlParserFactory] calls:
 *   factory.setXIncludeAware(false)
 * which Android's XML parser implementation does not support, throwing:
 *   java.lang.UnsupportedOperationException: This parser does not support specification "Unknown" version "0.0"
 *
 * Furthermore, Android does not support DTD/Schema validating parsers, so validating must remain false.
 */
class AndroidXmlParserFactory : IXmlParserFactory {

    override fun createDocumentBuilderInstance(namespaceAware: Boolean, ignoringComments: Boolean): DocumentBuilder {
        return try {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = namespaceAware
                isValidating = false
                isIgnoringComments = ignoringComments

                trySetFeature(this, "http://apache.org/xml/features/disallow-doctype-decl", true)
                trySetFeature(this, "http://xml.org/sax/features/external-general-entities", false)
                trySetFeature(this, "http://xml.org/sax/features/external-parameter-entities", false)
                trySetFeature(this, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false)

                try {
                    isExpandEntityReferences = false
                } catch (_: Throwable) {}
            }
            val builder = factory.newDocumentBuilder()
            builder.setEntityResolver(EmptyEntityResolver)
            builder
        } catch (e: Throwable) {
            Log.w("AndroidXmlParserFactory", "Secure DocumentBuilder failed, falling back to minimal builder: ${e.message}")
            val fallbackFactory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = namespaceAware
                isValidating = false
            }
            val builder = fallbackFactory.newDocumentBuilder()
            builder.setEntityResolver(EmptyEntityResolver)
            builder
        }
    }

    override fun createXMLReaderInstance(namespaceAware: Boolean, validating: Boolean): XMLReader {
        return try {
            val factory = SAXParserFactory.newInstance().apply {
                isNamespaceAware = namespaceAware
                isValidating = false

                trySetFeature(this, "http://apache.org/xml/features/disallow-doctype-decl", true)
                trySetFeature(this, "http://xml.org/sax/features/external-general-entities", false)
                trySetFeature(this, "http://xml.org/sax/features/external-parameter-entities", false)
                trySetFeature(this, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            }
            val parser = factory.newSAXParser()
            val reader = parser.xmlReader
            reader.entityResolver = EmptyEntityResolver
            reader
        } catch (e: Throwable) {
            Log.w("AndroidXmlParserFactory", "Secure SAXParser failed, falling back to minimal reader: ${e.message}")
            val fallbackFactory = SAXParserFactory.newInstance().apply {
                isNamespaceAware = namespaceAware
                isValidating = false
            }
            val parser = fallbackFactory.newSAXParser()
            val reader = parser.xmlReader
            reader.entityResolver = EmptyEntityResolver
            reader
        }
    }

    private fun trySetFeature(factory: DocumentBuilderFactory, feature: String, value: Boolean) {
        try {
            factory.setFeature(feature, value)
        } catch (_: Throwable) {}
    }

    private fun trySetFeature(factory: SAXParserFactory, feature: String, value: Boolean) {
        try {
            factory.setFeature(feature, value)
        } catch (_: Throwable) {}
    }

    private object EmptyEntityResolver : EntityResolver {
        override fun resolveEntity(publicId: String?, systemId: String?): InputSource {
            return InputSource(StringReader(""))
        }
    }
}
