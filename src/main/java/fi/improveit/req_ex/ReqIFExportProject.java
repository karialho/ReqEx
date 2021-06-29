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
import com.starbase.caliber.server.RemoteServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.xml.stream.XMLStreamException;

public class ReqIFExportProject extends ReqIFExportType {
    protected static final Logger logger = LoggerFactory.getLogger(ReqIFExportProject.class.getName());

    public ReqIFExportProject(CaliberSession s, String p, String rt, String od, int maxReq, ExportPanel ep, ProgressPanel pp) throws ExportException {
        super(s, p, rt, od, maxReq, ep, pp);
    }

    public void exportDefinitions() throws ExportException {
        try {
            of.startDataTypes();
            exportSystemDataTypes();
            pp.addText("System data types exported.");
            for (RequirementType t : cs.getTypes())
                exportDataTypes(t);
            pp.addText("User defined data types exported.");
            of.endDataTypes();

            of.startSpecTypes();
            for (RequirementType t : pr.getRequirementTypes())
                exportSpecType(t);
            // Constant Relation Type (Trace)
            of.writeSpecRelationType();
            of.writeRelationGroupType();
            of.endSpecTypes();
        } catch (XMLStreamException | CaliberException | RemoteServerException ex) {
            throw new ExportException(ex);
        }
    }

    public void exportRequirements() throws ExportException {
        try {
            of.startSpecObjects();
            for (RequirementType t : cs.getTypes()) {
                exportSingleTypeRequirements(t); // will update progress within from 20-80
            }
            of.endSpecObjects();
        } catch (XMLStreamException | CaliberException ex) {
            throw new ExportException(ex);
        }
    }

    public void exportSpecifications() throws ExportException {
        try {
            of.startSpecifications();
            for (RequirementType t : cs.getTypes()) {
                exportSingleTypeSpecification(t);
            }
            of.endSpecifications();
        } catch (XMLStreamException | CaliberException ex) {
            throw new ExportException(ex);
        }
        pp.addText("Project specifications exported.");
    }

    public void exportTraces() throws ExportException {
        try {
            of.startSpecRelations();
            for (RequirementType t : cs.getTypes()) {
                for (Requirement r : t.getRequirements(cs.getBaseline())) {
                    logger.info("Exporting traces for: {}", r.getName());
                    exportTraces(r, false);
                }
            }
            of.endSpecRelations();
//            exportRelationGroups();
        } catch (XMLStreamException | RemoteServerException | CaliberException ex) {
            logger.info("ExportException: {}", ex.getMessage());
            throw new ExportException(ex);
        }
    }

}
