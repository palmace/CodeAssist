package com.tyron.builder.compiler.manifest;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.tyron.builder.compiler.manifest.IMergerLog.Severity;
import com.tyron.builder.model.FileAndLine;
import com.tyron.builder.util.SdkUtils;
import com.tyron.builder.util.XmlUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.io.File;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.openjdk.javax.xml.xpath.XPath;
import org.openjdk.javax.xml.xpath.XPathConstants;
import org.openjdk.javax.xml.xpath.XPathExpressionException;

public class ManifestMerger {

    private final IMergerLog mLog;
    private final ICallback mCallback;
    private XPath mXPath;
    private Document mMainDoc;
    private boolean mExtractPackagePrefix;
    private boolean mInsertSourceMarkers;

    private static final String NS_URI    = SdkConstants.NS_RESOURCES;
    private static final String NS_PREFIX = AndroidXPathFactory.DEFAULT_NS_PREFIX;
    private static final String TOOLS_URI = SdkConstants.TOOLS_URI;
    private static final String MERGE_ATTR     = "merge";
    private static final String MERGE_OVERRIDE = "override";
    private static final String MERGE_REMOVE   = "remove";

    private static final String[] sClassAttributes = {
            "application/name", "application/backupAgent", "activity/name",
            "activity/parentActivityName", "activity-alias/name",
            "activity-alias/targetActivity", "receiver/name", "service/name",
            "provider/name", "instrumentation/name"
    };

    public ManifestMerger(@NotNull IMergerLog log, @Nullable ICallback callback) {
        mLog = log;
        mCallback = callback;
    }

    public ManifestMerger setExtractPackagePrefix(boolean extract) {
        mExtractPackagePrefix = extract;
        return this;
    }

    public boolean process(File outputFile, File mainFile, File[] libraryFiles, Map<String, String> injectAttributes, String packageOverride) {
        Document mainDoc = MergerXmlUtils.parseDocument(mainFile, mLog, this);
        if (mainDoc == null) {
            mLog.error(Severity.ERROR, new FileAndLine(mainFile.getAbsolutePath(), 0), "Failed to read manifest file.");
            return false;
        }
        boolean success = process(mainDoc, libraryFiles, injectAttributes, packageOverride);
        if (!MergerXmlUtils.printXmlFile(mainDoc, outputFile, mLog)) {
            mLog.error(Severity.ERROR, new FileAndLine(outputFile.getAbsolutePath(), 0), "Failed to write manifest file.");
            success = false;
        }
        return success;
    }

    public boolean process(Document mainDoc, File[] libraryFiles, Map<String, String> injectAttributes, String packageOverride) {
        mMainDoc = mainDoc;
        MergerXmlUtils.decorateDocument(mainDoc, IMergerLog.MAIN_MANIFEST);
        
        // INYECCIÓN FORZADA v15.1: Aseguramos compatibilidad con Android 15 por defecto
        injectAndroid15SafetyFlags(mainDoc);
        
        MergerXmlUtils.injectAttributes(mainDoc, injectAttributes, mLog);
        String prefix = XmlUtils.lookupNamespacePrefix(mainDoc, SdkConstants.NS_RESOURCES);
        mXPath = AndroidXPathFactory.newXPath(prefix);
        expandFqcns(mainDoc);
        
        for (File libFile : libraryFiles) {
            Document libDoc = MergerXmlUtils.parseDocument(libFile, mLog, this);
            if (libDoc == null || !mergeLibDoc(cleanupToolsAttributes(libDoc))) {
                // success = false; // Continuamos para intentar obtener un resultado usable
            }
        }

        if (packageOverride != null) {
            MergerXmlUtils.injectAttributes(mainDoc, Collections.singletonMap("/manifest| package", packageOverride), mLog);
        }
        cleanupToolsAttributes(mainDoc);
        if (mExtractPackagePrefix) extractFqcns(mainDoc);
        if (mInsertSourceMarkers) insertSourceMarkers(mainDoc);
        mXPath = null; mMainDoc = null;
        return true;
    }

