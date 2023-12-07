package com.untitled.ecm.core;

import com.untitled.ecm.testcommons.ResourceTestBase;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class TestBackgroundTaskManager extends ResourceTestBase {

    @Test
    public void testBackGroundTaskManager() {
        assertTrue(DakiyaBackGroundTasksManager.isBackGroundTaskManagerUP());
        assertTrue(DakiyaBackGroundTasksManager.getCurrentBackGroundThreadCount() <= RULE.getConfiguration().getMaxDakiyaBackgroundTasks());
    }
}
