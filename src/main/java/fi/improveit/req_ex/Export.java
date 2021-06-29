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

import com.starbase.caliber.Project;
import com.starbase.caliber.RequirementType;
import com.starbase.caliber.server.RemoteServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Export {

    protected final CaliberSession cs;
    protected final String project;
    protected final String reqType;
    protected final String outputDirectory;
    protected Project pr;
    protected RequirementType rt;
    // Set to the max req count from export panel
    protected final int maxReqCount;
    // current number during the export, including recursive
    protected int currReqCount = 0;
    // counted number of first level reqs
    protected int firstLevelReqCount = 0;
    // current number during the export (for progress counter)
    protected int currFirstLevelCount = 0;
    protected ExportPanel ep;
    protected ProgressPanel pp;

    private static final Logger logger = LoggerFactory.getLogger(Export.class.getName());

    public Export(CaliberSession s, String p, String reqt, String od,
                  int maxReq, ExportPanel ep, ProgressPanel pp) throws ExportException {
        cs = s;
        project = p;
        reqType = reqt;
        outputDirectory = od;
        maxReqCount = maxReq;
        this.ep = ep;
        this.pp = pp;

        try {
            pr = cs.getProject();
            logger.info("Project name: {}", project);
            if (reqType == null) {
                rt = null;
            } else {
                rt = cs.setType(reqType);
            }
            // get number of (top level) requirements to export
            countReqs();
        } catch (RemoteServerException | CaliberException ex) {
            throw new ExportException(ex);
        }
    }

    abstract void init() throws ExportException;
    abstract void finish() throws ExportException;

    // new simpler interface with same methods for Type & Project scoped export
    abstract void exportDefinitions() throws ExportException;
    abstract void exportRequirements() throws ExportException;
    abstract void exportSpecifications() throws ExportException;
    abstract void exportTraces() throws ExportException;

    // count top level requirements to export
    private void countReqs() throws RemoteServerException, CaliberException {

        if (rt == null) {  // count all types
            for (RequirementType t : cs.getTypes()) {
                firstLevelReqCount += countOneType(t);
                SummaryPanel.types++;
            }
        } else {           // count one type (previously set in constructor)
            firstLevelReqCount = countOneType(rt);
            SummaryPanel.types = 1;
        }
    }

    private int countOneType(RequirementType t) throws RemoteServerException {
        return t.getRequirements(cs.getBaseline()).length;
    }

}
