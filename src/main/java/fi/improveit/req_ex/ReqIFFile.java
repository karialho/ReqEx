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

import com.starbase.caliber.User;
import com.starbase.caliber.attribute.*;
import com.starbase.caliber.cache.ImageCache;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashSet;
import java.util.UUID;

import javanet.staxutils.IndentingXMLStreamWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static fi.improveit.req_ex.ReqIFExportType.dateToISO;

/**
 * High level abstraction of an ReqIF XML file
 *
 * @author Kari Alho
 */
public class ReqIFFile {

    private final String directory;
    private final String project;
    private final String reqType;

    XMLOutputFactory xof;
    XMLStreamWriter xtw;
    OutputStreamWriter osw;
    FileOutputStream fos;
    private String creationTime;
    private String filename;

    private static final String SPEC_RELATION_TYPE = "trace";
    private static final String RELATION_GROUP_TYPE = "requirementType-to-requirementType";
    private static final String EXPORT_FILE_PREFIX = "Export - ";
    private static final String EXPORT_FILE_SUFFIX = ".reqif";

    private static final Logger logger = LoggerFactory.getLogger(ReqIFFile.class.getName());

    ReqIFFile(String dir, String project, String reqType) throws ExcelFileException {
        if (dir.length() < 1) {
            throw new ExcelFileException("Directory needs to be specified");
        }
        this.directory = dir;
        SummaryPanel.outputDir = dir;
        this.project = project;
        this.reqType = reqType;
        xof = XMLOutputFactory.newInstance();
    }

    public String filename() { return filename; }

    public void open() throws XMLStreamException, IOException {
        // open the physical file
        if (reqType == null)
            filename = fixFileName(EXPORT_FILE_PREFIX + project + EXPORT_FILE_SUFFIX);
        else
            filename = fixFileName(EXPORT_FILE_PREFIX + project + " - " + reqType + EXPORT_FILE_SUFFIX);

        String pathname;
        pathname = directory + "\\" + filename;
        SummaryPanel.outputFile = filename;
        logger.info("Opening reqIF file for writing: {}", pathname);

        File check = new File(pathname);
        if (check.exists())
            logger.warn("Overwriting existing export file: {}", pathname);
        fos = new FileOutputStream(pathname);
        osw = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
        XMLStreamWriter writer = xof.createXMLStreamWriter(fos, "utf-8");
        xtw = new IndentingXMLStreamWriter(writer);
        xtw.writeStartDocument("utf-8", "1.0");
        xtw.writeStartElement("REQ-IF");
        xtw.writeAttribute("xmlns", "http://www.omg.org/spec/ReqIF/20110401/reqif.xsd");
        xtw.writeAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        xtw.writeAttribute("xmlns:reqif", "http://www.omg.org/spec/ReqIF/20110401/reqif.xsd");
        xtw.writeAttribute("xmlns:reqif-xhtml", "http://www.w3.org/1999/xhtml");
        xtw.writeAttribute("xmlns:reqif-common", "http://www.prostep.org/reqif");
        xtw.writeAttribute("xsi:schemaLocation", "http://www.omg.org/spec/ReqIF/20110401/reqif.xsd http://www.omg.org/spec/ReqIF/20110401/reqif.xsd");
        xtw.writeAttribute("xml:lang", "en");

        LocalDateTime dateTime = LocalDateTime.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        creationTime = dtf.format(dateTime);

        // Initialize Caliber image cache to the export dir
        ImageCache.setImageCacheDir(directory);
    }

    private String fixFileName(String s) {
        return s.replace("<", "-")
                .replace(">", "-")
                .replace(":", "-")
                .replace("/", "-")
                .replace("\\", "-")
                .replace("*", "-")
                .replace("?", "-");
    }

    public void close() throws XMLStreamException {
        logger.info("Closing XML file.");
        xtw.writeEndElement();      // REQ-IF-CONTENT
        xtw.writeEndElement();      // CORE-CONTENT
        xtw.writeEndElement();      // REQ-IF
        xtw.writeEndDocument();
        xtw.flush();
        xtw.close();
    }

