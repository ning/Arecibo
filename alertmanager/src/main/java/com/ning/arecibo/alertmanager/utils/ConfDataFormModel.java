package com.ning.arecibo.alertmanager.utils;

import com.ning.arecibo.alert.confdata.dao.ConfDataDAO;

public interface ConfDataFormModel {

    public boolean insert(ConfDataDAO confDataDAO);
    public boolean update(ConfDataDAO confDataDAO);
    public boolean delete(ConfDataDAO confDataDAO);

    public String getLastStatusMessage();
}
