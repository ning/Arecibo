/*
 * Copyright 2010-2012 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.ning.arecibo.alertmanager.utils.columns;

import java.sql.Timestamp;
import java.io.Serializable;
import java.text.SimpleDateFormat;

import org.apache.wicket.model.Model;
import org.apache.wicket.model.IModel;

public class TimestampPropertyColumn<T> extends PropertyColumnWithCssClass<T> implements Serializable {

    //TODO: use Joda Time
    public final static String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private final SimpleDateFormat df;

    public TimestampPropertyColumn(IModel<String> displayModel,String sortProperty,String propertyExpression) {
        this(displayModel,sortProperty,propertyExpression,null,DEFAULT_DATE_TIME_FORMAT);
    }

    public TimestampPropertyColumn(IModel<String> displayModel,String sortProperty,String propertyExpression,String cssClass) {
        this(displayModel,sortProperty,propertyExpression,cssClass,DEFAULT_DATE_TIME_FORMAT);
    }

    public TimestampPropertyColumn(IModel<String> displayModel,String sortProperty,String propertyExpression,String cssClass,String formatPattern) {
        super(displayModel,sortProperty,propertyExpression,cssClass);
        this.df = new SimpleDateFormat(formatPattern);
    }

    @Override
    protected IModel<String> createLabelModel(IModel<T> rowModel) {

        IModel retModel = super.createLabelModel(rowModel);
        Timestamp ts = (Timestamp)retModel.getObject();
        if(ts != null)
            return new Model<String>(this.df.format(ts));
        else
            return null;
    }

    //TODO: break this out into separate util static class (use Joda time)
    public static String format(Timestamp ts) {
        return format(ts,DEFAULT_DATE_TIME_FORMAT);
    }

    public static String format(Timestamp ts,String formatPattern) {
        SimpleDateFormat df = new SimpleDateFormat(formatPattern);
        return df.format(ts);
    }
}
