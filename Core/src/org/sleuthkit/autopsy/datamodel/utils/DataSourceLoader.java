/*
 *
 * Autopsy Forensic Browser
 *
 * Copyright 2018-2019 Basis Technology Corp.
 * Contact: carrier <at> sleuthkit <dot> org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sleuthkit.autopsy.datamodel.utils;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.sleuthkit.autopsy.casemodule.Case;
import org.sleuthkit.autopsy.casemodule.NoCurrentCaseException;
import org.sleuthkit.datamodel.SleuthkitCase;
import org.sleuthkit.datamodel.TskCoreException;

/**
 * Encapsulates logic required to create a mapping of data sources in the
 * current case to their data source IDs.
 *
 * Intended to be used within the context of a SwingWorker or other background
 * thread.
 */
public class DataSourceLoader {

    private static final String SELECT_DATA_SOURCES_LOGICAL = "select obj_id, name from tsk_files where obj_id in (SELECT obj_id FROM tsk_objects WHERE obj_id in (select obj_id from data_source_info))";

    private static final String SELECT_DATA_SOURCES_IMAGE = "select obj_id, name from tsk_image_names where obj_id in (SELECT obj_id FROM tsk_objects WHERE obj_id in (select obj_id from data_source_info))";

    /**
     * Query case database for a list of all existing logical data sources and
     * their object IDs. Method can return either full path to data sources or
     * just data source file names.
     *
     * @param tskDb SleuthkitCase database handle
     * @param dataSourceMap map to which to add data sources
     * @throws TskCoreException
     * @throws SQLException
     */
    private void loadLogicalSources(SleuthkitCase tskDb, Map<Long, String> dataSouceMap) throws TskCoreException, SQLException {
        //try block releases resources - exceptions are handled in done()
        try (
                SleuthkitCase.CaseDbQuery query = tskDb.executeQuery(SELECT_DATA_SOURCES_LOGICAL);
                ResultSet resultSet = query.getResultSet()) {
            while (resultSet.next()) {
                Long objectId = resultSet.getLong(1);
                String dataSourceName = resultSet.getString(2);
                dataSouceMap.put(objectId, dataSourceName);
            }
        }
    }

    /**
     * Query case database for a list of all existing image data sources and
     * their object IDs. Method can return either full path to data sources or
     * just data source file names.
     *
     * @param tskDb SleuthkitCase database handle
     * @param dataSourceMap map to which to add data sources
     * @param fullPath true if full path to data source is needed, false
     * otherwise
     * @throws SQLException
     * @throws TskCoreException
     */
    private void loadImageSources(SleuthkitCase tskDb, Map<Long, String> dataSourceMap, boolean fullPath) throws SQLException, TskCoreException {
        //try block releases resources - exceptions are handled in done()
        try (
                SleuthkitCase.CaseDbQuery query = tskDb.executeQuery(SELECT_DATA_SOURCES_IMAGE);
                ResultSet resultSet = query.getResultSet()) {

            while (resultSet.next()) {
                Long objectId = resultSet.getLong(1);
                if (!dataSourceMap.containsKey(objectId)) {
                    String dataSourceName = resultSet.getString(2);
                    if (fullPath) {
                        dataSourceMap.put(objectId, dataSourceName);
                    } else {
                        File image = new File(dataSourceName);
                        String dataSourceNameTrimmed = image.getName();
                        dataSourceMap.put(objectId, dataSourceNameTrimmed);
                    }
                }
            }
        }
    }

    /**
     * Get a map of data source Ids to their string names (data source file name
     * only) for the current case.
     *
     * @return Map of Long (id) to String (data source file name only)
     *
     * @throws NoCurrentCaseException
     * @throws TskCoreException
     * @throws SQLException
     */
    public Map<Long, String> getDataSourceMap() throws NoCurrentCaseException, TskCoreException, SQLException {
        Map<Long, String> dataSouceMap = new HashMap<>();

        Case currentCase = Case.getCurrentCaseThrows();
        SleuthkitCase tskDb = currentCase.getSleuthkitCase();

        loadLogicalSources(tskDb, dataSouceMap);

        loadImageSources(tskDb, dataSouceMap, false);

        return dataSouceMap;
    }

    /**
     * Get a map of data source Ids to their string names (full path) for the
     * current case.
     *
     * @return Map of Long (id) to String (full path to data source)
     *
     * @throws NoCurrentCaseException
     * @throws TskCoreException
     * @throws SQLException
     */
    public Map<Long, String> getFullPathDataSourceMap() throws NoCurrentCaseException, TskCoreException, SQLException {
        Map<Long, String> dataSouceMap = new HashMap<>();

        Case currentCase = Case.getCurrentCaseThrows();
        SleuthkitCase tskDb = currentCase.getSleuthkitCase();

        loadLogicalSources(tskDb, dataSouceMap);

        loadImageSources(tskDb, dataSouceMap, true);

        return dataSouceMap;
    }
}
