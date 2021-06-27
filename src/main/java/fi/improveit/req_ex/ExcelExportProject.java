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
