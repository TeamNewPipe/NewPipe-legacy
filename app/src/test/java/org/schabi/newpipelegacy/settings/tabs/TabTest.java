package org.schabi.newpipelegacy.settings.tabs;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class TabTest {
    @Test
    public void checkIdDuplication() {
        final Set<Integer> usedIds = new HashSet<>();

        for (final Tab.Type type : Tab.Type.values()) {
            final boolean added = usedIds.add(type.getTabId());
            assertTrue("Id was already used: " + type.getTabId(), added);
        }
    }
}