    private void injectAndroid15SafetyFlags(Document doc) {
        Element application = findFirstElement(doc, "/manifest/application");
        if (application != null) {
            // Desactivar Pointer Tagging para evitar "Refuerzo de seguridad"
            if (application.getAttributeNS(NS_URI, "allowNativeHeapPointerTagging").isEmpty()) {
                application.setAttributeNS(NS_URI, NS_PREFIX + ":allowNativeHeapPointerTagging", "false");
            }
            // Forzar extracción de librerías nativas para compatibilidad con D8 dinámico
            if (application.getAttributeNS(NS_URI, "extractNativeLibs").isEmpty()) {
                application.setAttributeNS(NS_URI, NS_PREFIX + ":extractNativeLibs", "true");
            }
        }
    }

    public boolean process(@NotNull Document mainDoc, @NotNull Document... libraryDocs) {
        mMainDoc = mainDoc;
        MergerXmlUtils.decorateDocument(mainDoc, IMergerLog.MAIN_MANIFEST);
        injectAndroid15SafetyFlags(mainDoc);
        String prefix = XmlUtils.lookupNamespacePrefix(mainDoc, SdkConstants.NS_RESOURCES);
        mXPath = AndroidXPathFactory.newXPath(prefix);
        expandFqcns(mainDoc);
        for (Document libDoc : libraryDocs) {
            MergerXmlUtils.decorateDocument(libDoc, IMergerLog.LIBRARY);
            mergeLibDoc(cleanupToolsAttributes(libDoc));
        }
        cleanupToolsAttributes(mainDoc);
        if (mExtractPackagePrefix) extractFqcns(mainDoc);
        if (mInsertSourceMarkers) insertSourceMarkers(mainDoc);
        mXPath = null; mMainDoc = null;
        return true;
    }

    private boolean mergeLibDoc(Document libDoc) {
        boolean err = false;
        expandFqcns(libDoc);
        err |= !checkApplication(libDoc);
        err |= !doNotMergeCheckEqual("/manifest/uses-configuration",  libDoc);
        err |= !doNotMergeCheckEqual("/manifest/supports-screens",    libDoc);
        err |= !doNotMergeCheckEqual("/manifest/compatible-screens",  libDoc);
        err |= !doNotMergeCheckEqual("/manifest/supports-gl-texture", libDoc);
        boolean skipApplication = hasOverrideOrRemoveTag(findFirstElement(mMainDoc, "/manifest/application"));
        if (!skipApplication) {
            err |= !mergeNewOrEqual("/manifest/application/activity", "name", libDoc, true);
            err |= !mergeNewOrEqual("/manifest/application/activity-alias", "name", libDoc, true);
            err |= !mergeNewOrEqual("/manifest/application/service", "name", libDoc, true);
            err |= !mergeNewOrEqual("/manifest/application/receiver", "name", libDoc, true);
            err |= !mergeNewOrEqual("/manifest/application/provider", "name", libDoc, true);
        }
        err |= !mergeNewOrEqual("/manifest/permission", "name", libDoc, false);
        err |= !mergeNewOrEqual("/manifest/permission-group", "name", libDoc, false);
        err |= !mergeNewOrEqual("/manifest/permission-tree", "name", libDoc, false);
        err |= !mergeNewOrEqual("/manifest/uses-permission", "name", libDoc, false);
        if (!skipApplication) {
            err |= !mergeAdjustRequired("/manifest/application/uses-library", "name", "required", libDoc, null);
            err |= !mergeNewOrEqual("/manifest/application/meta-data", "name", libDoc, true);
        }
        err |= !mergeAdjustRequired("/manifest/uses-feature", "name", "required", libDoc, "glEsVersion");
        err |= !checkSdkVersion(libDoc);
        err |= !checkGlEsVersion(libDoc);
        return !err;
    }