    public void writeHeader(CaliberSession cs) throws XMLStreamException, CaliberException {
        logger.info("Writing REQ-IF Header.");
        xtw.writeStartElement("THE-HEADER");
        xtw.writeStartElement("REQ-IF-HEADER");
        String identifier = UUID.randomUUID().toString();
        xtw.writeAttribute("IDENTIFIER", "id-" + identifier);

        xtw.writeStartElement("COMMENT");
        xtw.writeCharacters("Export from Micro Focus Caliber, executed by " + cs.getUser());
        xtw.writeEndElement();  // COMMENT

        xtw.writeStartElement("CREATION-TIME");
        xtw.writeCharacters(creationTime);
        xtw.writeEndElement();  // CREATION-TIME

        xtw.writeStartElement("REPOSITORY-ID");
        xtw.writeCharacters(cs.getHost());
        xtw.writeEndElement();  // REPOSITORY-ID

        xtw.writeStartElement("REQ-IF-TOOL-ID");
        xtw.writeCharacters("ReqEx by ImproveIt Oy");
        xtw.writeEndElement();  // REQ-IF-TOOL-ID

        xtw.writeStartElement("REQ-IF-VERSION");
        xtw.writeCharacters("1.0");
        xtw.writeEndElement();  // REQ-IF-VERSION

        xtw.writeStartElement("SOURCE-TOOL-ID");
        xtw.writeCharacters("Micro Focus Caliber " + cs.getVersion());
        xtw.writeEndElement();  // SOURCE-TOOL-ID

        xtw.writeStartElement("TITLE");
        xtw.writeCharacters("Project: " + cs.getProject().getName());
        if (cs.getRequirementType() == null)
            xtw.writeCharacters(": all requirement types");
        else
            xtw.writeCharacters(": " + cs.getRequirementType().getName());
        xtw.writeEndElement();   // TITLE

        xtw.writeEndElement();   // REQ-IF-HEADER
        xtw.writeEndElement();   // THE-HEADER
    }

    public void startContent() throws XMLStreamException {
        logger.info("Writing CORE-CONTENT.");
        xtw.writeStartElement("CORE-CONTENT");
        xtw.writeStartElement("REQ-IF-CONTENT");
    }

    public void startDataTypes() throws XMLStreamException {
        logger.info("Writing DATATYPES.");
        xtw.writeStartElement("DATATYPES");
    }

    public void endDataTypes() throws XMLStreamException {
        xtw.writeEndElement();  // DATATYPES
    }

    // Modify identifier so that it is a valid XML ID
    private String fixID(String s) {
        return s.replaceAll("[^-_:a-zA-Z]", "-");
    }

    /***********************************************************************************
     Datatype definition functions
     ***********************************************************************************/

   public void writeBooleanDataType() throws XMLStreamException {
        xtw.writeStartElement("DATATYPE-DEFINITION-BOOLEAN");
        xtw.writeAttribute("IDENTIFIER", "Boolean-datatype");
        xtw.writeAttribute("LONG-NAME", "Boolean-datatype");
        xtw.writeAttribute("LAST-CHANGE", creationTime);
        xtw.writeEndElement();  // DATATYPE-DEFINITION-BOOLEAN
        logger.info("Boolean datatype added.");
    }

    public void writeDateDataType() throws XMLStreamException {
        xtw.writeStartElement("DATATYPE-DEFINITION-DATE");
        xtw.writeAttribute("IDENTIFIER", "Date-datatype");
        xtw.writeAttribute("LONG-NAME", "Date-datatype");
        xtw.writeAttribute("LAST-CHANGE", creationTime);
        xtw.writeEndElement();  // DATATYPE-DEFINITION-DATE
        logger.info("Date datatype added.");
    }

    public void writeRealDataType(UDAFloat a) throws XMLStreamException {
        xtw.writeStartElement("DATATYPE-DEFINITION-REAL");
        xtw.writeAttribute("IDENTIFIER", fixID(a.getName()) + "-datatype");
        xtw.writeAttribute("LONG-NAME", a.getName());
        xtw.writeAttribute("LAST-CHANGE", creationTime);
        xtw.writeAttribute("MAX", String.valueOf(a.getMaximum()));
        xtw.writeAttribute("MIN", String.valueOf(a.getMinimum()));
        xtw.writeAttribute("ACCURACY", "1");
        xtw.writeEndElement();  // DATATYPE-DEFINITION-REAL
        logger.info("Real datatype added: {}", a.getName());
    }

    public void writeIntegerDataType(UDAInteger a) throws XMLStreamException {
        xtw.writeStartElement("DATATYPE-DEFINITION-INTEGER");
        xtw.writeAttribute("IDENTIFIER", fixID(a.getName()) + "-datatype");
        xtw.writeAttribute("LONG-NAME", a.getName());
        xtw.writeAttribute("LAST-CHANGE", creationTime);
        xtw.writeAttribute("MAX", String.valueOf(a.getMaximum()));
        xtw.writeAttribute("MIN", String.valueOf(a.getMinimum()));
        xtw.writeEndElement();  // DATATYPE-DEFINITION-INTEGER
        logger.info("Integer datatype added: {}", a.getName());
    }

    public void writeStringDataType(UDAText a) throws XMLStreamException {
        xtw.writeStartElement("DATATYPE-DEFINITION-STRING");
        xtw.writeAttribute("IDENTIFIER", fixID(a.getName()) + "-datatype");
        xtw.writeAttribute("LONG-NAME", a.getName());
        xtw.writeAttribute("LAST-CHANGE", creationTime);
        xtw.writeAttribute("MAX-LENGTH", String.valueOf(a.getMaximumLength()));
        xtw.writeEndElement();  // DATATYPE-DEFINITION-STRING
        logger.info("String datatype added: {}", a.getName());
    }

