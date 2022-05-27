/*
 * Copyright 2021. ImproveIt Oy
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package fi.improveit.req_ex;

import com.starbase.caliber.*;
import com.starbase.caliber.attribute.*;
import com.starbase.caliber.server.ObjectDoesNotExistException;
import com.starbase.caliber.server.RemoteServerException;
import com.starbase.caliber.util.HTMLHelper;
import com.starbase.caliber.util.UnicodeHelper;
import org.xml.sax.SAXException;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static fi.improveit.req_ex.ReqIFXHTML.htmlToXhtml;
import static fi.improveit.req_ex.RoundTripGUI.theGUI;

public class ReqIFExportType extends Export {

    private static final String CALIBER_ID_ATTRIBUTE = "ReqIF.ForeignID";
    private static final String CALIBER_NAME_ATTRIBUTE = "ReqIF.Name";
    private static final String CALIBER_DESCRIPTION_ATTRIBUTE = "ReqIF.Description";
    private static final String CALIBER_VERSION_ATTRIBUTE = "ReqIF.ForeignRevision";
    private static final String CALIBER_CHANGED_ON_ATTRIBUTE = "ReqIF.ForeignModifiedAt";
    private static final String CALIBER_CHANGED_BY_ATTRIBUTE = "ReqIF.ForeignModifiedBy";
    private static final String CALIBER_CREATED_ON_ATTRIBUTE = "ReqIF.ForeignCreatedAt";
    private static final String CALIBER_CREATED_BY_ATTRIBUTE = "ReqIF.ForeignCreatedBy";
    private static final String CALIBER_OWNER_ATTRIBUTE = "ReqIF.ForeignOwner";

    protected static final Logger logger = LoggerFactory.getLogger(ReqIFExportType.class.getName());
    protected ReqIFFile of;
    protected HashSet<Attribute> dataTypeAttributes;            // hash of attributes already listed to avoid dupes
    protected HashSet<String> exportFiles;                      // set of files for the zip archive
    private boolean reverse_traces;                             // option for reversing trace direction globally
    private boolean include_type_attr;                          // option for type attribute (enables folder identification)
    private HashSet<String> statusValues;
    private HashSet<String> priorityValues;
    final static String[] TYPE_VALUES =
            {"Folder", "Functional"};

    // protected HashMap<String, HashSet<String>> relationGroups;

    public ReqIFExportType(CaliberSession s, String p, String reqt, String od, int maxReq, ExportPanel ep, ProgressPanel pp) throws ExportException {
        super(s, p, reqt, od, maxReq, ep, pp);
        dataTypeAttributes = new HashSet<>();
        relationGroups = new HashMap<>();
        exportFiles = new HashSet<>();
        if ("true".equals(Defaults.get("reverse_traces"))) {
            reverse_traces = true;
        }
        if ("true".equals(Defaults.get("include_type_attr"))) {
            include_type_attr = true;
        }
        logger.info("Reverse traces: {}", reverse_traces);
    }

    private static String ownerFullName(Requirement r) throws RemoteServerException {
        return r.getOwner().getFirstName() + " " + r.getOwner().getLastName();
    }

    public void init() throws ExportException {
        // set project and type
        try {
            // open export file
            of = new ReqIFFile(outputDirectory, project, reqType);
            // open the file and write header
            of.open();
            of.writeHeader(cs);
            of.startContent();
        } catch (XMLStreamException | IOException | ExcelFileException | CaliberException ex) {
            throw new ExportException(ex);
        }
    }

    // if requested, package reqif (xml) file and .png image file in the output directory to one .reqifz zip archive
    public void finish() throws ExportException {
        try {
            of.close();
            if (ep.isReqIFZ()) {
                String reqIFFilename = of.filename();
                exportFiles.add(reqIFFilename);

                String dir = ep.getDirectory();
                String zipFilePath = dir + "\\" + of.filename() + "z";
                logger.info("Adding files to the reqifz archive: {}", zipFilePath);
                File check = new File(zipFilePath);
                if (check.exists())
                    logger.warn("Overwriting existing reqifz file: {}", zipFilePath);

                FileOutputStream fos = new FileOutputStream(zipFilePath);
                ZipOutputStream zipOut = new ZipOutputStream(fos);
                for (String srcFile : exportFiles) {
                    File fileToZip = new File(dir + "\\" + srcFile);
                    FileInputStream fis = new FileInputStream(fileToZip);
                    ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
                    zipOut.putNextEntry(zipEntry);

                    byte[] bytes = new byte[1024];
                    int length;
                    while((length = fis.read(bytes)) >= 0) {
                        zipOut.write(bytes, 0, length);
                    }
                    fis.close();
                }
                zipOut.close();
                fos.close();
            }
        } catch (XMLStreamException | IOException ex) {
            logger.error("Exception: {}", ex.getMessage());
            throw new ExportException(ex);
        }
    }

    // replace illegal characters from the requirement type tag
    // so that it is a valid XML id
    private String fixTag(String tag) {
        return tag.replaceAll(" ", "-")
                .replaceAll("/", "_")
                .replaceAll("&", ".");
    }

    public void exportDefinitions() throws ExportException {
        try {
            of.startDataTypes();
            exportSystemDataTypes();
            pp.addText("System data types exported.");
            exportDataTypes(rt);
            pp.addText("User defined data types exported.");
            of.endDataTypes();

            of.startSpecTypes();
            exportSpecType(rt);
            // Constant Relation Type (Trace)
            of.writeSpecRelationType();
            of.writeRelationGroupType();
            of.endSpecTypes();

        } catch (XMLStreamException | CaliberException | RemoteServerException ex) {
            throw new ExportException(ex);
        }
    }

    // Export data types related to system attributes
    protected void exportSystemDataTypes() throws XMLStreamException, CaliberException, RemoteServerException {
        logger.info("Exporting data types of system attributes");
        // Built-in standard data types
        of.writeStringDataType("String", 128);
        of.writeXHTMLDataType("XHTML");
        of.writeBooleanDataType();
        of.writeDateDataType();

        getUsedStatusValues();
        of.writeEnumDataType("Status", statusValues.toArray(new String[0]));
        of.writeEnumDataType("Priority", priorityValues.toArray(new String[0]));
/*
        for (Attribute a : cs.getAttributes()) {
            if (a instanceof UDAList) {
                if (a.getName().equals("Requirement Status"))
                    of.writeEnumDataType("Status", (UDAList) a);
                if (a.getName().equals("Requirement Priority"))
                    of.writeEnumDataType("Priority", (UDAList) a);
            }
        }
*/
        if (include_type_attr) {
            of.writeEnumDataType("Type", TYPE_VALUES);
        }
    }

    // Returns all *used* Status attribute values of the current requirement type or the whole project
    protected void getUsedStatusValues() throws RemoteServerException, CaliberException {
        statusValues = new HashSet<>();
        priorityValues = new HashSet<>();

        if (rt == null) {       // project export - iterate over all types
            for (RequirementType t : cs.getTypes()) {
                logger.info("Getting used status and priority values for type {}", t.getName());
                for (Requirement r : t.getRequirements(cs.getBaseline())) {
                    getStatusAndPriorityValues(r);
                }
            }
        } else {                // single type export
            logger.info("Getting used status and priority values for single type {}", rt.getName());
            for (Requirement r : rt.getRequirements(cs.getBaseline())) {
                getStatusAndPriorityValues(r);
            }
        }
        logger.info("Used status values: {}", statusValues.toString());
        logger.info("Used priority values: {}", priorityValues.toString());
    }

    private void getStatusAndPriorityValues(Requirement r) throws RemoteServerException {
        String s = r.getStatus().getSelectedValue().toString();
        statusValues.add(s);
        s = r.getPriority().getSelectedValue().toString();
        priorityValues.add(s);
        // recursion for the children
        for (Requirement c : r.getChildRequirements()) {
            getStatusAndPriorityValues(c);
        }
    }

    // Export data types for user defined attributes of the Caliber requirement type
    protected void exportDataTypes(RequirementType t) throws XMLStreamException, RemoteServerException {
        logger.info("Exporting data types of requirement type: {}", t.getName());
        for (Attribute a : t.getAttributes()) {
            // with project export we check if datatype is already exported
            if (!dataTypeAttributes.contains(a)) {
                if (a instanceof UDAFloat) {
                    of.writeRealDataType((UDAFloat) a);
                }
                if (a instanceof UDAInteger) {
                    of.writeIntegerDataType((UDAInteger) a);
                }
                if (a instanceof UDAText) {
                    of.writeStringDataType((UDAText) a);
                }
                if (a instanceof UDAList) {
                    of.writeEnumDataType(a.getName(), (UDAList) a);
                }
                dataTypeAttributes.add(a);
            }
        }
    }

    // Export spec-type of a Requirement type
    protected void exportSpecType(RequirementType t) throws XMLStreamException, RemoteServerException {
        logger.info("Exporting requirement type: {}", t.getName());
        String tag = fixTag(t.getTag());
        of.writeSpecObjectTypeWithAttributes(t.getName(), tag, t.getDescription());
        of.writeStringAttribute(tag, CALIBER_NAME_ATTRIBUTE, "The name the requirement.");
        of.writeStringAttribute(tag, CALIBER_ID_ATTRIBUTE, "The tag and ID of the requirement.");
        of.writeStringAttribute(tag, CALIBER_VERSION_ATTRIBUTE, "The requirement's version.");
        of.writeDateAttribute(tag, CALIBER_CHANGED_ON_ATTRIBUTE, "Time and date of last change.");
        of.writeStringAttribute(tag, CALIBER_CHANGED_BY_ATTRIBUTE, "User who made the last change.");
        of.writeDateAttribute(tag, CALIBER_CREATED_ON_ATTRIBUTE, "Time and date when created.");
        of.writeStringAttribute(tag, CALIBER_CREATED_BY_ATTRIBUTE, "User who created the requirement.");
        of.writeStringAttribute(tag, CALIBER_OWNER_ATTRIBUTE, "The requirement's owner.");
        of.writeEnumAttribute(tag, "Status", "The requirement's status.", false);
        of.writeEnumAttribute(tag, "Priority", "The requirement's priority.", false);
        if (include_type_attr) {
            of.writeEnumAttribute(tag, "Type",
                    "Requirement nodes used as folders are indicated as Type=Folder", false);
        }
        of.writeXHTMLAttribute(tag, CALIBER_DESCRIPTION_ATTRIBUTE, "The description text of the requirement.");
        for (Attribute a : t.getAttributes()) {
            of.writeUDAAttribute(tag, a);
        }
        of.endSpecType();
        // Specification (=Listing) for the type
        of.writeSpecificationType(t.getDescription(), tag, t.getName());
    }

    public void exportRequirements() throws ExportException {
        try {
            of.startSpecObjects();
            exportSingleTypeRequirements(rt);
            of.endSpecObjects();
        } catch (XMLStreamException ex) {
            throw new ExportException(ex);
        }
    }

    protected void exportSingleTypeRequirements(RequirementType t) throws ExportException {
        try {
            for (Requirement r : t.getRequirements(cs.getBaseline())) {
                exportRequirement(r);
                currFirstLevelCount++;
                theGUI.progressPanel.setProgress(20 + (int) ((currFirstLevelCount * 60.0) / firstLevelReqCount));
            }
        } catch (XMLStreamException | IOException | RemoteServerException | SAXException ex) {
            throw new ExportException(ex);
        }
    }

    // Export Requirement, type and attribute values
    // Recursively exports children
    private void exportRequirement(Requirement r)
            throws XMLStreamException, IOException, RemoteServerException, SAXException, ExportException {
        if (currReqCount == maxReqCount)
            throw new ExportException("Req count limit encountered.");
        else {
            currReqCount++;
            SummaryPanel.exported++;

            logger.info("Exporting requirement: {}", r.getName());

            String tag = fixTag(r.getRequirementType().getTag());
            String ID = tag + r.getIDNumber();

            logger.info("Requirement ID: {}", ID);

            // Get creation time & created by
            HistoryRevision[] rev = r.getHistory().getRevisions();
            Date creationTime = rev[0].getDate();
            User createdByUser = rev[0].getUser();
            String createdBy = createdByUser.getFirstName() + " " + createdByUser.getLastName();

            // Get last modification time & changed by
            int last = rev.length - 1;
            Date modTime = rev[last].getDate();
            User user = rev[last].getUser();
            String changedBy = user.getFirstName() + " " + user.getLastName();

            // Get version
            String versionString;
            Version v = r.getVersion();
            versionString = v.getMajorVersion() + "." + v.getMinorVersion();

            // Output title data & type
            of.writeSpecObject(ID, dateToISO(modTime), r.getName(), tag);

            // Output system attribute values
            of.startValues();
            of.writeStringAttributeValue(tag, CALIBER_NAME_ATTRIBUTE, r.getName());
            of.writeStringAttributeValue(tag, CALIBER_ID_ATTRIBUTE, ID);
            of.writeStringAttributeValue(tag, CALIBER_VERSION_ATTRIBUTE, versionString);
            of.writeDateAttributeValue(tag, CALIBER_CHANGED_ON_ATTRIBUTE, modTime);
            of.writeStringAttributeValue(tag, CALIBER_CHANGED_BY_ATTRIBUTE, changedBy);
            of.writeDateAttributeValue(tag, CALIBER_CREATED_ON_ATTRIBUTE, creationTime);
            of.writeStringAttributeValue(tag, CALIBER_CREATED_BY_ATTRIBUTE, createdBy);
            of.writeStringAttributeValue(tag, CALIBER_OWNER_ATTRIBUTE, ownerFullName(r));
            of.writeEnumAttributeValue(tag, "Status", r.getStatus().getSelectedValue().toString());
            of.writeEnumAttributeValue(tag, "Priority", r.getPriority().getSelectedValue().toString());

            if (include_type_attr) {
                String type;
                if (r.getChildRequirements().length > 0)
                    type = "Folder";
                else
                    type = "Functional";
                of.writeEnumAttributeValue(tag, "Type", type);
            }

            // Output description
            String orig = r.getDescription().getText();
            logger.info("Original Description HTML: {}", orig);
            logger.info("isUTF8: {}", UnicodeHelper.isUTF8(orig));
            String xhtml;
            if (ep.isPlaintext()) {
                xhtml = toPlainText(orig);
                logger.info("Plaintext Description: {}", xhtml);
            }
            else {
                xhtml = htmlToXhtml(this, orig);
                logger.info("Fixed Description XHTML: {}", xhtml);
            }

            of.writeXHTMLAttributeValue(tag, CALIBER_DESCRIPTION_ATTRIBUTE, xhtml);

            // Output UDA values
            for (Attribute a : r.getRequirementType().getAttributes()) {
                for (AttributeValue av : r.getAttributeValues()) {
                    if (av.getAttribute().equals(a))
                        if (a instanceof UDAList)
                            if (((UDAList) a).getMaximumSelections() == 1)
                                of.writeEnumAttributeValue(tag, a.getName(), (new RValue(av)).getValue());
                            else
                                of.writeEnumAttributeValues(tag, a.getName(), (new RValue(av)).getValues());
                        else
                            of.writeScalarAttributeValue(tag, a, (new RValue(av)).getValue());
                }
            }
            of.endValues();
            of.endSpecObject();

            // recursion for the children
            for (Requirement c : r.getChildRequirements()) {
                exportRequirement(c);
            }
        }
    }

    protected static String dateToISO(Date date) {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        return df.format(date);
    }

    // remove harmful extra tags from description html
    // leave everything between <body> and </body>
    private String stripHead(String s) {
        int start, end;
        if (((start = s.indexOf("<body>")) == -1)
                || ((end = s.indexOf("</body>", start + 6)) == -1)) {
            return s;
        } else {
            return s.substring(start + 6, end);
        }
    }

    private String toPlainText(String s) throws SAXException {
        String plain = HTMLHelper.htmlToPlainText(s);
        return plain.replace("&", "&amp;").trim();
    }

    public void exportSpecifications() throws ExportException {
        try {
            of.startSpecifications();
            exportSingleTypeSpecification(rt);
            of.endSpecifications();
        } catch (XMLStreamException ex) {
            throw new ExportException(ex);
        }
        pp.addText("Specification exported.");
    }

    // Export Specification (all requirements of the type)
    // Recursively exports children
    protected void exportSingleTypeSpecification(RequirementType t) throws ExportException {
        logger.info("Exporting Specification from type: {}", t.getName());
        try {
            of.writeSpecification(t.getName(), fixTag(t.getTag()), t.getDescription());
            of.startChildren();

            for (Requirement r : t.getRequirements(cs.getBaseline())) {
                exportRequirementNode(r);
            }
            of.endChildren();
            of.endSpecification();
        } catch (XMLStreamException | RemoteServerException | SAXException ex) {
            throw new ExportException(ex);
        }
    }

    private void exportRequirementNode(Requirement r) throws
            XMLStreamException, RemoteServerException, SAXException {
        logger.info("Exporting requirement node: {}", r.getName());
        String tagID = fixTag(r.getRequirementType().getTag()) + r.getIDNumber();
        of.writeSpecHierarchy(tagID);
        // are there children ?
        Requirement[] children = r.getChildRequirements();
        if (children.length > 0) {
            of.startChildren();
            // recursion for the children
            for (Requirement c : r.getChildRequirements()) {
                exportRequirementNode(c);
            }
            of.endChildren();
        }
        of.endSpecHierarchy();
    }

    // Here all traces are from one type to the same
    public void exportTraces() throws ExportException {
        try {
            of.startSpecRelations();
            for (Requirement r : rt.getRequirements(cs.getBaseline())) {
                exportTraces(r, true);
            }
            of.endSpecRelations();
            // exportRelationGroups();
        } catch (XMLStreamException | RemoteServerException ex) {
            throw new ExportException(ex);
        }
    }

    // Export requirement traces + recursion
    protected void exportTraces(Requirement r, boolean sameType) throws XMLStreamException, RemoteServerException {
        // now doing only to, because only traces within a type or project are used
        // We avoid dupes by not reporting the "other way around" trace
        // exportTracesFrom(r);
        if (sameType)
            exportTracesToSameType(r);
        else
            exportTracesTo(r);
        // recursion for the children
        for (Requirement c : r.getChildRequirements()) {
            exportTraces(c, sameType);
        }
    }

    // Export requirement traces (to any requirement in the same project)
    private void exportTracesTo(Requirement r) throws XMLStreamException, RemoteServerException {
        logger.info("Exporting traces of requirement: {}", r.getName());
        for (Trace t : r.getTracesTo()) {
            try {
                Object o = t.getToObject();
                if (o != null && o.getClass().equals(Requirement.class)) {
                    Requirement toR = ((Requirement) o);
                    RequirementType toType = toR.getRequirementType();

                    // Needs to be in the same project
                    // Using StringBuilder for better performance
                    if (toR.getProject().equals(pr)) {
                        StringBuilder source = new StringBuilder();
                        source.append(fixTag(r.getRequirementType().getTag())).append(r.getIDNumber());
                        StringBuilder target = new StringBuilder();
                        target.append(fixTag(toType.getTag())).append(toR.getIDNumber());
                        StringBuilder relationID =  new StringBuilder();
                        relationID.append(source).append("-to-").append(target);
                        if (reverse_traces)
                            of.writeSpecRelation(relationID.toString(), target.toString(), source.toString(),
                                    t.isSuspect());
                        else
                            of.writeSpecRelation(relationID.toString(), source.toString(), target.toString(),
                                    t.isSuspect());
                    }
                }
            } catch (ObjectDoesNotExistException e) {
                logger.warn("ObjectDoesNotExistException in exportTracesTo(). Probably ToObject of trace is deleted.");
            }
        }
    }

    // Export requirement traces (same type requirement in the same project)
    private void exportTracesToSameType(Requirement r) throws XMLStreamException, RemoteServerException {
        logger.info("Exporting traces of requirement to same type: {}", r.getName());
        for (Trace t : r.getTracesTo()) {
            try {
                Object o = t.getToObject();
                if (o != null && o.getClass().equals(Requirement.class)) {
                    Requirement toR = ((Requirement) o);
                    RequirementType toType = toR.getRequirementType();

                    // Needs to be in the same project && same type
                    // Using StringBuilder for better performance
                    if (toR.getProject().equals(pr) && (toType.equals(rt))) {
                        String tag = fixTag(toType.getTag());
                        StringBuilder source = new StringBuilder();
                        source.append(tag).append(r.getIDNumber());
                        StringBuilder target = new StringBuilder();
                        target.append(tag).append(toR.getIDNumber());
                        StringBuilder relationID =  new StringBuilder();
                        relationID.append(source).append("-to-").append(target);
                        if (reverse_traces)
                            of.writeSpecRelation(relationID.toString(), target.toString(), source.toString(),
                                    t.isSuspect());
                        else
                            of.writeSpecRelation(relationID.toString(), source.toString(), target.toString(),
                                t.isSuspect());
                    }
                }
            } catch (ObjectDoesNotExistException e) {
                logger.warn("ObjectDoesNotExistException in exportTracesTo(). Probably ToObject of trace is deleted.");
            }
        }
    }

    protected HashMap<String, HashSet<String>> relationGroups;

    // Add to relation group map, if entry is not there already
    private void addToRelationGroup(String group, String relation) {
        HashSet<String> relations;
        if (relationGroups.containsKey(group)) {
            // group already in map, add the relation
            relations = relationGroups.get(group);
            relations.add(relation);
        }
        else {
            // Create a new set, add relation to it. Map group to the new set
            relations = new HashSet<>();
            relations.add(relation);
            relationGroups.put(group, relations);
        }
    }

    // Export relation groups
    protected void exportRelationGroups() throws XMLStreamException {
        of.startRelationGroups();
        relationGroups.forEach((group, relations) -> {
            try {
                of.writeRelationGroup(group, relations);
            } catch (XMLStreamException e) {
                e.printStackTrace();
            }
        });
        of.endRelationGroups();
    }

}