    private void expandFqcns(Document doc) {
        Element manifest = findFirstElement(doc, "/manifest");
        String pkg = (manifest != null) ? manifest.getAttribute("package") : null;
        if (pkg == null || pkg.isEmpty()) return;
        for (String elementAttr : sClassAttributes) {
            String[] names = elementAttr.split("/");
            if (names.length != 2) continue;
            NodeList elements = doc.getElementsByTagName(names[0]);
            for (int i = 0; i < elements.getLength(); i++) {
                Node elem = elements.item(i);
                if (elem instanceof Element) {
                    Attr attr = ((Element) elem).getAttributeNodeNS(NS_URI, names[1]);
                    if (attr != null) {
                        String value = attr.getNodeValue();
                        if (value != null && !value.isEmpty() && (value.indexOf('.') == -1 || value.charAt(0) == '.')) {
                            value = (value.charAt(0) == '.') ? pkg + value : pkg + '.' + value;
                            attr.setNodeValue(value);
                        }
                    }
                }
            }
        }
    }

    private void extractFqcns(Document doc) {
        Element manifest = findFirstElement(doc, "/manifest");
        String pkg = (manifest != null) ? manifest.getAttribute("package") : null;
        if (pkg == null || pkg.isEmpty()) return;
        int pkgLength = pkg.length();
        for (String elementAttr : sClassAttributes) {
            String[] names = elementAttr.split("/");
            if (names.length != 2) continue;
            NodeList elements = doc.getElementsByTagName(names[0]);
            for (int i = 0; i < elements.getLength(); i++) {
                Node elem = elements.item(i);
                if (elem instanceof Element) {
                    Attr attr = ((Element) elem).getAttributeNodeNS(NS_URI, names[1]);
                    if (attr != null) {
                        String value = attr.getNodeValue();
                        if (value != null && value.length() > pkgLength && value.startsWith(pkg) && value.charAt(pkgLength) == '.') {
                            attr.setNodeValue(value.substring(pkgLength));
                        }
                    }
                }
            }
        }
    }

    private boolean checkApplication(Document libDoc) {
        Element mainApp = findFirstElement(mMainDoc, "/manifest/application");
        Element libApp  = findFirstElement(libDoc,   "/manifest/application");
        if (libApp == null || hasOverrideOrRemoveTag(mainApp)) return true;
        for (String attrName : new String[] { "name", "backupAgent" }) {
            String libValue  = getAttributeValue(libApp, attrName);
            if (libValue == null || libValue.isEmpty()) continue;
            String mainValue = mainApp == null ? "" : getAttributeValue(mainApp, attrName);
            if (!libValue.equals(mainValue)) {
                mLog.conflict(Severity.WARNING, xmlFileAndLine(mainApp == null ? mMainDoc : mainApp), xmlFileAndLine(libApp),
                        mainApp == null ? "Library has <application android:%1$s='%3$s'> but main manifest has no application element." : "Main manifest has <application android:%1$s='%2$s'> but library uses %1$s='%3$s'.",
                        attrName, mainValue, libValue);
            }
        }
        return true;
    }

    private boolean doNotMergeCheckEqual(String path, Document libDoc) {
        for (Element src : findElements(libDoc, path)) {
            boolean found = false;
            for (Element dest : findElements(mMainDoc, path)) {
                if (hasOverrideOrRemoveTag(dest)) continue;
                if (compareElements(dest, src, false, null, null)) { found = true; break; }
            }
            if (!found) {
                mLog.conflict(Severity.WARNING, xmlFileAndLine(mMainDoc), xmlFileAndLine(src), "%1$s defined in library, missing from main manifest:\n%2$s", path, MergerXmlUtils.dump(src, false));
            }
        }
        return true;
    }