    public void writeStringDataType(String name, int maxLength) throws XMLStreamException {
        xtw.writeStartElement("DATATYPE-DEFINITION-STRING");
        xtw.writeAttribute("IDENTIFIER", fixID(name) + "-datatype");
        xtw.writeAttribute("LONG-NAME", name);
        xtw.writeAttribute("LAST-CHANGE", creationTime);
        xtw.writeAttribute("MAX-LENGTH", String.valueOf(maxLength));
        xtw.writeEndElement();  // DATATYPE-DEFINITION-STRING
        logger.info("String datatype added: {}", name);
    }

    public void writeXHTMLDataType(String name) throws XMLStreamException {
        xtw.writeStartElement("DATATYPE-DEFINITION-XHTML");
        xtw.writeAttribute("IDENTIFIER", fixID(name) + "-datatype");
        xtw.writeAttribute("LONG-NAME", name);
        xtw.writeAttribute("LAST-CHANGE", creationTime);
        xtw.writeEndElement();  // DATATYPE-DEFINITION-XHTML
        logger.info("XHTML datatype added: {}", name);
    }

    // Write Enum data type definition with values from UDAList attribute
    public void writeEnumDataType(String name, UDAList a) throws XMLStreamException {
        logger.info("Enum subtype: {}", a.getUITypeName());
        xtw.writeStartElement("DATATYPE-DEFINITION-ENUMERATION");
        xtw.writeAttribute("IDENTIFIER", fixID(name) + "-datatype");
        xtw.writeAttribute("LONG-NAME", name);
        xtw.writeAttribute("LAST-CHANGE", creationTime);
        xtw.writeStartElement("SPECIFIED-VALUES");
        int index = 0;
        HashSet<String> entries = new HashSet<>();
        for (UDAListEntry c : a.getListEntries()) {
            logger.info("UDAListEntry.toString(): {}", c.toString());
            Object e = c.getObject();
            // skip possible null elements
            if (e != null) {
                String longName;
                if (e instanceof User) {
                    longName = ((User) e).getFirstName() + " " + ((User) e).getLastName();
                } else {
                    longName = e.toString();
                }
                // check for duplicates
                if (entries.contains(longName))
                    break;
                entries.add(longName);
                xtw.writeStartElement("ENUM-VALUE");
                xtw.writeAttribute("LAST-CHANGE", creationTime);
                if (longName.length() == 0)
                    xtw.writeAttribute("IDENTIFIER", fixID(name) + "-value_Empty");
                else
                    xtw.writeAttribute("IDENTIFIER", fixID(name) + "-value_" + fixID(longName));
                xtw.writeAttribute("LONG-NAME", longName);
                xtw.writeStartElement("PROPERTIES");
                xtw.writeStartElement("EMBEDDED-VALUE");
                xtw.writeAttribute("KEY", Integer.toString(index));
                xtw.writeAttribute("OTHER-CONTENT", "");
                xtw.writeEndElement();  // EMBEDDED-VALUE
                xtw.writeEndElement();  // PROPERTIES
                xtw.writeEndElement();  // ENUM-VALUE
            }
            index++;
        }
        xtw.writeEndElement();  // SPECIFIED-VALUES
        xtw.writeEndElement();  // DATATYPE-DEFINITION-ENUMERATION
        logger.info("Enum datatype added: {}", "DATATYPE-" + a.getName());
    }

    // Write Enum data type definition with values from a String table
    public void writeEnumDataType(String name, String[] values) throws XMLStreamException {
        xtw.writeStartElement("DATATYPE-DEFINITION-ENUMERATION");
        xtw.writeAttribute("IDENTIFIER", fixID(name) + "-datatype");
        xtw.writeAttribute("LONG-NAME", name);
        xtw.writeAttribute("LAST-CHANGE", creationTime);
        xtw.writeStartElement("SPECIFIED-VALUES");
        int index = 0;
        for (String v : values) {
               xtw.writeStartElement("ENUM-VALUE");
                xtw.writeAttribute("LAST-CHANGE", creationTime);
                if (v.length() == 0)
                    xtw.writeAttribute("IDENTIFIER", fixID(name) + "-value_Empty");
                else
                    xtw.writeAttribute("IDENTIFIER", fixID(name) + "-value_" + fixID(v));
                xtw.writeAttribute("LONG-NAME", v);
                xtw.writeStartElement("PROPERTIES");
                xtw.writeStartElement("EMBEDDED-VALUE");
                xtw.writeAttribute("KEY", Integer.toString(index));
                xtw.writeAttribute("OTHER-CONTENT", "");
                xtw.writeEndElement();  // EMBEDDED-VALUE
                xtw.writeEndElement();  // PROPERTIES
                xtw.writeEndElement();  // ENUM-VALUE
            index++;
        }
        xtw.writeEndElement();  // SPECIFIED-VALUES
        xtw.writeEndElement();  // DATATYPE-DEFINITION-ENUMERATION
        logger.info("Enum datatype added: {}", "DATATYPE-" + name);
    }

   /*
    Spec-type related functions
     */

    public void startSpecTypes() throws XMLStreamException {
        logger.info("Writing SPEC-TYPES.");
        xtw.writeStartElement("SPEC-TYPES");
    }

