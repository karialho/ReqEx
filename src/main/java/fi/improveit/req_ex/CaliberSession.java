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
import com.starbase.caliber.attribute.Attribute;
import com.starbase.caliber.server.CaliberServer;
import com.starbase.caliber.server.RemoteServerException;

import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaliberSession {
    private static final String CURRENT_BASELINE = "Current Baseline";

    private final CaliberServer server;
    private final Session session;
    private Project project;
    private Baseline baseline;
    private RequirementType rtype;

    private static final Logger logger = LoggerFactory.getLogger(CaliberSession.class.getName());

    // Open a new session with a Caliber server
    CaliberSession(String host, String user, char[] passwd)
            throws CaliberException {
        if (host.contains(":")) {
            String[] h = host.split(":");
            server = new CaliberServer(h[0], Integer.parseInt(h[1]));
            System.out.println("host: " + h[0] + ", port:" + h[1]);
        } else {
            server = new CaliberServer(host);
            System.out.println("host: " + host);
        }
        try {
            System.out.println("user: " + user);
            System.out.println("passwd: *******");
            session = server.login(user, new String(passwd));
            for (@SuppressWarnings("unused") char c : passwd) {
                c = '\0';
            }
        } catch (RemoteServerException e) {
            throw new CaliberException(e);
        }
        project = null;
        rtype = null;
    }

    public void logout() {
        if (session.isLoggedIn()) {
            session.logout();
        }
    }

    // Get server version number
    public String getVersion() throws CaliberException {
        try {
            return server.getServerFileVersion();
        } catch (RemoteServerException e) {
            throw new CaliberException(e);
        }
    }

    // Get project table
    public String[] getProjectNames() throws CaliberException {
        try {
            logger.info("getProjectNames()");
            Project[] plist = session.getProjects();
            String[] names = new String[plist.length];
            int i = 0;
            for (Project p : plist) {
                names[i++] = p.getName();
            }
            return names;
        } catch (RemoteServerException e) {
            throw new CaliberException(e);
        }
    }

    // Set current project based on supplied name.
    public void setProject(String name) throws CaliberException {
        try {
            for (Project p : session.getProjects()) {
                if (p.getName().equals(name)) {
                    project = p;
                    baseline = p.getCurrentBaseline();
                    logger.info("Setting project to: {}", p.getName());
                    break;
                }
            }
        } catch (RemoteServerException e) {
            throw new CaliberException(e);
        }
    }

    public Project getProject() {
        return project;
    }

    // Get baseline names of a project
    public String[] getBaselineNames() throws CaliberException {
        try {
            if (project == null)
                return null;
            Baseline[] blList = project.getBaselines();
            String[] names = new String[blList.length - 1];
            int i = 0;
            for (Baseline b : blList) {
                switch (b.getName()) {
                    case "Deleted View":
                        break;
                    case CURRENT_BASELINE:
                        names[i++] = "Current";
                        break;
                    default:
                        names[i++] = b.getName();
                        break;
                }
            }
            return names;
        } catch (RemoteServerException e) {
            throw new CaliberException(e);
        }
    }

    // Set current baseline based on supplied name.
    public void setBaseline(String name) throws CaliberException {
        try {
            if (project == null)
                return;
            for (Baseline b : project.getBaselines()) {
                if ((b.getName().equals(CURRENT_BASELINE) && name.equals("Current"))
                        || b.getName().equals(name)) {
                    baseline = b;
                    logger.info("Setting baseline to: {}", b.getName());
                    break;
                }
            }
        } catch (RemoteServerException e) {
            throw new CaliberException(e);
        }
    }

    // Get current baseline
    public Baseline getBaseline() {
        return baseline;
    }

    // Get requirement types of current project / baseline
    public RequirementType[] getTypes() throws CaliberException {
        if (project != null) {
            try {
                RequirementType[] types;
                if (baseline != null) {
                    types = baseline.getRequirementTypes();
                } else {
                    types = project.getRequirementTypes();
                }
                Arrays.sort(types);
                return types;
            } catch (RemoteServerException e) {
                throw new CaliberException(e);
            }
        } else {
            return null;
        }
    }

    // Set current requirement type on supplied name.
    public RequirementType setType(String name) throws CaliberException {
        try {
            if (project == null)
                return null;
            RequirementType[] types;
            if (baseline != null) {
                types = baseline.getRequirementTypes();
            } else {
                types = project.getRequirementTypes();
            }
            for (RequirementType t : types) {
                if (t.getName().equals(name)) {
                    rtype = t;
                    logger.info("Setting requirement type to: {}", rtype.getName());
                    return rtype;
                }
            }
            return null;
        } catch (RemoteServerException e) {
            throw new CaliberException(e);
        }
    }

    public RequirementType getRequirementType() {
        return rtype;
    }

    // Get requirement types of a project
    public String[] getTypeNames() throws CaliberException {
        RequirementType[] typeList = getTypes();
        String[] names = new String[typeList.length];
        int i = 0;
        for (RequirementType t : typeList) {
            names[i++] = t.getName();
        }
        return names;
    }

    // Get all attributes of current session.
    public Attribute[] getAttributes() throws CaliberException {
        if (project != null) {
            try {
                return session.getAttributes();
            } catch (RemoteServerException e) {
                throw new CaliberException(e);
            }
        } else {
            return null;
        }
    }

    public String getHost() {
        return server.getHost();
    }


    public String getUser() {
        assert (session != null);
        try {
            User u = session.getCurrentUser();
            return u.getFirstName() + " " + u.getLastName();
        } catch (RemoteServerException e) {
            e.printStackTrace();
            return "";
        }
    }

    public Session getSession() {
        return session;
    }

}