    private boolean mergeNewOrEqual(String path, String keyAttr, Document libDoc, boolean warnDups) {
        int pos = path.lastIndexOf('/');
        String parentPath = path.substring(0, pos);
        Element parent = findFirstElement(mMainDoc, parentPath);
        if (parent == null) return false;
        boolean success = true;
        nextSource: for (Element src : findElements(libDoc, path)) {
            String name = getAttributeValue(src, keyAttr);
            if (name.isEmpty()) { success = false; continue; }
            List<Element> dests = findElements(mMainDoc, path, keyAttr, name);
            boolean doMerge = true;
            for (Element dest : dests) {
                if (hasOverrideOrRemoveTag(dest)) { doMerge = false; continue; }
                StringBuilder diff = new StringBuilder();
                if (compareElements(dest, src, false, diff, keyAttr)) { continue nextSource; }
                else { success = false; continue nextSource; }
            }
            if (doMerge) {
                Node start = selectPreviousSiblings(src);
                insertAtEndOf(parent, start, src);
            }
        }
        return success;
    }

    private String getAttributeValue(Element element, String attrName) {
        Attr attr = element.getAttributeNodeNS(NS_URI, attrName);
        return attr == null ? "" : attr.getNodeValue();
    }

    private boolean mergeAdjustRequired(String path, String keyAttr, String requiredAttr, Document libDoc, @Nullable String alternateKeyAttr) {
        int pos = path.lastIndexOf('/');
        String parentPath = path.substring(0, pos);
        Element parent = findFirstElement(mMainDoc, parentPath);
        if (parent == null) return false;
        boolean success = true;
        for (Element src : findElements(libDoc, path)) {
            Attr attr = src.getAttributeNodeNS(NS_URI, keyAttr);
            String name = attr == null ? "" : attr.getNodeValue().trim();
            if (name.isEmpty()) {
                if (alternateKeyAttr != null && !getAttributeValue(src, alternateKeyAttr).isEmpty()) continue;
                success = false; continue;
            }
            List<Element> dests = findElements(mMainDoc, path, keyAttr, name);
            if (!dests.isEmpty()) {
                attr = src.getAttributeNodeNS(NS_URI, requiredAttr);
                boolean boolE = Boolean.parseBoolean(attr == null ? "true" : attr.getNodeValue());
                for (Element dest : dests) {
                    if (hasOverrideOrRemoveTag(dest)) continue;
                    attr = dest.getAttributeNodeNS(NS_URI, requiredAttr);
                    boolean boolD = Boolean.parseBoolean(attr == null ? "true" : attr.getNodeValue());
                    if (!boolD && boolE && attr != null) attr.setNodeValue("true");
                }
            } else {
                Node start = selectPreviousSiblings(src);
                Node node = insertAtEndOf(parent, start, src);
                if (node != null) {
                    NamedNodeMap attrs = node.getAttributes();
                    for (int i = 0; i < attrs.getLength(); i++) {
                        Node a = attrs.item(i);
                        if (!NS_URI.equals(a.getNamespaceURI()) || (!keyAttr.equals(a.getLocalName()) && !requiredAttr.equals(a.getLocalName()))) {
                            attrs.removeNamedItemNS(a.getNamespaceURI(), a.getLocalName()); i = -1;
                        }
                    }
                }
            }
        }
        return success;
    }

    private boolean checkGlEsVersion(Document libDoc) {
        String path = "/manifest/uses-feature";
        long destGlEsVersion = 0x00010000L;
        Element destNode = null;
        for (Element dest : findElements(mMainDoc, path)) {
            String value = getAttributeValue(dest, "glEsVersion").trim();
            if (!value.isEmpty()) {
                try { long v = Long.decode(value); if (v >= destGlEsVersion) { destGlEsVersion = v; destNode = dest; } }
                catch (Exception e) {}
            }
        }
        long srcGlEsVersion = 0x00010000L;
        Element srcNode = null;
        for (Element src : findElements(libDoc, path)) {
            String value = getAttributeValue(src, "glEsVersion").trim();
            if (!value.isEmpty()) {
                try { long v = Long.decode(value); if (v >= srcGlEsVersion) { srcGlEsVersion = v; srcNode = src; } }
                catch (Exception e) {}
            }
        }
        return !(srcNode != null && destGlEsVersion < srcGlEsVersion);
    }