    // write one Spec Object Type (Caliber Requirement type) element
    public void writeSpecObjectTypeWithAttributes(String name, String tag, String desc) throws XMLStreamException {
        xtw.writeStartElement("SPEC-OBJECT-TYPE");
        xtw.writeAttribute("DESC", desc);
        xtw.writeAttribute("IDENTIFIER", tag + "-type");
        xtw.writeAttribute("LAST-CHANGE", creationTime);
        xtw.writeAttribute("LONG-NAME", name);
        xtw.writeStartElement("SPEC-ATTRIBUTES");
        logger.info("Spec Object Type added: {}", name);
    }

    // end Spec Object Type element
    public void endSpecType() throws XMLStreamException {
        xtw.writeEndElement();  // SPEC-ATTRIBUTES
        xtw.writeEndElement();  // SPEC-OBJECT_TYPE
    }

    // Write Specification Type (Requirement list for one type) element
    // No attributes
    public void writeSpecificationType(String desc, String tag, String name) throws XMLStreamException {
        xtw.writeStartElement("SPECIFICATION-TYPE");
        xtw.writeAttribute("DESC", desc);
        xtw.writeAttribute("IDENTIFIER", tag + "-list");
        xtw.writeAttribute("LAST-CHANGE", creationTime);
        xtw.writeAttribute("LONG-NAME", name);
        xtw.writeEndElement();  // SPECIFICATION-TYPE
        logger.info("Specification Type added: {}", name);
    }

    // Single Constant Relation Type (Trace)
    public void writeSpecRelationType() throws XMLStreamException {
        xtw.writeStartElement("SPEC-RELATION-TYPE");
        xtw.writeAttribute("DESC", "Caliber trace from/to another requirement. Suspect status can be read.");
        xtw.writeAttribute("IDENTIFIER", SPEC_RELATION_TYPE);
        xtw.writeAttribute("LAST-CHANGE", creationTime);
        xtw.writeAttribute("LONG-NAME", "Trace from or to");
        xtw.writeStartElement("SPEC-ATTRIBUTES");
        writeSuspectAttribute();
        xtw.writeEndElement();  // SPEC-ATTRIBUTES
        xtw.writeEndElement();  // SPEC-RELATION-TYPE
    }

    // Single Constant Relation Group Type (from type to type)
    public void writeRelationGroupType() throws XMLStreamException {
        xtw.writeStartElement("RELATION-GROUP-TYPE");
        xtw.writeAttribute("DESC", "Caliber trace from one requirement type to another (or the same).");
        xtw.writeAttribute("IDENTIFIER", RELATION_GROUP_TYPE);
        xtw.writeAttribute("LAST-CHANGE", creationTime);
        xtw.writeAttribute("LONG-NAME", "requirement type to requirement type");
        xtw.writeEndElement();  // RELATION-GROUP-TYPE
    }

    // end Spec Object Type element
    public void endSpecTypes() throws XMLStreamException {
        xtw.writeEndElement();  // SPEC-TYPES
    }

    /***********************************************************************************
     Attribute definition functions
     ***********************************************************************************/

    private void writeSuspectAttribute() throws XMLStreamException {
        xtw.writeStartElement("ATTRIBUTE-DEFINITION-BOOLEAN");
        xtw.writeAttribute("DESC", "The suspect status of the trace.");
        xtw.writeAttribute("IDENTIFIER", "Suspect-attribute");
        xtw.writeAttribute("LONG-NAME", "Suspect");
        xtw.writeAttribute("LAST-CHANGE", creationTime);
        xtw.writeStartElement("TYPE");
        xtw.writeStartElement("DATATYPE-DEFINITION-BOOLEAN-REF");
        // Standard datatype for Boolean attributes
        xtw.writeCharacters("Boolean-datatype");
        xtw.writeEndElement();  // DATATYPE-DEFINITION-BOOLEAN-REF
        xtw.writeEndElement();  // TYPE
        xtw.writeEndElement();  // ATTRIBUTE-DEFINITION-BOOLEAN
    }

    public void writeStringAttribute(String tag, String name, String desc) throws XMLStreamException {
        xtw.writeStartElement("ATTRIBUTE-DEFINITION-STRING");
        xtw.writeAttribute("DESC", desc);
        xtw.writeAttribute("IDENTIFIER", tag + "-" + fixID(name) + "-attribute");
        xtw.writeAttribute("LONG-NAME", name);
        xtw.writeAttribute("LAST-CHANGE", creationTime);
        xtw.writeStartElement("TYPE");
        xtw.writeStartElement("DATATYPE-DEFINITION-STRING-REF");
        // Standard datatype for system String attributes
        xtw.writeCharacters("String-datatype");
        xtw.writeEndElement();  // DATATYPE-DEFINITION-STRING-REF
        xtw.writeEndElement();  // TYPE
        xtw.writeEndElement();  // ATTRIBUTE-DEFINITION-STRING
        logger.info("String attribute added: {}", name);
    }

