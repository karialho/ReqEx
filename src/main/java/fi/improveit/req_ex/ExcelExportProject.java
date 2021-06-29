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

import com.starbase.caliber.RequirementType;
import com.starbase.caliber.server.RemoteServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ExcelExportProject extends ExcelExportType {

    private static final Logger logger = LoggerFactory.getLogger(ExcelExportProject.class.getName());

    public ExcelExportProject(CaliberSession s, String p, String rt, String od, int maxReq, ExportPanel ep, ProgressPanel pp) throws ExportException {
        super(s, p, rt, od, maxReq, ep, pp);
        logger.info("Excel export created, maxRows = {}", maxReq);
    }

    public void exportRequirements() throws ExportException {
        try {
            for (RequirementType t : cs.getTypes()) {
                exportTitles(t);
                exportSingleTypeRequirements(t);
            }
        } catch (RemoteServerException | CaliberException ex) {
            throw new ExportException(ex);
        }
    }

    // these are null implementations for ExcelExportProject class
    public void exportDefinitions() {}
    public void exportSpecifications() {}
    public void exportTraces() {}
}