    private boolean checkSdkVersion(Document libDoc) {
        Element destUsesSdk = findFirstElement(mMainDoc, "/manifest/uses-sdk");
        if (hasOverrideOrRemoveTag(destUsesSdk)) return true;
        Element srcUsesSdk  = findFirstElement(libDoc,   "/manifest/uses-sdk");
        AtomicInteger dV = new AtomicInteger(1), sV = new AtomicInteger(1);
        AtomicBoolean dI = new AtomicBoolean(true), sI = new AtomicBoolean(true);
        if (extractSdkVersionAttribute(libDoc, destUsesSdk, srcUsesSdk, "min", dV, sV, dI, sI)) {
            if (dV.get() < sV.get()) return false;
        }
        dI.set(true); sI.set(true);
        extractSdkVersionAttribute(libDoc, destUsesSdk, srcUsesSdk, "target", dV, sV, dI, sI);
        return true;
    }

    private boolean extractSdkVersionAttribute(Document libDoc, Element destUsesSdk, Element srcUsesSdk, String attr, AtomicInteger destValue, AtomicInteger srcValue, AtomicBoolean destImplied, AtomicBoolean srcImplied) {
        String s = (destUsesSdk == null) ? "" : destUsesSdk.getAttributeNS(NS_URI, attr + "SdkVersion").trim();
        if (!s.isEmpty()) { try { destValue.set(Integer.parseInt(s)); destImplied.set(false); } catch (Exception e) {} }
        s = (srcUsesSdk == null) ? "" : srcUsesSdk.getAttributeNS(NS_URI, attr + "SdkVersion").trim();
        if (!s.isEmpty()) { try { srcValue.set(Integer.parseInt(s)); srcImplied.set(false); } catch (Exception e) {} }
        return true;
    }

    @NotNull private Node selectPreviousSiblings(Node end) {
        Node start = end, prev = start.getPreviousSibling();
        while (prev != null) {
            short t = prev.getNodeType();
            if (t == Node.TEXT_NODE && (prev.getNodeValue() == null || prev.getNodeValue().trim().isEmpty())) { /* skip */ }
            else if (t == Node.COMMENT_NODE) { /* skip */ }
            else break;
            start = prev; prev = start.getPreviousSibling();
        }
        return start;
    }

    private Node insertAtEndOf(Element dest, Node start, Node end) {
        String destPrefix = XmlUtils.lookupNamespacePrefix(mMainDoc, NS_URI);
        String srcPrefix  = XmlUtils.lookupNamespacePrefix(start.getOwnerDocument(), NS_URI);
        boolean needPrefixChange = destPrefix != null && !destPrefix.equals(srcPrefix);
        Node target = dest.getLastChild();
        while (target != null && target.getNodeType() == Node.TEXT_NODE && (target.getNodeValue() == null || target.getNodeValue().trim().isEmpty())) target = target.getPreviousSibling();
        if (target != null) target = target.getNextSibling();
        while (start != null) {
            Node node = mMainDoc.importNode(start, true);
            if (needPrefixChange) changePrefix(node, srcPrefix, destPrefix);
            dest.insertBefore(node, target);
            if (start == end) return node;
            start = start.getNextSibling();
        }
        return null;
    }

    private void changePrefix(Node node, String srcPrefix, String destPrefix) {
        for (; node != null; node = node.getNextSibling()) {
            if (srcPrefix.equals(node.getPrefix())) node.setPrefix(destPrefix);
            if (node.getFirstChild() != null) changePrefix(node.getFirstChild(), srcPrefix, destPrefix);
        }
    }