    public void writeXHTMLAttribute(String tag, String name, String desc) throws XMLStreamException {
        xtw.writeStartElement("ATTRIBUTE-DEFINITION-XHTML");
        xtw.writeAttribute("DESC", desc);
        xtw.writeAttribute("IDENTIFIER", tag + "-" + fixID(name) + "-attribute");
        xtw.writeAttribute("LONG-NAME", name);
        xtw.writeAttribute("LAST-CHANGE", creationTime);
        xtw.writeStartElement("TYPE");
        xtw.writeStartElement("DATATYPE-DEFINITION-XHTML-REF");
        xtw.writeCharacters("XHTML-datatype");
        xtw.writeEndElement();  // DATATYPE-DEFINITION-XHTML-REF
        xtw.writeEndElement();  // TYPE
        xtw.writeEndElement();  // ATTRIBUTE-DEFINITION-XHTML
        logger.info("XHTML attribute added: {}", name);
    }

    public void writeDateAttribute(String tag, String name, String desc) throws XMLStreamException {
        xtw.writeStartElement("ATTRIBUTE-DEFINITION-DATE");
        xtw.writeAttribute("DESC", desc);
        xtw.writeAttribute("IDENTIFIER", tag + "-" + fixID(name) + "-attribute");
        xtw.writeAttribute("LONG-NAME", name);
        xtw.writeAttribute("LAST-CHANGE", creationTime);
        xtw.writeStartElement("TYPE");
        xtw.writeStartElement("DATATYPE-DEFINITION-DATE-REF");
        // Standard datatype for Date attributes
        xtw.writeCharacters("Date-datatype");
        xtw.writeEndElement();  // DATATYPE-DEFINITION-DATE-REF
        xtw.writeEndElement();  // TYPE
        xtw.writeEndElement();  // ATTRIBUTE-DEFINITION-DATE
    }

    public void writeEnumAttribute(String tag, String name, String desc, boolean multiValued) throws XMLStreamException {
        xtw.writeStartElement("ATTRIBUTE-DEFINITION-ENUMERATION");
        xtw.writeAttribute("DESC", desc);
        xtw.writeAttribute("IDENTIFIER",tag + "-" + fixID(name) + "-attribute");
        xtw.writeAttribute("LONG-NAME", name);
        xtw.writeAttribute("LAST-CHANGE", creationTime);
        xtw.writeAttribute("IS-EDITABLE", "true");
        xtw.writeAttribute("MULTI-VALUED", String.valueOf(multiValued));
        xtw.writeStartElement("TYPE");
        xtw.writeStartElement("DATATYPE-DEFINITION-ENUMERATION-REF");
        xtw.writeCharacters(fixID(name) + "-datatype");
        xtw.writeEndElement();  // DATATYPE-DEFINITION-ENUMERATION-REF
        xtw.writeEndElement();  // TYPE
        xtw.writeEndElement();  // ATTRIBUTE-DEFINITION-ENUMERATION
        logger.info("Attribute added: {}", name);
    }

    // Write a Caliber UDA Attribute reference for Specification type
    // Text, Boolean, Date, Integer, Float and List types supported
    public void writeUDAAttribute(String tag, Attribute a) throws XMLStreamException {
        boolean finished = false;
        String type = null;
        if (a instanceof UDAText)
            type = "String";
        else if (a instanceof UDABoolean)
            type = "Boolean";
        else if (a instanceof UDADate)
            type = "Date";
        else if (a instanceof UDAInteger)
            type = "Integer";
        else if (a instanceof UDAFloat)
            type = "Real";
        else if (a instanceof UDAList)
            type = "Enumeration";
        else {
            finished = true;// Other types not supported
        }
        if (!finished) {
            xtw.writeStartElement("ATTRIBUTE-DEFINITION-" + type.toUpperCase());
            xtw.writeAttribute("DESC", a.getDescription());
            xtw.writeAttribute("IDENTIFIER",tag + "-" + fixID(a.getName()) + "-attribute");
            xtw.writeAttribute("LONG-NAME", a.getName());
            xtw.writeAttribute("LAST-CHANGE", creationTime);
            if (a instanceof UDAList) {
                xtw.writeAttribute("IS-EDITABLE", "true");
                String mv;
                if (((UDAList) a).getMaximumSelections() == 1)
                    mv = "false";
                else
                    mv = "true";
                xtw.writeAttribute("MULTI-VALUED", mv);
            }
            xtw.writeStartElement("TYPE");
            xtw.writeStartElement("DATATYPE-DEFINITION-" + type.toUpperCase() + "-REF");
            if (a instanceof UDABoolean || a instanceof UDADate)
                // These types have only one single datatype
                xtw.writeCharacters(type + "-datatype");
            else
                // Other types have a datatype per (UDA) attribute
                xtw.writeCharacters(fixID(a.getName()) + "-datatype");
            xtw.writeEndElement();  // DATATYPE-DEFINITION-<TYPE>-REF
            xtw.writeEndElement();  // TYPE
            xtw.writeEndElement();  // ATTRIBUTE-DEFINITION-<TYPE>
            logger.info(type + " attribute added: {}", a.getName());
        }
    }

