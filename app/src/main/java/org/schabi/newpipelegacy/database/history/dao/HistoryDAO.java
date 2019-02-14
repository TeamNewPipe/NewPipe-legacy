package org.schabi.newpipelegacy.database.history.dao;

import org.schabi.newpipelegacy.database.BasicDAO;

public interface HistoryDAO<T> extends BasicDAO<T> {
    T getLatestEntry();
}
