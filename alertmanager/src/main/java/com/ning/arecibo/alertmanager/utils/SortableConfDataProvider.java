package com.ning.arecibo.alertmanager.utils;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Comparator;
import java.util.Collections;
import java.util.ArrayList;

import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.data.table.filter.IFilterStateLocator;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.commons.beanutils.BeanUtils;
import com.ning.arecibo.alert.confdata.objects.ConfDataObject;

import com.ning.arecibo.util.Logger;

public class SortableConfDataProvider<T extends ConfDataObject> extends SortableDataProvider<T> implements IFilterStateLocator {
    private final static Logger log = Logger.getLogger(SortableConfDataProvider.class);

    private List<T> dataList;
    private List<T> filteredList;
    private T filterObject = null;


    public SortableConfDataProvider(List<T> dataList,String initialSortId) {
        this(dataList,initialSortId,null);
    }

    public SortableConfDataProvider(List<T> dataList,String initialSortId,Class<T> clazz) {

        // pain in the ass to assign a newInstance from a generic type param
        try {
            if(clazz != null)
                filterObject = clazz.newInstance();
        }
        catch(Exception ex) {
            log.warn(ex);
            filterObject = null;
        }

        if(dataList == null)
            dataList = new ArrayList<T>();

        this.dataList = dataList;

        // set default sort
        setSort(initialSortId, true);
    }

    public synchronized void updateDataList(List<T> dataList) {
        this.dataList = dataList;
    }

    @Override
    public synchronized Iterator<T> iterator(int first,int count) {
        SortParam sp = getSort();
        Collections.sort(filteredList,new ConfDataComparator<T>(sp.getProperty(),sp.isAscending()));
        return new ListIterator<T>(filteredList,first,count);
    }

    @Override
    public IModel<T> model(T object) {
        return new Model<T>(object);
    }

    @Override
    public synchronized int size() {
        updateFilteredList();
        return filteredList.size();
    }

    @Override
    public Object getFilterState() {
        return filterObject;
    }

    @Override
    public void setFilterState(Object state) {
        filterObject = (T)state;
    }

    private void updateFilteredList() {

        if (this.filterObject == null) {
            this.filteredList = this.dataList;
        }
        else {
            this.filteredList = new ArrayList<T>();
            for (T t : this.dataList) {
                if (this.filterObject.filterMatchesDataObject(t)) {
                    this.filteredList.add(t);
                }
            }
        }
    }

    private class ConfDataComparator<T> implements Comparator<T> {

        private final String property;
        private final boolean isAscending;

        public ConfDataComparator(String property,Boolean isAscending) {
            this.property = property;
            this.isAscending = isAscending;
        }

        @Override
        public int compare(T o1,T o2) {

            try {
                String val1 = BeanUtils.getProperty(o1,property);
                String val2 = BeanUtils.getProperty(o2,property);

                if(isAscending) {
                    if(val1 == null) {
                        if(val2 == null) {
                            return 0;
                        }
                        else {
                            return 1;
                        }
                    }
                    else if(val2 == null) {
                        return -1;
                    }
                    else
                        return val1.toLowerCase().compareTo(val2.toLowerCase());
                }
                else {
                    if(val2 == null) {
                        if(val1 == null) {
                            return 0;
                        }
                        else {
                            return 1;
                        }
                    }
                    else if(val1 == null) {
                        return -1;
                    }
                    else
                        return val2.toLowerCase().compareTo(val1.toLowerCase());
                }
            }
            catch(Exception ex) {
                throw new RuntimeException (ex);
            }
        }
    }

    private class ListIterator<T extends ConfDataObject> implements Iterator<T> {

        private final List<T> filteredList;
        private final int first;
        private final int count;

        private int current;

        public ListIterator(List<T> filteredList,int first,int count) {
            this.filteredList = filteredList;
            this.first = first;
            this.count = count;
            this.current = first;
        }

        @Override
        public boolean hasNext() {
            if(current > first + count)
                return false;

            if(filteredList.size() <= current)
                return false;

            return true;
        }

        @Override
        public T next() {
            if(!hasNext())
                throw new NoSuchElementException();

            return this.filteredList.get(this.current++);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