   /***********************************************************************************
    Spec object functions
    ***********************************************************************************/

    public void startSpecObjects() throws XMLStreamException {
        logger.info("Writing SPEC-OBJECTS.");
        xtw.writeStartElement("SPEC-OBJECTS");
    }

    public void endSpecObjects() throws XMLStreamException {
        logger.info("Ending SPEC-OBJECTS.");
        xtw.writeEndElement();  // SPEC-OBJECTS
    }

    // write one Spec Object (Caliber Requirement) element
    public void writeSpecObject(String identifier, String lastChange,
                                String name, String type) throws XMLStreamException {
        xtw.writeStartElement("SPEC-OBJECT");
        xtw.writeAttribute("IDENTIFIER", identifier);
        xtw.writeAttribute("LAST-CHANGE", lastChange);
        xtw.writeAttribute("LONG-NAME", name);
        xtw.writeStartElement("TYPE");
        xtw.writeStartElement("SPEC-OBJECT-TYPE-REF");
        xtw.writeCharacters(type + "-type");
        xtw.writeEndElement();  // SPEC-OBJECT-TYPE-REF
        xtw.writeEndElement();  // TYPE
    }

    public void endSpecObject() throws XMLStreamException {
        logger.info("Ending SPEC-OBJECT.");
        xtw.writeEndElement();  // SPEC-OBJECT
    }

    /***********************************************************************************
     Attribute value writing functions
     ***********************************************************************************/

    public void writeStringAttributeValue(String tag, String name, String value) throws XMLStreamException {
        xtw.writeStartElement("ATTRIBUTE-VALUE-STRING");
        xtw.writeAttribute("THE-VALUE", value);
        xtw.writeStartElement("DEFINITION");
        xtw.writeStartElement("ATTRIBUTE-DEFINITION-STRING-REF");
        xtw.writeCharacters(tag + "-" + fixID(name) + "-attribute");
        xtw.writeEndElement();  // ATTRIBUTE-DEFINITION-STRING-REF
        xtw.writeEndElement();  // DEFINITION
        xtw.writeEndElement();  // ATTRIBUTE-VALUE-STRING
        logger.info("String attribute value added: {}", name);
    }

    public void writeXHTMLAttributeValue(String tag, String name, String value) throws XMLStreamException, IOException {
        xtw.writeStartElement("ATTRIBUTE-VALUE-XHTML");
        xtw.writeStartElement("THE-VALUE");
        xtw.flush();

        // Special hack to write this element correctly
        osw.write("><reqif-xhtml:div>");
        osw.write(value);
        osw.write("</reqif-xhtml:div");
        osw.flush();

        xtw.writeEndElement();  // THE-VALUE
        xtw.writeStartElement("DEFINITION");
        xtw.writeStartElement("ATTRIBUTE-DEFINITION-XHTML-REF");
        xtw.writeCharacters(tag + "-" + fixID(name) + "-attribute");
        xtw.writeEndElement();  // ATTRIBUTE-DEFINITION-XHTML-REF
        xtw.writeEndElement();  // DEFINITION
        xtw.writeEndElement();  // ATTRIBUTE-VALUE-XHTML
        logger.info("XHTML attribute value added: {}", name);
    }

    public void writeDateAttributeValue(String tag, String name, Date date) throws XMLStreamException {
        xtw.writeStartElement("ATTRIBUTE-VALUE-DATE");
        xtw.writeAttribute("THE-VALUE", dateToISO(date));
        xtw.writeStartElement("DEFINITION");
        xtw.writeStartElement("ATTRIBUTE-DEFINITION-DATE-REF");
        xtw.writeCharacters(tag + "-" + fixID(name) + "-attribute");
        xtw.writeEndElement();  // ATTRIBUTE-DEFINITION-DATE-REF
        xtw.writeEndElement();  // DEFINITION
        xtw.writeEndElement();  // ATTRIBUTE-VALUE-DATE
        logger.info("Date attribute value added: {}", name);
    }

    public void writeScalarAttributeValue(String tag, Attribute a, String value) throws XMLStreamException {
        String type = null;
        if (a instanceof UDAText)
            type = "String";
        else if (a instanceof UDABoolean)
            type = "Boolean";
        else if (a instanceof UDADate)
            type = "Date";
        else if (a instanceof UDAInteger)
            type = "Integer";
        else if (a instanceof UDAFloat)
            type = "Real";
        if (type != null) {
            xtw.writeStartElement("ATTRIBUTE-VALUE-" + type.toUpperCase());
            xtw.writeAttribute("THE-VALUE", value);
            xtw.writeStartElement("DEFINITION");
            xtw.writeStartElement("ATTRIBUTE-DEFINITION-" + type.toUpperCase() + "-REF");
            xtw.writeCharacters(tag + "-" + fixID(a.getName()) + "-attribute");
            xtw.writeEndElement();  // ATTRIBUTE-DEFINITION-<TYPE>-REF
            xtw.writeEndElement();  // DEFINITION
            xtw.writeEndElement();  // ATTRIBUTE-VALUE-<TYPE>
            logger.info("Scalar attribute value added: {}", a.getName());
        }
    }