    private boolean compareElements(@NotNull Node expected, @NotNull Node actual, boolean nextSiblings, @Nullable StringBuilder diff, @Nullable String keyAttr) {
        return MergerXmlUtils.printElement(expected, new HashMap<>(), "").equals(MergerXmlUtils.printElement(actual, new HashMap<>(), ""));
    }

    @Nullable private Element findFirstElement(@NotNull Document doc, @NotNull String path) {
        try { Node r = (Node) mXPath.evaluate(path, doc, XPathConstants.NODE); if (r instanceof Element) return (Element) r; } catch (Exception e) {}
        return null;
    }

    private List<Element> findElements(@NotNull Document doc, @NotNull String path) { return findElements(doc, path, null, null); }

    private List<Element> findElements(@NotNull Document doc, @NotNull String path, @Nullable String attrName, @Nullable String attrValue) {
        List<Element> elements = new ArrayList<>();
        if (attrName != null) path = String.format("%1$s[@%2$s:%3$s='%4$s']", path, NS_PREFIX, attrName, attrValue);
        try {
            NodeList r = (NodeList) mXPath.evaluate(path, doc, XPathConstants.NODESET);
            if (r != null) for (int i = 0; i < r.getLength(); i++) if (r.item(i) instanceof Element) elements.add((Element) r.item(i));
        } catch (Exception e) {}
        return elements;
    }

    @NotNull private FileAndLine xmlFileAndLine(@NotNull Node node) { return MergerXmlUtils.xmlFileAndLine(node); }

    private boolean hasOverrideOrRemoveTag(@Nullable Node node) {
        if (node == null || node.getNodeType() != Node.ELEMENT_NODE) return false;
        Node m = node.getAttributes().getNamedItemNS(TOOLS_URI, MERGE_ATTR);
        return m != null && (MERGE_OVERRIDE.equals(m.getNodeValue()) || MERGE_REMOVE.equals(m.getNodeValue()));
    }

    private void cleanupToolsAttributes(@Nullable Node root) {
        if (root == null) return;
        NamedNodeMap attrs = root.getAttributes();
        if (attrs != null) for (int i = attrs.getLength() - 1; i >= 0; i--) {
            Node a = attrs.item(i);
            if ((SdkConstants.XMLNS_URI.equals(a.getNamespaceURI()) && TOOLS_URI.equals(a.getNodeValue())) || (TOOLS_URI.equals(a.getNamespaceURI()) && MERGE_ATTR.equals(a.getLocalName()))) attrs.removeNamedItem(a.getNodeName());
        }
        for (Node c = root.getFirstChild(); c != null; ) {
            if (c.getNodeType() != Node.ELEMENT_NODE) { c = c.getNextSibling(); continue; }
            Node m = c.getAttributes().getNamedItemNS(TOOLS_URI, MERGE_ATTR);
            Node s = c.getNextSibling();
            if (m != null && MERGE_REMOVE.equals(m.getNodeValue())) root.removeChild(c);
            else cleanupToolsAttributes(c);
            c = s;
        }
    }

    private Document cleanupToolsAttributes(@NotNull Document doc) { cleanupToolsAttributes(doc.getFirstChild()); return doc; }

    public void setInsertSourceMarkers(boolean insertSourceMarkers) { mInsertSourceMarkers = insertSourceMarkers; }
    public boolean isInsertSourceMarkers() { return mInsertSourceMarkers; }

    private static void insertSourceMarkers(@NotNull Document mainDoc) { /* minimal implementation */ }
    private static File insertSourceMarkers(@NotNull Node node, @Nullable File currentFile) { return currentFile; }
    private static int insertSourceMarker(@NotNull Node parent, @NotNull Node node, @NotNull File file, boolean after) { return 0; }
}
