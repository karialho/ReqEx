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

import com.starbase.caliber.Requirement;
import com.starbase.caliber.RequirementType;
import com.starbase.caliber.Trace;
import com.starbase.caliber.attribute.Attribute;
import com.starbase.caliber.attribute.AttributeValue;
import com.starbase.caliber.attribute.UDAList;
import com.starbase.caliber.server.ObjectDoesNotExistException;
import com.starbase.caliber.server.RemoteServerException;
import com.starbase.caliber.util.HTMLHelper;
import org.xml.sax.SAXException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static fi.improveit.req_ex.RoundTripGUI.theGUI;

public class ExcelExportType extends Export {
    private ExcelOutFile eof;
    private static final Logger logger = LoggerFactory.getLogger(ExcelExportType.class.getName());

    public ExcelExportType(CaliberSession s, String p, String reqt, String od, int maxReq, ExportPanel ep, ProgressPanel pp) throws ExportException {
        super(s, p, reqt, od, maxReq, ep, pp);
    }

    private static String fixDescription(String s) throws SAXException {
        String fixed = HTMLHelper.htmlToPlainText(s);
        return fixed.trim();
    }

    public void init() throws ExportException {
        // set project and type
        try {
            // open export file
            eof = new ExcelOutFile(outputDirectory, project, reqType);
            // create worksheet with requirement type as name
            eof.open();
        } catch (ExcelFileException ex) {
            throw new ExportException(ex);
        }
    }

    public void finish() {
        try {
            eof.close();
        } catch (ExcelFileException ex) {
            logger.info("ExcelFileException: {}", ex.getMessage());
        }
    }

    public void exportDefinitions() throws ExportException {
        try {
            exportTitles(rt);
        } catch (RemoteServerException ex) {
            throw new ExportException(ex);
        }
        if (rt != null) pp.addText("Titles exported.");
    }

    // Export title row into Excel file
    protected void exportTitles(RequirementType t) throws RemoteServerException {
        logger.info("Exporting requirement titles for: {}", t.getName());
        eof.addSheet(t.getName());
        eof.addRow();
        eof.addTitleCell("Requirement");
        eof.addTitleCell("ID");
        eof.addTitleCell("Status");
        eof.addTitleCell("Priority");
        eof.addTitleCell("Owner");
        eof.addTitleCell("Description");
        eof.addTitleCell("Traces from");
        eof.addTitleCell("Traces to");
        for (Attribute a : t.getAttributes()) {
            eof.addTitleCell(a.getName());
        }
    }

    public void exportRequirements() throws ExportException {
        exportSingleTypeRequirements(rt);
    }

    protected void exportSingleTypeRequirements(RequirementType t) throws ExportException {
        try {
            logger.info("Exporting requirements of type: {}", t.getName());
            for (Requirement r : t.getRequirements(cs.getBaseline())) {
                // export top level requirements
                exportRequirement(r, (short) 0);
                currFirstLevelCount++;
                theGUI.progressPanel.setProgress(20 + (int) ((currFirstLevelCount * 60.0) / firstLevelReqCount));
            }
            closeType();
        } catch (RemoteServerException | ExcelFileException | SAXException ex) {
            throw new ExportException(ex);
        }
    }

    // Export requirement row into Excel file
    private void exportRequirement(Requirement r, short level) throws ExcelFileException,
            RemoteServerException, SAXException, ExportException {
        if (currReqCount == maxReqCount)
            throw new ExportException("Req count limit encountered.");
        else {
            currReqCount++;
            SummaryPanel.exported++;
            logger.info("Exporting requirement: {}", r.getName());
            eof.addRow();
            eof.addNameCell(r.getName(), level);
            eof.addCell(r.getRequirementType().getTag() + r.getIDNumber());
            eof.addCell(r.getStatus().getSelectedValue().toString());
            eof.addCell(r.getPriority().getSelectedValue().toString());
            String owner;
            if (r.getOwner() != null) {
                owner = r.getOwner().getUserIDString();
            } else {
                owner = "";
            }
            eof.addCell(owner);
            if (r.getDescription() != null) {
                eof.addDescriptionCell(ExcelExportType.fixDescription(r.getDescription().getText()));
            } else {
                eof.addDescriptionCell("");
            }
            exportTracesFrom(r);
            exportTracesTo(r);
            for (Attribute a : r.getRequirementType().getAttributes()) {
                for (AttributeValue av : r.getAttributeValues()) {
                    if (av.getAttribute().equals(a)) {
                        if (a instanceof UDAList && ((UDAList) a).getMaximumSelections() != 1)
                            eof.addCell((new RValue(av)).getValues());
                        else
                            eof.addCell((new RValue(av)).getValue());
                    }
                }
            }
            for (Requirement c : r.getChildRequirements()) {
                exportRequirement(c, (short) (level + 1));
            }
        }
    }

    // close current sheet and autosize columns
    private void closeType() throws ExcelFileException {
        eof.closeSheet();
    }

    // Export requirement traces from
    private void exportTracesFrom(Requirement r) throws RemoteServerException {
        Trace[] traces = r.getTracesFrom();
        int traceCount = traces.length;
        int i = 0;
        StringBuilder traceList = new StringBuilder();
        for (Trace t : traces) {
            String tag = "";
            String project = "";
            if (t.getFromObject().getClass().equals(Requirement.class)) {
                tag = ((Requirement) t.getFromObject()).getRequirementType().getTag();
                project = ((Requirement) t.getFromObject()).getProject().getName();
            }
            traceList.append(tag).append(t.getTraceFromID().getIDNumber()).append(" | ").append(project);
            if (++i < traceCount) {
                traceList.append("\n");
            }
        }
        eof.addCellWrapStyle(traceList.toString());
    }

    // Export requirement traces to
    private void exportTracesTo(Requirement r) throws RemoteServerException {
        Trace[] traces = r.getTracesTo();
        int traceCount = traces.length;
        int i = 0;
        StringBuilder traceList = new StringBuilder();
        for (Trace t : traces) {
            try {
                Object o = t.getToObject();
                if (o != null) {
                    String tag = "";
                    String project = "";
                    if (o.getClass().equals(Requirement.class)) {
                        tag = ((Requirement) o).getRequirementType().getTag();
                        project = ((Requirement) o).getProject().getName();
                    }
                    traceList.append(tag).append(t.getTraceToID().getIDNumber());
                    if (project.equals(this.project))
                        traceList.append(" | ").append(project);
                    else
                        traceList.append(" || ").append(project);
                    if (++i < traceCount) {
                        traceList.append("\n");
                    }
                }
            } catch (ObjectDoesNotExistException e) {
                logger.warn("ObjectDoesNotExistException in exportTracesTo(). Probably ToObject of trace is deleted.");
            }
        }
        eof.addCellWrapStyle(traceList.toString());
    }

        // these are null implementations for ExcelExportProject class
        public void exportSpecifications () {
        }
        public void exportTraces () {
        }

    }