    public void writeEnumAttributeValue(String tag, String name, String value) throws XMLStreamException {
        xtw.writeStartElement("ATTRIBUTE-VALUE-ENUMERATION");

        xtw.writeStartElement("DEFINITION");
        xtw.writeStartElement("ATTRIBUTE-DEFINITION-ENUMERATION-REF");
        xtw.writeCharacters(tag + "-" + fixID(name) + "-attribute");
        xtw.writeEndElement();  // ATTRIBUTE-DEFINITION-ENUMERATION-REF
        xtw.writeEndElement();  // DEFINITION

        xtw.writeStartElement("VALUES");
        if (value != null) {
            xtw.writeStartElement("ENUM-VALUE-REF");
            if (value.length() == 0)
                xtw.writeCharacters(fixID(name) + "-value_Empty");
            else
                xtw.writeCharacters( fixID(name) + "-value_" + fixID(value));
            xtw.writeEndElement();  // ENUM-VALUE-REF
        }
        xtw.writeEndElement();  // VALUES

        xtw.writeEndElement();  // ATTRIBUTE-VALUE-ENUMERATION");
        logger.info("Enum attribute value added: {}", name);
    }

    public void writeEnumAttributeValues(String tag, String name, String[] values) throws XMLStreamException {
        xtw.writeStartElement("ATTRIBUTE-VALUE-ENUMERATION");

        xtw.writeStartElement("DEFINITION");
        xtw.writeStartElement("ATTRIBUTE-DEFINITION-ENUMERATION-REF");
        xtw.writeCharacters(tag + "-" + fixID(name) + "-attribute");
        xtw.writeEndElement();  // ATTRIBUTE-DEFINITION-ENUMERATION-REF
        xtw.writeEndElement();  // DEFINITION

        xtw.writeStartElement("VALUES");
        for (String value : values) {
            if (value != null) {
                xtw.writeStartElement("ENUM-VALUE-REF");
                if (value.length() == 0)
                    xtw.writeCharacters(fixID(name) + "-value_Empty");
                else
                    xtw.writeCharacters( fixID(name) + "-value_" + fixID(value));
                xtw.writeEndElement();  // ENUM-VALUE-REF
            }
        }
        xtw.writeEndElement();  // VALUES

        xtw.writeEndElement();  // ATTRIBUTE-VALUE-ENUMERATION");
        logger.info("Enum attribute value added: {}", name);
    }

    public void startValues() throws XMLStreamException {
        logger.info("Writing VALUES.");
        xtw.writeStartElement("VALUES");
    }

    public void endValues() throws XMLStreamException {
        logger.info("Ending VALUES.");
        xtw.writeEndElement();  // VALUES
    }

    /***********************************************************************************
     Specification writing functions
     ***********************************************************************************/

    public void startSpecifications() throws XMLStreamException {
        logger.info("Writing SPECIFICATIONS.");
        xtw.writeStartElement("SPECIFICATIONS");
    }

    public void endSpecifications() throws XMLStreamException {
        logger.info("Ending SPECIFICATIONS.");
        xtw.writeEndElement();  // SPECIFICATIONS
    }

    // write one Specification Object element
    public void writeSpecification(String name, String tag, String desc) throws XMLStreamException {
        xtw.writeStartElement("SPECIFICATION");
        xtw.writeAttribute("DESC", desc);
        xtw.writeAttribute("IDENTIFIER", tag + "-spec");
        xtw.writeAttribute("LAST-CHANGE", creationTime);
        xtw.writeAttribute("LONG-NAME", name);
        xtw.writeStartElement("TYPE");
        xtw.writeStartElement("SPECIFICATION-TYPE-REF");
        xtw.writeCharacters(tag + "-list");
        xtw.writeEndElement();  // SPECIFICATION-TYPE-REF
        xtw.writeEndElement();  // TYPE

        logger.info("Specification added: {}", name);
    }

    public void endSpecification() throws XMLStreamException {
        logger.info("Ending SPECIFICATION.");
        xtw.writeEndElement();  // SPECIFICATION
    }

    public void startChildren() throws XMLStreamException {
        logger.info("Writing CHILDREN.");
        xtw.writeStartElement("CHILDREN");
    }

    public void endChildren() throws XMLStreamException {
        logger.info("Ending CHILDREN.");
        xtw.writeEndElement();  // CHILDREN
    }

    // write one Specification Object element
    public void writeSpecHierarchy(String ref) throws XMLStreamException {
        xtw.writeStartElement("SPEC-HIERARCHY");
        xtw.writeAttribute("IDENTIFIER", ref + "-node");
        xtw.writeAttribute("LAST-CHANGE", creationTime);
        xtw.writeStartElement("OBJECT");
        xtw.writeStartElement("SPEC-OBJECT-REF");
        xtw.writeCharacters(ref);
        xtw.writeEndElement();  // SPEC-OBJECT-REF
        xtw.writeEndElement();  // OBJECT

        logger.info("Specification hierarchy added: {}", ref);
    }

