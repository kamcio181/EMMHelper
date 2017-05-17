package net.dongliu.apk.parser.parser;

import net.dongliu.apk.parser.struct.xml.*;
import net.dongliu.apk.parser.utils.xml.XmlEscaper;

import java.util.List;

/**
 * trans to xml text when parse binary xml file.
 *
 * @author dongliu
 */
public class XmlTranslator implements XmlStreamer {
    private StringBuilder sb;
    private int shift = 0;
    private XmlNamespaces namespaces;
    private boolean isLastStartTag;

    public XmlTranslator() {
        sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        this.namespaces = new XmlNamespaces();
    }

    @Override
    public void onStartTag(XmlNodeStartTag xmlNodeStartTag) {
        if (isLastStartTag) {
            sb.append(">\n\n");
        }
        appendShift(shift++);
        sb.append('<');
        if (xmlNodeStartTag.getNamespace() != null) {
            String prefix = namespaces.getPrefixViaUri(xmlNodeStartTag.getNamespace());
            if (prefix != null) {
                sb.append(prefix).append(":");
            } else {
                sb.append(xmlNodeStartTag.getNamespace()).append(":");
            }
        }
        sb.append(xmlNodeStartTag.getName());

        List<XmlNamespaces.XmlNamespace> nps = namespaces.consumeNameSpaces();
        if (!nps.isEmpty()) {
            for (int i = 0; i < nps.size(); i++) {
                if(i == 0)
                    sb.append(" ");
                else
                    appendShift(shift);
                sb.append("xmlns:").append(nps.get(i).getPrefix()).append("=\"")
                        .append(nps.get(i).getUri())
                        .append("\"");
                if(i != nps.size() -1)
                    sb.append("\n");
            }
        }
        isLastStartTag = true;

        int attributesSize = xmlNodeStartTag.getAttributes().size();
        if(attributesSize == 0)
            return;

        if(attributesSize == 1) {
            sb.append(" ");
            onAttribute(xmlNodeStartTag.getAttributes().value()[0]);
        } else {
            sb.append("\n");
            for (int i = 0; i < attributesSize; i++) {
                appendShift(shift);
                onAttribute(xmlNodeStartTag.getAttributes().value()[i]);
                if(i != xmlNodeStartTag.getAttributes().size() -1)
                    sb.append("\n");
            }
        }


//        for (Attribute attribute : xmlNodeStartTag.getAttributes().value()) {
//            onAttribute(attribute);
//
//        }
    }

    private void onAttribute(Attribute attribute) {
        String namespace = this.namespaces.getPrefixViaUri(attribute.getNamespace());
        if (namespace == null) {
            namespace = attribute.getNamespace();
        }
        if (namespace != null && !namespace.isEmpty()) {
            sb.append(namespace).append(':');
        }
        String escapedFinalValue = XmlEscaper.escapeXml10(attribute.getValue());
        sb.append(attribute.getName()).append('=').append('"')
                .append(escapedFinalValue).append('"');
    }

    @Override
    public void onEndTag(XmlNodeEndTag xmlNodeEndTag) {//TODO \n
        --shift;
        if (isLastStartTag) {
            sb.append(" />\n\n");
        } else {
            appendShift(shift);
            sb.append("</");
            if (xmlNodeEndTag.getNamespace() != null) {
                sb.append(xmlNodeEndTag.getNamespace()).append(":");
            }
            sb.append(xmlNodeEndTag.getName());
            sb.append(">\n\n");
        }
        isLastStartTag = false;
    }


    @Override
    public void onCData(XmlCData xmlCData) {
        appendShift(shift);
        sb.append(xmlCData.getValue()).append('\n');
        isLastStartTag = false;
    }

    @Override
    public void onNamespaceStart(XmlNamespaceStartTag tag) {
        this.namespaces.addNamespace(tag);
    }

    @Override
    public void onNamespaceEnd(XmlNamespaceEndTag tag) {
        this.namespaces.removeNamespace(tag);
    }

    private void appendShift(int shift) {
        for (int i = 0; i < shift; i++) {
            sb.append("\t");
        }
    }

    public String getXml() {
        return sb.toString();
    }
}