    public void endSpecHierarchy() throws XMLStreamException {
        logger.info("Ending SPEC-HIERARCHY.");
        xtw.writeEndElement();  // SPEC-HIERARCHY
    }

    /***********************************************************************************
     Spec relations (traces) writing functions
     ***********************************************************************************/

    public void startSpecRelations() throws XMLStreamException {
        logger.info("Writing SPEC-RELATIONS.");
        xtw.writeStartElement("SPEC-RELATIONS");
    }

    // write one Specification Relation (trace) element
    public void writeSpecRelation(String id, String source, String target, boolean suspect) throws XMLStreamException {
        xtw.writeStartElement("SPEC-RELATION");
        xtw.writeAttribute("IDENTIFIER", id);
        xtw.writeAttribute("LAST-CHANGE", creationTime);

        xtw.writeStartElement("SOURCE");
        xtw.writeStartElement("SPEC-OBJECT-REF");
        xtw.writeCharacters(source);
        xtw.writeEndElement();  // SPEC-OBJECT-REF
        xtw.writeEndElement();  // SOURCE

        xtw.writeStartElement("TARGET");
        xtw.writeStartElement("SPEC-OBJECT-REF");
        xtw.writeCharacters(target);
        xtw.writeEndElement();  // SPEC-OBJECT-REF
        xtw.writeEndElement();  // TARGET

        xtw.writeStartElement("TYPE");
        xtw.writeStartElement("SPEC-RELATION-TYPE-REF");
        xtw.writeCharacters(SPEC_RELATION_TYPE);
        xtw.writeEndElement();  // SPEC-RELATION-TYPE-REF
        xtw.writeEndElement();  // TYPE

        xtw.writeStartElement("VALUES");
        xtw.writeStartElement("ATTRIBUTE-VALUE-BOOLEAN");
        xtw.writeAttribute("THE-VALUE", String.valueOf(suspect));
        xtw.writeStartElement("DEFINITION");
        xtw.writeStartElement("ATTRIBUTE-DEFINITION-BOOLEAN-REF");
        xtw.writeCharacters("Suspect-attribute");
        xtw.writeEndElement();  // ATTRIBUTE-DEFINITION-BOOLEAN-REF
        xtw.writeEndElement();  // DEFINITION
        xtw.writeEndElement();  // ATTRIBUTE-VALUE-BOOLEAN

        xtw.writeEndElement();  // VALUES

        xtw.writeEndElement();  // SPEC-RELATION

        logger.info("SPEC-RELATION added: {}", id);
    }

    public void endSpecRelations() throws XMLStreamException {
        logger.info("Ending SPEC-RELATIONS.");
        xtw.writeEndElement();
    }

    /* Relation groups are currently not used.
    public void startRelationGroups() throws XMLStreamException {
        logger.info("Writing SPEC-RELATION-GROUPS.");
        xtw.writeStartElement("SPEC-RELATION-GROUPS");
    }
    public void endRelationGroups() throws XMLStreamException {
        logger.info("Ending SPEC-RELATION-GROUPS.");
        xtw.writeEndElement();
    }

    // write one Relation Group element
    public void writeRelationGroup(String group, HashSet<String> relations) throws XMLStreamException {
        xtw.writeStartElement("RELATION-GROUP");
        xtw.writeAttribute("IDENTIFIER", group);
        xtw.writeAttribute("LONG-NAME", group);
        xtw.writeAttribute("LAST-CHANGE", creationTime);

        xtw.writeStartElement("SPEC-RELATIONS");
        for (String r : relations) {
            xtw.writeStartElement("SPEC-RELATION-REF");
            xtw.writeCharacters(r);
            xtw.writeEndElement();  // SPEC-RELATION-REF
        }
        xtw.writeEndElement();  // SPEC-RELATIONS

        xtw.writeStartElement("TYPE");
        xtw.writeStartElement("RELATION-GROUP-TYPE-REF");
        xtw.writeCharacters(RELATION_GROUP_TYPE);
        xtw.writeEndElement();  // SPEC-RELATION-GROUP-TYPE-REF
        xtw.writeEndElement();  // TYPE

        String sourceSpec = group.substring(0, group.indexOf("-")) + "-spec";
        xtw.writeStartElement("SOURCE-SPECIFICATION");
        xtw.writeStartElement("SPECIFICATION-REF");
        xtw.writeCharacters(sourceSpec);
        xtw.writeEndElement();  // SPECIFICATION-REF
        xtw.writeEndElement();  // SOURCE-SPECIFICATION

        String targetSpec = group.substring(group.lastIndexOf("-") + 1) + "-spec";
        xtw.writeStartElement("TARGET-SPECIFICATION");
        xtw.writeStartElement("SPECIFICATION-REF");
        xtw.writeCharacters(targetSpec);
        xtw.writeEndElement();  // SPECIFICATION-REF
        xtw.writeEndElement();  // TARGET-SPECIFICATION

        xtw.writeEndElement();  // RELATION-GROUP

        logger.info("RELATION-GROUP added: {}", group);
    }

     */

}
